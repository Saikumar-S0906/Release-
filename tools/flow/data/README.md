# Quality data (M4 Flow) — auto

```bash
python prepare_quality_data.py
```

Pulls **DIV2K** photos and builds labeled flow triplets (`frame0` / `frame1` / `flow_npy`) + `READY`. No manual upload.

Then: `python train.py --quality`
