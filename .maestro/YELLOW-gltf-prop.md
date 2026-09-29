# YELLOW / manual — load a glTF prop

Maestro cannot drive the system document picker. Loading a prop stays manual on a device or emulator with a `.gltf` / `.glb` file.

Manual path:

1. Movie session → CG
2. Tap `movie_cg_load_gltf` (Load glTF) and pick an uncompressed `.glb` or a `.gltf` with an embedded buffer
3. Place with `movie_cg_x` / `movie_cg_y` and scale with `movie_cg_scale`

The plate is classical canvas lighting. It is not Filament and not neural. A missing file falls back to the cube stand-in. Compressed (Draco / meshopt) glTF is rejected.

Soft-emu stays yellow until a picker-free fixture exists. Do not mark this flow green from the picker tap alone.
