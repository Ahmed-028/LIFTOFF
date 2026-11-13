package com.liftoff.session;

import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.ResourceReport;
import com.liftoff.model.Catalog;
import com.liftoff.model.GroundStation;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.session.stub.BuildCalculator;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.Mission;
import com.liftoff.session.stub.ShipBuild;
import com.liftoff.session.stub.ShipPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The single door between the user interface and the game logic. Holds the catalog and the
 * current design, recalculates the resources after every change and tells listeners.
 */
public final class GameSession {

    private final Catalog catalog;
    private final List<Runnable> listeners = new ArrayList<>();
    private MissionDesign design = MissionDesign.empty();
    private ResourceReport resources = ResourceCalculator.calculate(design);

    private final Function<String, Optional<ShipPart>> partLookup;
    private Mission mission;
    private ShipBuild build = ShipBuild.empty();
    private BuildReport buildReport = BuildCalculator.calculate(build, null);
    private MissionFlow flow = newFlow();

    public GameSession(Catalog catalog) {
        // TODO(Member 1/2): look parts up in the catalog once it carries ship parts.
        this(catalog, partId -> Optional.empty());
    }

    /** @param partLookup finds a ship part by id; stands in for the catalog until it carries parts */
    public GameSession(Catalog catalog, Function<String, Optional<ShipPart>> partLookup) {
        this.catalog = catalog;
        this.partLookup = partLookup;
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
        build = build.with(part);
        buildReport = BuildCalculator.calculate(build, mission);
        flow.partsChanged();
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
        Objective objective = found(catalog.objective(missionId), "mission", missionId);
        // TODO(Member 2): use the real Mission and its deltaVRequiredMs once model/Mission is on main;
        // zero here means "not known yet", it is not a physical value.
        mission = new Mission(objective.id(), 0);
        design = MissionDesign.empty().withObjective(objective).withScienceMonths(objective.minScienceMonths());
        resources = ResourceCalculator.calculate(design);
        build = ShipBuild.empty();
        buildReport = BuildCalculator.calculate(build, mission);
        flow = newFlow();
        notifyListeners();
    }

    public MissionStep step() {
        return flow.current();
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
        notifyListeners();
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
        flow.partsChanged();
        notifyListeners();
    }

    private void notifyListeners() {
        List.copyOf(listeners).forEach(Runnable::run);
    }

    private static <T> T found(Optional<T> entry, String kind, String id) {
        return entry.orElseThrow(() -> new IllegalArgumentException("Unknown " + kind + ": " + id));
    }
}
