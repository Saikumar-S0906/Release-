package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 4-up stays encoded as four columns and draws as a square 2×2. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class CollageLayoutTest {
    @Test
    fun fourUp_isTwoByTwo_notOneRow() {
        assertEquals(2 to 2, collageGrid(4, 4))
        assertEquals(2 to 1, collageGrid(2, 2))
        val sources = List(4) { index ->
            Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(Color.rgb(40 + index * 50, 10, 10))
            }
        }
        val out = applyCollage(sources, 4, 400)
        // 2×2 of a 400 edge is 400×400. A stored row of four would be 400×100.
        assertEquals(400, out.width)
        assertEquals(400, out.height)
        sources.forEach { if (!it.isRecycled) it.recycle() }
        if (!out.isRecycled) out.recycle()
    }
}
