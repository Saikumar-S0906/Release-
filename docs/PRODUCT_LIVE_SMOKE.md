# PixiJoy — live product smoke (Wave C)

Run after board UI looks consistent. Order is fixed for store risk.

## C1 — Gemini (Beautiful / Funny)

1. Copy `local.properties.example` → `local.properties` (if needed).
2. Set a real key + flag:
   ```properties
   GEMINI_API_KEY=AIza...your_key...
   GEMINI_MODEL=gemini-2.5-flash
   BOARD_LIVE_GENERATE=true
   ```
3. Rebuild/install **debug** APK (BuildConfig embeds key + flag).
4. Accept privacy + AI cloud consent; stay online.
5. Create → **AI** → **Make it Beautiful** → pick a **real photo** → generate through processing → result shows your photo + Gemini analysis (or soft fallback).
6. Create → **AI** → **Make it Funny** → pick photo → generate → Gemini captions on your photo.
7. Logcat filter: `BoardSoftGenerate|AiOrchestrator|Gemini`  
   - Success: `isCloudPowered` / Powered by Gemini UI; no HTTP errors.  
   - Failure: soft rematch / local captions; flow completes.
8. Set `BOARD_LIVE_GENERATE=false` or `GEMINI_API_KEY=dummy` when done to avoid quota burn.

Details: [GEMINI_TESTING.md](GEMINI_TESTING.md).

## C2 — Billing / Pro / credits

1. Play Console products per [BILLING_SETUP.md](../BILLING_SETUP.md) (`pixijoy_pro_monthly` / yearly / lifetime).
2. License tester + **Internal testing** install (not sideload) for real purchases.
3. Soft path smoke (any build): Me → credits / Pro Pay → **no fake Success** (toast + Manage in Play). Board Pay must not bump demo balance or set Pro. See [FUNCTIONALITY_MATRIX.md](FUNCTIONALITY_MATRIX.md) Wave 4.
4. Free→Pro: open Pro / subscription hub from Me trust flows; confirm upgrade CTA and restored entitlements after real purchase (license tester).

## C3 — Ads

1. Optional AdMob IDs in `local.properties` (else Google test IDs).
2. Cold start: confirm no ANR (AdMob init is deferred off UI thread).
3. Rewarded path: HD / credit reward UI with daily limit copy; cancel and complete once each.

## C4 — Auth / privacy URLs

1. Open Settings → Privacy / Terms in-app.
2. Open Privacy policy and Terms from the first-run screen and from Library. Both are in the app. Neither opens a website.
3. OAuth only if Play listing claims require it.

**Exit:** Beautiful + Funny live probe green (or documented soft fallback); Pro/credits/ads smoke noted; privacy/terms URLs live.
