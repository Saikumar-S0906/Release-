#!/usr/bin/env python3
"""Emit complete i18n JSON caches for hi/ar/iw/ur/ja/ko (AI-assisted)."""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
OUT = ROOT / "tools/i18n_cache"
OUT.mkdir(parents=True, exist_ok=True)

FMT = re.compile(r"%(\d+\$)?[\.0-9]*[sdif]|%%")
BRANDS = (
    "PixiJoy",
    "WhatsApp",
    "LUT",
    "HDR",
    "GIF",
    "MP4",
    "EXIF",
    "GPS",
    "sRGB",
    "Display P3",
    "LiteRT",
    "Media3",
    "Ultra HDR",
)


def validate(loc: str, done: dict[str, str]) -> None:
    if set(done) != set(EN):
        raise SystemExit(
            f"{loc}: key mismatch missing={sorted(set(EN)-set(done))[:8]} extra={sorted(set(done)-set(EN))[:8]}"
        )
    if done.get("app_name") != "PixiJoy":
        raise SystemExit(f"{loc}: app_name must be PixiJoy")
    for k, en in EN.items():
        tr = done[k]
        for m in FMT.finditer(en):
            if m.group(0) not in tr:
                raise SystemExit(f"{loc}/{k}: missing format {m.group(0)!r} in {tr!r}")
        for b in BRANDS:
            if b in en and b not in tr:
                raise SystemExit(f"{loc}/{k}: lost brand {b}")


def dump(loc: str, done: dict[str, str]) -> None:
    validate(loc, done)
    path = OUT / f"{loc}.json"
    # stable key order matching EN
    ordered = {k: done[k] for k in EN}
    path.write_text(json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)} keys={len(ordered)}")


def main() -> None:
    # Late imports keep each pack editable independently.
    sys.path.insert(0, str(ROOT / "tools"))
    from i18n_data import LOCALES

    for loc, mapping in LOCALES.items():
        dump(loc, mapping)


if __name__ == "__main__":
    main()
