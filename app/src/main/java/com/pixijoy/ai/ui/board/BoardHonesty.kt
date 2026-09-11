package com.pixijoy.ai.ui.board

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.pixijoy.ai.R
import com.pixijoy.ai.data.ConsentRepository
import com.pixijoy.ai.di.BoardConsentEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

fun Context.boardConsentRepository(): ConsentRepository =
    EntryPointAccessors
        .fromApplication(applicationContext, BoardConsentEntryPoint::class.java)
        .consentRepository()

fun Context.toastSavedOnDevice() {
    Toast.makeText(this, getString(R.string.board_saved_to_album), Toast.LENGTH_SHORT).show()
}

fun Context.toastBoardSaveResult(ok: Boolean, failRes: Int = R.string.board_c04_save_failed) {
    Toast.makeText(
        this,
        getString(if (ok) R.string.board_saved_to_album else failRes),
        Toast.LENGTH_SHORT
    ).show()
}

fun Context.toastBillingPlaySoon() {
    Toast.makeText(this, getString(R.string.board_billing_play_soon), Toast.LENGTH_LONG).show()
}

fun Context.toastOauthPreview() {
    Toast.makeText(this, getString(R.string.board_oauth_preview), Toast.LENGTH_SHORT).show()
}

/** Opens Google Play subscription management (real deep-link). */
fun Context.openPlaySubscriptions() {
    val uri = Uri.parse("https://play.google.com/store/account/subscriptions")
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(this, getString(R.string.board_billing_play_soon), Toast.LENGTH_SHORT).show()
    }
}

suspend fun ConsentRepository.snapshot(): ConsentSnapshot = ConsentSnapshot(
    aiCloud = aiCloudConsent.first(),
    analytics = analyticsConsent.first(),
    notifications = notificationsEnabled.first()
)

data class ConsentSnapshot(
    val aiCloud: Boolean,
    val analytics: Boolean,
    val notifications: Boolean
)
