# PixiJoy — complete app plan

**Product:** PixiJoy — the studio that never leaves the phone.  
**Local. Photo and movie. No upload, no account, no watermark, no monthly fee.**  
**Branch:** `next` (greenfield). `main` is a parts bin until this ships.  
**Specs:** this file + `docs/ARCHITECTURE.md` + `docs/LIBS_AND_FEATURES.md` + `docs/PIXIjOY_VISUAL_IDENTITY.md`.

This is the whole-app plan: product, design, architecture, algorithms, patterns, development sequence, and test automation.

---

## 0. Non-goals (locked)

Do not build: Gemini/cloud generate, accounts, community, credits, ads, monthly subscriptions, watermarks, Firebase on the editor path, Unity, TensorFlow-full, RenderScript, a 5-tab bible shell, `BoardScreenHost` as the product.

Do keep: logo, splash look, dark brand tokens, on-device tools from `main` as **libraries**.

---

## 1. Product

### 1.1 Who it is for

People who want Lightroom-class stills and CapCut-class trim/export **without sending media to a server**. Ages 7+ copy. Premium, not toy, not a dump of 30 boards.

### 1.2 Information architecture

```
Splash (logo + spine line)
  → first run: Privacy (accept only — no signup)
    → Shell
         Home     recents, last project, Take photo / Gallery
         Studio   Photo | Movie  (media first, then tool)
         Library  projects, exports, batch
```

Export is a **sheet on the session**, never a Create tile.

### 1.3 Feature releases

| Release | Photo | Movie | Platform |
|---|---|---|---|
| **R0 skeleton** | — | — | Splash, privacy, 3-tab adaptive shell |
| **R1 stills** | Crop/rotate/flip, exposure/contrast/HSL, export JPEG/PNG/WebP | — | Compact + expanded |
| **R1.1 stills** | Heal/clone, text, restore, expand/fill, cutout/erase, stickers, collage | — | Same |
| **R2 motion** | — | Play, scrub, filmstrip, trim/split, concat, speed, Ken Burns/talking GIF, export MP4 | Same |
| **R3 capture + catalog** | CameraX into project | CameraX video into project | Fold hinge |
| **R4 enhance packs** | LiteRT cutout quality, 2×/4× SR, denoise (Play Feature Delivery) | Optional 30→60 RIFE pack | On-demand only |
| **R5 billing** | One-time unlocks (RAW, 4K, SR pack). Never paywall crop/trim | Same | Play Billing 7/8 |

RAW/DNG, Filament titles, ARCore previz, Oboe voiceover = **R6+**, not v1.

### 1.4 Promise copy (everywhere)

Splash, Studio footer, Library empty:  
*No upload. No account. No watermark. No monthly fee.*

---

## 2. Design

### 2.1 Brand (locked tokens)

| Token | Hex | Use |
|---|---|---|
| Background | `#0B0B10` | Canvas |
| Surface | `#15151C` | Bars, cards |
| Surface 2 | `#20202A` | Sheets |
| Text | `#FFFFFF` | Primary |
| Text secondary | `#B7B7C7` | Hints |
| Border | `#30303A` | Hairlines |
| Accent | `#6C4DFF` | Primary CTA, rail |
| Accent end | `#FF5AA5` | Sparse gradient (splash only) |
| Success | `#25B878` | Export OK |
| Error | `#E94D63` | Failures |

Dark default. Light tokens already exist (`BoardColors.light`) for system follow.

Type: platform sans. Scale from current `BoardType` (hero 32 Bold → caption 12). CTAs SemiBold, min height 52 dp.

Logo: `design/pixijoy_logo.svg` / `BoardFlowArt.logo`. Splash: logo + spine line + thin progress. No “Loading your creativity”.

### 2.2 UX principles (2025–26)

1. **Content first.** Preview is the hero. Chrome is a rail or a pane, not a dashboard of tiles-as-bible.  
2. **Media before tool.** Camera / Gallery / sample appear **after** the user picks Editor / Trim / Cutout.  
3. **Canonical layouts.** Compact = single pane. Expanded = `ListDetailPaneScaffold` (tool list | live canvas). Fold = avoid hinge via `DisplayFeature`. Landscape phone is **not** a tablet (`width Expanded AND heightDp ≥ 520`).  
4. **Material 3 Expressive** motion: container transform photo → editor, predictive back, `NavigationSuiteScaffold` (bar ↔ rail). Honor reduce-motion.  
5. **One primary action** per screen (Apply, Export). Secondary is outlined. No clickable text for destructive or primary work.  
6. **Honesty.** No fake catalog. Empty Library is empty. Processing never auto-loops Back.  
7. **A11y.** TalkBack names, 48 dp targets, contrast, large-text, RTL later (R3+).  
8. **Tablet input.** Undo/Redo/Export keys; hover states on rail.

