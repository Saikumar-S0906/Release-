package com.pixijoy.ai.ui.board

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pixijoy.ai.R
import com.pixijoy.ai.data.pixiJoyPrefsDataStore
import com.pixijoy.ai.ui.board.components.BoardFlowArt
import com.pixijoy.ai.ui.board.ctools.BoardCTool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Last Create-Tools destination the user opened. */
data class BoardLastTool(
    val screenId: String,
    @StringRes val titleRes: Int,
    val canvasKey: String
)

/** One saved still/clip for Home's last-three row. */
data class BoardRecentCreation(
    val uri: String,
    @DrawableRes val fallbackRes: Int,
    @StringRes val titleRes: Int,
    val openScreenId: String,
    val timestamp: Long
)

/**
 * Process-local + DataStore memory: last tool, last three gallery saves.
 * No sixth tab. No marketplace. Survives process death via [pixiJoyPrefsDataStore].
 */
object BoardMemory {
    private const val MAX_CREATIONS = 3
    private const val RS = '\u001e'
    private const val FS = '\u001f'

    private object Keys {
        val LAST_TOOL = stringPreferencesKey("board_mem_last_tool")
        val CREATIONS = stringPreferencesKey("board_mem_creations")
    }

    private val mutex = Mutex()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var hydrated = false

    private var lastTool: BoardLastTool? = null
    private val creations = mutableListOf<BoardRecentCreation>()

    var revision by mutableIntStateOf(0)
        private set

    private fun bump() {
        revision++
    }

    fun lastTool(): BoardLastTool? = lastTool

    fun lastCreations(): List<BoardRecentCreation> = creations.toList()

    @Composable
    fun observeRevision(): Int {
        val context = LocalContext.current
        val rev = revision
        LaunchedEffect(Unit) { hydrate(context) }
        return rev
    }

    suspend fun hydrate(context: Context) {
        mutex.withLock {
            if (hydrated) return
            val snap = context.applicationContext.pixiJoyPrefsDataStore.data.first()
            lastTool = parseTool(snap[Keys.LAST_TOOL])
            creations.clear()
            creations.addAll(parseCreations(snap[Keys.CREATIONS]))
            hydrated = true
            bump()
        }
    }

    /**
     * Remember a Tools-segment job. Skips Home/Create hub, AI, processing, and pickers.
     */
    suspend fun rememberTool(context: Context, screenId: String) {
        val tool = resolveTool(screenId) ?: return
        hydrate(context)
        mutex.withLock {
            if (lastTool?.screenId == tool.screenId) return
            lastTool = tool
            persist(context.applicationContext)
            bump()
        }
    }

    fun rememberCreationAsync(context: Context, uri: Uri, filePrefix: String) {
        val app = context.applicationContext
        ioScope.launch {
            rememberCreation(app, uri.toString(), filePrefix)
            BoardNavPrefs.markUserCreations(app)
        }
    }

    suspend fun rememberCreation(context: Context, uri: String, filePrefix: String) {
        if (uri.isBlank()) return
        hydrate(context)
        mutex.withLock {
            val tool = lastTool ?: toolFromPrefix(filePrefix)
            val entry = BoardRecentCreation(
                uri = uri,
                fallbackRes = BoardFlowArt.creationThumb,
                titleRes = tool.titleRes,
                openScreenId = tool.screenId,
                timestamp = System.currentTimeMillis()
            )
            creations.removeAll { it.uri == uri }
            creations.add(0, entry)
            while (creations.size > MAX_CREATIONS) creations.removeAt(creations.lastIndex)
            persist(context.applicationContext)
            bump()
        }
    }

    private suspend fun persist(context: Context) {
        context.pixiJoyPrefsDataStore.edit { prefs ->
            val tool = lastTool
            if (tool == null) {
                prefs.remove(Keys.LAST_TOOL)
            } else {
                prefs[Keys.LAST_TOOL] =
                    listOf(tool.screenId, tool.titleRes.toString(), tool.canvasKey)
                        .joinToString(FS.toString())
            }
            prefs[Keys.CREATIONS] = creations.joinToString(RS.toString()) { c ->
                listOf(
                    c.uri,
                    c.fallbackRes.toString(),
                    c.titleRes.toString(),
                    c.openScreenId,
                    c.timestamp.toString()
                ).joinToString(FS.toString())
            }
        }
    }

    private fun parseTool(raw: String?): BoardLastTool? {
        if (raw.isNullOrBlank()) return null
        val bits = raw.split(FS)
        if (bits.size < 3) return null
        val title = bits[1].toIntOrNull() ?: return null
        return BoardLastTool(screenId = bits[0], titleRes = title, canvasKey = bits[2])
    }

