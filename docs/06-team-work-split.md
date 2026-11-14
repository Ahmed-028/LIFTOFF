# LIFTOFF — Work Split for Five Members

Read `05-missions-and-ship-builder.md` first. This document says who owns what,
which classes each member writes, what each class does, and what data each
member needs.

## 0. How five people work at once without colliding

1. **One owner per folder.** A member only edits files in the folders they own.
   A change needed in someone else's folder is a request to that owner.
2. **Contracts first.** Section 2 lists the few types that cross between
   members. Member 2 commits them (as empty records and interfaces) on day one.
   After that, everyone codes against them and nobody waits.
3. **Fakes until the real thing lands.** UI and 3D work start against
   `SampleData` (hand-made parts, Member 2 provides it with the contracts) so
   they do not wait for the real catalog or engine.
4. **Branches.** One branch per member (`data`, `engine`, `flow`, `ship3d`,
   `ui`), merged to `main` at least twice a day. `./mvnw test` must pass before
   a merge.
5. **Rules that apply to everyone** (already enforced by tests):
   - `model`, `engine`, `data`, `session`, `util` never import `javafx.*`.
   - No number is typed in from memory. Every catalog entry has a `source`;
     every number is `PUBLISHED`, `DERIVED` (sum shown in `data-sources.md`) or
     `GAME_ESTIMATE`.
   - Tests are written before the code they test.
   - Java 21 language level, built with `./mvnw` (the machine has JDK 23 and no
     system Maven).

| Member | Role | Owns |
|---|---|---|
| 1 | Data and sources | `resources/.../catalog/`, `data/`, `docs/data-sources.md` |
| 2 | Build rules engine | `model/`, `engine/` (design-time rules) |
| 3 | Mission flow, simulation, score | `session/`, `engine/sim/`, `engine/score/` |
| 4 | 3D hangar and ship | `render3d/`, `resources/.../models/` |
| 5 | Screens, theme, interaction | `app/`, `ui/`, `render/` (2D icons), `resources/.../ui/`, `css/`, `i18n/` |

Write the member's name beside each number before starting.

## 1. What already exists (Milestone 1, 73 tests passing)

| File | What it does | New owner |
|---|---|---|
| `model/Objective`, `Instrument`, `Bus`, `Orbit`, `GroundStation`, `LaunchVehicle`, `Capability`, `Requirement`, `SourceRef`, `Catalog`, `MissionDesign`, enums `MeasurementType`, `LaunchClass`, `ValueBasis` | Immutable records for everything the player can choose, and the current design | 2 |
| `data/CatalogLoader`, `CatalogException` | Reads the six JSON catalogs strictly; validates ids, sources, positive values | 1 |
| `catalog/*.json` (6 files) | 3 objectives, 10 instruments, 3 buses, 3 orbits, 6 ground stations, 4 rockets | 1 |
| `engine/OrbitMath` | Orbit period, highest latitude reached, ground-station view angle, contact minutes per day | 2 |
| `engine/ResourceCalculator`, `ResourceReport`, `ResourceUsage`, `ResourceType`, `ResourceLevel`, `GameRules` | The six resources of a design and their OK / TIGHT / OVER state | 2 |
| `session/GameSession` | The only object the UI talks to; holds the design, recalculates, notifies listeners | 3 |
| `app/LiftoffApp`, `SceneNavigator`, `Screen`, `ScreenController`, `Launcher` | Window, navigation between FXML screens | 5 |
| `ui/TitleController`, `MissionSelectController`, `ObjectiveCard` | Title and mission-select screens (v1 look; to be restyled) | 5 |
| `render/Palette`, `RocketArt`, `ObjectiveArt`, `Icons` | 2D vector art (v1 look; icons stay, art is replaced) | 5 |
| `css/liftoff.css`, `ui/*.fxml`, `i18n/messages.properties` | Theme, layouts, interface text | 5 |
| Tests: `ArchitectureTest`, `CatalogLoaderTest`, `OrbitMathTest`, `ResourceCalculatorTest`, `ResourceUsageTest`, `MissionDesignTest`, `PartFactsTest`, `GameSessionTest`, `TestParts`, `dev/SnapshotTool` | 73 tests; `SnapshotTool` renders screens to PNG | each owner keeps the tests of their folder |

## 2. Contracts between members (Member 2 commits these first)

