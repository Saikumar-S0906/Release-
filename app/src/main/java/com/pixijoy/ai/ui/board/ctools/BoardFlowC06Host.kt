package com.pixijoy.ai.ui.board.ctools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardGateScreen
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardProcessingHero
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.theme.PixiSpacing
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.ui.board.BoardCToolEngine
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.decodeBoardDrawable
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Slider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardScreenHeader
import kotlinx.coroutines.launch

/** C06 session — soft face-swap prefs (local demo only). */
internal object BoardFlowC06Session {
    var funnyCategory: Int = 0
    var recastGroup: Int = 0
    var faceIndex: Int = 0
    var variationIndex: Int = 0

    var usedSoftFallback: Boolean = false
    @Volatile
    var resultBitmap: android.graphics.Bitmap? = null
        private set

    fun setResult(bitmap: android.graphics.Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }
}

/**
 * C06 — Face Swap & Recast.
 * Soft: simulated recast; consent UI only (no real face swap / network).
 */
@Composable
fun BoardFlowC06Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C06ChooseIntent(
            onStart = { onNavigate("b_c06_photo") },
            onBack = onBack,
            onTab = tab
        )
        "photo" -> C06FunnyCategories(
            onPick = { onNavigate("b_c06_options") },
            onBack = { onNavigate("b_c06_intro") },
            onTab = tab
        )
        "options" -> C06RecastFaces(
            onContinue = { onNavigate("b_c06_consent") },
            onBack = { onNavigate("b_c06_photo") }
        )
        "consent" -> C06Consent(
            onConfirm = { onNavigate("b_c06_processing") },
            onDeny = { onNavigate("b_c06_consent_denied") },
            onBack = { onNavigate("b_c06_options") }
        )
        "consent_denied" -> BoardGateScreen(
            title = stringResource(R.string.board_ct_consent_denied_title),
            body = stringResource(R.string.board_ct_consent_denied_body),
            primary = stringResource(R.string.board_ct_review_consent),
            onPrimary = { onNavigate("b_c06_consent") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        "processing", "adjust" -> C06Processing(
            onDone = { onNavigate("b_c06_result") },
            onOffline = { onNavigate("b_c06_offline") },
            onLowCredits = { onNavigate("b_c06_low_credits") },
            onBack = { onNavigate("b_c06_consent") }
        )
        "result" -> C06Result(
            onShare = { onNavigate("b_c06_share") },
            onRemix = { onNavigate("b_c06_variations") },
            onEdit = { onNavigate("b15_open") },
            onExport = { onNavigate("b17_open") },
            onRemake = { onNavigate("b_c06_consent") },
            onPublish = { onNavigate("b8_publish") },
            onBack = { onNavigate("b_c06_consent") }
        )
        "variations" -> C06Variations(
            onPick = { onNavigate("b_c06_share") },
            onBack = { onNavigate("b_c06_result") }
        )
        "share" -> C06Share(
            onSave = { onNavigate("b8_ready") },
            onShare = { onNavigate("b8_share") },
            onSticker = { onNavigate("b13_photo") },
            onDone = onBack,
            onBack = { onNavigate("b_c06_variations") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c06_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c06_consent") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c06_consent") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c06_photo") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C06ChooseIntent(
            onStart = { onNavigate("b_c06_photo") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C06ChooseIntent(
    onStart: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C06_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c06_title_short),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c06_faces),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_c06_tagline),
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.create_hub_tools_caption),
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            com.pixijoy.ai.ui.board.BoardUseSampleButton(
                sampleRes = R.drawable.board_c06_faces,
                onReady = onStart
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_ct_get_started),
                onClick = onStart
            )
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C06FunnyCategories(
    onPick: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC06Session.funnyCategory) }
    val cats = listOf(
        R.string.board_c06_cat_animal to BoardFlowArt.funnyHero,
        R.string.board_c06_cat_movie to R.drawable.board_c06_faces,
        R.string.board_c06_cat_wfh to R.drawable.board_c09_portrait,
        R.string.board_c06_cat_expect to R.drawable.board_ba_after,
        R.string.board_c06_cat_thug to BoardFlowArt.memeMonday,
        R.string.board_c06_cat_baby to R.drawable.board_c08_people
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C06_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.home_intent_funny),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Image(
                painter = painterResource(R.drawable.board_c06_faces),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            cats.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { (label, art) ->
                        val index = cats.indexOfFirst { it.first == label }
                        val on = selected == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC06Session.funnyCategory = index
                                    onPick()
                                }
                        ) {
                            Image(
                                painter = painterResource(art),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(radii.md)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                stringResource(label),
                                modifier = Modifier.padding(PixiSpacing.sm),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C06RecastFaces(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var group by remember { mutableIntStateOf(BoardFlowC06Session.recastGroup) }
    var face by remember { mutableIntStateOf(BoardFlowC06Session.faceIndex) }
    val groups = listOf(
        R.string.board_c06_recast_movie to listOf(
            R.drawable.board_c06_faces,
            R.drawable.board_c09_portrait
        ),
        R.string.board_c06_recast_baby to listOf(
            R.drawable.board_c08_people,
            BoardFlowArt.profileAvatar,
            R.drawable.board_ba_after
        ),
        R.string.board_c06_recast_cartoon to listOf(
            BoardFlowArt.funnyHero,
            BoardFlowArt.stickerHero,
            R.drawable.board_c00_funny_dog
        )
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C06_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.home_intent_funny),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            groups.forEachIndexed { g, (title, faces) ->
                Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    faces.forEachIndexed { i, art ->
                        val on = group == g && face == i
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .then(if (on) Modifier.border(3.dp, colors.primary, CircleShape) else Modifier)
                                .clickable {
                                    group = g
                                    face = i
                                    BoardFlowC06Session.recastGroup = g
                                    BoardFlowC06Session.faceIndex = i
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C06Consent(onConfirm: () -> Unit, onDeny: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW_C06_HOST)
        ) {
            Image(
                painter = painterResource(R.drawable.board_c06_faces),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.35f
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.45f))
                    .clickable(onClick = onBack)
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(PixiSpacing.xl)
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surface)
                    .padding(PixiSpacing.xl)
                    .clickable(enabled = false) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Text(
                    stringResource(R.string.board_c06_make_meme),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
                Icon(painter = painterResource(R.drawable.pj_ic_sparkles), null, tint = colors.primary, modifier = Modifier.size(40.dp))
                Text(
                    stringResource(R.string.board_c06_consent_prompt),
                    textAlign = TextAlign.Center,
                    color = colors.textSecondary
                )
                BoardTipAllowed()
                BoardPrimaryButton(text = stringResource(R.string.board_c06_confirm), onClick = onConfirm)
                TextButton(onClick = onDeny) {
                    Text(stringResource(R.string.board_ct_consent_disagree), color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun BoardTipAllowed() {
    val colors = BoardTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BoardTheme.radii.md))
            .background(colors.surfaceMuted)
            .padding(PixiSpacing.md)
    ) {
        Text(stringResource(R.string.board_ct_allowed_title), fontWeight = FontWeight.SemiBold, color = colors.success)
        Text(stringResource(R.string.board_ct_allowed_body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(modifier = Modifier.height(PixiSpacing.sm))
        Text(stringResource(R.string.board_ct_not_allowed_title), fontWeight = FontWeight.SemiBold, color = colors.danger)
        Text(stringResource(R.string.board_ct_not_allowed_body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
    }
}

@Composable
private fun C06Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }
    var softNote by remember { mutableStateOf(false) }
    val checks = listOf(
        R.string.board_c06_check_understand,
        R.string.board_c06_check_style,
        R.string.board_c06_check_funny,
        R.string.board_c06_check_variations
    )
    LaunchedEffect(Unit) {
        val app = context.applicationContext
        BoardFlowC06Session.usedSoftFallback = false
        softNote = false
        val pipeline = runCatching {
            EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                .createExtraPipeline()
        }.getOrNull()
        step = 1
        progress = 0.25f
        val user = withContext(Dispatchers.IO) {
            BoardMediaSession.decodeBitmap(app)
                ?: BitmapFactory.decodeResource(app.resources, BoardFlowArt.funnyHero)
        }
        val backdrop = withContext(Dispatchers.IO) {
            BitmapFactory.decodeResource(app.resources, R.drawable.board_c06_faces)
        }
        step = 2
        progress = 0.5f
        if (pipeline != null && user != null && !user.isRecycled) {
            val hasFace = runCatching { pipeline.hasFace(user) }.getOrDefault(false)
            if (!hasFace) {
                softNote = true
                BoardFlowC06Session.usedSoftFallback = true
            }
            step = 3
            progress = 0.75f
            val composite = withContext(Dispatchers.Default) {
                runCatching {
                    if (backdrop != null && !backdrop.isRecycled) {
                        pipeline.faceMemeComposite(user, backdrop)
                    } else {
                        null
                    }
                }.getOrNull()
            }
            if (composite != null) {
                BoardFlowC06Session.setResult(composite)
            } else {
                BoardFlowC06Session.usedSoftFallback = true
                softNote = true
            }
        } else {
            BoardFlowC06Session.usedSoftFallback = true
            softNote = true
        }
        step = 4
        progress = 1f
        delay(250)
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .statusBarsPadding()
                .testTag(TestTags.FLOW_C06_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                Text(
                    stringResource(R.string.board_c06_title_short),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(stringResource(R.string.create_hub_tools_caption), color = Color.White.copy(0.85f))
                Text(stringResource(R.string.board_c06_recasting), color = Color.White.copy(0.85f))
                if (softNote) {
                    Spacer(modifier = Modifier.height(PixiSpacing.sm))
                    Text(
                        stringResource(R.string.ai_status_on_device),
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                checks.forEachIndexed { i, res ->
                    if (i < step) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                        ) {
                            Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(20.dp))
                            Text(stringResource(res), color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(PixiSpacing.md))
                BoardEdgeActions(
                    onOffline = onOffline,
                    onLowCredits = null,
                    darkSurface = true
                )
                // Cancel only — do not recycle result mid-frame from Back.
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.cd_back), color = Color.White.copy(0.8f))
                }
            }
        }
    }
}

@Composable
private fun C06Result(
    onShare: () -> Unit,
    onRemix: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C06_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(PixiSpacing.sm))
                Text(
                    stringResource(R.string.board_c06_meme_ready),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
            ) {
                BoardSafeResultImage(
                    bitmap = BoardFlowC06Session.resultBitmap,
                    fallbackRes = R.drawable.board_c06_faces,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.7f))
                            )
                        )
                        .padding(PixiSpacing.lg)
                ) {
                    Text(
                        stringResource(R.string.board_c06_meme_caption),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (BoardFlowC06Session.usedSoftFallback || BoardFlowC06Session.resultBitmap != null) {
                Text(
                    stringResource(R.string.ai_status_on_device),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            BoardResultFooter(
                onSave = {
                    val out = BoardFlowC06Session.resultBitmap
                    val ok = if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_FaceSwap")
                    } else {
                        BoardDrawableIo.saveToGallery(
                            context,
                            R.drawable.board_c06_faces,
                            filePrefix = "PixiJoy_FaceSwap"
                        )
                    }
                    Toast.makeText(
                        context,
                        if (ok) R.string.board_c04_saved else R.string.board_c04_save_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onShare = onShare,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            BoardOutlinedButton(text = stringResource(R.string.board_c06_remix), onClick = onRemix)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C06Variations(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC06Session.variationIndex) }
    val arts = listOf(
        R.drawable.board_c06_faces,
        BoardFlowArt.funnyHero,
        BoardFlowArt.memeGoodVibes,
        R.drawable.board_ba_after
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C06_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c06_more_variations),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            arts.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, art ->
                        val index = rowIndex * 2 + col
                        val on = selected == index
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC06Session.variationIndex = index
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c04_use_this), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C06Share(
    onSave: () -> Unit,
    onShare: () -> Unit,
    onSticker: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C06_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c06_share_title),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC06Session.resultBitmap,
                fallbackRes = R.drawable.board_c06_faces,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("WA", "IG", "SC", "•••").forEach { label ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted)
                            .clickable(onClick = onShare),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f2_save), onClick = onSave)
            BoardOutlinedButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
            BoardOutlinedButton(text = stringResource(R.string.board_c04_make_sticker), onClick = onSticker)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_heart), null, tint = colors.accentPink, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    stringResource(R.string.board_c06_keep_kind),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.board_ct_done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
