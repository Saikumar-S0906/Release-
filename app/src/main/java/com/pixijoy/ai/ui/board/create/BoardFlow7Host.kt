package com.pixijoy.ai.ui.board.create

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pixijoy.ai.R
import com.pixijoy.ai.di.BoardAiEntryPoint
import com.pixijoy.ai.domain.model.NlAction
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardMediaSession
import com.pixijoy.ai.ui.board.BoardSafeResultImage
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardDrawableIo
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardEdgeActions
import com.pixijoy.ai.ui.board.components.BoardSoftProgress
import com.pixijoy.ai.ui.board.components.BoardEdgeKind
import com.pixijoy.ai.ui.board.components.BoardSoftEdgeGate
import com.pixijoy.ai.ui.board.components.BoardOutlinedButton
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardResultFooter
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.components.decodeBoardDrawable
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Tell PixiJoy prompt session across Flow 7. */
internal object BoardFlow7Session {
    var prompt: String =
        "Make this photo look like I'm on a beach with a sunset background"
    var resultVariant: Int = 0
    var usedSoftFallback: Boolean = false
    var planAction: NlAction = NlAction.UNKNOWN
    var agentCaption: String? = null

    @Volatile
    var resultBitmap: Bitmap? = null
        private set

    fun setResult(bitmap: Bitmap?) {
        val old = resultBitmap
        resultBitmap = bitmap
        if (old != null && old !== bitmap && !old.isRecycled) {
            runCatching { old.recycle() }
        }
    }

    fun resultImage(): Int = BoardFlowArt.rematchTell.getOrElse(resultVariant % BoardFlowArt.rematchTell.size) {
        BoardFlowArt.tellHero
    }
}

private fun saveDrawableToGallery(context: Context, @DrawableRes resId: Int): Boolean =
    BoardDrawableIo.saveToGallery(context, resId, filePrefix = "PixiJoy_Tell")

private fun shareDrawable(context: Context, @DrawableRes resId: Int) {
    BoardDrawableIo.shareAsync(
        context = context,
        resId = resId,
        textRes = R.string.board_f7_share_text,
        filePrefix = "pixijoy_tell"
    )
}


/** Flow 7 — Tell PixiJoy. Board: REMATCH_07_Tell_PixiJoy.png */
@Composable
fun BoardFlow7Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    when (screenId) {
        "b7_sheet", "b7_tell" -> OpenTell(
            onPrompt = { text ->
                BoardFlow7Session.prompt = text
                onNavigate("b7_request")
            },
            onInputMethod = { onNavigate("b7_input") },
            onBack = onBack
        )
        "b7_input" -> InputMethod(
            onContinue = { onNavigate("b7_request") },
            onBack = { onNavigate("b7_sheet") }
        )
        "b7_request", "b7_confirm" -> YourRequest(
            onSubmit = { onNavigate("b7_understanding") },
            onBack = { onNavigate("b7_input") }
        )
        "b7_understanding" -> Understanding(
            onDone = { onNavigate("b7_generating") },
            onCancel = { onNavigate("b7_request") }
        )
        "b7_generating" -> Generating(
            onDone = { onNavigate("b7_result") },
            onCancel = onBack,
            onOffline = { onNavigate("b7_offline") },
            onLowCredits = { onNavigate("b7_low_credits") },
            onSafety = { onNavigate("b7_safety") }
        )
        "b7_result" -> ReviewResult(
            onUse = { onNavigate("b7_next") },
            onAnotherStyle = { onNavigate("b7_request") },
            onEdit = { onNavigate("b15_open") },
            onExport = { onNavigate("b17_open") },
            onRemake = { onNavigate("b7_request") },
            onPublish = { onNavigate("b8_publish") },
            onShare = { onNavigate("b8_share") },
            onRegenerate = { onNavigate("b7_request") },
            onBack = { onNavigate("b7_request") }
        )
        "b7_next" -> WhatsNext(
            onVideo = { onNavigate("b14_input") },
            onStickers = { onNavigate("b13_photo") },
            onCaption = { onNavigate("b6_customize") },
            onAnotherPrompt = { onNavigate("b7_sheet") },
            onShare = { onNavigate("b8_share") },
            onHome = onBack,
            onBack = { onNavigate("b7_result") }
        )
        "b7_offline" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Offline,
            onPrimary = { onNavigate("b7_request") },
            onSecondary = { onNavigate("b7_request") }
        )
        "b7_low_credits" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.LowCredits,
            onPrimary = { onNavigate("b23_dash") },
            onSecondary = { onNavigate("b7_request") }
        )
        "b7_safety" -> BoardSoftEdgeGate(
            kind = BoardEdgeKind.Safety,
            onPrimary = { onNavigate("b7_sheet") },
            onSecondary = onBack
        )
        else -> OpenTell(
            onPrompt = { text ->
                BoardFlow7Session.prompt = text
                onNavigate("b7_request")
            },
            onInputMethod = { onNavigate("b7_input") },
            onBack = onBack
        )
    }
}

