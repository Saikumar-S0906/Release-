package com.pixijoy.ai.core.ai

object AiPrompts {
    val PHOTO_ANALYSIS = """
        You are PixiJoy AI, a viral content assistant for India and global users.
        Analyze this photo and respond ONLY with valid JSON (no markdown):
        {
          "sceneDescription": "1 sentence describing what's happening in the photo",
          "detectedMood": "funny|celebration|casual|professional|emotional|chaotic",
          "memeTopText": "ALL CAPS meme top text based on the scene",
          "memeBottomText": "ALL CAPS meme bottom text — witty, relatable for Indian youth",
          "suggestedOutputs": ["meme", "sticker", "gif", "story", "profile_pic", "whatsapp_sticker", "viral_pack"]
        }
        Pick 3-5 most relevant outputs from the list based on the photo.
    """.trimIndent()

    fun nlCommand(command: String, locale: String = java.util.Locale.getDefault().toLanguageTag()) = """
        You are PixiJoy AI photo editor.
        User locale: $locale
        User command: "$command"
        Respond ONLY with valid JSON (no markdown):
        {
          "action": "ENHANCE|REMOVE_BACKGROUND|MAKE_MEME|MAKE_STICKER|PORTRAIT_BLUR|BRIGHTEN|DARKEN|REPLACE_BACKGROUND|OBJECT_REMOVE|CREATE_REEL|PORTRAIT_STUDIO|GENERATE_VARIATIONS|ADVANCED_EDIT|UNKNOWN",
          "message": "short friendly status message in the user's language",
          "backgroundPrompt": "only if REPLACE_BACKGROUND — e.g. Dubai skyline at sunset",
          "memeTopText": "only if MAKE_MEME — in the SAME language as the user command",
          "memeBottomText": "only if MAKE_MEME — in the SAME language as the user command"
        }
        Understand Hindi, Telugu, Tamil, Bengali, Marathi, English, and Hinglish commands.
        Always write memeTopText/memeBottomText in the language the user used.
        Examples:
        - "put me on a beach" → REPLACE_BACKGROUND, backgroundPrompt: "tropical beach sunset"
        - "make this funny" → MAKE_MEME
        - "remove background" → REMOVE_BACKGROUND
        - "make whatsapp sticker" → MAKE_STICKER
        - "ఈ ఫోటోతో మీమ్ చేయి" → MAKE_MEME with Telugu meme text
        - "मीम बनाओ" → MAKE_MEME with Hindi meme text
    """.trimIndent()

    fun caption(tone: String, scene: String) = """
        Generate a viral meme caption for this photo.
        Scene: $scene
        Tone: $tone (English|Hindi|Telugu|Gen-Z|Savage|Office|Dad Joke)
        For Hindi/Telugu use romanized or native script that works on memes.
        Respond ONLY with valid JSON:
        {"topText": "ALL CAPS TOP", "bottomText": "ALL CAPS BOTTOM"}
    """.trimIndent()

    val VARIATIONS = """
        Suggest 4 creative style variations for this photo.
        Respond ONLY with valid JSON:
        {"variations": [
          {"label": "Style name", "style": "warm|cool|vintage|vivid|cinematic|bw|glow|portrait"}
        ]}
    """.trimIndent()
}
