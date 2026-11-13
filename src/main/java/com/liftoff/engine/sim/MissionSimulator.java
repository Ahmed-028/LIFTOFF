package com.liftoff.engine.sim;

import com.liftoff.engine.OrbitMath;
import com.liftoff.model.Bus;
import com.liftoff.model.Difficulty;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.StepUnit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.SplittableRandom;

/**
 * Flies a design through time, one month (Earth, Mars) or one day (Moon) per step.
 * Pure and repeatable: the same inputs and seed always give the same result.
 */
public final class MissionSimulator {

    private static final double SECONDS_PER_MINUTE = 60;
    private static final double MEGABITS_PER_GIGABIT = 1000;

    private MissionSimulator() {
    }

    /** Flies with the events bundled in the catalog and no inputs beyond the design. */
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

    /** Ship power and radio. With no fixed contact window, the chosen orbit sets station visibility. */
    public record DeepSpaceLink(double powerW, double downlinkMbps, OptionalDouble contactMinutesPerDay) {
        public DeepSpaceLink(double powerW, double downlinkMbps, double contactMinutesPerDay) {
            this(powerW, downlinkMbps, OptionalDouble.of(contactMinutesPerDay));
        }
    }

    public static SimulationResult run(MissionDesign design, Mission mission, Difficulty difficulty, long seed,
                                       EventDeck deck, SimExtras extras, DeepSpaceLink link) {
        var flight = new Flight(design, mission, extras, link);
        int steps = Math.min(SimRules.MAX_STEPS, mission.steps() > 0 ? mission.steps() : design.scienceMonths());
        EventDeck offered = deck.forDestination(mission.destination());
        for (int step = 0; step < steps && !flight.suppliesRanOut; step++) {
            int index = step;
            offered.draw(difficulty, seed, step).ifPresent(card -> flight.meet(card, seed, index));
            flight.advance(step);
        }
        return flight.result();
    }

    /** Everything that changes during one flight. Used by one {@link #run} call only. */
    private static final class Flight {
        private final Mission mission;
        private final SimExtras extras;
        private final List<Instrument> instruments;
        private final List<GroundStation> stations;
        private final double stepDays;
        private final double supplyW;
        private final double drawW;
        private final double downlinkMbps;
        /** Share of each instrument's data still coming through (1 until a fault hits it). */
        private final double[] output;
        private final double[] contactMinutesPerDay;

        private double storedGbit;
        private double safeModeDays;
        private double dustDays;
        private double outageDays;
        private int outageStation = -1;
        private int launchSlipMonths;
        private final List<SimStep> timeline = new ArrayList<>();
        private final List<SimEvent> events = new ArrayList<>();
        private double totalProduced;
        private double totalSent;
        private double totalLost;
        private SimLimit firstLimit = SimLimit.NONE;
        private int firstLimitIndex = -1;
        private boolean suppliesRanOut;

        Flight(MissionDesign design, Mission mission, SimExtras extras, DeepSpaceLink link) {
            this.mission = mission;
            this.extras = extras;
            instruments = design.instruments();
            stations = design.stations();
            stepDays = mission.stepUnit() == StepUnit.DAY ? 1 : SimRules.DAYS_PER_MONTH;
            Bus bus = design.bus();
            supplyW = link == null ? (bus == null ? 0 : bus.payloadPowerW()) : link.powerW();
            drawW = instruments.stream().mapToDouble(Instrument::avgPowerW).sum();
            downlinkMbps = link == null ? (bus == null ? 0 : bus.downlinkMbps()) : link.downlinkMbps();
            output = new double[instruments.size()];
            Arrays.fill(output, 1);
            contactMinutesPerDay = new double[stations.size()];
            for (int i = 0; i < contactMinutesPerDay.length; i++) {
                if (link != null && link.contactMinutesPerDay().isPresent()) {
                    contactMinutesPerDay[i] = link.contactMinutesPerDay().getAsDouble() / stations.size();
                } else if (design.orbit() != null) {
                    contactMinutesPerDay[i] = OrbitMath.contactMinutesPerDay(design.orbit(), stations.get(i).latitudeDeg());
                }
            }
        }

