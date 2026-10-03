# PixiJoy Studio — QA Stack

QA targets the current **Home · Studio · Library** product. Automated execution is
not a claim of complete defect detection or creative-quality validation.

Product status terms (**LIVE**, **BAKED**, **EXPORT_ONLY**, **FALLBACK**,
**UNAVAILABLE**) are defined in
[FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md).

## Fast local checks

Run focused JVM gates first:

```powershell
.\gradlew.bat :photo:testDebugUnitTest `
  --tests "com.pixijoy.photo.ExportExifPolicyTest" `
  --tests "com.pixijoy.photo.PhotoOpFillTest" `
  --tests "com.pixijoy.photo.LayerStackTest"

.\gradlew.bat :movie:testDebugUnitTest `
  --tests "com.pixijoy.movie.ClipSlowMoTest" `
  --tests "com.pixijoy.movie.ClipFrameTest"

.\gradlew.bat :app:testDebugUnitTest `
  --tests "com.pixijoy.studio.ui.StudioLayoutTest"
```

Compile the visible capability gates:

```powershell
.\gradlew.bat :app:compileDebugKotlin
```

## Truth-gate coverage

| Product assertion | Expected status | Automated evidence |
|---|---|---|
| Quick enhance is grade/saturation/sharpen | **BAKED** | `LayerStackTest` label contract; photo engine tests cover graph/render primitives. |
| Mirror expand names mirrored edge extension | **BAKED** | `LayerStackTest`; `PhotoFilters` implementation. |
| Basic cutout does not promise ML parity | **FALLBACK** | Layer label contract; subject-mask path falls back to corner matching. |
| Neural inpaint does not ship | **UNAVAILABLE** | `PhotoOpFillTest` checks the capability gate. |
| Slow motion uses frame hold | **LIVE** | `ClipSlowMoTest` checks rate, duration, and engine resolution. |
| Neural frame interpolation does not ship | **UNAVAILABLE** | `ClipSlowMoTest` checks the base capability and heuristic resolution. |
| Steady crop is digital zoom | **LIVE** | `ClipFrameTest` checks preset mapping and frame effects. |
| AVIF is not selectable | **UNAVAILABLE** | `ExportExifPolicyTest` asserts the exact format-chip list and legacy JPEG fallback. |
| Selective HSL stays CPU | **LIVE** | `StudioCapabilityTest` keeps the GPU path gated until jank is measured. |
| Batch look apply | **LIVE** | `BatchPresetApplyTest` copies a preset graph onto destination projects. |
| Room schema v1 | **LIVE** | `StudioSchemaTest` requires the exported baseline schema. |

## Device and visual checks

These require an installed debug APK and emulator/device:

```powershell
maestro --device emulator-5556 test .maestro\ui_review\after-shell.yaml
maestro --device emulator-5556 test .maestro\ui_review\after-photo.yaml
maestro --device emulator-5556 test .maestro\ui_review\after-movie.yaml
maestro --device emulator-5556 test .maestro\movie\d8-stabilize-reframe.yaml
maestro --device emulator-5556 test .maestro\movie\d10-slowmo-interp.yaml
```

Review generated images under `docs/ui_review/r4_after/`; screenshot capture is
evidence for human review, not a pixel-diff assertion.

## Manual product-truth checklist

1. Home shows Create, Browse tools, and local recents; bottom tabs read Home, Studio, Library.
2. Studio photo cards read Quick enhance, Mirror expand, and Basic cutout.
3. Quick enhance changes tone/color/sharpness without claiming reconstruction.
4. Mirror expand visibly repeats edge content rather than inventing content.
5. Movie Effects names the zoom control Steady crop and explains that it does not track motion.
6. Slow-mo labels Frame hold; Neural frames is visibly unavailable and cannot be selected.
7. Fill says it uses nearby pixels and does not expose a neural method.
8. Photo export offers JPEG, PNG, WebP, and HEIC as supported/fallback permits; AVIF is absent.
9. PiP/split/grid copy states that layout applies on export.
10. Library reopen, rename, delete, filters, and batch photo export operate on local projects.

## Release and acceptance matrix

| Gate | How it is run | Status |
|---|---|---|
| JVM unit | `.\gradlew.bat testDebugUnitTest` | Blocking in `.github/workflows/qa.yml` |
| Detekt | `.\gradlew.bat :app:detekt` | Blocking in CI |
| Instrumented smoke | `StudioAppSmokeTest` on API 34 emulator | Blocking in CI |
| Maestro | Existing `.maestro` tags for shell, photo, and movie | Device/local |
| Minified release | `.\gradlew.bat :app:assembleRelease` | Blocking in CI |
| Performance | Reject >10% regressions on cold start, editor-open, slider jank, memory peak, export | Device |
| Accessibility | TalkBack reachability, 48dp targets, 12sp labels, no color-only state | Device |
| Product acceptance | Five users find before/after, undo a movie trim, heal while zoomed, and export without coaching | Manual |

## Validation limits

- JVM tests do not validate Media3 codec/device behavior, GPU shader output, or visual quality.
- Maestro verifies reachability and semantics; final image/video quality still needs human review.
- HEIC/HDR behavior must be checked on at least one supported and one fallback device.