```java
// model/ModuleSlot.java — the five tabs
enum ModuleSlot { AIRFRAME, THRUSTERS, POWER, CARGO_BAY, SATELLITE }

// model/ShipPart.java — one option in a tab
record ShipPart(String id, ModuleSlot slot, String name, String kidName, String description,
                String basedOn,            // the real vehicle or part
                List<String> missions,     // mission ids this part is offered in
                double massKg,
                double powerSupplyW,       // what it gives (power parts), else 0
                double powerDrawW,         // what it uses, else 0
                double payloadMassKg,      // what it can carry (airframe, cargo bay), else 0
                double bayLengthM, double bayDiameterM,   // cargo bay inside size, else 0
                double lengthM, double diameterM,         // outside size of a payload, else 0
                double thrustN, double specificImpulseS,  // thrusters, else 0
                double propellantCapacityKg,              // airframe tanks, else 0
                double dataGbitPerDay,
                double costMusd, ValueBasis costBasis,
                String modelId,            // which 3D model shows this part
                SourceRef source) {}

// model/ShipBuild.java — what is chosen in each tab; immutable
record ShipBuild(Map<ModuleSlot, ShipPart> parts) {
    static ShipBuild empty();
    ShipBuild with(ShipPart part);          // replaces the part in that part's slot only
    Optional<ShipPart> part(ModuleSlot slot);
    boolean isComplete();                   // all five slots filled
}

// engine/BuildIssue.java — one warning
record BuildIssue(Kind kind, ModuleSlot slot, double needed, double available) {
    enum Kind { SLOT_EMPTY, PAYLOAD_TOO_HEAVY, PAYLOAD_TOO_LONG, PAYLOAD_TOO_WIDE,
                POWER_SHORT, FUEL_SHORT, OVER_BUDGET }
}

// engine/BuildReport.java — everything the builder screen shows
record BuildReport(double dryMassKg, double propellantKg, double totalMassKg,
                   double costMusd, double powerSupplyW, double powerDrawW,
                   double deltaVAvailableMs, double deltaVRequiredMs,
                   List<BuildIssue> issues) {
    boolean canConfirm();                   // issues is empty
}

// engine/BuildCalculator.java
static BuildReport calculate(ShipBuild build, Mission mission);

// session/GameSession.java — additions
ShipBuild build();   BuildReport buildReport();
void choosePart(String partId);            // swaps that slot, keeps the others
void confirmBuild();                       // throws IllegalStateException unless canConfirm()
MissionStep step();  void goTo(MissionStep step);

// render3d/ShipView.java — the only thing the UI knows about the 3D
interface ShipView {
    javafx.scene.Node node();                    // put this in the screen
    void showPart(ModuleSlot slot, String modelId);   // snap-in swap; camera untouched
    void setCargoBayOpen(boolean open);
    void setDropHighlight(ModuleSlot slot);      // null clears it
    ModuleSlot slotAt(double sceneX, double sceneY);  // for drag and drop; null if none
}
```

`Mission` replaces `Objective` as the name of a playable goal and gains
`difficulty`, `destination`, `deltaVRequiredMs` and `steps`. Member 2 renames
it and keeps the old tests green.

## 3. Member 1 — Data and sources

**Purpose:** every number in the game, with proof of where it came from.

**Files**

| File | Status | Content |
|---|---|---|
| `catalog/missions.json` | new (replaces `objectives.json`) | The three missions of document 05, plus Storm Chasers and Ice Patrol marked `"ladder": false` |
| `catalog/ship-parts.json` | new | At least four options per tab per mission, in the `ShipPart` shape |
| `catalog/instruments.json` | extend | Add CubeSat-class imager(s), Moon instruments, Mars instruments |
| `catalog/launch-vehicles.json` | extend | Add SLS Block 1 and Atlas V 541; fill the missing polar figures |
| `catalog/events.json` | new | Random events for Member 3, each with a real example and its source |
| `data/CatalogLoader.java` | extend | Load the new files; validate that every mission has four or more options in every tab, every `modelId` is non-blank, and sizes are positive where the slot needs them |
| `data/CatalogLoaderTest.java` | extend | One test per new rule, written first |
| `docs/data-sources.md` | extend | One row per new entry: value, source, page, basis |

**Data you already have (verified; do not change without a source)**

- Source for all of these: NASA *Earth Science Reference Handbook* (2006),
  https://assets.science.nasa.gov/content/dam/science/missions/a-train/publications/2006ReferenceHandbook.pdf
  (page = PDF page).
