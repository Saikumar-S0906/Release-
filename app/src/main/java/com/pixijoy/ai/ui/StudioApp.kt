package com.pixijoy.ai.ui

import android.content.Context
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.TextButton
import android.content.Intent
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.pixijoy.designsystem.PixiAccentMuted
import com.pixijoy.designsystem.PixiBg
import com.pixijoy.designsystem.PixiPink
import com.pixijoy.designsystem.PixiPrimaryButton
import com.pixijoy.designsystem.PixiSurface
import com.pixijoy.designsystem.PixiText
import com.pixijoy.designsystem.PixiTextMuted
import com.pixijoy.designsystem.pixiMotionMs
import com.pixijoy.photo.PhotoPreset
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Gate { Splash, Privacy, Shell }

private const val SAMPLE_MOVIE_ASSET = "sample_movie.mp4"

@Composable
fun StudioApp() {
    val context = LocalContext.current
    val shell = hiltViewModel<StudioShellViewModel>()
    val prefs = shell.prefs
    val repo = shell.repository
    val scope = rememberCoroutineScope()
    var gate by remember { mutableStateOf(Gate.Splash) }
    var tab by rememberSaveable { mutableStateOf(StudioTab.Home) }
    var session by rememberSaveable(stateSaver = OpenSession.Saver) {
        mutableStateOf<OpenSession?>(null)
    }
    var pending by remember { mutableStateOf<PendingPick?>(null) }
    var showPickSheet by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showCamera by remember { mutableStateOf(false) }
    var cameraRequest by remember { mutableStateOf<PendingPick?>(null) }
    val observedProjects by repo.observeProjects().collectAsState(initial = null)
    val projects = observedProjects.orEmpty()
    val lastProjectId by prefs.lastProjectId.collectAsState(initial = null)
    val lastLookPresetId by prefs.lastLookPresetId.collectAsState(initial = null)
    var libraryPresets by remember { mutableStateOf(emptyList<PhotoPreset>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteIds by remember { mutableStateOf(setOf<String>()) }
    var deleteJob by remember { mutableStateOf<Job?>(null) }
    val visibleProjects = remember(projects, pendingDeleteIds) {
        projects.filterNot { it.id in pendingDeleteIds }
    }
    val config = LocalConfiguration.current
    val twoPane = isStudioTwoPane(config.screenWidthDp, config.screenHeightDp)

    fun reportImportFailure(detail: String) {
        scope.launch {
            snackbarHostState.showSnackbar(
                context.getString(R.string.studio_import_failed, detail)
            )
        }
    }

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
    LaunchedEffect(tab, gate) {
        if (gate == Gate.Shell && tab == StudioTab.Library) {
            libraryPresets = repo.listPresetFiles().mapNotNull { file ->
                runCatching { PhotoPreset.decode(file.readText()) }.getOrNull()
            }
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val request = pending
        pending = null
        showPickSheet = false
        showCreateSheet = false
        if (uri != null && request != null) {
            scope.launch {
                openImported(
                    context = context,
                    repo = repo,
                    prefs = prefs,
                    uri = uri,
                    request = request,
                    onOpen = { session = it; tab = StudioTab.Studio },
                    onError = { reportImportFailure(it) }
                )
            }
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
            scope.launch {
                openImported(
                    context = context,
                    repo = repo,
                    prefs = prefs,
                    uri = uri,
                    request = request,
                    onOpen = { session = it; tab = StudioTab.Studio },
                    onError = { reportImportFailure(it) }
                )
            }
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
        runCatching {
            pickMedia.launch(PickVisualMediaRequest(type))
        }.onFailure {
            pending = null
            reportImportFailure(it.localizedMessage ?: context.getString(R.string.studio_import_unknown))
        }
    }

    fun launchOpenFile(request: PendingPick) {
        pending = request
        showPickSheet = false
        showCreateSheet = false
        runCatching {
            pickFile.launch(
                arrayOf(
                    "image/*",
                    "image/x-adobe-dng",
                    "image/dng",
                    "*/*"
                )
            )
        }.onFailure {
            pending = null
            reportImportFailure(it.localizedMessage ?: context.getString(R.string.studio_import_unknown))
        }
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
            val title = newProjectTitle(context, request.kind, sourceName = null, sample = true)
            val result = if (request.kind == MediaKind.MOVIE) {
                importMovieSample(context, repo, title)
            } else {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.board_c00_hero)
                    ?: BitmapFactory.decodeResource(context.resources, R.drawable.board_logo_pixijoy)
                if (bitmap == null) {
                    snackbarHostState.showSnackbar(
                        context.getString(
                            R.string.studio_import_failed,
                            context.getString(R.string.studio_sample_unavailable)
                        )
                    )
                    return@launch
                }
                try {
                    repo.importBitmap(bitmap, title, kind = request.kind)
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
            when (result) {
                is AppResult.Ok -> {
                    prefs.setLastProject(result.value.id)
                    session = OpenSession.fromProject(result.value, request.tool)
                    tab = StudioTab.Studio
                }
                is AppResult.Err -> snackbarHostState.showSnackbar(
                    context.getString(R.string.studio_import_failed, result.message)
                )
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
            val backEnabled = showCamera || showPickSheet || showCreateSheet
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
                        projectsLoaded = observedProjects != null,
                        onBack = { session = null }
                    )
                } else {
                    StudioShell(
                        tab = tab,
                        onTab = { tab = it },
                        twoPane = twoPane,
                        projects = visibleProjects,
                        projectsLoaded = observedProjects != null,
                        continueProject = findContinueProject(visibleProjects, lastProjectId),
                        session = session,
                        onOpenStudio = { tab = StudioTab.Studio },
                        onCreate = { showCreateSheet = true },
                        onOpenProject = { project ->
                            session = OpenSession.fromProject(
                                project,
                                if (project.kind == MediaKind.MOVIE) "move" else "editor"
                            )
                            tab = StudioTab.Studio
                            scope.launch { prefs.setLastProject(project.id) }
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
                        libraryPresets = libraryPresets,
                        lastLookPresetId = lastLookPresetId,
                        onDeleteAllProjects = {
                            scope.launch {
                                deleteJob?.cancel()
                                pendingDeleteIds = emptySet()
                                session = null
                                repo.deleteAll()
                            }
                        },
                        onResetAppData = {
                            scope.launch {
                                deleteJob?.cancel()
                                pendingDeleteIds = emptySet()
                                session = null
                                repo.deleteAll()
                                prefs.clearAll()
                                gate = Gate.Privacy
                            }
                        }
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
                            openCaptured(
                                context = context,
                                repo = repo,
                                prefs = prefs,
                                file = file,
                                kind = MediaKind.PHOTO,
                                request = request,
                                onOpen = {
                                    session = it
                                    tab = StudioTab.Studio
                                },
                                onError = { reportImportFailure(it) }
                            )
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
                            openCaptured(
                                context = context,
                                repo = repo,
                                prefs = prefs,
                                file = file,
                                kind = MediaKind.MOVIE,
                                request = movieRequest,
                                onOpen = {
                                    session = it
                                    tab = StudioTab.Studio
                                },
                                onError = { reportImportFailure(it) }
                            )
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

private fun enqueuePhotoBatch(
    context: Context,
    projects: List<Project>,
    presetBody: String? = null
) {
    val ids = projects.filter { it.kind == MediaKind.PHOTO }.map { it.id }
    if (ids.isEmpty()) return
    val data = androidx.work.Data.Builder()
        .putStringArray(BatchPhotoExportWorker.KEY_PROJECTS, ids.toTypedArray())
        .putString(BatchPhotoExportWorker.KEY_FORMAT, "jpeg")
        .putInt(BatchPhotoExportWorker.KEY_QUALITY, 92)
        .putBoolean(BatchPhotoExportWorker.KEY_KEEP_EXIF, false)
        .putBoolean(BatchPhotoExportWorker.KEY_STRIP_GPS, true)
        .putString(BatchPhotoExportWorker.KEY_COLOR_INTENT, "srgb")
    if (presetBody != null) {
        data.putString(BatchPhotoExportWorker.KEY_PRESET, presetBody)
    }
    val request = OneTimeWorkRequestBuilder<BatchPhotoExportWorker>()
        .setInputData(data.build())
        .build()
    com.pixijoy.ai.export.ExportNotifications.ensureChannel(context)
    WorkManager.getInstance(context).enqueueUniqueWork(
        BatchPhotoExportWorker.uniqueName(),
        ExistingWorkPolicy.REPLACE,
        request
    )
}

private suspend fun importMovieSample(
    context: Context,
    repo: ProjectRepository,
    title: String
): AppResult<Project> = runCatching {
    val sampleFile = File.createTempFile("pixijoy-sample-", ".mp4", context.cacheDir)
    try {
        context.assets.open(SAMPLE_MOVIE_ASSET).use { input ->
            sampleFile.outputStream().use { output -> input.copyTo(output) }
        }
        repo.importFile(sampleFile, MediaKind.MOVIE, title)
    } finally {
        sampleFile.delete()
    }
}.getOrElse { error ->
    AppResult.Err(error.localizedMessage ?: error.javaClass.simpleName, error)
}

internal fun newProjectTitle(
    context: Context,
    kind: MediaKind,
    sourceName: String?,
    sample: Boolean = false
): String {
    val date = android.text.format.DateFormat.format("MMM d", java.util.Date()).toString()
    val kindLabel = when {
        sample -> context.getString(R.string.studio_sample_title)
        kind == MediaKind.MOVIE -> context.getString(R.string.studio_movie)
        else -> context.getString(R.string.studio_photo)
    }
    return ProjectTitle.resolve(sourceName, kindLabel, date)
}

private fun contentDisplayName(context: Context, uri: Uri): String? {
    val queried = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index < 0) null else cursor.getString(index)
        }
    }.getOrNull()
    return queried ?: uri.lastPathSegment
}

private suspend fun openCaptured(
    context: Context,
    repo: ProjectRepository,
    prefs: StudioPrefs,
    file: File,
    kind: MediaKind,
    request: PendingPick,
    onOpen: (OpenSession) -> Unit,
    onError: (String) -> Unit
) {
    val title = newProjectTitle(context, kind, file.name)
    when (val result = repo.importFile(file, kind, title)) {
        is AppResult.Ok -> {
            prefs.setLastProject(result.value.id)
            onOpen(OpenSession.fromProject(result.value, request.tool))
            file.delete()
        }
        is AppResult.Err -> {
            file.delete()
            onError(result.message)
        }
    }
}

private suspend fun openImported(
    context: Context,
    repo: ProjectRepository,
    prefs: StudioPrefs,
    uri: Uri,
    request: PendingPick,
    onOpen: (OpenSession) -> Unit,
    onError: (String) -> Unit
) {
    val title = newProjectTitle(context, request.kind, contentDisplayName(context, uri))
    when (val result = repo.importUri(uri, request.kind, title)) {
        is AppResult.Ok -> {
            prefs.setLastProject(result.value.id)
            onOpen(OpenSession.fromProject(result.value, request.tool))
        }
        is AppResult.Err -> onError(result.message)
    }
}

@Composable
private fun SessionHost(
    session: OpenSession,
    projects: List<Project>,
    projectsLoaded: Boolean,
    onBack: () -> Unit
) {
    val project = when (
        val state = resolveSessionProject(session, if (projectsLoaded) projects else null)
    ) {
        SessionProjectState.Loading -> {
            SessionProjectStatus(
                message = stringResource(R.string.studio_loading),
                tag = "session_loading",
                onBack = onBack
            )
            return
        }
        SessionProjectState.Missing -> {
            SessionProjectStatus(
                message = stringResource(R.string.session_project_missing),
                tag = "session_missing",
                onBack = onBack
            )
            return
        }
        is SessionProjectState.Ready -> state.project
    }
    val label = PhotoTools.plus(MovieTools).firstOrNull { it.key == session.tool }?.title
        ?.let { stringResource(it) }
        ?: stringResource(R.string.tool_editor)
    if (session.kind == MediaKind.MOVIE) {
        MovieSessionScreen(project, label, session.tool, onBack)
    } else {
        PhotoSessionScreen(project, label, session.tool, onBack)
    }
}

internal sealed interface SessionProjectState {
    data object Loading : SessionProjectState
    data object Missing : SessionProjectState
    data class Ready(val project: Project) : SessionProjectState
}

internal fun resolveSessionProject(
    session: OpenSession,
    projects: List<Project>?
): SessionProjectState {
    if (projects == null) return SessionProjectState.Loading
    val project = projects.firstOrNull { it.id == session.projectId }
        ?: return SessionProjectState.Missing
    return if (project.kind == session.kind) {
        SessionProjectState.Ready(project)
    } else {
        SessionProjectState.Missing
    }
}

internal fun findContinueProject(
    projects: List<Project>,
    lastProjectId: String?
): Project? = lastProjectId?.let { id -> projects.firstOrNull { it.id == id } }

@Composable
private fun SessionProjectStatus(
    message: String,
    tag: String,
    onBack: () -> Unit
) {
    PredictiveBackHandler {
        it.collect { }
        onBack()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .padding(24.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = PixiText, textAlign = TextAlign.Center)
        Text(
            text = stringResource(R.string.studio_back),
            color = PixiAccent,
            modifier = Modifier
                .padding(top = 16.dp)
                .clickable(onClick = onBack)
                .testTag("session_back")
                .padding(12.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun StudioShell(
    tab: StudioTab,
    onTab: (StudioTab) -> Unit,
    twoPane: Boolean,
    projects: List<Project>,
    projectsLoaded: Boolean,
    continueProject: Project?,
    session: OpenSession?,
    onOpenStudio: () -> Unit,
    onCreate: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onPickTool: (StudioTool) -> Unit,
    onCloseSession: () -> Unit,
    onRequestDelete: (Project) -> Unit,
    onRenameProject: (Project, String) -> Unit,
    snackbarHostState: SnackbarHostState,
    libraryPresets: List<PhotoPreset> = emptyList(),
    lastLookPresetId: String? = null,
    onDeleteAllProjects: () -> Unit = {},
    onResetAppData: () -> Unit = {}
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
                            contentDescription = stringResource(dest.label),
                            tint = if (tab == dest) PixiAccentMuted else PixiTextMuted
                        )
                    },
                    label = {
                        Text(
                            stringResource(dest.label),
                            color = if (tab == dest) PixiText else PixiTextMuted
                        )
                    },
                    modifier = Modifier.testTag(dest.tag)
                )
            }
        },
        containerColor = PixiBg,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = PixiSurface,
            navigationBarContentColor = PixiTextMuted,
            navigationRailContainerColor = PixiSurface,
            navigationRailContentColor = PixiTextMuted
        )
    ) {
        Box(Modifier.statusBarsPadding()) {
            when (tab) {
                StudioTab.Home -> HomePane(
                    projects = projects,
                    continueProject = continueProject,
                    onOpenStudio = onOpenStudio,
                    onCreate = onCreate,
                    onContinue = onOpenProject,
                    onOpenProject = onOpenProject,
                    onPickTool = onPickTool
                )
                StudioTab.Studio -> {
                    if (twoPane) {
                        StudioTwoPane(onPickTool = onPickTool) {
                            if (session != null) {
                                SessionHost(
                                    session,
                                    projects,
                                    projectsLoaded,
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
                    presets = libraryPresets,
                    lastLookPresetId = lastLookPresetId,
                    onExportAllPhotos = {
                        enqueuePhotoBatch(context, projects)
                    },
                    onExportPhotosWithLook = { presetId ->
                        val body = libraryPresets.firstOrNull { it.id == presetId }?.encode()
                        enqueuePhotoBatch(context, projects, body)
                    },
                    onDeleteAllProjects = onDeleteAllProjects,
                    onResetAppData = onResetAppData
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
    val context = LocalContext.current
    val shortHeight = LocalConfiguration.current.screenHeightDp < 560
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PixiBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (shortHeight) 10.dp else 16.dp)
        ) {
            Spacer(Modifier.height(if (shortHeight) 8.dp else 24.dp))
            Image(
                painter = painterResource(R.drawable.board_logo_pixijoy),
                contentDescription = null,
                modifier = Modifier.size(if (shortHeight) 48.dp else 64.dp)
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { openWebUrl(context, context.getString(R.string.studio_url_privacy)) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("privacy_policy_link")
                ) {
                    Text(stringResource(R.string.studio_privacy_policy), color = PixiAccent)
                }
                TextButton(
                    onClick = { openWebUrl(context, context.getString(R.string.studio_url_terms)) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("privacy_terms_link")
                ) {
                    Text(stringResource(R.string.studio_terms), color = PixiAccent)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        PixiPrimaryButton(
            text = stringResource(R.string.studio_accept),
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("privacy_accept")
        )
    }
}

private fun openWebUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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
