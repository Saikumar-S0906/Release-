# PixiJoy neural inpaint lab

Full UNet train/export/verify for `inpaint.tflite`.

Contract (FLOAT32):

- image `1×256×256×3`
- mask `1×256×256×1` (1 = hole)
- RGB out `1×256×256×3`

Quality is not compromised for APK size. If export exceeds ~3 MB, host on the
PixiJoy GitHub Release (`ml-packs-v1`) and pin `downloadUrl` + SHA — same as FILM.
Telea nearby-pixel Fill stays always-on in the app; neural is the upgrade path.

```bash
cd tools/inpaint
python train.py
python export_tflite.py
python verify_contract.py
```
