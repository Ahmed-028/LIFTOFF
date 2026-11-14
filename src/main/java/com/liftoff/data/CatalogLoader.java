package com.liftoff.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftoff.engine.sim.EventCard;
import com.liftoff.model.Bus;
import com.liftoff.model.Catalog;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.SourceRef;
import com.liftoff.session.stub.Mission;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipPart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


public final class CatalogLoader {

    private static final String CATALOG_FOLDER = "/com/liftoff/catalog/";

    // Reject malformed data instead of silently substituting zeroes.
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private CatalogLoader() {
    }


    public static Catalog loadFromClasspath() {
        List<MissionEntry> missions = parseList(readResource("missions.json"), MissionEntry.class);
        Catalog catalog = new Catalog(
                missions.stream().filter(mission -> mission.destination() == Mission.Destination.EARTH)
                        .map(MissionEntry::briefing).toList(),
                parseList(readResource("instruments.json"), Instrument.class),
                parseList(readResource("buses.json"), Bus.class),
                parseList(readResource("orbits.json"), Orbit.class),
                parseList(readResource("ground-stations.json"), GroundStation.class),
                parseList(readResource("launch-vehicles.json"), LaunchVehicle.class),
                missions,
                parseList(readResource("ship-parts.json"), ShipPart.class),
                parseList(readResource("events.json"), EventCard.class));
        require(!missions.isEmpty(), "missions.json", "must contain missions");
        require(!catalog.events().isEmpty(), "events.json", "must contain events");
        validate(catalog);
        return catalog;
    }


