package com.pixijoy.ai.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

object LocaleFormatters {

    fun formatNumber(value: Int, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getNumberInstance(locale).format(value)

    fun formatCredits(value: Int, locale: Locale = Locale.getDefault()): String =
        formatNumber(value, locale)

    fun formatDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

    fun formatCurrency(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        return format.format(amount)
    }

    fun deviceLanguageTag(): String = Locale.getDefault().toLanguageTag()
}
