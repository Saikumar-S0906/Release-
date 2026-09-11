package com.pixijoy.ai.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixijoy.ai.BuildConfig
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.launch

/** Settings — Design Bible dark canvas via [MaterialTheme] (Identity #0B0B10). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenRoadmap: () -> Unit = {},
    onOpenFlowIndex: () -> Unit = {},
    onOpenA11y: () -> Unit = {},
    onOpenResponsiveQa: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setNotificationsEnabled(true)
        } else {
            viewModel.setNotificationsEnabled(false)
            scope.launch {
                snackbar.showSnackbar(context.getString(R.string.notification_permission_denied))
            }
        }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.setNotificationsEnabled(false)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.setNotificationsEnabled(true)
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            viewModel.setNotificationsEnabled(true)
        }
    }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        color = colors.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics {
                            contentDescription = context.getString(R.string.cd_back)
                        }
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_back),
                            contentDescription = null,
                            tint = colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            SettingToggle(
                title = stringResource(R.string.settings_ai_consent),
                subtitle = stringResource(R.string.ai_consent_message),
                checked = uiState.aiCloudConsent,
                onCheckedChange = viewModel::setAiConsent
            )
            SettingToggle(
                title = stringResource(R.string.settings_analytics),
                subtitle = stringResource(R.string.settings_analytics_desc),
                checked = uiState.analyticsConsent,
                onCheckedChange = viewModel::setAnalyticsConsent
            )
            SettingToggle(
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(R.string.settings_notifications_desc),
                checked = uiState.notificationsEnabled,
                onCheckedChange = ::onNotificationsToggle
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = PixiSpacing.md),
                color = colors.outline.copy(alpha = 0.4f)
            )

            Text(
                stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onBackground
            )
            Text(
                stringResource(R.string.settings_language_desc),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = PixiSpacing.md),
                color = colors.outline.copy(alpha = 0.4f)
            )

            SettingsLink(stringResource(R.string.open_privacy_policy), onOpenPrivacy)
            SettingsLink(stringResource(R.string.open_terms), onOpenTerms)
            SettingsLink(stringResource(R.string.flow29_title), onOpenA11y)
            if (BuildConfig.DEBUG) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = PixiSpacing.md),
                    color = colors.outline.copy(alpha = 0.4f)
                )
                Text(
                    text = stringResource(R.string.settings_debug_tools),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurfaceVariant
                )
                SettingsLink(stringResource(R.string.open_roadmap), onOpenRoadmap)
                SettingsLink(stringResource(R.string.flow_index_title), onOpenFlowIndex)
                SettingsLink(stringResource(R.string.responsive_qa_title), onOpenResponsiveQa)
            }
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.privacy_policy_url))
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.privacy_policy_url),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = PixiSpacing.md),
                color = colors.outline.copy(alpha = 0.4f)
            )

            SettingsLink(
                text = stringResource(R.string.qa_send_report),
                onClick = {
                    viewModel.exportBugReport { intent ->
                        intent?.let {
                            context.startActivity(
                                Intent.createChooser(it, context.getString(R.string.qa_send_report))
                            )
                        }
                    }
                },
                modifier = Modifier.testTag(TestTags.SETTINGS_BUG_REPORT)
            )

            TextButton(
                onClick = viewModel::deleteAllData,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.delete_my_data),
                    color = colors.error
                )
            }
        }
    }
}

@Composable
private fun SettingsLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(text, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PixiSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = PixiSpacing.md)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onBackground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title }
        )
    }
}
