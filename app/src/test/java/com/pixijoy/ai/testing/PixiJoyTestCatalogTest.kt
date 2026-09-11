package com.pixijoy.ai.testing

import org.junit.Assert.assertTrue
import org.junit.Test

class PixiJoyTestCatalogTest {

    @Test
    fun catalog_hasMinimumCoverage() {
        assertTrue("Expected at least 30 automated cases", PixiJoyTestCatalog.cases.size >= 30)
        val ids = PixiJoyTestCatalog.cases.map { it.id }
        assertTrue(ids.contains("NAV-01"))
        assertTrue(ids.contains("REG-01"))
        assertTrue(ids.contains("MON-01"))
    }

    @Test
    fun matrix_coversAllSuites() {
        QaTestSuite.entries.forEach { suite ->
            assertTrue(
                "Missing cases for suite $suite",
                QaTestMatrix.bySuite(suite).isNotEmpty()
            )
        }
    }

    @Test
    fun matrix_coversAllCategories() {
        QaTestCategory.entries.forEach { category ->
            assertTrue(
                "Missing cases for category $category",
                QaTestMatrix.byCategory(category).isNotEmpty()
            )
        }
    }
}
