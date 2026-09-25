# YELLOW / manual — Play Feature Delivery pack install

Maestro cannot drive Play Store module download or a system file picker sideload.

Manual:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. Tap **Download on-device pack**
3. On a Play-installed build, confirm the Play module installs and checksums
4. On a sideload APK or emulator, tap **Download** — Super-res fetches distilled ESRGAN over HTTPS. Other packs: **Install from file** or copy `{inpaint,denoise,interp,track}.tflite` into `filesDir/ml_incoming/`
5. Neural Apply stays disabled until a real interpreter is bound — nearby-pixel fill / frame-hold still work

Automated: `.maestro/packs/fill-pack-status.yaml`, `.maestro/packs/superres-denoise.yaml`, `.maestro/movie/f1-track-roto.yaml` assert the unavailable/download chrome.
