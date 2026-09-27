# Quality data (M4 Flow) — auto

```bash
python prepare_quality_data.py
```

Pulls **DIV2K** from ETH Zurich and builds labeled flow triplets (`frame0` / `frame1` / `flow_npy`) + `READY`. No Hugging Face.

Official FlyingChairs is ~33 GB — not auto-downloaded. If you already have it:

```bash
python prepare_quality_data.py --chairs-root /path/to/FlyingChairs_release
```

Then: `python train.py --quality`
