# LIFTOFF — Visual Identity and Screen Layouts

> **Style guide:** colours, type and component rules now live in `style-guide.md` (and `Theme.java`).

> **Updated direction (2026-11-13):** the missions, screen layouts, colours and milestone split in this document are superseded by `05-missions-and-ship-builder.md` and `06-team-work-split.md`. The rest still applies.

Direction produced with the UI/UX Pro Max design-system query for a children's
educational space game (style "Vibrant & Block-based", fonts Baloo 2 / Comic
Neue, JavaFX stack guidance). Its suggested cream-and-amber palette is adapted
here to a night-sky scheme, because the game is set in space; that adaptation
is our call.

## 1. Feel

A friendly Saturday-morning space cartoon: deep blue space, chunky rounded
shapes, thick outlines, bright parts that snap onto a spacecraft. Never a
spreadsheet. Numbers are always paired with a picture or a gauge.

## 2. Colour

| Token | Hex | Used for |
|---|---|---|
| `space-900` | `#0F1535` | Window background |
| `space-700` | `#1B2559` | Panels |
| `space-500` | `#2E3A80` | Raised cards on dark, borders |
| `paper` | `#FFF8E7` | Light cards (objective, part cards) |
| `ink` | `#1B1B3A` | Text on light surfaces and on bright buttons |
| `star` | `#FFFFFF` | Text on dark surfaces |
| `mist` | `#C9D4FF` | Secondary text on dark surfaces |
| `rocket` | `#FF6B35` | Primary action button |
| `sky` | `#3EC1F3` | Secondary action, links, selection ring |

Each resource owns one colour **and** one icon, used everywhere it appears:

| Resource | Hex | Icon |
|---|---|---|
| Mass | `#FF9F1C` | weight |
| Power | `#FFD23F` | lightning bolt |
| Budget | `#3DDC97` | coin |
| Data | `#3EC1F3` | stacked discs |
| Comms | `#B388FF` | antenna dish |
| Time | `#FF6B9D` | clock |

State is never shown by colour alone: OK is a tick, TIGHT is a "!" triangle,
OVER is a cross plus a shaking gauge, and each has a word beside it.

Contrast: `star` on `space-900` is about 16:1; `ink` on `rocket` is about
5.6:1; `ink` on every resource colour is above 4.5:1.

## 3. Type

- Headings and buttons: **Baloo 2** (700). Body: **Comic Neue** (400/700).
  Both are open-licence Google Fonts and will be bundled in M2. Until then the
  CSS falls back to a rounded system font.
- Base size 18 px, never below 16 px. Headings 28–56 px.
- Short sentences. One idea per bubble. Scientific words appear with a plain
  explanation the first time ("orbit — the path a spacecraft loops around
  Earth").

## 4. Shape, spacing, motion

- Corner radius 20 px on cards, 28 px on primary buttons. Outlines 3 px.
- Spacing scale 8 / 16 / 24 / 32 / 48 px, set in CSS, never ad hoc.
- Minimum button size 56 px high, 16 px apart.
- Motion always means something: a part flies to the spacecraft when added, a
  gauge fills to its new value, an overloaded gauge shakes. Interface
  transitions take 150–250 ms. A "calm mode" setting turns decorative motion
  off; nothing requires motion to be understood.

## 5. Art

All art is vector, drawn in JavaFX from shapes and SVG paths and coloured from
the tokens above. Parts are separate pieces so the spacecraft on screen is
always the spacecraft the player built: adding an instrument adds its picture.

Recurring cast: the rocket, the spacecraft (bus + parts), Earth with an orbit
ring, ground-station dishes, and Nova, the astronaut guide.

## 6. Screens

**1. Title**

```
+--------------------------------------------------------------+
|        *        .            *                .         *    |
|                     L I F T O F F                            |
|               The Space Mission Design Game                  |
|                                                              |
|                       [rocket art]                           |
|                                                              |
|                  ( Start a mission )                         |
|   Sources                                        Calm mode   |
+--------------------------------------------------------------+
```

**2. Mission select** — three objective cards; each shows a picture, the science
question in one sentence, what must be measured, and its budget and deadline.

```
+--------------------------------------------------------------+
| < Back            Choose your mission                        |
| +----------------+ +----------------+ +----------------+     |
| | [farm icon]    | | [storm icon]   | | [ice icon]     |     |
| | Farm Watch     | | Storm Chasers  | | Ice Patrol     |     |
| | question...    | | question...    | | question...    |     |
| | coin 350  clock| | coin 200  clock| | coin 650  clock|     |
| | ( Choose )     | | ( Choose )     | | ( Choose )     |     |
| +----------------+ +----------------+ +----------------+     |
+--------------------------------------------------------------+
```

**3. Mission control** — the main screen. Stations on the left, the spacecraft
being built in the centre, the six gauges always visible on the right, the guide
along the bottom.

```
+--------------------------------------------------------------+
| Farm Watch                                   ( Launch check )|
| +----------+ +---------------------------+ +---------------+ |
| | Planner  | |                           | | Mass   ###--  | |
| | Engineer*| |     [spacecraft art]      | | Power  ##---  | |
| | Science  | |                           | | Budget ####-! | |
| | Comms    | +---------------------------+ | Data   #----  | |
| +----------+ | part cards for the station| | Comms  ##---  | |
|              | [ALI] [MODIS] [ASTER] ... | | Time   ###--  | |
|              +---------------------------+ +---------------+ |
| (Nova) "Adding MODIS used a lot of data. What could help?"   |
+--------------------------------------------------------------+
```

**4. Launch & orbit** — Earth with selectable orbit rings and the coverage band
each gives; a rocket line-up, each with a lift bar against the spacecraft mass.

**5. Simulation** — the spacecraft circling Earth, a month timeline, a counter
of science data returned, event cards when something happens.

**6. Mission report** — stars and score, three "why" cards (what worked, what
broke first, what to try), buttons for Redesign and New mission.

**7. Sources** — every catalog entry with its NASA reference and whether the
value is published, derived, or a game estimate.

## 7. Rules for every screen

- One primary action per screen, always in `rocket` orange, always bottom-right
  or centre.
- Back is always top-left and always returns to the previous screen.
- Every icon-only control has a text label or tooltip and an accessible name.
- Everything works with keyboard alone; focus is visible as a `sky` ring.
- No element is decorative only: if it is on screen, it shows state or does
  something.
