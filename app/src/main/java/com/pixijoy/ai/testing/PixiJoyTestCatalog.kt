package com.pixijoy.ai.testing

/**
 * Legacy catalog — delegates to [QaTestMatrix] for full coverage metadata.
 */
object PixiJoyTestCatalog {
    val cases: List<TestCase> = QaTestMatrix.all.map { matrixCase ->
        TestCase(
            id = matrixCase.id,
            description = matrixCase.title,
            tag = matrixCase.automationClass ?: matrixCase.id
        )
    }
}

data class TestCase(val id: String, val description: String, val tag: String)
