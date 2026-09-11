package com.pixijoy.ai.ui.board.create

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixijoy.ai.R
import com.pixijoy.ai.testing.TestTags
import com.pixijoy.ai.ui.board.LocalBoardEmbeddedInMainShell
import com.pixijoy.ai.ui.board.LocalBoardPipeline
import com.pixijoy.ai.ui.board.rememberBoardPhotoGate
import com.pixijoy.ai.ui.board.components.BoardAdaptiveFrame
import com.pixijoy.ai.ui.board.components.BoardAiSparkleIcon
import com.pixijoy.ai.ui.board.components.BoardBodyText
import com.pixijoy.ai.ui.board.components.BoardCaptionText
import com.pixijoy.ai.ui.board.components.BoardFadeIn
import com.pixijoy.ai.ui.board.components.BoardFilterPill
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.components.BoardIntentTile
import com.pixijoy.ai.ui.board.components.BoardListDetail
import com.pixijoy.ai.ui.board.components.BoardScrollCue
import com.pixijoy.ai.ui.board.components.BoardTitleText
import com.pixijoy.ai.ui.board.components.BoardWideFrame
import com.pixijoy.ai.ui.board.components.boardHeading
import com.pixijoy.ai.ui.board.components.rememberBoardTwoPane
import com.pixijoy.ai.ui.board.theme.BoardTheme
import com.pixijoy.ai.ui.board.theme.BoardType
import com.pixijoy.ai.ui.theme.PixiSpacing

private const val CreateHubTileAspect = 0.95f
private const val CreateHubLandscapeAspect = 2.15f
private const val CreateHubPaneAspect = 2.05f

private data class CreateIntent(
    val title: String,
    val subtitle: String,
    val accent: Color,
    @DrawableRes val imageRes: Int,
    val onClick: () -> Unit,
    val testTag: String? = null
)

/**
 * Create tab root — Tools (on-device studio) default, AI as a second segment.
 * Expanded (≥840dp): intents list + persistent canvas pane.
 */
