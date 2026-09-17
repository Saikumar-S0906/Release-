package com.pixijoy.ai.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.core.data.StudioPrefs
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiCardShape
import com.pixijoy.designsystem.PixiPink
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiSurface2
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Gate { Splash, Privacy, Shell }

private enum class StudioTab { Home, Studio, Library }

@Composable
fun StudioApp() {
    val context = LocalContext.current
    val prefs = remember { StudioPrefs(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var gate by remember { mutableStateOf(Gate.Splash) }

    LaunchedEffect(Unit) {
        delay(1400)
        val accepted = prefs.privacyAccepted.first()
        gate = if (accepted) Gate.Shell else Gate.Privacy
    }

    when (gate) {
        Gate.Splash -> StudioSplash()
        Gate.Privacy -> StudioPrivacy(
            onAccept = {
                scope.launch {
                    prefs.acceptPrivacy()
                    gate = Gate.Shell
                }
            }
        )
        Gate.Shell -> StudioShell()
    }
}

@Composable
private fun StudioSplash() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("studio_splash")
            .background(
                Brush.verticalGradient(
                    listOf(PixiBg, Color(0xFF2E1065), PixiAccent.copy(alpha = 0.55f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.board_logo_pixijoy),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(128.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "PixiJoy",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.studio_tagline),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(3.dp),
                color = PixiAccent,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun StudioPrivacy(onAccept: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.board_logo_pixijoy),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Text(
            stringResource(R.string.studio_privacy_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            stringResource(R.string.studio_privacy_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            stringResource(R.string.studio_promise),
            style = MaterialTheme.typography.bodyMedium,
            color = PixiPink
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("privacy_accept"),
            colors = ButtonDefaults.buttonColors(containerColor = PixiAccent),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(stringResource(R.string.studio_accept), fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun StudioShell() {
    var tab by rememberSaveable { mutableStateOf(StudioTab.Home) }
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            StudioTab.entries.forEach { dest ->
                item(
                    selected = tab == dest,
                    onClick = { tab = dest },
                    icon = {
                        Icon(
                            painter = painterResource(dest.icon),
                            contentDescription = stringResource(dest.label)
                        )
                    },
                    label = { Text(stringResource(dest.label)) },
                    modifier = Modifier.testTag(dest.tag)
                )
            }
        }
    ) {
        when (tab) {
            StudioTab.Home -> HomePane(onOpenStudio = { tab = StudioTab.Studio })
            StudioTab.Studio -> StudioPane()
            StudioTab.Library -> LibraryPane()
        }
    }
}

private val StudioTab.label: Int
    get() = when (this) {
        StudioTab.Home -> R.string.nav_home
        StudioTab.Studio -> R.string.nav_studio
        StudioTab.Library -> R.string.nav_library
    }

private val StudioTab.icon: Int
    get() = when (this) {
        StudioTab.Home -> R.drawable.pj_ic_home
        StudioTab.Studio -> R.drawable.pj_ic_add
        StudioTab.Library -> R.drawable.pj_ic_folder
    }

private val StudioTab.tag: String
    get() = when (this) {
        StudioTab.Home -> "nav_home"
        StudioTab.Studio -> "nav_create"
        StudioTab.Library -> "nav_my_stuff"
    }

@Composable
private fun HomePane(onOpenStudio: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.board_logo_pixijoy),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Text(
            "PixiJoy",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            stringResource(R.string.studio_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.75f)
        )
        Text(
            stringResource(R.string.studio_promise),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.55f)
        )
        Button(
            onClick = onOpenStudio,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("home_open_studio"),
            colors = ButtonDefaults.buttonColors(containerColor = PixiAccent),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text(stringResource(R.string.studio_open), fontWeight = FontWeight.SemiBold)
        }
        Text(
            stringResource(R.string.studio_recents_empty),
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private data class StudioTool(
    val title: Int,
    val subtitle: Int,
    val icon: Int,
    val tag: String
)

@Composable
private fun StudioPane() {
    var photo by rememberSaveable { mutableStateOf(true) }
    val photoTools = listOf(
        StudioTool(R.string.tool_editor, R.string.tool_editor_sub, R.drawable.pj_ic_edit, "create_hub_editor"),
        StudioTool(R.string.tool_restore, R.string.tool_restore_sub, R.drawable.pj_ic_heal, "create_hub_restore"),
        StudioTool(R.string.tool_expand, R.string.tool_expand_sub, R.drawable.pj_ic_expand, "create_hub_expand"),
        StudioTool(R.string.tool_cutout, R.string.tool_cutout_sub, R.drawable.pj_ic_cut, "create_hub_remove_bg"),
        StudioTool(R.string.tool_stickers, R.string.tool_stickers_sub, R.drawable.pj_ic_sticker, "create_hub_sticker"),
        StudioTool(R.string.tool_collage, R.string.tool_collage_sub, R.drawable.pj_ic_grid, "create_hub_collage")
    )
    val movieTools = listOf(
        StudioTool(R.string.tool_move, R.string.tool_move_sub, R.drawable.pj_ic_movie, "create_hub_move"),
        StudioTool(R.string.tool_talking, R.string.tool_talking_sub, R.drawable.pj_ic_gif, "create_hub_talking")
    )
    val tools = if (photo) photoTools else movieTools
    val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 3 else 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            stringResource(R.string.nav_studio),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            stringResource(R.string.studio_hub_sub),
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Segment(
                label = stringResource(R.string.studio_photo),
                selected = photo,
                tag = "create_hub_segment_tools",
                onClick = { photo = true }
            )
            Segment(
                label = stringResource(R.string.studio_movie),
                selected = !photo,
                tag = "create_hub_segment_movie",
                onClick = { photo = false }
            )
        }
        Text(
            stringResource(if (photo) R.string.studio_photo_caption else R.string.studio_movie_caption),
            color = Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tools, key = { it.tag }) { tool ->
                ToolTile(tool)
            }
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) PixiAccent else PixiSurface2)
            .clickable(onClick = onClick)
            .testTag(tag)
            .semantics { role = Role.Tab }
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun ToolTile(tool: StudioTool) {
    Column(
        modifier = Modifier
            .clip(PixiCardShape)
            .background(PixiSurface)
            .testTag(tool.tag)
            .semantics { role = Role.Button }
            .padding(16.dp)
            .height(120.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(tool.icon),
            contentDescription = null,
            tint = PixiAccent,
            modifier = Modifier.size(28.dp)
        )
        Column {
            Text(
                stringResource(tool.title),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(tool.subtitle),
                color = Color.White.copy(alpha = 0.55f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun LibraryPane() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.pj_ic_folder),
                contentDescription = null,
                tint = PixiAccent,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.nav_library),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                stringResource(R.string.studio_library_empty),
                color = Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