@Composable
private fun OpenTell(
    onPrompt: (String) -> Unit,
    onInputMethod: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var draft by remember { mutableStateOf("") }
    val suggestions = listOf(
        R.string.board_f7_sug_remove,
        R.string.board_f7_sug_meme,
        R.string.board_f7_sug_sticker,
        R.string.board_f7_sug_beach,
        R.string.board_f7_sug_cinematic
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.splashTop)
                .testTag(TestTags.FLOW7_TELL)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(PixiSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(painter = painterResource(R.drawable.pj_ic_back),
                            null,
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.board_f7_title),
                            fontWeight = FontWeight.Bold,
                            style = BoardType.title,
                            color = Color.White
                        )
                        Text(
                            stringResource(R.string.board_f7_subtitle),
                            color = Color.White.copy(0.7f)
                        )
                    }
                }
                suggestions.forEachIndexed { i, res ->
                    val text = stringResource(res)
                    val thumb = BoardFlowArt.rematchTell[i % BoardFlowArt.rematchTell.size]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.pill))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onPrompt(text) }
                            .padding(PixiSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                    ) {
                        Image(
                            painter = painterResource(thumb),
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Text(text, color = Color.White, style = BoardType.body)
                    }
                }
                BoardOutlinedButton(
                    text = stringResource(R.string.board_f7_choose_method),
                    onClick = onInputMethod
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PixiSpacing.lg)
                    .clip(RoundedCornerShape(radii.pill))
                    .background(Color.White.copy(0.12f))
                    .padding(horizontal = PixiSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            stringResource(R.string.board_f7_type_hint),
                            color = Color.White.copy(0.5f)
                        )
                    },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = colors.primary
                    )
                )
                IconButton(
                    onClick = { onInputMethod() }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_mic),
                        contentDescription = stringResource(R.string.cd_voice_input),
                        tint = colors.primary
                    )
                }
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) onPrompt(draft.trim())
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_send), contentDescription = stringResource(R.string.cd_send), tint = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun InputMethod(onContinue: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var mode by remember { mutableIntStateOf(0) } // 0 speak, 1 type, 2 prompt
    var typed by remember { mutableStateOf(BoardFlow7Session.prompt) }
    var listening by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        listening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                BoardFlow7Session.prompt = spoken
                typed = spoken
                onContinue()
            }
        }
    }

    fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.board_f7_listening))
        }
        try {
            listening = true
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            listening = false
            Toast.makeText(context, context.getString(R.string.board_f7_speech_unavailable), Toast.LENGTH_SHORT).show()
            mode = 1
        }
    }

    LaunchedEffect(listening) {
        seconds = 0
        while (listening) {
            delay(1000)
            seconds++
        }
    }

    val popular = remember {
        listOf(
            R.string.board_f7_pop_bg,
            R.string.board_f7_pop_text,
            R.string.board_f7_pop_meme,
            R.string.board_f7_pop_sticker,
            R.string.board_f7_pop_location
        )
    }

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f7_input_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                listOf(
                    Triple(0, R.drawable.pj_ic_mic, R.string.flow7_speak),
                    Triple(1, R.drawable.pj_ic_keyboard, R.string.flow7_type),
                    Triple(2, R.drawable.pj_ic_bulb, R.string.flow7_prompt)
                ).forEach { (index, icon, label) ->
                    val on = mode == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.card))
                            .background(if (on) colors.primary else colors.surfaceMuted)
                            .clickable {
                                mode = index
                                if (index == 0) startSpeech()
                            }
                            .padding(PixiSpacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(painter = painterResource(icon),
                            null,
                            tint = if (on) Color.White else colors.textSecondary
                        )
                        Text(
                            stringResource(label),
                            color = if (on) Color.White else colors.textSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            if (mode == 0) {
                Text(
                    stringResource(R.string.board_f7_listening),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                ListeningWaveBars(active = listening, accent = colors.primary)
                Text(
                    String.format(Locale.getDefault(), "00:%02d", seconds.coerceAtMost(59)),
                    color = colors.textSecondary
                )
                BoardPrimaryButton(
                    text = stringResource(R.string.board_f7_stop),
                    onClick = {
                        listening = false
                        if (BoardFlow7Session.prompt.isNotBlank()) onContinue()
                    }
                )
            }
            Text(stringResource(R.string.board_f7_or_type), color = colors.textSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.board_f7_type_example)) },
                    shape = RoundedCornerShape(radii.md)
                )
                IconButton(
                    onClick = {
                        if (typed.isNotBlank()) {
                            BoardFlow7Session.prompt = typed.trim()
                            onContinue()
                        }
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.pj_ic_send), contentDescription = stringResource(R.string.cd_send), tint = colors.primary)
                }
            }
            if (mode == 2) {
                Text(
                    stringResource(R.string.flow7_popular),
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                popular.forEach { res ->
                    val text = stringResource(res)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(radii.card))
                            .background(colors.surfaceMuted)
                            .clickable {
                                BoardFlow7Session.prompt = text
                                typed = text
                                onContinue()
                            }
                            .padding(PixiSpacing.md)
                    ) {
                        Text(text, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun YourRequest(onSubmit: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var text by remember { mutableStateOf(BoardFlow7Session.prompt) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f7_request_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(radii.card))
            ) {
                Image(
                    painter = painterResource(BoardFlowArt.baAfter),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(radii.md)
            )
            BoardPrimaryButton(
                text = stringResource(R.string.flow7_submit),
                onClick = {
                    BoardFlow7Session.prompt = text.trim().ifBlank { BoardFlow7Session.prompt }
                    onSubmit()
                }
            )
        }
    }
}

@Composable
private fun Understanding(onDone: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val steps = listOf(
        R.string.board_f7_step_request,
        R.string.board_f7_step_photo,
        R.string.board_f7_step_elements,
        R.string.board_f7_step_style,
        R.string.board_f7_step_create
    )
    var done by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        done = 1
        val app = context.applicationContext
        withContext(Dispatchers.Default) {
            runCatching {
                val entry = EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                val src = BoardMediaSession.decodeBitmap(app)
                    ?: decodeBoardDrawable(app, BoardFlowArt.baAfter, maxSidePx = 1280)
                val plan = entry.aiOrchestrator().parseCommand(src, BoardFlow7Session.prompt)
                BoardFlow7Session.planAction = plan.action
            }
        }
        done = steps.size
        delay(150)
        onDone()
    }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.splashTop)
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PixiJoy AI",
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_sparkles), null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            steps.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = PixiSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    when {
                        i < done -> Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success)
                        i == done -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(0.35f), CircleShape)
                        )
                    }
                    Text(
                        stringResource(res),
                        color = if (i <= done) Color.White else Color.White.copy(0.45f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.board_f7_seconds),
                color = Color.White.copy(0.7f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            BoardOutlinedButton(
                text = stringResource(R.string.board_f19_cancel),
                onClick = onCancel
            )
        }
    }
}

