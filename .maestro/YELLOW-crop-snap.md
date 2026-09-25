# Crop handle snap — yellow / manual

Crop handles stick to the photo edge when the drag is within 2% (`CropEdgeSnap`) and that edge is still legal (minimum span stays 0.15). A newly acquired bound uses the same long-press haptic as crop Apply. The stuck edge is drawn in the accent color.

Unit coverage: `CropSnapTest` (snap, release, min span). No device or Maestro run in this change — a drag on `session_crop_overlay` needs a finger path, and this note does not claim that flow green.
