#!/usr/bin/env python3
"""Watch i18n_cache and emit XML when a locale reaches full key coverage."""
from __future__ import annotations

import json
import pathlib
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
CACHE = ROOT / "tools/i18n_cache"
EMITTED: set[str] = set()

LOCALES = [
    "es",
    "pt-rBR",
    "fr",
    "de",
    "it",
    "in",
    "hi",
    "ar",
    "iw",
    "ur",
    "ja",
    "ko",
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


def ready(loc: str) -> bool:
    p = CACHE / f"{loc}.json"
    if not p.exists():
        return False
    try:
        d = json.loads(p.read_text(encoding="utf-8"))
    except Exception:
        return False
    if len(d) < len(EN):
        return False
    miss = [k for k in EN if k not in d or not str(d[k]).strip()]
    return not miss


def main() -> None:
    deadline = time.time() + float(sys.argv[1]) if len(sys.argv) > 1 else time.time() + 3600
    while time.time() < deadline:
        for loc in LOCALES:
            if loc in EMITTED:
                continue
            if ready(loc):
                subprocess.check_call(
                    [sys.executable, str(ROOT / "tools/emit_locale_xml.py"), loc]
                )
                EMITTED.add(loc)
                print(f"emitted {loc} ({len(EMITTED)}/{len(LOCALES)})", flush=True)
        if len(EMITTED) == len(LOCALES):
            print("all locales emitted", flush=True)
            return
        # status
        parts = []
        for loc in LOCALES:
            p = CACHE / f"{loc}.json"
            n = 0
            if p.exists():
                try:
                    n = len(json.loads(p.read_text(encoding="utf-8")))
                except Exception:
                    n = -1
            parts.append(f"{loc}:{n}")
        print("status " + " ".join(parts), flush=True)
        time.sleep(20)
    print(f"timeout with {len(EMITTED)}/{len(LOCALES)} emitted", flush=True)


if __name__ == "__main__":
    main()
