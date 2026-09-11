package com.pixijoy.ai.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.components.BoardDestructiveButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Privacy — Design Bible dark canvas via [MaterialTheme]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    onAccept: () -> Unit,
    onDeleteData: () -> Unit,
    showConsentActions: Boolean = false,
    showBackNavigation: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.privacy_title), color = colors.onBackground)
                },
                navigationIcon = {
                    if (showBackNavigation) {
                        IconButton(onClick = onBack) {
                            Icon(painter = painterResource(R.drawable.pj_ic_back),
                                contentDescription = stringResource(R.string.cd_back),
                                tint = colors.onBackground
                            )
                        }
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
                .padding(PixiSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.lg)
        ) {
            if (showConsentActions) {
                Text(
                    stringResource(R.string.privacy_kid_summary),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground
                )
            }

            Text(
                stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground
            )

            PrivacySection(
                title = stringResource(R.string.privacy_photos_title),
                body = stringResource(R.string.privacy_photos_desc)
            )
            PrivacySection(
                title = stringResource(R.string.privacy_data_title),
                body = stringResource(R.string.privacy_data_desc)
            )
            PrivacySection(
                title = stringResource(R.string.privacy_rights_title),
                body = stringResource(R.string.privacy_rights_desc)
            )

            Text(
                stringResource(R.string.privacy_contact),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(PixiSpacing.sm))

            if (showConsentActions) {
                BoardPrimaryButton(
                    text = stringResource(R.string.accept),
                    onClick = onAccept,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TestTags.PRIVACY_ACCEPT)
                )

                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                BoardDestructiveButton(
                    text = stringResource(R.string.delete_my_data),
                    onClick = onDeleteData,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Spacer(modifier = Modifier.height(PixiSpacing.xs))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}
