package com.liftoff.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftoff.model.Bus;
import com.liftoff.model.Catalog;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.SourceRef;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Reads the JSON catalogs and checks them. */
public final class CatalogLoader {

    private static final String CATALOG_FOLDER = "/com/liftoff/catalog/";

    /** Strict on purpose: a typo in a catalog file must stop the game, not silently become a zero. */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    private CatalogLoader() {
    }

    /** Loads and validates the six catalogs bundled with the game. */
    public static Catalog loadFromClasspath() {
        Catalog catalog = new Catalog(
                parseList(readResource("objectives.json"), Objective.class),
                parseList(readResource("instruments.json"), Instrument.class),
                parseList(readResource("buses.json"), Bus.class),
                parseList(readResource("orbits.json"), Orbit.class),
                parseList(readResource("ground-stations.json"), GroundStation.class),
                parseList(readResource("launch-vehicles.json"), LaunchVehicle.class));
        validate(catalog);
        return catalog;
    }

    /** Parses a JSON array into a list of catalog entries. Unknown or missing fields are an error. */
    public static <T> List<T> parseList(String json, Class<T> entryType) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, entryType));
        } catch (JsonProcessingException e) {
            throw new CatalogException("Cannot read " + entryType.getSimpleName() + " entries: "
                    + e.getOriginalMessage(), e);
        }
    }

    /** Checks the catalog rules: unique ids, a source on every entry, sensible physical values. */
    public static void validate(Catalog catalog) {
        checkEntries("objective", catalog.objectives(), Objective::id, Objective::source);
        checkEntries("instrument", catalog.instruments(), Instrument::id, Instrument::source);
        checkEntries("bus", catalog.buses(), Bus::id, Bus::source);
        checkEntries("orbit", catalog.orbits(), Orbit::id, Orbit::source);
        checkEntries("ground station", catalog.groundStations(), GroundStation::id, GroundStation::source);
        checkEntries("launch vehicle", catalog.launchVehicles(), LaunchVehicle::id, LaunchVehicle::source);

        for (Objective objective : catalog.objectives()) {
            String where = "objective " + objective.id();
            require(!objective.required().isEmpty(), where, "must require at least one measurement");
            require(objective.targetLatitudeDeg() >= 0 && objective.targetLatitudeDeg() <= 90, where,
                    "target latitude must be between 0 and 90");
            positive(objective.budgetCapMusd(), where, "budget cap");
            positive(objective.deadlineMonths(), where, "deadline");
            positive(objective.minScienceMonths(), where, "minimum science duration");
        }
        for (Instrument instrument : catalog.instruments()) {
            String where = "instrument " + instrument.id();
            positive(instrument.massKg(), where, "mass");
            positive(instrument.avgPowerW(), where, "power");
            positive(instrument.avgDataRateKbps(), where, "data rate");
            positive(instrument.buildMonths(), where, "build time");
            require(instrument.costMusd() >= 0, where, "cost must not be negative");
        }
        for (Bus bus : catalog.buses()) {
            String where = "bus " + bus.id();
            positive(bus.massKg(), where, "mass");
            positive(bus.payloadMassKg(), where, "payload mass");
            positive(bus.payloadPowerW(), where, "payload power");
            positive(bus.downlinkMbps(), where, "downlink rate");
            positive(bus.designLifeMonths(), where, "design life");
            positive(bus.buildMonths(), where, "build time");
            require(bus.costMusd() >= 0, where, "cost must not be negative");
        }
        for (Orbit orbit : catalog.orbits()) {
            String where = "orbit " + orbit.id();
            positive(orbit.altitudeKm(), where, "altitude");
            require(orbit.inclinationDeg() >= 0 && orbit.inclinationDeg() <= 180, where,
                    "inclination must be between 0 and 180");
            require(orbit.launchClass() != null, where, "must name a launch class");
        }
        for (GroundStation station : catalog.groundStations()) {
            String where = "ground station " + station.id();
            require(station.latitudeDeg() >= -90 && station.latitudeDeg() <= 90, where,
                    "latitude must be between -90 and 90");
            require(station.longitudeDeg() >= -180 && station.longitudeDeg() <= 180, where,
                    "longitude must be between -180 and 180");
            require(station.costMusdPerMonth() >= 0, where, "cost must not be negative");
        }
        for (LaunchVehicle rocket : catalog.launchVehicles()) {
            String where = "launch vehicle " + rocket.id();
            require(!rocket.capacityKg().isEmpty(), where, "must have at least one capacity figure");
            rocket.capacityKg().values().forEach(capacity -> positive(capacity, where, "capacity"));
            positive(rocket.costMusd(), where, "cost");
        }
    }

    private static <T> void checkEntries(String kind, List<T> entries, Function<T, String> idOf,
                                         Function<T, SourceRef> sourceOf) {
        Set<String> seen = new HashSet<>();
        for (T entry : entries) {
            String id = idOf.apply(entry);
            require(id != null && !id.isBlank(), kind, "every entry needs an id");
            String where = kind + " " + id;
            require(seen.add(id), where, "id is used more than once");
            SourceRef source = sourceOf.apply(entry);
            require(source != null, where, "has no source");
            require(source.title() != null && !source.title().isBlank(), where, "source has no title");
            require(source.url() != null && !source.url().isBlank(), where, "source has no link");
        }
    }

    private static void positive(double value, String where, String what) {
        require(value > 0, where, what + " must be greater than zero");
    }

    private static void require(boolean condition, String where, String problem) {
        if (!condition) {
            throw new CatalogException("Catalog problem in " + where + ": " + problem);
        }
    }

    private static String readResource(String fileName) {
        String path = CATALOG_FOLDER + fileName;
        try (InputStream stream = CatalogLoader.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new CatalogException("Catalog file not found: " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CatalogException("Cannot read catalog file " + path, e);
        }
    }
}
