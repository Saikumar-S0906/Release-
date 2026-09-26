#!/usr/bin/env python3
"""
Generate tools/i18n_cache/{hi,ar,iw,ur,ja,ko}.json from AI translation tables.

Tables are value lists in the same order as tools/strings_en.json keys.
Run: python tools/gen_i18n_from_ai.py
"""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
KEYS = list(EN.keys())
OUT = ROOT / "tools/i18n_cache"
OUT.mkdir(parents=True, exist_ok=True)

FMT = re.compile(r"%(\d+\$)?[\.0-9]*[sdif]|%%")
BRANDS = (
    "PixiJoy", "WhatsApp", "LUT", "HDR", "GIF", "MP4", "EXIF", "GPS",
    "sRGB", "Display P3", "LiteRT", "Media3", "Ultra HDR",
)


def check(loc: str, values: list[str]) -> dict[str, str]:
    if len(values) != len(KEYS):
        raise SystemExit(f"{loc}: expected {len(KEYS)} values, got {len(values)}")
    done = {k: v for k, v in zip(KEYS, values)}
    done["app_name"] = "PixiJoy"
    for k, en in EN.items():
        tr = done[k]
        for m in FMT.finditer(en):
            if m.group(0) not in tr:
                raise SystemExit(f"{loc}/{k}: missing {m.group(0)!r}\n EN={en!r}\n TR={tr!r}")
        for b in BRANDS:
            if b in en and b not in tr:
                raise SystemExit(f"{loc}/{k}: lost {b}\n EN={en!r}\n TR={tr!r}")
    return done


def dump(loc: str, values: list[str]) -> None:
    done = check(loc, values)
    path = OUT / f"{loc}.json"
    ordered = {k: done[k] for k in KEYS}
    path.write_text(json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)} keys={len(ordered)}")


def main() -> None:
    sys.path.insert(0, str(ROOT / "tools"))
    # Each module exposes VALUES: list[str] length == len(KEYS)
    import i18n_values_hi as hi
    import i18n_values_ar as ar
    import i18n_values_iw as iw
    import i18n_values_ur as ur
    import i18n_values_ja as ja
    import i18n_values_ko as ko

    for loc, mod in [
        ("hi", hi),
        ("ar", ar),
        ("iw", iw),
        ("ur", ur),
        ("ja", ja),
        ("ko", ko),
    ]:
        dump(loc, mod.VALUES)


if __name__ == "__main__":
    main()
