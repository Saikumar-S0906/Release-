#!/usr/bin/env python3
"""Translate target locales to i18n_cache using Google (resume-safe)."""
from __future__ import annotations

import json
import pathlib
import re
import sys
import time

from deep_translator import GoogleTranslator
from deep_translator.exceptions import TooManyRequests

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
CACHE = ROOT / "tools/i18n_cache"
CACHE.mkdir(parents=True, exist_ok=True)

LOCALE_MAP = {
    "zh-rCN": "zh-CN",
    "zh-rTW": "zh-TW",
    "th": "th",
    "vi": "vi",
    "tr": "tr",
    "ru": "ru",
    "pl": "pl",
    "fil": "tl",
    "ms": "ms",
    "uk": "uk",
}

KEEP_AS_IS = {
    "PixiJoy",
    "Sample",
    "HDR",
    "SDR",
    "GIF",
    "MP4",
    "LUT",
    "EXIF",
    "GPS",
    "sRGB",
    "Display P3",
    "LiteRT",
    "Media3",
    "Ultra HDR",
    "PiP",
    "RAW",
    "VO",
    "SFX",
    "EQ",
    "WYSIWYG",
    "WhatsApp",
}

PLACEHOLDER = re.compile(
    r"%(\d+\$)?(\.\d+)?[sdif]|%%|\\'|\\\"|\\n|\\t|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR|"
    r"PiP|RAW|WYSIWYG|Ultra HDR"
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

    text = text.replace("[[", "⟦").replace("]]", "⟧")
    return re.sub(r"⟦(\d+)⟧", repl, text)


def translate_one(translator: GoogleTranslator, text: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() in KEEP_AS_IS:
        return text
    p, held = protect(text)
    for attempt in range(8):
        try:
            out = translator.translate(p)
            time.sleep(0.25)
            restored = restore(out or text, held)
            for token in held:
                if token.startswith("%") and token not in restored:
                    return text
            return restored
        except TooManyRequests:
            time.sleep(20 * (attempt + 1))
        except Exception:
            time.sleep(3 + attempt)
    return text


def run_locale(loc: str, gcode: str) -> None:
    path = CACHE / f"{loc}.json"
    done: dict[str, str] = {}
    if path.exists():
        done = json.loads(path.read_text(encoding="utf-8"))
    done["app_name"] = "PixiJoy"
    translator = GoogleTranslator(source="en", target=gcode)
    names = list(EN.keys())
    for i, name in enumerate(names):
        if name == "app_name":
            done[name] = "PixiJoy"
            continue
        if name in done and done[name] and done[name] != EN[name]:
            continue
        print(f"{loc}: {i + 1}/{len(names)} {name}", flush=True)
        done[name] = translate_one(translator, EN[name])
        if (i + 1) % 25 == 0:
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
    print(f"{loc}: done {len(ordered)} keys", flush=True)


def main() -> None:
    targets = [a for a in sys.argv[1:] if not a.startswith("-")] or list(LOCALE_MAP)
    for loc in targets:
        run_locale(loc, LOCALE_MAP[loc])


if __name__ == "__main__":
    main()
