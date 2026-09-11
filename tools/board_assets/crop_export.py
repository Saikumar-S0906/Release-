#!/usr/bin/env python3
"""Export crops from crops.json when Pillow is installed."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CFG = Path(__file__).with_name("crops.json")
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable"


def main() -> int:
    try:
        from PIL import Image
    except ImportError:
        print("Pillow not installed; skip crop_export. Use GenerateImage stand-ins.", file=sys.stderr)
        return 0
    data = json.loads(CFG.read_text(encoding="utf-8"))
    crops = data.get("crops") or []
    if not crops:
        print("No crops defined in crops.json")
        return 0
    OUT.mkdir(parents=True, exist_ok=True)
    for item in crops:
        src = ROOT / item["file"]
        box = item["box"]  # x,y,w,h
        out_name = item["out"]
        im = Image.open(src)
        x, y, w, h = box
        crop = im.crop((x, y, x + w, y + h))
        dest = OUT / f"{out_name}.png"
        crop.save(dest)
        print("wrote", dest)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
