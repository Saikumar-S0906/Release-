# YELLOW / manual — Play Feature Delivery pack install

Maestro cannot drive Play Store module download or a system file picker sideload.

Manual:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. Tap **Download** (`empty_packs`; the button follows the same install states as before)
3. On a Play-installed build, confirm the Play module installs and checksums
4. On a sideload APK or emulator, tap **Download** — Super-res fetches distilled ESRGAN; Denoise fetches Raspberry Pi UNet. Inpaint / Slow-mo / Track: **Install from file** or copy `{inpaint,interp,track}.tflite` into `filesDir/ml_incoming/`
5. Super-res / Denoise Apply enable after LiteRT bind. Fill stays nearby-pixel until an RGB+mask inpaint file binds. Slow-mo / Track chips stay locked (no fake neural frames)

Automated: `.maestro/packs/fill-pack-status.yaml`, `.maestro/packs/empty-packs.yaml`, `.maestro/packs/superres-denoise.yaml`, `.maestro/movie/f1-track-roto.yaml` assert the unavailable/download chrome. `empty-packs.yaml` checks the `empty_packs` plate while the pack is Missing and does not install a model.
