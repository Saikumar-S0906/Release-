#!/usr/bin/env python3
"""Fill missing i18n_cache keys for a locale (placeholder-safe)."""
from __future__ import annotations

import json
import pathlib
import re
import sys
import time

from deep_translator import LingueeTranslator

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
CACHE = ROOT / "tools/i18n_cache"

TARGET = {
    "fr": "french",
    "it": "italian",
}

PLACEHOLDER = re.compile(
    r"%(\d+\$)?(\.\d+)?[sdif]|%%|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR|"
    r"PiP|RAW|WYSIWYG|Ken Burns|Neural frames|Play Store|\.tflite"
)


def protect(text: str) -> tuple[str, list[str]]:
    held: list[str] = []

    def repl(m: re.Match[str]) -> str:
        held.append(m.group(0))
        return f"⟦{len(held) - 1}⟧"

    return PLACEHOLDER.sub(repl, text), held


def restore(text: str, held: list[str]) -> str:
    def repl(m: re.Match[str]) -> str:
        i = int(m.group(1))
        return held[i] if 0 <= i < len(held) else m.group(0)

    return re.sub(r"⟦(\d+)⟧", repl, text)


def translate_one(translator: LingueeTranslator, text: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() == "PixiJoy":
        return text
    p, held = protect(text)
    for attempt in range(6):
        try:
            out = translator.translate(p)
            time.sleep(1.2)
            return restore(out or text, held)
        except Exception as e:
            wait = 3 + attempt * 2
            print(f"    err {e!r}, sleep {wait}s", flush=True)
            time.sleep(wait)
    return text


def fill(loc: str) -> None:
    gcode = TARGET[loc]
    path = CACHE / f"{loc}.json"
    done: dict[str, str] = {}
    if path.exists():
        done = json.loads(path.read_text(encoding="utf-8"))
    translator = LingueeTranslator(source="english", target=gcode)
    names = list(EN.keys())
    for i, name in enumerate(names):
        if name == "app_name":
            done[name] = "PixiJoy"
            continue
        if name in done and done[name] and done[name] != EN[name]:
            continue
        print(f"{loc}: {i + 1}/{len(names)} {name}", flush=True)
        done[name] = translate_one(translator, EN[name])
        if (i + 1) % 15 == 0:
            ordered = {k: done.get(k, EN[k]) for k in names}
            path.write_text(
                json.dumps(ordered, ensure_ascii=False, indent=1) + "\n",
                encoding="utf-8",
            )
    ordered = {k: done.get(k, EN[k]) for k in names}
    ordered["app_name"] = "PixiJoy"
    path.write_text(
        json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )
    print(f"{loc}: wrote {len(ordered)} keys", flush=True)


def main() -> None:
    for loc in sys.argv[1:] or ["fr", "it"]:
        fill(loc)


if __name__ == "__main__":
    main()
