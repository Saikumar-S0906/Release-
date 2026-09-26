#!/usr/bin/env python3
"""Emit values-*/strings.xml from tools/i18n_cache/*.json."""
from __future__ import annotations

import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
META = json.loads((ROOT / "tools/strings_meta.json").read_text(encoding="utf-8"))


def emit(loc: str) -> None:
    cache = ROOT / f"tools/i18n_cache/{loc}.json"
    done = json.loads(cache.read_text(encoding="utf-8"))
    missing = [k for k in EN if k not in done or not str(done[k]).strip()]
    for k in missing:
        done[k] = EN[k]
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- PixiJoy {loc} — machine-assisted translation; review before store release -->",
        "<resources>",
    ]
    for name in EN:
        attrs = META.get(name) or ""
        attr_s = f" {attrs}" if attrs else ""
        safe = str(done[name])
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
    print(
        f"wrote {dest.relative_to(ROOT)} keys={len(EN)} filled_from_en={len(missing)}"
    )


def main() -> None:
    locs = sys.argv[1:] or [
        p.stem for p in (ROOT / "tools/i18n_cache").glob("*.json")
    ]
    for loc in locs:
        emit(loc)


if __name__ == "__main__":
    main()
