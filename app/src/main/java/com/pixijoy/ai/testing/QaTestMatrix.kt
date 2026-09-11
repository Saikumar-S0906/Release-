package com.pixijoy.ai.testing

/**
 * Full QA test matrix: every case tagged with suite, category, and automation mapping.
 */
object QaTestMatrix {
    val all: List<QaTestCase> = buildList {
        addAll(navigationCases())
        addAll(coldStartCases())
        addAll(memeCases())
        addAll(stickerCases())
        addAll(createForMeCases())
        addAll(creditsCases())
        addAll(settingsCases())
        addAll(stabilityCases())
        addAll(regressionCases())
    }

    fun bySuite(suite: QaTestSuite): List<QaTestCase> = all.filter { it.suite == suite }
    fun byCategory(category: QaTestCategory): List<QaTestCase> = all.filter { it.category == category }
    fun automated(): List<QaTestCase> = all.filter { it.automationClass != null }

    private fun navigationCases() = listOf(
        case("NAV-01", QaTestSuite.UI, QaTestCategory.POSITIVE, "Home tab loads", "PixiJoyNavigationSmokeTest", "homeTab_showsHero"),
        case("NAV-02", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "All bottom tabs reachable", "PixiJoyNavigationSmokeTest", "allBottomTabs_areReachable"),
        case("NAV-03", QaTestSuite.UI, QaTestCategory.POSITIVE, "Create hub photo tile", "PixiJoyScreenReachabilityTest", "createHub_photoTile_isDisplayed"),
        case("NAV-04", QaTestSuite.EDGE, QaTestCategory.EDGE, "Rapid tab switching", "EdgeCaseTest", "rapidTabSwitch_doesNotCrash"),
        case("NAV-05", QaTestSuite.EDGE, QaTestCategory.CORNER, "Double-tap same tab", "EdgeCaseTest", "doubleTapHomeTab_staysStable"),
    )

    private fun coldStartCases() = listOf(
        case("COLD-01", QaTestSuite.E2E, QaTestCategory.POSITIVE, "Privacy accept → onboarding skip → home", "ColdStartE2ETest", "privacyToHome_happyPath"),
        case("COLD-02", QaTestSuite.E2E, QaTestCategory.NEGATIVE, "Fresh install shows privacy gate", "ColdStartE2ETest", "freshInstall_showsPrivacy"),
        case("COLD-03", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "Seeded user lands on home", "PixiJoyNavigationSmokeTest", "homeTab_showsHero"),
    )

    private fun memeCases() = listOf(
        case("MEME-01", QaTestSuite.E2E, QaTestCategory.POSITIVE, "Navigate to meme generator", "MemeStickerFlowTest", "navigateToMemeScreen"),
        case("MEME-02", QaTestSuite.NEGATIVE, QaTestCategory.NEGATIVE, "Generate without photo shows error", "NegativeCaseTest", "meme_generateWithoutPhoto_blocked"),
        case("MEME-03", QaTestSuite.UI, QaTestCategory.POSITIVE, "Meme screen shows generate button", "MemeStickerFlowTest", "memeScreen_showsGenerateButton"),
        case("MEME-04", QaTestSuite.REGRESSION, QaTestCategory.EDGE, "Back from meme returns to create", "MemeStickerFlowTest", "meme_backNavigation_works"),
    )

    private fun stickerCases() = listOf(
        case("STKR-01", QaTestSuite.E2E, QaTestCategory.POSITIVE, "Navigate to sticker studio", "MemeStickerFlowTest", "navigateToStickerScreen"),
        case("STKR-02", QaTestSuite.NEGATIVE, QaTestCategory.NEGATIVE, "Generate pack without photo blocked", "NegativeCaseTest", "sticker_generateWithoutPhoto_blocked"),
        case("STKR-03", QaTestSuite.NEGATIVE, QaTestCategory.EDGE, "Deselect last emotion prevented", "NegativeCaseTest", "sticker_cannotDeselectLastEmotion"),
    )

    private fun createForMeCases() = listOf(
        case("CFM-01", QaTestSuite.E2E, QaTestCategory.POSITIVE, "Add photo opens board create (b5_choose)", "PixiJoyNavigationSmokeTest", "homeAddPhoto_opensCreateForMe"),
        case("CFM-02", QaTestSuite.UI, QaTestCategory.POSITIVE, "Board create choose reachable (was Create for me)", "MemeStickerFlowTest", "createForMe_screenLoads"),
        case("CFM-03", QaTestSuite.NEGATIVE, QaTestCategory.NEGATIVE, "Continue disabled without command", "NegativeCaseTest", "createForMe_continueDisabledWithoutPhoto"),
    )

    private fun creditsCases() = listOf(
        case("CRD-01", QaTestSuite.EDGE, QaTestCategory.EDGE, "Zero HD credits state seeds", "CreditEdgeCaseTest", "exhaustedHdCredits_seedsCorrectly"),
        case("CRD-02", QaTestSuite.EDGE, QaTestCategory.EDGE, "Zero AI credits state seeds", "CreditEdgeCaseTest", "exhaustedAiCredits_seedsCorrectly"),
        case("CRD-03", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "Pro user seeds unlimited", "CreditEdgeCaseTest", "proUser_seedsCorrectly"),
    )

    private fun settingsCases() = listOf(
        case("SET-01", QaTestSuite.UI, QaTestCategory.POSITIVE, "Settings bug report visible", "PixiJoyScreenReachabilityTest", "profile_opensSettings_withBugReport"),
        case("SET-02", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "QA report file builds", "QaReportManagerTest", "buildsReportFile_withDeviceInfo"),
    )

    private fun stabilityCases() = listOf(
        case("MON-01", QaTestSuite.MONKEY, QaTestCategory.CORNER, "Random nav 40 iterations no crash", "MonkeyStabilityTest", "randomNavigation_noCrash"),
        case("MON-02", QaTestSuite.STABILITY, QaTestCategory.CORNER, "Back press loop 20 times", "MonkeyStabilityTest", "backPressLoop_noCrash"),
        case("MON-03", QaTestSuite.EDGE, QaTestCategory.EDGE, "Screen rotation survives", "EdgeCaseTest", "rotation_doesNotCrash"),
    )

    private fun regressionCases() = listOf(
        case("REG-01", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "Full smoke regression bundle", "RegressionSuiteTest", "runFullRegressionSmoke"),
        case("LOC-01", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "24 locale strings resolve", "LocaleResourcesTest", "coreStringsResolveForPriorityLocales"),
        case("LOC-02", QaTestSuite.EDGE, QaTestCategory.EDGE, "Arabic RTL layout", "LocaleResourcesTest", "arabicUsesRtlLayoutDirection"),
        case("CAP-01", QaTestSuite.REGRESSION, QaTestCategory.POSITIVE, "All caption tones generate", "ToneCaptionGeneratorTest", "generatesCaption_forEachTone"),
    )

    private fun case(
        id: String,
        suite: QaTestSuite,
        category: QaTestCategory,
        title: String,
        automationClass: String,
        automationMethod: String
    ) = QaTestCase(id, suite, category, title, automationClass, automationMethod)
}

enum class QaTestSuite { UI, E2E, REGRESSION, NEGATIVE, EDGE, STABILITY, MONKEY }
enum class QaTestCategory { POSITIVE, NEGATIVE, EDGE, CORNER }

data class QaTestCase(
    val id: String,
    val suite: QaTestSuite,
    val category: QaTestCategory,
    val title: String,
    val automationClass: String?,
    val automationMethod: String?
)
