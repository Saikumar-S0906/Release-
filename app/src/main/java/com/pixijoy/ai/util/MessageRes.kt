package com.pixijoy.ai.util

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

data class MessageRes(
    @StringRes val resId: Int,
    val args: Array<Any> = emptyArray()
) {
    fun resolve(context: Context): String = context.getString(resId, *args)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MessageRes
        return resId == other.resId && args.contentEquals(other.args)
    }

    override fun hashCode(): Int = 31 * resId + args.contentHashCode()
}

data class PluralRes(
    @PluralsRes val resId: Int,
    val quantity: Int,
    val args: Array<Any> = emptyArray()
) {
    fun resolve(context: Context): String = context.resources.getQuantityString(resId, quantity, *args)
}

@Composable
@ReadOnlyComposable
fun MessageRes.resolve(): String = resolve(LocalContext.current)

@Composable
@ReadOnlyComposable
fun PluralRes.resolve(): String = resolve(LocalContext.current)