@Composable
fun BoardCreateHub(
    onBack: (() -> Unit)? = null,
    onNavigate: (String) -> Unit = {}
) {
    val colors = BoardTheme.colors
    val radii = BoardTheme.radii
    val pipeline = LocalBoardPipeline.current
    val embedded = LocalBoardEmbeddedInMainShell.current
    val twoPane = rememberBoardTwoPane()
    val shortViewport = LocalConfiguration.current.screenHeightDp < 520
    val columns = if (twoPane) 1 else 2
    val tileAspect = when {
        twoPane -> CreateHubPaneAspect
        shortViewport -> CreateHubLandscapeAspect
        else -> CreateHubTileAspect
    }
    var toolsSegment by rememberSaveable { mutableStateOf(true) }
    var canvasKey by rememberSaveable { mutableStateOf("editor") }
    val hubScroll = rememberScrollState()
    val eraGate = rememberBoardPhotoGate(
        onCameraDenied = { onNavigate("b3_camera_denied") }
    ) {
        onNavigate("b_diff_options")
    }
    val canvasGate = rememberBoardPhotoGate(
        onCameraDenied = { onNavigate("b3_camera_denied") }
    ) { }
    val packGate = rememberBoardPhotoGate(
        onCameraDenied = { onNavigate("b3_camera_denied") }
    ) {
        onNavigate("b_pack_run")
    }
    val remixGate = rememberBoardPhotoGate(
        onCameraDenied = { onNavigate("b3_camera_denied") }
    ) {
        onNavigate("b_c14")
    }
    val selectedCanvasRes = when (canvasKey) {
        "funny" -> BoardFlowArt.funnyHero
        "sticker" -> BoardFlowArt.stickerHero
        "move" -> BoardFlowArt.motionHero
        "different" -> BoardFlowArt.differentHero
        "collage" -> BoardFlowArt.collageHero
        "tell" -> BoardFlowArt.tellHero
        "beautiful" -> BoardFlowArt.beautifulHero
        "restore" -> R.drawable.board_c05_after
        "cutout" -> R.drawable.board_c04_cutout
        "talking" -> R.drawable.board_c11_talk
        "portrait" -> R.drawable.board_c09_portrait
        "faceswap" -> R.drawable.board_c06_faces
        "export" -> BoardFlowArt.editorHero
        else -> BoardFlowArt.editorHero
    }
    val backAction = onBack
    val showBack = backAction != null && !embedded

    if (showBack) {
        BackHandler(onBack = backAction!!)
    }

    fun pick(key: String, open: () -> Unit) {
        canvasKey = key
        open()
    }

    fun openBoard(key: String, screenId: String) {
        pick(key) { onNavigate(screenId) }
    }

    val fix = listOf(
        CreateIntent(
            title = stringResource(R.string.flow15_title),
            subtitle = stringResource(R.string.create_hub_editor_desc),
            accent = colors.primary,
            imageRes = BoardFlowArt.editorHero,
            onClick = { openBoard("editor", "b15_open") },
            testTag = TestTags.CREATE_HUB_EDITOR
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_restore),
            subtitle = stringResource(R.string.create_hub_restore_desc),
            accent = colors.accentPink,
            imageRes = R.drawable.board_c05_before,
            onClick = { openBoard("restore", "b_c05") },
            testTag = TestTags.CREATE_HUB_RESTORE
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_expand),
            subtitle = stringResource(R.string.create_hub_expand_desc),
            accent = colors.primary,
            imageRes = R.drawable.board_c10_expand,
            onClick = { openBoard("expand", "b_c10") },
            testTag = TestTags.CREATE_HUB_EXPAND
        )
    )
    val cutout = listOf(
        CreateIntent(
            title = stringResource(R.string.create_extra_remove_bg),
            subtitle = stringResource(R.string.create_hub_remove_bg_desc),
            accent = colors.accentPink,
            imageRes = R.drawable.board_c04_cutout,
            onClick = { openBoard("cutout", "b_c04") },
            testTag = TestTags.CREATE_HUB_REMOVE_BG
        ),
        CreateIntent(
            title = stringResource(R.string.create_stickers),
            subtitle = stringResource(R.string.home_intent_stickers_desc),
            accent = colors.primary,
            imageRes = BoardFlowArt.stickerHero,
            onClick = { openBoard("sticker", "b13_photo") },
            testTag = TestTags.CREATE_HUB_STICKER
        ),
    )
    val motion = listOf(
        CreateIntent(
            title = stringResource(R.string.make_it_move),
            subtitle = stringResource(R.string.home_intent_move_desc),
            accent = colors.accentPurple,
            imageRes = BoardFlowArt.motionHero,
            onClick = { pick("move", pipeline.openMove) },
            testTag = TestTags.CREATE_HUB_MOVE
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_talking),
            subtitle = stringResource(R.string.create_hub_talking_desc),
            accent = colors.accentPink,
            imageRes = R.drawable.board_c11_talk,
            onClick = { openBoard("talking", "b_c11") },
            testTag = TestTags.CREATE_HUB_TALKING
        ),
    )
    val compose = listOf(
        CreateIntent(
            title = stringResource(R.string.board_f16_create_hub),
            subtitle = stringResource(R.string.home_intent_collage_desc),
            accent = colors.accentPink,
            imageRes = BoardFlowArt.collageHero,
            onClick = { pick("collage", pipeline.openCollage) },
            testTag = TestTags.CREATE_HUB_COLLAGE
        )
    )
    val aiLooks = listOf(
        CreateIntent(
            title = stringResource(R.string.compete_pack_title),
            subtitle = stringResource(R.string.compete_pack_sub),
            accent = colors.accentPink,
            imageRes = BoardFlowArt.stickerHero,
            onClick = { pick("pack", packGate) }
        ),
        CreateIntent(
            title = stringResource(R.string.make_it_beautiful),
            subtitle = stringResource(R.string.home_intent_beautiful_desc),
            accent = colors.accentPink,
            imageRes = BoardFlowArt.beautifulHero,
            onClick = { pick("beautiful", pipeline.openBeautiful) },
            testTag = TestTags.CREATE_HUB_BEAUTIFUL
        ),
        CreateIntent(
            title = stringResource(R.string.make_it_funny),
            subtitle = stringResource(R.string.home_intent_funny_desc),
            accent = colors.accentYellow,
            imageRes = BoardFlowArt.funnyHero,
            onClick = { pick("funny", pipeline.openFunny) },
            testTag = TestTags.CREATE_HUB_MEME
        ),
        CreateIntent(
            title = stringResource(R.string.make_it_different),
            subtitle = stringResource(R.string.home_intent_different_desc),
            accent = colors.primary,
            imageRes = BoardFlowArt.differentHero,
            onClick = { pick("different", pipeline.openDifferent) },
            testTag = TestTags.CREATE_HUB_DIFFERENT
        )
    )
    val aiMore = listOf(
        CreateIntent(
            title = stringResource(R.string.home_talk_to_pixijoy),
            subtitle = stringResource(R.string.home_talk_hint),
            accent = colors.accentPurple,
            imageRes = BoardFlowArt.tellHero,
            onClick = { pick("tell", pipeline.openTell) },
            testTag = TestTags.HOME_TALK
        ),
        CreateIntent(
            title = stringResource(R.string.create_hub_time_machine),
            subtitle = stringResource(R.string.create_hub_time_machine_desc),
            accent = colors.primary,
            imageRes = BoardFlowArt.differentHero,
            onClick = {
                BoardFlowDifferentSession.eraIndex = 1
                BoardFlowDifferentSession.styleIndex = 8
                eraGate()
            }
        ),
        CreateIntent(
            title = stringResource(R.string.board_c06_title_short),
            subtitle = stringResource(R.string.create_hub_face_swap_desc),
            accent = colors.accentPink,
            imageRes = R.drawable.board_c06_faces,
            onClick = { openBoard("faceswap", "b_c06") }
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_scene),
            subtitle = stringResource(R.string.create_hub_scene_desc),
            accent = colors.primary,
            imageRes = R.drawable.board_c07_scene,
            onClick = { openBoard("scene", "b_c07") }
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_portrait),
            subtitle = stringResource(R.string.create_hub_portrait_desc),
            accent = colors.accentPink,
            imageRes = R.drawable.board_c09_portrait,
            onClick = { openBoard("portrait", "b_c09") }
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_greeting),
            subtitle = stringResource(R.string.create_hub_greeting_desc),
            accent = colors.primary,
            imageRes = R.drawable.board_c12_card,
            onClick = { openBoard("greeting", "b_c12") }
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_remix),
            subtitle = stringResource(R.string.create_hub_remix_desc),
            accent = colors.primary,
            imageRes = R.drawable.board_c14_remix,
            onClick = { pick("remix", remixGate) }
        ),
        CreateIntent(
            title = stringResource(R.string.create_extra_people),
            subtitle = stringResource(R.string.create_hub_people_desc),
            accent = colors.accentPurple,
            imageRes = R.drawable.board_c08_people,
            onClick = { openBoard("people", "b_c08") }
        )
    )
    val intentList: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(hubScroll)
                .padding(PixiSpacing.xl)
                .testTag(TestTags.CREATE_HUB_TOOLS),
            verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)
        ) {
            if (showBack) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { backAction?.invoke() }) {
                        Icon(
                            painter = painterResource(R.drawable.pj_ic_back),
                            contentDescription = stringResource(R.string.cd_back),
                            tint = colors.textPrimary
                        )
                    }
                    BoardTitleText(
                        text = stringResource(R.string.create_hub_title),
                        modifier = Modifier
                            .weight(1f)
                            .boardHeading()
                    )
                }
            } else {
                BoardTitleText(
                    text = stringResource(R.string.create_hub_title),
                    modifier = Modifier.boardHeading()
                )
            }
            BoardBodyText(
                text = stringResource(
                    if (toolsSegment) R.string.create_hub_subtitle
                    else R.string.create_hub_ai_subtitle
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
            ) {
                BoardFilterPill(
                    text = stringResource(R.string.create_hub_tools),
                    selected = toolsSegment,
                    onClick = { toolsSegment = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.CREATE_HUB_SEGMENT_TOOLS)
                )
                BoardFilterPill(
                    text = stringResource(R.string.create_hub_ai),
                    selected = !toolsSegment,
                    onClick = { toolsSegment = false },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TestTags.CREATE_HUB_SEGMENT_AI)
                )
            }
            BoardFadeIn(key = toolsSegment) {
            Column(verticalArrangement = Arrangement.spacedBy(PixiSpacing.md)) {
            if (toolsSegment) {
                BoardCaptionText(
                    text = stringResource(R.string.create_hub_tools_caption),
                    color = colors.textPrimary
                )
                HubGroup(stringResource(R.string.create_hub_group_fix), fix, columns, tileAspect)
                HubGroup(stringResource(R.string.create_hub_group_cutout), cutout, columns, tileAspect)
                HubGroup(stringResource(R.string.create_hub_group_motion), motion, columns, tileAspect)
                HubGroup(stringResource(R.string.create_hub_group_compose), compose, columns, tileAspect)
            } else {
                BoardCaptionText(
                    text = stringResource(R.string.create_hub_ai_caption),
                    color = colors.textPrimary
                )
                HubGroup(stringResource(R.string.create_hub_ai), aiLooks, columns, tileAspect)
                HubGroup(stringResource(R.string.create_hub_more), aiMore, columns, tileAspect)
            }
            }
            }
        }
        BoardScrollCue(
            canScrollForward = hubScroll.canScrollForward,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }

    if (twoPane) {
        BoardWideFrame {
            BoardListDetail(
                showDetail = true,
                list = { intentList() },
                detail = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceMuted)
                        .padding(PixiSpacing.xl),
                    verticalArrangement = Arrangement.spacedBy(PixiSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BoardTitleText(text = stringResource(R.string.board_create_canvas_title))
                    BoardBodyText(text = stringResource(R.string.board_create_canvas_body))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(radii.card))
                    ) {
                        val mediaRev = com.pixijoy.ai.ui.board.BoardMediaSession.revision
                        if (com.pixijoy.ai.ui.board.BoardMediaSession.hasPhoto()) {
                            com.pixijoy.ai.ui.board.BoardSessionPhoto(
                                fallbackRes = selectedCanvasRes,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            com.pixijoy.ai.ui.board.components.BoardEmptyState(
                                title = stringResource(R.string.choose_a_photo),
                                body = stringResource(R.string.photo_gate_body),
                                ctaLabel = stringResource(R.string.take_photo),
                                onCta = canvasGate,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        @Suppress("UNUSED_VARIABLE")
                        val observed = mediaRev
                    }
                }
            }
            )
        }
    } else {
        BoardAdaptiveFrame {
            intentList()
        }
    }
}

@Composable
private fun HubGroup(
    title: String,
    items: List<CreateIntent>,
    columns: Int,
    tileAspect: Float
) {
    BoardCaptionText(
        text = title,
        color = BoardTheme.colors.textPrimary
    )
    items.chunked(columns).forEach { row ->
        IntentTileRow(row = row, columns = columns, tileAspect = tileAspect)
    }
}

@Composable
private fun IntentTileRow(
    row: List<CreateIntent>,
    columns: Int,
    tileAspect: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PixiSpacing.sm)
    ) {
        row.forEach { intent ->
            BoardIntentTile(
                title = intent.title,
                subtitle = intent.subtitle,
                accent = intent.accent,
                imageRes = intent.imageRes,
                onClick = intent.onClick,
                aspectRatio = tileAspect,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (intent.testTag != null) {
                            Modifier.testTag(intent.testTag)
                        } else {
                            Modifier
                        }
                    )
            )
        }
        repeat(columns - row.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