- Instruments (mass kg / power W / average data kbps / page): ALI 90 / 100 /
  67.8 derived / 143; ETM+ 425 / 244.3 derived / 25,050 derived / 189; ASTER
  421 / 463 / 8,300 / 241; MODIS 229 / 162.5 / 6,200 / 98; TMI 54.1 / 66.5 /
  8.5 / 260; VIRS 48.6 / 51.2 / 49.8 / 261; LIS 20 / 33 / 6 / 258; AMSR-E 314 /
  350 / 87.4 / 94; GLAS 300 / 330 / 450 / 177; CERES 50 / 47 / 10 / 96.
- Buses (bus mass / payload mass / payload power / downlink Mbps): SORCE class
  248.4 / 66.6 / 61.3 / 1.5; EO-1 class 428.5 / 100.5 / 115 / 105; Landsat 7
  class 1557 / 425 / 590 / 150.
- Orbits: 402 km at 35° (TRMM); 705 km at 98.2° (Aqua, Landsat 7); 600 km at
  94° (ICESat).
- Rockets (capacity kg, cost $M): Electron 300 LEO / 200 sun-synchronous, 7.5;
  Pegasus XL 453.59 LEO, 56.3; Falcon 9 22,800 LEO, 80.4; Atlas V 401 9,800
  LEO / 8,080 polar, 153.8.
- Game estimates, labelled as such: instrument $0.5M per kg, bus $0.15M per kg,
  ground station $0.15M per month.

**Numbers you must add, and exactly how**

For each row: open the source, find the figure, put it in the named JSON field,
add the page or section to `source.detail`, and add a row to `data-sources.md`.
If the figure is not in the source, leave the option out and note it under
"Still to source". Do not use Wikipedia as the only source for a mass or power.

| # | What | JSON file and fields | Where to find it |
|---|---|---|---|
| 1 | CubeSat sizes 1U, 3U, 6U: outside size and maximum mass. Verified so far: 1U is at most 2.00 kg | `ship-parts.json`, slot `SATELLITE`: `lengthM`, `diameterM`, `massKg` | CubeSat Design Specification rev 14.1, Table 1 (cubesat.org) |
| 2 | One or more CubeSat-class Earth imagers: mass, power, ground resolution, data rate | `instruments.json` | NASA *State-of-the-Art Small Spacecraft Technology* report, https://www.nasa.gov/smallsat-institute/sst-soa/ |
| 3 | Spaceplane airframes: Shuttle Orbiter, X-37B, Dream Chaser — dry mass, payload mass, cargo bay length and diameter | `ship-parts.json`, slots `AIRFRAME` and `CARGO_BAY` | NASA Space Shuttle reference pages; NASA Dream Chaser page; the operator's own fact sheet for X-37B. Secondary sites disagree on Dream Chaser (5,000 to 5,500 kg), so use the NASA figure only |
| 4 | Thrusters: at least four engines with thrust (N), specific impulse (s), mass | `ship-parts.json`, slot `THRUSTERS`: `thrustN`, `specificImpulseS`, `massKg` | NASA engine fact sheets (Shuttle OMS, RL10, Orion main engine); NASA SmallSat report, Propulsion chapter, for small thrusters |
| 5 | Power: body solar, deployable solar, fuel cell, radioisotope generator — watts and mass. Known so far: MMRTG about 110 W at launch; arrays cluster near 30 W/kg; cells 30–34 % efficient | `ship-parts.json`, slot `POWER`: `powerSupplyW`, `massKg` | JPL Mars 2020 press kit, Power page; NASA SmallSat report, Power chapter |
| 6 | Moon: Orion crew module and service module masses, propellant, crew size; lander | `ship-parts.json` (missions `moon-landing`) | NASA Orion Reference Guide; NASA Artemis pages |
| 7 | Moon rocket: SLS Block 1. Verified: more than 27 t toward the Moon | `launch-vehicles.json`: add class `TRANS_LUNAR` | NASA SLS fact sheet / reference guide |
| 8 | Moon instruments: surface and orbit instruments — mass, power, data | `instruments.json` | NASA Artemis III instrument announcement; NASA Lunar Reconnaissance Orbiter press kit |
| 9 | Crew supplies per person per day (food, water, oxygen) in kg | new field `suppliesKgPerCrewDay` in `missions.json` | NASA life-support baseline documents on ntrs.nasa.gov |
| 10 | Mars: rover (verified about 1,025 kg), helicopter, cruise stage, aeroshell, landing system masses | `ship-parts.json` (missions `mars-drones`) | JPL Mars 2020 Launch Press Kit, https://www.jpl.nasa.gov/news/press_kits/mars_2020/launch/ |
| 11 | Mars rocket: Atlas V 541 capacity to Mars | `launch-vehicles.json`: add class `TRANS_MARS` | ULA Atlas V page; JPL press kit |
| 12 | Mars instruments: mast camera, laser chemistry camera, X-ray analyser, ground radar, weather station — mass, power | `instruments.json` | JPL Mars 2020 press kit, Science page |
| 13 | Trip speed changes (delta-v) for each mission leg, m/s | `missions.json`: `deltaVRequiredMs` | NASA mission design references on ntrs.nasa.gov; state the legs summed |
| 14 | Polar capacity of Falcon 9 and Pegasus XL | `launch-vehicles.json`: `capacityKg.POLAR` | NASA Launch Vehicle Performance site, elvperf.ksc.nasa.gov |
| 15 | Onboard data storage of each bus | new field `storageGbit` | Handbook: Landsat 7 recorder is 380 Gbit (p. 190); others to find |

