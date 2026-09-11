package com.pixijoy.ai.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.pixijoy.ai.R

/**
 * Canvas / bitmap text. Downloadable Noto XML may be unavailable offline —
 * always falls back; never throws.
 */
object CanvasFontHelper {

    fun memeTypeface(context: Context, bold: Boolean = true): Typeface {
        return safeFont(context, if (bold) R.font.noto_sans_bold else R.font.noto_sans_regular)
            ?: if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    fun labelTypeface(context: Context): Typeface {
        return safeFont(context, R.font.noto_sans_regular) ?: Typeface.SANS_SERIF
    }

    private fun safeFont(context: Context, fontRes: Int): Typeface? = try {
        ResourcesCompat.getFont(context, fontRes)
    } catch (_: Throwable) {
        null
    }
}
