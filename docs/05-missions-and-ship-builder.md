# LIFTOFF — Direction v2: Three Missions and the Modular Ship Builder

Written 2026-11-13. **This document supersedes** the objective list, the screen
layouts and the colour palette in documents 01 and 02. The rules engine, data
rule and architecture in 01, 03 and `data-sources.md` still stand.

Nothing in this document is implemented yet. It is the specification the five
members build from (`06-team-work-split.md`).

## 1. What changes

| Before (v1) | Now (v2) |
|---|---|
| Three Earth-orbit objectives of similar difficulty | Three missions of rising difficulty: Earth, Moon, Mars |
| Pick a bus from a list | Build the ship from modules dragged onto a main body |
| Flat cartoon on a night-sky background | 3D ship in a blue hangar, glowing cyan panels, orange action buttons |
| Six resources | Six resources **plus fuel**, which the engine works out from the ship's mass |

## 2. The three missions

Difficulty rises by shrinking the player's leeway, not by adding trick
questions: fewer designs close, margins get thinner, and more modules interact.

| | 1 — Farm Watch (easy) | 2 — Moon Landing (medium) | 3 — Mars Sample Drones (hard) |
|---|---|---|---|
| Goal | Put a satellite in Earth orbit that can see single farm fields | Take astronauts to the Moon and bring them home | Land drones on Mars to collect terrain samples |
| Real model | Landsat, EO-1, CubeSat imagers | Artemis (SLS, Orion, lander) | Mars 2020 (Perseverance, Ingenuity), sample-return helicopters |
| Destination | Low Earth orbit | Lunar surface and back | Mars surface |
| Module slots that matter | Airframe, Thrusters, Power, Cargo bay, Satellite | The same five, plus crew cabin and lander in the cargo bay | The same five, plus entry shield, lander and drones |
| Leeway | Wide: many builds close, margins of 30 % or more are easy | Medium: fuel dominates; crew supplies add mass every day | Narrow: mass at landing is capped, power is scarce, comms are slow |
| New idea taught | One choice moves several gauges | Fuel grows much faster than payload (the rocket equation) | You cannot fix it after launch: everything must fit first time |
| Science duration | Months to years in orbit | Days | Months on the surface |

Storm Chasers and Ice Patrol from v1 stay in the catalog as optional extra
Earth missions; they are not part of the three-mission ladder.

## 3. Mission workflow

The same seven steps for every mission. The top stepper in the reference art
shows the four design steps.

```
 BRIEFING --> SHIP --> SUPPLIES --> REVIEW --> LAUNCH --> FLY --> REPORT
 (goal,      (five    (instruments, (all gauges, (checks,  (month  (score,
  limits)    module    stations,     warnings,   count-    or day   why,
             tabs)     fuel, crew    Confirm     down)     steps)   redesign)
                       supplies)     Build)
                         ^                                            |
                         +------------------ Redesign ----------------+
```

| Step | Player does | Engine does | Blocked until |
|---|---|---|---|
| Briefing | Reads the goal, the limits and what must be measured | Sets the caps for the mission | — |
| Ship | Chooses one part in each of the five tabs | Recomputes mass, cost, power, fuel after every change | Briefing read |
| Supplies | Chooses instruments, ground stations; on Moon/Mars, crew supplies or drones | Recomputes data, comms, power draw, consumables mass | A complete ship |
| Review | Reads every gauge and warning; presses Confirm Build | Runs every fit and budget check | No check failing |
| Launch | Watches the checks run | Launch check: mass vs rocket, fuel vs trip, orbit vs target | Build confirmed |
| Fly | Watches time pass, reacts to events | Simulation | Launch passed |
| Report | Reads score and reasons; may redesign | Score and explanation | — |

## 4. The ship builder (spec for the Ship step)

Reference: `docs/reference/2.webp`.

**Scene.** A ship standing on a lit pad in a blue hangar, Earth visible through
the window. The player can rotate the ship by dragging and zoom with the wheel.
The camera never moves on its own: changing a part does not reset the view.

**Five module tabs**, down the left: Airframe, Thrusters, Power, Cargo bay,
Satellite. Each tab offers at least four options. The current tab's options
appear as cards in the strip along the bottom, with arrows when there are more
than fit.

**Choosing a part.** Two ways, same result:
- click a card; or
- drag a card onto the ship and drop it on the matching place.

On either: the old part is removed and the new part snaps into place with a
short scale-and-settle animation (about 200 ms); the chosen card gets the cyan
ring; the detail panel on the right shows the part's name, picture, mass, power,
data and cost; the gauges update. All other chosen parts stay as they are.

**The body changes with the parts.** A larger airframe is visibly longer; more
thrusters show more nozzles; a bigger power option shows a bigger array; a
larger cargo bay moves the bay doors apart.

