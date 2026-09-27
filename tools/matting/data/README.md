# Quality data (M3 Matting) — auto

```bash
python prepare_quality_data.py
```

Pulls **Oxford-IIIT Pet** segmentation (TFDS) into `rgb/` + `alpha/` and writes `READY`. No manual upload.

Then: `python train.py --quality`
