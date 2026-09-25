# R4 studio UX

Pixel_7a (`emulator-5556`) on 2026-09-23. Baseline shots are the build that was already installed. After shots are the grouped chrome in this change.

The live product is the three-tab studio (Home, Studio, Library). Explore, Community, and Create → AI are not on this shell, so they were not captured.

## What the screenshots showed

Shell shots: `docs/ui_review/r4_baseline/.maestro/tests/2026-09-23_211719/baseline-shell/takeScreenshot/`.

- Home is a dark canvas, one Create button, and an empty Recents line. Most of the screen is unused. That is fine for an empty library. It is not the editor problem.
- Studio Photo listed Editor and Cutout twice: once under Recommended and again in the grid. Subtitles such as “Best for: remove the background” were cut off at the card edge. The grid now skips tools already in Recommended, and tile subtitles ellipsize at two lines.
- Library is the third tab. There is no Explore or Community tab on this build.

Photo editor shots: `docs/ui_review/r4_baseline/.maestro/tests/2026-09-23_212942/baseline-photo/takeScreenshot/` (`photo_crop`, `photo_adjust`, `photo_curves`, `photo_selective_hsl`, `photo_looks`, `photo_sky`). Later rail items (`session_warp` and after) did not scroll into view before the flow stopped. The pattern is the same on every shot that did land.

- On Adjust, the portrait is a short band. Exposure, contrast, hue, saturation, and blur fill the rest, and the tool names still scroll sideways (Crop, Adjust, Curves, Color, Looks…).
- A full-width Export button sits under that rail.
- The top row crushes “Saved” into a one-letter column and clips the compare sentence against Undo and Redo. The title is squeezed out. That row is now two lines: Back / title / Export, then Undo / Redo / compare / save status, each on one line.

Movie shots: `docs/ui_review/r4_baseline/.maestro/tests/2026-09-23_213321/baseline-movie/takeScreenshot/` (`movie_sheet_top`, `movie_sheet_grade`). The timeline tag never scrolled into view within 40 seconds. That is the stacked sheet: tracks, edit, motion, frame, chroma, slow-mo, HDR, titles, and grade all sit in one dark list, and the playhead is buried under them.

## Decision

Same pattern as Lightroom (one category, a few sliders) and CapCut (one tool group, timeline stays put):

- The photo or video keeps `weight(1f)`.
- Six groups, one open at a time, in a sheet at most 220 dp.
- The sheet is `PixiBg` at 72% opacity. Compose blur would smear the controls themselves, so the sheet is translucent and sits under the canvas. Crop handles stay on the photo, not under the sheet.
- Export is a top-bar button. The full-width Export button is gone.
- Movie transport (scrub, play, frame step, clip strip) is pinned under the video. It does not scroll away with the tools.
- PiP / Split / Grid use the same `Composition` as export with Media3’s multi-input video graph. If the device/runtime rejects that graph, the editor removes the misleading preview and labels the layout export-only.

Photo groups: Crop, Light, Color, Retouch, Compose, Add.
Movie groups: Edit, Audio, Text, Color, Effects, Layout.

Clip volume, fades, and mute stay on Edit, next to trim. Audio holds ducking, music, voice, and the master strip.

## Live update

Photo Light, hue/saturation, curves (API 33+), and bundled looks still update the image while the slider moves. Apply writes the graph op. Selective HSL, sky, warp, soften, and imported LUTs still bake on Apply. The selective hint already says Apply bakes. No downscale was added.

Movie preview used to call `setComposition` + `prepare()` on every timeline encode, including while a slider was down. Drags now update the graph without rebinding. The player rebinds when the gesture ends, or immediately for a structural edit (split, transition chip, layout, title, motion preset). Playhead is restored for single-video sequences; seeking stays guarded for Media3’s known multi-video limitation.

## Functional fixes in this change

1. Movie preview no longer restarts on every slider tick. Export still saves the graph on each change.
2. Blur, sharpen, and vignette chips that baked a fixed amount are gone. The Light sliders are the only control. Their test tags are `session_blur`, `session_sharpen`, and `session_vignette`.
3. Layout group states that multi-layout applies on export.
4. Studio no longer repeats Recommended tools in the grid.
5. Photo header no longer collapses the save status into a vertical stack of letters.

## Deferred

- Typing a slider number landed on the shared live-value slider (photo Light and Color). Short-tap types, long-press resets. Curves still uses the plain slider.
- Library cover skeletons landed (G6). Empty library and pack download use the same glass plate (G7).
- Rename after export (G8) — landed: tappable project title, and Name this project on export success.
- Live PiP preview when `CompositionPlayer` can take more than one input.
- Selective HSL on the GPU after a Pixel_7a frame-time measurement (B4).
- LiteRT pack engines, and ViewModel extraction.

## After (glass overlay polish)

Pixel_7a (`emulator-5556`), same day after the competitive chrome pass.

Photo: `docs/ui_review/r4_after/.maestro/tests/2026-09-24_192122/after-photo/takeScreenshot/`.
Movie: `docs/ui_review/r4_after/.maestro/tests/2026-09-24_192808/after-movie/takeScreenshot/`.

What changed on top of the first grouped chrome:

- The picture fills the stage. Controls sit in a bottom glass stack (`StudioChromeBar`) with a fade into the media, not a solid slab under a letterboxed preview.
- The icon rail is under the panel (CapCut-style thumb reach). Six groups share the width evenly.
- Light and Curves show **one** control at a time (chips pick Exposure / Contrast / … or Blacks / Midtones / …). The panel no longer scrolls through five stacked sliders.
- Hints are one short line. Movie Edit reads “Roll, dissolve, or ramp speed.”
- Histogram is a small glass strip at the top of the photo, not between the image and the tools.
- Crop pads the fit box so handles stay above the glass.

Honest remaining gaps vs Lightroom / CapCut: true backdrop blur (Compose cannot blur the photo under controls), live PiP preview, GPU Selective HSL, and a real video sample for the timeline dock. Those stay deferred.

## Premium pass

Top and bottom chrome both fade over the picture (`StudioTopChrome` + `StudioChromeBar`, drawn above the canvas). The group rail uses an accent icon and underline instead of a solid purple tile. Light shows a large live value (tap to type, long-press to reset) and a short Apply. Layout keeps one export hint.

Glass panel max height is 220 dp so common groups fit. When content still overflows, a bottom fade with “More ⌄” appears (`studio_scroll_more`). Horizontal chip rows fade at the trailing edge when more chips sit off-screen.

Recapture: movie `docs/ui_review/r4_after/.maestro/tests/2026-09-24_195106/` (shows “More ⌄” on Edit). Photo recapture follows the same build.

## How to recapture

```powershell
.\gradlew.bat :app:assembleDebug
adb -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
maestro --device emulator-5556 test --debug-output docs\ui_review\r4_after .maestro\ui_review\after-shell.yaml
maestro --device emulator-5556 test --debug-output docs\ui_review\r4_after .maestro\ui_review\after-photo.yaml
maestro --device emulator-5556 test --debug-output docs\ui_review\r4_after .maestro\ui_review\after-movie.yaml
```
