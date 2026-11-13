# Member 2 — Build Rules Engine: Working Guide

The single file to read before any Member 2 work session. It condenses
`05-missions-and-ship-builder.md`, `06-team-work-split.md` and
`03-architecture.md` for Member 2, records the decisions taken while working,
and tracks progress. **Update the progress table (section 9) after every class.**

---

## 1. The game in one page

LIFTOFF is a space-mission design game for children aged 7–13 (team
404PlanetNotFound, NASA Space Apps Cairo 2026). The player picks a mission,
builds a spacecraft from modules, loads it, launches, flies and reads a report
that explains *why* it worked or failed. The lesson: **every choice costs
something across several resources at once.**

**Three missions of rising difficulty** (difficulty = less leeway, not trick questions):

| # | Mission | Goal | Real model | Body (Airframe tab) | Idea taught |
|---|---|---|---|---|---|
| 1 | Farm Watch (easy) | Satellite in Earth orbit that sees single fields (30 m) | Landsat, EO-1, CubeSats | Winged spaceplane / small buses | One choice moves several gauges |
| 2 | Moon Landing (medium) | Astronauts to the Moon and back | Artemis (SLS, Orion, lander) | Crew capsule + service module | Fuel grows much faster than payload (rocket equation) |
| 3 | Mars Sample Drones (hard) | Drones/rover collect samples on Mars | Mars 2020 (Perseverance, Ingenuity) | Cruise stage + aeroshell | You cannot fix it after launch |

Storm Chasers and Ice Patrol (v1) stay as optional extra Earth missions (`ladder = false`).

**Workflow (same for every mission):**
`BRIEFING → SHIP → SUPPLIES → REVIEW → LAUNCH → FLY → REPORT` (Report can loop back to Ship = Redesign).

- **Ship step** = the builder: a 3D ship in a blue hangar, five tabs —
  **Airframe, Thrusters, Power, Cargo bay, Satellite** — at least four options
  each. Click or drag a card → the part in that slot is swapped, all other
  parts stay, gauges and warnings update. **Confirm Build** is enabled only when
  every slot is filled, everything fits, power and fuel suffice and the budget
  holds. ← *this logic is Member 2's.*
- **Resources:** Mass (kg), Power (W), Budget ($M), Data (Gbit/day), Comms
  (min/day), Time (months), **plus Fuel**, which is worked out from mass, never chosen.

**Who does what**

| Member | Role | Owns |
|---|---|---|
| 1 | Data and sources | `catalog/*.json`, `data/`, `docs/data-sources.md` |
| **2** | **Build rules engine** | **`model/`, `engine/` (design-time rules)** |
| 3 | Mission flow, simulation, score | `session/`, `engine/sim/`, `engine/score/` |
| 4 | 3D hangar and ship | `render3d/`, `resources/.../models/` |
| 5 | Screens, theme, interaction | `app/`, `ui/`, `render/`, FXML, CSS, i18n |

## 2. Rules Member 2 must never break

1. `model` and `engine` **never import `javafx.*`** (`ArchitectureTest` fails the build).
2. **No number from memory.** Real numbers come only from Member 1's catalogs
   (each with `source` and `ValueBasis`). Formulas and constants in code carry a
   comment naming their source. Test numbers are round and obviously fake.
3. **Test first:** write the test, run it red, write the code, run it green.
4. Only edit Member 2's folders. Anything needed elsewhere = a request to that owner.
5. Records are immutable; "changing" returns a new copy.
6. Java 21 language level; build only with the wrapper (JDK 23 installed, no system Maven):
   - PowerShell: `.\mvnw.cmd test`   Git Bash: `./mvnw test`
7. Branch `engine`; merge to `main` at least twice a day; `mvnw test` must pass first.
8. Commits: team rule — no assistant attribution lines in commit messages.

## 3. What already exists that Member 2 now owns (Milestone 1, 73 tests green)

- `model/`: `Objective`, `Instrument`, `Bus`, `Orbit`, `GroundStation`,
  `LaunchVehicle` (`capacityFor(LaunchClass)`), `Capability(type, resolutionM)`,
  `Requirement(type, maxResolutionM)`, `SourceRef(title, detail, url)`,
  `Catalog` (lookups by id), `MissionDesign` (immutable, `withX` / `plusX` / `minusX`),
  enums `MeasurementType`, `LaunchClass` (LOW_INCLINATION, POLAR), `ValueBasis`
  (PUBLISHED, DERIVED, GAME_ESTIMATE).
