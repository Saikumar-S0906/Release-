#!/usr/bin/env python3
"""Throttled locale translation with resume support (MyMemory primary)."""
from __future__ import annotations

import json
import pathlib
import re
import sys
import time

from deep_translator import MyMemoryTranslator
from deep_translator.exceptions import TooManyRequests

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
META = json.loads((ROOT / "tools/strings_meta.json").read_text(encoding="utf-8"))

# MyMemory langpair targets (en|{code})
LOCALE_MAP = {
    "es": "es-ES",
    "pt-rBR": "pt-BR",
    "fr": "fr-FR",
    "de": "de-DE",
    "it": "it-IT",
    "in": "id-ID",
    "hi": "hi-IN",
    "ar": "ar-SA",
    "iw": "he-IL",
    "ur": "ur-PK",
    "ja": "ja-JP",
    "ko": "ko-KR",
    "zh-rCN": "zh-CN",
    "zh-rTW": "zh-TW",
    "th": "th-TH",
    "vi": "vi-VN",
    "tr": "tr-TR",
    "ru": "ru-RU",
    "pl": "pl-PL",
    "fil": "fil-PH",
    "ms": "ms-MY",
    "uk": "uk-UA",
}

PLACEHOLDER = re.compile(
    r"%(\d+\$)?[sdif]|%%|\\'|\\\"|\\n|\\t|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR"
)

SKIP_EXACT = {"PixiJoy", "Sample", "HDR", "GIF", "MP4", "LUT", "OK", "HD"}


def protect(text: str) -> tuple[str, list[str]]:
    held: list[str] = []

    def repl(m: re.Match[str]) -> str:
        held.append(m.group(0))
        return f"[[{len(held) - 1}]]"

    return PLACEHOLDER.sub(repl, text), held


def restore(text: str, held: list[str]) -> str:
    def repl(m: re.Match[str]) -> str:
        i = int(m.group(1))
        return held[i] if 0 <= i < len(held) else m.group(0)

    return re.sub(r"\[\[(\d+)\]\]", repl, text)


def translate_one(translator: MyMemoryTranslator, text: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() in SKIP_EXACT:
        return text
    p, held = protect(text)
    for attempt in range(8):
        try:
            out = translator.translate(p)
            time.sleep(0.35)
            return restore(out or text, held)
        except TooManyRequests:
            wait = 25 * (attempt + 1)
            print(f"    rate-limited, sleep {wait}s", flush=True)
            time.sleep(wait)
        except Exception as e:
            msg = str(e).lower()
            wait = 8 if "query length" in msg or "limit" in msg else 4
            print(f"    err {e!r}, sleep {wait}s", flush=True)
            time.sleep(wait)
            if "query length" in msg and len(p) > 400:
                # Split long strings on sentence boundaries
                parts = re.split(r"(?<=[.!?])\s+", p)
                if len(parts) > 1:
                    bits = [translate_one(translator, part) for part in parts]
                    return restore(" ".join(bits), held)
    return text


def write_xml(loc: str, translated: dict[str, str]) -> pathlib.Path:
    dest = ROOT / f"app/src/main/res/values-{loc}/strings.xml"
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- PixiJoy {loc} — machine-assisted translation; review before store release -->",
        "<resources>",
    ]
    for name, value in translated.items():
        attrs = META.get(name) or ""
        attr_s = f" {attrs}" if attrs else ""
        safe = value
        if "&" in safe and not re.search(
            r"&(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);", safe
        ):
            safe = safe.replace("&", "&amp;")
        if "'" in safe:
            safe = safe.replace("\\'", "'").replace("'", r"\'")
        lines.append(f'    <string name="{name}"{attr_s}>{safe}</string>')
    lines.append("</resources>\n")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text("\n".join(lines), encoding="utf-8")
    return dest


def translate_locale(loc: str, gcode: str) -> None:
    cache = ROOT / f"tools/i18n_cache/{loc}.json"
    cache.parent.mkdir(parents=True, exist_ok=True)
    done: dict[str, str] = {}
    if cache.exists():
        done = json.loads(cache.read_text(encoding="utf-8"))
        print(f"{loc}: resume {len(done)}/{len(EN)} from cache", flush=True)

    translator = MyMemoryTranslator(source="en-US", target=gcode)
    names = list(EN.keys())
    for i, name in enumerate(names):
        if name in done and str(done[name]).strip() and done[name] != EN[name]:
            continue
        if name in done and done[name] == EN[name] and name != "app_name":
            # re-translate English leftovers unless brand/identical
            pass
        done[name] = translate_one(translator, EN[name])
        if (i + 1) % 20 == 0:
            cache.write_text(
                json.dumps(done, ensure_ascii=False, indent=1), encoding="utf-8"
            )
            print(f"{loc}: {i + 1}/{len(names)}", flush=True)

    for name in names:
        done.setdefault(name, EN[name])
    cache.write_text(json.dumps(done, ensure_ascii=False, indent=1), encoding="utf-8")
    path = write_xml(loc, {n: done[n] for n in names})
    print(f"{loc}: wrote {path.relative_to(ROOT)}", flush=True)


def main() -> None:
    force = "--force" in sys.argv
    targets = [a for a in sys.argv[1:] if not a.startswith("--")] or list(LOCALE_MAP)
    for loc in targets:
        gcode = LOCALE_MAP[loc]
        cache = ROOT / f"tools/i18n_cache/{loc}.json"
        if (
            not force
            and cache.exists()
            and len(json.loads(cache.read_text(encoding="utf-8"))) >= len(EN)
        ):
            # Prefer cache even if XML header already says machine-assisted
            translated = json.loads(cache.read_text(encoding="utf-8"))
            # Skip only if not mostly English
            sample = [translated.get(k, "") for k in list(EN)[1:8]]
            en_sample = [EN[k] for k in list(EN)[1:8]]
            if sample != en_sample:
                write_xml(loc, {n: translated.get(n, EN[n]) for n in EN})
                print(f"{loc}: skip (cache complete)", flush=True)
                continue
        translate_locale(loc, gcode)


if __name__ == "__main__":
    main()
