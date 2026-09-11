package com.pixijoy.ai.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

sealed class UiMessage {
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiMessage()
    data class Raw(val text: String) : UiMessage()

    fun resolve(context: Context): String = when (this) {
        is Resource -> {
            val formatArgs = args.map { arg ->
                if (arg is UiMessage) arg.resolve(context) else arg
            }.toTypedArray()
            context.getString(resId, *formatArgs)
        }
        is Raw -> text
    }
}

@Composable
@ReadOnlyComposable
fun UiMessage.resolve(): String = resolve(LocalContext.current)
