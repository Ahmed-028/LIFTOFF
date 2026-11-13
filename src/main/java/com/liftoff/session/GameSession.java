package com.liftoff.session;

import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.ResourceReport;
import com.liftoff.model.Catalog;
import com.liftoff.model.GroundStation;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The single door between the user interface and the game logic. Holds the catalog and the
 * current design, recalculates the resources after every change and tells listeners.
 */
public final class GameSession {

    private final Catalog catalog;
    private final List<Runnable> listeners = new ArrayList<>();
    private MissionDesign design = MissionDesign.empty();
    private ResourceReport resources = ResourceCalculator.calculate(design);

    public GameSession(Catalog catalog) {
        this.catalog = catalog;
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

    private void update(MissionDesign newDesign) {
        design = newDesign;
        resources = ResourceCalculator.calculate(newDesign);
        List.copyOf(listeners).forEach(Runnable::run);
    }

    private static <T> T found(Optional<T> entry, String kind, String id) {
        return entry.orElseThrow(() -> new IllegalArgumentException("Unknown " + kind + ": " + id));
    }
}
