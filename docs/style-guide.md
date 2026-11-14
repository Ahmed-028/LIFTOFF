# LIFTOFF Style Guide: the kids hangar theme

This page is the style guide for everything the player sees. It replaces the colour tables in
`02-visual-identity.md`. The values live in two places that must always agree:

| Where | What uses it |
|---|---|
| `src/main/resources/com/liftoff/css/liftoff.css` (top of the file) | Styled controls: FXML screens, buttons, cards and chips |
| `src/main/java/com/liftoff/render/Theme.java` | Anything drawn in Java: canvas mini-games, the road map, the intro cutscene and 3D scenes |

`ThemeTest` reads the stylesheet and fails the build if a colour in `Theme` differs from its
CSS token. **To change a colour, change both, then run the tests.**

## 1. Feel

A friendly hangar for young explorers aged 7–13. The shell is a deep navy room. Each mission
recolours that room in its own world colours. The parts are bright, the corners round, and
there is room to breathe. Real NASA pictures and models stay real: we never replace them with
copied or AI-made art.

## 2. Colour

### Hangar shell

| Token | Hex | Use |
|---|---|---|
| `-navy-950` | `#050E26` | Deepest shadow, game backdrops |
| `-navy-900` | `#071330` | Window background |
| `-navy-800` | `#0D1E42` | Panels in the hangar |
| `-navy-700` | `#16305E` | Raised panels, control base |
| `-navy-600` | `#21427A` | Hover on dark controls |
| `-shell-line` | `#33609A` | Borders of bars and panels |
| `-text` | `#F3F8FF` | Text on dark |
| `-text-soft` | `#C3D3EE` | Hints on dark |

### Meaning colours

| Token | Hex | Means |
|---|---|---|
| `-cyan` | `#22E3F2` | Focus, "picked", "you are here" |
| `-orange` | `#FF7A1A` | **Go**: the one primary action on a screen; the ship's stripe |
| `-amber` | `#FFCB69` | Stars, rewards, gentle "check this" |
| `-good` | `#8BE07A` | Done, worked |
| `-alert` | `#FFACA4` | Something broke (always with a warning sign and words) |

### Mission worlds

Mission screens add `.world-earth`, `.world-moon` or `.world-mars` to their root. In Java,
use `Theme.World.of(destination)`.

| World | Top / bottom | Accent | Feel |
|---|---|---|---|
| Earth | `#0E6582` / `#08303F` | `#8BE07A` | teal, sky blue, green |
| Moon | `#2D3478` / `#121334` | `#D9DEEC` | indigo, silver, pale blue |
| Mars | `#8E3B22` / `#3A140E` | `#FF9A5A` | orange, coral, dusty red |

### Light cards

Choice cards are bright (`-card #F2F8FF`, ink `-card-ink #0B1A38`) so they stand out on the
darker panels. When a card is picked, its background turns `-card-picked`, a `-card-ring`
outline appears and a **"✓ Picked"** badge shows.

## 3. Type

| Role | Font | Size |
|---|---|---|
| Game title | Barlow Condensed Bold Italic | 104 px |
| Screen heading | Barlow Condensed Bold Italic | 40 px (32–34 px on dense screens) |
| Section title | Barlow Bold | 21 px |
| Body | Barlow | 16–18 px |
| Eyebrow (small label above a title) | Barlow Bold | 14 px, world accent colour |

Use `Theme.display(size)`, `Theme.bold(size)` and `Theme.body(size)` for text you draw.

## 4. Shape, spacing, motion

- **Spacing scale:** 4, 8, 12, 18, 28 px (`Theme.SPACE_XS` to `SPACE_XL`).
- **Corners:** chips 12, cards 18, panels 22, pills fully round.
- **Targets:** at least 44 px (`Theme.MIN_TARGET`); primary buttons are 54 px tall.
- **Motion:** quick 180 ms, soft 420 ms; the road-map ship takes 1.6 s per stop. **Calm mode**
  (the checkbox on the busy screens) skips travel animations and slows the runner game.

## 5. Components

| Component | Style class | Notes |
|---|---|---|
| Primary action | `.button.primary` | Orange. **Only one per screen.** |
| Other actions | `.button.secondary` | Outline; dashed when disabled |
| Command bar / action bar | `.command-bar`, `.action-bar` | Rounded navy bars at the top and bottom |
| Choice card | `.choice-card` (+ `.chosen`) | The whole card toggles; it shows a tick *and* the word "Picked" |
| Info chip | `.stat-chip`, `.info-chip`, `.goal-tag` | Small rounded facts with an icon |
| Road-map stop | `.level-node` (+ `.level-done`, `.level-here`, `.level-locked`) | A round planet button with its number; its state is also written under it |
| Mini-game frame | `.game-frame`, `.game-hud` | Dark play area with a HUD of chips |
| Cutscene caption | `.cutscene-caption` | A subtitle strip, because the cutscenes have no sound |
| "More details" link | `.details-toggle`, `.card-details` | Science tool cards stay short; the link opens the rest of the card in place |
| Over-limit tips | `.gauge-fix`, `.tips-tooltip` | "⚠ Over limit · How to fix" on a gauge; hover or press it for part swaps that bring it back down |
| Mini-game difficulty | `.difficulty-tag` | ★☆☆ Easy (Farm Watch), ★★☆ Medium (Moon), ★★★ Hard (Mars) |

## 6. Rules for every screen

1. Colour is never the only signal: add a word or symbol (✓ done, 🔒 locked, ★ star, ⚠ warning).
2. Use one orange primary action per screen.
3. Everything works from the keyboard. Every button has an accessible name and a tooltip if
   its label is short.
4. Short, friendly sentences. Say what to do next.
5. New text goes in `messages.properties`, never hard-coded.
6. Mini-games and the cutscene are just for fun: they never change catalog numbers, engine
   results or the mission outcome.
