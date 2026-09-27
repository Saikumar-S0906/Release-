# Quality data layout (M1 Inpaint)

**Do not pin Release- weights trained only on synthetic smoke.**

## Easiest (recommended) — auto download

```bash
python prepare_quality_data.py
```

Pulls **DIV2K validation HR** (~100 public research photos) into `data/rgb/` and writes `READY`.  
No manual upload. If DIV2K is blocked, tries a small Release- sample zip next.

## Manual (optional)

1. Put ≥32 photos in `rgb/` (Places2 / DIV2K / COCO / phone)
2. Optional: paired hole masks in `masks/` (same stem)
3. Touch empty file `READY`

## Train

```bash
python train.py --quality --data ./data
python export_tflite.py && python verify_contract.py --quality
```

**Quality gate:** masked MAE ≤ **0.08** on held-out real holes.
