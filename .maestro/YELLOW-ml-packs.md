# YELLOW / manual — Play Feature Delivery pack install

Maestro cannot drive Play Store module download or a system file picker sideload.

Manual:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. Tap **Download** (`empty_packs`; the button follows the same install states as before)
3. On a Play-installed build, confirm the Play module installs and checksums
4. On a sideload APK or emulator, tap **Download**. Super-res fetches the pinned distilled ESRGAN; Denoise fetches the pinned Raspberry Pi UNet. Checksum failure stays Failed, not Ready. Inpaint has no HTTPS pin: **Install from file** or copy `inpaint.tflite` into `filesDir/ml_incoming/`. Slow-mo / Track have no hosted weight (HOLD).
5. Super-res / Denoise: status “Downloaded and verified (Ready)” still leaves Apply off. Apply turns on only when the status says “Neural ready” (`PackRuntime.engineReady`). A missing LiteRT init or a contract miss keeps Apply off and does not relabel the photo as bicubic or blur. Fill stays nearby-pixel until an RGB+mask inpaint file binds; that fill is not neural. Slow-mo / Track chips stay off `engineReady` (frame hold is not neural). Device steps, with no recorded pass: `docs/R5_ML_PACKS.md`.

Automated: `.maestro/packs/fill-pack-status.yaml`, `.maestro/packs/empty-packs.yaml`, `.maestro/packs/superres-denoise.yaml`, `.maestro/movie/f1-track-roto.yaml` assert the unavailable/download chrome. `empty-packs.yaml` checks the `empty_packs` plate while the pack is Missing and does not install a model.
