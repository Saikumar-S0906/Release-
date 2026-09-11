# PixiJoy — status, crash fixes, what you do next

## What we already have

| Area | Status |
|------|--------|
| Board UI (tabs, create hub, rematch, C-tools, social/recovery) | **UI Done** — functionality tags in [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md) |
| Soft AI generate / OAuth / sync demos | Simulated walkthroughs (honest copy + credit chips) where still SOFT |
| Live Gemini Beautiful/Funny (keyed debug) | Wired — see matrix Wave 1 + [GEMINI_TESTING.md](GEMINI_TESTING.md) |
| Creations library I/O + device social prefs | Wired — matrix Waves 3–4 |
| On-device crash capture (**Settings → Send bug report**) | Built |
| Unit + fuzz tests | Passing |
| Compose E2E / UI / regression suites | Built (~33 matrix cases) |
| Maestro E2E (positive / negative / edge / monkey) | Built — `maestro/` + `scripts/run_maestro.ps1` |
| UI Automator lifecycle tests | Built |
| Lint + Detekt + CI workflow | Built |
| FTL Robo scripts | Ready (needs Firebase/`gcloud`) |
| Macrobenchmark module | Compiles |

**Not store-ready yet** — Imagen / live community / real Play purchases still SOFT or pending Console setup. L3 pixel approve is still yours ([BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md)).

## Crashes / ANRs found in testing → fixes applied

| Issue | Evidence | Fix |
|-------|----------|-----|
| Startup ANR (`failed to complete startup`) | Instrumentation killed process during AdMob/Billing init | AdMob init off UI thread; ads/billing/auth deferred ~300ms after start |
| Duplicate DataStore crash | `multiple DataStores active for pixijoy_prefs` | Shared `PixiJoyDataStores` for app + tests |
| Stuck on privacy after seed | NavHost used default prefs before DataStore emit | `prefsReady` gate + gate `LaunchedEffect` navigation |
| Compose dispose crash on recreate | `AIOOBE` in `Composer.removeCurrentGroup` during `Activity.recreate()` | Tests no longer call `recreate()`; seed + navigate instead |
| HiltTestApplication ANR | Empty test app + heavy providers | Runner uses real `PixiJoyApp` |

## What you need to do now

1. **Phone smoke (Waves 0–4):** install `app/build/outputs/apk/debug/app-debug.apk` and run the checklist in [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md).  
2. **Confirm live Gemini** (before Play): follow [GEMINI_TESTING.md](GEMINI_TESTING.md) + [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md)  
   - Key from [AI Studio](https://aistudio.google.com/apikey) (usually `AIza…`)  
   - `BOARD_LIVE_GENERATE=true` + `gemini-2.5-flash`  
   - Rebuild/install debug APK after changing `local.properties`  
3. **L3 board approve** (before marketing screenshots): [BOARD_L3_REVIEW.md](BOARD_L3_REVIEW.md) — mark Approved / Blocked; eng fixes Blocked only  
4. **Then Play shipment**: signed AAB from `1.0.1-internal` / `versionCode` 2 (bump again for prod), privacy/terms live, listing, Data safety, content rating, **internal track** — see [INFRASTRUCTURE.md](INFRASTRUCTURE.md) + [BOARD_RELEASE_HYGIENE.md](BOARD_RELEASE_HYGIENE.md)  

FTL Robo / own backend can wait until after manual Gemini + phone smoke.
