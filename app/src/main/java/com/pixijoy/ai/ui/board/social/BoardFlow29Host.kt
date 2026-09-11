package com.pixijoy.ai.ui.board.social

import androidx.annotation.StringRes
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.BoardProductionTabs
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardMainShell
import com.pixijoy.ai.ui.board.components.BoardPrimaryButton
import com.pixijoy.ai.ui.board.components.BoardShellTab
import com.pixijoy.ai.ui.board.components.BoardTrustFooter
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

/** Flow 29 session — a11y / locale prefs across screens (soft, local only). */
internal object BoardFlow29Session {
    var screenReader: Boolean = true
    var highContrast: Boolean = false
    var reduceMotion: Boolean = false
    var colorBlind: Boolean = false
    var textSize: Float = 0.5f
    var language: Int = 0
    var region: Int = 0
    var displayMode: Int = 2 // Auto
    var themeColor: Int = 0
    var roundedButtons: Boolean = true
    var voiceNav: Boolean = false
    var voiceFeedback: Boolean = true
    var audioDesc: Boolean = false
    var speechRate: Float = 0.5f
    var localizedDemo: Boolean = false
}

/**
 * Flow 29 — Accessibility / Localization.
 * Prefs are soft session state; Spanish home is a demo overlay only.
 */
@Composable
fun BoardFlow29Host(
    screenId: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val tab = BoardProductionTabs.tabHandler(onNavigate)
    when (screenId) {
        "b29_hub", "b29_home" -> OpenSettings(
            onSettings = { onNavigate("b29_settings") },
            onTab = tab
        )
        "b29_settings" -> SettingsMenu(
            onA11y = { onNavigate("b29_a11y") },
            onLanguage = { onNavigate("b29_language") },
            onAppearance = { onNavigate("b29_appearance") },
            onVoice = { onNavigate("b29_voice") },
            onBack = { onNavigate("b29_home") }
        )
        "b29_a11y" -> AccessibilityPanel(
            onSave = { onNavigate("b29_save") },
            onBack = { onNavigate("b29_settings") }
        )
        "b29_language" -> LanguagePanel(
            onContinue = { onNavigate("b29_save") },
            onBack = { onNavigate("b29_settings") }
        )
        "b29_appearance" -> AppearancePanel(
            onContinue = { onNavigate("b29_save") },
            onBack = { onNavigate("b29_settings") }
        )
        "b29_voice" -> VoicePanel(
            onContinue = { onNavigate("b29_save") },
            onBack = { onNavigate("b29_settings") }
        )
        "b29_save" -> SavedPrefs(
            onContinue = {
                BoardFlow29Session.localizedDemo = BoardFlow29Session.language == 2
                onNavigate("b29_localized")
            },
            onBack = { onNavigate("b29_settings") }
        )
        "b29_localized" -> LocalizedHome(
            onDone = onBack,
            onTab = tab
        )
        else -> OpenSettings(
            onSettings = { onNavigate("b29_settings") },
            onTab = tab
        )
    }
}

@Composable
private fun OpenSettings(onSettings: () -> Unit, onTab: (BoardShellTab) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW29_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(BoardFlowArt.logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onSettings) {
                    Icon(painter = painterResource(R.drawable.pj_ic_settings), contentDescription = stringResource(R.string.flow29_title))
                }
            }
            Image(
                painter = painterResource(BoardFlowArt.settingsHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(R.string.board_f29_hero_en),
                style = BoardType.title,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.flow29_settings_hint),
                style = BoardType.body,
                color = colors.textSecondary
            )
            BoardPrimaryButton(text = stringResource(R.string.board_f29_open_settings), onClick = onSettings)
        }
    }
}

