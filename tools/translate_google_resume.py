#!/usr/bin/env python3
"""Translate via deep_translator Google with PH_N_ markers + resume."""
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

LOCALE_MAP = {
    "hi": "hi",
    "ar": "ar",
    "iw": "iw",
    "ur": "ur",
    "ja": "ja",
    "ko": "ko",
}

PLACEHOLDER = re.compile(
    r"%(\d+\$)?[\.0-9]*[sdif]|%%|\\'|\\\"|\\n|\\t|"
    r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR"
)
BRAND_ONLY = {
    "PixiJoy", "Sample", "HDR", "GIF", "MP4", "LUT", "SDR", "PiP", "SFX", "RAW",
    "½×", "¼×", "⅛×", "2×", "4×",
}


def protect(text: str) -> tuple[str, list[str]]:
    held: list[str] = []

    def repl(m: re.Match[str]) -> str:
        held.append(m.group(0))
        return f" PH{len(held) - 1}X "

    return PLACEHOLDER.sub(repl, text), held


def restore(text: str, held: list[str]) -> str:
    def repl(m: re.Match[str]) -> str:
        i = int(m.group(1))
        return held[i] if 0 <= i < len(held) else m.group(0)

    # Google often alters spacing/case: PH0X, ph 0 x, PH_0_X, etc.
    out = re.sub(r"PH\s*[_-]?\s*(\d+)\s*[_-]?\s*X", repl, text, flags=re.I)
    return out


def translate_one(translator: GoogleTranslator, text: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() in BRAND_ONLY:
        return text
    p, held = protect(text)
    for attempt in range(10):
        try:
            out = translator.translate(p)
            time.sleep(1.2)
            restored = restore(out or text, held)
            # verify format tokens
            ok = True
            for tok in held:
                if tok.startswith("%") and tok not in restored:
                    ok = False
                    break
                if tok in (
                    "PixiJoy", "WhatsApp", "LUT", "HDR", "GIF", "MP4", "EXIF", "GPS",
                    "sRGB", "Display P3", "LiteRT", "Media3", "Ultra HDR",
                ) and tok not in restored:
                    ok = False
                    break
            if not ok and attempt < 3:
                time.sleep(2)
                continue
            return restored
        except TooManyRequests:
            wait = 30 * (attempt + 1)
            print(f"    rate-limited, sleep {wait}s", flush=True)
            time.sleep(wait)
        except Exception as e:
            print(f"    err {e!r}, sleep 8s", flush=True)
            time.sleep(8)
    return text


def needs(name: str, cur: str) -> bool:
    if name == "app_name":
        return False
    en = EN[name]
    if not cur or not str(cur).strip():
        return True
    if "PH" in cur and re.search(r"PH\s*\d+\s*X", cur, re.I):
        return True
    if "@@" in cur or "⟦" in cur:
        return True
    if en.strip() in BRAND_ONLY:
        return False
    if cur == en and en.strip() not in BRAND_ONLY:
        if len(en) <= 3 or en in {"Off", "Grid", "Split", "Cut", "Soft", "Hard", "Light", "Medium", "Strong", "Wide", "Tight", "None", "Left", "Right", "Close", "Green", "Blue", "Sky", "Film", "Warm", "Cool", "Night", "Clean", "Original", "Mute", "Unmute", "Hide", "Show", "Protect", "Paint", "Brush", "Fade", "Mask", "Track", "Warp", "Blur", "Sharpen", "Vignette", "Curves", "Looks", "Layers", "Opacity", "Hue", "Saturation", "Exposure", "Contrast", "Color", "Text", "Audio", "Edit", "Add", "Layout", "Effects", "Retouch", "Compose", "Crop", "Light", "Tune", "Roll", "Scrub", "Play", "Pause", "Start", "End", "Back", "Cancel", "Apply", "Export", "Undo", "Redo", "Home", "Studio", "Library", "Photo", "Movie", "Video", "Camera", "Gallery", "Create", "View", "Share", "Done", "High", "Balanced", "Maximum", "Recommended", "Presets", "Stickers", "Collage", "Sample", "Recents", "Subject", "Background", "Whites", "Blacks", "Shadows", "Midtones", "Lights", "Reds", "Oranges", "Yellows", "Greens", "Cyans", "Blues", "Magentas", "Brightness", "Resolution", "Dynamics", "Denoise", "Soften", "Sky", "Fill", "Heal", "Erase", "Download", "Stop"}:
            return False
        return True
    return False


def translate_locale(loc: str, gcode: str) -> None:
    cache = ROOT / f"tools/i18n_cache/{loc}.json"
    cache.parent.mkdir(parents=True, exist_ok=True)
    done: dict[str, str] = {}
    if cache.exists():
        done = json.loads(cache.read_text(encoding="utf-8"))
        print(f"{loc}: resume {sum(1 for k in EN if k in done and done[k])}/{len(EN)}", flush=True)

    translator = GoogleTranslator(source="en", target=gcode)
    names = list(EN.keys())
    done["app_name"] = "PixiJoy"
    for i, name in enumerate(names):
        if not needs(name, done.get(name, "")):
            continue
        done[name] = translate_one(translator, EN[name])
        if (i + 1) % 5 == 0:
            cache.write_text(json.dumps(done, ensure_ascii=False, indent=1), encoding="utf-8")
            print(f"{loc}: {i + 1}/{len(names)}", flush=True)

    for name in names:
        done.setdefault(name, EN[name])
    done["app_name"] = "PixiJoy"
    cache.write_text(json.dumps(done, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{loc}: cache complete {len(done)}", flush=True)


def main() -> None:
    targets = [a for a in sys.argv[1:] if a in LOCALE_MAP] or list(LOCALE_MAP)
    for loc in targets:
        translate_locale(loc, LOCALE_MAP[loc])


if __name__ == "__main__":
    main()