        /** An event card is drawn: its effect starts now and runs into the coming steps. */
        void meet(EventCard card, long seed, int step) {
            // A second, independent stream picks what the event hits.
            SplittableRandom random = EventDeck.randomFor(~seed, step);
            SimEffect effect = switch (card.type()) {
                case SOLAR_STORM -> {
                    safeModeDays = Math.max(safeModeDays, SimRules.SAFE_MODE_DAYS);
                    yield new SimEffect(1, SimRules.SAFE_MODE_DAYS, null);
                }
                case STATION_OUTAGE -> {
                    if (stations.isEmpty()) {
                        yield SimEffect.none();
                    }
                    outageStation = random.nextInt(stations.size());
                    outageDays = SimRules.STATION_OUTAGE_DAYS;
                    yield new SimEffect(1, SimRules.STATION_OUTAGE_DAYS, stations.get(outageStation).id());
                }
                case INSTRUMENT_FAULT -> {
                    if (instruments.isEmpty()) {
                        yield SimEffect.none();
                    }
                    int hit = random.nextInt(instruments.size());
                    output[hit] *= 1 - SimRules.INSTRUMENT_FAULT_DATA_LOST_FRACTION;
                    yield new SimEffect(SimRules.INSTRUMENT_FAULT_DATA_LOST_FRACTION,
                            Double.POSITIVE_INFINITY, instruments.get(hit).id());
                }
                case LAUNCH_SLIP -> {
                    launchSlipMonths += SimRules.LAUNCH_SLIP_MONTHS;
                    yield new SimEffect(0, SimRules.LAUNCH_SLIP_MONTHS * SimRules.DAYS_PER_MONTH, null);
                }
                case DUST_STORM -> {
                    if (!extras.solarPowered()) {
                        yield SimEffect.none();
                    }
                    dustDays = Math.max(dustDays, SimRules.DUST_STORM_DAYS);
                    yield new SimEffect(SimRules.DUST_SOLAR_POWER_LOST_FRACTION, SimRules.DUST_STORM_DAYS, null);
                }
            };
            events.add(new SimEvent(step, card.type(), card.messageKey(), effect));
        }

        /** One step of the flight: data made, data sent home, data lost, supplies used. */
        void advance(int step) {
            // Share of this step each running effect covers; what is left carries into the next step.
            double safeModeShare = Math.min(safeModeDays, stepDays) / stepDays;
            double dustShare = Math.min(dustDays, stepDays) / stepDays;
            double outageShare = Math.min(outageDays, stepDays) / stepDays;
            safeModeDays = Math.max(0, safeModeDays - stepDays);
            dustDays = Math.max(0, dustDays - stepDays);
            outageDays = Math.max(0, outageDays - stepDays);

            // Power: a shortfall switches the instruments off part of the time, so they produce
            // proportionally less. It is never an outright failure.
            double supplyNowW = supplyW * (1 - SimRules.DUST_SOLAR_POWER_LOST_FRACTION * dustShare);
            double shortfallW = Math.max(0, drawW - supplyNowW);
            double onShare = drawW > supplyNowW ? supplyNowW / drawW : 1;
            double produced = producedPerDay() * stepDays * onShare * (1 - safeModeShare);

            double waiting = storedGbit + produced;
            double sent = Math.min(waiting, sendableGbit(outageShare));
            double backlog = waiting - sent;
            double lost = Math.max(0, backlog - extras.storageGbit());
            storedGbit = backlog - lost;

            double suppliesNeededKg = mission.crew() * mission.suppliesKgPerCrewDay() * stepDays;
            double suppliesUsedKg = (step + 1) * suppliesNeededKg;
            suppliesRanOut = suppliesUsedKg > extras.suppliesKg();
            double suppliesLeftKg = Math.max(0, extras.suppliesKg() - suppliesUsedKg);

            noteFirstLimit(step, shortfallW, lost, backlog);
            totalProduced += produced;
            totalSent += sent;
            totalLost += lost;
            timeline.add(new SimStep(step, produced, sent, lost, storedGbit, shortfallW, suppliesLeftKg));
        }

        private double producedPerDay() {
            double perDay = 0;
            for (int i = 0; i < output.length; i++) {
                perDay += instruments.get(i).dataGbitPerDay() * output[i];
            }
            return perDay;
        }

        /** Downlink: Mbps x contact minutes x 60 / 1000 = Gbit per day, summed over the stations. */
        private double sendableGbit(double outageShare) {
            double contactMinutes = 0;
            for (int i = 0; i < contactMinutesPerDay.length; i++) {
                contactMinutes += contactMinutesPerDay[i] * (i == outageStation ? 1 - outageShare : 1);
            }
            return downlinkMbps * contactMinutes * SECONDS_PER_MINUTE / MEGABITS_PER_GIGABIT * stepDays;
        }

        /**
         * Fixed order for limits that break in the same step. Lost data implies a backlog, so the
         * full recorder is named before the downlink that caused it.
         */
        private void noteFirstLimit(int step, double shortfallW, double lost, double backlog) {
            if (firstLimit != SimLimit.NONE) {
                return;
            }
            firstLimit = shortfallW > 0 ? SimLimit.POWER
                    : lost > 0 ? SimLimit.STORAGE
                    : backlog > 0 ? SimLimit.DOWNLINK
                    : suppliesRanOut ? SimLimit.SUPPLIES
                    : SimLimit.NONE;
            if (firstLimit != SimLimit.NONE) {
                firstLimitIndex = step;
            }
        }

        SimulationResult result() {
            boolean success = !suppliesRanOut && totalSent > 0;
            return new SimulationResult(mission.stepUnit(), timeline, events, totalProduced, totalSent, totalLost,
                    storedGbit, launchSlipMonths, firstLimit, firstLimitIndex, success);
        }
    }
}
