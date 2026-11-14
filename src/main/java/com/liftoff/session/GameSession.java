package com.liftoff.session;

import com.liftoff.data.MissionEntry;
import com.liftoff.engine.DesignIssue;
import com.liftoff.engine.DesignValidator;
import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.engine.score.Report;
import com.liftoff.engine.score.ReportBuilder;
import com.liftoff.engine.score.Score;
import com.liftoff.engine.score.ScoreCalculator;
import com.liftoff.engine.sim.Difficulty;
import com.liftoff.engine.sim.EventDeck;
import com.liftoff.engine.sim.LaunchChecker;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.engine.sim.MissionSimulator;
import com.liftoff.engine.sim.SimExtras;
import com.liftoff.engine.sim.SimRules;
import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.model.Bus;
import com.liftoff.model.Catalog;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.session.stub.BuildCalculator;
import com.liftoff.session.stub.BuildIssue;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.Mission;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipBuild;
import com.liftoff.session.stub.ShipPart;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

/**
 * The single door between the user interface and the game logic. Holds the catalog and the
 * current design, recalculates the resources after every change and tells listeners.
 */
public final class GameSession {

    /** Message keys of the reasons a launch or flight is refused. */
    public static final String NO_MISSION = "flow.noMissionSelected";
    public static final String ALREADY_FLOWN = "flow.alreadyFlown";

    private final Catalog catalog;
    private final List<Runnable> listeners = new ArrayList<>();
    private MissionDesign design = MissionDesign.empty();
    private ResourceReport resources = ResourceCalculator.calculate(design);

    private final Function<String, Optional<ShipPart>> partLookup;
    private Mission mission;
    private MissionEntry missionEntry;
    private double crewSuppliesKg;
    private ShipBuild build = ShipBuild.empty();
    private BuildReport buildReport = BuildCalculator.calculate(build, null);
    private MissionFlow flow = newFlow();
    private final LevelPath levels = new LevelPath(step -> flow.isUnlocked(step), () -> flow.current());

    private final MissionLog log;
    private Difficulty difficulty = Difficulty.CADET;
    private List<LaunchIssue> launchIssues = List.of();
    private SimulationResult simulation;
    private Score score;
    private Report report;

    public GameSession(Catalog catalog) {
        this(catalog, catalog::shipPart);
    }

    /** @param partLookup alternate source of ship parts, used by test scenarios */
    public GameSession(Catalog catalog, Function<String, Optional<ShipPart>> partLookup) {
        this(catalog, partLookup, MissionLog.inMemory());
    }

    /**
     * @param log where finished missions are recorded; the game passes {@code new MissionLog()} to keep
     *            them in the user's home folder, the shorter constructors keep them in memory only
     */
    public GameSession(Catalog catalog, Function<String, Optional<ShipPart>> partLookup, MissionLog log) {
        this.catalog = catalog;
        this.partLookup = partLookup;
        this.log = log;
    }

    public Catalog catalog() {
        return catalog;
    }

    public MissionDesign design() {
        return design;
    }

    public ResourceReport resources() {
        return resources;
    }

