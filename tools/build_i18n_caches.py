#!/usr/bin/env python3
"""Build complete i18n JSON caches for hi/ar/iw/ur/ja/ko from embedded AI translations."""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
CACHE = ROOT / "tools/i18n_cache"
CACHE.mkdir(parents=True, exist_ok=True)

# Tokens that must survive unchanged when present in English source.
KEEP = re.compile(
    r"%(\d+\$)?[\.0-9]*[sdif]|%%|\\n|\\'|\\\"|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR|"
    r"½×|¼×|⅛×|←|→|·|⌄|×"
)


def assert_placeholders(en: str, tr: str, key: str, loc: str) -> None:
    """Ensure format tokens from EN appear in translation."""
    toks = re.findall(r"%(\d+\$)?[\.0-9]*[sdif]|%%", en)
    for t in toks:
        full = "%" + t if not t.startswith("%") else t
        # re.findall above returns groups; rebuild properly
    for m in re.finditer(r"%(\d+\$)?[\.0-9]*[sdif]|%%", en):
        tok = m.group(0)
        if tok not in tr:
            raise AssertionError(f"{loc}/{key}: missing {tok!r} in {tr!r}")


def write_locale(loc: str, mapping: dict[str, str]) -> None:
    if set(mapping) != set(EN):
        missing = set(EN) - set(mapping)
        extra = set(mapping) - set(EN)
        raise SystemExit(f"{loc}: missing={len(missing)} extra={len(extra)} sample_missing={list(missing)[:5]}")
    mapping = dict(mapping)
    mapping["app_name"] = "PixiJoy"
    for k, v in mapping.items():
        assert_placeholders(EN[k], v, k, loc)
        for brand in (
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
        ):
            if brand in EN[k] and brand not in v:
                raise AssertionError(f"{loc}/{k}: lost brand token {brand}")
    path = CACHE / f"{loc}.json"
    path.write_text(json.dumps(mapping, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    print(f"wrote {path.relative_to(ROOT)} ({len(mapping)} keys)")


def main() -> None:
    # Import locale packs lazily so each file can be edited independently.
    from i18n_pack_hi import HI
    from i18n_pack_ar import AR
    from i18n_pack_iw import IW
    from i18n_pack_ur import UR
    from i18n_pack_ja import JA
    from i18n_pack_ko import KO

    for loc, mapping in [
        ("hi", HI),
        ("ar", AR),
        ("iw", IW),
        ("ur", UR),
        ("ja", JA),
        ("ko", KO),
    ]:
        write_locale(loc, mapping)


if __name__ == "__main__":
    main()
