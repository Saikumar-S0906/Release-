#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate zh-rTW from zh-rCN via OpenCC s2twp + TW UX overrides."""
from __future__ import annotations

import json
import pathlib

from opencc import OpenCC

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN = json.loads((ROOT / "tools/strings_en.json").read_text(encoding="utf-8"))
zh = json.loads((ROOT / "tools/i18n_cache/zh-rCN.json").read_text(encoding="utf-8"))
cc = OpenCC("s2twp")

tw = {k: ("PixiJoy" if k == "app_name" else cc.convert(v)) for k, v in zh.items()}

OV = {
    "studio_apply": "套用",
    "studio_export": "匯出",
    "studio_exporting": "正在匯出…",
    "studio_export_sheet_title": "匯出",
    "studio_export_mp4": "匯出 MP4",
    "studio_export_gif": "匯出 GIF",
    "studio_export_pct": "正在匯出 %1$d%%",
    "nav_library": "資料庫",
    "tool_cutout": "基本去背",
    "tool_cutout_sub": "適合簡單背景的基本去背",
    "tool_best_for_cutout": "適合簡單背景的基本去背",
    "tool_stickers": "貼圖",
    "tool_stickers_sub": "在本機基本去背",
    "studio_stickers": "貼圖",
    "studio_cutout_now": "套用基本去背",
    "studio_mask": "遮罩",
    "studio_mask_clear": "清除遮罩",
    "studio_mask_restore": "還原上次遮罩",
    "studio_mask_hint": "主體填滿遮罩。塗抹以細修。",
    "studio_cutout_subject_available": "此裝置可用主體遮罩。",
    "studio_cutout_corner_fallback": "主體遮罩無法使用。基本去背會移除與照片邊角相符的顏色。",
    "studio_mask_subject_unavailable": "主體填滿需要裝置端遮罩。請改用塗抹細修。",
    "studio_brush": "筆刷",
    "studio_undo": "復原",
    "export_view": "檢視",
    "studio_open": "開啟工作室",
    "home_create": "建立",
    "create_source_title": "建立",
    "studio_library_create_cta": "建立",
    "studio_library_rename": "重新命名",
    "session_rename_project": "重新命名專案",
    "studio_crop": "裁切",
    "studio_group_crop": "裁切",
    "a11y_crop_edge_left": "裁切邊緣，左",
    "a11y_crop_edge_right": "裁切邊緣，右",
    "a11y_crop_edge_top": "裁切邊緣，上",
    "a11y_crop_edge_bottom": "裁切邊緣，下",
    "a11y_trim_in": "裁切起點",
    "a11y_trim_out": "裁切終點",
    "studio_lut_import": "匯入 LUT",
    "studio_lut_imported": "已匯入 %1$s",
    "studio_open_file": "開啟檔案",
    "create_source_files": "照片或 RAW 檔案",
    "studio_export_quality_label": "品質",
    "studio_export_quality": "品質 %1$d",
    "studio_export_resolution": "解析度",
    "studio_super_res": "超解析度",
    "studio_pack_storage": "儲存空間：%1$s",
    "studio_fx_reset": "重設特效",
    "a11y_reset_value": "重設",
    "studio_master_eq": "主等化器",
    "studio_hsl_magentas": "洋紅",
    "studio_look_film": "底片",
    "studio_gallery": "圖庫",
    "tool_restore": "一鍵強化",
    "studio_restore_now": "套用一鍵強化",
    "studio_fill": "填滿",
    "studio_fill_apply_nearby": "套用鄰近像素填滿",
    "studio_fill_apply_neural": "套用神經填滿",
    "studio_export_ok": "已儲存到 Pictures/PixiJoy。無浮水印。",
    "studio_export_movie_ok": "已儲存到 Movies/PixiJoy。無浮水印。",
    "studio_export_gif_ok": "GIF 已儲存到 Pictures/PixiJoy。無浮水印。",
    "studio_export_no_watermark": "無浮水印。不會上傳。",
    "studio_promise": "不需上傳。不需帳戶。無浮水印。無月費。",
    "studio_export_4k_cost": "編碼較重 · 手機上較慢",
    "session_compare_before": "之前 · 放開查看之後",
    "studio_import_failed": "無法匯入媒體：%1$s",
    "studio_export_background": "正在背景匯出。你可以離開此畫面。",
    "studio_library_empty": "尚無專案",
    "studio_packs_empty": "下載以解鎖",
    "studio_pack_download": "下載",
    "studio_pack_from_file": "從檔案安裝",
    "studio_preset_save": "儲存預設",
    "studio_presets": "預設",
    "studio_library_edited_just_now": "剛剛儲存",
    "studio_library_edited_minutes": "%1$d 分鐘前儲存",
    "studio_library_edited_hours": "%1$d 小時前儲存",
    "studio_library_edited_days": "%1$d 天前儲存",
    "session_save_status_saved": "已儲存",
    "session_save_status_saving": "正在儲存…",
    "session_save_status_unsaved": "未儲存",
    "studio_export_destination_pictures": "儲存至 Pictures/PixiJoy",
    "studio_export_destination_movies": "儲存至 Movies/PixiJoy",
}
tw.update(OV)
ordered = {k: tw[k] for k in EN}
assert set(ordered) == set(EN)
path = ROOT / "tools/i18n_cache/zh-rTW.json"
path.write_text(json.dumps(ordered, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
print(f"wrote {path} ({len(ordered)})")
