# Laptop / CPU quality train (no GPU)

Same scripts as Colab. From a lab folder after unpacking `*_lab.zip` (or from the repo):

```bash
# example: M3
cd tools/matting
python -u prepare_quality_data.py --data ./data
python -u train.py --quality --batch 4 --out dist/matting.keras
# reconnect after interrupt:
python -u train.py --quality --batch 4 --out dist/matting.keras --resume
python -u export_tflite.py --keras dist/matting.keras --out dist/matting.tflite
python -u verify_contract.py --quality --data ./data
```

- No GPU required.
- Keep `dist/*.ckpt.keras` + `train_state.json` to resume.
- Pin to Release- only if verify prints `OK`.
