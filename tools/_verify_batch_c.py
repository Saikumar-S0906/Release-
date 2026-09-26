#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
for loc in ["zh-rCN", "zh-rTW", "th", "vi", "tr", "ru", "pl", "fil", "ms", "uk"]:
    d = json.loads((ROOT / f"tools/i18n_cache/{loc}.json").read_text(encoding="utf-8"))
    xml_path = ROOT / f"app/src/main/res/values-{loc}/strings.xml"
    xml = xml_path.read_text(encoding="utf-8")
    names = re.findall(r'<string name="([^"]+)"', xml)
    long_same = sum(1 for k, v in EN.items() if d[k] == v and len(v) > 25)
    assert set(d) == set(EN)
    assert set(names) == set(EN)
    assert d["app_name"] == "PixiJoy"
    assert "machine-assisted translation" in xml
    print(
        loc,
        "json",
        len(d),
        "xml",
        len(names),
        "long_same",
        long_same,
        "sample",
        d["studio_accept"],
    )
print("ALL OK")
