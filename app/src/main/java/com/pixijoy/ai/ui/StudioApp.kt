package com.pixijoy.ai.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.pixijoy.ai.R
import com.pixijoy.ai.export.BatchPhotoExportWorker
import com.pixijoy.ai.ui.camera.CameraCaptureScreen
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
import com.pixijoy.designsystem.pixiMotionMs
import com.pixijoy.movie.Media3MovieEngine
import com.pixijoy.ml.MediaPipeVision
import com.pixijoy.photo.BitmapPhotoEngine
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Gate { Splash, Privacy, Shell }

@Composable
fun StudioApp() {
    val context = LocalContext.current
    val prefs = remember { StudioPrefs(context.applicationContext) }
    val repo = remember { ProjectRepository(context.applicationContext) }
    val vision = remember { MediaPipeVision(context.applicationContext) }
    val engine = remember { BitmapPhotoEngine(vision) }
    val movieEngine = remember { Media3MovieEngine() }
    val scope = rememberCoroutineScope()
    var gate by remember { mutableStateOf(Gate.Splash) }
    var tab by rememberSaveable { mutableStateOf(StudioTab.Home) }
    var session by remember { mutableStateOf<OpenSession?>(null) }
    var pending by remember { mutableStateOf<PendingPick?>(null) }
    var showPickSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var cameraRequest by remember { mutableStateOf<PendingPick?>(null) }
    val projects by repo.observeProjects().collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteIds by remember { mutableStateOf(setOf<String>()) }
    var deleteJob by remember { mutableStateOf<Job?>(null) }
    val visibleProjects = remember(projects, pendingDeleteIds) {
        projects.filterNot { it.id in pendingDeleteIds }
    }
    val config = LocalConfiguration.current
    val twoPane = isStudioTwoPane(config.screenWidthDp, config.screenHeightDp)

    fun requestDelete(project: Project) {
        pendingDeleteIds = pendingDeleteIds + project.id
        deleteJob?.cancel()
        deleteJob = scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.studio_library_deleted),
                actionLabel = context.getString(R.string.studio_undo),
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    pendingDeleteIds = pendingDeleteIds - project.id
                }
                SnackbarResult.Dismissed -> {
                    repo.delete(project.id)
                    pendingDeleteIds = pendingDeleteIds - project.id
                }
            }
        }
    }

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
        showCreateSheet = false
        if (uri != null && request != null) {
            scope.launch { openImported(repo, prefs, uri, request) { session = it; tab = StudioTab.Studio } }
        }
    }
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val request = pending
        pending = null
        showPickSheet = false
        showCreateSheet = false
        if (uri != null && request != null) {
            scope.launch { openImported(repo, prefs, uri, request) { session = it; tab = StudioTab.Studio } }
        }
    }
    fun launchGallery(request: PendingPick) {
        pending = request
        showPickSheet = false
        showCreateSheet = false
        val type = if (request.kind == MediaKind.MOVIE) {
            ActivityResultContracts.PickVisualMedia.ImageAndVideo
        } else {
            ActivityResultContracts.PickVisualMedia.ImageOnly
        }
        pickMedia.launch(PickVisualMediaRequest(type))
    }

    fun launchOpenFile(request: PendingPick) {
        pending = request
        showPickSheet = false
        showCreateSheet = false
        pickFile.launch(
            arrayOf(
                "image/*",
                "image/x-adobe-dng",
                "image/dng",
                "*/*"
            )
        )
    }

    fun launchCamera(request: PendingPick) {
        pending = null
        showPickSheet = false
        showCreateSheet = false
        cameraRequest = request
        showCamera = true
    }

    fun launchSample(request: PendingPick) {
        pending = null
        showPickSheet = false
        showCreateSheet = false
        scope.launch {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.board_c00_hero)
                ?: BitmapFactory.decodeResource(context.resources, R.drawable.board_logo_pixijoy)
            if (bitmap == null) return@launch
            when (
                val result = repo.importBitmap(
                    bitmap,
                    context.getString(R.string.studio_sample_title),
                    kind = request.kind
                )
            ) {
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
            val backEnabled = showCamera || showPickSheet || showCreateSheet || session != null
            val motion = pixiMotionMs(emphasized = true)
            PredictiveBackHandler(enabled = backEnabled) { progress ->
                progress.collect { }
                when {
                    showCamera -> {
                        showCamera = false
                        cameraRequest = null
                    }
                    showPickSheet -> {
                        showPickSheet = false
                        pending = null
                    }
                    showCreateSheet -> showCreateSheet = false
                    session != null -> session = null
                }
            }
            AnimatedContent(
                targetState = compactSession,
                transitionSpec = {
                    fadeIn(tween(motion)) togetherWith fadeOut(tween((motion / 2).coerceAtLeast(0)))
                },
                label = "session-pane"
            ) { compact ->
                if (compact && session != null) {
                    SessionHost(
                        session = session!!,
                        projects = projects,
                        repo = repo,
                        engine = engine,
                        movieEngine = movieEngine,
                        onBack = { session = null }
                    )
                } else {
                    StudioShell(
                        tab = tab,
                        onTab = { tab = it },
                        twoPane = twoPane,
                        projects = visibleProjects,
                        session = session,
                        onOpenStudio = { tab = StudioTab.Studio },
                        onCreate = { showCreateSheet = true },
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
                        onRequestDelete = ::requestDelete,
                        onRenameProject = { project, title ->
                            scope.launch { repo.rename(project.id, title) }
                        },
                        snackbarHostState = snackbarHostState,
                        repo = repo,
                        engine = engine,
                        movieEngine = movieEngine
                    )
                }
            }
            val pick = pending
            if (showPickSheet && pick != null) {
                MediaSourceSheet(
                    title = pick.title,
                    onGallery = { launchGallery(pick) },
                    onCamera = { launchCamera(pick) },
                    onSample = { launchSample(pick) },
                    onDismiss = {
                        pending = null
                        showPickSheet = false
                    },
                    isMovie = pick.kind == MediaKind.MOVIE
                )
            }
            if (showCreateSheet) {
                CreateSourceSheet(
                    onPhoto = {
                        launchGallery(
                            PendingPick(
                                "editor",
                                MediaKind.PHOTO,
                                context.getString(R.string.tool_editor)
                            )
                        )
                    },
                    onVideo = {
                        launchGallery(
                            PendingPick(
                                "move",
                                MediaKind.MOVIE,
                                context.getString(R.string.tool_move)
                            )
                        )
                    },
                    onCamera = {
                        launchCamera(
                            PendingPick(
                                "editor",
                                MediaKind.PHOTO,
                                context.getString(R.string.tool_editor)
                            )
                        )
                    },
                    onSample = {
                        launchSample(
                            PendingPick(
                                "editor",
                                MediaKind.PHOTO,
                                context.getString(R.string.tool_editor)
                            )
                        )
                    },
                    onOpenFile = {
                        launchOpenFile(
                            PendingPick(
                                "editor",
                                MediaKind.PHOTO,
                                context.getString(R.string.tool_editor)
                            )
                        )
                    },
                    onDismiss = { showCreateSheet = false }
                )
            }
            if (showCamera) {
                CameraCaptureScreen(
                    initialVideo = cameraRequest?.kind == MediaKind.MOVIE,
                    onStill = { file ->
                        showCamera = false
                        val request = cameraRequest ?: PendingPick(
                            "editor",
                            MediaKind.PHOTO,
                            context.getString(R.string.tool_editor)
                        )
                        cameraRequest = null
                        scope.launch {
                            openCaptured(repo, prefs, file, MediaKind.PHOTO, request) {
                                session = it
                                tab = StudioTab.Studio
                            }
                        }
                    },
                    onVideo = { file ->
                        showCamera = false
                        val request = cameraRequest ?: PendingPick(
                            "move",
                            MediaKind.MOVIE,
                            context.getString(R.string.tool_move)
                        )
                        cameraRequest = null
                        val movieRequest = request.copy(
                            kind = MediaKind.MOVIE,
                            tool = if (request.kind == MediaKind.MOVIE) request.tool else "move"
                        )
                        scope.launch {
                            openCaptured(repo, prefs, file, MediaKind.MOVIE, movieRequest) {
                                session = it
                                tab = StudioTab.Studio
                            }
                        }
                    },
                    onClose = {
                        showCamera = false
                        cameraRequest = null
                    }
                )
            }
        }
    }
}

