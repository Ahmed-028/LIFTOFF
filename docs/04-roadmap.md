# LIFTOFF — Development Roadmap

> **Updated direction (2026-11-13):** the missions, screen layouts, colours and milestone split in this document are superseded by `05-missions-and-ship-builder.md` and `06-team-work-split.md`. The rest still applies.

Milestone names follow the deck (slide 6). Each milestone ends with tests
passing and the app run by hand. Nothing is called done without that.

## M1 — Foundation: data model (this milestone)

Goal: the rules of the game exist and are proven, and the app opens.

| # | Task | Done when |
|---|---|---|
| 1 | Maven project, wrapper, Java 21 target, JavaFX/Jackson/JUnit | `mvnw test` runs |
| 2 | Model records and enums | Compiles; `MissionDesign` copy methods tested |
| 3 | JSON catalogs with sources | Files load; integrity tests pass |
| 4 | `CatalogLoader` with validation | Rejects unknown fields and missing sources |
| 5 | `OrbitMath` | Period matches published values; contact-time cases pass |
| 6 | `ResourceCalculator` for all six resources | Tests for each resource and each state pass |
| 7 | `GameSession` | Selecting parts updates the report and notifies listeners |
| 8 | No-JavaFX architecture test | Passes |
| 9 | App shell: title screen and mission select, themed | Launches; objective choice reaches `GameSession` |

## M2 — User interface: mission control

Build a spacecraft and watch the gauges.

- Mission control screen with the four role stations.
- Part cards for buses, instruments and ground stations; add, remove, swap.
- Live six-gauge dashboard with OK / TIGHT / OVER states and change tags.
- Spacecraft picture assembled from the chosen parts.
- `DesignValidator`: bus payload mass and power limits, required measurements.
- Guide prompts; Sources screen; bundled fonts; calm-mode setting.

## M3 — Simulation engine: launch and fly

- Launch & orbit screen: orbit rings with coverage band, rocket line-up.
- `LaunchChecker`: mass, orbit reach, budget, time, with reasons.
- `MissionSimulator`: month-by-month data, downlink, storage, power shortfall.
- Simulation screen with timeline and science counter.
- Use the `systems-designer` agent to review the simulation formulas.

## M4 — Test and polish: score, report, replay

- `ScoreCalculator` and `ReportBuilder`: score, stars, the three "why" cards.
- Report screen; Redesign loop; local mission log.
- Difficulty levels and random events, each tied to a real NASA example.
- Use the `economy-designer` agent to balance caps across difficulty levels.
- Full keyboard pass, contrast check, play-test with the target age group.

## Stretch (only after M4 works)

| Item | Needs |
|---|---|
| Pass-and-play for up to four players | Player setup screen, turn hand-off on the existing stations |
| Arabic | Translated `messages` bundle, right-to-left layout check |
| Separate solar-array and radio parts | Figures from NASA's Small Spacecraft Technology report |
| More rockets and orbits | Published capacities for polar orbits (see `data-sources.md`) |
| 3D orbit view | JavaFX built-in 3D first; LWJGL/OpenGL only if that falls short |
| Rendered showpiece art | Blender (connected) for title and launch scenes |
| Moon and planetary missions | New objectives, propulsion and deep-space comms rules |

## Working method

- Tests are written before the code they test.
- Engine and data never depend on JavaFX.
- One milestone at a time; the roadmap is revised at the end of each.
- Open decisions are raised with the team before they are built.