@Composable
private fun SettingsMenu(
    onA11y: () -> Unit,
    onLanguage: () -> Unit,
    onAppearance: () -> Unit,
    onVoice: () -> Unit,
    onBack: () -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val rows = listOf(
        Triple(R.drawable.pj_ic_a11y, R.string.flow29_title, onA11y),
        Triple(R.drawable.pj_ic_language, R.string.flow29_language_item, onLanguage),
        Triple(R.drawable.pj_ic_brightness, R.string.flow29_appearance_item, onAppearance),
        Triple(R.drawable.pj_ic_mic, R.string.flow29_voice_item, onVoice)
    )
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW29_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    stringResource(R.string.board_f29_settings),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )
            }
            rows.forEach { (icon, label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(
                            if (label == R.string.flow29_title) colors.primary.copy(0.08f) else colors.surfaceMuted
                        )
                        .clickable(onClick = action)
                        .padding(PixiSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(icon), null, tint = colors.primary)
                    Text(
                        stringResource(label),
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = PixiSpacing.md)
                    )
                    Icon(painter = painterResource(R.drawable.pj_ic_chevron_right), null, tint = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun AccessibilityPanel(onSave: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var screenReader by remember { mutableStateOf(BoardFlow29Session.screenReader) }
    var highContrast by remember { mutableStateOf(BoardFlow29Session.highContrast) }
    var reduceMotion by remember { mutableStateOf(BoardFlow29Session.reduceMotion) }
    var colorBlind by remember { mutableStateOf(BoardFlow29Session.colorBlind) }
    var textSize by remember { mutableFloatStateOf(BoardFlow29Session.textSize) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Header(stringResource(R.string.flow29_a11y_item), onBack)
            ToggleRow(R.string.flow29_screen_reader, R.string.board_f29_talkback, screenReader) {
                screenReader = it
                BoardFlow29Session.screenReader = it
            }
            Text(stringResource(R.string.flow29_text_size), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Slider(
                value = textSize,
                onValueChange = {
                    textSize = it
                    BoardFlow29Session.textSize = it
                },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.flow29_preview_sample),
                color = colors.textPrimary,
                fontSize = (14 + textSize * 10).sp
            )
            ToggleRow(R.string.flow29_high_contrast, null, highContrast) {
                highContrast = it
                BoardFlow29Session.highContrast = it
            }
            ToggleRow(R.string.flow29_reduce_motion, null, reduceMotion) {
                reduceMotion = it
                BoardFlow29Session.reduceMotion = it
            }
            ToggleRow(R.string.flow29_color_blind, null, colorBlind) {
                colorBlind = it
                BoardFlow29Session.colorBlind = it
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f4_continue), onClick = onSave)
        }
    }
}

