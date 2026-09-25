# YELLOW / manual — Play Feature Delivery pack install

Maestro cannot drive Play Store module download or a system file picker sideload. No device or Maestro pass is recorded here.

Manual:

1. Photo Fill / Super-res / Denoise, or Movie Slow-mo / Track
2. Super-res and Denoise Missing shows **Download** on `empty_packs`. Fill (Inpaint) has no hosted weight, so Missing shows **Install from file**, not Download. Slow-mo / Track are HOLD and use that same sideload plate.
3. On a Play-installed build, confirm a Play module only for packs that ship one. This APK does not ship inpaint, interp, or track weights.
4. On a sideload APK or emulator, tap **Download** only for Super-res and Denoise. Super-res fetches the pinned distilled ESRGAN; Denoise fetches the pinned Raspberry Pi UNet. Checksum failure stays Failed, not Ready. Inpaint has no HTTPS pin and no Download button: **Install from file** or copy `inpaint.tflite` into `filesDir/ml_incoming/`. Slow-mo / Track have no hosted weight (HOLD). Do not invent a URL or SHA for them.
5. Super-res / Denoise: status “Downloaded and verified (Ready)” still leaves Apply off. Apply turns on only when the status says “Neural ready” (`PackRuntime.engineReady`). A missing LiteRT init or a contract miss keeps Apply off and does not relabel the photo as bicubic or blur. Fill: “Installed and verified (Ready)” is the sideloaded file only. Nearby-pixel Apply stays on and is not neural. Neural Apply stays off until the status says the sideloaded model is bound (`PackRuntime.engineReady(Inpaint)` after an RGB+mask FLOAT32 bind). Slow-mo / Track chips stay off `engineReady` (frame hold is not neural). Device steps, with no recorded pass: `docs/R5_ML_PACKS.md`.

Automated: `.maestro/packs/fill-pack-status.yaml` and `.maestro/packs/empty-packs.yaml` assert Fill’s sideload plate (Install from file, no Download) and the nearby-pixel Apply. They do not claim `engineReady`. `.maestro/packs/superres-denoise.yaml` and `.maestro/movie/f1-track-roto.yaml` assert pack chrome. `empty-packs.yaml` checks the `empty_packs` plate while Inpaint is Missing and does not install a model.