    public static <T> List<T> parseList(String json, Class<T> entryType) {
        try {
            List<T> entries = MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, entryType));
            require(entries != null && entries.stream().noneMatch(entry -> entry == null),
                    entryType.getSimpleName(), "expected an array of non-null entries");
            return entries;
        } catch (JsonProcessingException e) {
            throw new CatalogException("Cannot read " + entryType.getSimpleName() + " entries: "
                    + e.getOriginalMessage(), e);
        }
    }


    public static void validate(Catalog catalog) {
        checkEntries("objective", catalog.objectives(), Objective::id, Objective::source);
        checkEntries("instrument", catalog.instruments(), Instrument::id, Instrument::source);
        checkEntries("bus", catalog.buses(), Bus::id, Bus::source);
        checkEntries("orbit", catalog.orbits(), Orbit::id, Orbit::source);
        checkEntries("ground station", catalog.groundStations(), GroundStation::id, GroundStation::source);
        checkEntries("launch vehicle", catalog.launchVehicles(), LaunchVehicle::id, LaunchVehicle::source);

        if (catalog.missions().isEmpty()) {
            catalog.objectives().forEach(CatalogLoader::validateBriefing);
        }
        validateMissionCatalogs(catalog);
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
            if (bus.storageGbit() != null) {
                positive(bus.storageGbit(), where, "recorder capacity");
            }
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

    private static void validateBriefing(Objective objective) {
        String where = "briefing " + objective.id();
        require(!objective.required().isEmpty(), where, "must require at least one measurement");
        require(objective.targetLatitudeDeg() >= 0 && objective.targetLatitudeDeg() <= 90, where,
                "target latitude must be between 0 and 90");
        positive(objective.budgetCapMusd(), where, "budget cap");
        positive(objective.deadlineMonths(), where, "deadline");
        positive(objective.minScienceMonths(), where, "minimum science duration");
    }

    private static void validateMissionCatalogs(Catalog catalog) {
        checkEntries("mission", catalog.missions(), MissionEntry::id, MissionEntry::source);
        checkEntries("ship part", catalog.shipParts(), ShipPart::id, ShipPart::source);
        checkEntries("event", catalog.events(), EventCard::id, EventCard::source);

        for (ShipPart part : catalog.shipParts()) {
            String where = "ship part " + part.id();
            require(part.slot() != null, where, "must name a slot");
            require(part.modelId() != null && !part.modelId().isBlank(), where, "modelId must not be blank");
            require(part.missions() != null && !part.missions().isEmpty(), where, "must name at least one mission");
            for (String missionId : part.missions()) {
                require(catalog.mission(missionId).isPresent(), where, "unknown mission " + missionId);
            }
            nonnegative(part.massKg(), where, "mass");
            nonnegative(part.costMusd(), where, "cost");
            nonnegative(part.propellantCapacityKg(), where, "propellant capacity");
            nonnegative(part.powerDrawW(), where, "power draw");
            nonnegative(part.dataGbitPerDay(), where, "data rate");
            require(part.costBasis() != null, where, "must name a cost basis");
            switch (part.slot()) {
                case AIRFRAME -> positive(part.payloadMassKg(), where, "payload capacity");
                case THRUSTERS -> {
                    positive(part.thrustN(), where, "thrust");
                    positive(part.specificImpulseS(), where, "specific impulse");
                }
                case POWER -> positive(part.powerSupplyW(), where, "power supply");
                case CARGO_BAY -> {
                    positive(part.payloadMassKg(), where, "payload capacity");
                    positive(part.bayLengthM(), where, "bay length");
                    positive(part.bayDiameterM(), where, "bay diameter");
                }
                case SATELLITE -> {
                    positive(part.lengthM(), where, "payload length");
                    positive(part.diameterM(), where, "payload diameter");
                }
            }
        }

        for (MissionEntry mission : catalog.missions()) {
            String where = "mission " + mission.id();
            validateBriefing(mission.briefing());
            require(mission.difficulty() != null && mission.destination() != null && mission.stepUnit() != null,
                    where, "must name a difficulty, destination and step unit");
            require(mission.steps() >= 0 && mission.crew() >= 0, where, "steps and crew must not be negative");
            if (mission.crew() > 0) {
                positive(mission.suppliesKgPerCrewDay(), where, "crew supplies per day");
                checkSource(mission.suppliesSource(), where + " crew supplies");
            } else {
                require(mission.suppliesKgPerCrewDay() == 0, where, "robotic missions carry no crew supplies");
            }
            if (mission.deltaVRequiredMs() != null) {
                nonnegative(mission.deltaVRequiredMs(), where, "delta-v");
                require(mission.deltaVBasis() != null, where, "must name a delta-v basis");
                checkSource(mission.deltaVSource(), where + " delta-v");
            } else {
                require(mission.deltaVBasis() == null && mission.deltaVSource() == null, where,
                        "an unspecified delta-v must have no basis or source");
            }
            nonnegative(mission.surfaceDeltaVRequiredMs(), where, "surface delta-v");
            if (mission.surfaceDeltaVRequiredMs() > 0) {
                require(mission.deltaVRequiredMs() != null
                                && mission.surfaceDeltaVRequiredMs() <= mission.deltaVRequiredMs(),
                        where, "surface delta-v must fit within the mission delta-v");
            }
            if (mission.destination() != Mission.Destination.EARTH) {
                positive(mission.downlinkMbps(), where, "downlink rate");
                positive(mission.contactMinutesPerDay(), where, "contact minutes per day");
                checkSource(mission.downlinkSource(), where + " downlink");
            }
            for (ModuleSlot slot : ModuleSlot.values()) {
                require(catalog.partsFor(mission.id(), slot).size() >= 4, where,
                        slot + " must offer at least four parts");
            }
        }

        for (EventCard event : catalog.events()) {
            String where = "event " + event.id();
            require(event.type() != null && !event.destinations().isEmpty(), where,
                    "must name an event type and at least one destination");
            require(event.messageKey() != null && !event.messageKey().isBlank(), where, "needs a message key");
            require(event.realExample() != null && !event.realExample().isBlank(), where, "needs a real example");
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
            checkSource(sourceOf.apply(entry), where);
        }
    }

    private static void checkSource(SourceRef source, String where) {
        require(source != null, where, "has no source");
        require(source.title() != null && !source.title().isBlank(), where, "source has no title");
        require(source.url() != null && !source.url().isBlank(), where, "source has no link");
    }

    private static void positive(double value, String where, String what) {
        require(Double.isFinite(value) && value > 0, where, what + " must be finite and greater than zero");
    }

    private static void nonnegative(double value, String where, String what) {
        require(Double.isFinite(value) && value >= 0, where, what + " must be finite and nonnegative");
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
