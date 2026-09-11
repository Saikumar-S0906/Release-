"""Generate PixiJoy 24dp tintable vector icons and rewrite Material Icons call sites."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DRAW = ROOT / "app" / "src" / "main" / "res" / "drawable"
JAVA = ROOT / "app" / "src" / "main" / "java"

# Unique filled 24x24 paths — white, tinted at runtime. Match existing pj_ic_* language.
ICONS: dict[str, str] = {
    "pj_ic_chevron_right": "M9,5 L17,12 L9,19 L7.6,17.6 L14.2,12 L7.6,6.4 Z",
    "pj_ic_tune": "M4,7 L10,7 L10,5 L14,5 L14,7 L20,7 L20,9 L14,9 L14,11 L10,11 L10,9 L4,9 Z M4,15 L14,15 L14,13 L18,13 L18,15 L20,15 L20,17 L18,17 L18,19 L14,19 L14,17 L4,17 Z",
    "pj_ic_crop": "M6,2 L8,2 L8,6 L18,6 L18,16 L22,16 L22,18 L18,18 L18,22 L16,22 L16,18 L6,18 L6,8 L2,8 L2,6 L6,6 Z M8,8 L8,16 L16,16 L16,8 Z",
    "pj_ic_notifications": "M12,3 C8.7,3 7,5.4 7,8.5 L7,12 L5,15 L5,17 L19,17 L19,15 L17,12 L17,8.5 C17,5.4 15.3,3 12,3 Z M10,18 A2,2 0,0 0,14,18 Z",
    "pj_ic_settings": "M12,8 A4,4 0,1 0,12,16 A4,4 0,1 0,12,8 Z M11,2 L13,2 L13,5 L11,5 Z M11,19 L13,19 L13,22 L11,22 Z M2,11 L5,11 L5,13 L2,13 Z M19,11 L22,11 L22,13 L19,13 Z M4.1,4.1 L5.5,5.5 L3.4,7.6 L2,6.2 Z M18.5,18.5 L19.9,19.9 L17.8,22 L16.4,20.6 Z M18.5,5.5 L19.9,4.1 L22,6.2 L20.6,7.6 Z M4.1,19.9 L5.5,18.5 L7.6,20.6 L6.2,22 Z",
    "pj_ic_filter": "M3,5 L21,5 L14,13 L14,19 L10,21 L10,13 Z",
    "pj_ic_premium": "M5,18 L19,18 L19,20 L5,20 Z M6,8 L9,14 L12,6 L15,14 L18,8 L19,16 L5,16 Z",
    "pj_ic_movie": "M3,5 L7,8 L7,16 L3,19 Z M8,5 L21,5 L21,19 L8,19 Z M11,9 L17,12 L11,15 Z",
    "pj_ic_palette": "M12,3 A9,9 0,1 0,12,21 C10,21 10,19.5 11,18.5 C12,17.5 10.5,16 9.5,16 L8,16 A5,5 0,0 1,12,3 Z M7.5,8.5 A1.3,1.3 0,1 0,7.5,11.1 A1.3,1.3 0,1 0,7.5,8.5 Z M11,6.2 A1.3,1.3 0,1 0,11,8.8 A1.3,1.3 0,1 0,11,6.2 Z M15.2,7.2 A1.3,1.3 0,1 0,15.2,9.8 A1.3,1.3 0,1 0,15.2,7.2 Z M16.8,11.2 A1.3,1.3 0,1 0,16.8,13.8 A1.3,1.3 0,1 0,16.8,11.2 Z",
    "pj_ic_block": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M7.2,8.6 L15.4,16.8 L16.8,15.4 L8.6,7.2 Z",
    "pj_ic_emoji": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M8.5,9.5 A1.3,1.3 0,1 0,8.5,12.1 A1.3,1.3 0,1 0,8.5,9.5 Z M15.5,9.5 A1.3,1.3 0,1 0,15.5,12.1 A1.3,1.3 0,1 0,15.5,9.5 Z M7.8,14 C9.2,16.4 14.8,16.4 16.2,14 L14.6,13.1 C13.6,14.6 10.4,14.6 9.4,13.1 Z",
    "pj_ic_mic": "M12,3 C10.1,3 9,4.3 9,6.2 L9,11.2 C9,13.1 10.1,14.4 12,14.4 C13.9,14.4 15,13.1 15,11.2 L15,6.2 C15,4.3 13.9,3 12,3 Z M7,11 L7,11.5 C7,14.4 9.2,16.6 12,16.6 C14.8,16.6 17,14.4 17,11.5 L17,11 L19,11 L19,11.5 C19,15.1 16.3,17.8 13,18.3 L13,21 L11,21 L11,18.3 C7.7,17.8 5,15.1 5,11.5 L5,11 Z",
    "pj_ic_cut": "M7,4 A3,3 0,1 0,7,10 A3,3 0,1 0,7,4 Z M17,14 A3,3 0,1 0,17,20 A3,3 0,1 0,17,14 Z M9,9 L19,19 L17.6,20.4 L7.6,10.4 Z M9,15 L19,5 L20.4,6.4 L10.4,16.4 Z",
    "pj_ic_grid": "M4,4 L10,4 L10,10 L4,10 Z M14,4 L20,4 L20,10 L14,10 Z M4,14 L10,14 L10,20 L4,20 Z M14,14 L20,14 L20,20 L14,20 Z",
    "pj_ic_payments": "M3,6 L21,6 L21,10 L3,10 Z M3,11 L21,11 L21,18 L3,18 Z M5,14 L11,14 L11,16 L5,16 Z",
    "pj_ic_help": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M11,16 L13,16 L13,18 L11,18 Z M9.2,8.6 C9.2,7 10.5,6 12.1,6 C13.8,6 15,7.1 15,8.7 C15,10 14.2,10.7 13.1,11.4 C12.3,11.9 12,12.3 12,13.2 L12,14 L10.2,14 L10.2,13 C10.2,11.7 10.9,11.1 12,10.4 C12.8,9.9 13.2,9.6 13.2,8.8 C13.2,8.1 12.7,7.6 12.1,7.6 C11.4,7.6 11,8 11,8.7 Z",
    "pj_ic_info": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M11,10 L13,10 L13,17 L11,17 Z M11,7 L13,7 L13,9 L11,9 Z",
    "pj_ic_brightness": "M12,7 A5,5 0,1 0,12,17 A5,5 0,1 0,12,7 Z M11,1 L13,1 L13,4 L11,4 Z M11,20 L13,20 L13,23 L11,23 Z M1,11 L4,11 L4,13 L1,13 Z M20,11 L23,11 L23,13 L20,13 Z M3.8,3.8 L5.9,5.9 L4.5,7.3 L2.4,5.2 Z M18.1,18.1 L20.2,20.2 L18.8,21.6 L16.7,19.5 Z M18.1,5.9 L20.2,3.8 L21.6,5.2 L19.5,7.3 Z M3.8,20.2 L5.9,18.1 L7.3,19.5 L5.2,21.6 Z",
    "pj_ic_sticker": "M5,4 L14,4 L19,9 L19,20 L5,20 Z M14,4 L14,9 L19,9",
    "pj_ic_refresh": "M17.7,6.3 A8,8 0,1 0,20,12 L18,12 A6,6 0,1 1,16.2,7.8 L13,11 L13,4 L20,4 Z",
    "pj_ic_star": "M12,3 L14.5,9.2 L21,9.6 L16,13.8 L17.6,20.2 L12,16.8 L6.4,20.2 L8,13.8 L3,9.6 L9.5,9.2 Z",
    "pj_ic_history": "M12,4 A8,8 0,1 0,12,20 A8,8 0,1 0,12,4 Z M12,7 L12,12.5 L16,14.5 L15.2,16 L10,13.2 L10,7 Z",
    "pj_ic_lock": "M8,10 L8,8 A4,4 0,0 1,16,8 L16,10 L18,10 L18,20 L6,20 L6,10 Z M10,10 L14,10 L14,8 A2,2 0,0 0,10,8 Z",
    "pj_ic_cloud_off": "M4,16 C2,16 2,13 4.5,12.5 C4.2,8 8,7 10,9 C11,6 16,6 17,10 C20,10 21,13 19.5,15 Z M3,4 L21,22 L19.6,23.4 L1.6,5.4 Z",
    "pj_ic_person": "M12,4 A4,4 0,1 0,12,12 A4,4 0,1 0,12,4 Z M6,20 C6,16.2 8.7,14 12,14 C15.3,14 18,16.2 18,20 Z",
    "pj_ic_person_add": "M10,4 A4,4 0,1 0,10,12 A4,4 0,1 0,10,4 Z M4,20 C4,16.2 6.7,14 10,14 C13.3,14 16,16.2 16,20 Z M18,8 L20,8 L20,11 L23,11 L23,13 L20,13 L20,16 L18,16 L18,13 L15,13 L15,11 L18,11 Z",
    "pj_ic_person_remove": "M10,4 A4,4 0,1 0,10,12 A4,4 0,1 0,10,4 Z M4,20 C4,16.2 6.7,14 10,14 C13.3,14 16,16.2 16,20 Z M15,11 L23,11 L23,13 L15,13 Z",
    "pj_ic_speed": "M12,4 A8,8 0,1 0,12,20 A8,8 0,0 0,12,4 Z M12,6 A6,6 0,0 1,18,12 L16,12 A4,4 0,1 0,12,8 Z M13,12 L17,7 L18.4,8.2 L13.8,13.2 Z",
    "pj_ic_more": "M12,5 A2,2 0,1 0,12,9 A2,2 0,1 0,12,5 Z M12,10 A2,2 0,1 0,12,14 A2,2 0,1 0,12,10 Z M12,15 A2,2 0,1 0,12,19 A2,2 0,1 0,12,15 Z",
    "pj_ic_more_horiz": "M5,10 A2,2 0,1 0,5,14 A2,2 0,1 0,5,10 Z M10,10 A2,2 0,1 0,10,14 A2,2 0,1 0,10,10 Z M15,10 A2,2 0,1 0,15,14 A2,2 0,1 0,15,10 Z",
    "pj_ic_shield": "M12,3 L20,6 L20,12 C20,16.5 16.7,19.8 12,21 C7.3,19.8 4,16.5 4,12 L4,6 Z",
    "pj_ic_send": "M3,11 L21,3 L14,21 L11,14 Z",
    "pj_ic_visibility": "M12,6 C7,6 3.2,9.2 2,12 C3.2,14.8 7,18 12,18 C17,18 20.8,14.8 22,12 C20.8,9.2 17,6 12,6 Z M12,9 A3,3 0,1 1,12,15 A3,3 0,1 1,12,9 Z",
    "pj_ic_copy": "M8,6 L18,6 L18,18 L8,18 Z M6,8 L6,20 L16,20 L16,22 L4,22 L4,8 Z",
    "pj_ic_time": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M11,7 L13,7 L13,12.2 L17,14.4 L16.1,16 L11,13.2 Z",
    "pj_ic_chat": "M4,5 L20,5 L20,16 L8,16 L4,20 Z",
    "pj_ic_language": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M4.6,11 L19.4,11 L19.4,13 L4.6,13 Z M12,4.2 C14.2,6.6 15.4,9.2 15.6,12 C15.4,14.8 14.2,17.4 12,19.8 C9.8,17.4 8.6,14.8 8.4,12 C8.6,9.2 9.8,6.6 12,4.2 Z",
    "pj_ic_wallet": "M4,7 L20,7 L20,19 L4,19 Z M16,12 A1.6,1.6 0,1 0,16,15.2 A1.6,1.6 0,1 0,16,12 Z M6,5 L18,5 L18,7 L6,7 Z",
    "pj_ic_bag": "M7,8 L5,20 L19,20 L17,8 Z M9,8 L9,6 A3,3 0,0 1,15,6 L15,8 L13,8 L13,6 A1,1 0,0 0,11,6 L11,8 Z",
    "pj_ic_save": "M5,4 L16,4 L19,7 L19,20 L5,20 Z M8,4 L8,10 L15,10 L15,4 Z M8,13 L16,13 L16,18 L8,18 Z",
    "pj_ic_blur": "M7,7 A2,2 0,1 0,7,11 A2,2 0,1 0,7,7 Z M12,5 A2.4,2.4 0,1 0,12,9.8 A2.4,2.4 0,1 0,12,5 Z M17,7 A2,2 0,1 0,17,11 A2,2 0,1 0,17,7 Z M8,13 A2.2,2.2 0,1 0,8,17.4 A2.2,2.2 0,1 0,8,13 Z M16,13 A2.2,2.2 0,1 0,16,17.4 A2.2,2.2 0,1 0,16,13 Z M12,12 A2.6,2.6 0,1 0,12,17.2 A2.6,2.6 0,1 0,12,12 Z",
    "pj_ic_face": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M9,9 A1.2,1.2 0,1 0,9,11.4 A1.2,1.2 0,1 0,9,9 Z M15,9 A1.2,1.2 0,1 0,15,11.4 A1.2,1.2 0,1 0,15,9 Z M8,13.5 C9.5,16 14.5,16 16,13.5 L14.5,12.8 C13.5,14.2 10.5,14.2 9.5,12.8 Z",
    "pj_ic_brush": "M7,16 L11,20 L5,22 Z M13,3 L21,11 L12,20 L4,12 Z",
    "pj_ic_bulb": "M9,18 L15,18 L15,20 L9,20 Z M8,21 L16,21 L16,22.5 L8,22.5 Z M8,10 A4,4 0,1 1,16,10 C16,13 14,14.5 13,16 L11,16 C10,14.5 8,13 8,10 Z",
    "pj_ic_focus": "M4,4 L9,4 L9,6 L6,6 L6,9 L4,9 Z M15,4 L20,4 L20,9 L18,9 L18,6 L15,6 Z M4,15 L6,15 L6,18 L9,18 L9,20 L4,20 Z M18,15 L20,15 L20,20 L15,20 L15,18 L18,18 Z M12,8 A4,4 0,1 0,12,16 A4,4 0,1 0,12,8 Z",
    "pj_ic_waves": "M3,12 C5,8 7,8 9,12 C11,16 13,16 15,12 C17,8 19,8 21,12 L21,15 C19,11 17,11 15,15 C13,19 11,19 9,15 C7,11 5,11 3,15 Z",
    "pj_ic_music": "M10,6 L19,4 L19,16 A3,3 0,1 1,17,13.1 L17,8 L12,9.1 L12,17 A3,3 0,1 1,10,14.1 Z",
    "pj_ic_undo": "M7,8 L3,12 L7,16 L7,13.5 C12,13.5 15,15 17.5,19 C16,13 12.5,10.5 7,10.5 Z",
    "pj_ic_redo": "M17,8 L21,12 L17,16 L17,13.5 C12,13.5 9,15 6.5,19 C8,13 11.5,10.5 17,10.5 Z",
    "pj_ic_storage": "M3,5 L21,5 L21,9 L3,9 Z M3,10 L21,10 L21,14 L3,14 Z M3,15 L21,15 L21,19 L3,19 Z M6,6.6 A0.9,0.9 0,1 0,6,8.4 A0.9,0.9 0,1 0,6,6.6 Z M6,11.6 A0.9,0.9 0,1 0,6,13.4 A0.9,0.9 0,1 0,6,11.6 Z M6,16.6 A0.9,0.9 0,1 0,6,18.4 A0.9,0.9 0,1 0,6,16.6 Z",
    "pj_ic_bug": "M9,8 A3,3 0,0 1,15,8 L15,10 L9,10 Z M8,11 L16,11 L16,18 C16,19.5 8,19.5 8,18 Z M4,12 L8,12 L8,14 L4,14 Z M16,12 L20,12 L20,14 L16,14 Z M5,8 L8,10 L7,11.2 L4,9.2 Z M19,8 L16,10 L17,11.2 L20,9.2 Z",
    "pj_ic_megaphone": "M4,10 L10,10 L18,6 L18,18 L10,14 L4,14 Z M4,14 L4,18 L8,18 L8,14 Z",
    "pj_ic_public": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M5,11 L19,11 L19,13 L5,13 Z M12,4.5 C14,7 15,9.5 15.2,12 C15,14.5 14,17 12,19.5 C10,17 9,14.5 8.8,12 C9,9.5 10,7 12,4.5 Z",
    "pj_ic_gift": "M4,11 L20,11 L20,20 L4,20 Z M3,7 L21,7 L21,11 L3,11 Z M11,7 L11,20 L13,20 L13,7 Z M8,3 L12,7 L6,7 C6,5 6.8,3 8,3 Z M16,3 C17.2,3 18,5 18,7 L12,7 Z",
    "pj_ic_people": "M8,5 A3,3 0,1 0,8,11 A3,3 0,1 0,8,5 Z M16,6 A2.5,2.5 0,1 0,16,11 A2.5,2.5 0,1 0,16,6 Z M3,19 C3,15.8 5.2,14 8,14 C10.8,14 13,15.8 13,19 Z M13,19 C13.4,16.2 15.2,14.8 17.4,14.8 C19.6,14.8 21.5,16.1 21.8,19 Z",
    "pj_ic_rotate": "M12,6 A6,6 0,1 1,6,12 L4,12 A8,8 0,1 0,12,4 L12,1 L16,5 L12,9 Z",
    "pj_ic_flip": "M11,3 L13,3 L13,21 L11,21 Z M8,6 L3,12 L8,18 L8,14 L10,14 L10,10 L8,10 Z M16,6 L16,10 L14,10 L14,14 L16,14 L16,18 L21,12 Z",
    "pj_ic_warning": "M12,3 L22,20 L2,20 Z M11,9 L13,9 L13,14 L11,14 Z M11,16 L13,16 L13,18 L11,18 Z",
    "pj_ic_document": "M6,3 L14,3 L18,7 L18,21 L6,21 Z M14,3 L14,7 L18,7",
    "pj_ic_quote": "M6,8 L11,8 L11,13 C11,16 8,17 6,18 L6,16 C7.5,15.5 8.5,14.7 8.5,13 L6,13 Z M13,8 L18,8 L18,13 C18,16 15,17 13,18 L13,16 C14.5,15.5 15.5,14.7 15.5,13 L13,13 Z",
    "pj_ic_bolt": "M13,2 L6,13 L11,13 L10,22 L18,10 L13,10 Z",
    "pj_ic_gif": "M4,7 L20,7 L20,17 L4,17 Z M7,10 L7,14 L10,14 L10,13 L8,13 L8,10 Z M11,10 L12.2,10 L12.2,14 L11,14 Z M13.2,10 L16.5,10 L16.5,11.1 L14.4,11.1 L14.4,11.7 L16.2,11.7 L16.2,12.8 L14.4,12.8 L14.4,14 L13.2,14 Z",
    "pj_ic_flash": "M12,2 L8,12 L12,12 L10,22 L18,10 L13,10 Z",
    "pj_ic_badge": "M12,2 L14.2,8.4 L21,8.8 L16,13.2 L17.6,20 L12,16.6 L6.4,20 L8,13.2 L3,8.8 L9.8,8.4 Z",
    "pj_ic_heal": "M13,3 L16,8 L21,9 L17,13 L18,19 L13,16 L8,19 L9,13 L5,9 L10,8 Z M11,10 L13,10 L13,12 L15,12 L15,14 L13,14 L13,16 L11,16 L11,14 L9,14 L9,12 L11,12 Z",
    "pj_ic_timer": "M9,2 L15,2 L15,4 L9,4 Z M12,6 A8,8 0,1 0,12,22 A8,8 0,1 0,12,6 Z M11,10 L13,10 L13,14 L16,15.5 L15.2,17 L11,15 Z",
    "pj_ic_error": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M11,7 L13,7 L13,13 L11,13 Z M11,15 L13,15 L13,17 L11,17 Z",
    "pj_ic_font": "M5,18 L8,8 L11,18 L9.8,18 L9.3,16 L6.7,16 L6.2,18 Z M7.1,14.5 L8.9,14.5 L8,11.2 Z M13,10 L21,10 L21,12 L18,12 L18,18 L16,18 L16,12 L13,12 Z",
    "pj_ic_keyboard": "M3,7 L21,7 L21,17 L3,17 Z M5,9 L7,9 L7,11 L5,11 Z M8,9 L10,9 L10,11 L8,11 Z M11,9 L13,9 L13,11 L11,11 Z M14,9 L16,9 L16,11 L14,11 Z M17,9 L19,9 L19,11 L17,11 Z M6,13 L18,13 L18,15 L6,15 Z",
    "pj_ic_sun": "M12,7 A5,5 0,1 0,12,17 A5,5 0,1 0,12,7 Z M11,1.5 L13,1.5 L13,4 L11,4 Z M11,20 L13,20 L13,22.5 L11,22.5 Z M1.5,11 L4,11 L4,13 L1.5,13 Z M20,11 L22.5,11 L22.5,13 L20,13 Z",
    "pj_ic_expand": "M4,4 L10,4 L10,6 L6,6 L6,10 L4,10 Z M14,4 L20,4 L20,10 L18,10 L18,6 L14,6 Z M4,14 L6,14 L6,18 L10,18 L10,20 L4,20 Z M18,14 L20,14 L20,20 L14,20 L14,18 L18,18 Z",
    "pj_ic_cake": "M8,3 L10,3 L10,6 L8,6 Z M11,3 L13,3 L13,6 L11,6 Z M14,3 L16,3 L16,6 L14,6 Z M5,8 L19,8 L19,12 L5,12 Z M4,12 L20,12 L20,20 L4,20 Z",
    "pj_ic_celebrate": "M4,18 L8,8 L12,18 Z M13,7 L20,7 L16.5,14 Z M18,3 L20,5 L18,7 L16,5 Z M3,4 L5,6 L3,8 L1,6 Z",
    "pj_ic_phone": "M8,2 L16,2 L16,22 L8,22 Z M10,19 L14,19 L14,20.5 L10,20.5 Z",
    "pj_ic_moon": "M13,3 C8,4 5,8.5 5,13 C5,18 9,21.5 14,21.5 C16.5,21.5 18.7,20.5 20,19 C15,19.2 11,15.5 11,11 C11,8 12,5.5 13,3 Z",
    "pj_ic_chart": "M4,20 L20,20 L20,18 L4,18 Z M6,10 L9,10 L9,17 L6,17 Z M11,6 L14,6 L14,17 L11,17 Z M16,12 L19,12 L19,17 L16,17 Z",
    "pj_ic_gavel": "M3,19 L13,19 L13,21 L3,21 Z M13,4 L20,11 L18.6,12.4 L11.6,5.4 Z M8,10 L14,16 L12.6,17.4 L6.6,11.4 Z",
    "pj_ic_cloud": "M7,18 C3.8,18 3,14.5 6,13.5 C6,9.5 10,8 12,10.5 C13.5,8 18,8.5 19,12 C21.5,12.2 22,15.5 20,18 Z",
    "pj_ic_device": "M7,4 L17,4 L17,20 L7,20 Z M9,17 L15,17 L15,18.5 L9,18.5 Z",
    "pj_ic_video": "M3,7 L15,7 L15,17 L3,17 Z M16,9 L21,6 L21,18 L16,15 Z",
    "pj_ic_email": "M3,6 L21,6 L21,18 L3,18 Z M3,6 L12,13 L21,6",
    "pj_ic_calendar": "M6,4 L8,4 L8,6 L16,6 L16,4 L18,4 L18,6 L20,6 L20,20 L4,20 L4,6 L6,6 Z M6,10 L18,10 L18,12 L6,12 Z",
    "pj_ic_swap": "M7,7 L3,11 L7,15 L7,12.5 L11,12.5 L11,9.5 L7,9.5 Z M17,9 L21,13 L17,17 L17,14.5 L13,14.5 L13,11.5 L17,11.5 Z",
    "pj_ic_file": "M7,3 L14,3 L18,7 L18,21 L7,21 Z M14,3 L14,7 L18,7",
    "pj_ic_bookmark": "M7,3 L17,3 L17,21 L12,17 L7,21 Z",
    "pj_ic_wifi": "M12,18 A1.6,1.6 0,1 0,12,21.2 A1.6,1.6 0,1 0,12,18 Z M7.2,14.2 C8.6,12.8 10.2,12.1 12,12.1 C13.8,12.1 15.4,12.8 16.8,14.2 L15.3,15.6 C14.4,14.7 13.3,14.2 12,14.2 C10.7,14.2 9.6,14.7 8.7,15.6 Z M4.2,11.2 C6.4,9 9.1,7.8 12,7.8 C14.9,7.8 17.6,9 19.8,11.2 L18.3,12.6 C16.6,10.9 14.4,10 12,10 C9.6,10 7.4,10.9 5.7,12.6 Z",
    "pj_ic_wifi_off": "M12,18 A1.6,1.6 0,1 0,12,21.2 A1.6,1.6 0,1 0,12,18 Z M4,4 L20,20 L18.6,21.4 L2.6,5.4 Z M7.2,14.2 L8.7,15.6 C9.6,14.7 10.7,14.2 12,14.2 L12,12.1 C10.2,12.1 8.6,12.8 7.2,14.2 Z",
    "pj_ic_upload": "M11,14 L13,14 L13,21 L11,21 Z M6,10 L12,4 L18,10 L14,10 L14,14 L10,14 L10,10 Z",
    "pj_ic_a11y": "M12,2 A2,2 0,1 0,12,6 A2,2 0,1 0,12,2 Z M8,8 L16,8 L16,10 L13,10 L13,22 L11,22 L11,15 L9,22 L7,22 L10,14 L10,10 L8,10 Z",
    "pj_ic_sync": "M7,7 A7,7 0,0 1,18,10 L16,10 L16,6 L21,6 L21,11 L19,11 A9,9 0,1 0,19,16 L21,16 A11,11 0,1 1,6,6 Z",
    "pj_ic_compare": "M4,5 L11,5 L11,19 L4,19 Z M13,5 L20,5 L20,19 L13,19 Z M10,11 L14,11 L14,13 L10,13 Z",
    "pj_ic_comment": "M4,5 L20,5 L20,15 L12,15 L8,19 L8,15 L4,15 Z",
    "pj_ic_landscape": "M3,7 L10,7 L12,5 L21,5 L21,19 L3,19 Z M8,14 L11,11 L14,14 L16,12 L19,16 L5,16 Z",
    "pj_ic_transform": "M4,4 L10,4 L10,6 L6,6 L6,10 L4,10 Z M14,4 L20,4 L20,10 L18,10 L18,6 L14,6 Z M4,14 L6,14 L6,18 L10,18 L10,20 L4,20 Z M18,14 L20,14 L20,20 L14,20 L14,18 L18,18 Z M8,8 L16,8 L16,16 L8,16 Z",
    "pj_ic_schedule": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M11,7 L13,7 L13,12.2 L17,14.4 L16.1,16 L11,13.2 Z",
    "pj_ic_no_photo": "M4,6 L20,6 L20,18 L4,18 Z M3,3 L21,21 L19.6,22.4 L1.6,4.4 Z M8,14 L10.5,11 L13,13.5 L14.5,12 L17,15 L7,15 Z",
    "pj_ic_check_circle": "M12,3 A9,9 0,1 0,12,21 A9,9 0,1 0,12,3 Z M10.2,15.2 L6.7,11.7 L8.1,10.3 L10.2,12.4 L16.2,6.4 L17.6,7.8 Z",
    "pj_ic_account": "M12,4 A4,4 0,1 0,12,12 A4,4 0,1 0,12,4 Z M5,20 C5,16 8,14 12,14 C16,14 19,16 19,20 Z",
    "pj_ic_wave": "M4,14 C6,10 8,10 10,14 C12,18 14,18 16,14 C18,10 20,10 22,14 L22,17 C20,13 18,13 16,17 C14,21 12,21 10,17 C8,13 6,13 4,17 Z",
}

# Material icon name -> existing or new drawable (without .xml)
ALIAS: dict[str, str] = {
    "ArrowBack": "pj_ic_back",
    "ArrowForward": "pj_ic_forward",
    "KeyboardArrowRight": "pj_ic_chevron_right",
    "CheckCircle": "pj_ic_check_circle",
    "Check": "pj_ic_check",
    "CloudDone": "pj_ic_check_circle",
    "AutoAwesome": "pj_ic_sparkles",
    "AutoFixHigh": "pj_ic_magic",
    "Whatshot": "pj_ic_sparkles",
    "Star": "pj_ic_star",
    "Style": "pj_ic_sparkles",
    "Share": "pj_ic_share",
    "IosShare": "pj_ic_share",
    "Favorite": "pj_ic_heart",
    "FavoriteBorder": "pj_ic_heart",
    "Bookmark": "pj_ic_bookmark",
    "BookmarkBorder": "pj_ic_bookmark",
    "Tune": "pj_ic_tune",
    "TextFields": "pj_ic_text",
    "Close": "pj_ic_close",
    "Clear": "pj_ic_close",
    "Crop": "pj_ic_crop",
    "Search": "pj_ic_search",
    "Notifications": "pj_ic_notifications",
    "PhotoLibrary": "pj_ic_gallery",
    "Image": "pj_ic_gallery",
    "Photo": "pj_ic_gallery",
    "Collections": "pj_ic_gallery",
    "ViewModule": "pj_ic_grid",
    "GridView": "pj_ic_grid",
    "ViewAgenda": "pj_ic_grid",
    "Edit": "pj_ic_edit",
    "DriveFileRenameOutline": "pj_ic_edit",
    "Settings": "pj_ic_settings",
    "Filter": "pj_ic_filter",
    "FilterList": "pj_ic_filter",
    "Folder": "pj_ic_folder",
    "WorkspacePremium": "pj_ic_premium",
    "HighQuality": "pj_ic_premium",
    "Badge": "pj_ic_badge",
    "Movie": "pj_ic_movie",
    "Videocam": "pj_ic_video",
    "SmartDisplay": "pj_ic_play",
    "PlayArrow": "pj_ic_play",
    "Palette": "pj_ic_palette",
    "Colorize": "pj_ic_palette",
    "Block": "pj_ic_block",
    "EmojiEmotions": "pj_ic_emoji",
    "Mic": "pj_ic_mic",
    "RecordVoiceOver": "pj_ic_mic",
    "ContentCut": "pj_ic_cut",
    "Add": "pj_ic_add",
    "AddCircleOutline": "pj_ic_add",
    "Delete": "pj_ic_delete",
    "DeleteForever": "pj_ic_delete",
    "DeleteOutline": "pj_ic_delete",
    "Payments": "pj_ic_payments",
    "CreditCard": "pj_ic_payments",
    "AccountBalanceWallet": "pj_ic_wallet",
    "HelpOutline": "pj_ic_help",
    "QuestionAnswer": "pj_ic_help",
    "Info": "pj_ic_info",
    "Brightness6": "pj_ic_brightness",
    "LightMode": "pj_ic_sun",
    "WbSunny": "pj_ic_sun",
    "StickyNote2": "pj_ic_sticker",
    "Download": "pj_ic_download",
    "Refresh": "pj_ic_refresh",
    "History": "pj_ic_history",
    "AccessTime": "pj_ic_time",
    "Schedule": "pj_ic_schedule",
    "Timer": "pj_ic_timer",
    "Lock": "pj_ic_lock",
    "Security": "pj_ic_lock",
    "CameraAlt": "pj_ic_camera",
    "PhotoCamera": "pj_ic_camera",
    "CloudOff": "pj_ic_cloud_off",
    "Cloud": "pj_ic_cloud",
    "CloudUpload": "pj_ic_upload",
    "Home": "pj_ic_home",
    "Person": "pj_ic_person",
    "AccountCircle": "pj_ic_account",
    "PersonAdd": "pj_ic_person_add",
    "PersonRemove": "pj_ic_person_remove",
    "Wallpaper": "pj_ic_landscape",
    "Speed": "pj_ic_speed",
    "MoreVert": "pj_ic_more",
    "MoreHoriz": "pj_ic_more_horiz",
    "Layers": "pj_ic_layers",
    "Shield": "pj_ic_shield",
    "Send": "pj_ic_send",
    "Visibility": "pj_ic_visibility",
    "ContentCopy": "pj_ic_copy",
    "Chat": "pj_ic_chat",
    "ChatBubbleOutline": "pj_ic_comment",
    "Comment": "pj_ic_comment",
    "Language": "pj_ic_language",
    "ShoppingBag": "pj_ic_bag",
    "Save": "pj_ic_save",
    "BlurOn": "pj_ic_blur",
    "Face": "pj_ic_face",
    "FaceRetouchingNatural": "pj_ic_face",
    "Brush": "pj_ic_brush",
    "Lightbulb": "pj_ic_bulb",
    "CenterFocusStrong": "pj_ic_focus",
    "Waves": "pj_ic_waves",
    "MusicNote": "pj_ic_music",
    "Audiotrack": "pj_ic_music",
    "WavingHand": "pj_ic_wave",
    "Undo": "pj_ic_undo",
    "Redo": "pj_ic_redo",
    "Storage": "pj_ic_storage",
    "BugReport": "pj_ic_bug",
    "Campaign": "pj_ic_megaphone",
    "Public": "pj_ic_public",
    "CardGiftcard": "pj_ic_gift",
    "People": "pj_ic_people",
    "RotateRight": "pj_ic_rotate",
    "Flip": "pj_ic_flip",
    "Transform": "pj_ic_transform",
    "SwapHoriz": "pj_ic_swap",
    "WarningAmber": "pj_ic_warning",
    "ErrorOutline": "pj_ic_error",
    "Description": "pj_ic_document",
    "InsertDriveFile": "pj_ic_file",
    "FormatQuote": "pj_ic_quote",
    "Bolt": "pj_ic_bolt",
    "Gif": "pj_ic_gif",
    "FlashOn": "pj_ic_flash",
    "Healing": "pj_ic_heal",
    "FormatSize": "pj_ic_font",
    "Keyboard": "pj_ic_keyboard",
    "OpenInFull": "pj_ic_expand",
    "Cake": "pj_ic_cake",
    "Celebration": "pj_ic_celebrate",
    "Phone": "pj_ic_phone",
    "PhoneIphone": "pj_ic_device",
    "PhoneAndroid": "pj_ic_device",
    "Tablet": "pj_ic_device",
    "Laptop": "pj_ic_device",
    "DarkMode": "pj_ic_moon",
    "Analytics": "pj_ic_chart",
    "Gavel": "pj_ic_gavel",
    "Email": "pj_ic_email",
    "MarkEmailRead": "pj_ic_email",
    "CalendarMonth": "pj_ic_calendar",
    "Landscape": "pj_ic_landscape",
    "NoPhotography": "pj_ic_no_photo",
    "Wifi": "pj_ic_wifi",
    "WifiOff": "pj_ic_wifi_off",
    "Accessibility": "pj_ic_a11y",
    "SystemUpdate": "pj_ic_sync",
    "Sync": "pj_ic_sync",
    "Compare": "pj_ic_compare",
}

TEMPLATE = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:autoMirrored="{mirror}">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="{path}" />
</vector>
"""

