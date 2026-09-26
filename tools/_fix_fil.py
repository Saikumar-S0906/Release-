#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
p = ROOT / "tools/i18n_cache/fil.json"
d = json.loads(p.read_text(encoding="utf-8"))
d["studio_speed_ramp_values"] = "Simula ×%1$.1f → dulo ×%2$.1f"
d["session_compare_before"] = "Bago · bitawan para sa Pagkatapos"
p.write_text(json.dumps(d, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print("fixed fil gaps")
