# Quality data (M3 Matting) — auto

```bash
python prepare_quality_data.py
```

Downloads **Oxford-IIIT Pet** images + trimaps from official VGG (`robots.ox.ac.uk`) into `rgb/` + `alpha/` and writes `READY`.

- No Hugging Face / TFDS
- No manual upload

Then: `python train.py --quality`
