#!/usr/bin/env python3
"""Merge TSV + optional JSON overrides; fill gaps via Argos EN->target."""
from __future__ import annotations

import csv
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
OUT = ROOT / "tools/i18n_cache"
LOCALES = [
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
ARGOS = {
    "zh-rCN": ("en", "zh"),
    "zh-rTW": ("en", "zh"),  # same model; post-process TW below
    "th": ("en", "th"),
    "vi": ("en", "vi"),
    "tr": ("en", "tr"),
    "ru": ("en", "ru"),
    "pl": ("en", "pl"),
    "fil": ("en", "tl"),
    "ms": ("en", "ms"),
    "uk": ("en", "uk"),
}

KEEP = re.compile(
    r"(PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR|"
    r"Neural frames|Neural|PiP|SDR|WYSIWYG|Ken Burns GIF|Pictures/PixiJoy|Movies/PixiJoy|"
    r"2×|4×|½×|¼×|⅛×|2-up|4-up|Ready|Apply|VO|SFX|EQ|RAW|UNet|\.tflite)"
)


def protect(text: str) -> tuple[str, list[str]]:
    held: list[str] = []

    def repl(m: re.Match[str]) -> str:
        held.append(m.group(0))
        return f"⟦{len(held)-1}⟧"

    out = text
    for pat in [
        r"%(\d+\$)?(\.\d+)?[sdif]",
        r"%%",
        r"PixiJoy|WhatsApp|LUT|HDR|GIF|MP4|EXIF|GPS|sRGB|Display P3|LiteRT|Media3|Ultra HDR",
        r"Neural frames|Neural|PiP|SDR|WYSIWYG|Ken Burns GIF",
        r"Pictures/PixiJoy|Movies/PixiJoy",
        r"2×|4×|½×|¼×|⅛×|2-up|4-up",
    ]:
        out = re.sub(pat, repl, out)
    return out, held


def restore(text: str, held: list[str]) -> str:
    def repl(m: re.Match[str]) -> str:
        i = int(m.group(1))
        return held[i] if 0 <= i < len(held) else m.group(0)

    return re.sub(r"⟦(\d+)⟧", repl, text)


def load_tsv() -> dict[str, dict[str, str]]:
    by_key: dict[str, dict[str, str]] = {}
    for tsv in sorted((ROOT / "tools").glob("i18n_master*.tsv")):
        with tsv.open(encoding="utf-8", newline="") as f:
            for row in csv.DictReader(f, delimiter="\t"):
                by_key[row["key"]] = row
    rem = ROOT / "tools/i18n_remaining.json"
    if rem.exists():
        extra = json.loads(rem.read_text(encoding="utf-8"))
        for key, locs in extra.items():
            row = by_key.setdefault(key, {"key": key})
            for loc, val in locs.items():
                row[loc] = val
    return by_key


def twify(s: str) -> str:
    """Minimal zh-CN -> zh-TW character tweaks (fallback when Argos shares zh)."""
    pairs = {
        "软件": "軟體",
        "网络": "網路",
        "视频": "影片",
        "导出": "匯出",
        "导入": "匯入",
        "保存": "儲存",
        "设置": "設定",
        "屏幕": "螢幕",
        "鼠标": "滑鼠",
        "程序": "程式",
        "文件": "檔案",
        "信息": "資訊",
        "内存": "記憶體",
        "默认": "預設",
        "删除": "刪除",
        "创建": "建立",
        "应用": "套用",
        "库": "資料庫",
        "相册": "相簿",
        "允许": "允許",
        "相机": "相機",
        "加载": "載入",
        "打开": "開啟",
        "关闭": "關閉",
        "无": "無",
        "后": "後",
        "时": "時",
        "发": "發",
        "线": "線",
        "质": "質",
        "风": "風",
        "画": "畫",
        "动": "動",
        "复": "復",
        "制": "製",
        "台": "臺",
        "机": "機",
        "这": "這",
        "还": "還",
        "为": "為",
        "从": "從",
        "与": "與",
        "个": "個",
        "来": "來",
        "对": "對",
        "说": "說",
        "请": "請",
        "选": "選",
        "显": "顯",
        "预": "預",
        "览": "覽",
        "编": "編",
        "辑": "輯",
        "质": "質",
        "标": "標",
        "题": "題",
        "签": "簽",
        "录": "錄",
        "声": "聲",
        "调": "調",
        "节": "節",
        "点": "點",
        "击": "點",
        "击": "擊",
        "击": "點",
    }
    for a, b in pairs.items():
        s = s.replace(a, b)
    return s


def get_argos_translators():
    import argostranslate.package
    import argostranslate.translate

    argostranslate.package.update_package_index()
    installed = argostranslate.package.get_installed_packages()
    cache: dict[tuple[str, str], object] = {}

    def get(from_code: str, to_code: str):
        key = (from_code, to_code)
        if key in cache:
            return cache[key]
        for p in installed:
            if p.from_code == from_code and p.to_code == to_code:
                cache[key] = p
                return p
        available = argostranslate.package.get_available_packages()
        match = next(
            (p for p in available if p.from_code == from_code and p.to_code == to_code),
            None,
        )
        if not match:
            raise RuntimeError(f"No Argos package {from_code}->{to_code}")
        path = match.download()
        pkg = argostranslate.package.install_from_path(path)
        cache[key] = pkg
        return pkg

    def translate(text: str, from_code: str, to_code: str) -> str:
        if from_code == to_code or not text.strip():
            return text
        if text.strip() == "PixiJoy":
            return text
        p, held = protect(text)
        pkg = get(from_code, to_code)
        out = argostranslate.translate.translate(p, from_code, to_code)
        return restore(out, held)

    return translate


def main() -> None:
    use_argos = "--no-argos" not in sys.argv
    translate_fn = None
    if use_argos:
        try:
            translate_fn = get_argos_translators()
        except Exception as e:
            print(f"Argos unavailable ({e}); need manual TSV/json fill", file=sys.stderr)
            sys.exit(1)

    by_key = load_tsv()
    OUT.mkdir(parents=True, exist_ok=True)
    names = list(EN.keys())

    for loc in LOCALES:
        out: dict[str, str] = {}
        fr, to = ARGOS[loc]
        for name in names:
            if name == "app_name":
                out[name] = "PixiJoy"
                continue
            en = EN[name]
            row = by_key.get(name) or {}
            val = (row.get(loc) or "").strip()
            if val and val != en:
                out[name] = val
                continue
            if translate_fn:
                val = translate_fn(en, fr, to)
                if loc == "zh-rTW" and to == "zh":
                    val = twify(val)
                out[name] = val
            else:
                out[name] = en
        out["app_name"] = "PixiJoy"
        ordered = {k: out[k] for k in names}
        path = OUT / f"{loc}.json"
        path.write_text(
            json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8"
        )
        missing = [k for k in names if not ordered.get(k)]
        print(f"{loc}: keys={len(ordered)} missing={len(missing)}")


if __name__ == "__main__":
    main()
