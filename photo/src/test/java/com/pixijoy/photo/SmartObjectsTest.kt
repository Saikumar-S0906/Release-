package com.pixijoy.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        val page = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawColor(Color.WHITE)
        }
        val out = File.createTempFile("pixi", ".pdf")
        try {
            assertTrue(PdfExport.write(listOf(page), out))
            assertTrue(out.length() > 100)
        } finally {
            out.delete()
        }
    }
}
