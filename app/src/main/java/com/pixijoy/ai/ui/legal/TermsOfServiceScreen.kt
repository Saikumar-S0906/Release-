package com.pixijoy.ai.ui.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Terms — Design Bible dark canvas via [MaterialTheme]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.terms_title), color = colors.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.pj_ic_back),
                            contentDescription = stringResource(R.string.cd_back),
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
                .padding(PixiSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.lg)
        ) {
            Text(
                stringResource(R.string.terms_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground
            )
            TermsSection(
                title = stringResource(R.string.terms_age_title),
                body = stringResource(R.string.terms_age_desc)
            )
            TermsSection(
                title = stringResource(R.string.terms_content_title),
                body = stringResource(R.string.terms_content_desc)
            )
            TermsSection(
                title = stringResource(R.string.terms_usage_title),
                body = stringResource(R.string.terms_usage_desc)
            )
            TermsSection(
                title = stringResource(R.string.terms_subscription_title),
                body = stringResource(R.string.terms_subscription_desc)
            )
            Text(
                stringResource(R.string.terms_url),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TermsSection(title: String, body: String) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.xs)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground
        )
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}