**Done when:** all catalogs load; `CatalogLoaderTest` passes with the new rules;
every new number has a row in `data-sources.md`; "Still to source" lists
anything left out.

## 4. Member 2 — Build rules engine

**Purpose:** given a set of chosen parts, say what the ship weighs, costs,
supplies, needs in fuel, and what is wrong with it. No JavaFX.

**Classes**

| Class | Status | What it does |
|---|---|---|
| `model/ModuleSlot`, `ShipPart`, `ShipBuild` | new | Section 2. `ShipBuild.with` replaces one slot and keeps the rest |
| `model/Mission` | rename of `Objective` | Adds `difficulty`, `destination`, `deltaVRequiredMs`, `ladder` |
| `model/MissionDesign` | extend | Holds the `ShipBuild` in place of the single `Bus` |
| `engine/BuildIssue`, `BuildReport` | new | Section 2 |
| `engine/FitRules` | new | `payloadFits(bay, payload)`: length, diameter and mass checks, each returning a `BuildIssue` or nothing |
| `engine/FuelMath` | new | `propellantKg(dryMassKg, deltaVMs, ispS)` and `deltaVMs(dryMassKg, propellantKg, ispS)` |
| `engine/BuildCalculator` | new | Sums mass and cost, compares power supply with draw, calls `FuelMath` and `FitRules`, builds the `BuildReport` |
| `engine/ResourceCalculator` | extend | Mass now includes propellant; power limit comes from the POWER part |
| `engine/DesignValidator` | new | For the Review step: required measurements met, orbit reaches the target latitude |
| `SampleData` (test folder, shared) | new | A small hand-made catalog with round numbers, for Members 4 and 5 to code against |

**Formulas and constants you need**

- Rocket equation: `propellant = dryMass × (e^(Δv / (Isp × g0)) − 1)`,
  `g0 = 9.80665 m/s²`. Check: Δv = Isp × g0 × ln 2 must give propellant equal
  to dry mass.
- Fit: payload length ≤ bay length, payload diameter ≤ bay diameter, payload
  mass ≤ airframe payload mass.
- Power: sum of `powerDrawW` ≤ `powerSupplyW` of the POWER part.
- Budget: sum of `costMusd` ≤ mission cap.
- Already in `OrbitMath` (do not change): `T = 2π√(a³/μ)`, μ = 398,600.4418
  km³/s², Earth radius 6378.137 km; view angle
  `arccos(R cos ε / (R + h)) − ε` with ε = 5°. Checks in place: 705 km gives
  98.8 min; a station at 78.2° N sees the 705 km sun-synchronous orbit about
  137 min/day and never sees the 35° orbit.
- `GameRules`: TIGHT from 85 % of a limit; integration 6 months + 2 per
  instrument.

**Tests to write first:** `ShipBuildTest` (swap keeps other slots),
`FuelMathTest` (the ln 2 check; zero Δv gives zero fuel; round trip of the two
functions), `FitRulesTest` (each of the three failures and a pass),
`BuildCalculatorTest` (one test per `BuildIssue.Kind`, plus `canConfirm` true
only when none).

**Done when:** contracts are on `main` on day one; all the tests above pass;
the existing 73 still pass.

