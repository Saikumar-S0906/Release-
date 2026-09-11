#!/usr/bin/env python3
"""Generate Android strings.xml for all supported locales from translation data."""
import os
import xml.etree.ElementTree as ET
from xml.dom import minidom

BASE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

# Key translations per locale (subset of most visible UI; Android falls back to English for missing keys)
LOCALES = {
    "values-hi": {
        "app_name": "PixiJoy AI",
        "nav_home": "होम", "nav_create": "बनाएं", "nav_meme": "मीम", "nav_stickers": "स्टिकर", "nav_pro": "प्रो",
        "get_started": "शुरू करें", "skip": "छोड़ें", "next": "आगे",
        "home_hero_title": "AI फोटो स्टूडियो", "home_hero_subtitle": "एक टैप में वायरल कंटेंट बनाएं",
        "try_now": "अभी आज़माएं", "ai_tools": "AI टूल्स", "daily_challenge": "दैनिक चुनौती",
        "explore": "खोजें", "see_more_tools": "और टूल देखें",
        "pick_photo": "फोटो चुनें", "processing": "प्रोसेस हो रहा है…", "export": "एक्सपोर्ट", "share": "शेयर",
        "pro_title": "PixiJoy Pro", "pro_unlock_features": "सभी फीचर्स अनलॉक करें",
        "offline_banner": "आप ऑफलाइन हैं — कुछ AI फीचर्स के लिए इंटरनेट चाहिए",
        "privacy_title": "गोपनीयता और नियम", "ai_consent_title": "AI फोटो प्रोसेसिंग",
        "ai_consent_accept": "मैं सहमत हूं", "ai_consent_decline": "केवल डिवाइस पर उपयोग करें",
        "meme_title": "AI मीम जनरेटर", "meme_subtitle": "कुछ भी लिखें। मीम पाएं।",
    },
    "values-es": {
        "nav_home": "Inicio", "nav_create": "Crear", "nav_meme": "Meme", "nav_stickers": "Stickers", "nav_pro": "Pro",
        "get_started": "Comenzar", "skip": "Omitir", "next": "Siguiente",
        "home_hero_title": "Estudio de Fotos IA", "home_hero_subtitle": "Crea contenido viral\nen un toque",
        "try_now": "Probar ahora", "ai_tools": "Herramientas IA", "daily_challenge": "Desafío diario",
        "explore": "Explorar", "see_more_tools": "Ver más herramientas",
        "pick_photo": "Elegir foto", "processing": "Procesando…", "export": "Exportar", "share": "Compartir",
        "pro_title": "PixiJoy Pro", "pro_unlock_features": "Desbloquear todas las funciones",
        "offline_banner": "Sin conexión — algunas funciones IA requieren internet",
        "privacy_title": "Privacidad y Términos", "ai_consent_title": "Procesamiento de fotos IA",
        "ai_consent_accept": "Acepto", "ai_consent_decline": "Solo en el dispositivo",
        "meme_title": "Generador de Memes IA", "meme_subtitle": "Escribe algo. Obtén un meme.",
    },
    "values-pt": {
        "nav_home": "Início", "nav_create": "Criar", "nav_meme": "Meme", "nav_stickers": "Stickers", "nav_pro": "Pro",
        "get_started": "Começar", "skip": "Pular", "next": "Próximo",
        "home_hero_title": "Estúdio de Fotos IA", "home_hero_subtitle": "Crie conteúdo viral\ncom um toque",
        "try_now": "Experimentar", "ai_tools": "Ferramentas IA", "daily_challenge": "Desafio diário",
        "explore": "Explorar", "see_more_tools": "Ver mais ferramentas",
        "pick_photo": "Escolher foto", "processing": "Processando…", "export": "Exportar", "share": "Compartilhar",
        "pro_title": "PixiJoy Pro", "offline_banner": "Você está offline — alguns recursos de IA precisam de internet",
        "privacy_title": "Privacidade e Termos", "meme_title": "Gerador de Memes IA",
    },
    "values-fr": {
        "nav_home": "Accueil", "nav_create": "Créer", "nav_meme": "Mème", "nav_stickers": "Autocollants", "nav_pro": "Pro",
        "get_started": "Commencer", "skip": "Passer", "next": "Suivant",
        "home_hero_title": "Studio Photo IA", "home_hero_subtitle": "Créez du contenu viral\nen un clic",
        "try_now": "Essayer", "ai_tools": "Outils IA", "explore": "Explorer",
        "pick_photo": "Choisir une photo", "processing": "Traitement…", "export": "Exporter", "share": "Partager",
        "offline_banner": "Hors ligne — certaines fonctions IA nécessitent internet",
        "privacy_title": "Confidentialité et Conditions",
    },
    "values-de": {
        "nav_home": "Start", "nav_create": "Erstellen", "nav_meme": "Meme", "nav_stickers": "Sticker", "nav_pro": "Pro",
        "get_started": "Loslegen", "skip": "Überspringen", "next": "Weiter",
        "home_hero_title": "KI-Fotostudio", "home_hero_subtitle": "Virale Inhalte\nmit einem Tipp erstellen",
        "try_now": "Jetzt testen", "ai_tools": "KI-Tools", "explore": "Entdecken",
        "pick_photo": "Foto wählen", "processing": "Verarbeitung…", "export": "Exportieren", "share": "Teilen",
        "offline_banner": "Offline — einige KI-Funktionen benötigen Internet",
        "privacy_title": "Datenschutz & Nutzungsbedingungen",
    },
    "values-ar": {
        "nav_home": "الرئيسية", "nav_create": "إنشاء", "nav_meme": "ميم", "nav_stickers": "ملصقات", "nav_pro": "برو",
        "get_started": "ابدأ", "skip": "تخطي", "next": "التالي",
        "home_hero_title": "استوديو الصور بالذكاء الاصطناعي", "home_hero_subtitle": "أنشئ محتوى viral\nبلمسة واحدة",
        "try_now": "جرّب الآن", "ai_tools": "أدوات الذكاء الاصطناعي", "explore": "استكشف",
        "pick_photo": "اختر صورة", "processing": "جارٍ المعالجة…", "export": "تصدير", "share": "مشاركة",
        "offline_banner": "أنت غير متصل — بعض ميزات الذكاء الاصطناعي تحتاج إنترنت",
        "privacy_title": "الخصوصية والشروط",
    },
    "values-zh-rCN": {
        "nav_home": "首页", "nav_create": "创建", "nav_meme": "表情包", "nav_stickers": "贴纸", "nav_pro": "专业版",
        "get_started": "开始使用", "skip": "跳过", "next": "下一步",
        "home_hero_title": "AI 照片工作室", "home_hero_subtitle": "一键创建\n病毒式内容",
        "try_now": "立即体验", "ai_tools": "AI 工具", "explore": "探索",
        "pick_photo": "选择照片", "processing": "处理中…", "export": "导出", "share": "分享",
        "offline_banner": "您已离线 — 部分 AI 功能需要网络",
        "privacy_title": "隐私与条款",
    },
    "values-zh-rTW": {
        "nav_home": "首頁", "nav_create": "建立", "nav_meme": "迷因", "nav_stickers": "貼圖", "nav_pro": "專業版",
        "get_started": "開始使用", "skip": "略過", "next": "下一步",
        "home_hero_title": "AI 相片工作室", "home_hero_subtitle": "一鍵建立\n病毒式內容",
        "try_now": "立即體驗", "ai_tools": "AI 工具", "explore": "探索",
        "pick_photo": "選擇照片", "processing": "處理中…", "export": "匯出", "share": "分享",
        "offline_banner": "您已離線 — 部分 AI 功能需要網路",
        "privacy_title": "隱私與條款",
    },
    "values-ja": {
        "nav_home": "ホーム", "nav_create": "作成", "nav_meme": "ミーム", "nav_stickers": "ステッカー", "nav_pro": "Pro",
        "get_started": "始める", "skip": "スキップ", "next": "次へ",
        "home_hero_title": "AIフォトスタジオ", "home_hero_subtitle": "ワンタップで\nバイラルコンテンツを作成",
        "try_now": "今すぐ試す", "ai_tools": "AIツール", "explore": "探索",
        "pick_photo": "写真を選択", "processing": "処理中…", "export": "エクスポート", "share": "共有",
        "offline_banner": "オフライン — 一部のAI機能にはインターネットが必要です",
        "privacy_title": "プライバシーと利用規約",
    },
    "values-ko": {
        "nav_home": "홈", "nav_create": "만들기", "nav_meme": "밈", "nav_stickers": "스티커", "nav_pro": "Pro",
        "get_started": "시작하기", "skip": "건너뛰기", "next": "다음",
        "home_hero_title": "AI 포토 스튜디오", "home_hero_subtitle": "한 번의 탭으로\n바이럴 콘텐츠 제작",
        "try_now": "지금 체험", "ai_tools": "AI 도구", "explore": "탐색",
        "pick_photo": "사진 선택", "processing": "처리 중…", "export": "보내기", "share": "공유",
        "offline_banner": "오프라인 — 일부 AI 기능은 인터넷이 필요합니다",
        "privacy_title": "개인정보 및 약관",
    },
    "values-ru": {
        "nav_home": "Главная", "nav_create": "Создать", "nav_meme": "Мем", "nav_stickers": "Стикеры", "nav_pro": "Pro",
        "get_started": "Начать", "skip": "Пропустить", "next": "Далее",
        "home_hero_title": "AI Фотостудия", "home_hero_subtitle": "Создавайте вирусный контент\nодним касанием",
        "try_now": "Попробовать", "ai_tools": "AI инструменты", "explore": "Обзор",
        "pick_photo": "Выбрать фото", "processing": "Обработка…", "export": "Экспорт", "share": "Поделиться",
        "offline_banner": "Вы офлайн — некоторые AI функции требуют интернет",
        "privacy_title": "Конфиденциальность и условия",
    },
    "values-in": {
        "nav_home": "Beranda", "nav_create": "Buat", "nav_meme": "Meme", "nav_stickers": "Stiker", "nav_pro": "Pro",
        "get_started": "Mulai", "skip": "Lewati", "next": "Berikutnya",
        "home_hero_title": "Studio Foto AI", "home_hero_subtitle": "Buat konten viral\ndalam satu ketukan",
        "try_now": "Coba Sekarang", "ai_tools": "Alat AI", "explore": "Jelajahi",
        "pick_photo": "Pilih Foto", "processing": "Memproses…", "export": "Ekspor", "share": "Bagikan",
        "offline_banner": "Anda offline — beberapa fitur AI memerlukan internet",
        "privacy_title": "Privasi & Ketentuan",
    },
    "values-tr": {
        "nav_home": "Ana Sayfa", "nav_create": "Oluştur", "nav_meme": "Meme", "nav_stickers": "Çıkartmalar", "nav_pro": "Pro",
        "get_started": "Başla", "skip": "Atla", "next": "İleri",
        "home_hero_title": "AI Fotoğraf Stüdyosu", "home_hero_subtitle": "Tek dokunuşla\nviral içerik oluşturun",
        "try_now": "Şimdi Dene", "ai_tools": "AI Araçları", "explore": "Keşfet",
        "pick_photo": "Fotoğraf Seç", "processing": "İşleniyor…", "export": "Dışa Aktar", "share": "Paylaş",
        "offline_banner": "Çevrimdışısınız — bazı AI özellikleri internet gerektirir",
        "privacy_title": "Gizlilik ve Şartlar",
    },
    "values-vi": {
        "nav_home": "Trang chủ", "nav_create": "Tạo", "nav_meme": "Meme", "nav_stickers": "Nhãn dán", "nav_pro": "Pro",
        "get_started": "Bắt đầu", "skip": "Bỏ qua", "next": "Tiếp",
        "home_hero_title": "Studio Ảnh AI", "home_hero_subtitle": "Tạo nội dung viral\nchỉ một chạm",
        "try_now": "Thử ngay", "ai_tools": "Công cụ AI", "explore": "Khám phá",
        "pick_photo": "Chọn ảnh", "processing": "Đang xử lý…", "export": "Xuất", "share": "Chia sẻ",
        "offline_banner": "Bạn đang ngoại tuyến — một số tính năng AI cần internet",
        "privacy_title": "Quyền riêng tư & Điều khoản",
    },
    "values-th": {
        "nav_home": "หน้าแรก", "nav_create": "สร้าง", "nav_meme": "มีม", "nav_stickers": "สติกเกอร์", "nav_pro": "Pro",
        "get_started": "เริ่มต้น", "skip": "ข้าม", "next": "ถัดไป",
        "home_hero_title": "สตูดิโอภาพ AI", "home_hero_subtitle": "สร้างคอนเทนต์ไวรัล\nในคลิกเดียว",
        "try_now": "ลองเลย", "ai_tools": "เครื่องมือ AI", "explore": "สำรวจ",
        "pick_photo": "เลือกรูป", "processing": "กำลังประมวลผล…", "export": "ส่งออก", "share": "แชร์",
        "offline_banner": "คุณออฟไลน์ — ฟีเจอร์ AI บางอย่างต้องใช้อินเทอร์เน็ต",
        "privacy_title": "ความเป็นส่วนตัวและข้อกำหนด",
    },
    "values-bn": {
        "nav_home": "হোম", "nav_create": "তৈরি", "nav_meme": "মিম", "nav_stickers": "স্টিকার", "nav_pro": "প্রো",
        "get_started": "শুরু করুন", "skip": "এড়িয়ে যান", "next": "পরবর্তী",
        "home_hero_title": "AI ফটো স্টুডিও", "home_hero_subtitle": "এক ট্যাপে ভাইরাল কন্টেন্ট তৈরি করুন",
        "try_now": "এখনই চেষ্টা করুন", "ai_tools": "AI টুলস", "explore": "অন্বেষণ",
        "pick_photo": "ছবি বাছুন", "processing": "প্রক্রিয়াকরণ…", "export": "রপ্তানি", "share": "শেয়ার",
        "offline_banner": "আপনি অফলাইন — কিছু AI ফিচারের জন্য ইন্টারনেট প্রয়োজন",
        "privacy_title": "গোপনীয়তা ও শর্তাবলী",
    },
    "values-te": {
        "nav_home": "హోమ్", "nav_create": "సృష్టించు", "nav_meme": "మీమ్", "nav_stickers": "స్టికర్లు", "nav_pro": "ప్రో",
        "get_started": "ప్రారంభించు", "skip": "దాటవేయి", "next": "తదుపరి",
        "home_hero_title": "AI ఫోటో స్టూడియో", "home_hero_subtitle": "ఒక ట్యాప్‌లో వైరల్ కంటెంట్ సృష్టించండి",
        "try_now": "ఇప్పుడే ప్రయత్నించండి", "ai_tools": "AI టూల్స్", "explore": "అన్వేషించు",
        "pick_photo": "ఫోటో ఎంచుకోండి", "processing": "ప్రాసెస్ అవుతోంది…", "export": "ఎగుమతి", "share": "షేర్",
        "offline_banner": "మీరు ఆఫ్‌లైన్ — కొన్ని AI ఫీచర్లకు ఇంటర్నెట్ అవసరం",
        "privacy_title": "గోప్యత & నిబంధనలు",
    },
    "values-ta": {
        "nav_home": "முகப்பு", "nav_create": "உருவாக்கு", "nav_meme": "மீம்", "nav_stickers": "ஸ்டிக்கர்கள்", "nav_pro": "ப்ரோ",
        "get_started": "தொடங்கு", "skip": "தவிர்", "next": "அடுத்து",
        "home_hero_title": "AI புகைப்பட ஸ்டுடியோ", "home_hero_subtitle": "ஒரு தட்டில் வைரல் உள்ளடக்கம் உருவாக்குங்கள்",
        "try_now": "இப்போது முயற்சி", "ai_tools": "AI கருவிகள்", "explore": "ஆராய்",
        "pick_photo": "புகைப்படம் தேர்வு", "processing": "செயலாக்கம்…", "export": "ஏற்றுமதி", "share": "பகிர்",
        "offline_banner": "நீங்கள் ஆஃப்லைன் — சில AI அம்சங்களுக்கு இணையம் தேவை",
        "privacy_title": "தனியுரிமை & விதிமுறைகள்",
    },
    "values-mr": {
        "nav_home": "होम", "nav_create": "तयार करा", "nav_meme": "मीम", "nav_stickers": "स्टिकर्स", "nav_pro": "प्रो",
        "get_started": "सुरू करा", "skip": "वगळा", "next": "पुढे",
        "home_hero_title": "AI फोटो स्टुडिओ", "home_hero_subtitle": "एका टॅपमध्ये व्हायरल कंटेंट तयार करा",
        "try_now": "आत्ता प्रयत्न करा", "ai_tools": "AI साधने", "explore": "शोधा",
        "pick_photo": "फोटो निवडा", "processing": "प्रक्रिया सुरू…", "export": "निर्यात", "share": "शेअर",
        "offline_banner": "तुम्ही ऑफलाइन आहात — काही AI वैशिष्ट्यांसाठी इंटरनेट आवश्यक",
        "privacy_title": "गोपनीयता आणि अटी",
    },
    "values-ur": {
        "nav_home": "ہوم", "nav_create": "بنائیں", "nav_meme": "میم", "nav_stickers": "اسٹیکرز", "nav_pro": "پرو",
        "get_started": "شروع کریں", "skip": "چھوڑیں", "next": "اگلا",
        "home_hero_title": "AI فوٹو اسٹوڈیو", "home_hero_subtitle": "ایک ٹیپ میں وائرل مواد بنائیں",
        "try_now": "ابھی آزمائیں", "ai_tools": "AI ٹولز", "explore": "دریافت",
        "pick_photo": "تصویر منتخب کریں", "processing": "پروسیسنگ…", "export": "برآمد", "share": "شیئر",
        "offline_banner": "آپ آف لائن ہیں — کچھ AI فیچرز کے لیے انٹرنیٹ درکار ہے",
        "privacy_title": "رازداری اور شرائط",
    },
    "values-it": {
        "nav_home": "Home", "nav_create": "Crea", "nav_meme": "Meme", "nav_stickers": "Adesivi", "nav_pro": "Pro",
        "get_started": "Inizia", "skip": "Salta", "next": "Avanti",
        "home_hero_title": "Studio Foto AI", "home_hero_subtitle": "Crea contenuti virali\ncon un tocco",
        "try_now": "Prova ora", "ai_tools": "Strumenti AI", "explore": "Esplora",
        "pick_photo": "Scegli foto", "processing": "Elaborazione…", "export": "Esporta", "share": "Condividi",
        "offline_banner": "Sei offline — alcune funzioni AI richiedono internet",
        "privacy_title": "Privacy e Termini",
    },
    "values-pl": {
        "nav_home": "Główna", "nav_create": "Utwórz", "nav_meme": "Mem", "nav_stickers": "Naklejki", "nav_pro": "Pro",
        "get_started": "Rozpocznij", "skip": "Pomiń", "next": "Dalej",
        "home_hero_title": "Studio Zdjęć AI", "home_hero_subtitle": "Twórz viralowe treści\njednym dotknięciem",
        "try_now": "Wypróbuj", "ai_tools": "Narzędzia AI", "explore": "Odkrywaj",
        "pick_photo": "Wybierz zdjęcie", "processing": "Przetwarzanie…", "export": "Eksportuj", "share": "Udostępnij",
        "offline_banner": "Jesteś offline — niektóre funkcje AI wymagają internetu",
        "privacy_title": "Prywatność i Warunki",
    },
    "values-nl": {
        "nav_home": "Home", "nav_create": "Maken", "nav_meme": "Meme", "nav_stickers": "Stickers", "nav_pro": "Pro",
        "get_started": "Beginnen", "skip": "Overslaan", "next": "Volgende",
        "home_hero_title": "AI Fotostudio", "home_hero_subtitle": "Maak virale content\nmet één tik",
        "try_now": "Nu proberen", "ai_tools": "AI-tools", "explore": "Ontdekken",
        "pick_photo": "Kies foto", "processing": "Verwerken…", "export": "Exporteren", "share": "Delen",
        "offline_banner": "Je bent offline — sommige AI-functies vereisen internet",
        "privacy_title": "Privacy & Voorwaarden",
    },
    "values-ms": {
        "nav_home": "Laman Utama", "nav_create": "Cipta", "nav_meme": "Meme", "nav_stickers": "Pelekat", "nav_pro": "Pro",
        "get_started": "Mula", "skip": "Langkau", "next": "Seterusnya",
        "home_hero_title": "Studio Foto AI", "home_hero_subtitle": "Cipta kandungan viral\ndengan satu sentuhan",
        "try_now": "Cuba Sekarang", "ai_tools": "Alat AI", "explore": "Teroka",
        "pick_photo": "Pilih Foto", "processing": "Memproses…", "export": "Eksport", "share": "Kongsi",
        "offline_banner": "Anda luar talian — beberapa ciri AI memerlukan internet",
        "privacy_title": "Privasi & Terma",
    },
    "values-fil": {
        "nav_home": "Home", "nav_create": "Gumawa", "nav_meme": "Meme", "nav_stickers": "Stickers", "nav_pro": "Pro",
        "get_started": "Magsimula", "skip": "Laktawan", "next": "Susunod",
        "home_hero_title": "AI Photo Studio", "home_hero_subtitle": "Gumawa ng viral content\nsa isang tap",
        "try_now": "Subukan Ngayon", "ai_tools": "AI Tools", "explore": "Tuklasin",
        "pick_photo": "Pumili ng Larawan", "processing": "Pinoproseso…", "export": "I-export", "share": "Ibahagi",
        "offline_banner": "Offline ka — kailangan ng internet ang ilang AI features",
        "privacy_title": "Privacy at Mga Tuntunin",
    },
}


def write_strings(locale_dir: str, translations: dict):
    os.makedirs(locale_dir, exist_ok=True)
    root = ET.Element("resources")
    for key, value in sorted(translations.items()):
        elem = ET.SubElement(root, "string", name=key)
        elem.text = value
    xml_str = ET.tostring(root, encoding="unicode")
    pretty = minidom.parseString(xml_str).toprettyxml(indent="    ")
    lines = [l for l in pretty.split("\n") if l.strip()]
    content = '<?xml version="1.0" encoding="utf-8"?>\n' + "\n".join(lines[1:]) + "\n"
    path = os.path.join(locale_dir, "strings.xml")
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Wrote {path} ({len(translations)} strings)")


if __name__ == "__main__":
    for folder, translations in LOCALES.items():
        write_strings(os.path.join(BASE, folder), translations)
