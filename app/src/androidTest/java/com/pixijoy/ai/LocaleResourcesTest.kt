package com.pixijoy.ai

import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleResourcesTest {

  private val coreStringIds = listOf(
    R.string.nav_home,
    R.string.nav_create,
    R.string.export,
    R.string.privacy_title,
    R.string.settings,
    R.string.offline_banner,
    R.string.slow_network,
    R.string.create_hub_tools,
    R.string.create_extra_remove_bg,
    R.string.create_extra_generate,
  )

  private val localeTags = listOf(
    "hi", "es", "ar", "zh-CN", "ja", "ko", "ru", "pt", "fr", "de",
    "bn", "te", "ta", "mr", "ur", "vi", "th", "tr", "id", "it", "pl", "nl", "ms", "fil",
  )

  @Test
  fun coreStringsResolveForPriorityLocales() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    for (tag in localeTags) {
      val localized = localizedContext(appContext, tag)
      for (resId in coreStringIds) {
        val value = localized.getString(resId)
        assertFalse("Empty string for $tag res=$resId", value.isBlank())
      }
    }
  }

  @Test
  fun arabicUsesRtlLayoutDirection() {
    val locale = Locale.forLanguageTag("ar")
    assertTrue(TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL)
  }

  @Test
  fun hindiNavHomeIsTranslated() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val hi = localizedContext(appContext, "hi")
    assertFalse(hi.getString(R.string.nav_home).equals("Home", ignoreCase = true))
  }

  @Test
  fun hindiPluralsResolve() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val hi = localizedContext(appContext, "hi")
    val streak = hi.resources.getQuantityString(R.plurals.streak_days, 3, 3)
    assertFalse(streak.isBlank())
  }

  private fun localizedContext(
    base: android.content.Context,
    languageTag: String,
  ): android.content.Context {
    val locale = Locale.forLanguageTag(languageTag)
    val config = Configuration(base.resources.configuration)
    config.setLocale(locale)
    return base.createConfigurationContext(config)
  }
}
