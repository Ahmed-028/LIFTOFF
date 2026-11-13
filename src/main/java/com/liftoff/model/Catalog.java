package com.liftoff.model;

import com.liftoff.data.MissionEntry;
import com.liftoff.engine.sim.EventCard;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipPart;

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
        List<LaunchVehicle> launchVehicles,
        List<MissionEntry> missions,
        List<ShipPart> shipParts,
        List<EventCard> events) {

    public Catalog(List<Objective> objectives, List<Instrument> instruments, List<Bus> buses,
                   List<Orbit> orbits, List<GroundStation> groundStations, List<LaunchVehicle> launchVehicles) {
        this(objectives, instruments, buses, orbits, groundStations, launchVehicles,
                List.of(), List.of(), List.of());
    }

    public Catalog {
        objectives = List.copyOf(objectives);
        instruments = List.copyOf(instruments);
        buses = List.copyOf(buses);
        orbits = List.copyOf(orbits);
        groundStations = List.copyOf(groundStations);
        launchVehicles = List.copyOf(launchVehicles);
        missions = List.copyOf(missions);
        shipParts = List.copyOf(shipParts);
        events = List.copyOf(events);
    }

    public Optional<MissionEntry> mission(String id) {
        return find(missions, MissionEntry::id, id);
    }

    public Optional<ShipPart> shipPart(String id) {
        return find(shipParts, ShipPart::id, id);
    }

    public List<ShipPart> partsFor(String missionId, ModuleSlot slot) {
        return shipParts.stream()
                .filter(part -> part.slot() == slot && part.missions().contains(missionId))
                .toList();
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
