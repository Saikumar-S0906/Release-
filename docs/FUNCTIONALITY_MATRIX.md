# PixiJoy Studio — Functionality Matrix

Current product truth for the shipped three-tab shell: **Home · Studio · Library**.
Statuses describe the implementation, not category-level intent.

| Status | Meaning |
|---|---|
| **LIVE** | The control and its stated result work in the editor or local project flow. |
| **BAKED** | Apply/export renders the operation into pixels or media; continuous preview is not promised. |
| **EXPORT_ONLY** | The result is intentionally produced only by export. |
| **FALLBACK** | A narrower local implementation is used and is named below. |
| **UNAVAILABLE** | No working engine ships in this build; the capability is not selectable. |

## Shell

| Surface | Status | Current behavior |
|---|---|---|
| Home | **LIVE** | Create source sheet, Studio shortcut, and local recent-project cards. |
| Studio | **LIVE** | Photo and Movie tool catalogs; all editing stays on-device. |
| Library | **LIVE** | Local photo/movie filters, reopen, rename, delete, and batch photo export. |
| Project persistence | **LIVE** | Source, graph, cover, title, and timestamps are stored locally. |

Evidence: `HomePane.kt`, `StudioPane.kt`, `LibraryPane.kt`, and `StudioApp.kt`.

## Photo Studio

| Capability | Status | Exact implementation |
|---|---|---|
| Crop, rotate, flip | **LIVE** | Non-destructive graph operations with undo/redo. |
| Exposure, contrast, hue, saturation | **LIVE** | Live grade/HSL preview; Apply records the operation. |
| Curves and bundled looks | **LIVE** | Live preview; Apply records curves or a look. |
| Selective color, imported LUT, sky, warp, soften | **BAKED** | Apply renders the selected operation. |
| Quick enhance | **BAKED** | Fixed grade, saturation, and unsharp-mask pass. It is not photo restoration. |
| Mirror expand | **BAKED** | Extends the canvas with mirrored edge pixels. It is not generative fill. |
| Basic cutout | **FALLBACK** | Uses the bundled subject mask when available; otherwise removes colors matching sampled corners. |
| Heal | **BAKED** | Clone/ring-based local repair. |
| Fill | **BAKED** | Nearby-pixel Telea fill. That Apply stays on and is not neural. Neural Apply stays off until `PackRuntime.engineReady` after a sideloaded RGB+mask FLOAT32 bind. Ready (file verified) is not neural. No HTTPS inpaint pin. |
| Super-res | **PACK** | Distilled ESRGAN HTTPS pin in `PackCatalog` (`compressed_esrgan.tflite`). Ready is downloaded and SHA-verified only. Apply turns on only for `PackRuntime.engineReady` (FLOAT32 2×/4× NHWC). Ready without that bind leaves the photo unchanged. |
| Denoise | **PACK** | Raspberry Pi UNet HTTPS pin in `PackCatalog` (`unet_rgb_fast.tflite`). Ready is not neural. Apply turns on only for `engineReady` (FLOAT32 same-size RGB). |
| Text, stickers, collage | **BAKED** | Rasterized into the rendered photo. |
| Layers, blend, opacity, masks | **BAKED** | Evaluated by the local layer renderer. |
| Presets | **LIVE** | Edit graphs are saved and reapplied on this device. Library can batch-apply a saved look, then export locally. |
| Fit + spot preview | **LIVE** | Everyday editing uses a 1600px fit preview; heal/mask inspection at 2× or more requests a 4096px spot render. |

Evidence: `photo/BitmapPhotoEngine.kt`, `PhotoFilters.kt`, `ContentAwareFill.kt`,
`PackEnhance.kt`, `Layer.kt`, and `app/.../PhotoSessionScreen.kt`.

## Movie Studio

| Capability | Status | Exact implementation |
|---|---|---|
| Trim, split, concat, transitions, speed ramp | **LIVE** | Timeline state previews and exports through Media3. |
| Motion presets, titles/captions, grade/looks | **LIVE** | Stored in the timeline and used by preview/export. |
| Music, voice, SFX, fades, ducking, EQ/dynamics | **LIVE** | Local mix controls feed preview/export. |
| Chroma key | **LIVE** | Green/blue shader key with range and edge softness. |
| Steady crop | **LIVE** | Constant digital zoom hides frame edges; there is no motion analysis. |
| Slow motion | **LIVE** | Playback-rate change with frame hold. Neural frames stay locked until the interp pack engine is bound. |
| Point track / roto-lite | **LIVE** | Bundled `track.tflite` (~2 MB) in APK. Follow bakes pan/zoom when LiteRT binds; unbound shows “Coming with pack”. |
| PiP, split, grid | **LIVE** | The export composition previews through Media3’s multi-input graph. Multi-sequence seeking stays disabled; unsupported device/runtime paths show an explicit export-only fallback. |
| Keep HDR | **FALLBACK** | Preserves HDR when the device/export path supports it, otherwise tone-maps safely. |
| Talking still | **BAKED** | Ken Burns-style GIF encoded on-device. |

Evidence: `movie/ClipFrame.kt`, `ClipSlowMo.kt`, `ClipTrack.kt`, `MovieEngine.kt`, and
`app/.../MovieSessionScreen.kt`.

## Export

| Output | Status | Current behavior |
|---|---|---|
| JPEG, PNG, WebP | **LIVE** | Selectable photo formats. |
| HEIC | **FALLBACK** | Uses HeifWriter when available; a failed/unsupported encode becomes JPEG. |
| AVIF | **UNAVAILABLE** | No encoder ships; AVIF is not shown as a format chip. Legacy requests resolve to JPEG. |
| MP4 | **LIVE** | Movie export runs as background work. |
| GIF | **LIVE** | Available for supported still-motion flows. |

## Hard truth gates

| Capability | Status | Gate |
|---|---|---|
| Neural inpaint | **PACK** | Sideload only. No HTTPS pin and no Download CTA. Ready is the verified file (`InpaintStatus.ReadyFileOnly`). Neural Apply reads `engineReady` after `PackLiteRt` binds FLOAT32 RGB+mask (4-channel or image+mask). A miss leaves Telea. Telea is never labeled neural. |
| Neural super-res | **PACK** | HTTPS SHA pin → Ready → FLOAT32 `PackModelContract.superResScale` → `PackLiteRt` register. A miss releases the interpreter. Apply reads `engineReady`. Ready alone is not neural. |
| Neural denoise | **PACK** | HTTPS SHA pin → Ready → FLOAT32 `isDenoiseSameRgb` → `PackLiteRt`. A 2×/4× graph cannot register. Apply reads `engineReady`. |
| Neural frame interpolation | **PACK** | HTTPS FILM TFLite (~67 MB). Ready = verified file. Neural chip + export mux unlock when LiteRT binds the FILM FLOAT32 contract. Export synthesizes mid-frames (`NeuralSlowMoMux`); preview stays Media3 rate/hold. Synth failure falls back to frame-hold. |
| Point track / roto-lite | **LIVE** | Bundled APK asset → Ready → FLOAT32 `PackModelContract.isPointTrack` → `PackLiteRt.bindTrack`. `TrackPathBake` applies follow when `engineReady`. No HTTPS Download. |
| True motion stabilization | **UNAVAILABLE** | No gyro/optical-flow tracking; the available control is named Steady crop. |
| Generative expand | **UNAVAILABLE** | Mirror expand is the only edge-extension engine. |

When behavior changes, update this matrix and the focused capability test in the same change.