    /** Combined limits for the modular builder and review screens. */
    public ResourceReport missionResources() {
        var combined = new EnumMap<ResourceType, ResourceUsage>(ResourceType.class);
        combined.putAll(resources.usages());
        var rocket = design.launchVehicle();
        var launchClass = mission == null ? null : switch (mission.destination()) {
            case EARTH -> design.orbit() == null ? null : design.orbit().launchClass();
            case MOON -> LaunchClass.TRANS_LUNAR;
            case MARS -> LaunchClass.TRANS_MARS;
        };
        combined.put(ResourceType.MASS, ResourceUsage.of(ResourceType.MASS,
                resources.get(ResourceType.MASS).used() + buildReport.totalMassKg(),
                rocket == null || launchClass == null ? OptionalDouble.empty() : rocket.capacityFor(launchClass)));
        var budget = resources.get(ResourceType.BUDGET);
        combined.put(ResourceType.BUDGET, ResourceUsage.of(ResourceType.BUDGET,
                budget.used() + buildReport.costMusd(), budget.capacity()));
        if (build.part(ModuleSlot.POWER).isPresent()) {
            combined.put(ResourceType.POWER, ResourceUsage.of(ResourceType.POWER,
                    buildReport.powerDrawW(), OptionalDouble.of(buildReport.powerSupplyW())));
        }
        if (usesModularRadio()) {
            var link = flightLink();
            double moduleData = build.parts().values().stream().mapToDouble(ShipPart::dataGbitPerDay).sum();
            combined.putAll(ResourceCalculator.communication(design, link.downlinkMbps(), link.contactMinutesPerDay(), moduleData));
        }
        return new ResourceReport(combined);
    }

    public List<DesignIssue> designIssues() {
        return DesignValidator.validate(design);
    }

    /** Chooses the mission goal and starts the science duration at the goal's minimum. */
    public void selectObjective(String objectiveId) {
        Objective objective = found(catalog.objective(objectiveId), "objective", objectiveId);
        update(design.withObjective(objective).withScienceMonths(objective.minScienceMonths()));
    }

    public void chooseBus(String busId) {
        update(design.withBus(found(catalog.bus(busId), "bus", busId)));
    }

    public void addInstrument(String instrumentId) {
        update(design.plusInstrument(found(catalog.instrument(instrumentId), "instrument", instrumentId)));
    }

    public void removeInstrument(String instrumentId) {
        update(design.minusInstrument(instrumentId));
    }

    public void chooseOrbit(String orbitId) {
        update(design.withOrbit(found(catalog.orbit(orbitId), "orbit", orbitId)));
    }

    /** Adds the station if the design does not use it yet, removes it if it does. */
    public void toggleStation(String stationId) {
        GroundStation station = found(catalog.groundStation(stationId), "ground station", stationId);
        update(design.hasStation(stationId) ? design.minusStation(stationId) : design.plusStation(station));
    }

    public void chooseLaunchVehicle(String launchVehicleId) {
        update(design.withLaunchVehicle(
                found(catalog.launchVehicle(launchVehicleId), "launch vehicle", launchVehicleId)));
    }

    public void setScienceMonths(int months) {
        if (months < 0) {
            throw new IllegalArgumentException("Science duration cannot be negative: " + months);
        }
        update(design.withScienceMonths(months));
    }

    /** Registers a callback run after every change to the design. */
    public void addListener(Runnable onChange) {
        listeners.add(onChange);
    }

    /** Stops a callback registered with {@link #addListener}; used when a screen is closed. */
    public void removeListener(Runnable onChange) {
        listeners.remove(onChange);
    }

    // ---- ship build ----

    public ShipBuild build() {
        return build;
    }

    public BuildReport buildReport() {
        return buildReport;
    }

    /** Puts the part in its own slot and keeps the other slots; un-confirms the build. */
    public void choosePart(String partId) {
        ShipPart part = found(partLookup.apply(partId), "ship part", partId);
        String missionId = mission == null
                ? (design.objective() == null ? null : design.objective().id())
                : mission.id();
        if (missionId != null && !part.missions().isEmpty() && !part.missions().contains(missionId)) {
            throw new IllegalArgumentException("Ship part " + partId + " is unavailable for " + missionId);
        }
        build = build.with(part);
        refreshBuildReport();
        flow.partsChanged();
        clearResults();
        notifyListeners();
    }

    /** @throws IllegalStateException unless {@code buildReport().canConfirm()} and the review step is open */
    public void confirmBuild() {
        if (!buildReport.canConfirm()) {
            throw new IllegalStateException("The build still has issues: " + buildReport.issues());
        }
        flow.confirmBuild();
        notifyListeners();
    }

    // ---- mission flow ----

