# LIFTOFF — Game Design Document

> **Updated direction (2026-11-13):** the missions, screen layouts, colours and milestone split in this document are superseded by `05-missions-and-ship-builder.md` and `06-team-work-split.md`. The rest still applies.

Team 404PlanetNotFound · NASA Space Apps Cairo 2026 · written 2026-11-13

## 1. What LIFTOFF is

LIFTOFF is a mission design game for children aged 7–13. The player designs a
real kind of NASA Earth-science mission from goal to results and discovers that
**every choice has a cost**: a better instrument returns more science, but it
also adds mass, draws power, produces data that must be sent home, and costs
money and time.

The problem it answers (deck, slide 2): existing tools teach orbits, rockets or
spacecraft one topic at a time, so students never see how the decisions affect
each other. LIFTOFF puts all of them in one loop.

**One-line learning goal:** after one mission, a player can explain in their own
words why adding something to a spacecraft changes more than one thing.

## 2. Sources of this design

| Source | What it settles |
|---|---|
| Team deck (8 slides) | The five-step loop, six resources, four roles, score, report with WHY, random events, difficulty, milestones M1–M4 |
| NASA "Design and Develop Science Missions" webinar (Mar 2025) | Four mission segments (launch, space, ground, user); "begin with the end in mind" |
| JPL "Concept Maturity Levels" paper (Wessen et al.) | The feasibility → trade space → point design loop; margins; cost class |
| NASA Earth Science Reference Handbook (2006) | Instrument and spacecraft mass, power, data rate, orbits (see `data-sources.md`) |
| Decisions taken with the team on 2026-11-13 | Solo player through four role stations; vector art drawn in JavaFX; Maven Wrapper targeting Java 21 |

No separate "detailed development plan" file was supplied, so anything not in
the deck is marked **[proposal]** below and is open to correction.

## 3. Core loop

```
1 OBJECTIVE      2 SPACECRAFT        3 LAUNCH & ORBIT     4 SIMULATE        5 REPORT
pick the goal -> bus + instruments -> rocket + orbit fit -> run the mission -> score + WHY
      ^                                                                        |
      +----------------------- "Redesign" (trade-space loop) ------------------+
```

The loop back from Report to Spacecraft is deliberate. It is the CML 2–4 loop
from the JPL paper: real teams return from a point design to the trade space
when a design does not close.

## 4. The four role stations (solo player)

One player wears four hats in turn. Each station owns a few decisions and
watches the resources those decisions drive.

| Station | Mission segment | Decisions | Resources it drives most |
|---|---|---|---|
| Mission Planner | Launch + user | Objective, orbit, rocket, science duration | Mass (limit), Budget, Time |
| Spacecraft Engineer | Space | Spacecraft bus | Mass, Power, Budget, Time |
| Science Lead | Space | Instruments | Mass, Power, Data, Budget |
| Comms & Resources | Ground | Ground stations | Comms, Data, Budget |

Stations can be visited in any order and revisited freely. Pass-and-play for up
to four players is a later layer on the same stations (roadmap, stretch).

## 5. The six resources

All six are always visible on the mission dashboard. Each gauge shows
*used / limit* and a state: **OK**, **TIGHT** (less than 15 % margin left),
**OVER**, or **not set yet**.

| Resource | Unit | "Used" is | "Limit" is |
|---|---|---|---|
| Mass | kg | bus + all instruments | what the chosen rocket can lift to the chosen orbit |
| Power | W | average power of all instruments | power the bus can supply to its payload |
| Budget | $M | bus + instruments + rocket + ground stations over the mission | the objective's budget cap |
| Data | Gbit/day | data the instruments produce per day | data the radio can send down per day |
| Comms | min/day | contact minutes needed to send the daily data | minutes per day the spacecraft is in view of the chosen stations |
| Time | months | build and test time + science duration | the objective's deadline |

Rules that keep the model honest:

- **Mass, power and data rates** come from published NASA instrument and
  spacecraft facts. Nothing is rounded for convenience.
- **Contact time is computed, not looked up.** The engine works out how long the
  chosen orbit keeps the spacecraft above each ground station's horizon. A polar
  station sees a polar orbiter on almost every lap; it never sees a tropical
  orbiter. Children discover this by trying it.
