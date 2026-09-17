# PixiJoy local studio — architecture

**Branch:** `next` (greenfield). `main` stays the old product until this ships.  
**Spine:** PixiJoy — the studio that never leaves the phone. Local. Photo and movie. No upload, no account, no watermark, no monthly fee.  
**Spec:** `docs/LIBS_AND_FEATURES.md` (source: Downloads).  
**Do not** continue the old Create/AI/Community/account app. Do not gut `main` in place.

Logo, splash, and the existing dark brand (purple / pink / dark canvas) stay. Visual language is rebuilt on **Material 3 Expressive + canonical layouts**, not a bible-as-grid of 30 flows.

---

## 1. Why a new app

The current `app` module is one Compose host that routes ~30 board flows, dual shells, AI hubs, credits, community, and onboarding. It cannot grow into a photo engine + Media3 movie engine + on-device ML packs without becoming more tangled.

New app = new Gradle modules, new navigation, new IA. **Borrow implementations** from `main` (crop canvas, editor document, Media3 callers, theme tokens, logo) as libraries — do not keep `BoardScreenHost` as the product.

---

## 2. Product surface (user)

Three destinations only:

| Tab | Job |
|---|---|
| **Home** | Recents, continue last project, capture (camera / picker) |
| **Studio** | Photo tools \| Movie tools. Open a tool **after** a photo/clip is chosen |
| **Library** | Local projects, exports, batch |

No Explore, Community, AI tab, signup, credits, ads, Gemini consent.

**Studio — Photo (v1):** Editor (crop/rotate/flip, exposure/contrast/HSL, heal, text), Restore, Expand/fill/upscale (on-device), Remove BG / erase, Stickers (cutout), Collage.  
**Studio — Movie (v1):** Open/play/scrub, trim/split, concat, speed, talking still / Ken Burns GIF, export.  
**Later packs (on-demand ML):** inpaint, 2×/4× SR, denoise, frame interpolate — never in the base APK.

Promise on splash and Studio: *No upload. No account. No watermark. No monthly fee.*  
Billing: **one-time** Play products only (`:billing`). Never subscriptions.

---

## 3. Module graph

```
:app                 # Activity, splash, nav host, adaptive scaffold
:designsystem        # Color, type, motion, logo, buttons, tiles, panes
:core:common         # Coroutines, Result, logging
:core:data           # Room projects, DataStore, SAF URIs
:core:media          # Decode/encode, EXIF, color space, file IO
:photo               # Photo engine (GL/AGSL) + photo UI
:movie               # Media3 play / transform / export + movie UI
:ml                  # LiteRT / MediaPipe facades (optional at runtime)
:billing             # Play Billing 7/8 one-time SKUs
```

Optional later (Play Feature Delivery, not base APK): `:ml-sr`, `:ml-inpaint`, `:optional-ffmpeg`.

**Dependency rule:** UI modules depend on engines, never the reverse. `:app` depends on all. `:photo` and `:movie` depend on `:core:media` + `:designsystem`. `:ml` is called from engines, not from screens.

Target: base APK **< 40 MB**.

---

## 4. Layers (every feature)

```
Screen (Compose) 
  → ViewModel (state + intents)
    → Use case
      → Repository / Engine
        → Android / Media3 / GL / Room / SAF
```

- One **Project** is the unit of work: `projectId`, media refs, edit graph, export jobs.  
- UI never holds a giant `Bitmap` as screen state; it holds a **preview session** (texture / Coil model / player).  
- Export always goes through **WorkManager + foreground service**.  
- No network on the editor path. Network security config: deny cleartext; editor code has no HTTP client. ML packs download only from Play Feature Delivery when the user opts in.

---

## 5. Adaptive UI (phone, tablet, fold)

Use **Jetpack Compose Material3 Adaptive** (`ListDetailPaneScaffold`, `SupportingPaneScaffold`, `NavigationSuiteScaffold`) plus `WindowSizeClass` and `WindowInfoTracker` (hinge / folding feature).

| Width | Studio | Editor |
|---|---|---|
| Compact (phone) | Tool grid, full-screen tool | Preview + bottom tool rail |
| Medium (fold outer / large phone) | Same, wider grid | Preview + rail |
| Expanded (tablet / unfolded) | **List \| canvas**: tools on the start pane, live preview on the end pane | Preview fills the supporting pane; controls in a trailing pane or bottom sheet |

**Fold:** content avoids the hinge (`DisplayFeature`). Do not treat landscape phone as tablet (`heightDp` gate as well as width — the old `rememberBoardTwoPane` idea).

**Chrome:** `NavigationSuiteScaffold` — bottom bar on compact, nav rail on expanded. Three items only.

**Motion:** shared-element photo → editor; predictive back; reduce motion respects system setting.

**Input:** touch first; keyboard shortcuts on tablet (Undo, Export).

