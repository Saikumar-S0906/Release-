package com.pixijoy.ai.ui.board.ctools

import androidx.annotation.DrawableRes

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/** C12 session — greeting-card prefs + composited result. */
internal object BoardFlowC12Session {
    var occasionIndex: Int = 1
    var photoIndex: Int = 0
    var templateIndex: Int = 0
    var message: String = ""

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
 * C12 — Greeting Card & Festival.
 * WORKS: composite template + photo + message → session bitmap; Save/Share via MediaStore.
 */
@Composable
fun BoardFlowC12Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    val step = BoardCTool.stepOf(screenId)
    when (step) {
        "intro", "hub", "" -> C12Discover(
            onContinue = { onNavigate("b_c12_options") },
            onBack = onBack,
            onTab = tab
        )
        "options" -> C12PickOccasion(
            onPick = { onNavigate("b_c12_photo") },
            onBack = { onNavigate("b_c12_intro") }
        )
        "photo" -> C12SelectPhoto(
            onPick = { onNavigate("b_c12_adjust") },
            onBack = { onNavigate("b_c12_options") }
        )
        "adjust" -> C12CardTemplates(
            onPick = { onNavigate("b_c12_edit") },
            onBack = { onNavigate("b_c12_photo") }
        )
        "edit" -> C12EditText(
            onContinue = { onNavigate("b_c12_processing") },
            onBack = { onNavigate("b_c12_adjust") }
        )
        "processing" -> C12Processing(
            onDone = { onNavigate("b_c12_result") },
            onOffline = { onNavigate("b_c12_offline") },
            onLowCredits = { onNavigate("b_c12_low_credits") },
            onCancel = { onNavigate("b_c12_edit") }
        )
        "result" -> C12Preview(
            onShare = { onNavigate("b_c12_share") },
            onVariations = { onNavigate("b_c12_variations") },
            onBack = { onNavigate("b_c12_edit") }
        )
        "variations" -> C12CardTemplates(
            onPick = { onNavigate("b_c12_edit") },
            onBack = { onNavigate("b_c12_result") }
        )
        "share" -> C12Share(
            onWhatsApp = { onNavigate("b8_share") },
            onSave = { onNavigate("b8_ready") },
            onDone = onBack,
            onBack = { onNavigate("b_c12_result") }
        )
        "offline" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_offline_title),
            body = stringResource(R.string.board_ct_offline_body),
            primary = stringResource(R.string.board_f3_try_again),
            onPrimary = { onNavigate("b_c12_processing") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c12_edit") }
        )
        "low_credits" -> BoardGateScreen(
            title = stringResource(R.string.board_f19_low_credits_title),
            body = stringResource(R.string.board_f19_low_credits_body),
            primary = stringResource(R.string.board_f19_get_credits),
            onPrimary = { onNavigate("b23_dash") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = { onNavigate("b_c12_edit") }
        )
        "safety" -> BoardGateScreen(
            title = stringResource(R.string.board_f4_safety_title),
            body = stringResource(R.string.board_f4_safety_body),
            primary = stringResource(R.string.board_f3_try_another),
            onPrimary = { onNavigate("b_c12_photo") },
            secondary = stringResource(R.string.cd_back),
            onSecondary = onBack
        )
        else -> C12Discover(
            onContinue = { onNavigate("b_c12_options") },
            onBack = onBack,
            onTab = tab
        )
    }
}

