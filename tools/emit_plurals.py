#!/usr/bin/env python3
"""Emit localized plurals.xml for all PixiJoy locales from EN + i18n_cache when available."""
from __future__ import annotations

import json
import pathlib
import re
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
EN_PLURALS = ROOT / "app/src/main/res/values/plurals.xml"

# quantity forms commonly needed; Android falls back if missing
LOCALES = [
    "es",
    "pt-rBR",
    "fr",
    "de",
    "it",
    "in",
    "hi",
    "ar",
    "iw",
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

# Hand-translated plurals (machine-assisted) for store-ready scaffolding.
# Keys: plural_name -> {quantity: text}
TRANSLATIONS: dict[str, dict[str, dict[str, str]]] = {
    "es": {
        "ai_credits_remaining": {
            "one": "Queda %s uso de IA hoy",
            "other": "Quedan %s usos de IA hoy",
        },
        "hd_credits_remaining": {
            "one": "Queda %s exportación HD hoy",
            "other": "Quedan %s exportaciones HD hoy",
        },
        "rewarded_ads_remaining": {
            "one": "Queda %d anuncio con recompensa hoy",
            "other": "Quedan %d anuncios con recompensa hoy",
        },
        "sticker_pack_saved": {
            "one": "¡Pack de stickers guardado! %d sticker listo para WhatsApp",
            "other": "¡Pack de stickers guardado! %d stickers listos para WhatsApp",
        },
        "stickers_saved": {
            "one": "¡Se guardó %d sticker para WhatsApp!",
            "other": "¡Se guardaron %d stickers para WhatsApp!",
        },
        "streak_days": {"one": "Racha de %d día", "other": "Racha de %d días"},
        "viral_pack_saved": {
            "other": "¡Viral Pack guardado! %d imágenes listas para compartir 🚀"
        },
    },
    "pt-rBR": {
        "ai_credits_remaining": {
            "one": "%s uso de IA restante hoje",
            "other": "%s usos de IA restantes hoje",
        },
        "hd_credits_remaining": {
            "one": "%s exportação HD restante hoje",
            "other": "%s exportações HD restantes hoje",
        },
        "rewarded_ads_remaining": {
            "one": "%d anúncio premiado restante hoje",
            "other": "%d anúncios premiados restantes hoje",
        },
        "sticker_pack_saved": {
            "one": "Pacote de figurinhas salvo! %d figurinha pronta para o WhatsApp",
            "other": "Pacote de figurinhas salvo! %d figurinhas prontas para o WhatsApp",
        },
        "stickers_saved": {
            "one": "%d figurinha salva para o WhatsApp!",
            "other": "%d figurinhas salvas para o WhatsApp!",
        },
        "streak_days": {"one": "Sequência de %d dia", "other": "Sequência de %d dias"},
        "viral_pack_saved": {
            "other": "Viral Pack salvo! %d imagens prontas para compartilhar 🚀"
        },
    },
    "fr": {
        "ai_credits_remaining": {
            "one": "%s utilisation IA restante aujourd’hui",
            "other": "%s utilisations IA restantes aujourd’hui",
        },
        "hd_credits_remaining": {
            "one": "%s export HD restant aujourd’hui",
            "other": "%s exports HD restants aujourd’hui",
        },
        "rewarded_ads_remaining": {
            "one": "%d pub récompensée restante aujourd’hui",
            "other": "%d pubs récompensées restantes aujourd’hui",
        },
        "sticker_pack_saved": {
            "one": "Pack de stickers enregistré ! %d sticker prêt pour WhatsApp",
            "other": "Pack de stickers enregistré ! %d stickers prêts pour WhatsApp",
        },
        "stickers_saved": {
            "one": "%d sticker enregistré pour WhatsApp !",
            "other": "%d stickers enregistrés pour WhatsApp !",
        },
        "streak_days": {"one": "Série de %d jour", "other": "Série de %d jours"},
        "viral_pack_saved": {
            "other": "Viral Pack enregistré ! %d images prêtes à partager 🚀"
        },
    },
    "de": {
        "ai_credits_remaining": {
            "one": "Noch %s KI-Nutzung heute",
            "other": "Noch %s KI-Nutzungen heute",
        },
        "hd_credits_remaining": {
            "one": "Noch %s HD-Export heute",
            "other": "Noch %s HD-Exporte heute",
        },
        "rewarded_ads_remaining": {
            "one": "Noch %d belohnte Anzeige heute",
            "other": "Noch %d belohnte Anzeigen heute",
        },
        "sticker_pack_saved": {
            "one": "Sticker-Pack gespeichert! %d Sticker bereit für WhatsApp",
            "other": "Sticker-Pack gespeichert! %d Sticker bereit für WhatsApp",
        },
        "stickers_saved": {
            "one": "%d Sticker für WhatsApp gespeichert!",
            "other": "%d Sticker für WhatsApp gespeichert!",
        },
        "streak_days": {"one": "%d-Tage-Serie", "other": "%d-Tage-Serie"},
        "viral_pack_saved": {
            "other": "Viral Pack gespeichert! %d Bilder bereit zum Teilen 🚀"
        },
    },
    "it": {
        "ai_credits_remaining": {
            "one": "%s uso IA rimasto oggi",
            "other": "%s usi IA rimasti oggi",
        },
        "hd_credits_remaining": {
            "one": "%s export HD rimasto oggi",
            "other": "%s export HD rimasti oggi",
        },
        "rewarded_ads_remaining": {
            "one": "%d annuncio reward rimasto oggi",
            "other": "%d annunci reward rimasti oggi",
        },
        "sticker_pack_saved": {
            "one": "Pack sticker salvato! %d sticker pronto per WhatsApp",
            "other": "Pack sticker salvato! %d sticker pronti per WhatsApp",
        },
        "stickers_saved": {
            "one": "Salvato %d sticker per WhatsApp!",
            "other": "Salvati %d sticker per WhatsApp!",
        },
        "streak_days": {"one": "Serie di %d giorno", "other": "Serie di %d giorni"},
        "viral_pack_saved": {
            "other": "Viral Pack salvato! %d immagini pronte da condividere 🚀"
        },
    },
    "in": {
        "ai_credits_remaining": {
            "one": "%s penggunaan AI tersisa hari ini",
            "other": "%s penggunaan AI tersisa hari ini",
        },
        "hd_credits_remaining": {
            "one": "%s ekspor HD tersisa hari ini",
            "other": "%s ekspor HD tersisa hari ini",
        },
        "rewarded_ads_remaining": {
            "one": "%d iklan reward tersisa hari ini",
            "other": "%d iklan reward tersisa hari ini",
        },
        "sticker_pack_saved": {
            "one": "Paket stiker disimpan! %d stiker siap untuk WhatsApp",
            "other": "Paket stiker disimpan! %d stiker siap untuk WhatsApp",
        },
        "stickers_saved": {
            "one": "%d stiker disimpan untuk WhatsApp!",
            "other": "%d stiker disimpan untuk WhatsApp!",
        },
        "streak_days": {"one": "Streak %d hari", "other": "Streak %d hari"},
        "viral_pack_saved": {
            "other": "Viral Pack disimpan! %d gambar siap dibagikan 🚀"
        },
    },
    "hi": {
        "ai_credits_remaining": {
            "one": "आज %s AI उपयोग बचा है",
            "other": "आज %s AI उपयोग बचे हैं",
        },
        "hd_credits_remaining": {
            "one": "आज %s HD एक्सपोर्ट बचा है",
            "other": "आज %s HD एक्सपोर्ट बचे हैं",
        },
        "rewarded_ads_remaining": {
            "one": "आज %d रिवॉर्ड विज्ञापन बचा है",
            "other": "आज %d रिवॉर्ड विज्ञापन बचे हैं",
        },
        "sticker_pack_saved": {
            "one": "स्टिकर पैक सेव! WhatsApp के लिए %d स्टिकर तैयार",
            "other": "स्टिकर पैक सेव! WhatsApp के लिए %d स्टिकर तैयार",
        },
        "stickers_saved": {
            "one": "WhatsApp के लिए %d स्टिकर सेव!",
            "other": "WhatsApp के लिए %d स्टिकर सेव!",
        },
        "streak_days": {"one": "%d दिन की स्ट्रीक", "other": "%d दिन की स्ट्रीक"},
        "viral_pack_saved": {
            "other": "Viral Pack सेव! शेयर करने को %d इमेज तैयार 🚀"
        },
    },
    "ar": {
        "ai_credits_remaining": {
            "one": "تبقّى استخدام ذكاء اصطناعي واحد اليوم: %s",
            "other": "تبقّى %s استخدامات ذكاء اصطناعي اليوم",
        },
        "hd_credits_remaining": {
            "one": "تبقّى تصدير HD واحد اليوم: %s",
            "other": "تبقّى %s تصديرات HD اليوم",
        },
        "rewarded_ads_remaining": {
            "one": "تبقّى إعلان مكافأة واحد اليوم: %d",
            "other": "تبقّى %d إعلانات مكافأة اليوم",
        },
        "sticker_pack_saved": {
            "one": "تم حفظ حزمة الملصقات! ملصق %d جاهز لـ WhatsApp",
            "other": "تم حفظ حزمة الملصقات! %d ملصقات جاهزة لـ WhatsApp",
        },
        "stickers_saved": {
            "one": "تم حفظ ملصق %d لـ WhatsApp!",
            "other": "تم حفظ %d ملصقات لـ WhatsApp!",
        },
        "streak_days": {"one": "سلسلة %d يوم", "other": "سلسلة %d أيام"},
        "viral_pack_saved": {
            "other": "تم حفظ Viral Pack! %d صور جاهزة للمشاركة 🚀"
        },
    },
    "iw": {
        "ai_credits_remaining": {
            "one": "נותר שימוש AI אחד היום: %s",
            "other": "נותרו %s שימושי AI היום",
        },
        "hd_credits_remaining": {
            "one": "נותר ייצוא HD אחד היום: %s",
            "other": "נותרו %s ייצואי HD היום",
        },
        "rewarded_ads_remaining": {
            "one": "נותרה מודעת תגמול אחת היום: %d",
            "other": "נותרו %d מודעות תגמול היום",
        },
        "sticker_pack_saved": {
            "one": "חבילת מדבקות נשמרה! מדבקה %d מוכנה ל-WhatsApp",
            "other": "חבילת מדבקות נשמרה! %d מדבקות מוכנות ל-WhatsApp",
        },
        "stickers_saved": {
            "one": "נשמרה מדבקה %d ל-WhatsApp!",
            "other": "נשמרו %d מדבקות ל-WhatsApp!",
        },
        "streak_days": {"one": "רצף של %d יום", "other": "רצף של %d ימים"},
        "viral_pack_saved": {
            "other": "Viral Pack נשמר! %d תמונות מוכנות לשיתוף 🚀"
        },
    },
    "ur": {
        "ai_credits_remaining": {
            "one": "آج %s AI استعمال باقی",
            "other": "آج %s AI استعمالات باقی",
        },
        "hd_credits_remaining": {
            "one": "آج %s HD ایکسپورٹ باقی",
            "other": "آج %s HD ایکسپورٹس باقی",
        },
        "rewarded_ads_remaining": {
            "one": "آج %d انعامی اشتہار باقی",
            "other": "آج %d انعامی اشتہارات باقی",
        },
        "sticker_pack_saved": {
            "one": "اسٹیکر پیک محفوظ! WhatsApp کے لیے %d اسٹیکر تیار",
            "other": "اسٹیکر پیک محفوظ! WhatsApp کے لیے %d اسٹیکرز تیار",
        },
        "stickers_saved": {
            "one": "WhatsApp کے لیے %d اسٹیکر محفوظ!",
            "other": "WhatsApp کے لیے %d اسٹیکرز محفوظ!",
        },
        "streak_days": {"one": "%d دن کی اسٹریک", "other": "%d دن کی اسٹریک"},
        "viral_pack_saved": {
            "other": "Viral Pack محفوظ! شیئر کے لیے %d تصاویر تیار 🚀"
        },
    },
    "ja": {
        "ai_credits_remaining": {
            "one": "本日のAI利用残り %s 回",
            "other": "本日のAI利用残り %s 回",
        },
        "hd_credits_remaining": {
            "one": "本日のHD書き出し残り %s 回",
            "other": "本日のHD書き出し残り %s 回",
        },
        "rewarded_ads_remaining": {
            "one": "本日のリワード広告残り %d 回",
            "other": "本日のリワード広告残り %d 回",
        },
        "sticker_pack_saved": {
            "one": "ステッカーパックを保存しました！WhatsApp用ステッカー %d 個準備完了",
            "other": "ステッカーパックを保存しました！WhatsApp用ステッカー %d 個準備完了",
        },
        "stickers_saved": {
            "one": "WhatsApp用ステッカー %d 個を保存しました！",
            "other": "WhatsApp用ステッカー %d 個を保存しました！",
        },
        "streak_days": {"one": "%d 日連続", "other": "%d 日連続"},
        "viral_pack_saved": {
            "other": "Viral Packを保存しました！共有用画像 %d 枚準備完了 🚀"
        },
    },
    "ko": {
        "ai_credits_remaining": {
            "one": "오늘 AI 사용 %s회 남음",
            "other": "오늘 AI 사용 %s회 남음",
        },
        "hd_credits_remaining": {
            "one": "오늘 HD 내보내기 %s회 남음",
            "other": "오늘 HD 내보내기 %s회 남음",
        },
        "rewarded_ads_remaining": {
            "one": "오늘 리워드 광고 %d회 남음",
            "other": "오늘 리워드 광고 %d회 남음",
        },
        "sticker_pack_saved": {
            "one": "스티커 팩 저장! WhatsApp용 스티커 %d개 준비됨",
            "other": "스티커 팩 저장! WhatsApp용 스티커 %d개 준비됨",
        },
        "stickers_saved": {
            "one": "WhatsApp용 스티커 %d개 저장!",
            "other": "WhatsApp용 스티커 %d개 저장!",
        },
        "streak_days": {"one": "%d일 연속", "other": "%d일 연속"},
        "viral_pack_saved": {
            "other": "Viral Pack 저장! 공유할 이미지 %d장 준비됨 🚀"
        },
    },
    "zh-rCN": {
        "ai_credits_remaining": {
            "one": "今日剩余 %s 次 AI 使用",
            "other": "今日剩余 %s 次 AI 使用",
        },
        "hd_credits_remaining": {
            "one": "今日剩余 %s 次 HD 导出",
            "other": "今日剩余 %s 次 HD 导出",
        },
        "rewarded_ads_remaining": {
            "one": "今日剩余 %d 次激励广告",
            "other": "今日剩余 %d 次激励广告",
        },
        "sticker_pack_saved": {
            "one": "贴纸包已保存！%d 张贴纸可供 WhatsApp 使用",
            "other": "贴纸包已保存！%d 张贴纸可供 WhatsApp 使用",
        },
        "stickers_saved": {
            "one": "已保存 %d 张 WhatsApp 贴纸！",
            "other": "已保存 %d 张 WhatsApp 贴纸！",
        },
        "streak_days": {"one": "连续 %d 天", "other": "连续 %d 天"},
        "viral_pack_saved": {
            "other": "Viral Pack 已保存！%d 张图片可分享 🚀"
        },
    },
    "zh-rTW": {
        "ai_credits_remaining": {
            "one": "今日剩餘 %s 次 AI 使用",
            "other": "今日剩餘 %s 次 AI 使用",
        },
        "hd_credits_remaining": {
            "one": "今日剩餘 %s 次 HD 匯出",
            "other": "今日剩餘 %s 次 HD 匯出",
        },
        "rewarded_ads_remaining": {
            "one": "今日剩餘 %d 次獎勵廣告",
            "other": "今日剩餘 %d 次獎勵廣告",
        },
        "sticker_pack_saved": {
            "one": "貼圖包已儲存！%d 張貼圖可供 WhatsApp 使用",
            "other": "貼圖包已儲存！%d 張貼圖可供 WhatsApp 使用",
        },
        "stickers_saved": {
            "one": "已儲存 %d 張 WhatsApp 貼圖！",
            "other": "已儲存 %d 張 WhatsApp 貼圖！",
        },
        "streak_days": {"one": "連續 %d 天", "other": "連續 %d 天"},
        "viral_pack_saved": {
            "other": "Viral Pack 已儲存！%d 張圖片可分享 🚀"
        },
    },
    "th": {
        "ai_credits_remaining": {
            "one": "เหลือการใช้ AI %s ครั้งวันนี้",
            "other": "เหลือการใช้ AI %s ครั้งวันนี้",
        },
        "hd_credits_remaining": {
            "one": "เหลือการส่งออก HD %s ครั้งวันนี้",
            "other": "เหลือการส่งออก HD %s ครั้งวันนี้",
        },
        "rewarded_ads_remaining": {
            "one": "เหลือโฆษณารับรางวัล %d ครั้งวันนี้",
            "other": "เหลือโฆษณารับรางวัล %d ครั้งวันนี้",
        },
        "sticker_pack_saved": {
            "one": "บันทึกแพ็กสติกเกอร์แล้ว! สติกเกอร์ %d อันพร้อมสำหรับ WhatsApp",
            "other": "บันทึกแพ็กสติกเกอร์แล้ว! สติกเกอร์ %d อันพร้อมสำหรับ WhatsApp",
        },
        "stickers_saved": {
            "one": "บันทึกสติกเกอร์ %d อันสำหรับ WhatsApp แล้ว!",
            "other": "บันทึกสติกเกอร์ %d อันสำหรับ WhatsApp แล้ว!",
        },
        "streak_days": {"one": "สตรีค %d วัน", "other": "สตรีค %d วัน"},
        "viral_pack_saved": {
            "other": "บันทึก Viral Pack แล้ว! รูป %d ภาพพร้อมแชร์ 🚀"
        },
    },
    "vi": {
        "ai_credits_remaining": {
            "one": "Còn %s lượt dùng AI hôm nay",
            "other": "Còn %s lượt dùng AI hôm nay",
        },
        "hd_credits_remaining": {
            "one": "Còn %s lần xuất HD hôm nay",
            "other": "Còn %s lần xuất HD hôm nay",
        },
        "rewarded_ads_remaining": {
            "one": "Còn %d quảng cáo thưởng hôm nay",
            "other": "Còn %d quảng cáo thưởng hôm nay",
        },
        "sticker_pack_saved": {
            "one": "Đã lưu gói sticker! %d sticker sẵn sàng cho WhatsApp",
            "other": "Đã lưu gói sticker! %d sticker sẵn sàng cho WhatsApp",
        },
        "stickers_saved": {
            "one": "Đã lưu %d sticker cho WhatsApp!",
            "other": "Đã lưu %d sticker cho WhatsApp!",
        },
        "streak_days": {"one": "Chuỗi %d ngày", "other": "Chuỗi %d ngày"},
        "viral_pack_saved": {
            "other": "Đã lưu Viral Pack! %d ảnh sẵn sàng chia sẻ 🚀"
        },
    },
    "tr": {
        "ai_credits_remaining": {
            "one": "Bugün %s YZ kullanımı kaldı",
            "other": "Bugün %s YZ kullanımı kaldı",
        },
        "hd_credits_remaining": {
            "one": "Bugün %s HD dışa aktarma kaldı",
            "other": "Bugün %s HD dışa aktarma kaldı",
        },
        "rewarded_ads_remaining": {
            "one": "Bugün %d ödüllü reklam kaldı",
            "other": "Bugün %d ödüllü reklam kaldı",
        },
        "sticker_pack_saved": {
            "one": "Çıkartma paketi kaydedildi! WhatsApp için %d çıkartma hazır",
            "other": "Çıkartma paketi kaydedildi! WhatsApp için %d çıkartma hazır",
        },
        "stickers_saved": {
            "one": "WhatsApp için %d çıkartma kaydedildi!",
            "other": "WhatsApp için %d çıkartma kaydedildi!",
        },
        "streak_days": {"one": "%d günlük seri", "other": "%d günlük seri"},
        "viral_pack_saved": {
            "other": "Viral Pack kaydedildi! Paylaşmaya hazır %d görsel 🚀"
        },
    },
    "ru": {
        "ai_credits_remaining": {
            "one": "Сегодня осталось %s использование ИИ",
            "few": "Сегодня осталось %s использования ИИ",
            "many": "Сегодня осталось %s использований ИИ",
            "other": "Сегодня осталось %s использований ИИ",
        },
        "hd_credits_remaining": {
            "one": "Сегодня остался %s HD-экспорт",
            "few": "Сегодня осталось %s HD-экспорта",
            "many": "Сегодня осталось %s HD-экспортов",
            "other": "Сегодня осталось %s HD-экспортов",
        },
        "rewarded_ads_remaining": {
            "one": "Сегодня осталась %d реклама с наградой",
            "few": "Сегодня осталось %d рекламы с наградой",
            "many": "Сегодня осталось %d реклам с наградой",
            "other": "Сегодня осталось %d реклам с наградой",
        },
        "sticker_pack_saved": {
            "one": "Набор стикеров сохранён! %d стикер готов для WhatsApp",
            "few": "Набор стикеров сохранён! %d стикера готовы для WhatsApp",
            "many": "Набор стикеров сохранён! %d стикеров готовы для WhatsApp",
            "other": "Набор стикеров сохранён! %d стикеров готовы для WhatsApp",
        },
        "stickers_saved": {
            "one": "Сохранён %d стикер для WhatsApp!",
            "few": "Сохранены %d стикера для WhatsApp!",
            "many": "Сохранено %d стикеров для WhatsApp!",
            "other": "Сохранено %d стикеров для WhatsApp!",
        },
        "streak_days": {
            "one": "Серия %d день",
            "few": "Серия %d дня",
            "many": "Серия %d дней",
            "other": "Серия %d дней",
        },
        "viral_pack_saved": {
            "other": "Viral Pack сохранён! %d изображений готовы к публикации 🚀"
        },
    },
    "pl": {
        "ai_credits_remaining": {
            "one": "Pozostało %s użycie AI dziś",
            "few": "Pozostały %s użycia AI dziś",
            "many": "Pozostało %s użyć AI dziś",
            "other": "Pozostało %s użyć AI dziś",
        },
        "hd_credits_remaining": {
            "one": "Pozostał %s eksport HD dziś",
            "few": "Pozostały %s eksporty HD dziś",
            "many": "Pozostało %s eksportów HD dziś",
            "other": "Pozostało %s eksportów HD dziś",
        },
        "rewarded_ads_remaining": {
            "one": "Pozostała %d reklama z nagrodą dziś",
            "few": "Pozostały %d reklamy z nagrodą dziś",
            "many": "Pozostało %d reklam z nagrodą dziś",
            "other": "Pozostało %d reklam z nagrodą dziś",
        },
        "sticker_pack_saved": {
            "one": "Zapisano pakiet naklejek! %d naklejka gotowa do WhatsApp",
            "few": "Zapisano pakiet naklejek! %d naklejki gotowe do WhatsApp",
            "many": "Zapisano pakiet naklejek! %d naklejek gotowych do WhatsApp",
            "other": "Zapisano pakiet naklejek! %d naklejek gotowych do WhatsApp",
        },
        "stickers_saved": {
            "one": "Zapisano %d naklejkę do WhatsApp!",
            "few": "Zapisano %d naklejki do WhatsApp!",
            "many": "Zapisano %d naklejek do WhatsApp!",
            "other": "Zapisano %d naklejek do WhatsApp!",
        },
        "streak_days": {
            "one": "Seria %d dzień",
            "few": "Seria %d dni",
            "many": "Seria %d dni",
            "other": "Seria %d dni",
        },
        "viral_pack_saved": {
            "other": "Zapisano Viral Pack! %d obrazów gotowych do udostępnienia 🚀"
        },
    },
    "fil": {
        "ai_credits_remaining": {
            "one": "%s AI use ang natitira ngayon",
            "other": "%s AI uses ang natitira ngayon",
        },
        "hd_credits_remaining": {
            "one": "%s HD export ang natitira ngayon",
            "other": "%s HD exports ang natitira ngayon",
        },
        "rewarded_ads_remaining": {
            "one": "%d rewarded ad ang natitira ngayon",
            "other": "%d rewarded ads ang natitira ngayon",
        },
        "sticker_pack_saved": {
            "one": "Na-save ang sticker pack! %d sticker handa para sa WhatsApp",
            "other": "Na-save ang sticker pack! %d stickers handa para sa WhatsApp",
        },
        "stickers_saved": {
            "one": "Na-save ang %d sticker para sa WhatsApp!",
            "other": "Na-save ang %d stickers para sa WhatsApp!",
        },
        "streak_days": {"one": "%d araw na streak", "other": "%d araw na streak"},
        "viral_pack_saved": {
            "other": "Na-save ang Viral Pack! %d images handa nang i-share 🚀"
        },
    },
    "ms": {
        "ai_credits_remaining": {
            "one": "%s penggunaan AI berbaki hari ini",
            "other": "%s penggunaan AI berbaki hari ini",
        },
        "hd_credits_remaining": {
            "one": "%s eksport HD berbaki hari ini",
            "other": "%s eksport HD berbaki hari ini",
        },
        "rewarded_ads_remaining": {
            "one": "%d iklan ganjaran berbaki hari ini",
            "other": "%d iklan ganjaran berbaki hari ini",
        },
        "sticker_pack_saved": {
            "one": "Pek pelekat disimpan! %d pelekat sedia untuk WhatsApp",
            "other": "Pek pelekat disimpan! %d pelekat sedia untuk WhatsApp",
        },
        "stickers_saved": {
            "one": "%d pelekat disimpan untuk WhatsApp!",
            "other": "%d pelekat disimpan untuk WhatsApp!",
        },
        "streak_days": {"one": "Streak %d hari", "other": "Streak %d hari"},
        "viral_pack_saved": {
            "other": "Viral Pack disimpan! %d imej sedia dikongsi 🚀"
        },
    },
    "uk": {
        "ai_credits_remaining": {
            "one": "Сьогодні залишилось %s використання ШІ",
            "few": "Сьогодні залишилось %s використання ШІ",
            "many": "Сьогодні залишилось %s використань ШІ",
            "other": "Сьогодні залишилось %s використань ШІ",
        },
        "hd_credits_remaining": {
            "one": "Сьогодні залишився %s HD-експорт",
            "few": "Сьогодні залишилось %s HD-експорти",
            "many": "Сьогодні залишилось %s HD-експортів",
            "other": "Сьогодні залишилось %s HD-експортів",
        },
        "rewarded_ads_remaining": {
            "one": "Сьогодні залишилась %d реклама з нагородою",
            "few": "Сьогодні залишилось %d реклами з нагородою",
            "many": "Сьогодні залишилось %d реклам з нагородою",
            "other": "Сьогодні залишилось %d реклам з нагородою",
        },
        "sticker_pack_saved": {
            "one": "Набір стікерів збережено! %d стікер готовий для WhatsApp",
            "few": "Набір стікерів збережено! %d стікери готові для WhatsApp",
            "many": "Набір стікерів збережено! %d стікерів готові для WhatsApp",
            "other": "Набір стікерів збережено! %d стікерів готові для WhatsApp",
        },
        "stickers_saved": {
            "one": "Збережено %d стікер для WhatsApp!",
            "few": "Збережено %d стікери для WhatsApp!",
            "many": "Збережено %d стікерів для WhatsApp!",
            "other": "Збережено %d стікерів для WhatsApp!",
        },
        "streak_days": {
            "one": "Серія %d день",
            "few": "Серія %d дні",
            "many": "Серія %d днів",
            "other": "Серія %d днів",
        },
        "viral_pack_saved": {
            "other": "Viral Pack збережено! %d зображень готові до поширення 🚀"
        },
    },
}


def escape_android(text: str) -> str:
    safe = text
    if "&" in safe and not re.search(
        r"&(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);", safe
    ):
        safe = safe.replace("&", "&amp;")
    if "'" in safe:
        safe = safe.replace("\\'", "'").replace("'", r"\'")
    return safe


def emit_locale(loc: str) -> None:
    data = TRANSLATIONS[loc]
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- PixiJoy {loc} plurals — machine-assisted; review before store release -->",
        "<resources>",
    ]
    for name, quantities in data.items():
        lines.append(f'    <plurals name="{name}">')
        for qty, text in quantities.items():
            lines.append(
                f'        <item quantity="{qty}">{escape_android(text)}</item>'
            )
        lines.append("    </plurals>")
    lines.append("</resources>\n")
    dest = ROOT / f"app/src/main/res/values-{loc}/plurals.xml"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text("\n".join(lines), encoding="utf-8")
    print(f"wrote {dest.relative_to(ROOT)}")


def main() -> None:
    for loc in LOCALES:
        if loc not in TRANSLATIONS:
            raise SystemExit(f"missing plurals for {loc}")
        emit_locale(loc)


if __name__ == "__main__":
    main()
