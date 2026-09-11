#!/usr/bin/env python3
"""Merge missing Phase 2/3 string keys from values/strings.xml into locale files."""
import os
import re
import xml.etree.ElementTree as ET

BASE_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")
EN_FILE = os.path.join(BASE_DIR, "values", "strings.xml")

NEW_KEYS = {
    "profile_credits_title": {
        "values-hi": "AI क्रेडिट",
        "values-ar": "رصيد الذكاء الاصطناعي",
        "values-es": "Créditos IA",
        "values-fr": "Crédits IA",
        "values-de": "KI-Guthaben",
        "values-pt": "Créditos IA",
        "values-ja": "AIクレジット",
        "values-ko": "AI 크레딧",
        "values-zh-rCN": "AI 积分",
        "values-zh-rTW": "AI 點數",
    },
    "regenerate": {
        "values-hi": "फिर से बनाएं",
        "values-ar": "إعادة الإنشاء",
        "values-es": "Regenerar",
        "values-fr": "Régénérer",
        "values-de": "Neu generieren",
        "values-pt": "Regenerar",
    },
    "result_title": {
        "values-hi": "आपकी रचना तैयार है!",
        "values-ar": "إبداعك جاهز!",
        "values-es": "¡Tu creación está lista!",
        "values-fr": "Votre création est prête !",
        "values-de": "Deine Kreation ist fertig!",
    },
    "result_what_next": {
        "values-hi": "आगे क्या करना चाहेंगे?",
        "values-ar": "ماذا تريد أن تفعل بعد ذلك؟",
        "values-es": "¿Qué te gustaría hacer ahora?",
        "values-fr": "Que souhaitez-vous faire ensuite ?",
        "values-de": "Was möchtest du als Nächstes tun?",
    },
    "nav_explore": {
        "values-hi": "खोजें",
        "values-ar": "استكشف",
        "values-es": "Explorar",
        "values-fr": "Explorer",
        "values-de": "Entdecken",
    },
    "nav_projects": {
        "values-hi": "प्रोजेक्ट",
        "values-ar": "المشاريع",
        "values-es": "Proyectos",
        "values-fr": "Projets",
        "values-de": "Projekte",
    },
    "nav_profile": {
        "values-hi": "प्रोफ़ाइल",
        "values-ar": "أنا",
        "values-es": "Perfil",
        "values-fr": "Profil",
        "values-de": "Profil",
    },
}

def load_english():
    tree = ET.parse(EN_FILE)
    return {elem.attrib["name"]: (elem.text or "") for elem in tree.getroot() if elem.tag == "string" and "name" in elem.attrib}

def merge_locale(locale_dir, english):
    path = os.path.join(BASE_DIR, locale_dir, "strings.xml")
    if not os.path.exists(path):
        return
    tree = ET.parse(path)
    root = tree.getroot()
    existing = {elem.attrib["name"] for elem in root if elem.tag == "string" and "name" in elem.attrib}
    added = 0
    for key in [
        "nav_explore", "nav_projects", "nav_profile", "home_create_with_ai", "home_what_to_make",
        "home_your_creations", "explore_title", "projects_title", "share_title",
        "regenerate", "result_title", "result_what_next", "result_create_another",
        "profile_credits_title", "sticker_step_expressions", "sticker_expressions_hint"
    ]:
        if key in existing:
            continue
        text = NEW_KEYS.get(key, {}).get(locale_dir) or english.get(key)
        if not text:
            continue
        elem = ET.SubElement(root, "string", {"name": key})
        elem.text = text
        added += 1
    if added:
        tree.write(path, encoding="utf-8", xml_declaration=True)
        print(f"{locale_dir}: added {added} keys")

if __name__ == "__main__":
    english = load_english()
    for name in os.listdir(BASE_DIR):
        if name.startswith("values-") and os.path.isdir(os.path.join(BASE_DIR, name)):
            merge_locale(name, english)