    private fun parseCreations(raw: String?): List<BoardRecentCreation> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RS).mapNotNull { part ->
            val bits = part.split(FS)
            if (bits.size < 5) return@mapNotNull null
            BoardRecentCreation(
                uri = bits[0],
                fallbackRes = bits[1].toIntOrNull() ?: BoardFlowArt.creationThumb,
                titleRes = bits[2].toIntOrNull() ?: R.string.board_f2_recents,
                openScreenId = bits[3],
                timestamp = bits[4].toLongOrNull() ?: 0L
            )
        }
    }

    internal fun resolveTool(screenId: String): BoardLastTool? {
        if (screenId.isBlank()) return null
        if (SKIP_ANY.any { screenId.contains(it, ignoreCase = true) }) return null
        if (screenId == BoardProductionTabs.HOME ||
            screenId == BoardProductionTabs.HOME_EMPTY ||
            screenId == BoardProductionTabs.CREATE ||
            screenId == BoardProductionTabs.DISCOVER ||
            screenId == BoardProductionTabs.COMMUNITY ||
            screenId == BoardProductionTabs.MY_STUFF
        ) {
            return null
        }
        BoardCTool.fromScreenId(screenId)?.let { c ->
            return BoardLastTool(c.prefix, c.titleRes, canvasKeyForCTool(c))
        }
        return when {
            screenId.startsWith("b15_") ->
                BoardLastTool("b15_open", R.string.flow15_title, "editor")
            screenId.startsWith("b13_") ->
                BoardLastTool("b13_photo", R.string.create_stickers, "sticker")
            screenId.startsWith("b16_") ->
                BoardLastTool("b16_select", R.string.board_f16_create_hub, "collage")
            screenId.startsWith("b17_") ->
                BoardLastTool("b17_open", R.string.board_f17_export_share, "export")
            screenId.startsWith("b14_") ->
                BoardLastTool("b14_input", R.string.board_f14_multi_sub, "move")
            screenId == "b3_video" || screenId.startsWith("b3_gif") ->
                BoardLastTool("b3_video", R.string.board_f17_video, "gif")
            else -> null
        }
    }

    private fun toolFromPrefix(filePrefix: String): BoardLastTool {
        val p = filePrefix.lowercase()
        return when {
            "restore" in p -> BoardLastTool("b_c05", R.string.create_extra_restore, "restore")
            "bg" in p || "cutout" in p ->
                BoardLastTool("b_c04", R.string.create_extra_remove_bg, "cutout")
            "sticker" in p -> BoardLastTool("b13_photo", R.string.create_stickers, "sticker")
            "edit" in p -> BoardLastTool("b15_open", R.string.flow15_title, "editor")
            "collage" in p -> BoardLastTool("b16_select", R.string.board_f16_create_hub, "collage")
            "export" in p -> BoardLastTool("b17_open", R.string.board_f17_export_share, "export")
            "motion" in p || "talking" in p ->
                BoardLastTool("b14_input", R.string.board_f14_multi_sub, "move")
            "greeting" in p -> BoardLastTool("b_c12", R.string.create_extra_greeting, "greeting")
            "expand" in p -> BoardLastTool("b_c10", R.string.create_extra_expand, "expand")
            "portrait" in p -> BoardLastTool("b_c09", R.string.create_extra_portrait, "portrait")
            "people" in p -> BoardLastTool("b_c08", R.string.create_extra_people, "people")
            "scene" in p -> BoardLastTool("b_c07", R.string.create_extra_scene, "scene")
            "gif" in p -> BoardLastTool("b3_video", R.string.board_f17_video, "gif")
            else -> lastTool ?: BoardLastTool(
                BoardProductionTabs.MY_STUFF,
                R.string.board_f2_recents,
                "editor"
            )
        }
    }

    private fun canvasKeyForCTool(tool: BoardCTool): String = when (tool) {
        BoardCTool.RemoveBg -> "cutout"
        BoardCTool.Restore -> "restore"
        BoardCTool.Expand -> "expand"
        BoardCTool.Talking -> "talking"
        BoardCTool.Portrait -> "portrait"
        BoardCTool.Greeting -> "greeting"
        BoardCTool.Remix -> "remix"
        BoardCTool.People -> "people"
        BoardCTool.Scene -> "scene"
        BoardCTool.FaceSwap -> "editor"
    }

    private val SKIP_ANY = listOf(
        "_processing",
        "generating",
        "_creating",
        "exporting",
        "_offline",
        "_low_credits",
        "_safety",
        "b3_gallery",
        "b3_camera",
        "b3_chooser",
        "b3_preview",
        "b3_open",
        "b5_",
        "b6_",
        "b7_",
        "b_diff",
        "b_different"
    )
}

@Composable
fun rememberBoardMemoryRevision(): Int = BoardMemory.observeRevision()
