package com.liftoff.engine.sim;

import com.liftoff.engine.OrbitMath;
import com.liftoff.model.Bus;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.MissionDesign;
import com.liftoff.session.stub.Mission;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.SplittableRandom;


public final class MissionSimulator {

    private static final double SECONDS_PER_MINUTE = 60;
    private static final double MEGABITS_PER_GIGABIT = 1000;

    private MissionSimulator() {
    }


    public static SimulationResult run(MissionDesign design, Mission mission, Difficulty difficulty, long seed) {
        Bus bus = design.bus();
        double storageGbit = bus == null || bus.storageGbit() == null
                ? Double.POSITIVE_INFINITY : bus.storageGbit();
        return run(design, mission, difficulty, seed, EventDeck.fromClasspath(), new SimExtras(storageGbit, 0, true));
    }

    public static SimulationResult run(MissionDesign design, Mission mission, Difficulty difficulty, long seed,
                                       EventDeck deck, SimExtras extras) {
        return run(design, mission, difficulty, seed, deck, extras, null);
    }


    public record DeepSpaceLink(double powerW, double downlinkMbps, OptionalDouble contactMinutesPerDay) {
        public DeepSpaceLink(double powerW, double downlinkMbps, double contactMinutesPerDay) {
            this(powerW, downlinkMbps, OptionalDouble.of(contactMinutesPerDay));
        }
    }

