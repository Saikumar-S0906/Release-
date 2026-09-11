# PixiJoy infrastructure checklist

What you need **now** vs **later**. The app **builds and runs without** `google-services.json`.

## Already enough for local develop / smoke

| Item | Required? | Notes |
|------|-----------|--------|
| Android SDK + emulator/phone | **Yes** | You have this |
| `local.properties` (`sdk.dir`) | **Yes** | Gitignored |
| `GEMINI_API_KEY` | Recommended | Real key from AI Studio (`AIza…`); `dummy` / empty → offline simulator |
| `GEMINI_MODEL` | Optional | Default `gemini-2.5-flash` (`gemini-2.0-flash` shut down June 2026) |
| `PIXIJOY_API_BASE_URL` | Optional | Blank = in-app mock backend |
| AdMob IDs | Optional | Defaults to **Google test ad units** |
| **`google-services.json`** | **No (optional)** | Plugin applies **only if file exists** |

So: you can keep testing the APK **without** Firebase JSON.

---

## Optional Google / cloud pieces

### 1. `google-services.json` (Firebase Analytics, etc.)

**When you need it:** Analytics, Crashlytics (if you add it), Remote Config, Auth, etc.

**How:**
1. [Firebase Console](https://console.firebase.google.com/) → create project  
2. Add Android app with package `com.pixijoy.ai`  
3. Download `google-services.json` → put in **`app/google-services.json`**  
4. Rebuild — Gradle auto-applies the Google Services plugin  

Without it: Analytics SDK is on the classpath but not fully configured; app still runs.

### 2. Firebase Test Lab Robo (cloud exploratory QA)

**Does not require** `google-services.json` in the APK.

**Does require:**
1. Firebase / Google Cloud project  
2. **Billing enabled** on the project (Test Lab)  
3. [`gcloud` CLI](https://cloud.google.com/sdk/docs/install) installed  
4. Login: `gcloud auth login` + `gcloud config set project YOUR_PROJECT_ID`  

Then:
```powershell
.\scripts\run_ftl_robo.ps1 -Matrix pr -ProjectId YOUR_PROJECT_ID
```

### 3. Gemini API (real AI memes / captions)

1. Key from [Google AI Studio](https://aistudio.google.com/apikey)  
2. In `local.properties`:
   ```properties
   GEMINI_API_KEY=your_real_key
   # Phase 7 board Beautiful/Funny live attempt (soft fallback unless key + flag):
   BOARD_LIVE_GENERATE=true
   ```
3. Rebuild APK  

### 4. AdMob (real ads, not test IDs)

1. AdMob account → create app + ad units  
2. Put IDs in `local.properties` (`ADMOB_APP_ID`, banner/rewarded/interstitial)  
3. Rebuild  

For Play Store, use **real** AdMob app ID (not the sample `~3347511713`).

### 5. Play Billing (Pro purchases)

- Real purchases need Play Console app + products + signed release on a test track  
- Debug builds usually can’t complete real billing flows  

### 6. Backend API

- Leave `PIXIJOY_API_BASE_URL` empty until you deploy a server  
- Then set the HTTPS base URL in `local.properties`  

### 7. Play Console (distribution + pre-launch report)

- Upload internal/alpha AAB → Play’s **pre-launch report** runs Robo-like tests for free  
- Separate from Firebase Test Lab  

---

## Suggested order for you

1. **Now:** Install latest debug APK → phone smoke + bug reports  
2. **Soon:** Real `GEMINI_API_KEY` if you want live AI  
3. **When you want cloud Robo:** Firebase project + `gcloud` + billing (JSON still optional for Robo)  
4. **Before Play release:** `google-services.json`, real AdMob, Play Console, signed release  

---

## Security

Never commit:
- `local.properties`
- `google-services.json` (or commit only a non-secret template carefully — prefer gitignore)
- Real API keys  

See also: `local.properties.example`, `docs/WHAT_NEXT.md`, `docs/QA_STACK.md`, `docs/BOARD_RELEASE_HYGIENE.md`.