- `engine/`: `OrbitMath` (do not change), `ResourceCalculator.calculate(MissionDesign)`
  → `ResourceReport` of six `ResourceUsage(type, used, capacity, level)`;
  `ResourceLevel` OK/TIGHT/OVER/UNKNOWN; `GameRules` (TIGHT_FRACTION 0.85,
  MIN_ELEVATION_DEG 5, INTEGRATION_BASE_MONTHS 6, INTEGRATION_MONTHS_PER_INSTRUMENT 2).
- Tests: `MissionDesignTest`, `PartFactsTest`, `OrbitMathTest`,
  `ResourceCalculatorTest`, `ResourceUsageTest`; helper `src/test/java/com/liftoff/TestParts.java`.

Current `MissionDesign(objective, bus, instruments, orbit, stations, launchVehicle, scienceMonths)`.
`GameSession` (Member 3) and `CatalogLoader` (Member 1) both use `Objective` and `MissionDesign`.

## 4. Decisions taken for Member 2 (keep this list current)

| # | Decision | Why |
|---|---|---|
| D1 | `Mission` is added as a **new record next to `Objective`**, not a rename on day one. `Objective` is deleted later, in the same change where Member 1 switches the loader to `missions.json`. | The loader rejects missing JSON fields, so a rename would force new fields (delta-v, difficulty) into `objectives.json` — numbers nobody has sourced yet, in Member 1's file. Adding beside it breaks nobody and invents nothing. |
| D2 | `Mission` has no `steps` field. | Doc 05 §3: the seven steps are the same for every mission; `MissionStep` lives in `session` (Member 3), and `model` may not depend on `session`. |
| D3 | `BuildCalculator.calculate(build, mission)` covers the five ship parts only. Instruments join when the Supplies step lands (overload or `MissionDesign`-based call). | Matches the contract in 06 §2; instruments are chosen in Supplies. |
| D4 | Propellant cost is **not** added to the budget yet. | No sourced cost per kg of propellant. Added when Member 1 sources one (doc 05 §7 asks for it). |
| D5 | Payload checks: satellite length ≤ bay length, satellite diameter ≤ bay diameter, satellite mass ≤ bay `payloadMassKg` (slot CARGO_BAY); cargo bay + satellite mass ≤ airframe `payloadMassKg` (slot AIRFRAME). | Contract says `payloadMassKg` is "what it can carry (airframe, cargo bay)". |
| D6 | Fuel: dry mass = sum of all five part masses; Isp from the THRUSTERS part; needed propellant from the rocket equation with the mission's delta-v; tank capacity = AIRFRAME `propellantCapacityKg`. FUEL_SHORT when needed > capacity. | Doc 05 §7. |
| D7 | `SampleData` lives in `src/test/java/com/liftoff/SampleData.java`; every value is round, `GAME_ESTIMATE`, source titled "Sample data — not real". | Members 4 and 5 code against it; must never pass for NASA data. |
| D8 | `MissionDesign` gains `mission` and `build` **beside** `objective` and `bus`; the bus is not removed yet. `build` is never null (empty ship). | `GameSession` (Member 3) and its tests still call `withBus` / `withObjective`, and `ShipPart` has no downlink rate, so data and comms would have no limit without the bus. Nobody outside `MissionDesign` calls its constructor, so this broke nothing. |
| D9 | `ShipPart` rejects negative, NaN or infinite numbers, and a THRUSTERS part must have `specificImpulseS > 0`. `Mission` rejects a negative or NaN budget cap, delta-v or crew supplies. | The rocket equation divides by Isp; bad data must fail when it is loaded, not crash the builder screen later. |
| D10 | `BuildCalculator` on an unfinished ship: one SLOT_EMPTY per empty tab, and a check that needs a missing part is skipped (no POWER_SHORT without a power part, no FUEL_SHORT without airframe and thruster). Exactly on a limit passes. Issue order: empty tabs (tab order), fit (long, wide, heavy-bay, heavy-airframe), power, fuel, budget. | One clear message per problem; a stable order for the UI and tests. |
| D11 | `DesignValidator.validate(MissionDesign)` returns `List<DesignIssue>` (kinds MEASUREMENT_MISSING, RESOLUTION_TOO_COARSE, ORBIT_MISSES_TARGET). It reads the goal from `mission`, else `objective`. A capability whose source states no resolution cannot satisfy a resolution limit (`available` = NaN). No orbit chosen = orbit check skipped. The orbit reaches \|target latitude\| ≤ `OrbitMath.maxLatitudeDeg`; instrument swath is ignored (conservative). | Data rule: never assume an unstated figure is good enough. |
| D12 | `ResourceCalculator` with a ship: mass = instruments + bus + ship parts + propellant (rocket equation on the whole dry mass, only when mission, airframe and thruster are present); power used = instruments + parts' draw, limit = POWER part's supply (bus payload power only when there is no POWER part); budget adds parts' cost; mission cap and deadline win over the objective's; data per day adds the parts' `dataGbitPerDay`. Downlink rate still comes from the bus. | Doc 06 §4: "mass now includes propellant; power limit comes from the POWER part". |