    public static SimulationResult run(MissionDesign design, Mission mission, Difficulty difficulty, long seed,
                                       EventDeck deck, SimExtras extras, DeepSpaceLink link) {
        double stepDays = mission.stepUnit() == Mission.StepUnit.DAY ? 1 : SimRules.DAYS_PER_MONTH;
        int steps = mission.steps() > 0 ? mission.steps() : design.scienceMonths();
        EventDeck offered = deck.forDestination(mission.destination());

        List<Instrument> instruments = design.instruments();
        List<GroundStation> stations = design.stations();
        Bus bus = design.bus();
        double supplyW = link == null ? (bus == null ? 0 : bus.payloadPowerW()) : link.powerW();
        double drawW = instruments.stream().mapToDouble(Instrument::avgPowerW).sum();
        double downlinkMbps = link == null ? (bus == null ? 0 : bus.downlinkMbps()) : link.downlinkMbps();


        double[] output = new double[instruments.size()];
        java.util.Arrays.fill(output, 1);
        double[] contactMinutesPerDay = new double[stations.size()];
        for (int i = 0; i < contactMinutesPerDay.length; i++) {
            if (link != null && link.contactMinutesPerDay().isPresent()) {
                contactMinutesPerDay[i] = link.contactMinutesPerDay().getAsDouble() / stations.size();
            } else if (design.orbit() != null) {
                contactMinutesPerDay[i] = OrbitMath.contactMinutesPerDay(design.orbit(), stations.get(i).latitudeDeg());
            }
        }

        double storedGbit = 0;
        double safeModeDays = 0;
        double dustDays = 0;
        double outageDays = 0;
        int outageStation = -1;
        int launchSlipMonths = 0;

        List<SimStep> timeline = new ArrayList<>();
        List<SimEvent> events = new ArrayList<>();
        double totalProduced = 0;
        double totalSent = 0;
        double totalLost = 0;
        SimLimit firstLimit = SimLimit.NONE;
        int firstLimitIndex = -1;
        boolean suppliesRanOut = false;

        for (int step = 0; step < steps && !suppliesRanOut; step++) {
            Optional<EventCard> card = offered.draw(difficulty, seed, step);
            if (card.isPresent()) {

                SplittableRandom random = EventDeck.randomFor(~seed, step);
                SimEffect effect = SimEffect.none();
                switch (card.get().type()) {
                    case SOLAR_STORM -> {
                        safeModeDays = Math.max(safeModeDays, SimRules.SAFE_MODE_DAYS);
                        effect = new SimEffect(1, SimRules.SAFE_MODE_DAYS, null);
                    }
                    case STATION_OUTAGE -> {
                        if (!stations.isEmpty()) {
                            outageStation = random.nextInt(stations.size());
                            outageDays = SimRules.STATION_OUTAGE_DAYS;
                            effect = new SimEffect(1, SimRules.STATION_OUTAGE_DAYS, stations.get(outageStation).id());
                        }
                    }
                    case INSTRUMENT_FAULT -> {
                        if (!instruments.isEmpty()) {
                            int hit = random.nextInt(instruments.size());
                            output[hit] *= 1 - SimRules.INSTRUMENT_FAULT_DATA_LOST_FRACTION;
                            effect = new SimEffect(SimRules.INSTRUMENT_FAULT_DATA_LOST_FRACTION,
                                    Double.POSITIVE_INFINITY, instruments.get(hit).id());
                        }
                    }
                    case LAUNCH_SLIP -> {
                        launchSlipMonths += SimRules.LAUNCH_SLIP_MONTHS;
                        effect = new SimEffect(0, SimRules.LAUNCH_SLIP_MONTHS * SimRules.DAYS_PER_MONTH, null);
                    }
                    case DUST_STORM -> {
                        if (extras.solarPowered()) {
                            dustDays = Math.max(dustDays, SimRules.DUST_STORM_DAYS);
                            effect = new SimEffect(SimRules.DUST_SOLAR_POWER_LOST_FRACTION,
                                    SimRules.DUST_STORM_DAYS, null);
                        }
                    }
                }
                events.add(new SimEvent(step, card.get().type(), card.get().messageKey(), effect));
            }


            double safeModeShare = Math.min(safeModeDays, stepDays) / stepDays;
            double dustShare = Math.min(dustDays, stepDays) / stepDays;
            double outageShare = Math.min(outageDays, stepDays) / stepDays;
            safeModeDays = Math.max(0, safeModeDays - stepDays);
            dustDays = Math.max(0, dustDays - stepDays);
            outageDays = Math.max(0, outageDays - stepDays);


            double supplyNowW = supplyW * (1 - SimRules.DUST_SOLAR_POWER_LOST_FRACTION * dustShare);
            double shortfallW = Math.max(0, drawW - supplyNowW);
            double onShare = drawW > supplyNowW ? supplyNowW / drawW : 1;

            double producedPerDay = 0;
            for (int i = 0; i < output.length; i++) {
                producedPerDay += instruments.get(i).dataGbitPerDay() * output[i];
            }
            double produced = producedPerDay * stepDays * onShare * (1 - safeModeShare);


            double contactMinutes = 0;
            for (int i = 0; i < contactMinutesPerDay.length; i++) {
                contactMinutes += contactMinutesPerDay[i] * (i == outageStation ? 1 - outageShare : 1);
            }
            double sendable = downlinkMbps * contactMinutes * SECONDS_PER_MINUTE / MEGABITS_PER_GIGABIT * stepDays;

            double waiting = storedGbit + produced;
            double sent = Math.min(waiting, sendable);
            double backlog = waiting - sent;
            double lost = Math.max(0, backlog - extras.storageGbit());
            storedGbit = backlog - lost;

            double suppliesNeededKg = mission.crew() * mission.suppliesKgPerCrewDay() * stepDays;
            double suppliesUsedKg = (step + 1) * suppliesNeededKg;
            suppliesRanOut = suppliesUsedKg > extras.suppliesKg();
            double suppliesLeftKg = Math.max(0, extras.suppliesKg() - suppliesUsedKg);

            if (firstLimit == SimLimit.NONE) {


                firstLimit = shortfallW > 0 ? SimLimit.POWER
                        : lost > 0 ? SimLimit.STORAGE
                        : backlog > 0 ? SimLimit.DOWNLINK
                        : suppliesRanOut ? SimLimit.SUPPLIES
                        : SimLimit.NONE;
                if (firstLimit != SimLimit.NONE) {
                    firstLimitIndex = step;
                }
            }

            totalProduced += produced;
            totalSent += sent;
            totalLost += lost;
            timeline.add(new SimStep(step, produced, sent, lost, storedGbit, shortfallW, suppliesLeftKg));
        }

        boolean success = !suppliesRanOut && totalSent > 0;
        return new SimulationResult(mission.stepUnit(), timeline, events, totalProduced, totalSent, totalLost,
                storedGbit, launchSlipMonths, firstLimit, firstLimitIndex, success);
    }
}
