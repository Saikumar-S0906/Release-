#!/usr/bin/env python3
"""Extract EN strings and generate localized values-*/strings.xml for PixiJoy."""
from __future__ import annotations

import json
import re
import pathlib
import xml.sax.saxutils as xu

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = ROOT / "app/src/main/res/values/strings.xml"
OUT_JSON = ROOT / "tools/strings_en.json"

# CapCut App Store set + DE/HI (Lightroom). Android resource qualifiers.
LOCALES = [
    "es",
    "pt-rBR",
    "fr",
    "de",
    "it",
    "in",  # Indonesian
    "hi",
    "ar",
    "iw",  # Hebrew
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


def parse_strings(path: pathlib.Path) -> dict[str, dict]:
    text = path.read_text(encoding="utf-8")
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    strings: dict[str, dict] = {}
    for m in re.finditer(
        r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>', text, re.S
    ):
        name, attrs, val = m.group(1), m.group(2).strip(), m.group(3)
        strings[name] = {"attrs": attrs, "value": val}
    return strings


def write_xml(strings: dict[str, dict], dest: pathlib.Path, header: str) -> None:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- {header} -->",
        "<resources>",
    ]
    for name, meta in strings.items():
        attrs = f" {meta['attrs']}" if meta["attrs"] else ""
        # Values are already XML-escaped from source; keep as-is.
        lines.append(f'    <string name="{name}"{attrs}>{meta["value"]}</string>')
    lines.append("</resources>")
    lines.append("")
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text("\n".join(lines), encoding="utf-8")


def main() -> None:
    strings = parse_strings(EN)
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(
        json.dumps({k: v["value"] for k, v in strings.items()}, ensure_ascii=False, indent=1),
        encoding="utf-8",
    )
    print(f"extracted {len(strings)} strings -> {OUT_JSON}")

    # Scaffold: copy EN into each locale folder so the project builds while
    # translations are filled. A follow-up pass overwrites with real copy.
    meta_path = ROOT / "tools/strings_meta.json"
    meta_path.write_text(
        json.dumps({k: v["attrs"] for k, v in strings.items()}, ensure_ascii=False, indent=1),
        encoding="utf-8",
    )
    for loc in LOCALES:
        dest = ROOT / f"app/src/main/res/values-{loc}/strings.xml"
        write_xml(
            strings,
            dest,
            f"PixiJoy {loc} — scaffold (replace with professional translation)",
        )
        print(f"wrote {dest.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