**Open questions for the team (not Member 2's alone to decide)**

1. **Where does the radio live in v2?** `ShipPart` has no downlink rate, so the Data and Comms gauges only get a limit from the old `Bus`. Options: add `downlinkMbps` to `ShipPart` (SATELLITE or AIRFRAME), or keep a separate choice. Needs Member 1 (JSON), Member 3 (simulator) and Member 5 (gauges).
2. **Build time of ship parts.** `ShipPart` has no `buildMonths`, so only instruments (and the old bus) feed the Time gauge.
3. Propellant cost per kg (D4), still unsourced.
4. `SampleData` is in the test folder, so the running app cannot load it; Members 4 and 5 can use it from tests, `SnapshotTool` and demo mains under `src/test`.

## 5. Contracts (commit to `main` on day one — others are waiting on these)

Package `com.liftoff.model`:

```java
enum ModuleSlot { AIRFRAME, THRUSTERS, POWER, CARGO_BAY, SATELLITE }

record ShipPart(String id, ModuleSlot slot, String name, String kidName, String description,
                String basedOn, List<String> missions,
                double massKg, double powerSupplyW, double powerDrawW,
                double payloadMassKg, double bayLengthM, double bayDiameterM,
                double lengthM, double diameterM,
                double thrustN, double specificImpulseS, double propellantCapacityKg,
                double dataGbitPerDay, double costMusd, ValueBasis costBasis,
                String modelId, SourceRef source)
    // compact ctor: id, slot, source non-null; missions = List.copyOf
    boolean offeredIn(String missionId)

record ShipBuild(Map<ModuleSlot, ShipPart> parts)
    // compact ctor: copy into EnumMap, unmodifiable; each key must equal part.slot()
    static ShipBuild empty()
    ShipBuild with(ShipPart part)        // replaces that slot only
    Optional<ShipPart> part(ModuleSlot slot)
    boolean isComplete()                 // all five slots filled

enum Difficulty  { EASY, MEDIUM, HARD }
enum Destination { EARTH_ORBIT, MOON_SURFACE, MARS_SURFACE }

record Mission(String id, String title, String question, String description, String basedOn,
               Difficulty difficulty, Destination destination, boolean ladder,
               double targetLatitudeDeg, boolean prefersSunSynchronous,
               List<Requirement> required, List<MeasurementType> bonus,
               double budgetCapMusd, int deadlineMonths, int minScienceMonths,
               double deltaVRequiredMs, double suppliesKgPerCrewDay,
               SourceRef source)
```

Package `com.liftoff.engine`:

```java
record BuildIssue(Kind kind, ModuleSlot slot, double needed, double available) {
    enum Kind { SLOT_EMPTY, PAYLOAD_TOO_HEAVY, PAYLOAD_TOO_LONG, PAYLOAD_TOO_WIDE,
                POWER_SHORT, FUEL_SHORT, OVER_BUDGET }
}
record BuildReport(double dryMassKg, double propellantKg, double totalMassKg,
                   double costMusd, double powerSupplyW, double powerDrawW,
                   double deltaVAvailableMs, double deltaVRequiredMs,
                   List<BuildIssue> issues) {
    boolean canConfirm();   // issues.isEmpty()
}
final class BuildCalculator { static BuildReport calculate(ShipBuild build, Mission mission); }
```

`BuildCalculator.calculate` is real (no longer a stub). Also available:

```java
final class FuelMath  { static double propellantKg(dryKg, deltaVMs, ispS);  static double deltaVMs(dryKg, propellantKg, ispS); }
final class FitRules  { static List<BuildIssue> payloadFits(ShipPart bay, ShipPart payload);
                        static Optional<BuildIssue> airframeCarries(ShipPart airframe, double cargoKg); }
record DesignIssue(Kind kind, MeasurementType measurement, double needed, double available)
    // Kind: MEASUREMENT_MISSING, RESOLUTION_TOO_COARSE, ORBIT_MISSES_TARGET
final class DesignValidator { static List<DesignIssue> validate(MissionDesign design); }
// MissionDesign additions: mission(), build(), withMission(Mission), withBuild(ShipBuild), withPart(ShipPart)
```

`needed` / `available` per kind (Member 5 uses them in messages):

| Kind | slot | needed | available |
|---|---|---|---|
| SLOT_EMPTY | the empty slot | 0 | 0 |
| PAYLOAD_TOO_LONG | CARGO_BAY | satellite length m | bay length m |
| PAYLOAD_TOO_WIDE | CARGO_BAY | satellite diameter m | bay diameter m |
| PAYLOAD_TOO_HEAVY | CARGO_BAY or AIRFRAME | cargo kg | limit kg |
| POWER_SHORT | POWER | total draw W | supply W |
| FUEL_SHORT | AIRFRAME | propellant needed kg | tank capacity kg |
| OVER_BUDGET | null | cost $M | cap $M |

## 6. Formulas

- **Rocket equation** (Tsiolkovsky; doc 05 §7):
  `propellant = dry × (e^(Δv / (Isp × g0)) − 1)`, inverse `Δv = Isp × g0 × ln((dry + propellant) / dry)`,
  `g0 = 9.80665 m/s²` (standard gravity, defined constant).
  Check: Δv = Isp·g0·ln 2 ⇒ propellant = dry.
- **Fit:** length ≤ bay length; diameter ≤ bay diameter; mass ≤ limit.
- **Power:** Σ `powerDrawW` (all parts) ≤ Σ `powerSupplyW` (in practice the POWER part).
- **Budget:** Σ `costMusd` ≤ `mission.budgetCapMusd()`.
- **Orbit (exists, do not change):** `T = 2π√(a³/μ)`, μ = 398600.4418 km³/s²,
  Rₑ = 6378.137 km; view angle `arccos(Rₑ cos ε/(Rₑ+h)) − ε`, ε = 5°.

## 7. Classes, in the order we build them

Each row: test class first (red), then the class (green), then `mvnw test` all green.

| # | Class | Test | Key test cases |
|---|---|---|---|
| 1 | `model/ModuleSlot` | — | five values in tab order |
| 2 | `model/ShipPart` | `ShipPartTest` | null id/slot/source rejected; `missions` list is a copy; `offeredIn` |
| 3 | `model/ShipBuild` | `ShipBuildTest` | empty has no parts; `with` fills one slot; swap keeps other slots (same objects); `isComplete` only with five; returned map unmodifiable; key/slot mismatch rejected |
| 4 | `model/Difficulty`, `Destination`, `Mission` | `MissionTest` | lists copied; fields readable |
| 5 | `engine/BuildIssue`, `BuildReport`, `BuildCalculator` (stub) | `BuildReportTest` | `canConfirm` true only with no issues; issues list copied |
| 6 | `test/.../SampleData` | used by tests | one sample mission, ≥4 parts per slot, round fake numbers |
| — | **Commit + merge contracts to `main`; tell the team** | | |
| 7 | `engine/FuelMath` | `FuelMathTest` | ln 2 check; zero Δv → 0 fuel; round trip both functions; rejects dry ≤ 0 or Isp ≤ 0 |
| 8 | `engine/FitRules` | `FitRulesTest` | too long, too wide, too heavy (bay), too heavy (airframe), a pass |
| 9 | `engine/BuildCalculator` (real) | `BuildCalculatorTest` | one test per `Kind`; sums correct; `canConfirm` true only when none |
| 10 | `engine/DesignValidator` | `DesignValidatorTest` | required measurement missing; resolution too coarse (MODIS 250 m vs 30 m goal); orbit max latitude below target |
| 11 | `model/MissionDesign` holds `ShipBuild` (+ `Mission`) | `MissionDesignTest` | coordinate with Member 3 (`GameSession`) before merging |
| 12 | `engine/ResourceCalculator` extension | `ResourceCalculatorTest` | mass includes propellant; power limit from POWER part |
| 13 | Balance review with Member 3 | — | uses `economy-designer` / `systems-designer` agents if wanted |

## 8. Hand-offs

- **To Member 1:** `ShipPart` and `Mission` field names = JSON field names in
  `ship-parts.json` / `missions.json`. Ask for: delta-v per mission, propellant
  cost per kg (D4), tank capacity per airframe.
- **To Member 3:** `BuildCalculator.calculate`, `BuildReport.canConfirm()` for
  `confirmBuild()`; `DesignValidator` for Review; the `MissionDesign` change (row 11).
- **To Members 4, 5:** `ModuleSlot`, `ShipPart.modelId`, `ShipBuild`, `SampleData`, `BuildIssue.Kind`.

## 9. Progress (update after every class)

| # | Item | Status | Notes |
|---|---|---|---|
| 0 | Branch `engine` created | done | from `main` at `d17a8f6` |
| 1 | ModuleSlot | done (not committed) | `ModuleSlotTest` checks tab order; 74 tests green |
| 2 | ShipPart | done (not committed) | `ShipPartTest` 6 tests; 80 tests green |
| 3 | ShipBuild | done (not committed) | `ShipBuildTest` 9 tests |
| 4 | Difficulty, Destination, Mission | done (not committed) | `MissionTest` 9 tests; rejects negative figures (D9) |
| 5 | BuildIssue, BuildReport | done (not committed) | `BuildReportTest` 7 tests |
| 6 | SampleData | done (not committed) | `SampleDataTest` 7 tests; 20 parts; starter build proven to pass every check in `BuildCalculatorTest` |
| — | Contracts on main | waiting for go-ahead | commit on `engine`, merge to `main`, push, tell the team |
| 7 | FuelMath | done (not committed) | `FuelMathTest` 9 tests; `propellantKg`, `deltaVMs`, `STANDARD_GRAVITY_MS2` |
| 8 | FitRules | done (not committed) | `FitRulesTest` 8 tests; `payloadFits(bay, payload)`, `airframeCarries(airframe, cargoKg)` |
| 9 | BuildCalculator | done (not committed) | `BuildCalculatorTest` 20 tests; one per `Kind`, edges, sample catalog (D10) |
| 10 | DesignValidator, DesignIssue | done (not committed) | `DesignValidatorTest` 15 tests (D11) |
| 11 | MissionDesign + Mission + ShipBuild | done (not committed) | `MissionDesignShipTest` 7 tests; `withMission`, `withBuild`, `withPart` (D8) |
| 12 | ResourceCalculator extension | done (not committed) | `ResourceCalculatorShipTest` 12 tests (D12) |
| — | Revision pass | done | input validation added (D9); zero-dry-mass guard in `BuildCalculator`; **186 tests green from `mvnw clean test`**; app not launched by hand (no UI code changed) |
| 13 | Balance review | waits for Member 3 | needs the simulator and real catalog numbers |

**Still to do after this:** delete `Objective` and `Bus` from `MissionDesign` once Member 1 ships
`missions.json` / `ship-parts.json` and Member 3 moves `GameSession` to `choosePart` / missions;
answer the open questions under section 4.

## 10. Working mode

The assistant writes each **test** file and runs it red. Member 2 writes each
**main class** (`src/main/...`) themselves, guided one class at a time: what
the class is for, what each part does, a skeleton with hints. The assistant
does not write main classes. When Member 2 says a class is done, it reads the
files, runs the tests and reviews.

## 11. How to resume in a new chat

"I am Member 2. Read `docs/member-2-guide.md` and continue from the first item
in section 9 that is not done. Guide me through the class; I write the code."