## 5. Member 3 — Mission flow, simulation, score

**Purpose:** what happens between pressing Confirm Build and reading the
report. No JavaFX.

**Classes**

| Class | Status | What it does |
|---|---|---|
| `session/MissionStep` | new | `BRIEFING, PLAN, SHIP, SUPPLIES, REVIEW, LAUNCH, FLY, REPORT` |
| `session/MissionFlow` | new | Which step is current, which steps are unlocked (table in document 05 §3), `next()`, `back()`, `goTo()` |
| `session/GameSession` | extend | Section 2 additions; delegates to `MissionFlow`; `confirmBuild()` refuses unless `buildReport().canConfirm()` |
| `engine/sim/LaunchChecker` | new | Mass vs rocket capacity for the destination, fuel vs trip, orbit vs target latitude; returns a list of reasons |
| `engine/sim/MissionSimulator` | new | Steps through time (months for Earth and Mars, days for the Moon): data produced, data sent home, data lost, power shortfall, supplies used; takes a seed so runs repeat |
| `engine/sim/SimulationResult`, `SimEvent` | new | The timeline and totals the Fly screen plays back |
| `engine/sim/EventDeck` | new | Draws random events from `events.json` by difficulty and seed |
| `engine/score/ScoreCalculator` | new | 0–100: science returned 60, healthy margins 20, on budget and on time 20; one to three stars |
| `engine/score/ReportBuilder` | new | The three "why" cards: what worked, what broke first, what to try |
| `session/MissionLog` | new | Local list of finished missions and scores, saved as JSON in the user's home folder; ranks missions, never people |

**Data you need**

- From `ResourceReport`: used and limit for mass, power, budget, data, comms,
  time.
- Data returned per day = downlink rate × contact minutes × 60; contact minutes
  come from `OrbitMath.contactMinutesPerDay`.
- Difficulty: Mission 1 no events and wide caps; Mission 2 some events; Mission
  3 more events and the tightest caps. Caps live in `missions.json` (Member 1).