### 2.3 Key screens (wire in designsystem, not 30 PNGs)

- **Splash** — logo, “The studio that never leaves the phone.”  
- **Privacy** — short local-processing copy, Accept.  
- **Home** — recents grid, last project card, capture row.  
- **Studio** — Photo | Movie segmented control, 2-up (compact) / list|detail (expanded).  
- **Session** — preview + tool rail (Crop, Adjust, Heal, Text, …) or timeline (movie).  
- **Export sheet** — format, quality, Save to Pictures / Movies. No watermark toggle because there is no watermark.  
- **Library** — projects, filter Photo/Movie, swipe actions.

Motion spec: 200–280 ms emphasized, 120 ms standard (`PixiMotionMs`). Shared element on the bitmap/player surface.

### 2.4 Adaptive matrix (must test)

| Device | Class | Expect |
|---|---|---|
| Pixel 6/8 | Compact | Bottom bar, full-screen session |
| Pixel Fold outer | Compact/Medium | Same as phone |
| Pixel Fold inner / Pixel Tablet | Expanded | Nav rail + list\|canvas |
| Phone landscape | Compact width-tall? **No** — width may be Expanded, height short → **single pane**, not two-pane |

---

## 3. Architecture

### 3.1 Style

**Modular Clean Architecture** + **UDF (MVI-lite)**:

```
app/ui  →  feature ViewModel (State, Event, Effect)
        →  domain use case
        →  data repo / engine
        →  Android / Media3 / GLES / Room / SAF
```

Not a single `:app` God-module. Not “Clean Architecture” as 40 interface files per slider.

### 3.2 Modules

```
:app
:designsystem
:core:common          # Result, AppDispatchers, logging
:core:data            # Room, DataStore, ProjectRepository
:core:media           # decode, EXIF, color, file
:photo                # PhotoEngine + photo UI
:movie                # MovieEngine + movie UI
:ml                   # LiteRT/MediaPipe facades (no-op if pack missing)
:billing              # one-time Play products
```

Dynamic features later: `:ml-sr`, `:ml-inpaint`, `:optional-ffmpeg`.

**Rule:** engines never import Compose. UI never calls `MediaCodec` / GLES.

### 3.3 Design patterns (where they earn their keep)

| Pattern | Where |
|---|---|
| **Unidirectional state** | Every screen: `data class UiState` + sealed `Intent` + one-shot `Effect` |
| **Command / edit graph** | Photo & movie ops are immutable commands on a graph. Undo = pop. Redo = push. Serialize to `.pixijoy.json` |
| **Strategy** | `ExportStrategy` (JPEG/WebP/PNG/MP4), `ScaleStrategy` (Lanczos vs SR pack) |
| **Facade** | `PhotoEngine`, `MovieEngine`, `VisionFacade` hide GL / Transformer / LiteRT |
| **Repository** | `ProjectRepository`, `MediaRepository` (SAF + takePersistableUriPermission) |
| **Worker + FGS** | All exports |
| **State machine** | Session: Idle → Preview → Editing → Exporting → Done / Failed |
| **Adapter** | Media3 `Player` → `TimelineUiModel`; Coil → preview |
| **Observer (Flow)** | Room, WorkInfo, player position |
| **Factory** | `PreviewSessionFactory` (still vs movie) |
| **Null Object** | `VisionFacade.Unavailable` when ML pack not installed |
| **Builder** | Transformer `EditedMediaItem` composition |

Avoid: Singleton bitmap cache as global; God `BoardScreenHost`; dual nav shells.

### 3.4 Domain model

```
Project
  id, title, kind: PHOTO | MOVIE, createdAt, updatedAt
  MediaRef[]     contentUri, mime, durationMs?, width, height
  EditGraph      version, ops: List<Op>
  ExportJob?     workId, status

Op (photo)   Crop, Rotate, Flip, Grade, HealStroke, TextLayer, CutoutMask, ExpandPad
Op (movie)   Trim, Split, Concat, Speed, Mute, KenBurns, AudioFade
```

Preview session holds a **downscaled GPU texture or player**, never a 48 MP `Bitmap` in Compose state.

### 3.5 Concurrency

- `AppDispatchers`: Main, Default, IO, Engine (single-thread GL), Export (WorkManager).  
- Photo preview: request id; drop stale frames.  
- Movie: one Transformer job per project at a time; cancel on graph change.

### 3.6 Security / privacy

- No HTTP client in `:photo` `:movie` `:core:media`.  
- Network security config: no cleartext.  
- EXIF GPS strip on export **default on**, keep as opt-in.  
- Privacy screen is a local flag in DataStore, not an account.

