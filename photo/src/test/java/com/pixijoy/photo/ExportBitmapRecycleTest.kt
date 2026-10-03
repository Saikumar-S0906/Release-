package com.pixijoy.photo

import android.graphics.Bitmap
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Color-intent export must free both the original and the copy. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class ExportBitmapRecycleTest {
    @Test
    fun recyclesColorIntentCopyAndOriginal() {
        val rendered = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val colored = rendered.copy(Bitmap.Config.ARGB_8888, true)
        recycleExportBitmaps(rendered, colored)
        assertTrue(rendered.isRecycled)
        assertTrue(colored.isRecycled)
    }

    @Test
    fun recyclesASingleBitmapOnce() {
        val rendered = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        recycleExportBitmaps(rendered, rendered)
        assertTrue(rendered.isRecycled)
    }
}