@Composable
private fun C12Discover(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onTab: (BoardShellTab) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scenes = listOf(
        R.drawable.board_c12_card,
        R.drawable.board_c00_funny_dog,
        R.drawable.board_c09_portrait,
        R.drawable.board_c11_talk,
        R.drawable.board_b5_photo,
        R.drawable.board_ba_after
    )
    BoardMainShell(selected = BoardShellTab.Create, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.create_extra_greeting),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        stringResource(R.string.board_c12_slogan),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Text(
                stringResource(R.string.board_c12_discover),
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            scenes.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEach { art ->
                        Image(
                            painter = painterResource(art),
                            contentDescription = null,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .clickable(onClick = onContinue),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c09_get_started), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C12PickOccasion(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC12Session.occasionIndex) }
    val occasions = listOf(
        OccasionItem(R.string.board_ct_opt_birthday, R.drawable.pj_ic_cake, listOf(Color(0xFF7C4DFF), Color(0xFF448AFF))),
        OccasionItem(R.string.board_ct_opt_diwali, R.drawable.pj_ic_sun, listOf(Color(0xFF9C27B0), Color(0xFFFF9800))),
        OccasionItem(R.string.board_ct_opt_christmas, R.drawable.pj_ic_celebrate, listOf(Color(0xFFFF7043), Color(0xFFE53935))),
        OccasionItem(R.string.board_ct_opt_new_year, R.drawable.pj_ic_sparkles, listOf(Color(0xFF00897B), Color(0xFF26C6DA))),
        OccasionItem(R.string.board_c12_holi, R.drawable.pj_ic_heart, listOf(Color(0xFFEC407A), Color(0xFFFFA726)))
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c12_pick_occasion),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_home), contentDescription = stringResource(R.string.nav_home))
                }
            }
            occasions.forEachIndexed { index, item ->
                val on = selected == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.pill))
                        .background(Brush.horizontalGradient(item.colors))
                        .then(if (on) Modifier.border(3.dp, Color.White, RoundedCornerShape(radii.pill)) else Modifier)
                        .clickable {
                            selected = index
                            BoardFlowC12Session.occasionIndex = index
                            BoardFlowC12Session.message = ""
                            onPick()
                        }
                        .padding(horizontal = PixiSpacing.xl, vertical = PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Icon(painter = painterResource(item.icon), null, tint = Color.White, modifier = Modifier.size(28.dp))
                    Text(
                        stringResource(item.title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (on) {
                        Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = Color.White)
                    }
                }
            }
            BoardTrustFooter()
        }
    }
}

private data class OccasionItem(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val colors: List<Color>
)

@Composable
private fun C12SelectPhoto(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC12Session.photoIndex) }
    val photos = listOf(
        R.drawable.board_c12_card,
        R.drawable.board_c00_funny_dog,
        R.drawable.board_c09_portrait,
        R.drawable.board_b5_photo,
        R.drawable.board_c11_talk,
        R.drawable.board_ba_after,
        R.drawable.board_c08_people,
        R.drawable.board_c00_hero,
        R.drawable.board_c06_faces
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c12_select_photo),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            photos.chunked(3).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, art ->
                        val index = rowIndex * 3 + col
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
                                    BoardFlowC12Session.photoIndex = index
                                    onPick()
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_use_this_photo), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C12CardTemplates(onPick: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var selected by remember { mutableIntStateOf(BoardFlowC12Session.templateIndex) }
    val templates = listOf(
        R.drawable.board_c12_card,
        R.drawable.board_c00_funny_dog,
        R.drawable.board_c09_portrait,
        R.drawable.board_ba_after,
        R.drawable.board_c11_talk,
        R.drawable.board_b5_photo
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c12_templates),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            templates.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    row.forEachIndexed { col, art ->
                        val index = rowIndex * 2 + col
                        val on = selected == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(radii.md))
                                .then(if (on) Modifier.border(2.dp, colors.primary, RoundedCornerShape(radii.md)) else Modifier)
                                .clickable {
                                    selected = index
                                    BoardFlowC12Session.templateIndex = index
                                    onPick()
                                }
                        ) {
                            Image(
                                painter = painterResource(art),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(0.45f))
                                    .padding(PixiSpacing.sm)
                            ) {
                                Text(
                                    stringResource(R.string.board_c12_template_label),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_c00_continue), onClick = onPick)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun C12EditText(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val defaultMessage = stringResource(defaultMessageRes(BoardFlowC12Session.occasionIndex))
    var message by remember {
        mutableStateOf(BoardFlowC12Session.message.ifEmpty { "" })
    }
    LaunchedEffect(Unit) {
        if (message.isEmpty()) {
            message = defaultMessage
            BoardFlowC12Session.message = defaultMessage
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_c12_edit_text),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            Text(
                stringResource(R.string.board_c12_template_label),
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Image(
                painter = painterResource(R.drawable.board_c12_card),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            OutlinedTextField(
                value = message,
                onValueChange = {
                    message = it
                    BoardFlowC12Session.message = it
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(radii.md),
                label = { Text(stringResource(R.string.board_c12_message_hint)) }
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f19_generate), onClick = onContinue)
            BoardTrustFooter()
        }
    }
}

@StringRes
private fun defaultMessageRes(occasionIndex: Int): Int = when (occasionIndex) {
    0 -> R.string.board_c12_msg_birthday
    1 -> R.string.board_c12_msg_diwali
    2 -> R.string.board_c12_msg_christmas
    3 -> R.string.board_c12_msg_new_year
    else -> R.string.board_c12_msg_holi
}

@Composable
private fun C12Processing(
    onDone: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = BoardTheme.colors
    var progress by remember { mutableFloatStateOf(0.2f) }
    LaunchedEffect(Unit) {
        progress = 0.55f
        delay(900)
        progress = 0.85f
        delay(900)
        onDone()
    }
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.processingSurface)
                .statusBarsPadding()
                .testTag(TestTags.FLOW_C12_HOST)
                .padding(PixiSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BoardProcessingHero(progress = progress, darkSurface = true)
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                Text(
                    stringResource(R.string.board_c12_processing),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(PixiSpacing.lg))
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel), color = Color.White.copy(0.85f))
                }
            }
        }
    }
}