---

## 4. Algorithms (the ones that matter)

Pick proven, mobile-bounded algorithms. Do not invent new vision papers for v1.

### 4.1 Photo

| Job | Algorithm | Notes |
|---|---|---|
| Downscale preview | Box then Lanczos-3 | Preview ≤ 2048 on long edge |
| Crop | Normalized rect + homography | Existing `BoardCropCanvas` pinch + handles |
| Rotate/flip | 90° integer / Householder | Cheap; push as graph op |
| Exposure/contrast | Gain + pivot around 0.18 | AGSL |
| HSL | Cylindrical convert, per-band | AGSL |
| Curves | Piecewise cubic / LUT 1D | Bake to 256-LUT |
| Histogram | 256-bin on 1/8 downsample | GPU or CPU |
| Blur | Dual Kawase or Gaussian pyramid | Not NxN naive |
| Sharpen | Unsharp mask | Amount/radius/threshold |
| Vignette | Radial smoothstep | — |
| Heal/clone | Poisson seamless clone (optional OpenCV) or mean-shift patch | Brush on mask |
| Cutout | MediaPipe Image Segmenter | Fallback: existing on-device processor from `main` |
| Expand/outpaint | Pad + reflect/content-aware fill; SR later | v1 = pad + Lanczos |
| Super-res 2×/4× | Real-ESRGAN-class LiteRT/ncnn, **tiled with overlap** | Feature module |
| Denoise | NAFNet/MIRNet-class LiteRT | Feature module |
| Color manage | `ColorSpace` sRGB / P3 / AdobeRGB matrix | No full ICC v1 |

### 4.2 Movie

| Job | Algorithm | Notes |
|---|---|---|
| Probe | `MediaExtractor` + retriever | Duration, fps, size, HDR |
| Play/scrub | ExoPlayer | Keyframe then sample |
| Filmstrip | `getFrameAtTime` + disk LRU | 1 fps thumbs |
| Trim | Transformer transmux when GOP allows | Else re-encode |
| Concat | `EditedMediaItemSequence` | Audio resample |
| Speed | `SpeedChangeEffect` | Pitch optional later |
| Ken Burns | Scale+translate keyframes on still | Talking GIF from `main` |
| Proxy | 540p Transformer | Edit on proxy, export full |
| Export | Transformer + muxer fast-start | 1080 default, 4K unlock |
| Frame interp | RIFE | Pack only, flagship |

### 4.3 Vision / tracking (packs)

| Job | Algorithm |
|---|---|
| Subject mask | MediaPipe Image Segmenter |
| Face | Face Landmarker |
| Pose / cut-on-action | Pose Landmarker |
| Planar track | PyrLK optical flow or ECC |
| Inpaint | LaMa-class LiteRT, tiled |

### 4.4 Systems

| Job | Algorithm |
|---|---|
| Undo | Command stack (graph ops) |
| Thumb cache | LRU disk, Coil memory |
| Face cluster (later) | Embeddings + HNSW/ANN local |
| Batch | WorkManager unique work per URI |

---

## 5. Development

### 5.1 Tooling

- AGP 8.7+, Kotlin 2.0, Compose BOM current, Hilt, KSP.  
- JVM: **JBR 17** (`org.gradle.java.home`).  
- minSdk 31 (Android 12+ as in LIBS). targetSdk current.  
- Version catalog `gradle/libs.versions.toml`.  
- Detekt + Android Lint on CI.

### 5.2 Package layout (per feature module)

```
ui/          screens, components
vm/          ViewModels
domain/      use cases, models
data/        repos, Room if module-local
engine/      GL / Transformer (no Compose)
```

### 5.3 What to copy from `main` (by PR)

| Source on `main` | Destination |
|---|---|
| `Color.kt`, `BoardType`, radii, `pj_ic_*`, logo | `:designsystem` |
| `BoardCropCanvas`, `BoardEditorDocument`, heal | `:photo` |
| Cutout / expand processors | `:photo` + `:ml` |
| Collage rasterizer | `:photo` |
| Move / talking GIF | `:movie` |
| Library local file IO | `:core:data` |

Do not copy: `BoardFlow5/6/7`, Different, C06–C09, C12, C14 remix, social, credits, ads, Gemini orchestrator.

### 5.4 PR sequence (implement in this order)

1. Gradle multi-module skeleton + empty `:app` Activity.  
2. Splash + Privacy + `NavigationSuiteScaffold` (Home/Studio/Library) compact **and** expanded.  
3. `:designsystem` tokens, buttons, tiles, panes.  
4. `:core:data` Project Room + picker/SAF. Home recents.  
5. Photo session: crop + grade + export still.  
6. Photo tools: heal, text, restore, expand, cutout, stickers, collage.  
7. Movie session: play, trim, export. Then concat/speed/Ken Burns.  
8. CameraX capture into Project.  
9. Fold hinge + predictive back + shared element.  
10. One-time billing (optional SKUs).  
11. ML feature modules (segmenter, SR).  

