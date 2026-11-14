package com.liftoff.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public record MissionDesign(
        Objective objective,
        Mission mission,
        Bus bus,
        ShipBuild build,
        List<Instrument> instruments,
        Orbit orbit,
        List<GroundStation> stations,
        LaunchVehicle launchVehicle,
        int scienceMonths) {

    public MissionDesign {
        Objects.requireNonNull(build, "build");
        instruments = List.copyOf(instruments);
        stations = List.copyOf(stations);
    }

    public static MissionDesign empty() {
        return new MissionDesign(null, null, null, ShipBuild.empty(), List.of(), null, List.of(), null, 0);
    }

    public MissionDesign withObjective(Objective newObjective) {
        return new MissionDesign(newObjective, mission, bus, build, instruments, orbit, stations,
                launchVehicle, scienceMonths);
    }

    public MissionDesign withMission(Mission newMission) {
        return new MissionDesign(objective, newMission, bus, build, instruments, orbit, stations,
                launchVehicle, scienceMonths);
    }

    public MissionDesign withBus(Bus newBus) {
        return new MissionDesign(objective, mission, newBus, build, instruments, orbit, stations,
                launchVehicle, scienceMonths);
    }


    public MissionDesign withBuild(ShipBuild newBuild) {
        return new MissionDesign(objective, mission, bus, newBuild, instruments, orbit, stations,
                launchVehicle, scienceMonths);
    }


    public MissionDesign withPart(ShipPart part) {
        return withBuild(build.with(part));
    }

    public MissionDesign withOrbit(Orbit newOrbit) {
        return new MissionDesign(objective, mission, bus, build, instruments, newOrbit, stations,
                launchVehicle, scienceMonths);
    }

    public MissionDesign withLaunchVehicle(LaunchVehicle newLaunchVehicle) {
        return new MissionDesign(objective, mission, bus, build, instruments, orbit, stations,
                newLaunchVehicle, scienceMonths);
    }

    public MissionDesign withScienceMonths(int months) {
        return new MissionDesign(objective, mission, bus, build, instruments, orbit, stations,
                launchVehicle, months);
    }


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
        return new MissionDesign(objective, mission, bus, build, newInstruments, orbit, stations,
                launchVehicle, scienceMonths);
    }

    private MissionDesign withStations(List<GroundStation> newStations) {
        return new MissionDesign(objective, mission, bus, build, instruments, orbit, newStations,
                launchVehicle, scienceMonths);
    }
}