private suspend fun openCaptured(
    repo: ProjectRepository,
    prefs: StudioPrefs,
    file: File,
    kind: MediaKind,
    request: PendingPick,
    onOpen: (OpenSession) -> Unit
) {
    when (val result = repo.importFile(file, kind, request.title)) {
        is AppResult.Ok -> {
            prefs.setLastProject(result.value.id)
            onOpen(OpenSession(result.value, request.tool))
            file.delete()
        }
        is AppResult.Err -> Unit
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
    movieEngine: Media3MovieEngine,
    onBack: () -> Unit
) {
    val project = projects.firstOrNull { it.id == session.project.id } ?: session.project
    val label = PhotoTools.plus(MovieTools).firstOrNull { it.key == session.tool }?.title
        ?.let { stringResource(it) }
        ?: stringResource(R.string.tool_editor)
    if (session.kind == MediaKind.MOVIE) {
        MovieSessionScreen(project, label, session.tool, repo, movieEngine, onBack)
    } else {
        PhotoSessionScreen(project, label, session.tool, repo, engine, onBack)
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
    onCreate: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onPickTool: (StudioTool) -> Unit,
    onCloseSession: () -> Unit,
    onRequestDelete: (Project) -> Unit,
    onRenameProject: (Project, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    repo: ProjectRepository,
    engine: BitmapPhotoEngine,
    movieEngine: Media3MovieEngine
) {
    val context = LocalContext.current
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
                    onCreate = onCreate,
                    onOpenProject = onOpenProject
                )
                StudioTab.Studio -> {
                    if (twoPane) {
                        StudioTwoPane(onPickTool = onPickTool) {
                            if (session != null) {
                                SessionHost(
                                    session,
                                    projects,
                                    repo,
                                    engine,
                                    movieEngine,
                                    onCloseSession
                                )
                            } else {
                                EmptyCanvas()
                            }
                        }
                    } else {
                        StudioPane(onPickTool = onPickTool)
                    }
                }
                StudioTab.Library -> LibraryPane(
                    projects = projects,
                    onOpenProject = onOpenProject,
                    onRequestDelete = onRequestDelete,
                    onRenameProject = onRenameProject,
                    onCreate = onCreate,
                    onExportAllPhotos = {
                        val ids = projects.filter { it.kind == MediaKind.PHOTO }.map { it.id }
                        if (ids.isEmpty()) return@LibraryPane
                        val request = OneTimeWorkRequestBuilder<BatchPhotoExportWorker>()
                            .setInputData(
                                workDataOf(
                                    BatchPhotoExportWorker.KEY_PROJECTS to ids.toTypedArray(),
                                    BatchPhotoExportWorker.KEY_FORMAT to "jpeg",
                                    BatchPhotoExportWorker.KEY_QUALITY to 92,
                                    BatchPhotoExportWorker.KEY_KEEP_EXIF to false,
                                    BatchPhotoExportWorker.KEY_STRIP_GPS to true,
                                    BatchPhotoExportWorker.KEY_COLOR_INTENT to "srgb"
                                )
                            )
                            .build()
                        WorkManager.getInstance(context)
                            .enqueueUniqueWork(
                                BatchPhotoExportWorker.uniqueName(),
                                ExistingWorkPolicy.REPLACE,
                                request
                            )
                    }
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .testTag("library_delete_snackbar")
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    actionOnNewLine = false
                )
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
