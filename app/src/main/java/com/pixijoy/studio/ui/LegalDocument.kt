package com.pixijoy.studio.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixijoy.studio.R
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import java.nio.charset.StandardCharsets

/** In-app legal text. Neither document is loaded from a website. */
enum class LegalDoc(val asset: String, val titleRes: Int) {
    Privacy("legal/privacy.txt", R.string.studio_privacy_policy),
    Terms("legal/terms.txt", R.string.studio_terms)
}

@Composable
fun LegalDocumentScreen(doc: LegalDoc, onClose: () -> Unit) {
    val context = LocalContext.current
    val body = remember(doc) {
        runCatching {
            context.assets.open(doc.asset).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }.getOrElse { "" }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("legal_document")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClose,
                modifier = Modifier.testTag("legal_back")
            ) {
                Image(
                    painter = painterResource(R.drawable.pj_ic_back),
                    contentDescription = stringResource(R.string.studio_back),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.studio_back),
                    color = PixiText,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                stringResource(doc.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        SelectionContainer {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
                    .testTag("legal_body"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
        }
    }
}
