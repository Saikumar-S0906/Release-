#!/usr/bin/env python3
"""Rename PixiJoy -> PixiJoy across the Android project."""
import os
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

REPLACEMENTS = [
    ("com.pixijoy.ai", "com.pixijoy.ai"),
    ("PixiJoy", "PixiJoy"),
    ("pixijoy", "pixijoy"),
    ("PIXIJOY", "PIXIJOY"),
]

SKIP_DIRS = {".git", "build", ".gradle", ".idea", "node_modules"}
TEXT_EXTENSIONS = {
    ".kt", ".kts", ".xml", ".properties", ".md", ".py", ".json", ".pro", ".txt"
}

FILE_RENAMES = {
    "PixiJoyApp.kt": "PixiJoyApp.kt",
    "PixiJoyAppRoot.kt": "PixiJoyAppRoot.kt",
    "PixiJoyComponents.kt": "PixiJoyComponents.kt",
    "PixiJoyBackendApi.kt": "PixiJoyBackendApi.kt",
    "MockPixiJoyBackendApi.kt": "MockPixiJoyBackendApi.kt",
    "NoOpPixiJoyBackendApi.kt": "NoOpPixiJoyBackendApi.kt",
}

DIR_MOVES = [
    (ROOT / "app/src/main/java/com/pixijoy/ai", ROOT / "app/src/main/java/com/pixijoy/ai"),
    (ROOT / "app/src/androidTest/java/com/pixijoy/ai", ROOT / "app/src/androidTest/java/com/pixijoy/ai"),
]


def should_process(path: Path) -> bool:
    if any(part in SKIP_DIRS for part in path.parts):
        return False
    return path.suffix.lower() in TEXT_EXTENSIONS


def replace_in_file(path: Path) -> bool:
    try:
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return False
    original = text
    for old, new in REPLACEMENTS:
        text = text.replace(old, new)
    if text != original:
        path.write_text(text, encoding="utf-8", newline="\n")
        return True
    return False


def move_package_dirs():
    for src, dst in DIR_MOVES:
        if not src.exists():
            print(f"SKIP move (missing): {src}")
            continue
        dst.parent.mkdir(parents=True, exist_ok=True)
        if dst.exists():
            shutil.rmtree(dst)
        shutil.move(str(src), str(dst))
        print(f"MOVED: {src} -> {dst}")
        # Clean up empty pixijoy parent
        parent = src.parent
        if parent.exists() and not any(parent.iterdir()):
            parent.rmdir()
            grand = parent.parent
            if grand.exists() and not any(grand.iterdir()):
                grand.rmdir()


def rename_files():
    for path in list(ROOT.rglob("*")):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        new_name = FILE_RENAMES.get(path.name)
        if new_name:
            new_path = path.with_name(new_name)
            if new_path.exists():
                new_path.unlink()
            path.rename(new_path)
            print(f"RENAMED: {path.name} -> {new_name}")


def replace_all_text():
    changed = 0
    for path in ROOT.rglob("*"):
        if path.is_file() and should_process(path):
            if replace_in_file(path):
                changed += 1
                print(f"UPDATED: {path.relative_to(ROOT)}")
    print(f"Total files updated: {changed}")


def main():
    print("=== Step 1: Move package directories ===")
    move_package_dirs()
    print("\n=== Step 2: Replace text in source files ===")
    replace_all_text()
    print("\n=== Step 3: Rename PixiJoy* files ===")
    rename_files()
    print("\n=== Done ===")


if __name__ == "__main__":
    main()
