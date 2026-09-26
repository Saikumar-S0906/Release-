#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Translate remaining locales via Bing (translators pkg) with placeholder protection."""
from __future__ import annotations

import json
import pathlib
import re
import time

import translators as ts

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
CACHE = ROOT / "tools/i18n_cache"
CACHE.mkdir(parents=True, exist_ok=True)

# Android locale -> Bing language code
LOCALE_MAP = {
    "th": "th",
    "vi": "vi",
    "tr": "tr",
    "fil": "fil",  # Filipino; fallback tl
    "ms": "ms",
}

PLACEHOLDER = re.compile(
    r"%(\d+\$)?[\.0-9]*[sdif]|%%|\\'|\\\"|\\n|\\t|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR|"
    r"PiP|WYSIWYG|SDR|RAW|UNet|VO|SFX|EQ|FX|EV|Ken Burns"
)

KEEP_AS_IS = {
    "PixiJoy",
    "Sample",
    "HDR",
    "GIF",
    "MP4",
    "LUT",
    "SDR",
    "sRGB",
    "Display P3",
    "½×",
    "¼×",
    "⅛×",
    "2×",
    "4×",
    "PiP",
}


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

    # Bing may alter brackets
    out = re.sub(r"⟦\s*(\d+)\s*⟧", repl, text)
    out = re.sub(r"\[\s*(\d+)\s*\]", repl, out)
    out = re.sub(r"【\s*(\d+)\s*】", repl, out)
    return out


def translate_one(text: str, lang: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() in KEEP_AS_IS:
        return text
    p, held = protect(text)
    for attempt in range(8):
        try:
            # fil sometimes needs tl
            codes = [lang]
            if lang == "fil":
                codes = ["fil", "tl"]
            last_err = None
            for code in codes:
                try:
                    out = ts.translate_text(
                        p, translator="bing", from_language="en", to_language=code
                    )
                    time.sleep(0.35)
                    return restore(out or text, held)
                except Exception as e:
                    last_err = e
                    continue
            raise last_err  # type: ignore
        except Exception as e:
            wait = 3 * (attempt + 1)
            print(f"    err {e!r}, sleep {wait}s", flush=True)
            time.sleep(wait)
    return text


def is_mostly_english(done: dict[str, str]) -> bool:
    if len(done) < len(EN):
        return True
    same = sum(1 for k, v in EN.items() if done.get(k) == v)
    return same > len(EN) * 0.8


def needs_retranslate(name: str, en: str, cur: str | None) -> bool:
    if name == "app_name":
        return False
    if not cur:
        return True
    if cur == en:
        if en.strip() in KEEP_AS_IS:
            return False
        # short shared UI tokens often stay English; still translate longer copy
        if len(en) <= 12 and en.isascii() and " " not in en.strip():
            return False
        return True
    return False


def translate_locale(loc: str, lang: str, *, force_gapfill: bool = False) -> None:
    cache = CACHE / f"{loc}.json"
    done: dict[str, str] = {}
    if cache.exists():
        done = json.loads(cache.read_text(encoding="utf-8"))
        gaps = sum(1 for k, v in EN.items() if needs_retranslate(k, v, done.get(k)))
        if len(done) >= len(EN) and not is_mostly_english(done) and gaps == 0 and not force_gapfill:
            print(f"{loc}: skip complete ({len(done)})", flush=True)
            return
        if is_mostly_english(done):
            print(f"{loc}: cache is English scaffold — retranslating", flush=True)
            done = {}
        else:
            print(f"{loc}: resume {len(done)}/{len(EN)} gaps={gaps}", flush=True)

    names = list(EN.keys())
    for i, name in enumerate(names):
        if not needs_retranslate(name, EN[name], done.get(name)):
            if name == "app_name":
                done[name] = "PixiJoy"
            elif name not in done:
                done[name] = EN[name]
            continue
        if name == "app_name":
            done[name] = "PixiJoy"
            continue
        done[name] = translate_one(EN[name], lang)
        if (i + 1) % 20 == 0:
            cache.write_text(
                json.dumps(done, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
            )
            print(f"{loc}: {i + 1}/{len(names)}", flush=True)

    for name in names:
        done.setdefault(name, EN[name])
    done["app_name"] = "PixiJoy"
    ordered = {k: done[k] for k in names}
    cache.write_text(
        json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )
    same = sum(1 for k in names if ordered[k] == EN[k])
    print(f"{loc}: wrote {len(ordered)} identical_en={same}", flush=True)


def main() -> None:
    import sys

    args = [a for a in sys.argv[1:] if a != "--force-gaps"]
    force = "--force-gaps" in sys.argv[1:]
    targets = args or list(LOCALE_MAP.keys())
    for loc in targets:
        translate_locale(loc, LOCALE_MAP[loc], force_gapfill=force)


if __name__ == "__main__":
    main()
