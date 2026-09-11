#!/usr/bin/env python3
"""Generate complete strings.xml for all PixiJoy locales."""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
import xml.etree.ElementTree as ET
from xml.dom import minidom

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

RES_DIR = os.path.normpath(os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "res"))
BASE_STRINGS = os.path.join(RES_DIR, "values", "strings.xml")
CACHE_PATH = os.path.join(SCRIPT_DIR, "translation_cache.json")
BATCH_SIZE = 40

LOCALE_LANG = {
    "values-hi": "hi",
    "values-es": "es",
    "values-pt": "pt",
    "values-fr": "fr",
    "values-de": "de",
    "values-ar": "ar",
    "values-zh-rCN": "zh-CN",
    "values-zh-rTW": "zh-TW",
    "values-ja": "ja",
    "values-ko": "ko",
    "values-ru": "ru",
    "values-in": "id",
    "values-tr": "tr",
    "values-vi": "vi",
    "values-th": "th",
    "values-bn": "bn",
    "values-te": "te",
    "values-ta": "ta",
    "values-mr": "mr",
    "values-ur": "ur",
    "values-it": "it",
    "values-pl": "pl",
    "values-nl": "nl",
    "values-ms": "ms",
    "values-fil": "tl",
}

SKIP_KEYS = {"app_name", "privacy_url", "terms_url", "support_email", "privacy_policy_url", "terms_url"}
SKIP_PATTERNS = [
    re.compile(r"^https?://"),
    re.compile(r"^ca-app-pub-"),
    re.compile(r"^com\.pixijoy"),
]

from generate_translations import LOCALES as MANUAL_OVERRIDES  # type: ignore


def parse_strings(path: str) -> dict[str, str]:
    tree = ET.parse(path)
    return {
        child.attrib["name"]: child.text or ""
        for child in tree.getroot()
        if child.tag == "string" and child.attrib.get("name")
    }


def should_skip_translation(key: str, value: str) -> bool:
    if key in SKIP_KEYS:
        return True
    return any(p.search(value) for p in SKIP_PATTERNS)


def load_cache() -> dict:
    if os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_cache(cache: dict) -> None:
    with open(CACHE_PATH, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=2)


def translate_batch(texts: list[str], lang: str, translator) -> list[str]:
    if not texts:
        return []
    try:
        return translator.translate_batch(texts)
    except Exception:
        results: list[str] = []
        for t in texts:
            try:
                results.append(translator.translate(t) or t)
            except Exception:
                results.append(t)
            time.sleep(0.05)
        return results


def build_locale_strings(
    base: dict[str, str],
    folder: str,
    translate: bool,
    cache: dict,
) -> dict[str, str]:
    lang = LOCALE_LANG[folder]
    manual = MANUAL_OVERRIDES.get(folder, {})
    out: dict[str, str] = {}
    cache_bucket = cache.setdefault(lang, {})

    pending_keys: list[str] = []
    pending_texts: list[str] = []

    def flush_batch(translator) -> None:
        nonlocal pending_keys, pending_texts
        if not pending_keys:
            return
        results = translate_batch(pending_texts, lang, translator)
        for key, english, translated in zip(pending_keys, pending_texts, results):
            value = translated or english
            cache_bucket[key] = value
            out[key] = value
        save_cache(cache)
        pending_keys = []
        pending_texts = []
        time.sleep(0.15)

    translator = None
    if translate:
        from deep_translator import GoogleTranslator
        translator = GoogleTranslator(source="en", target=lang)

    for key, english in base.items():
        if key in manual:
            out[key] = manual[key]
            continue
        if should_skip_translation(key, english):
            out[key] = english
            continue
        if key in cache_bucket:
            out[key] = cache_bucket[key]
            continue
        if not translate:
            out[key] = english
            continue
        pending_keys.append(key)
        pending_texts.append(english)
        if len(pending_keys) >= BATCH_SIZE:
            flush_batch(translator)

    if translate and pending_keys:
        flush_batch(translator)

    return out


def escape_android_string(text: str) -> str:
    if not text:
        return text
    escaped = text.replace("\\", "\\\\")
    escaped = escaped.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    escaped = escaped.replace("'", "\\'")
    escaped = escaped.replace('"', '\\"')
    return escaped


def write_strings_xml(path: str, strings: dict[str, str]) -> None:
    root = ET.Element("resources")
    for key in sorted(strings.keys()):
        elem = ET.SubElement(root, "string", name=key)
        elem.text = escape_android_string(strings[key])
    xml_str = ET.tostring(root, encoding="unicode")
    pretty = minidom.parseString(xml_str).toprettyxml(indent="    ")
    lines = [line for line in pretty.split("\n") if line.strip()]
    content = '<?xml version="1.0" encoding="utf-8"?>\n' + "\n".join(lines[1:]) + "\n"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--translate", action="store_true")
    parser.add_argument("--locale", help="Only process one folder, e.g. values-hi")
    args = parser.parse_args()

    base = parse_strings(BASE_STRINGS)
    cache = load_cache()
    folders = [args.locale] if args.locale else sorted(LOCALE_LANG)

    for folder in folders:
        if folder not in LOCALE_LANG:
            print(f"Unknown locale folder: {folder}")
            continue
        strings = build_locale_strings(base, folder, args.translate, cache)
        out_path = os.path.join(RES_DIR, folder, "strings.xml")
        write_strings_xml(out_path, strings)
        save_cache(cache)
        print(f"Wrote {out_path} ({len(strings)} strings)", flush=True)
    print("Done.", flush=True)


if __name__ == "__main__":
    main()
