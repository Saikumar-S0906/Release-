#!/usr/bin/env python3
"""Board asset helper: list flows boards and emit manifest stubs.

Usage:
  python tools/board_assets/list_boards.py
  python tools/board_assets/list_boards.py --write-manifest

Cropping phone frames is semi-manual: add entries to crops.json then run crop_export
when Pillow is available.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FLOWS = ROOT / "flows"
ADDITIONAL = FLOWS / "additional"
MANIFEST = ROOT / "docs" / "BOARD_ASSET_MANIFEST.md"
DRAWABLE = ROOT / "app" / "src" / "main" / "res" / "drawable"


def list_boards() -> list[Path]:
    files: list[Path] = []
    for folder in (FLOWS, ADDITIONAL):
        if not folder.exists():
            continue
        files.extend(sorted(folder.glob("*.png")))
        files.extend(sorted(folder.glob("*.jpg")))
        files.extend(sorted(folder.glob("*.jpeg")))
    return files


def existing_drawables() -> list[str]:
    if not DRAWABLE.exists():
        return []
    return sorted(p.name for p in DRAWABLE.glob("board_*"))


def write_manifest(boards: list[Path], drawables: list[str]) -> None:
    lines = [
        "# Board asset manifest",
        "",
        "Convention: `res/drawable/board_<flow>_<role>.webp|png`",
        "Roles: logo, hero, tile_*, result_*, poster_*, ba_before, ba_after",
        "",
        "## Packed drawables",
        "",
    ]
    for name in drawables:
        lines.append(f"- `{name}`")
    lines.extend(["", "## Source boards", ""])
    for p in boards:
        rel = p.relative_to(ROOT).as_posix()
        lines.append(f"- `{rel}`")
    lines.extend(
        [
            "",
            "## Crop pipeline",
            "",
            "1. Record phone-frame coords in `tools/board_assets/crops.json`.",
            "2. `pip install pillow` then `python tools/board_assets/crop_export.py`.",
            "3. Or generate stand-ins with Cursor GenerateImage (board PNG as reference).",
            "",
        ]
    )
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {MANIFEST}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write-manifest", action="store_true")
    args = parser.parse_args()
    boards = list_boards()
    drawables = existing_drawables()
    print(f"boards={len(boards)} drawables={len(drawables)}")
    if args.write_manifest:
        write_manifest(boards, drawables)
    else:
        for p in boards[:5]:
            print(" ", p.name)
        print("  ...")


if __name__ == "__main__":
    main()
