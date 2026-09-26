# Play listing & Data safety checklist (studio)

Align store text with the **on-device** product claim. Confirm live pages before production upload.

## Store listing

| Item | Status / notes |
|------|----------------|
| App name | PixiJoy |
| Short description | On-device photo & movie studio — no upload, no watermark |
| Full description | Emphasize local edits; optional model-pack download only after user taps Download |
| Privacy policy URL | `https://pixijoy.ai/privacy` (in-app links exist) |
| Terms URL | `https://pixijoy.ai/terms` (in-app links exist) |
| Content rating questionnaire | Complete IARC / Play questionnaire — creative tools, user-generated media, no social feed in studio shell |
| Featured graphic / screenshots | Phone + tablet; show editor glass, not “cloud AI” |
| Country / availability | Launch countries TBD from install data |
| Target audience | Age rating per questionnaire; no child-directed design |

## Data safety form (Play Console)

Declare consistently with privacy copy:

| Category | Expectation |
|----------|-------------|
| Collects personal info | **No** account; no upload of photos/movies for editing |
| Photos / videos | Processed **on device**; not shared with PixiJoy servers for edit/export |
| Files / docs | Optional on-device pack `.tflite` download (user-initiated) |
| App activity / diagnostics | None unless a future privacy-compliant analytics design ships |
| Data encrypted in transit | Pack HTTPS + SHA; no media upload path |
| Users can request deletion | In-app **Delete all projects** / **Reset app data** (Library) |

Do **not** claim “AI in the cloud” or account sync for the studio shell.

## Pre-upload hygiene

- [ ] Bump `versionName` / `versionCode` ([BOARD_RELEASE_HYGIENE.md](BOARD_RELEASE_HYGIENE.md))
- [ ] Privacy & Terms URLs return 200
- [ ] Release ABI filters only (`arm64-v8a`, `armeabi-v7a`)
- [ ] Signed release APK/AAB; ProGuard mapping retained
- [ ] Device matrix last-pass dates filled for Pixel + one Samsung + one Xiaomi ([DEVICE_MATRIX.md](DEVICE_MATRIX.md))
- [ ] OEM battery FAQ linked from support ([OEM_EXPORT.md](OEM_EXPORT.md))

## Related

- [GLOBAL_READINESS.md](GLOBAL_READINESS.md) Wave 4
- [PRODUCT_LIVE_SMOKE.md](PRODUCT_LIVE_SMOKE.md) (billing/ads if those tracks ship)