    /** Starts the mission from its briefing with an empty design and an empty ship. */
    public void selectMission(String missionId) {
        MissionEntry entry = catalog.mission(missionId).orElse(null);
        Objective objective;
        if (entry == null) {
            objective = found(catalog.objective(missionId), "mission", missionId);
            mission = new Mission(objective.id(), 0);
        } else {
            objective = entry.briefing();
            mission = new Mission(entry.id(), entry.destination(), entry.stepUnit(), entry.steps(),
                    entry.deltaVRequiredMs() == null ? 0 : entry.deltaVRequiredMs(),
                    entry.crew(), entry.suppliesKgPerCrewDay(), entry.surfaceDeltaVRequiredMs());
            difficulty = entry.difficulty();
        }
        missionEntry = entry;
        crewSuppliesKg = 0;
        design = MissionDesign.empty().withObjective(objective).withScienceMonths(objective.minScienceMonths());
        resources = ResourceCalculator.calculate(design);
        build = ShipBuild.empty();
        refreshBuildReport();
        flow = newFlow();
        levels.reset();
        clearResults();
        notifyListeners();
    }

    /**
     * True once the last mission of the main ladder (Mars, after Farm Watch and the Moon) has been
     * flown successfully: the moment for the game's ending.
     */
    public boolean finishedFinalMission() {
        if (missionEntry == null || simulation == null || !simulation.success()) {
            return false;
        }
        var ladder = catalog.missions().stream().filter(MissionEntry::ladder).toList();
        return !ladder.isEmpty() && ladder.getLast().id().equals(missionEntry.id());
    }

    /** The road map of the current mission: its five levels and where the ship is. */
    public LevelPath levels() {
        return levels;
    }

    /** Marks a mini-game level as played (or skipped). Mini-games never change the design. */
    public void finishMiniGame(Level game) {
        levels.finishGame(game);
        notifyListeners();
    }

    public MissionStep step() {
        return flow.current();
    }

    public double crewSuppliesKg() {
        return crewSuppliesKg;
    }

    public double requiredCrewSuppliesKg() {
        if (mission == null) {
            return 0;
        }
        double stepDays = mission.stepUnit() == Mission.StepUnit.DAY ? 1 : SimRules.DAYS_PER_MONTH;
        int steps = mission.steps() > 0 ? mission.steps() : design.scienceMonths();
        return steps * (mission.crew() * mission.suppliesKgPerCrewDay() * stepDays);
    }

    public void setCrewSuppliesKg(double massKg) {
        if (!Double.isFinite(massKg) || massKg < 0) {
            throw new IllegalArgumentException("Crew supplies must be a finite, nonnegative mass");
        }
        requireOpen(MissionStep.SUPPLIES);
        crewSuppliesKg = massKg;
        update(design);
    }

    /** @throws IllegalStateException with the reason's message key if the step is locked */
    public void goTo(MissionStep step) {
        moved(flow.current(), flow.goTo(step));
    }

    public void next() {
        moved(flow.current(), flow.next());
    }

    public void back() {
        moved(flow.current(), flow.back());
    }

    public boolean isUnlocked(MissionStep step) {
        return flow.isUnlocked(step);
    }

    /** Message key for the interface text; empty if the step is open. */
    public Optional<String> reasonLocked(MissionStep step) {
        return flow.reasonLocked(step);
    }

    /** Called when the launch check finds no reason to stop. */
    public void markLaunchPassed() {
        flow.markLaunchPassed();
        notifyListeners();
    }

    /** Called when the simulation has run to its end. */
    public void markSimulationFinished() {
        flow.markSimulationFinished();
        notifyListeners();
    }

    /** From the report back to the ship step, keeping the design. */
    public void redesign() {
        flow.redesign();
        clearResults();
        notifyListeners();
    }

    // ---- launch, flight, score and report ----

    public Difficulty difficulty() {
        return difficulty;
    }

