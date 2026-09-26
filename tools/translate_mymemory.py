#!/usr/bin/env python3
"""Translate strings_en.json via MyMemory with resume + placeholder protection."""
from __future__ import annotations

import json
import pathlib
import re
import sys
import time
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))

# Android values-* folder -> MyMemory langpair target
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

BRAND_ONLY = {"PixiJoy", "Sample", "HDR", "GIF", "MP4", "LUT", "SDR", "PiP", "SFX", "RAW"}


def protect(text: str) -> tuple[str, list[str]]:
    held: list[str] = []

    def repl(m: re.Match[str]) -> str:
        held.append(m.group(0))
        return f"@@{len(held) - 1}@@"

    return PLACEHOLDER.sub(repl, text), held


def restore(text: str, held: list[str]) -> str:
    def repl(m: re.Match[str]) -> str:
        i = int(m.group(1))
        return held[i] if 0 <= i < len(held) else m.group(0)

    return re.sub(r"@@(\d+)@@", repl, text)


def mymemory(text: str, target: str) -> str:
    q = urllib.parse.quote(text[:450])  # API soft limit
    url = f"https://api.mymemory.translated.net/get?q={q}&langpair=en|{target}"
    req = urllib.request.Request(url, headers={"User-Agent": "PixiJoy-i18n/1.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        data = json.loads(r.read().decode("utf-8"))
    status = data.get("responseStatus")
    if status != 200:
        raise RuntimeError(f"MyMemory status {status}: {data}")
    out = (data.get("responseData") or {}).get("translatedText") or ""
    # MyMemory sometimes returns the query unchanged or ERROR messages
    if out.upper().startswith("MYMEMORY WARNING") or out.upper().startswith("QUERY LENGTH"):
        raise RuntimeError(out)
    return out


def translate_one(text: str, target: str) -> str:
    if not text or not text.strip():
        return text
    if text.strip() in BRAND_ONLY:
        return text
    # Keep pure format / symbol strings
    if re.fullmatch(r"[%½¼⅛×←→·⌄\d\s\$\.\:\-—–×xX/\\'\"a-zA-Z]*", text) and any(
        c in text for c in "%½¼⅛×←→"
    ):
        # still translate labels mixed with symbols below
        pass
    p, held = protect(text)
    for attempt in range(8):
        try:
            out = mymemory(p, target)
            time.sleep(0.55)
            restored = restore(out, held)
            # If placeholders vanished, retry once with original
            for i, tok in enumerate(held):
                if tok.startswith("%") and tok not in restored and f"⟦{i}⟧" not in out:
                    # try to put back if clearly missing
                    pass
            return restored
        except Exception as e:
            wait = 8 * (attempt + 1)
            print(f"    err {e!r}, sleep {wait}s", flush=True)
            time.sleep(wait)
    return text


def needs_translate(name: str, current: str) -> bool:
    if name == "app_name":
        return False
    en = EN[name]
    if not current or not str(current).strip():
        return True
    if "@@" in current or "⟦" in current:
        return True
    if en.strip() in BRAND_ONLY:
        return False
    # Re-do if still identical to English (except brand-only / symbols)
    if current == en and en.strip() not in BRAND_ONLY:
        # Allow intentional identical technical labels
        if en in {"PiP", "SFX", "RAW", "SDR", "Off", "Grid", "Split", "Title", "Caption"}:
            return False
        return True
    return False


def translate_locale(loc: str, target: str) -> None:
    cache = ROOT / f"tools/i18n_cache/{loc}.json"
    cache.parent.mkdir(parents=True, exist_ok=True)
    done: dict[str, str] = {}
    if cache.exists():
        done = json.loads(cache.read_text(encoding="utf-8"))
        print(f"{loc}: resume {len(done)}/{len(EN)}", flush=True)

    names = list(EN.keys())
    done["app_name"] = "PixiJoy"
    for i, name in enumerate(names):
        cur = done.get(name, "")
        if not needs_translate(name, cur):
            if name == "app_name":
                done[name] = "PixiJoy"
            continue
        done[name] = translate_one(EN[name], target)
        if (i + 1) % 10 == 0:
            cache.write_text(json.dumps(done, ensure_ascii=False, indent=1), encoding="utf-8")
            print(f"{loc}: {i + 1}/{len(names)}", flush=True)

    for name in names:
        done.setdefault(name, EN[name])
    done["app_name"] = "PixiJoy"
    cache.write_text(json.dumps(done, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{loc}: cache complete {len(done)}", flush=True)


def main() -> None:
    targets = sys.argv[1:] or list(LOCALE_MAP.keys())
    for loc in targets:
        translate_locale(loc, LOCALE_MAP[loc])


if __name__ == "__main__":
    main()
