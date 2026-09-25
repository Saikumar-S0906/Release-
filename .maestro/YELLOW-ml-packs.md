# YELLOW / manual — Play Feature Delivery pack install

Maestro cannot drive Play Store module download or a system file picker sideload.

Manual:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. Tap **Download on-device pack**
3. On a Play-installed build, confirm the Play module installs and checksums
4. On a sideload APK, tap **Install from file** and pick `{inpaint,superres,denoise,interp,track}.tflite`, or copy the file into app `filesDir/ml_incoming/` then retry Download
5. Neural Apply stays disabled until a real interpreter is bound — nearby-pixel fill / frame-hold still work

Automated: `.maestro/packs/fill-pack-status.yaml`, `.maestro/packs/superres-denoise.yaml`, `.maestro/movie/f1-track-roto.yaml` assert the unavailable/download chrome.
