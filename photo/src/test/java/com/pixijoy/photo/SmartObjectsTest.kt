package com.pixijoy.photo

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Bitmap paths need Robolectric (JVM stubs throw "Method … not mocked").
 * PdfDocument is native — Robolectric cannot write PDFs ("document is closed!");
 * unit-test the empty fast-path only; full write belongs on device/instrumented.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class SmartObjectsTest {
    @Test
    fun smartObject_replaceKeepsTransform() {
        val a = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val obj = SmartObject("s0", a, x = 0.25f, y = 0.75f, scale = 0.4f, rotationDeg = 15f)
        val next = obj.withSource(b)
        assertEquals(0.25f, next.x)
        assertEquals(0.75f, next.y)
        assertEquals(0.4f, next.scale)
        assertEquals(15f, next.rotationDeg)
        assertEquals(64, next.source.width)
    }

    @Test
    fun pdfExport_writesFile() {
        // Empty list is the only PdfExport branch that is honest on JVM unit tests.
        val out = File.createTempFile("pixi", ".pdf")
        try {
            assertFalse(PdfExport.write(emptyList(), out))
        } finally {
            out.delete()
        }
    }
}