@Composable
private fun LanguagePanel(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var language by remember { mutableIntStateOf(BoardFlow29Session.language) }
    var region by remember { mutableIntStateOf(BoardFlow29Session.region) }
    val languages = listOf(
        R.string.board_f29_lang_en,
        R.string.board_f29_lang_hi,
        R.string.board_f29_lang_es,
        R.string.board_f29_lang_fr,
        R.string.board_f29_lang_de,
        R.string.board_f29_lang_ar,
        R.string.board_f29_lang_zh
    )
    val regions = listOf(R.string.board_f29_region_us, R.string.board_f29_region_in)
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Header(stringResource(R.string.flow29_language_item), onBack)
            Text(stringResource(R.string.flow29_app_language), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            languages.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(if (language == i) colors.primary.copy(0.08f) else colors.surfaceMuted)
                        .clickable {
                            language = i
                            BoardFlow29Session.language = i
                        }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = language == i, onClick = {
                        language = i
                        BoardFlow29Session.language = i
                    })
                    Text(stringResource(res), color = colors.textPrimary, modifier = Modifier.weight(1f))
                }
            }
            Text(stringResource(R.string.flow29_region), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            regions.forEachIndexed { i, res ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radii.card))
                        .background(if (region == i) colors.primary.copy(0.08f) else colors.surfaceMuted)
                        .clickable {
                            region = i
                            BoardFlow29Session.region = i
                        }
                        .padding(PixiSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = region == i, onClick = {
                        region = i
                        BoardFlow29Session.region = i
                    })
                    Text(stringResource(res), color = colors.textPrimary)
                }
            }
            Text(stringResource(R.string.board_s29_rtl), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            BoardPrimaryButton(text = stringResource(R.string.board_f4_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun AppearancePanel(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var textSize by remember { mutableFloatStateOf(BoardFlow29Session.textSize) }
    var mode by remember { mutableIntStateOf(BoardFlow29Session.displayMode) }
    var theme by remember { mutableIntStateOf(BoardFlow29Session.themeColor) }
    var rounded by remember { mutableStateOf(BoardFlow29Session.roundedButtons) }
    val modes = listOf(R.string.board_f29_light, R.string.board_f29_dark, R.string.board_f29_auto)
    val swatches = listOf(Color(0xFF3B82F6), Color(0xFFEC4899), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6))
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Header(stringResource(R.string.flow29_appearance_item), onBack)
            Text(stringResource(R.string.flow29_text_size), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Slider(
                value = textSize,
                onValueChange = {
                    textSize = it
                    BoardFlow29Session.textSize = it
                }
            )
            Text(stringResource(R.string.board_f29_display_mode), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                modes.forEachIndexed { i, res ->
                    val active = mode == i
                    Text(
                        stringResource(res),
                        modifier = Modifier
                            .clip(RoundedCornerShape(radii.pill))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                mode = i
                                BoardFlow29Session.displayMode = i
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(stringResource(R.string.board_f29_color_theme), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.md)) {
                swatches.forEachIndexed { i, c ->
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(
                                width = if (theme == i) 3.dp else 0.dp,
                                color = colors.textPrimary,
                                shape = CircleShape
                            )
                            .clickable {
                                theme = i
                                BoardFlow29Session.themeColor = i
                            }
                    )
                }
            }
            Text(stringResource(R.string.board_f29_button_style), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)) {
                listOf(true to R.string.flow29_rounded, false to R.string.flow29_square).forEach { (isRound, label) ->
                    val active = rounded == isRound
                    Text(
                        stringResource(label),
                        modifier = Modifier
                            .clip(RoundedCornerShape(if (isRound) radii.pill else radii.md))
                            .background(if (active) colors.primary else colors.surfaceMuted)
                            .clickable {
                                rounded = isRound
                                BoardFlow29Session.roundedButtons = isRound
                            }
                            .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                        color = if (active) colors.onPrimary else colors.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f4_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun VoicePanel(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    var voiceNav by remember { mutableStateOf(BoardFlow29Session.voiceNav) }
    var voiceFeedback by remember { mutableStateOf(BoardFlow29Session.voiceFeedback) }
    var audioDesc by remember { mutableStateOf(BoardFlow29Session.audioDesc) }
    var rate by remember { mutableFloatStateOf(BoardFlow29Session.speechRate) }
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Header(stringResource(R.string.flow29_voice_item), onBack)
            ToggleRow(R.string.flow29_voice_nav, null, voiceNav) {
                voiceNav = it
                BoardFlow29Session.voiceNav = it
            }
            ToggleRow(R.string.flow29_voice_feedback, null, voiceFeedback) {
                voiceFeedback = it
                BoardFlow29Session.voiceFeedback = it
            }
            ToggleRow(R.string.flow29_audio_desc, null, audioDesc) {
                audioDesc = it
                BoardFlow29Session.audioDesc = it
            }
            Text(stringResource(R.string.flow29_preferred_voice), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(
                stringResource(R.string.flow29_voice_sample),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(radii.card))
                    .background(colors.surfaceMuted)
                    .padding(PixiSpacing.lg),
                color = colors.textPrimary
            )
            Text(stringResource(R.string.flow29_speech_rate), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Slider(
                value = rate,
                onValueChange = {
                    rate = it
                    BoardFlow29Session.speechRate = it
                }
            )
            Text(stringResource(R.string.flow29_voice_note), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            BoardPrimaryButton(text = stringResource(R.string.board_f4_continue), onClick = onContinue)
        }
    }
}

@Composable
private fun SavedPrefs(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    BoardAdaptiveFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PixiSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colors.successSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.pj_ic_check_circle), null, tint = colors.success, modifier = Modifier.size(64.dp))
            }
            Spacer(modifier = Modifier.height(PixiSpacing.lg))
            Text(
                stringResource(R.string.flow29_save),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                stringResource(R.string.flow29_saved_body),
                textAlign = TextAlign.Center,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = PixiSpacing.md)
            )
            BoardPrimaryButton(text = stringResource(R.string.flow29_continue), onClick = onContinue)
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.cd_back), color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LocalizedHome(onDone: () -> Unit, onTab: (BoardShellTab) -> Unit) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val localized = BoardFlow29Session.localizedDemo
    BoardMainShell(selected = BoardShellTab.Home, onTab = onTab) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PixiSpacing.xl)
                .testTag(TestTags.FLOW29_HOST),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            Image(
                painter = painterResource(BoardFlowArt.logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(32.dp)
            )
            Image(
                painter = painterResource(BoardFlowArt.homeHero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(radii.card)),
                contentScale = ContentScale.Crop
            )
            Text(
                stringResource(if (localized) R.string.flow29_es_banner else R.string.board_f29_hero_en),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            if (localized) {
                Text(stringResource(R.string.flow29_es_creations), color = colors.textSecondary)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
                ) {
                    listOf(
                        R.string.board_f29_es_home,
                        R.string.board_f29_es_explore,
                        R.string.board_f29_es_birthday
                    ).forEach { res ->
                        Text(
                            stringResource(res),
                            modifier = Modifier
                                .clip(RoundedCornerShape(radii.pill))
                                .background(colors.surfaceMuted)
                                .padding(horizontal = PixiSpacing.md, vertical = PixiSpacing.sm),
                            color = colors.textPrimary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            BoardPrimaryButton(text = stringResource(R.string.board_f21_got_it), onClick = onDone)
            BoardTrustFooter()
        }
    }
}

@Composable
private fun Header(title: String, onBack: () -> Unit) {
    val colors = BoardTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(painter = painterResource(R.drawable.pj_ic_back), contentDescription = stringResource(R.string.cd_back))
        }
        Text(
            title,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ToggleRow(
    @StringRes title: Int,
    @StringRes subtitle: Int?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radii.card))
            .background(colors.surfaceMuted)
            .padding(PixiSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(title), fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            if (subtitle != null) {
                Text(stringResource(subtitle), color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