    /** Sets how many random events the flight meets. Mission 1 is flown at CADET: none. */
    public void setDifficulty(Difficulty newDifficulty) {
        difficulty = newDifficulty;
        notifyListeners();
    }

    /**
     * Runs the launch check on the confirmed build. With no issues the Fly step opens; otherwise
     * {@link #report()} explains the first one and the design has to change.
     *
     * @throws IllegalStateException if no mission is selected or the Launch step is still locked
     */
    public List<LaunchIssue> runLaunchCheck() {
        requireOpen(MissionStep.LAUNCH);
        launchIssues = LaunchChecker.check(design, buildReport, mission, design.launchVehicle(), design.orbit());
        if (launchIssues.isEmpty()) {
            flow.markLaunchPassed();
            report = null;
        } else {
            report = ReportBuilder.build(design, resources, launchIssues, null, mission);
        }
        notifyListeners();
        return launchIssues;
    }

    /**
     * Flies the mission once: simulates it, scores it, writes the report, opens the Report step and
     * records the mission in the log. The same seed replays the same flight.
     *
     * @throws IllegalStateException if the launch check has not passed, or the mission was already flown
     */
    public SimulationResult fly(long seed) {
        requireOpen(MissionStep.FLY);
        if (simulation != null) {
            throw new IllegalStateException(ALREADY_FLOWN);
        }
        if (usesModularRadio()) {
            boolean solarPowered = build.part(ModuleSlot.POWER).map(ShipPart::modelId)
                    .filter(id -> id.equals("body-solar") || id.equals("deployable-solar")).isPresent();
            double storage = radioBus().map(Bus::storageGbit).orElse(Double.POSITIVE_INFINITY);
            simulation = MissionSimulator.run(design, mission, difficulty, seed, new EventDeck(catalog.events()),
                    new SimExtras(storage, crewSuppliesKg, solarPowered), flightLink());
        } else {
            simulation = MissionSimulator.run(design, mission, difficulty, seed);
        }
        ResourceReport flightResources = usesModularRadio() ? missionResources() : resources;
        score = usesModularRadio()
                ? ScoreCalculator.scoreCombined(design, flightResources, buildReport, simulation, mission)
                : ScoreCalculator.score(design, flightResources, buildReport, simulation, mission);
        report = ReportBuilder.build(design, flightResources, List.of(), simulation, mission);
        flow.markSimulationFinished();
        log.add(new MissionLog.Entry(mission.id(), difficulty, score.total(), score.stars(),
                Instant.now().toString()));
        notifyListeners();
        return simulation;
    }

    /** What the last launch check found; empty when it passed or has not run. */
    public List<LaunchIssue> launchIssues() {
        return launchIssues;
    }

    /** The flight, once flown. */
    public Optional<SimulationResult> simulation() {
        return Optional.ofNullable(simulation);
    }

    /** The score, once flown. */
    public Optional<Score> score() {
        return Optional.ofNullable(score);
    }

    /** The report: after a flight, or after a launch check that failed. */
    public Optional<Report> report() {
        return Optional.ofNullable(report);
    }

    public MissionLog missionLog() {
        return log;
    }

    private void requireOpen(MissionStep step) {
        if (mission == null) {
            throw new IllegalStateException(NO_MISSION);
        }
        Optional<String> reason = flow.reasonLocked(step);
        if (reason.isPresent()) {
            throw new IllegalStateException(reason.get());
        }
    }

    private void clearResults() {
        launchIssues = List.of();
        simulation = null;
        score = null;
        report = null;
    }

    private MissionFlow newFlow() {
        return new MissionFlow(() -> build.isComplete());
    }

    private void moved(MissionStep from, MissionStep to) {
        if (from != to) {
            notifyListeners();
        }
    }

    private void update(MissionDesign newDesign) {
        design = newDesign;
        resources = ResourceCalculator.calculate(newDesign);
        refreshBuildReport();
        flow.partsChanged();
        clearResults();
        notifyListeners();
    }

