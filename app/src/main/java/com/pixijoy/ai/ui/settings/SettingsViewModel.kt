package com.pixijoy.ai.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixijoy.ai.data.ConsentRepository
import com.pixijoy.ai.data.UserPreferencesRepository
import com.pixijoy.ai.qa.QaReportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val aiCloudConsent: Boolean = false,
    val analyticsConsent: Boolean = false,
    val notificationsEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val consentRepository: ConsentRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val qaReportManager: QaReportManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        consentRepository.aiCloudConsent,
        consentRepository.analyticsConsent,
        consentRepository.notificationsEnabled
    ) { ai, analytics, notifications ->
        SettingsUiState(
            aiCloudConsent = ai,
            analyticsConsent = analytics,
            notificationsEnabled = notifications
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setAiConsent(enabled: Boolean) {
        viewModelScope.launch { consentRepository.setAiCloudConsent(enabled) }
    }

    fun setAnalyticsConsent(enabled: Boolean) {
        viewModelScope.launch { consentRepository.setAnalyticsConsent(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { consentRepository.setNotificationsEnabled(enabled) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            consentRepository.deleteAllData()
            preferencesRepository.setProStatus(false)
        }
    }

    fun exportBugReport(onReady: (android.content.Intent?) -> Unit) {
        viewModelScope.launch {
            qaReportManager.log("User exported bug report from Settings")
            onReady(qaReportManager.createShareIntent())
        }
    }
}