- **An orbit can only observe latitudes up to its inclination.** A 98.2°
  sun-synchronous orbit reaches 81.8°; it cannot map the pole. The real ICESat
  flew at 94° for exactly this reason.
- **Costs of buses and instruments are game estimates**, clearly labelled with
  "≈" in the game, because NASA does not publish per-component prices. Rocket
  costs are real NASA launch-service totals. See `data-sources.md`.
- **Build times and the caps on each objective are game parameters**, not NASA
  data, and are labelled as such.

## 6. Content of the first version

Three objectives, each modelled on a real mission family:

| Objective | Science question | Must measure | Real-world model |
|---|---|---|---|
| Farm Watch | How healthy are the crops of the Nile Delta, and how much water do they use? | Land imaging at 30 m or finer | Landsat, EO-1 |
| Storm Chasers | How much rain falls inside tropical storms? | Rain by microwave | TRMM |
| Ice Patrol | Are the polar ice sheets getting thinner? | Ice height by laser | ICESat |

Catalog for the first version: 10 instruments, 4 spacecraft buses, 3 orbits,
6 ground stations, 4 rockets. Every entry carries its source.

## 7. How learning is built into the mechanics

The game is not a quiz with a space skin. Each thing to learn is something the
player *does* and *sees change*.

| Idea to learn | What the player does | Object they handle | What they see | Variation |
|---|---|---|---|---|
| One choice moves many resources | Adds or swaps an instrument | Instrument card | Up to five gauges move at once, each with a +amount tag | Compare a small and a large imager |
| Margins matter | Fills a gauge to 99 % | Gauge | State turns TIGHT before it turns OVER | Random events later eat thin margins |
| Orbit decides what you can see | Picks an orbit for a polar goal | Orbit ring on Earth | Coverage band on the globe stops short of the pole | Same instrument, different orbit |
| Data must come home | Adds a data-hungry imager | Ground station pins | Data gauge overflows until stations are added | Polar vs equatorial stations |
| Rockets set the mass limit | Picks a rocket | Rocket line-up with lift bar | Spacecraft sits on a scale against the rocket's capacity | Cheap small rocket vs expensive large one |
| Missions fail for reasons | Runs the simulation | Timeline | Report names the first limit that broke and what would fix it | Redesign and rerun |

Progression inside one session: see the problem (briefing) → build freely →
watch the gauges → read why → redesign → try a harder objective.

Feedback principles:

- Every action has an immediate, visible consequence. No silent state changes.
- Failure is allowed and cheap. A failed mission always says what to change.
- Stars and scores come last and never replace the explanation.
- The astronaut guide asks one question at a time ("Your data gauge is red.
  What could bring more data home?") instead of giving the answer.

## 8. Simulation, score and report [proposal — built in M3/M4]

- **Launch check:** mass against rocket capacity for the orbit; orbit against
  the objective's latitude; budget and time against caps. A failed check stops
  the countdown and says why.
- **Simulation:** month by month over the science duration. Each month the
  spacecraft produces data, downlinks what contact time allows, and keeps or
  loses the rest. Power shortfall forces instruments to switch off part of the
  time, which reduces data honestly rather than failing outright.
- **Random events** (off at the easiest level), each tied to a real example:
  solar storm safe mode, ground station outage, instrument fault, launch slip.
- **Score, 0–100:** science returned (60), healthy margins (20), on budget and
  on time (20). Shown as one to three stars for younger players.
- **Report:** what worked, the first thing that went wrong, why, and one
  concrete suggestion. A "Redesign" button returns to the stations with the
  design intact.
- **Difficulty:** Cadet (generous caps, no events), Engineer (standard),
  Mission Director (tight caps, more events).
- **Leaderboard:** a local mission log on this computer, ranking missions, not
  children. No names are required and nothing leaves the machine.

## 9. Out of scope for the first playable version

Planetary and Moon missions, propulsion and fuel budgeting, separate solar-array
and radio parts, constellations, online play, accounts, and 3D. Each is listed
in `04-roadmap.md` with what it would need.

## 10. Open questions for the team

1. Costs: keep the labelled estimate rule, or supply real figures for buses and
   instruments if the team has a source?
2. The deck names OpenGL through LWJGL for optional 3D. JavaFX has built-in 3D
   that needs no new dependency; the roadmap proposes that instead.
3. Arabic: text is already externalised. Is an Arabic version wanted for the
   demo?
