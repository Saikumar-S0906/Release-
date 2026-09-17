package com.pixijoy.core.common

/** Domain result. UI maps this to state; engines never throw to Compose. */
sealed class AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>()
    data class Err(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
}
