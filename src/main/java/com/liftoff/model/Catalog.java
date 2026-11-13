package com.liftoff.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Everything the player can choose from. Loaded once from the JSON catalogs. */
public record Catalog(
        List<Objective> objectives,
        List<Instrument> instruments,
        List<Bus> buses,
        List<Orbit> orbits,
        List<GroundStation> groundStations,
        List<LaunchVehicle> launchVehicles) {

    public Catalog {
        objectives = List.copyOf(objectives);
        instruments = List.copyOf(instruments);
        buses = List.copyOf(buses);
        orbits = List.copyOf(orbits);
        groundStations = List.copyOf(groundStations);
        launchVehicles = List.copyOf(launchVehicles);
    }

    public Optional<Objective> objective(String id) {
        return find(objectives, Objective::id, id);
    }

    public Optional<Instrument> instrument(String id) {
        return find(instruments, Instrument::id, id);
    }

    public Optional<Bus> bus(String id) {
        return find(buses, Bus::id, id);
    }

    public Optional<Orbit> orbit(String id) {
        return find(orbits, Orbit::id, id);
    }

    public Optional<GroundStation> groundStation(String id) {
        return find(groundStations, GroundStation::id, id);
    }

    public Optional<LaunchVehicle> launchVehicle(String id) {
        return find(launchVehicles, LaunchVehicle::id, id);
    }

    private static <T> Optional<T> find(List<T> entries, Function<T, String> idOf, String id) {
        return entries.stream().filter(entry -> idOf.apply(entry).equals(id)).findFirst();
    }
}
