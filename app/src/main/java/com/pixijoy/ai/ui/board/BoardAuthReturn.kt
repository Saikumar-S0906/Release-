package com.pixijoy.ai.ui.board

/**
 * Guest wall / auth interrupt return target.
 * Set [pendingScreenId] before navigating to `b22_*`; consume after auth success.
 */
object BoardAuthReturn {
    private val lock = Any()

    @Volatile
    private var pending: String? = null

    var pendingScreenId: String?
        get() = synchronized(lock) { pending }
        set(value) {
            synchronized(lock) { pending = value }
        }

    fun remember(screenId: String?) {
        val clean = screenId?.takeIf { it.isNotBlank() && !it.startsWith("b22_") }
        synchronized(lock) { pending = clean }
    }

    fun consume(): String? = synchronized(lock) {
        val next = pending
        pending = null
        next
    }
}
