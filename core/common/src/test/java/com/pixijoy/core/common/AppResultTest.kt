package com.pixijoy.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {
    @Test
    fun okAndErrHoldValues() {
        val ok = AppResult.Ok(3)
        val err = AppResult.Err("nope")
        assertEquals(3, ok.value)
        assertEquals("nope", err.message)
        assertTrue(err.cause == null)
    }
}