This is the “new trending” bar: Expressive M3, canonical two-pane, predictive back — not a 5-tab bible shell.

---

## 6. Engines (from LIBS_AND_FEATURES)

| Engine | Owns | Stack |
|---|---|---|
| Photo | Crop, grade, layers, heal, text bake | GLES 3 / AGSL `RuntimeShader`, `Bitmap`+`Matrix` for simple ops. No RenderScript. |
| Movie | Play, trim, concat, speed, export | Media3 ExoPlayer + Transformer + Effect. ffmpeg only as on-demand escape hatch. |
| Vision | Masks, face, pose | LiteRT + MediaPipe Tasks Vision. Models via feature delivery. |
| Capture | Camera stills / video into a project | CameraX |
| Catalog | Projects, recents, batch | Room + SAF `DocumentFile` |

Reuse from `main` (copy into the right module, then delete board-host wrappers):

- Theme tokens, logo, splash gradient → `:designsystem`  
- `BoardCropCanvas`, `BoardEditorDocument`, heal brush → `:photo`  
- Move / talking GIF / collage rasterizer → `:movie` + `:photo`  
- Cutout / expand on-device processors → `:photo` + `:ml`  
- Library browse (local files only) → `:core:data` UI in `:app`

Do **not** port: Gemini, CompetePack, Beautiful/Funny/Tell, Community, credits, signup, ads, dual `MainScaffold`+`BoardMainShell`.

---

## 7. Navigation

Single Activity. Type-safe Navigation Compose (or Navigation 3 when the project’s AGP allows).

```
Splash → (first run) Privacy → Home
Tabs: Home | Studio | Library
Studio/{tool} → EditorSession(projectId)
Export is a sheet on the session, not a tab.
```

Back: tool session pops to Studio; Studio tab root does not invent a stack of marketing screens. No “Get Started” before photo — **photo/clip first, then tool**.

---

## 8. Data

```
Project (id, title, created, kind: PHOTO | MOVIE)
  MediaRef (contentUri or file, mime, duration?)
  EditGraph (ops list, versioned JSON `.pixijoy.json`)
  ExportJob (workId, status)
```

Prefs: DataStore (privacy accepted, last project, theme).  
No user account table.

---

## 9. Quality

- UI tests: Compose + Maestro on Pixel compact, tablet, fold AVD.  
- Engine tests: JVM/Robolectric for graph ops; instrumented for Transformer.  
- Screenshot tests for compact + expanded.  
- Gradle JVM: JBR 17 (existing).  
- Secrets: none on the editor path. Play Billing keys in Play Console only.

---

## 10. Build order (new session — do this)

Each step is a PR on `next`. Do not revive the old board hub.

1. **Skeleton** — modules, `:app` Activity, splash (logo + spine line), Privacy, 3-tab `NavigationSuiteScaffold`, empty panes that adapt compact/expanded.  
2. **Design system** — port colors, type, radii, buttons, tiles from `main`.  
3. **Projects** — Room + picker/SAF + recents on Home.  
4. **Photo v1** — crop/rotate/flip + basic grade + export still. Port crop canvas.  
5. **Photo v1.1** — heal, text, restore, expand, cutout, stickers, collage (port from `main` tools).  
6. **Movie v1** — Media3 player + trim + export. Port talking/move where it fits the new session model.  
7. **Adaptive polish** — list-detail studio, hinge, predictive back, motion.  
8. **Billing (one-time)** — optional unlocks; never a paywall on basic crop/trim.  
9. **ML packs** — feature delivery; LiteRT segmenter for cutout quality.  

---

## 11. Key decisions

1. **Greenfield on `next`, keep `main`.** Old board graph is a parts bin, not the product.  
2. **Three tabs, Photo | Movie studio.** No AI/social chrome.  
3. **Feature modules + engines.** UI cannot talk to MediaCodec/GL directly.  
4. **Canonical layouts** for tablet/fold; landscape phone is not a tablet.  
5. **On-device only on the editor path.** Cloud ML Kit, Firebase, Gemini stay out.  
6. **One-time billing, no watermark, no account.**  
7. **ML and ffmpeg are on-demand**, never base APK.

---

## 12. Continue prompt (paste in a new session)

```
Build PixiJoy local studio on branch next at C:\Users\andro\PixiJoy\PixiJoy.

Read docs/ARCHITECTURE.md and docs/LIBS_AND_FEATURES.md first.
Do not extend the old BoardScreenHost / Create AI / Community app.
Start PR 1: Gradle modules + splash (logo + “the studio that never leaves the phone”) + Privacy + three-tab adaptive shell (Home, Studio, Library) for phone, tablet, and fold.

Reuse visual tokens and on-device tools from main as libraries when you reach those PRs.
No upload, no account, no watermark, no monthly fee.
Gradle: JBR 17. Do not ask me to review each slice.
```
