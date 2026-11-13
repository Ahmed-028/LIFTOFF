# LIFTOFF — Architecture

## 1. Stack

Java 21 (language level; built with `--release 21`), JavaFX 21 with FXML and
JavaFX CSS, Maven (through the Maven Wrapper), Jackson for JSON, JUnit 5.
No other framework and no dependency-injection container.

## 2. Layers and the one rule

```
            +------------------------------------------+
  JavaFX -> |  app      window, navigation             |
            |  ui       FXML controllers, components   |
            |  render   vector art (shapes, paths)     |
            +---------------------+--------------------+
                                  | calls / listens
            +---------------------v--------------------+
  no JavaFX |  session  GameSession (the single door)  |
            |  engine   rules and calculations         |
            |  data     JSON catalog loading           |
            |  model    immutable records and enums    |
            |  util     small helpers                  |
            +------------------------------------------+
```

**The rule:** `model`, `engine`, `data`, `session` and `util` never import
`javafx.*`. A unit test (`ArchitectureTest`) scans those packages and fails the
build if one does. This keeps every game rule testable without a window.

Dependency direction is downward only: `ui → session → engine → model`,
`data → model`. `model` depends on nothing.

## 3. Folder structure

```
pom.xml, mvnw, mvnw.cmd, .mvn/
docs/                              design documents
src/main/java/com/liftoff/
  Launcher.java                    plain main(), starts the JavaFX app
  app/      LiftoffApp, SceneNavigator, Screen
  model/    Objective, Instrument, Bus, Orbit, GroundStation, LaunchVehicle,
            MissionDesign, Catalog, MeasurementType, LaunchClass, ValueBasis,
            SourceRef, Capability
  data/     CatalogLoader, CatalogException
  engine/   OrbitMath, ResourceCalculator, ResourceReport, ResourceUsage,
            ResourceType, ResourceLevel, GameRules
  session/  GameSession
  ui/       one controller per screen, reusable components
  render/   RocketArt, PlanetArt, ...
  util/     Fmt
src/main/resources/com/liftoff/
  catalog/  objectives.json, instruments.json, buses.json, orbits.json,
            ground-stations.json, launch-vehicles.json
  ui/       *.fxml
  css/      liftoff.css
  i18n/     messages.properties
src/test/java/com/liftoff/...      mirrors main
```

## 4. Module interfaces

**model** — plain immutable records. `MissionDesign` is the player's current
design and is changed only by producing a new copy:

```java
record MissionDesign(Objective objective, Bus bus, List<Instrument> instruments,
                     Orbit orbit, List<GroundStation> stations,
                     LaunchVehicle launchVehicle, int scienceMonths) {
    static MissionDesign empty();
    MissionDesign withObjective(Objective o);   // and withBus, withOrbit, ...
    MissionDesign plusInstrument(Instrument i); // minusInstrument, plusStation, ...
}
```

**data** — reads the JSON catalogs once and validates them:

```java
final class CatalogLoader {
    static Catalog loadFromClasspath();          // throws CatalogException
}
record Catalog(List<Objective> objectives, List<Instrument> instruments, ...) {
    Optional<Instrument> instrument(String id);  // lookups by id
}
```

**engine** — pure functions from a design to results:

```java
final class OrbitMath {
    static double periodMinutes(double altitudeKm);
    static double maxLatitudeDeg(double inclinationDeg);
    static double contactMinutesPerDay(Orbit orbit, double stationLatitudeDeg);
}
final class ResourceCalculator {
    static ResourceReport calculate(MissionDesign design);
}
record ResourceUsage(ResourceType type, double used, OptionalDouble capacity,
                     ResourceLevel level);      // OK, TIGHT, OVER, UNKNOWN
```

Arriving in later milestones, same style: `DesignValidator` (M2),
`LaunchChecker`, `MissionSimulator` (M3), `ScoreCalculator`, `ReportBuilder` (M4).

**session** — the only object the UI talks to:

```java
final class GameSession {
    GameSession(Catalog catalog);
    Catalog catalog();
    MissionDesign design();
    ResourceReport resources();
    void selectObjective(String id);  void chooseBus(String id);
    void addInstrument(String id);    void removeInstrument(String id);
    void chooseOrbit(String id);      void toggleStation(String id);
    void chooseLaunchVehicle(String id);  void setScienceMonths(int months);
    void addListener(Runnable onChange);  // UI refreshes itself
}
```

Controllers call a `GameSession` method, the session swaps in a new
`MissionDesign`, recomputes the `ResourceReport`, and notifies listeners.
Controllers never compute game rules themselves.

**app / ui / render** — `SceneNavigator.show(Screen)` loads an FXML file and
hands its controller the shared `GameSession`. `render` classes return JavaFX
`Node`s and know nothing about game rules; they are given plain values.

## 5. Data

All gameplay numbers live in JSON under `resources/com/liftoff/catalog/`.
Every entry carries a `source` and every non-published number a `basis`:

```json
{
  "id": "modis",
  "name": "MODIS",
  "kidName": "Wide-Eye Imager",
  "massKg": 229,
  "avgPowerW": 162.5,
  "avgDataRateKbps": 6200,
  "capabilities": [{ "type": "LAND_IMAGING", "resolutionM": 250 }],
  "costMusd": 114.5,  "costBasis": "GAME_ESTIMATE",
  "buildMonths": 30,  "buildBasis": "GAME_ESTIMATE",
  "source": { "title": "NASA Earth Science Reference Handbook (2006)",
              "detail": "Key MODIS Facts, PDF p. 98", "url": "..." }
}
```

`ValueBasis` is `PUBLISHED` (taken directly from the source), `DERIVED`
(computed from published numbers, with the sum shown in `data-sources.md`), or
`GAME_ESTIMATE` (ours, shown with "≈" in the game). Loading fails loudly on an
unknown field, a missing source, or a non-positive physical value.

Tunable rules that are not catalog data (the 15 % margin warning, the 5°
minimum elevation, integration time) sit together in `GameRules`, each with a
comment saying where it comes from.

## 6. Engine formulas (first-order, the level a concept study uses)

- Orbit period: `T = 2π √(a³/μ)`, `a = Rₑ + h`, `μ = 398 600.4418 km³/s²`,
  `Rₑ = 6378.137 km`. Check: 705 km gives 98.8 min, the handbook's figure for
  Aqua.
- Highest latitude overflown: `i` if `i ≤ 90°`, else `180° − i`.
- Ground-station view angle: `λ = arccos(Rₑ cos ε / (Rₑ + h)) − ε`, `ε = 5°`.
- Contact time per day: the long-run fraction of time the sub-satellite point
  lies within `λ` of the station, averaged numerically around the orbit, times
  1440 minutes. Contact from several stations is summed (a small overcount when
  two stations see the spacecraft at once; acceptable at this level and noted
  in the Sources screen).
- Daily data produced: `Σ average data rate × 86 400 s`.
- Daily data returned: `downlink rate × contact minutes × 60`.

## 7. Testing

- Engine and model: JUnit 5 unit tests written before the code, including
  checks against published numbers (orbit periods, Aqua's daily data volume).
- Data: every catalog loads; every entry has a source; ids are unique.
- Architecture: the no-JavaFX rule above.
- UI: kept thin so it needs little testing; each milestone ends with the app
  run by hand and screenshots of each screen checked.

## 8. Deliberately not done

No module-info (classpath app keeps Jackson and FXML reflection simple), no
event bus, no persistence layer, no plugin system, no 3D. Each can be added
when a milestone needs it.
