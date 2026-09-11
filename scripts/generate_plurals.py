#!/usr/bin/env python3
"""Generate plurals.xml for all locales from values/plurals.xml."""
from __future__ import annotations

import argparse
import json
import os
import time
import xml.etree.ElementTree as ET
from xml.dom import minidom

from generate_full_translations import LOCALE_LANG, RES_DIR, load_cache, save_cache

BASE_PLURALS = os.path.join(RES_DIR, "values", "plurals.xml")
CACHE_PATH = os.path.join(os.path.dirname(__file__), "plurals_cache.json")


def parse_plurals(path: str) -> dict[str, dict[str, str]]:
    tree = ET.parse(path)
    result: dict[str, dict[str, str]] = {}
    for plural in tree.getroot():
        if plural.tag != "plurals":
            continue
        name = plural.attrib.get("name")
        if not name:
            continue
        items: dict[str, str] = {}
        for item in plural:
            if item.tag == "item":
                qty = item.attrib.get("quantity")
                if qty:
                    items[qty] = item.text or ""
        result[name] = items
    return result


def load_plural_cache() -> dict:
    if os.path.exists(CACHE_PATH):
        with open(CACHE_PATH, encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_plural_cache(cache: dict) -> None:
    with open(CACHE_PATH, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=2)


def translate_items(items: dict[str, str], lang: str, translate: bool, cache_bucket: dict) -> dict[str, str]:
    out: dict[str, str] = {}
    pending: list[tuple[str, str]] = []
    for qty, text in items.items():
        cache_key = f"{qty}::{text}"
        if cache_key in cache_bucket:
            out[qty] = cache_bucket[cache_key]
        elif not translate:
            out[qty] = text
        else:
            pending.append((qty, text))

    if pending and translate:
        from deep_translator import GoogleTranslator

        translator = GoogleTranslator(source="en", target=lang)
        texts = [text for _, text in pending]
        try:
            translated = translator.translate_batch(texts)
        except Exception:
            translated = []
            for t in texts:
                try:
                    translated.append(translator.translate(t))
                except Exception:
                    translated.append(t)
                time.sleep(0.05)
        for (qty, original), value in zip(pending, translated):
            cache_key = f"{qty}::{original}"
            cache_bucket[cache_key] = value or original
            out[qty] = value or original
            time.sleep(0.02)
    return out


def write_plurals_xml(path: str, plurals: dict[str, dict[str, str]]) -> None:
    root = ET.Element("resources")
    for name in sorted(plurals.keys()):
        plural_elem = ET.SubElement(root, "plurals", name=name)
        for qty in sorted(plurals[name].keys()):
            item = ET.SubElement(plural_elem, "item", quantity=qty)
            item.text = plurals[name][qty]
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
    args = parser.parse_args()

    base = parse_plurals(BASE_PLURALS)
    cache = load_plural_cache()

    for folder, lang in sorted(LOCALE_LANG.items()):
        bucket = cache.setdefault(lang, {})
        localized: dict[str, dict[str, str]] = {}
        for name, items in base.items():
            localized[name] = translate_items(items, lang, args.translate, bucket)
        out_path = os.path.join(RES_DIR, folder, "plurals.xml")
        write_plurals_xml(out_path, localized)
        save_plural_cache(cache)
        print(f"Wrote {out_path}")
    print("Done.")


if __name__ == "__main__":
    main()
