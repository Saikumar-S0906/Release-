package com.pixijoy.ai.domain.model

data class FestivalTemplate(
    val id: String,
    val name: String,
    val emoji: String,
    val topText: String,
    val bottomText: String,
    val greetingHindi: String = ""
)

object FestivalTemplates {
    val all = listOf(
        FestivalTemplate("diwali", "Diwali", "🪔", "DIWALI MOOD", "LIGHTS ON, MEMES ON", "शुभ दीपावली"),
        FestivalTemplate("holi", "Holi", "🎨", "HOLI HAI", "RANG LAGAO MEME BANAO", "होली की शुभकामनाएं"),
        FestivalTemplate("ugadi", "Ugadi", "🌸", "UGADI SUBHAKANKSHALU", "NEW YEAR NEW MEMES", "ఉగాది శుభాకాంక్షలు"),
        FestivalTemplate("sankranti", "Sankranti", "🪁", "PATANG UDAO", "MEME VIRAL KARO", "సంక్రాంతి శుభాకాంక్షలు"),
        FestivalTemplate("eid", "Eid", "🌙", "EID MUBARAK", "CELEBRATE WITH MEMES", "ईद मुबारक"),
        FestivalTemplate("christmas", "Christmas", "🎄", "MERRY CHRISTMAS", "SANTA BROUGHT MEMES", "क्रिसमस की शुभकामनाएं"),
        FestivalTemplate("ganesh", "Ganesh", "🙏", "GANPATI BAPPA", "MORYA! MEME TIME", "गणपति बप्पा मोरया"),
        FestivalTemplate("onam", "Onam", "🌺", "HAPPY ONAM", "SADHYA & MEMES", "ഓണാശംസകൾ"),
        FestivalTemplate("wedding", "Wedding", "💒", "SHADI SEASON", "MEME BHI READY", "शादी का मौसम"),
        FestivalTemplate("birthday", "Birthday", "🎂", "HAPPY BIRTHDAY", "CAKE KHAO MEME BANAO", "जन्मदिन मुबारक")
    )
}
