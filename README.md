# LIFTOFF — The Space Mission Design Game

An educational mission design game for ages 7–13, built for NASA Space Apps
Cairo 2026 by team 404PlanetNotFound. Java 21, JavaFX 21, Maven.

## Run

You need a JDK (21 or newer). Maven is fetched by the wrapper on first use.

```bash
./mvnw javafx:run
```

```bash
./mvnw test
```

On Windows PowerShell or cmd, use `mvnw.cmd` in place of `./mvnw`.

## Where things are

| Path | What |
|---|---|
| `docs/01-game-design.md` | Game design document |
| `docs/02-visual-identity.md` | Colours, type, screen layouts |
| `docs/03-architecture.md` | Layers, module interfaces, formulas |
| `docs/04-roadmap.md` | Milestones M1–M4 |
| `docs/05-missions-and-ship-builder.md` | Current direction: three missions, workflow, modular 3D ship builder, look |
| `docs/06-team-work-split.md` | Who builds what: five members, classes, data and source tasks |
| `docs/PROJECT-CONTEXT.md` | Summary of the project so far, to paste into a new chat |
| `docs/reference/` | Reference pictures for the look |
| `docs/data-sources.md` | Where every catalog number comes from |
| `src/main/resources/com/liftoff/catalog/` | Gameplay data (JSON) |
| `src/main/java/com/liftoff/` | `model`, `engine`, `data`, `session` (no JavaFX); `app`, `ui`, `render` (JavaFX) |

## Checking the screens without clicking through

After `./mvnw test-compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt`,
run `com.liftoff.dev.SnapshotTool` from the test classes with that classpath;
it writes a PNG of every screen to `target/snapshots/`.

## Guard rails for changes

| Check | What it protects |
|---|---|
| `GoldenEngineTest` | Every engine number, in full precision, for hundreds of seeded designs and flights. A refactor must leave `src/test/resources/com/liftoff/golden/` untouched; to accept a deliberate change of rules, run with `-Dgolden.update=true` and review the diff |
| `PerformanceBudgetTest` | Generous time budgets for builds, flights, gauges, model copies and catalog loading |
| `LeakTest` | Opens every screen 100 times; listeners and node counts must stay flat and closed screens must be collectable |
| `com.liftoff.dev.PerfProbe` | Run like `SnapshotTool`: times start-up, opening the builder, part swaps and flights. `-Dprobe.readMs=1000` waits on the briefing as a player would |
| `com.liftoff.render3d.ModelSimplifier` | Brings a model file under the 5,000-triangle budget |
