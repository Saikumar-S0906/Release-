package com.pixijoy.ai.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.ui.session.MovieSessionScreen
import com.pixijoy.ai.ui.session.PhotoSessionScreen
import com.pixijoy.core.common.AppResult
import com.pixijoy.core.data.Project
import com.pixijoy.core.data.ProjectRepository
import com.pixijoy.core.data.StudioPrefs
import com.pixijoy.core.media.MediaKind
import com.pixijoy.designsystem.PixiAccent
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiPink
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiText
import com.pixijoy.photo.BitmapPhotoEngine
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Gate { Splash, Privacy, Shell }

@Composable
fun StudioApp() {
    val context = LocalContext.current
    val prefs = remember { StudioPrefs(context.applicationContext) }
    val repo = remember { ProjectRepository(context.applicationContext) }
    val engine = remember { BitmapPhotoEngine() }
    val scope = rememberCoroutineScope()
    var gate by remember { mutableStateOf(Gate.Splash) }
    var tab by rememberSaveable { mutableStateOf(StudioTab.Home) }
    var session by remember { mutableStateOf<OpenSession?>(null) }
    var pending by remember { mutableStateOf<PendingPick?>(null) }
    var showPickSheet by remember { mutableStateOf(false) }
    var captureUri by remember { mutableStateOf<Uri?>(null) }
    val projects by repo.observeProjects().collectAsState(initial = emptyList())
    val config = LocalConfiguration.current
    val twoPane = isStudioTwoPane(config.screenWidthDp, config.screenHeightDp)

    LaunchedEffect(Unit) {
        delay(1400)
        val accepted = prefs.privacyAccepted.first()
        gate = if (accepted) Gate.Shell else Gate.Privacy
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val request = pending
        pending = null
        showPickSheet = false
        if (uri != null && request != null) {
            scope.launch { openImported(repo, prefs, uri, request) { session = it; tab = StudioTab.Studio } }
        }
    }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = captureUri
        val request = pending
        pending = null
        showPickSheet = false
        if (ok && uri != null && request != null) {
            scope.launch { openImported(repo, prefs, uri, request) { session = it; tab = StudioTab.Studio } }
        }
    }

    fun launchGallery(request: PendingPick) {
        pending = request
        showPickSheet = false
        val type = if (request.kind == MediaKind.MOVIE) {
            ActivityResultContracts.PickVisualMedia.ImageAndVideo
        } else {
            ActivityResultContracts.PickVisualMedia.ImageOnly
        }
        pickMedia.launch(PickVisualMediaRequest(type))
    }

    fun launchCamera(request: PendingPick) {
        pending = request
        showPickSheet = false
        val file = File(context.cacheDir, "capture.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        captureUri = uri
        takePicture.launch(uri)
    }

    fun launchSample(request: PendingPick) {
        pending = null
        showPickSheet = false
        scope.launch {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.board_c00_hero)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.board_logo_pixijoy)
            if (bitmap == null) return@launch
            when (val result = repo.importBitmap(bitmap, context.getString(R.string.studio_sample_title))) {
                is AppResult.Ok -> {
                    if (!bitmap.isRecycled) bitmap.recycle()
                    prefs.setLastProject(result.value.id)
                    session = OpenSession(result.value, request.tool)
                    tab = StudioTab.Studio
                }
                is AppResult.Err -> if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
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
        Gate.Shell -> {
            val compactSession = session != null && !twoPane
            if (compactSession) {
                SessionHost(
                    session = session!!,
                    projects = projects,
                    repo = repo,
                    engine = engine,
                    onBack = { session = null }
                )
            } else {
                StudioShell(
                    tab = tab,
                    onTab = { tab = it },
                    twoPane = twoPane,
                    projects = projects,
                    session = session,
                    onOpenStudio = { tab = StudioTab.Studio },
                    onTakePhoto = {
                        launchCamera(PendingPick("editor", MediaKind.PHOTO, context.getString(R.string.tool_editor)))
                    },
                    onGallery = {
                        launchGallery(PendingPick("editor", MediaKind.PHOTO, context.getString(R.string.tool_editor)))
                    },
                    onSample = {
                        launchSample(PendingPick("editor", MediaKind.PHOTO, context.getString(R.string.tool_editor)))
                    },
                    onOpenProject = { project ->
                        session = OpenSession(
                            project,
                            if (project.kind == MediaKind.MOVIE) "move" else "editor"
                        )
                        tab = StudioTab.Studio
                    },
                    onPickTool = { tool ->
                        pending = PendingPick(
                            tool.key,
                            tool.kind,
                            context.getString(tool.title)
                        )
                        showPickSheet = true
                    },
                    onCloseSession = { session = null },
                    repo = repo,
                    engine = engine
                )
            }
            val pick = pending
            if (showPickSheet && pick != null) {
                MediaSourceSheet(
                    title = pick.title,
                    onGallery = { launchGallery(pick) },
                    onSample = { launchSample(pick) },
                    onDismiss = {
                        pending = null
                        showPickSheet = false
                    }
                )
            }
        }
    }
}

