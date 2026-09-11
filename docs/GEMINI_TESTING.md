# Gemini ML — manual testing checklist

## Goal

Confirm **live Gemini** works for memes / create-for-me / captions before Play shipment.  
Firebase Test Lab and backend hosting can wait.

## Setup (required for live Gemini)

1. Get a key from [Google AI Studio](https://aistudio.google.com/apikey)  
   - Typical key prefix: `AIza...`  
2. In `local.properties` (gitignored):
   ```properties
   GEMINI_API_KEY=AIza...your_key...
   GEMINI_MODEL=gemini-2.5-flash
   BOARD_LIVE_GENERATE=true
   PIXIJOY_API_BASE_URL=
   ```
3. Rebuild & reinstall the **debug APK** (BuildConfig embeds the key + flag at compile time).
4. On device: accept **privacy** and **AI cloud consent** when prompted.
5. Stay **online**.

`GEMINI_API_KEY=dummy` → on-device simulator only (no API cost).  
`BOARD_LIVE_GENERATE=false` → board Beautiful/Funny stay soft-only (no Gemini probe), even with a real key.

Board path: Create → Beautiful / Funny processing calls `BoardSoftGenerate` → when live is configured, probes Gemini (`analyzePhoto` / funny `generateCaption`) on sample board art, then always shows soft rematch result art. Soft fallback never crashes the flow.

Full checklist: [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md).

## Model choice (do **not** use shut-down models)

| Model ID | When to use |
|----------|-------------|
| `gemini-2.5-flash` | **Default** — solid vision + captions; good for memes |
| `gemini-3.5-flash` / `gemini-3.8-flash` | Newer keys if 2.5 returns 404 |
| `gemini-3.1-flash-lite` | Cheapest / highest volume |
| `gemini-2.0-flash` | **Shut down (June 2026)** — do not use |

Change only `GEMINI_MODEL=...` in `local.properties` and rebuild.

## How to know it worked

| Signal | Live Gemini | Simulator / fallback |
|--------|-------------|----------------------|
| UI | “Powered by Gemini” / `isCloudPowered` | “Using on-device AI (cloud unavailable)” |
| Logcat | No `AiOrchestrator` / `Gemini HTTP` errors | `Gemini analyze failed` / `Gemini caption failed` |

Filter logcat: `AiOrchestrator|Gemini|okhttp`

## Manual test steps

1. **Board Beautiful** — Create → **AI** → Make it Beautiful → **Add photo** (system picker) → Continue → Generate. With `BOARD_LIVE_GENERATE=true` + real key + AI cloud consent, result shows **your photo** + Gemini scene/tips and “Powered by Gemini”. Logcat: `AiOrchestrator|BoardSoftGenerate|Gemini`.  
2. **Board Funny** — same pick → Generate → result shows **your photo** with Gemini top/bottom captions.  
3. Soft thumbs in Flow3 gallery still set a sample photo if you skip the system picker.  
4. Consent off / credits exhausted → local/soft path, no crash (`userMessage` may show).  
5. Airplane mode → soft/local fallback after failed cloud call.

## Cost control while testing

- Use free AI Studio quota; set a billing budget alert if you enable billing.  
- Prefer `gemini-2.5-flash` (default).  
- Use `dummy` when you don’t need live ML.  
- In-app daily AI credits already throttle calls.

## After manual Gemini testing passes

Then prepare Play shipment: signed AAB, privacy policy URL, store listing, Data safety, content rating, internal track.  
See `docs/INFRASTRUCTURE.md` and `docs/WHAT_NEXT.md`.