**Cargo bay.** Selecting the Satellite tab opens the cargo bay doors and shows
the satellite inside. A CubeSat shows its recessed camera aperture. Leaving the
tab closes the doors.

**Warnings.** Shown next to the gauge they concern, with an icon and a
sentence, never colour alone:

| Check | Example message |
|---|---|
| Satellite larger than the bay | "This satellite is too long for the bay. Pick a larger cargo bay or a smaller satellite." |
| Payload heavier than the airframe can carry | "The airframe can carry 100 kg. Your cargo is 140 kg." |
| Power draw above supply | "Your parts need 160 W. Your power system gives 115 W." |
| Not enough fuel for the trip | "This ship needs 3.2 t of fuel for the trip but its tanks hold 2.5 t." |
| Over budget | "You have spent $470M of $450M." |
| A tab with nothing chosen | "Choose a power system." |

**Confirm Build** is enabled only when every tab has a part, every fit check
passes and the ship is within budget. When disabled, hovering or focusing it
lists what is still wrong.

**Honest limit of the 3D.** JavaFX's built-in 3D gives clean low-polygon models
with flat or textured materials. It will not match the ray-traced look of the
reference picture. The hangar backdrop can be a pre-rendered image (Blender);
the ship itself is real-time 3D.

## 5. Module catalog: what each tab offers and where its numbers come from

Every option is a real vehicle or part. **No number may be typed in from
memory.** The table lists the real items to use and the document to copy each
figure from. Items marked ✔ already have verified figures in this project.

| Tab | Options (real items) | Figures needed | Source |
|---|---|---|---|
| Airframe | Mission 1: winged cargo spaceplanes — Space Shuttle Orbiter, X-37B, Dream Chaser — and expendable small buses ✔ (SORCE, EO-1, Landsat 7 class). Mission 2: Orion crew module. Mission 3: Mars 2020 cruise stage and aeroshell | Dry mass, payload mass limit, bay length and diameter | NASA Shuttle reference manual; NASA Dream Chaser page; NASA Orion Reference Guide; JPL Mars 2020 press kit |
| Thrusters | Engines with published thrust and specific impulse: Shuttle OMS (AJ10), Orion European Service Module main engine, RL10, hydrazine monopropellant thrusters, ion engine (NSTAR, flown on Dawn) | Thrust (N), specific impulse (s), engine mass, propellant type | NASA engine fact sheets; NASA State-of-the-Art Small Spacecraft Technology report, Propulsion chapter |
| Power | Body-mounted solar cells, deployable solar wings, fuel cells (Shuttle), radioisotope generator (MMRTG, about 110 W at launch) | Power (W), mass (kg), works in shadow or far from the Sun? | NASA SmallSat report, Power chapter (cells 30–34 % efficient; arrays cluster near 30 W/kg); JPL Mars 2020 press kit, Power page |
| Cargo bay | Bay sizes of the airframes above; CubeSat dispensers (3U, 6U, 12U) | Usable length, diameter, mass limit | Same as Airframe; CubeSat Design Specification rev 14.1 |
| Satellite / payload | Mission 1: 1U, 3U, 6U CubeSats (1U is a 10 cm cube of at most 2.00 kg) and small satellites carrying the imagers in section 6. Mission 2: crew of 2–4 and a lunar lander. Mission 3: rover, helicopter drones | Mass, power, size, data per orbit | CubeSat Design Specification rev 14.1; NASA CubeSat 101; JPL Mars 2020 press kit |

Figures verified so far for v2: Perseverance rover about 1,025 kg, launched on
an Atlas V 541 (JPL Mars 2020 quick facts); SLS Block 1 sends more than 27 t
toward the Moon (NASA SLS reference guide); a 1U CubeSat is at most 2.00 kg
(CubeSat Design Specification rev 14.1). Everything else in the table is still
to be copied from its source by the data owner.

The numbers on the reference picture (3 kg, 4 W, 160 MB per orbit, 650
credits) are placeholders from the mock-up, **not data**. Money stays in $M.

## 6. Instrument choices and their impact

### Mission 1 — Farm Watch (figures verified, from the NASA Earth Science Reference Handbook 2006)

The goal needs land pictures sharp enough to see 30 m.

| Instrument | Sees | Mass kg | Power W | Data Gbit/day | Cost ≈ $M | Impact |
|---|---|---|---|---|---|---|
| ALI (EO-1) | 30 m | 90 | 100 | 5.9 | 45 | Meets the goal; light enough for a medium ship; little data to send |
| ETM+ (Landsat 7) | 30 m + heat at 60 m | 425 | 244 | 2,164 | 212.5 | Meets the goal and earns the heat-map bonus; needs the largest body, about 240 contact minutes a day, and nearly half the budget |
| ASTER (Terra) | 15 m + heat at 90 m | 421 | 463 | 717 | 210.5 | Sharpest view and the bonus; the highest power draw in the catalog |
| MODIS (Terra, Aqua) | 250 m, whole Earth daily | 229 | 162.5 | 536 | 114.5 | **Does not meet the goal**: fields blur at 250 m. Teaches that "more coverage" is not "enough detail" |
| CubeSat imager (to source) | to source | ≤ 2 per U with its CubeSat | to source | to source | to source | The cheap, tiny option that makes the spaceplane-plus-CubeSat build possible |

