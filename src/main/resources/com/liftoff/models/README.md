# LIFTOFF 3D hangar

Run the standalone demo from the repository root with JDK 21 or later.

```powershell
.\mvnw.cmd test-compile dependency:build-classpath '-Dmdep.outputFile=target/cp.txt'
$deps = (Get-Content target/cp.txt -Raw).Trim()
java -cp "target/classes;$deps" com.liftoff.render3d.HangarDemo
```

On Windows, if the Maven wrapper fails to quote a path containing spaces, use
`mvn` with the same arguments or run `./mvnw` from Git Bash.

Drag with the primary mouse button to orbit, scroll to zoom, and press
`1`–`5` to cycle the five modules. `B` toggles the bay. Selecting a payload
also opens it. Calm mode makes part swaps and door movements immediate.
The demo requires JavaFX hardware-accelerated 3D.

Capture the open and closed scenes and check drop targeting with:

```powershell
java -cp "target/test-classes;target/classes;$deps" com.liftoff.render3d.HangarSnapshotTool
```

## Builder integration

Embed `new HangarShipView().node()` in a resizable container. Keep the view
instance and use the methods in `ShipView` to apply the current selection and
handle drag feedback. `slotAt` uses scene coordinates, such as
`DragEvent.getSceneX()` and `getSceneY()`. Clear a slot with
`showPart(slot, null)`. The concrete view exposes `setCalmMode(boolean)`.

Each catalog `modelId` is the exact filename stem of a bundled OBJ. The demo
lists the current IDs in `HangarDemo.MODELS`. Use a consistent scale across
assets, with positive Y up and the nose toward negative Z. Center the bay and
payload in X and Z. Thruster origins are at the aft mounting plane. The
renderer measures the hull length and cargo bay bounds to place the modules
and hinges. The included meshes use stylized visual dimensions pending the
team's parts catalog and verified measurements. Model geometry supports
Wavefront positions, UVs, normal indices and polygon faces. Named `usemtl`
groups pick finishes from `ModelLibrary`.

`hangar.png` was created with the built-in ImageGen tool. The window, Earth
and floor are part of that image. The ship and illuminated pad are rendered
in JavaFX.

<details>
<summary>Backdrop generation prompt</summary>

Generate one wide landscape background image, 1536x1024 or wider, for LIFTOFF,
a high-quality stylized low-poly 3D space mission game. This is only the empty
background environment behind a real-time 3D object. Camera inside a futuristic
dark navy orbital maintenance hangar at human eye level, looking straight out
through an enormous angular metal-framed panoramic window. Rich cobalt navy
walls and bulkhead framing with restrained cyan strip lights and a handful of
amber worklights. Earth clearly visible beyond the left half of the windows,
large recognizable curved blue limb with white clouds and quiet glow, right
half mostly dark midnight starfield. Keep the center and lower middle dark and
uncluttered for compositing a live spaceship and circular platform in front.
Low plain blue steel floor recedes to the window sill around the lower quarter,
subtle paneling and atmosphere. Refined cinematic game environment with crisp
angular frames, good detail around the edges and mild floor light. Wide framing.
Absolutely no spaceship, no platform, no people, no writing, no letters, no
logos, no graphic UI, no overlay, no title.

</details>
