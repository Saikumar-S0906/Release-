#!/usr/bin/env python3
"""Append remaining i18n TSV rows (generated offline)."""
from __future__ import annotations

import csv
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
MISSING = (ROOT / "tools/_missing_keys.txt").read_text(encoding="utf-8").strip().splitlines()
DATA = json.loads((ROOT / "tools/i18n_remaining.json").read_text(encoding="utf-8"))
LOCALES = ["zh-rCN", "zh-rTW", "th", "vi", "tr", "ru", "pl", "fil", "ms", "uk"]
OUT = ROOT / "tools/i18n_master_part3.tsv"


def main() -> None:
    lines = ["key\t" + "\t".join(LOCALES)]
    for key in MISSING:
        if key not in DATA:
            raise SystemExit(f"missing translation block: {key}")
        row = DATA[key]
        for loc in LOCALES:
            if loc not in row:
                raise SystemExit(f"{key} missing {loc}")
        lines.append(key + "\t" + "\t".join(row[loc] for loc in LOCALES))
    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT} rows={len(lines)-1}")


if __name__ == "__main__":
    main()