### Mission 2 — Moon Landing (figures to source)

| Choice | Options | Impact |
|---|---|---|
| Crew size | 2, 3 or 4 | Each person adds cabin mass and daily supplies; more hands do more surface science |
| Surface stay | Days on the surface | Longer stay returns more science but adds supplies and power demand |
| Surface instruments | Seismometer, plant-growth experiment, dust/regolith analyser, sample boxes | Each adds landed mass, which multiplies into fuel; sample boxes add mass on the way home |
| Orbit instruments | Camera, laser altimeter, thermal mapper (as flown on Lunar Reconnaissance Orbiter) | Add data to send home and power draw, but no landed mass |

Sources to use: NASA Orion Reference Guide; NASA SLS fact sheet; NASA Artemis
III science instrument announcements; NASA Lunar Reconnaissance Orbiter press
kit.

### Mission 3 — Mars Sample Drones (figures to source)

| Choice | Options | Impact |
|---|---|---|
| Sample collector | Rover with drill and sample tubes; fetch helicopters | The rover is heavy but collects more; helicopters are light but carry little each trip |
| Scout drone | Ingenuity-class helicopter | Small mass, finds safe routes, uses battery recharged by its own panel |
| Rover instruments | Mast camera, laser chemistry camera, X-ray rock analyser, ground radar, weather station (as on Perseverance) | Each adds landed mass and draws from about 110 W of generator power; more instruments mean better-chosen samples |
| Power | Solar panels or radioisotope generator | Solar is lighter but fades with dust and distance; the generator works day and night |
| Relay | Direct to Earth, or through a Mars orbiter | Direct is slow and rare; a relay returns far more data per day |

Sources to use: JPL Mars 2020 Launch Press Kit (quick facts, spacecraft,
power, science pages); NASA Ingenuity fact sheet; NASA Mars Sample Return
pages.

## 7. Fuel: the new calculation

Fuel is worked out, never chosen directly. For each engine burn:

```
propellant = dry mass x ( e^( delta-v / (Isp x g0) ) - 1 )      g0 = 9.80665 m/s^2
```

- `Isp` (specific impulse, seconds) comes from the chosen thruster's source.
- `delta-v` (the speed change the trip needs, m/s) is a mission constant taken
  from a NASA source for that trip.
- The propellant is added to the Mass gauge and its cost to Budget.
- If the tanks of the chosen airframe hold less than the trip needs, the build
  fails the fuel check.

This is why Mission 2 is harder than Mission 1: every kilogram landed on the
Moon costs several kilograms of fuel at launch, and the player sees it happen.

## 8. Look and feel (replaces the palette in document 02)

Taken from the two reference pictures in `docs/reference/`.

| Token | Hex (starting point) | Used for |
|---|---|---|
| `hangar-900` | `#07122B` | Deepest background |
| `hangar-700` | `#0E2147` | Panels, cards |
| `hangar-500` | `#1B3A6B` | Raised surfaces, unselected buttons |
| `glow` | `#22E3F2` | Selection ring, active step, panel edges |
| `action` | `#FF7A1A` | The one primary button per screen (Select, Confirm Build) |
| `hazard` | `#F5B301` | Floor stripes, caution accents |
| `text` | `#FFFFFF` | Text on dark |
| `text-soft` | `#B9C7E6` | Secondary text |

- Type: a heavy, slightly slanted display face for headings in capitals; a
  clean rounded sans for body. Final fonts are the UI owner's choice, from
  open-licence families.
- Panels: dark, rounded, with a thin glowing cyan edge. The selected card has a
  thicker ring and a soft outer glow.
- One orange button per screen. Everything else is slate or cyan.
- The guide character must be our own design. The figure in the reference
  picture resembles a character style owned by someone else and must not be
  copied.
- The linked Spaceflight Simulator rocket is inspiration for how stacked
  modules read at a glance. None of its art may be reused.

## 9. Open decisions

1. A winged spaceplane fits Mission 1. For the Moon and Mars the "main body"
   should be a capsule or cruise stage, or the game would teach something false.
   This document assumes the body type changes per mission. Confirm.
2. Whether to add a third-party 3D model loader, or write a small loader of our
   own (the brief says no unnecessary dependencies; the plan assumes our own).
3. Whether costs stay as the labelled per-kilogram estimate.
