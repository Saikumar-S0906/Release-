# Crop handle snap — device drag green; unit math green

Crop handles stick to the photo edge when the drag is within 2% (`CropEdgeSnap`) and that edge is still legal (minimum span stays 0.15). A newly acquired bound uses the same long-press haptic as crop Apply. The stuck edge is drawn in the accent color.

**Unit:** `CropSnapTest` (snap, release, min span).

**Maestro (Pixel_7a `emulator-5556`, 2026-09-26):** `.maestro/crop/crop-edge-drag.yaml` — **green**. Opens Crop, asserts four edge a11y labels + `session_crop_edge_*`, swipes the right edge, Apply → export chrome. Does not assert accent stroke or haptic; those stay visual/device.
