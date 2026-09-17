package com.pixijoy.billing

/** One-time Play products only. No subscriptions. */
interface OneTimeStore {
    val hasPremiumExport: Boolean
}

object FreeOneTimeStore : OneTimeStore {
    override val hasPremiumExport: Boolean = false
}
