#!/usr/bin/env python3
"""Build i18n_cache/*.json from tools/i18n_master.tsv (UTF-8)."""
from __future__ import annotations

import csv
import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
TSV_GLOB = "i18n_master*.tsv"
OUT = ROOT / "tools/i18n_cache"
LOCALES = [
    "zh-rCN",
    "zh-rTW",
    "th",
    "vi",
    "tr",
    "ru",
    "pl",
    "fil",
    "ms",
    "uk",
]


def main() -> None:
    by_key: dict[str, dict[str, str]] = {}
    for tsv in sorted((ROOT / "tools").glob(TSV_GLOB)):
        with tsv.open(encoding="utf-8", newline="") as f:
            reader = csv.DictReader(f, delimiter="\t")
            for row in reader:
                by_key[row["key"]] = row
    for rem in sorted((ROOT / "tools").glob("i18n_remaining*.json")):
        extra = json.loads(rem.read_text(encoding="utf-8"))
        for key, locs in extra.items():
            row = by_key.setdefault(key, {"key": key})
            for loc, val in locs.items():
                row[loc] = val
    missing_keys = [k for k in EN if k not in by_key]
    if missing_keys:
        raise SystemExit(f"TSV missing {len(missing_keys)} keys, e.g. {missing_keys[:5]}")
    sample = next(iter(by_key.values()))
    for loc in LOCALES:
        if loc not in sample:
            raise SystemExit(f"TSV missing column {loc}")
    OUT.mkdir(parents=True, exist_ok=True)
    for loc in LOCALES:
        out = {k: by_key[k][loc] for k in EN}
        out["app_name"] = "PixiJoy"
        path = OUT / f"{loc}.json"
        path.write_text(
            json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
        )
        miss = set(EN) - set(out)
        print(f"{loc}: keys={len(out)} missing={len(miss)}")


if __name__ == "__main__":
    main()