@Composable
private fun Generating(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOffline: () -> Unit,
    onLowCredits: () -> Unit,
    onSafety: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    var pct by remember { mutableIntStateOf(12) }
    androidx.activity.compose.BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) {
        BoardFlow7Session.setResult(null)
        BoardFlow7Session.usedSoftFallback = false
        BoardFlow7Session.agentCaption = null
        pct = 15
        val app = context.applicationContext
        val prompt = BoardFlow7Session.prompt
        val photoUri = BoardMediaSession.photoUri
        var result: Bitmap? = null
        if (photoUri != null) {
            runCatching {
                val agent = EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                    .creativeAgentOrchestrator()
                agent.execute(prompt, photoUri).collect { progress ->
                    val done = progress.steps.count { it.status.name == "DONE" }
                    pct = (20 + (done * 70 / progress.steps.size.coerceAtLeast(1))).coerceIn(20, 90)
                    progress.result?.previewBitmap?.let { preview ->
                        result = preview
                        BoardFlow7Session.agentCaption = progress.result?.caption?.primary
                    }
                    if (progress.error != null) throw IllegalStateException(progress.error)
                }
            }
        }
        if (result == null) {
            val action = BoardFlow7Session.planAction
            result = withContext(Dispatchers.Default) {
                runCatching {
                    val entry = EntryPointAccessors.fromApplication(app, BoardAiEntryPoint::class.java)
                    val pipe = entry.createExtraPipeline()
                    val src = BoardMediaSession.decodeBitmap(app)
                        ?: decodeBoardDrawable(app, BoardFlowArt.baAfter, maxSidePx = 1280)
                        ?: return@runCatching null
                    when (action) {
                        NlAction.REMOVE_BACKGROUND, NlAction.MAKE_STICKER -> pipe.cutoutTransparent(src)
                        NlAction.OBJECT_REMOVE -> pipe.peopleEdit(src, removeMode = true)
                        NlAction.PORTRAIT_STUDIO, NlAction.PORTRAIT_BLUR, NlAction.MAKE_MEME, NlAction.MAKE_FUNNY ->
                            pipe.faceRecastOrSwap(src, 1)
                        NlAction.BRIGHTEN, NlAction.DARKEN, NlAction.GENERATE_VARIATIONS, NlAction.ADVANCED_EDIT,
                        NlAction.REPLACE_BACKGROUND, NlAction.CREATE_REEL ->
                            pipe.applyLook(src, 1)
                        NlAction.ENHANCE, NlAction.UNKNOWN -> pipe.enhanceFaceLook(src)
                    }
                }.getOrNull()
            }
        }
        pct = 95
        if (result != null) {
            BoardFlow7Session.setResult(result)
            BoardFlow7Session.usedSoftFallback = false
        } else {
            BoardFlow7Session.usedSoftFallback = true
        }
        pct = 100
        delay(200)
        onDone()
    }
    val previews = BoardFlowArt.rematchTell
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.splashTop)
                .padding(PixiSpacing.xl)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCancel) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null, tint = Color.White)
                }
                Text(
                    stringResource(R.string.board_f7_generating_title),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = BoardType.title
                )
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                previews.forEach { res ->
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(BoardTheme.radii.card)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(modifier = Modifier.height(PixiSpacing.md))
            Text(
                stringResource(R.string.board_f7_generating_pct, pct),
                color = Color.White.copy(0.8f)
            )
            BoardSoftProgress(progress = pct / 100f, darkSurface = true)
            Spacer(modifier = Modifier.height(PixiSpacing.sm))
            BoardEdgeActions(
                onOffline = onOffline,
                onLowCredits = onLowCredits,
                onSafety = onSafety,
                darkSurface = true
            )
        }
    }
}