Each PR: compiles, unit tests, Maestro smoke on compact, screenshot compact+expanded.

### 5.5 Definition of done (every tool)

- Photo/clip first, then tool.  
- Back returns to Studio, never to auto-processing.  
- Export writes to MediaStore, no watermark.  
- Works offline (airplane mode).  
- Compact and expanded layouts.  
- TalkBack name + 52 dp primary CTA.

---

## 6. Testing automation

### 6.1 Pyramid

```
Unit (JVM)           graph ops, mappers, ViewModel state
  Compose UI         screens with fake engines
    Screenshot       compact 411dp / expanded 840dp / fold fixture
      Instrumented   CameraX, Transformer, Room
        Maestro      user journeys on AVD
          FTL        Pixel 6, Tablet, Fold (when attached)
```

### 6.2 Unit

- EditGraph undo/redo, serialize `.pixijoy.json`.  
- Crop math, grade matrices.  
- ProjectRepository with in-memory Room.  
- ViewModels with Turbine.

### 6.3 Compose / screenshot

- Paparazzi or Compose screenshot-tests for Home, Studio, Session, Library.  
- Fixtures: compact, expanded, hinge.

### 6.4 Maestro (rewrite for studio — do not keep AI flows)

```
maestro/studio/
  helpers/onboard.yaml          splash + privacy Accept
  positive/01_cold_start.yaml
  positive/02_tabs.yaml         Home Studio Library
  positive/03_photo_editor.yaml sample → crop → apply → back
  positive/04_photo_cutout.yaml
  positive/05_movie_trim.yaml
  negative/01_no_account.yaml   no Sign in / Guest
  negative/02_no_ai_tab.yaml
  negative/03_offline_export.yaml
  edge/01_back_from_session.yaml
  adaptive/01_tablet_two_pane.yaml   wm size or Tablet AVD
```

Device tags: `compact`, `expanded`. `debug.pixijoy.e2e=1` for sample buttons.

Delete or ignore `maestro/positive/15–19` AI flows on `next`.

### 6.5 Instrumented / FTL

- Pixel 6 Pro (compact), Pixel Tablet (expanded), Pixel Fold if attached.  
- Airplane mode export.  
- `wm size` override is a fallback, not a substitute for Tablet AVD.

### 6.6 Performance

- `:benchmark` startup (existing module).  
- Perfetto: session open < 300 ms after decode of 12 MP preview.  
- Export 1080p 15 s clip: progress + FGS; no ANR.

### 6.7 Static

`detekt`, `lintDebug`, no network APIs in engine modules (custom lint or ArchUnit-style import check).

### 6.8 CI (GitHub Actions on `next`)

```
unit + detekt + lint
compose screenshots (linux)
Maestro compact emulator
```

Tablet/fold nightly, not every PR.

---

## 7. Quality bars

| Area | Bar |
|---|---|
| Base APK | < 40 MB |
| Editor path | 0 network calls |
| Primary CTA | ≥ 52 dp, real button |
| Two-pane | only if width Expanded **and** height ≥ 520 dp |
| Undo | every destructive graph op |
| Export | MediaStore, no watermark, GPS stripped by default |
| Crash | file log in app storage; Play Vitals optional |

---

## 8. Risks

| Risk | Mitigation |
|---|---|
| Porting `main` tools pulls AI with them | Copy files, strip pipeline callbacks, compile `:photo` without Gemini |
| Transformer GOP trim not frame-accurate | Document “nearest keyframe” + optional re-encode |
| ML packs blow size | Feature delivery; Null Object facade |
| Fold AVDs flaky | Screenshot fixtures + one real Fold when available |
| Dual nav from old app | Single Activity, one `NavigationSuiteScaffold` |

---

## 9. New-session kickoff

```
Build PixiJoy local studio on branch next at C:\Users\andro\PixiJoy\PixiJoy.

Read in order:
  docs/MASTER_PLAN.md
  docs/ARCHITECTURE.md
  docs/LIBS_AND_FEATURES.md
  docs/PIXIjOY_VISUAL_IDENTITY.md

Do not extend BoardScreenHost, Create AI, Community, accounts, or Gemini.
Start PR 1 from MASTER_PLAN §5.4: modules + splash (logo + spine line) + Privacy + three-tab adaptive shell for phone, tablet, and fold.

No upload, no account, no watermark, no monthly fee.
Gradle JBR 17. Do not ask me to review each slice.
```
