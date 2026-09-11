package com.pixijoy.ai.core.ai

import com.pixijoy.ai.domain.model.NlAction

/**
 * Locale-agnostic keyword matching for offline NL parsing.
 * Supports English, Hindi, Telugu, Tamil, Bengali, Marathi, and common Hinglish variants.
 */
object NlKeywordMatcher {

    fun matchAction(command: String): NlAction? {
        val text = command.lowercase().trim()
        if (text.isBlank()) return null

        return when {
            containsAny(text, REMOVE_BG) -> NlAction.REMOVE_BACKGROUND
            containsAny(text, MEME) -> NlAction.MAKE_MEME
            containsAny(text, STICKER) -> NlAction.MAKE_STICKER
            containsAny(text, ENHANCE) -> NlAction.ENHANCE
            containsAny(text, PORTRAIT_BLUR) -> NlAction.PORTRAIT_BLUR
            containsAny(text, OBJECT_REMOVE) -> NlAction.OBJECT_REMOVE
            containsAny(text, REEL) -> NlAction.CREATE_REEL
            containsAny(text, BRIGHTEN) -> NlAction.BRIGHTEN
            containsAny(text, DARKEN) -> NlAction.DARKEN
            containsAny(text, PORTRAIT_STUDIO) -> NlAction.PORTRAIT_STUDIO
            containsAny(text, VARIATIONS) -> NlAction.GENERATE_VARIATIONS
            containsAny(text, ADVANCED_EDIT) -> NlAction.ADVANCED_EDIT
            containsAny(text, REPLACE_BG) -> NlAction.REMOVE_BACKGROUND
            else -> null
        }
    }

    fun defaultAction(command: String): NlAction =
        matchAction(command) ?: NlAction.MAKE_MEME

    private fun containsAny(text: String, keywords: List<String>): Boolean =
        keywords.any { keyword -> text.contains(keyword) }

    private val REMOVE_BG = listOf(
        "remove background", "remove bg", "bg remove", "cutout", "transparent background",
        "पृष्ठभूमि हटा", "बैकग्राउंड हटा", "बैकग्राउंड", "पृष्ठभूमि",
        "నేపథ్యం తీసేయి", "నేపథ్యం", "బ్యాక్‌గ్రౌండ్",
        "பின்னணி நீக்க", "பின்னணி",
        "পটভূমি সরাও", "পটভূমি",
        "पार्श्वभूमी काढ", "पार्श्वभूमी"
    )

    private val MEME = listOf(
        "meme", "funny", "joke", "humor", "comedy",
        "मीम", "मजेदार", "मजाक", "हास्य",
        "మీమ్", "ఫన్నీ", "హాస్యం",
        "மீம்", "நகைச்சுவை", "காமெடி",
        "মিম", "মজার",
        "मीम", "विनोदी"
    )

    private val STICKER = listOf(
        "sticker", "stickers", "whatsapp sticker", "wa sticker",
        "स्टिकर", "स्टीकर",
        "స్టికర్", "వాట్సాప్ స్టికర్",
        "ஸ்டிக்கர்",
        "স্টিকার",
        "स्टिकर"
    )

    private val ENHANCE = listOf(
        "enhance", "professional", "dslr", "retouch", "beautify", "improve",
        "सुधार", "बेहतर", "प्रोफेशनल",
        "మెరుగు", "ప్రొఫెషనల్",
        "மேம்படுத்த", "தொழில்முறை",
        "উন্নত", "পেশাদার"
    )

    private val PORTRAIT_BLUR = listOf(
        "portrait blur", "blur background", "bokeh",
        "ब्लर", "धुंधला पृष्ठभूमि",
        "బ్లర్", "బ్యాక్‌గ్రౌండ్ బ్లర్",
        "ப்ளர்", "பின்னணி மங்கல்"
    )

    private val OBJECT_REMOVE = listOf(
        "erase", "remove object", "remove person", "delete object",
        "हटाओ", "मिटाओ", "वस्तु हटा", "व्यक्ति हटा",
        "తీసివేయి", "ఆబ్జెక్ట్ తీసేయి",
        "நீக்கு", "பொருள் நீக்க",
        "মুছে ফেলো", "অবজেক্ট সরাও"
    )

    private val REEL = listOf(
        "reel", "video", "short video", "slideshow",
        "रील", "वीडियो",
        "రీల్", "వీడియో",
        "ரீல்", "வீடியோ",
        "রিল", "ভিডিও"
    )

    private val BRIGHTEN = listOf(
        "bright", "brighter", "lighten",
        "चमक", "उज्ज्वल", "रोशन",
        "ప్రకాశం", "వెలుగు",
        "பிரகாசம்", "வெளிச்சம்"
    )

    private val DARKEN = listOf(
        "dark", "moody", "cinematic",
        "अंधेरा", "सिनेमैटिक",
        "చీకటి", "సినిమాటిక్",
        "இருள்", "சினிமா"
    )

    private val PORTRAIT_STUDIO = listOf(
        "younger", "glow", "portrait", "teeth", "hair", "relight", "face glow",
        "जवान", "ग्लो", "दांत", "बाल",
        "చిన్నవయసు", "గ్లో", "పళ్ళు",
        "இளமை", "பற்கள்", "முகம்"
    )

    private val VARIATIONS = listOf(
        "variation", "variations", "versions", "alternatives",
        "वेरिएशन", "संस्करण",
        "వేరియేషన్", "వెర్షన్",
        "மாறுபாடு", "பதிப்பு"
    )

    private val ADVANCED_EDIT = listOf(
        "layer", "layers", "curve", "curves", "hsl", "advanced edit", "timeline", "keyframe",
        "लेयर", "उन्नत संपादन",
        "లేయర్", "అడ్వాన్స్",
        "அடுக்கு", "மேம்பட்ட"
    )

    private val REPLACE_BG = listOf(
        "beach", "office", "poster", "dubai", "lamborghini", "mars", "replace background",
        "समुद्र", "कार्यालय", "दुबई",
        "బీచ్", "ఆఫీస్",
        "கடற்கரை", "அலுவலகம்"
    )
}
