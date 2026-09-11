package com.pixijoy.ai.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pixijoy.ai.BuildConfig
import com.pixijoy.ai.domain.model.PhotoTool
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.BoardRouteMap
import com.pixijoy.ai.ui.components.AdaptiveContent
import com.pixijoy.ai.ui.components.OfflineBanner
import com.pixijoy.ai.ui.components.SlowNetworkBanner
import com.pixijoy.ai.ui.legal.AiConsentDialog
import com.pixijoy.ai.ui.legal.PrivacyPolicyScreen
import com.pixijoy.ai.ui.legal.TermsOfServiceScreen
import com.pixijoy.ai.ui.navigation.AppDestination
import com.pixijoy.ai.ui.settings.SettingsScreen
import com.pixijoy.ai.ui.theme.PixiJoyTheme

@Composable
fun PixiJoyAppRoot(
    viewModel: MainViewModel = hiltViewModel(),
    pendingBoardScreenId: String? = null,
    onPendingBoardScreenConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.adMessageRes) {
        uiState.adMessageRes?.let { resId ->
            snackbar.showSnackbar(context.getString(resId))
            viewModel.clearAdMessage()
        }
    }

    if (!uiState.prefsReady) {
        PixiJoyTheme {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val startDestination = when {
        !uiState.privacyAccepted -> AppDestination.Privacy.route
        uiState.needsWhatsNew || !uiState.onboardingComplete -> AppDestination.Onboarding.route
        else -> AppDestination.Home.route
    }

    fun openBoard(screenId: String) {
        navController.navigate(AppDestination.BoardScreen.createRoute(screenId))
    }

    fun redirectToBoard(fromRoute: String, screenId: String) {
        navController.navigate(AppDestination.BoardScreen.createRoute(screenId)) {
            popUpTo(fromRoute) { inclusive = true }
            launchSingleTop = true
        }
    }

    val boardPipeline = com.pixijoy.ai.ui.board.BoardPipelineCallbacks(
        openBeautiful = { openBoard("b5_choose") },
        openFunny = { openBoard("b6_choose") },
        openDifferent = { openBoard("b_diff_choose") },
        openMove = { openBoard("b14_input") },
        openTell = { openBoard("b7_sheet") },
        openStickers = { openBoard("b13_photo") },
        openCollage = { openBoard("b16_select") },
        openExport = { openBoard("b17_open") },
        openMediaInput = { openBoard("b3_open") },
        openPro = { openBoard("b10_discover") },
        openShare = { openBoard("b8_share") },
        openCreations = { openBoard("b18_browse") }
    )

    // adb / QA: --es board_screen <id> — seed gates then open board host.
    var boardCaptureTarget by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(pendingBoardScreenId) {
        val screenId = pendingBoardScreenId ?: return@LaunchedEffect
        boardCaptureTarget = screenId
        viewModel.prepareBoardReviewCapture()
        onPendingBoardScreenConsumed()
    }
    LaunchedEffect(boardCaptureTarget, uiState.privacyAccepted, uiState.onboardingComplete, uiState.showAiConsentDialog) {
        val screenId = boardCaptureTarget ?: return@LaunchedEffect
        if (!uiState.privacyAccepted || !uiState.onboardingComplete) return@LaunchedEffect
        if (uiState.showAiConsentDialog) {
            viewModel.acceptAiConsent()
            return@LaunchedEffect
        }
        runCatching {
            navController.navigate(AppDestination.BoardScreen.createRoute(screenId)) {
                launchSingleTop = true
            }
        }
        boardCaptureTarget = null
    }

    PixiJoyTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!uiState.isOnline) {
                OfflineBanner()
            } else if (uiState.isSlowNetwork) {
                SlowNetworkBanner()
            }

            SnackbarHost(hostState = snackbar)

            AdaptiveContent(modifier = Modifier.fillMaxSize()) {
                // Re-sync gate destination if prefs change after first composition (tests / consent).
                LaunchedEffect(uiState.privacyAccepted, uiState.onboardingComplete, uiState.needsWhatsNew) {
                    val target = when {
                        !uiState.privacyAccepted -> AppDestination.Privacy.route
                        uiState.needsWhatsNew || !uiState.onboardingComplete -> AppDestination.Onboarding.route
                        else -> AppDestination.Home.route
                    }
                    val current = navController.currentBackStackEntry?.destination?.route
                    val gateRoutes = setOf(
                        AppDestination.Privacy.route,
                        AppDestination.Onboarding.route,
                        AppDestination.Home.route
                    )
                    if (current in gateRoutes && current != target) {
                        runCatching {
                            navController.navigate(target) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(AppDestination.Settings.route) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenPrivacy = { navController.navigate(AppDestination.Privacy.route) },
                            onOpenTerms = { navController.navigate(AppDestination.Terms.route) },
                            onOpenRoadmap = { navController.navigate(AppDestination.BoardUiIndex.route) },
                            onOpenFlowIndex = { navController.navigate(AppDestination.FlowIndex.route) },
                            onOpenA11y = { openBoard("b29_home") },
                            onOpenResponsiveQa = {
                                navController.navigate(AppDestination.ResponsiveQa.route)
                            }
                        )
                    }
                    if (BuildConfig.DEBUG) {
                        composable(AppDestination.FlowIndex.route) {
                            com.pixijoy.ai.ui.flow.FlowIndexScreen(
                                onBack = { navController.popBackStack() },
                                onOpenResponsiveQa = {
                                    navController.navigate(AppDestination.ResponsiveQa.route)
                                },
                                onOpenBoardUi = {
                                    navController.navigate(AppDestination.BoardUiIndex.route)
                                }
                            )
                        }
                        composable(AppDestination.BoardUiIndex.route) {
                            com.pixijoy.ai.ui.board.BoardUiIndexScreen(
                                onBack = { navController.popBackStack() },
                                onOpenScreen = { entry ->
                                    navController.navigate(AppDestination.BoardScreen.createRoute(entry.id))
                                }
                            )
                        }
                        composable(AppDestination.ResponsiveQa.route) {
                            com.pixijoy.ai.ui.flow.qa.ResponsiveQaScreen(
                                onBack = { navController.popBackStack() },
                                onOpenFlowIndex = { navController.navigate(AppDestination.FlowIndex.route) },
                                onOpenHome = {
                                    navController.navigate(AppDestination.Home.route) {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenCommunity = { openBoard("b27_feed") },
                                onOpenCreations = { openBoard("b18_browse") }
                            )
                        }
                    }
                    composable(
                        route = AppDestination.BoardScreen.route,
                        arguments = listOf(
                            navArgument(AppDestination.BoardScreen.SCREEN_ARG) {
                                type = NavType.StringType
                                defaultValue = "b1_splash"
                            }
                        )
                    ) { entry ->
                        val screenId = entry.arguments?.getString(AppDestination.BoardScreen.SCREEN_ARG)
                            ?: "b1_splash"
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.pixijoy.ai.ui.board.LocalBoardPipeline provides boardPipeline
                        ) {
                            com.pixijoy.ai.ui.board.BoardScreenHost(
                                screenId = screenId,
                                onBack = { navController.popBackStack() },
                                onNavigate = { nextId ->
                                    // Processing must not remain under Result (Back would re-run AI).
                                    // Cancel uses onBack/pop; SoftEdge gates may still push.
                                    val leavingBusy = screenId.contains("_processing") ||
                                        screenId.contains("_generating")
                                    val goingSoftEdge = nextId.contains("_offline") ||
                                        nextId.contains("_low_credits") ||
                                        nextId.contains("_safety")
                                    val route = AppDestination.BoardScreen.createRoute(nextId)
                                    when {
                                        leavingBusy && !goingSoftEdge -> {
                                            navController.navigate(route) {
                                                popUpTo(
                                                    AppDestination.BoardScreen.createRoute(screenId)
                                                ) { inclusive = true }
                                            }
                                        }
                                        else -> {
                                            if (nextId != screenId) {
                                                navController.navigate(route)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    composable(AppDestination.V2Hub.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.V2Hub.route, "b_create_hub")
                        }
                    }
                    composable(AppDestination.Templates.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.Templates.route, "b2_search")
                        }
                    }
                    composable(AppDestination.AiReels.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.AiReels.route, "b14_input")
                        }
                    }
                    composable(AppDestination.VideoEdit.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.VideoEdit.route, "b14_input")
                        }
                    }
                    composable(AppDestination.Avatar.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.Avatar.route, "b_c09")
                        }
                    }
                    composable(AppDestination.TalkingPhoto.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(
                                AppDestination.TalkingPhoto.route,
                                BoardRouteMap.createExtra("talking")
                            )
                        }
                    }
                    composable(AppDestination.PortraitStudio.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(
                                AppDestination.PortraitStudio.route,
                                BoardRouteMap.createExtra("portrait")
                            )
                        }
                    }
                    composable(AppDestination.VoiceStudio.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.VoiceStudio.route, "b14_input")
                        }
                    }
                    composable(AppDestination.MusicStudio.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.MusicStudio.route, "b14_input")
                        }
                    }
                    composable(AppDestination.Community.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.Community.route, "b27_feed")
                        }
                    }
                    composable(AppDestination.CreativeAgent.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.CreativeAgent.route, "b7_sheet")
                        }
                    }
                    composable(AppDestination.AdvancedEditor.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.AdvancedEditor.route, "b15_open")
                        }
                    }
                    composable(AppDestination.Variations.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.Variations.route, "b_diff_choose")
                        }
                    }
                    composable(AppDestination.Terms.route) {
                        TermsOfServiceScreen(onBack = { navController.popBackStack() })
                    }
                    composable(AppDestination.Privacy.route) {
                        PrivacyPolicyScreen(
                            onBack = { navController.popBackStack() },
                            showConsentActions = !uiState.privacyAccepted,
                            showBackNavigation = uiState.privacyAccepted,
                            onAccept = {
                                viewModel.acceptPrivacy()
                                navController.navigate(
                                    if (uiState.onboardingComplete) AppDestination.Home.route
                                    else AppDestination.Onboarding.route
                                ) {
                                    popUpTo(AppDestination.Privacy.route) { inclusive = true }
                                }
                            },
                            onDeleteData = viewModel::deleteUserData
                        )
                    }
                    composable(AppDestination.Onboarding.route) {
                        // Initialize once from entry id. Do NOT key rememberSaveable on
                        // onboardingEntryScreenId — splash marks prior launch and would
                        // flip entry to b1_returning mid-flow, wiping welcome/features.
                        var onboardStack by rememberSaveable {
                            mutableStateOf(listOf(uiState.onboardingEntryScreenId))
                        }
                        val onboardId = onboardStack.last()
                        val canPopOnboard = onboardStack.size > 1
                        val onboardScope = rememberCoroutineScope()
                        androidx.activity.compose.BackHandler(enabled = canPopOnboard) {
                            onboardStack = onboardStack.dropLast(1)
                        }
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.pixijoy.ai.ui.board.LocalBoardOnboardingActions provides
                                com.pixijoy.ai.ui.board.BoardOnboardingActions(
                                    markPriorLaunch = viewModel::markPriorLaunch,
                                    continueAsGuest = { onDone ->
                                        viewModel.continueAsGuest(onDone)
                                    },
                                    dismissWhatsNew = viewModel::dismissWhatsNew
                                ),
                            com.pixijoy.ai.ui.board.LocalBoardCanPop provides canPopOnboard
                        ) {
                            com.pixijoy.ai.ui.board.BoardScreenHost(
                                screenId = onboardId,
                                onBack = {
                                    if (canPopOnboard) {
                                        onboardStack = onboardStack.dropLast(1)
                                    }
                                    // At first onboard step: do not skip to Home — system back
                                    // is disabled (BackHandler enabled=false) so Activity finishes.
                                },
                                onNavigate = { next ->
                                    val finishToHome = next == "b2_home" ||
                                        next == BoardProductionTabs.HOME
                                    val finishIntoCreate = next.startsWith("b5_") ||
                                        next.startsWith("b6_") ||
                                        next.startsWith("b7_") ||
                                        next.startsWith("b_diff") ||
                                        next.startsWith("b13_") ||
                                        next.startsWith("b14_")
                                    when {
                                        finishToHome -> {
                                            onboardScope.launch {
                                                viewModel.dismissWhatsNew()
                                                viewModel.completeOnboardingAwait()
                                                navController.navigate(AppDestination.Home.route) {
                                                    popUpTo(AppDestination.Onboarding.route) {
                                                        inclusive = true
                                                    }
                                                }
                                            }
                                        }
                                        finishIntoCreate -> {
                                            onboardScope.launch {
                                                viewModel.dismissWhatsNew()
                                                viewModel.completeOnboardingAwait()
                                                navController.navigate(AppDestination.Home.route) {
                                                    popUpTo(AppDestination.Onboarding.route) {
                                                        inclusive = true
                                                    }
                                                }
                                                navController.navigate(
                                                    AppDestination.BoardScreen.createRoute(next)
                                                )
                                            }
                                        }
                                        else -> {
                                            val existing = onboardStack.indexOf(next)
                                            onboardStack = if (existing >= 0) {
                                                onboardStack.take(existing + 1)
                                            } else {
                                                onboardStack + next
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                    composable(AppDestination.Home.route) {
                        LaunchedEffect(uiState.onboardingComplete, uiState.stats.lastSeenVersionCode) {
                            if (uiState.onboardingComplete && uiState.stats.lastSeenVersionCode == 0) {
                                viewModel.dismissWhatsNew()
                            }
                        }
                        MainScaffold(
                            stats = uiState.stats,
                            onOpenBoardScreen = { screenId ->
                                navController.navigate(AppDestination.BoardScreen.createRoute(screenId))
                            },
                            boardPipeline = com.pixijoy.ai.ui.board.BoardPipelineCallbacks(
                                openBeautiful = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b5_choose"))
                                },
                                openFunny = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b6_choose"))
                                },
                                openDifferent = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b_diff_choose"))
                                },
                                openMove = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b14_input"))
                                },
                                openTell = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b7_sheet"))
                                },
                                openStickers = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b13_photo"))
                                },
                                openCollage = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b16_select"))
                                },
                                openExport = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b17_open"))
                                },
                                openMediaInput = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b3_open"))
                                },
                                openPro = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b10_discover"))
                                },
                                openShare = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b8_share"))
                                },
                                openCreations = {
                                    navController.navigate(AppDestination.BoardScreen.createRoute("b18_browse"))
                                }
                            )
                        )
                    }
                    composable(AppDestination.CreateForMe.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.CreateForMe.route, "b5_choose")
                        }
                    }
                    composable(AppDestination.MemeGenerator.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.MemeGenerator.route, "b6_choose")
                        }
                    }
                    composable(AppDestination.StickerCreate.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.StickerCreate.route, "b13_photo")
                        }
                    }
                    composable(AppDestination.AiResult.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.AiResult.route, "b19_success")
                        }
                    }
                    composable(AppDestination.Pro.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.Pro.route, "b10_discover")
                        }
                    }
                    composable(AppDestination.PhotoEdit.route) {
                        LaunchedEffect(Unit) {
                            redirectToBoard(AppDestination.PhotoEdit.route, "b15_open")
                        }
                    }
                    composable(
                        route = AppDestination.Share.route,
                        arguments = listOf(navArgument(AppDestination.Share.URI_ARG) { type = NavType.StringType })
                    ) { entry ->
                        LaunchedEffect(Unit) {
                            navController.navigate(AppDestination.BoardScreen.createRoute("b8_share")) {
                                popUpTo(entry.destination.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    composable(
                        route = AppDestination.Editor.route,
                        arguments = listOf(navArgument(AppDestination.Editor.TOOL_ARG) { type = NavType.StringType })
                    ) { entry ->
                        val toolId = entry.arguments?.getString(AppDestination.Editor.TOOL_ARG)
                        val tool = PhotoTool.fromId(toolId.orEmpty()) ?: PhotoTool.BACKGROUND_REMOVE
                        LaunchedEffect(tool) {
                            navController.navigate(
                                AppDestination.BoardScreen.createRoute(BoardRouteMap.photoTool(tool))
                            ) {
                                popUpTo(entry.destination.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    composable(
                        route = AppDestination.CreativeStudio.route,
                        arguments = listOf(navArgument(AppDestination.CreativeStudio.TOOL_ARG) { type = NavType.StringType })
                    ) { entry ->
                        val toolId = entry.arguments?.getString(AppDestination.CreativeStudio.TOOL_ARG)
                        val tool = PhotoTool.fromId(toolId.orEmpty()) ?: PhotoTool.MEME_MAKER
                        LaunchedEffect(tool) {
                            navController.navigate(
                                AppDestination.BoardScreen.createRoute(BoardRouteMap.photoTool(tool))
                            ) {
                                popUpTo(entry.destination.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }

        if (uiState.showAiConsentDialog && boardCaptureTarget == null) {
            AiConsentDialog(
                onAccept = viewModel::acceptAiConsent,
                onDecline = viewModel::declineAiConsent
            )
        }
    }
}