    private void refreshBuildReport() {
        buildReport = reportFor(build);
    }

    /** The build report the given ship would have with the rest of the current design. */
    private BuildReport reportFor(ShipBuild ship) {
        if (missionEntry == null || design.bus() != null) {
            return BuildCalculator.calculate(ship, mission);
        }
        var report = BuildCalculator.calculate(ship, mission,
                design.instruments().stream().mapToDouble(Instrument::massKg).sum(),
                design.instruments().stream().mapToDouble(Instrument::avgPowerW).sum(),
                crewSuppliesKg, requiredCrewSuppliesKg());
        var budget = resources.get(ResourceType.BUDGET);
        double cap = missionEntry.briefing().budgetCapMusd();
        double totalCost = report.costMusd() + budget.used();
        if (totalCost > cap) {
            var issues = new ArrayList<>(report.issues());
            issues.add(new BuildIssue(BuildIssue.Kind.OVER_BUDGET, null, totalCost, cap));
            report = new BuildReport(report.dryMassKg(), report.propellantKg(), report.totalMassKg(),
                    report.costMusd(), report.powerSupplyW(), report.powerDrawW(), report.deltaVAvailableMs(),
                    report.deltaVRequiredMs(), issues);
        }
        return report;
    }

    /**
     * Part swaps that would bring an over-limit gauge back down, best first. Empty when the
     * resource is within its limit, has no limit yet, or no mission is chosen.
     */
    public List<SwapAdvisor.Swap> limitTips(SwapAdvisor.Goal goal) {
        if (missionEntry == null) {
            return List.of();
        }
        double offset = 0;
        double limit = 0;
        var combined = missionResources();
        switch (goal) {
            case MASS -> {
                var mass = combined.get(ResourceType.MASS);
                if (mass.capacity().isEmpty()) {
                    return List.of();
                }
                offset = mass.used() - buildReport.totalMassKg();
                limit = mass.capacity().getAsDouble();
            }
            case BUDGET -> {
                var budget = combined.get(ResourceType.BUDGET);
                offset = budget.used() - buildReport.costMusd();
                limit = budget.capacity().orElse(missionEntry.briefing().budgetCapMusd());
            }
            default -> {
            }
        }
        var options = new EnumMap<ModuleSlot, List<ShipPart>>(ModuleSlot.class);
        for (var slot : ModuleSlot.values()) {
            options.put(slot, catalog.partsFor(missionEntry.id(), slot));
        }
        return SwapAdvisor.suggest(goal, build, options, this::reportFor, offset, limit, 3);
    }

    private boolean usesModularRadio() {
        return missionEntry != null && design.bus() == null;
    }

    private Optional<Bus> radioBus() {
        if (missionEntry.destination() != Mission.Destination.EARTH) {
            return Optional.empty();
        }
        return build.part(ModuleSlot.AIRFRAME).flatMap(part -> catalog.bus(part.id()));
    }

    private MissionSimulator.DeepSpaceLink flightLink() {
        boolean earth = missionEntry.destination() == Mission.Destination.EARTH;
        double rate = earth ? radioBus().map(Bus::downlinkMbps).orElse(0.0) : missionEntry.downlinkMbps();
        var contact = earth ? OptionalDouble.empty() : OptionalDouble.of(missionEntry.contactMinutesPerDay());
        return new MissionSimulator.DeepSpaceLink(Math.max(0, buildReport.powerSupplyW() - powerDrawByShip()),
                rate, contact);
    }

    private double powerDrawByShip() {
        return build.parts().values().stream().mapToDouble(ShipPart::powerDrawW).sum();
    }

    private void notifyListeners() {
        List.copyOf(listeners).forEach(Runnable::run);
    }

    private static <T> T found(Optional<T> entry, String kind, String id) {
        return entry.orElseThrow(() -> new IllegalArgumentException("Unknown " + kind + ": " + id));
    }
}
