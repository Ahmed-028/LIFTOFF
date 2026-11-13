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