- Each event needs a real example and a source; agree the list with Member 1.
  Starting list: solar storm puts the spacecraft in safe mode; ground station
  outage; instrument fault (for example Landsat 7's scan-line corrector; confirm the date from a NASA or USGS page);
  launch slip; Mars dust lowering solar power.

**Tests to write first:** `MissionFlowTest` (cannot reach Supplies with an
incomplete ship; cannot launch unconfirmed), `LaunchCheckerTest` (one per
reason), `MissionSimulatorTest` (same seed gives the same result; data sent
never exceeds data produced; no stations means nothing returned),
`ScoreCalculatorTest`, `ReportBuilderTest` (names the first limit that broke).

**Done when:** a mission can be played start to finish from a unit test with no
UI.

## 6. Member 4 — 3D hangar and ship

**Purpose:** the rotatable ship in the hangar. Knows nothing about game rules;
it is told which model to show in which slot.

**Classes** (new package `render3d`)

| Class | What it does |
|---|---|
| `ShipView` (interface, section 2) and `HangarShipView` | The JavaFX `SubScene` with camera, lights, pad and ship |
| `OrbitCamera` | Drag to rotate, wheel to zoom, with limits; never changed by a part swap |
| `ModelLibrary` | Loads each `modelId` once and hands out copies |
| `ObjLoader` | Reads Wavefront `.obj` files into a JavaFX `TriangleMesh` (our own small loader; no new dependency) |
| `ShipAssembly` | Holds one node per `ModuleSlot` at its anchor point; `showPart` removes the old node, adds the new, and moves later anchors so a longer airframe stretches the ship |
| `SnapAnimation` | The 200 ms scale-and-settle on a newly placed part; skipped in calm mode |
| `CargoBayDoors` | Opens and closes the doors; shows the satellite inside when open |
| `SlotPicker` | Turns a mouse position into the `ModuleSlot` under it, for drag and drop |

**Assets you make:** one low-polygon model per `modelId` (about 25 to start),
exported as `.obj` to `resources/com/liftoff/models/`, plus the hangar backdrop
as a rendered image. Blender is the suggested tool for both. The CubeSat models need the recessed camera aperture on one face.

**Data you need:** the list of `modelId` values from Member 1's
`ship-parts.json` (use `SampleData` until then), and each part's `lengthM` and
`diameterM` so models are in true proportion to each other.

**Limits to respect:** JavaFX 3D has no shadows or reflections; the look is
clean and stylised, not ray-traced. Keep each model under about 5,000
triangles.

**Tests:** `ObjLoaderTest` (a cube file gives 8 points and 12 triangles),
`ShipAssemblyTest` (swapping one slot leaves the other nodes the same objects;
a longer airframe moves the thruster anchor back). Visual check with
`SnapshotTool`.

**Done when:** a standalone demo window shows the ship, rotates, swaps parts
from a keyboard key with the snap, and opens the bay.

## 7. Member 5 — Screens, theme, interaction

**Purpose:** everything the player touches. Calls `GameSession`; never computes
a rule.

**Files**

| File | Status | What it does |
|---|---|---|
| `css/liftoff.css` | rewrite | The hangar theme of document 05 §8 |
| `ui/MissionSelectController`, `mission-select.fxml` | restyle | Three mission cards with pictures, as in `docs/reference/1.webp` |
| `ui/StepperBar` | new | Briefing / Ship / Supplies / Review; shows the current step and which are unlocked (from `GameSession.step()`) |
| `ui/BriefingController`, `briefing.fxml` | new | Goal, limits, what must be measured |
| `ui/ShipBuilderController`, `ship-builder.fxml` | new | The builder screen of `docs/reference/2.webp` |
| `ui/ModuleTabs` | new | The five tab buttons; selecting Satellite calls `shipView.setCargoBayOpen(true)` |
| `ui/PartStrip`, `PartCard` | new | The option cards with arrows; click selects; a card can be dragged |
| `ui/PartDetailPanel` | new | Name, picture, mass, power, data, cost of the focused part, with an "≈" on estimates |
| `ui/BuildGauges` | new | Mass, cost, power, fuel bars from `BuildReport` |
| `ui/WarningList` | new | One line per `BuildIssue`, icon plus sentence from `messages.properties` |
| `ui/SuppliesController`, `ReviewController`, `LaunchController`, `FlyController`, `ReportController` and their FXML | new | The remaining steps |
| `ui/SourcesController` | new | Every catalog entry with its source and basis |
| `render/Icons` | extend | One icon per resource and per warning kind |
| `i18n/messages.properties` | extend | All text, including one message per `BuildIssue.Kind` |
| `app/Screen` | extend | One entry per new screen |

**Behaviour to implement on the builder screen**

1. Click a card → `session.choosePart(id)` → on the session's change callback,
   call `shipView.showPart(slot, modelId)`, ring the card, refresh detail
   panel, gauges and warnings.
2. Drag a card → while dragging, `shipView.setDropHighlight(shipView.slotAt(x, y))`;
   on drop over the matching slot, do the same as a click; on drop elsewhere,
   nothing changes.
3. Confirm Build is disabled unless `session.buildReport().canConfirm()`; when
   disabled, its tooltip lists the warnings.
4. Keyboard: tabs and cards reachable with Tab and arrows; Enter selects.
5. Nothing is shown by colour alone; every state has an icon and a word.

**Data you need:** nothing numeric. Colours and layout from document 05 §8 and
`docs/reference/`. Use `SampleData` and a stub `ShipView` (a labelled box per
slot) until Members 1, 2 and 4 deliver.

**Tests:** keep controllers thin; test `StepperBar` state and `WarningList`
text with plain unit tests; check every screen with `SnapshotTool` at 1280×800
and 1280×660.

**Done when:** every step of the workflow can be clicked through against
`SampleData` with the stub ship.

## 8. Order of work

| When | 1 Data | 2 Engine | 3 Flow | 4 3D | 5 UI |
|---|---|---|---|---|---|
| First | Mission 1 parts and CubeSat figures | **Contracts + SampleData on main** | `MissionStep`, `MissionFlow` | `ObjLoader`, camera, pad | Theme, mission select |
| Then | Moon and Mars figures | `FuelMath`, `FitRules`, `BuildCalculator` | `LaunchChecker`, simulator | `ShipAssembly`, snap, bay doors | Builder screen on stub ship |
| Then | Events, storage, polar figures | `DesignValidator`, resource changes | Score, report, log | Models for all parts | Supplies, Review, Fly, Report |
| Last | Sources screen data check | Balance review with Member 3 | Events and difficulty | Polish, calm mode | Join real ship, real data; keyboard pass |

Integration points: (a) contracts merged; (b) builder screen with real 3D and
real Mission 1 data; (c) Mission 1 playable end to end; (d) Missions 2 and 3.
