# Quality data (M2 Depth) — auto

```bash
python prepare_quality_data.py
```

Downloads **official NYU Depth V2** labeled `.mat` from NYU (`horatio.cs.nyu.edu`) into `rgb/` + `depth/` and writes `READY`.

- No Hugging Face / ModelScope / TFDS
- ~2.8 GB one-time download (cached under `data/_dl/`)

Then: `python train.py --quality`