private suspend fun openImported(
    repo: ProjectRepository,
    prefs: StudioPrefs,
    uri: Uri,
    request: PendingPick,
    onOpen: (OpenSession) -> Unit
) {
    when (val result = repo.importUri(uri, request.kind, request.title)) {
        is AppResult.Ok -> {
            prefs.setLastProject(result.value.id)
            onOpen(OpenSession(result.value, request.tool))
        }
        is AppResult.Err -> Unit
    }
}

@Composable
private fun SessionHost(
    session: OpenSession,
    projects: List<Project>,
    repo: ProjectRepository,
    engine: BitmapPhotoEngine,
    onBack: () -> Unit
) {
    val project = projects.firstOrNull { it.id == session.project.id } ?: session.project
    val label = PhotoTools.plus(MovieTools).firstOrNull { it.key == session.tool }?.title
        ?.let { stringResource(it) }
        ?: stringResource(R.string.tool_editor)
    if (session.kind == MediaKind.MOVIE) {
        MovieSessionScreen(project, label, onBack)
    } else {
        PhotoSessionScreen(project, label, repo, engine, onBack)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun StudioShell(
    tab: StudioTab,
    onTab: (StudioTab) -> Unit,
    twoPane: Boolean,
    projects: List<Project>,
    session: OpenSession?,
    onOpenStudio: () -> Unit,
    onTakePhoto: () -> Unit,
    onGallery: () -> Unit,
    onSample: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onPickTool: (StudioTool) -> Unit,
    onCloseSession: () -> Unit,
    repo: ProjectRepository,
    engine: BitmapPhotoEngine
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            StudioTab.entries.forEach { dest ->
                item(
                    selected = tab == dest,
                    onClick = { onTab(dest) },
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
        Box(Modifier.statusBarsPadding()) {
            when (tab) {
                StudioTab.Home -> HomePane(
                    projects = projects,
                    onOpenStudio = onOpenStudio,
                    onTakePhoto = onTakePhoto,
                    onGallery = onGallery,
                    onSample = onSample,
                    onOpenProject = onOpenProject
                )
                StudioTab.Studio -> {
                    if (twoPane) {
                        Row(Modifier.fillMaxSize()) {
                            StudioPane(
                                onPickTool = onPickTool,
                                modifier = Modifier.weight(0.4f)
                            )
                            Box(Modifier.weight(0.6f)) {
                                if (session != null) {
                                    SessionHost(session, projects, repo, engine, onCloseSession)
                                } else {
                                    EmptyCanvas()
                                }
                            }
                        }
                    } else {
                        StudioPane(onPickTool = onPickTool)
                    }
                }
                StudioTab.Library -> LibraryPane(projects, onOpenProject)
            }
        }
    }
}

@Composable
private fun EmptyCanvas() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .testTag("studio_empty_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.studio_empty_canvas),
            color = Color.White.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp)
        )
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
        PixiPrimaryButton(
            text = stringResource(R.string.studio_accept),
            onClick = onAccept,
            modifier = Modifier.testTag("privacy_accept")
        )
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
