# Quality data (M2 Depth) — auto

```bash
python prepare_quality_data.py
```

Pulls **NYU Depth V2** (TFDS) into `rgb/` + `depth/` and writes `READY`. No manual upload.

Then: `python train.py --quality`
