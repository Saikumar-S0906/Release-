package com.pixijoy.ai.ui.session

/**
 * Parse a typed slider number and clamp it to [range].
 * Returns null when [raw] is blank or not a finite number, so the caller keeps the previous value.
 * A comma is a decimal separator when the text has no dot; with both, the later mark is the decimal.
 */
internal fun parseSliderTypedValue(
    raw: String,
    range: ClosedFloatingPointRange<Float>
): Float? {
    val value = normalizeSliderDecimal(raw.trim()).toFloatOrNull()
    if (value == null || !value.isFinite()) return null
    return value.coerceIn(range)
}

internal fun normalizeSliderDecimal(raw: String): String {
    val unified = unifySliderSeparator(raw)
    if (unified.none { it.isDigit() }) return unified
    val signed = unified.startsWith("-")
    val body = if (signed) unified.removePrefix("-") else unified
    val leading = if (body.startsWith(".")) "0$body" else body
    val trimmed = if (leading.endsWith(".")) leading.dropLast(1) else leading
    return if (signed) "-$trimmed" else trimmed
}

internal fun isPartialSliderInput(raw: String, allowNegative: Boolean): Boolean {
    val pattern = if (allowNegative) PartialSignedSlider else PartialUnsignedSlider
    return pattern.matches(raw)
}

/** Flip the leading sign. An empty draft becomes "-". */
internal fun toggleSliderSign(raw: String): String =
    if (raw.startsWith("-")) raw.removePrefix("-") else "-$raw"

private fun unifySliderSeparator(raw: String): String {
    val comma = raw.indexOf(',')
    val dot = raw.indexOf('.')
    return when {
        comma < 0 -> raw
        dot < 0 -> raw.replace(',', '.')
        comma < dot -> raw.replace(",", "")
        else -> raw.replace(".", "").replace(',', '.')
    }
}

private val PartialSignedSlider = Regex("^-?\\d*([.,]\\d*)?$")
private val PartialUnsignedSlider = Regex("^\\d*([.,]\\d*)?$")
