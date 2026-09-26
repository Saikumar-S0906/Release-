#!/usr/bin/env python3
"""Batch-translate strings_en.json into i18n_cache for target locales.

Uses deep_translator with longer backoff, smaller checkpoints, and
placeholder/brand protection. Resume-safe.
"""
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
    "es": "es",
    "pt-rBR": "pt",
    "fr": "fr",
    "de": "de",
    "it": "it",
    "in": "id",
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
    "2×",
    "4×",
    "½×",
    "¼×",
    "⅛×",
    "2-up",
    "4-up",
}

PLACEHOLDER = re.compile(
    r"%(\d+\$)?(\.\d+)?[sdif]|%%|\\'|\\\"|\\n|\\t|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR|"
    r"PiP|RAW|WYSIWYG|LiteRT"
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

    # deep_translator sometimes mangles brackets
    text = text.replace("[[", "⟦").replace("]]", "⟧")
    return re.sub(r"⟦(\d+)⟧", repl, text)


def translate_one(translator: GoogleTranslator, text: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() in KEEP_AS_IS:
        return text
    # Short brand-only with punctuation
    if text.replace(".", "").strip() in KEEP_AS_IS:
        return text
    p, held = protect(text)
    for attempt in range(10):
        try:
            out = translator.translate(p)
            time.sleep(0.55)
            restored = restore(out or text, held)
            # Ensure placeholders survived
            for token in held:
                if token.startswith("%") or token.startswith("\\") or token in {
                    "PixiJoy", "WhatsApp", "LUT", "HDR", "GIF", "MP4", "EXIF",
                    "GPS", "sRGB", "Display P3", "LiteRT", "Media3", "Ultra HDR",
                }:
                    if token not in restored and token in text:
                        # fallback: keep English if placeholder lost
                        return text
            return restored
        except TooManyRequests:
            wait = min(90, 15 * (attempt + 1))
            print(f"    rate-limited, sleep {wait}s", flush=True)
            time.sleep(wait)
        except Exception as e:
            wait = 5 + attempt * 2
            print(f"    err {e!r}, sleep {wait}s", flush=True)
            time.sleep(wait)
    return text


def run_locale(loc: str) -> None:
    gcode = LOCALE_MAP[loc]
    path = CACHE / f"{loc}.json"
    done: dict[str, str] = {}
    if path.exists():
        done = json.loads(path.read_text(encoding="utf-8"))
        # Skip complete hand-crafted caches (not identical to English)
        if len(done) >= len(EN) and set(done) == set(EN):
            translated = sum(
                1
                for k, v in EN.items()
                if k != "app_name" and done.get(k) and done[k] != v
            )
            if translated >= len(EN) * 0.85:
                print(
                    f"{loc}: skip hand-crafted cache ({translated}/{len(EN)} translated)",
                    flush=True,
                )
                return
        print(f"{loc}: resume {len(done)}/{len(EN)}", flush=True)

    # Force app_name
    done["app_name"] = "PixiJoy"

    translator = GoogleTranslator(source="en", target=gcode)
    names = list(EN.keys())
    for i, name in enumerate(names):
        if name == "app_name":
            done[name] = "PixiJoy"
            continue
        if name in done and done[name] and done[name] != EN[name]:
            continue
        # If equal to EN and we're resuming a failed partial, re-translate
        # (except intentional keep-as-is short tokens)
        if name in done and done[name] == EN[name] and EN[name].strip() not in KEEP_AS_IS:
            # still English — retranslate
            pass
        elif name in done and done[name]:
            continue

        print(f"{loc}: {i + 1}/{len(names)} {name}", flush=True)
        done[name] = translate_one(translator, EN[name])
        if (i + 1) % 10 == 0:
            path.write_text(
                json.dumps(done, ensure_ascii=False, indent=1) + "\n",
                encoding="utf-8",
            )

    for name in names:
        done.setdefault(name, EN[name])
    done["app_name"] = "PixiJoy"
    # Ordered like EN
    ordered = {k: done[k] for k in names}
    path.write_text(
        json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
    )
    print(f"{loc}: wrote {path} ({len(ordered)} keys)", flush=True)


def main() -> None:
    targets = [a for a in sys.argv[1:] if not a.startswith("-")] or list(LOCALE_MAP)
    for loc in targets:
        run_locale(loc)


if __name__ == "__main__":
    main()
