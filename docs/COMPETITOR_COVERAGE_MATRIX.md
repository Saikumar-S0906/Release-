# PixiJoy Studio — Competitor Coverage

This matrix compares outcomes, not feature names. Status meanings are defined in
[FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md).

## Product shell

| Category expectation | PixiJoy route | Status | Boundary |
|---|---|---|---|
| Start or reopen work quickly | Home | **LIVE** | Create, Studio shortcut, and local recents. |
| Discover editing tools | Studio | **LIVE** | Photo/Movie catalog; no marketplace or cloud catalog. |
| Manage projects | Library | **LIVE** | Filter, reopen, rename, delete, and batch photo export on-device. |

## Photo

| Category expectation | PixiJoy capability | Status | Boundary |
|---|---|---|---|
| Lightroom-style tone and color | Light, Color, Curves, Looks | **LIVE** | Local live preview and graph operations; no cloud catalog. |
| Selective color and imported LUTs | Color, Looks | **BAKED** | Apply renders the result. |
| Remini-style restoration | Quick enhance | **FALLBACK** | Fixed grade, saturation, and sharpening; no face reconstruction, deblur model, or damage recovery. |
| Generative canvas expansion | Mirror expand | **FALLBACK** | Mirrors edge pixels; it does not synthesize new scene content. |
| One-tap subject cutout | Basic cutout | **FALLBACK** | Subject mask when available, otherwise a basic corner-color key. |
| Object removal / healing | Heal | **BAKED** | Clone/ring repair, not semantic object reconstruction. |
| Content-aware fill | Fill | **BAKED** | Nearby-pixel Telea-like fill. |
| Neural inpaint | — | **UNAVAILABLE** | No neural model ships or appears as a selectable method. |
| Sky/background replacement | Compose → Sky | **BAKED** | Heuristic sky matte; flat-background fallback uses basic corner matching. |
| Perspective and liquify | Compose → Warp | **BAKED** | Local pixel transform on Apply. |
| Portrait skin treatment | Retouch → Soften | **BAKED** | Lightweight soften filter, not a portrait model. |
| Text, stickers, collage | Add | **BAKED** | Raster photo composition. |
| Layer opacity, blend, masks | Compose → Layers/Mask | **BAKED** | Local layer renderer; not PSD interchange. |

## Movie

| Category expectation | PixiJoy capability | Status | Boundary |
|---|---|---|---|
| CapCut-style timeline basics | Edit | **LIVE** | Trim, split, concat, transitions, and speed ramps. |
| Keyframed movement | Effects → Motion | **LIVE** | Preset zoom/pan motion. |
| Titles and captions | Text | **LIVE** | Timeline overlays used by preview/export. |
| Grade and scopes | Color | **LIVE** | Exposure, contrast, saturation, looks, and local scopes. |
| Audio finishing | Audio | **LIVE** | Music/voice/SFX, fades, ducking, EQ, dynamics, and limiter. |
| Green/blue screen | Effects → Chroma key | **LIVE** | Shader key with range and edge softness. |
| Motion stabilization | Effects → Steady crop | **FALLBACK** | Constant crop/zoom only; no camera-motion estimation. |
| Optical-flow / neural slow motion | Effects → Slow-mo | **UNAVAILABLE** | Available slow motion changes rate and holds frames; neural frames are locked. |
| PiP, split, grid preview | Layout | **LIVE** | Uses the export composition with Media3’s multi-input preview graph; unsupported runtime paths are labeled export-only. |
| HDR preservation | Color → HDR | **FALLBACK** | Keeps HDR only when supported; otherwise tone-maps. |

## Formats

| Format | Status | Boundary |
|---|---|---|
| JPEG, PNG, WebP | **LIVE** | Selectable photo outputs. |
| HEIC | **FALLBACK** | Uses the platform writer and falls back to JPEG on failure. |
| AVIF | **UNAVAILABLE** | No encoder and no selectable chip. |
| MP4, supported GIF flows | **LIVE** | Encoded locally. |

The unavailable rows are product gates, not roadmap claims. Do not relabel a fallback
as parity until the corresponding engine and focused tests ship.
