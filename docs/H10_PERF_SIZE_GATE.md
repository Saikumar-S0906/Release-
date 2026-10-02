# H10 — Classical CG perf + size gate

**Status:** **MET** (gate docs + base-APK measure wiring) on tip after classical F1 track-attach (`3fbc1da`).
**Pairs with:** [`R7_CG_EFFECTS.md`](R7_CG_EFFECTS.md) H10 · [`CODING_PHASES.md`](CODING_PHASES.md) P5.6 · E5 size in [`R5_ML_PACKS.md`](R5_ML_PACKS.md)
**Honesty bar:** classical plates + on-demand Filament only. Never label classical CG as neural / Ready / `engineReady`.

---

## What H10 gates

| Check | Rule | How |
|-------|------|-----|
| **On-demand only** | `:feature_cg` is a Play Feature Delivery dynamic module; Filament JNI stays in on-demand `:feature_titles3d` (`compileOnly` in `:feature_cg`) | `app/build.gradle.kts` `dynamicFeatures`; `feature_cg/build.gradle.kts` |
| **Base APK clean** | Base release APK must **not** contain Filament / gltfio JNI or `com.pixijoy.feature.cg` / `com.google.android.filament` classes | `./gradlew :app:reportH10SizeGate` |
| **Base size** | Same E5 aspiration: release phone-ABI base **≤ ~40 MiB** (warn above; FAIL only on Filament-in-base) | Task print + E5 `reportApkSize` |
| **Feature split size** | Report `:feature_cg` and `:feature_titles3d` release APK MiB (informational; no hard fail yet) | Same task |
| **Mid-range frame / MiB** | Honest classical budgets below — not a Filament GPU particle claim | This doc |
| **Pixel_7a smoke** | Checklist only; soft Maestro create-sheet / rename / glTF+dust YELLOW stay parked | § Pixel_7a |

Out of scope for this crumb: Filament HDRI feature work, JNI into base, PackCatalog/tflite, P7, BoardScreenHost / Gemini / Community / accounts, soft Maestro chrome fixes.

---

## Mid-range frame / MiB budgets (classical)

Target device class: **Pixel 7a** (and similar mid-range phones). Budgets are for the **classical** CG/VFX path that ships in base (cube / ground / sky / dust plates + optional F1 track-attach). PropGltf Filament frames apply only after the on-demand split binds.

| Surface | Budget | Notes |
|---------|--------|-------|
| Preview scrub with 0–2 classical CG plates | **≥ 30 fps** sustained | Shared `CgOverlays` path; no Filament required |
| Preview scrub with dust + F1 track-attach | **≥ 30 fps** preferred; **≥ 24 fps** acceptable under load | Classical motion-blur crumbs only; not a GPU sim |
| Export bake classical plates | Wall-clock OK if preview≈export; no separate “lite” export path | Same compositor |
| Studio session steady RSS (classical CG open) | Prefer **≤ ~450 MiB** additional over idle shell | Soft — device/OEM variance; capture with `dumpsys meminfo` |
| Base release APK (phone ABI) | **≤ ~40 MiB** aspiration | E5 + H10; MediaPipe JNI dominates remainder |
| `:feature_cg` split | Informational; keep lean (no HDRI asset, no second Filament AAR) | `compileOnly` Filament |
| `:feature_titles3d` split | Owns Filament JNI for titles + CG runtime load | Do not duplicate AAR into `:feature_cg` |

**Not budgets (yet):** Filament GPU particle fps (H4), HDRI IBL continuity (H3 remainder), neural soft-Z (H5/P7). Those get their own measures when those crumbs land.

---

## Measure commands

```powershell
# Base + feature release APKs, then H10 scan
.\gradlew.bat :app:assembleRelease :feature_cg:assembleRelease :feature_titles3d:assembleRelease
.\gradlew.bat :app:reportH10SizeGate
# Optional E5 detail (.so breakdown)
.\gradlew.bat :app:reportApkSize
# Wrapper
.\scripts\run_h10_size_gate.ps1
```

PASS = no Filament/`feature_cg` content in base + task exits 0.  
A base size over ~40 MiB prints a **warn** (same honesty as E5 fat vs phone-ABI notes) but does not FAIL H10 if the engine stays out of base.

---

## Measured on this crumb tip

Local DESKTOP measure after `:app:assembleRelease` + `:app:reportH10SizeGate` on branch `cursor/classical-h10-perf-size-3fbc1da` (off tip `3fbc1da`, 2026-10-02 IST):

| Artifact | MiB | Bytes | Notes |
|----------|-----|-------|-------|
| Base release APK (`app-release-unsigned.apk`) | **36.59** | 38,362,228 | Phone ABI `arm64-v8a` + `armeabi-v7a`; under ~40 MiB |
| Filament / feature_cg in base | **none** | — | Scan PASS |
| `:feature_cg` release APK | **0.02** | 16,672 | On-demand; Filament `compileOnly` (no JNI AAR) |
| `:feature_titles3d` release APK | **13.08** | 13,716,402 | Owns Filament JNI for titles + CG runtime |

---

## Pixel_7a smoke (gate checklist)

Run only when DESKTOP is free and a Pixel_7a (or equivalent) is attached. **Do not** divert into soft Maestro create-sheet / rename / chrome asserts (YELLOW parked).

1. Install phone-ABI release **or** debug base **without** forcing every dynamic feature if exercising classical-only.
2. Open Movie → CG: add Cube / Dust classical plates; scrub — expect preview≈export feel; no crash.
3. Optional: install `:feature_cg` / titles split (Play or local multi-APK) → PropGltf bind; miss falls back to classical raster (honest).
4. Soft Maestro suite may stay YELLOW; record PASS/FAIL only for the CG size/perf intent above.
5. Optional meminfo snapshot via `scripts/run_perf_gates.ps1 -SkipBenchmark -SkipStress` (or adb `dumpsys meminfo com.pixijoy.ai`) — attach under `app/build/reports/perf_gates/` when claimed.

No Pixel result is claimed in this file until a dated tip dump is pasted by test_automator / developer.

---

## Wiring already in tree (preconditions)

- `dynamicFeatures += … ":feature_cg"`
- `:feature_cg` = `com.android.dynamic-feature` + Filament `compileOnly`
- `OptFeatureModules.CG` + `tryBindCg()` reflection bind; classical raster when unbound
- E5 `reportApkSize` remains for `.so` breakdown

H10 adds the **explicit base scan + budget doc + script**, and flips plan rows to MET.
