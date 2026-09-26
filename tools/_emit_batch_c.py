#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Emit values-*/strings.xml for batch C locales."""
from __future__ import annotations

import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
META = json.loads((ROOT / "tools/strings_meta.json").read_text(encoding="utf-8"))

LOCS = ["zh-rCN", "zh-rTW", "th", "vi", "tr", "ru", "pl", "fil", "ms", "uk"]

for loc in LOCS:
    done = json.loads((ROOT / f"tools/i18n_cache/{loc}.json").read_text(encoding="utf-8"))
    assert set(done) == set(EN), (loc, len(set(done) & set(EN)), len(EN))
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- PixiJoy {loc} — machine-assisted translation; review before store release -->",
        "<resources>",
    ]
    for name in EN:
        attrs = META.get(name) or ""
        attr_s = f" {attrs}" if attrs else ""
        safe = done.get(name, EN[name])
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
    print("wrote", dest, len(done))