ICON_RE = re.compile(
    r"Icons\.(?:AutoMirrored\.)?(?:Outlined|Filled|Rounded)\.(\w+)"
)


def write_vectors() -> None:
    DRAW.mkdir(parents=True, exist_ok=True)
    n = 0
    for name, path in ICONS.items():
        dest = DRAW / f"{name}.xml"
        if dest.exists() and name.startswith("pj_ic_") and name in {
            "pj_ic_add", "pj_ic_back", "pj_ic_camera", "pj_ic_check", "pj_ic_close",
            "pj_ic_community", "pj_ic_delete", "pj_ic_download", "pj_ic_edit",
            "pj_ic_explore", "pj_ic_folder", "pj_ic_forward", "pj_ic_gallery",
            "pj_ic_heart", "pj_ic_home", "pj_ic_layers", "pj_ic_magic", "pj_ic_play",
            "pj_ic_search", "pj_ic_share", "pj_ic_sparkles", "pj_ic_text",
        }:
            continue
        mirror = "true" if name in {"pj_ic_chevron_right", "pj_ic_undo", "pj_ic_redo"} else "false"
        dest.write_text(TEMPLATE.format(path=path, mirror=mirror), encoding="utf-8")
        n += 1
    print(f"wrote {n} vector files")


def rewrite_kotlin() -> None:
    changed_files = 0
    for p in JAVA.rglob("*.kt"):
        src = p.read_text(encoding="utf-8")
        orig = src

        def repl(m: re.Match[str]) -> str:
            name = m.group(1)
            drawable = ALIAS.get(name)
            if not drawable:
                # last resort: keep Material (should be rare)
                return m.group(0)
            return f"R.drawable.{drawable}"

        src = ICON_RE.sub(repl, src)
        # Icon(R.drawable.foo,  -> painter form
        src = re.sub(
            r"Icon\(\s*R\.drawable\.(\w+)\s*,",
            r"Icon(painter = painterResource(R.drawable.\1),",
            src,
        )
        src = re.sub(
            r"Icon\(\s*imageVector\s*=\s*R\.drawable\.(\w+)",
            r"Icon(painter = painterResource(R.drawable.\1)",
            src,
        )
        src = src.replace("val icon: ImageVector", "@DrawableRes val icon: Int")
        src = src.replace("icon: ImageVector", "@DrawableRes icon: Int")
        src = src.replace("val icon: ImageVector?", "@DrawableRes val icon: Int?")
        # Icon(icon,  where icon is now a drawable id
        src = re.sub(
            r"Icon\(\s*icon\s*,",
            "Icon(painter = painterResource(icon),",
            src,
        )
        src = re.sub(
            r"Icon\(\s*([A-Za-z0-9_]+)\.icon\s*,",
            r"Icon(painter = painterResource(\1.icon),",
            src,
        )
        src = re.sub(
            r"Icon\(\s*imageVector\s*=\s*icon",
            "Icon(painter = painterResource(icon)",
            src,
        )
        if src != orig:
            if "painterResource(" in src and "import androidx.compose.ui.res.painterResource" not in src:
                src = src.replace(
                    "import androidx.compose.material.icons.Icons\n",
                    "import androidx.compose.ui.res.painterResource\n",
                    1,
                )
                if "import androidx.compose.ui.res.painterResource" not in src:
                    src = re.sub(
                        r"(package [^\n]+\n)",
                        r"\1\nimport androidx.compose.ui.res.painterResource\n",
                        src,
                        count=1,
                    )
            if "@DrawableRes" in src and "import androidx.annotation.DrawableRes" not in src:
                src = re.sub(
                    r"(package [^\n]+\n)",
                    r"\1\nimport androidx.annotation.DrawableRes\n",
                    src,
                    count=1,
                )
            p.write_text(src, encoding="utf-8")
            changed_files += 1
    print(f"rewrote {changed_files} kotlin files")


if __name__ == "__main__":
    write_vectors()
    rewrite_kotlin()
    leftover = {}
    pat = re.compile(r"Icons\.(?:AutoMirrored\.)?(?:Outlined|Filled|Rounded)\.(\w+)")
    for p in JAVA.rglob("*.kt"):
        t = p.read_text(encoding="utf-8")
        for m in pat.finditer(t):
            leftover[m.group(1)] = leftover.get(m.group(1), 0) + 1
    print("leftover Material", leftover)
