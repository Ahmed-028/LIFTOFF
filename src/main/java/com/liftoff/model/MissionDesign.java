package com.liftoff.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's current design. Immutable: every change returns a new design.
 * Any part may be null (or the lists empty) while the design is unfinished.
 */
public record MissionDesign(
        Objective objective,
        Bus bus,
        List<Instrument> instruments,
        Orbit orbit,
        List<GroundStation> stations,
        LaunchVehicle launchVehicle,
        int scienceMonths) {

    public MissionDesign {
        instruments = List.copyOf(instruments);
        stations = List.copyOf(stations);
    }

    public static MissionDesign empty() {
        return new MissionDesign(null, null, List.of(), null, List.of(), null, 0);
    }

    public MissionDesign withObjective(Objective newObjective) {
        return new MissionDesign(newObjective, bus, instruments, orbit, stations, launchVehicle, scienceMonths);
    }

    public MissionDesign withBus(Bus newBus) {
        return new MissionDesign(objective, newBus, instruments, orbit, stations, launchVehicle, scienceMonths);
    }

    public MissionDesign withOrbit(Orbit newOrbit) {
        return new MissionDesign(objective, bus, instruments, newOrbit, stations, launchVehicle, scienceMonths);
    }

    public MissionDesign withLaunchVehicle(LaunchVehicle newLaunchVehicle) {
        return new MissionDesign(objective, bus, instruments, orbit, stations, newLaunchVehicle, scienceMonths);
    }

    public MissionDesign withScienceMonths(int months) {
        return new MissionDesign(objective, bus, instruments, orbit, stations, launchVehicle, months);
    }

    /** Adds the instrument; a design carries at most one of each. */
    public MissionDesign plusInstrument(Instrument instrument) {
        if (hasInstrument(instrument.id())) {
            return this;
        }
        List<Instrument> added = new ArrayList<>(instruments);
        added.add(instrument);
        return withInstruments(added);
    }

    public MissionDesign minusInstrument(String instrumentId) {
        return withInstruments(instruments.stream()
                .filter(instrument -> !instrument.id().equals(instrumentId))
                .toList());
    }

    public MissionDesign plusStation(GroundStation station) {
        if (hasStation(station.id())) {
            return this;
        }
        List<GroundStation> added = new ArrayList<>(stations);
        added.add(station);
        return withStations(added);
    }

    public MissionDesign minusStation(String stationId) {
        return withStations(stations.stream()
                .filter(station -> !station.id().equals(stationId))
                .toList());
    }

    public boolean hasInstrument(String instrumentId) {
        return instruments.stream().anyMatch(instrument -> instrument.id().equals(instrumentId));
    }

    public boolean hasStation(String stationId) {
        return stations.stream().anyMatch(station -> station.id().equals(stationId));
    }

    private MissionDesign withInstruments(List<Instrument> newInstruments) {
        return new MissionDesign(objective, bus, newInstruments, orbit, stations, launchVehicle, scienceMonths);
    }

    private MissionDesign withStations(List<GroundStation> newStations) {
        return new MissionDesign(objective, bus, instruments, orbit, newStations, launchVehicle, scienceMonths);
    }
}
