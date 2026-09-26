#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json, pathlib, re
ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
src = (ROOT / "tools/_fill_batch_c1.py").read_text(encoding="utf-8")
m = re.search(r"^ZH_CN = \{", src, re.M)
end = src.find("\n# fmt: on", m.start())
ns = {}
exec(src[m.start():end], ns)
ZH = ns["ZH_CN"]
print(len(ZH), len(EN), len(set(EN)-set(ZH)), sorted(set(EN)-set(ZH))[:15])
