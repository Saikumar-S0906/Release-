package com.pixijoy.ai.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.ai.R
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import androidx.compose.foundation.background

/** CODING_PHASES P0 — find Grade / Track / CG without hunting the rail. */
@Composable
fun MovieToolSearchField(
    onJump: (ToolJump) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val hits = MovieToolSearch.find(query)
    val placeholder = stringResource(R.string.studio_tool_search_hint)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            cursorBrush = SolidColor(PixiText),
            textStyle = androidx.compose.ui.text.TextStyle(color = PixiText, fontSize = 14.sp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PixiBg.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("movie_tool_search")
                .semantics { contentDescription = placeholder },
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(placeholder, color = PixiTextMuted, fontSize = 14.sp)
                }
                inner()
            }
        )
        hits.forEach { jump ->
            Text(
                jump.label,
                color = PixiText,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        onJump(jump)
                        query = ""
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("movie_tool_jump_${jump.label.lowercase()}")
                    .semantics { role = Role.Button }
            )
        }
    }
}
