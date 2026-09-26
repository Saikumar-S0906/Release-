#!/usr/bin/env python3
"""Validate i18n caches then emit values-*/strings.xml."""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
META = json.loads((ROOT / "tools/strings_meta.json").read_text(encoding="utf-8"))

PLACEHOLDER = re.compile(r"%(\d+\$)?(\.\d+)?[sdif]|%%")
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


def placeholders(s: str) -> list[str]:
    return PLACEHOLDER.findall(s) and PLACEHOLDER.findall(s) or []


def ph_tokens(s: str) -> list[str]:
    return PLACEHOLDER.findall(s)  # wrong - need full matches


def ph_full(s: str) -> list[str]:
    return [m.group(0) for m in PLACEHOLDER.finditer(s)]


def validate(loc: str, done: dict[str, str]) -> None:
    assert set(done) == set(EN), f"{loc}: key mismatch missing={set(EN)-set(done)} extra={set(done)-set(EN)}"
    assert done.get("app_name") == "PixiJoy", loc
    for name, en_v in EN.items():
        tr = done[name]
        en_ph = ph_full(en_v)
        tr_ph = ph_full(tr)
        if en_ph != tr_ph:
            raise SystemExit(f"{loc}.{name}: placeholders {en_ph} -> {tr_ph}\n  EN={en_v!r}\n  TR={tr!r}")
        for b in BRANDS:
            if b in en_v and b not in tr:
                raise SystemExit(f"{loc}.{name}: missing brand token {b!r}\n  TR={tr!r}")
        if "\\n" in en_v and "\\n" not in tr and "\n" not in tr:
            # only if literal escape was in EN JSON
            pass
    print(f"{loc}: ok keys={len(done)}")


def emit(loc: str, done: dict[str, str]) -> pathlib.Path:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- PixiJoy {loc} — machine-assisted translation; review before store release -->",
        "<resources>",
    ]
    for name in EN:
        attrs = META.get(name) or ""
        attr_s = f" {attrs}" if attrs else ""
        safe = done[name]
        if "&" in safe and not re.search(
            r"&(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);", safe
        ):
            safe = safe.replace("&", "&amp;")
        if "'" in safe:
            safe = safe.replace("\\'", "'").replace("'", r"\'")
        lines.append(f'    <string name="{name}"{attr_s}>{safe}</string>')
    lines.append("</resources>\n")
    dest = ROOT / f"app/src/main/res/values-{loc}/strings.xml"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text("\n".join(lines), encoding="utf-8")
    print("wrote", dest)
    return dest


def main() -> None:
    locs = ["es", "pt-rBR", "fr", "de", "it", "in"]
    for loc in locs:
        done = json.loads(
            (ROOT / f"tools/i18n_cache/{loc}.json").read_text(encoding="utf-8")
        )
        validate(loc, done)
        emit(loc, done)


if __name__ == "__main__":
    main()
