# LIFTOFF — Project Context (paste this into a new chat)

A record of the working session of 2026-11-13 that created this repository:
what was asked, what was decided, what was built, and what is still open. It is
a structured account, not a word-for-word transcript.

## 1. The project

LIFTOFF is an educational space mission design game for children aged 7–13,
built by team 404PlanetNotFound for NASA Space Apps Cairo 2026 (13–14 November
2026). The player picks a mission, builds a spacecraft, launches it, flies the
mission and reads a report that explains why it succeeded or failed. The point
is that every choice has a cost across several resources at once.

## 2. Fixed requirements (from the project brief)

- Java 21, JavaFX 21, Maven, FXML and JavaFX CSS, JSON catalogs, Jackson,
  JUnit 5. No other application framework.
- Six resources: Mass (kg), Power (W), Budget ($M), Data (Gbit/day), Comms
  (min/day), Time (months). v2 adds fuel, worked out by the engine.
- Scientific accuracy: never invent a value; use NASA-derived data with source
  references; do not simplify to the point of teaching something false.
- Clean modular architecture: model, engine, data loading, UI, rendering,
  utilities, tests. Model and engine must not depend on JavaFX. A central
  `GameSession` connects UI and logic. Gameplay data lives in JSON.
- Work incrementally; do not over-engineer; no unnecessary dependencies; no
  decorative-only UI; never claim something works without testing it; ask
  before major design decisions the spec does not cover.

## 3. Inputs that were supplied

- Team deck `LIFTOFF_SpaceApps_Cairo_2026.pptx` (8 slides): the loop Objective
  → Spacecraft → Launch & Orbit → Simulate → Report; six resources; four roles
  (Mission Planner, Spacecraft Engineer, Science Lead, Comms & Resources);
  score, leaderboard, report with WHY, random events, difficulty; Java + JavaFX
  with optional OpenGL 3D; milestones M1 data model, M2 UI, M3 simulation, M4
  test and polish.
- NASA webinar "Design and Develop Science Missions" (March 2025): mission
  segments (launch, space, ground, user); begin with the end in mind.
- JPL paper "Space Mission Concept Development Using Concept Maturity Levels":
  the feasibility → trade space → point design loop.
- NASA Software Catalog 2025–26 and the `nasa/mission-viz` repository: reference
  only, no reusable code.
- Two reference pictures for the v2 look (`docs/reference/`).
- No separate "detailed development plan" file was ever supplied.

## 4. Decisions taken

| Decision | Choice |
|---|---|
| Player mode | Solo player moving through four role stations; pass-and-play later |
| Art (v1) | Vector art drawn in JavaFX |
| Build | Maven Wrapper; compile for Java 21 on the installed JDK 23 |
| Costs | Bus and instrument costs are labelled game estimates ($0.15M and $0.5M per kg); rocket costs are real NASA launch totals |
| Commits | Authored by Omar only, with no assistant attribution |
| Direction v2 (later in the session) | Three missions of rising difficulty (Farm Watch satellite; crewed Moon landing; Mars sample drones); modular drag-and-drop ship builder with a rotatable 3D ship in a blue hangar; five tabs (Airframe, Thrusters, Power, Cargo bay, Satellite); work split across five members; missions are planned, not implemented |

## 5. What exists in the repository

- Documents: `01-game-design`, `02-visual-identity`, `03-architecture`,
  `04-roadmap`, `05-missions-and-ship-builder` (v2 direction),
  `06-team-work-split`, `data-sources`.
- Code (Milestone 1): model records, strict JSON catalog loader, `OrbitMath`,
  `ResourceCalculator` for the six resources, `GameSession`, a JavaFX shell with
  title and mission-select screens, and 73 passing tests, including a test that
  fails the build if logic packages import JavaFX.
- Catalogs: 3 objectives, 10 instruments, 3 buses, 3 orbits, 6 ground stations,
  4 rockets, each entry with a source.

Run with `./mvnw javafx:run`; test with `./mvnw test`.

## 6. Where the data comes from

Main source: NASA *Earth Science Reference Handbook* (2006), with PDF page
numbers. Also NASA's Near Space Network page (ground stations), NASA Launch
Services Program page, United Launch Alliance and Rocket Lab pages (rocket
capacity), and NASA launch-contract releases for Landsat 9, ICON and PACE
(rocket cost). Full table and arithmetic: `data-sources.md`.

Engine facts worth knowing: contact time with a ground station is computed
from orbit geometry (Svalbard sees the 705 km sun-synchronous orbit about 137
minutes a day and never sees a 35° orbit); an orbit only observes latitudes up
to its inclination (the 98.2° orbit reaches 81.8°, so it cannot map the pole).

## 7. Tools found on the development machine

Installed and used: Superpowers (planning, test-first), UI/UX Pro Max (visual
direction, JavaFX guidance), Gamified Learning Design (learning-mechanic
mapping). Installed, not yet used: `systems-designer` and `economy-designer`
agents, Blender. Installed, not applicable: Three.js skills (web only).
Not installed: Context7. JDK 23 only; no system Maven; PDFs had to be read with
a Python text extractor.

## 8. Still open

1. Whether the main body changes per mission (spaceplane for Earth, capsule for
   the Moon, cruise stage for Mars). The v2 spec assumes yes.
2. Real cost figures for buses and instruments, if a source exists.
3. All Moon and Mars figures, and the spaceplane, thruster and power options:
   listed row by row in `06-team-work-split.md`, section 3.
4. Polar launch capacity for Falcon 9 and Pegasus XL.
5. Fonts for the v2 look; whether to write our own 3D model loader.
6. An Arabic version.

## 9. How to continue in a new chat

Give the assistant this file plus `05-missions-and-ship-builder.md` and
`06-team-work-split.md`, say which member you are, and ask it to work only in
that member's folders, tests first, with every number sourced.
