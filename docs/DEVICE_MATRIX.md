# PixiJoy device & OEM matrix

Last updated: **2026-09-26**. Studio product gate (`com.pixijoy.ai`).

## Build targets

| Item | Value |
|------|--------|
| minSdk | 31 |
| targetSdk / compileSdk | 35 |
| Release ABI | `arm64-v8a`, `armeabi-v7a` (`ndk.abiFilters`) |
| Export | WorkManager + foreground notification (`pixijoy_export`) |

## Lab devices (automated)

| Device | ID | API | Last pass | Notes |
|--------|-----|-----|-----------|--------|
| Pixel_7a AVD | `emulator-5556` | 35 | 2026-09-26 | Locales, TalkBack a11y-tree, fontScale 1.3, multiwindow-narrow, landscape, privacy/legal |
| Pixel Tablet AVD | `emulator-5554` | 37 | 2026-09-26 | Tablet side-tools green |

## Lab devices (manual — schedule before Play)

| Device | Owner | Camera pick | Export (photo + movie) | Pack download | Last pass |
|--------|-------|-------------|------------------------|---------------|-----------|
| Pixel (physical) | — | ☐ | ☐ | ☐ | — |
| Samsung (One UI) | — | ☐ | ☐ | ☐ | — |
| Xiaomi / Redmi (MIUI/HyperOS) | — | ☐ | ☐ | ☐ | — |

## API smoke (manual / CI)

| API | Focus | Status |
|-----|--------|--------|
| 31 | Camera / Media3 baseline | ☐ |
| 34 | Ultra HDR keep path | ☐ (Maestro `d11-hdr-export` on lab Pixel when available) |
| 35 | Default lab | **Partial** — automated on Pixel_7a AVD |

## Adaptive / a11y (automated)

| Flow | Result | Date |
|------|--------|------|
| `.maestro/chrome/talkback-studio.yaml` | green | 2026-09-26 |
| `.maestro/chrome/fontscale-studio.yaml` | green | 2026-09-26 |
| `.maestro/chrome/multiwindow-narrow.yaml` | green | 2026-09-26 |
| `.maestro/chrome/landscape-phone-session.yaml` | green (user_rotation) | 2026-09-26 |
| `.maestro/chrome/tablet-side-tools.yaml` | green | 2026-09-26 |
| `.maestro/chrome/privacy-legal-library.yaml` | green | 2026-09-26 |

See also: [OEM_EXPORT.md](OEM_EXPORT.md), [PLAY_LISTING_CHECKLIST.md](PLAY_LISTING_CHECKLIST.md).