@Composable
private fun C12Preview(onShare: () -> Unit, onVariations: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestTags.FLOW_C12_HOST)
        ) {
            BoardSafeResultImage(
                bitmap = BoardFlowC12Session.resultBitmap,
                fallbackRes = R.drawable.board_c12_card,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.55f))
                        )
                    )
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(PixiSpacing.md)
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_back),
                    contentDescription = stringResource(R.string.cd_back),
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    BoardFlowC12Session.message.ifEmpty {
                        stringResource(defaultMessageRes(BoardFlowC12Session.occasionIndex))
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                BoardPrimaryButton(text = stringResource(R.string.board_f2_share), onClick = onShare)
                TextButton(onClick = onVariations) {
                    Text(stringResource(R.string.board_f7_another_style), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun C12Share(
    onWhatsApp: () -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    fun persistCard(then: () -> Unit = {}) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val bmp = BoardFlowC12Session.resultBitmap
                if (bmp != null && !bmp.isRecycled) {
                    BoardDrawableIo.saveBitmapToGallery(context, bmp, filePrefix = "PixiJoy_Greeting")
                } else {
                    BoardDrawableIo.saveToGallery(
                        context,
                        R.drawable.board_c12_card,
                        filePrefix = "PixiJoy_Greeting"
                    )
                }
            }
            Toast.makeText(
                context,
                context.getString(
                    if (ok) R.string.board_c04_saved else R.string.board_c04_save_failed
                ),
                Toast.LENGTH_SHORT
            ).show()
            if (ok) then()
        }
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW_C12_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f2_share),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDone) {
                    Icon(painter = painterResource(R.drawable.pj_ic_home), contentDescription = stringResource(R.string.nav_home))
                }
            }
            BoardSafeResultImage(
                bitmap = BoardFlowC12Session.resultBitmap,
                fallbackRes = R.drawable.board_c12_card,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_ct_tip_greeting_share),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f17_wa),
                onClick = {
                    persistCard { onWhatsApp() }
                }
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f12_save),
                onClick = { persistCard { onSave() } }
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_c12_print_size),
                onClick = { persistCard { onSave() } }
            )
            TextButton(onClick = onDone) {
                Text(stringResource(R.string.done), color = colors.primary)
            }
            BoardTrustFooter()
        }
    }
}