@Composable
private fun ReviewResult(
    onUse: () -> Unit,
    onAnotherStyle: () -> Unit,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onRemake: () -> Unit,
    onPublish: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    var selected by remember { mutableIntStateOf(BoardFlow7Session.resultVariant) }
    val variants = BoardFlowArt.rematchTell
    val fallback = variants.getOrElse(selected % variants.size) { BoardFlowArt.tellHero }

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f7_result_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            BoardSafeResultImage(
                bitmap = BoardFlow7Session.resultBitmap,
                fallbackRes = fallback,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(radii.card))
            )
            if (BoardFlow7Session.usedSoftFallback) {
                Text(
                    stringResource(R.string.board_c04_soft_fallback),
                    color = colors.textSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                variants.forEachIndexed { i, res ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(radii.md))
                            .border(
                                width = if (i == selected) 2.dp else 0.dp,
                                color = colors.primary,
                                shape = RoundedCornerShape(radii.md)
                            )
                            .clickable {
                                selected = i
                                BoardFlow7Session.resultVariant = i
                            }
                    ) {
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            BoardResultFooter(
                onSave = {
                    scope.launch {
                        val out = BoardFlow7Session.resultBitmap
                        val ok = withContext(Dispatchers.IO) {
                            if (out != null && !out.isRecycled) {
                                BoardDrawableIo.saveBitmapToGallery(
                                    context,
                                    out,
                                    filePrefix = "PixiJoy_Tell"
                                )
                            } else {
                                saveDrawableToGallery(context, fallback)
                            }
                        }
                        Toast.makeText(
                            context,
                            context.getString(if (ok) R.string.board_f7_saved else R.string.board_f7_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onShare = onShare,
                onEdit = onEdit,
                onExport = onExport,
                onRemake = onRemake,
                onPublish = onPublish
            )
            BoardPrimaryButton(
                text = stringResource(R.string.board_f7_use_this),
                onClick = onUse
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f7_another_style),
                onClick = onAnotherStyle
            )
            BoardOutlinedButton(
                text = stringResource(R.string.board_f7_regenerate),
                onClick = onRegenerate
            )
        }
    }
}

@Composable
private fun QuickAction(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    val colors = BoardTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(painter = painterResource(icon), null, tint = colors.primary)
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun WhatsNext(
    onVideo: () -> Unit,
    onStickers: () -> Unit,
    onCaption: () -> Unit,
    onAnotherPrompt: () -> Unit,
    onShare: () -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val scope = rememberCoroutineScope()
    val image = BoardFlow7Session.resultImage()
    data class NextAction(@StringRes val label: Int, val accent: Color, val onClick: () -> Unit)
    val actions = listOf(
        NextAction(R.string.board_f5_save_gallery, colors.primary) {
            scope.launch {
                val out = BoardFlow7Session.resultBitmap
                val ok = withContext(Dispatchers.IO) {
                    if (out != null && !out.isRecycled) {
                        BoardDrawableIo.saveBitmapToGallery(context, out, filePrefix = "PixiJoy_Tell")
                    } else {
                        saveDrawableToGallery(context, image)
                    }
                }
                Toast.makeText(
                    context,
                    context.getString(if (ok) R.string.board_f7_saved else R.string.board_f7_save_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        NextAction(R.string.board_f7_share_social, colors.primary.copy(alpha = 0.75f), onShare),
        NextAction(R.string.board_f6_create_video, colors.accentPink, onVideo),
        NextAction(R.string.board_f6_make_stickers, colors.accentPurple, onStickers),
        NextAction(R.string.board_f7_add_caption, Color(0xFF26A69A), onCaption),
        NextAction(R.string.board_f7_another_prompt, colors.accentYellow, onAnotherPrompt),
        NextAction(R.string.board_f7_go_home, Color(0xFF1A237E), onHome)
    )

    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), null)
                }
                Text(
                    stringResource(R.string.board_f7_next_title),
                    fontWeight = FontWeight.Bold,
                    style = BoardType.title,
                    color = colors.textPrimary
                )
            }
            actions.forEach { action ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(colors.surfaceMuted)
                        .clickable(onClick = action.onClick)
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(action.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(R.drawable.pj_ic_home), null, tint = action.accent, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        stringResource(action.label),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Text("›", color = colors.textSecondary, style = BoardType.title)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.primarySoft)
                    .clickable(onClick = onAnotherPrompt)
                    .padding(PixiSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)
            ) {
                Image(
                    painter = painterResource(image),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(radii.md)),
                    contentScale = ContentScale.Crop
                )
                Text(
                    stringResource(R.string.board_f7_series_upsell),
                    modifier = Modifier.weight(1f),
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            BoardTrustFooter()
        }
    }
}

/** Voice bars — infinite transition only while actively listening. */
@Composable
private fun ListeningWaveBars(active: Boolean, accent: Color) {
    if (!active) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(7) { i ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(6.dp)
                        .height((12 + i * 2).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accent.copy(alpha = 0.35f))
                )
            }
        }
        return
    }
    val wave = rememberInfiniteTransition(label = "wave")
    val waveH by wave.animateFloat(
        initialValue = 8f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "waveH"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { i ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(6.dp)
                    .height((waveH + i * 3).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
    }
}
