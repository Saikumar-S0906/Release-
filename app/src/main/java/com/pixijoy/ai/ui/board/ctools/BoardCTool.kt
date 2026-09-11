package com.pixijoy.ai.ui.board.ctools

import androidx.annotation.StringRes
import com.pixijoy.ai.R

/**
 * C04–C14 board tool specs. Shared wizard steps; special flags for consent / BA / motion.
 */
enum class BoardCTool(
    val code: String,
    val flow: Int,
    @StringRes val titleRes: Int,
    val creditCost: Int,
    val needsConsent: Boolean = false,
    val showBeforeAfter: Boolean = false,
    val isMotion: Boolean = false,
    val isGreeting: Boolean = false,
    val tipRes: Int,
    val optionRes: List<Int>,
    val boardFile: String
) {
    RemoveBg(
        code = "c04",
        flow = 104,
        titleRes = R.string.create_extra_remove_bg,
        creditCost = 1,
        showBeforeAfter = true,
        tipRes = R.string.board_ct_tip_remove_bg,
        optionRes = listOf(
            R.string.board_ct_opt_auto_cutout,
            R.string.board_ct_opt_replace_bg,
            R.string.board_ct_opt_transparent,
            R.string.board_ct_opt_studio_white,
            R.string.board_ct_opt_blur_bg,
            R.string.board_ct_opt_remove_object
        ),
        boardFile = "C04_Remove_Background_Object.jpg"
    ),
    Restore(
        code = "c05",
        flow = 105,
        titleRes = R.string.create_extra_restore,
        creditCost = 2,
        showBeforeAfter = true,
        tipRes = R.string.board_ct_tip_restore,
        optionRes = listOf(
            R.string.board_ct_opt_full_restore,
            R.string.board_ct_opt_colorize,
            R.string.board_ct_opt_enhance_face,
            R.string.board_ct_opt_upscale_2x
        ),
        boardFile = "C05_Restore_Old_Photo.jpg"
    ),
    FaceSwap(
        code = "c06",
        flow = 106,
        titleRes = R.string.create_extra_face_swap,
        creditCost = 3,
        needsConsent = true,
        tipRes = R.string.board_ct_tip_face_swap,
        optionRes = listOf(
            R.string.board_ct_opt_swap_faces,
            R.string.board_ct_opt_recast,
            R.string.board_ct_opt_age_shift,
            R.string.board_ct_opt_subtle_blend
        ),
        boardFile = "C06_Face_Swap_Recast.jpg"
    ),
    Scene(
        code = "c07",
        flow = 107,
        titleRes = R.string.create_extra_scene,
        creditCost = 2,
        tipRes = R.string.board_ct_tip_scene,
        optionRes = listOf(
            R.string.board_ct_opt_beach,
            R.string.board_ct_opt_city,
            R.string.board_ct_opt_mountains,
            R.string.board_ct_opt_cafe,
            R.string.board_ct_opt_studio,
            R.string.board_ct_opt_space,
            R.string.board_ct_opt_festival,
            R.string.board_ct_opt_custom
        ),
        boardFile = "C07_Change_Scene_Relocate.jpg"
    ),
    People(
        code = "c08",
        flow = 108,
        titleRes = R.string.create_extra_people,
        creditCost = 2,
        needsConsent = true,
        tipRes = R.string.board_ct_tip_people,
        optionRes = listOf(
            R.string.board_ct_opt_remove_person,
            R.string.board_ct_opt_crowd_thin,
            R.string.board_ct_opt_inpaint,
            R.string.board_ct_opt_confirm_mask
        ),
        boardFile = "C08_Add_or_Remove_People.jpg"
    ),
    Portrait(
        code = "c09",
        flow = 109,
        titleRes = R.string.create_extra_portrait,
        creditCost = 2,
        tipRes = R.string.board_ct_tip_portrait,
        optionRes = listOf(
            R.string.board_ct_opt_glow,
            R.string.board_ct_opt_relight,
            R.string.board_ct_opt_youthful,
            R.string.board_ct_opt_teeth,
            R.string.board_ct_opt_hair
        ),
        boardFile = "C09_AI_Portrait_Headshot.jpg"
    ),
    Expand(
        code = "c10",
        flow = 110,
        titleRes = R.string.create_extra_expand,
        creditCost = 2,
        tipRes = R.string.board_ct_tip_expand,
        optionRes = listOf(
            R.string.board_ct_opt_expand_out,
            R.string.board_ct_opt_fill_gaps,
            R.string.board_ct_opt_upscale_2x,
            R.string.board_ct_opt_upscale_4x,
            R.string.board_ct_opt_square_pad,
            R.string.board_ct_opt_enhance_2x
        ),
        boardFile = "C10_AI_Expand_Fill_Upscale.jpg"
    ),
    Talking(
        code = "c11",
        flow = 111,
        titleRes = R.string.create_extra_talking,
        creditCost = 3,
        isMotion = true,
        tipRes = R.string.board_ct_tip_talking,
        optionRes = listOf(
            R.string.board_ct_opt_talk,
            R.string.board_ct_opt_wave,
            R.string.board_ct_opt_smile
        ),
        boardFile = "C11_Talking_Photo.jpg"
    ),
    Greeting(
        code = "c12",
        flow = 112,
        titleRes = R.string.create_extra_greeting,
        creditCost = 1,
        isGreeting = true,
        tipRes = R.string.board_ct_tip_greeting,
        optionRes = listOf(
            R.string.board_ct_opt_birthday,
            R.string.board_ct_opt_diwali,
            R.string.board_ct_opt_eid,
            R.string.board_ct_opt_christmas,
            R.string.board_ct_opt_new_year,
            R.string.board_ct_opt_thank_you,
            R.string.board_ct_opt_love,
            R.string.board_ct_opt_celebrate
        ),
        boardFile = "C12_Greeting_Card_Festival.jpg"
    ),
    Remix(
        code = "c14",
        flow = 114,
        titleRes = R.string.create_extra_remix,
        creditCost = 2,
        tipRes = R.string.board_ct_tip_remix,
        optionRes = listOf(
            R.string.board_ct_opt_remix_style,
            R.string.board_ct_opt_boost_color,
            R.string.board_ct_opt_keep_face,
            R.string.board_ct_opt_soft_relight
        ),
        boardFile = "C14_Remix_from_Community.jpg"
    );

    val prefix: String get() = "b_$code"

    companion object {
        fun fromScreenId(screenId: String): BoardCTool? {
            val match = Regex("""^b_(c\d{2})""").find(screenId) ?: return null
            return entries.find { it.code == match.groupValues[1] }
        }

        fun stepOf(screenId: String): String {
            val tool = fromScreenId(screenId) ?: return "intro"
            val rest = screenId
                .removePrefix("${tool.prefix}_")
                .let { if (it == screenId) it.removePrefix(tool.prefix) else it }
                .trimStart('_')
            return rest.ifEmpty { "intro" }
        }
    }
}
