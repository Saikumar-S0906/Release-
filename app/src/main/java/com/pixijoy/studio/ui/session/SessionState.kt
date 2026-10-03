package com.pixijoy.studio.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.Timeline
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.decodeLayers
import com.pixijoy.photo.encodeLayers
import com.pixijoy.photo.layerName
import com.pixijoy.photo.withBackgroundSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val GRAPH_SAVE_DEBOUNCE_MS = 400L
internal const val MOVIE_HISTORY_LIMIT = 50

fun interface GraphPersistence {
    suspend fun persist(graph: String)
}

interface ScheduledGraphSave {
    fun cancel()
    suspend fun join()
}

fun interface GraphSaveScheduler {
    fun schedule(delayMs: Long, action: suspend () -> Unit): ScheduledGraphSave
}

internal class CoroutineGraphSaveScheduler(
    private val scope: CoroutineScope
) : GraphSaveScheduler {
    override fun schedule(delayMs: Long, action: suspend () -> Unit): ScheduledGraphSave {
        val job = scope.launch {
            delay(delayMs)
            action()
        }
        return JobGraphSave(job)
    }

    private class JobGraphSave(private val job: Job) : ScheduledGraphSave {
        override fun cancel() = job.cancel()
        override suspend fun join() = job.join()
    }
}

/** What the session chrome may say about the on-disk graph. */
enum class GraphSaveStatus {
    /** The latest announced write landed, or no edit is waiting. */
    Saved,
    /** A user edit is debounced or a write is in flight. */
    Saving,
    /** The latest write failed. The graph stays pending for the next flush. */
    Failed
}

/**
 * Keeps only the newest graph until the debounce expires. [flush] cancels and joins the
 * pending task before synchronously persisting the newest graph.
 *
 * [submit] with `quiet = true` still writes, but does not leave [GraphSaveStatus.Saved].
 * That is the open-session rewrite of a graph that is already on disk.
 */
internal class DebouncedGraphSaver(
    private val persistence: GraphPersistence,
    private val scheduler: GraphSaveScheduler,
    private val delayMs: Long = GRAPH_SAVE_DEBOUNCE_MS
) {
    private val lock = Any()
    private var generation = 0L
    private var pendingGraph: String? = null
    private var scheduled: ScheduledGraphSave? = null
    private val flushMutex = Mutex()
    private val _status = MutableStateFlow(GraphSaveStatus.Saved)
    val status: StateFlow<GraphSaveStatus> = _status

    fun submit(graph: String, quiet: Boolean = false) {
        val previous: ScheduledGraphSave?
        val token: Long
        synchronized(lock) {
            pendingGraph = graph
            generation += 1
            token = generation
            previous = scheduled
            scheduled = null
        }
        previous?.cancel()
        if (!quiet) _status.value = GraphSaveStatus.Saving

        val next = scheduler.schedule(delayMs) { persistIfCurrent(token) }
        synchronized(lock) {
            if (generation == token) {
                scheduled = next
            } else {
                next.cancel()
            }
        }
    }

    suspend fun flush() = flushMutex.withLock {
        val task: ScheduledGraphSave?
        val graph: String?
        val token: Long
        synchronized(lock) {
            generation += 1
            token = generation
            task = scheduled
            scheduled = null
            graph = pendingGraph
        }
        task?.cancel()
        task?.join()
        if (graph == null) return@withLock
        _status.value = GraphSaveStatus.Saving
        val ok = write(graph)
        finish(token, graph, ok)
    }

    private suspend fun persistIfCurrent(token: Long) {
        val graph = synchronized(lock) {
            if (generation == token) pendingGraph else null
        } ?: return
        val ok = write(graph)
        finish(token, graph, ok)
    }

    private suspend fun write(graph: String): Boolean =
        runCatching { persistence.persist(graph) }.isSuccess

    private fun finish(token: Long, graph: String, ok: Boolean) {
        synchronized(lock) {
            if (generation != token || pendingGraph != graph) return
            scheduled = null
            if (ok) {
                pendingGraph = null
                _status.value = GraphSaveStatus.Saved
            } else {
                _status.value = GraphSaveStatus.Failed
            }
        }
    }
}

data class PhotoSessionState(
    val layers: List<LayeredOp>,
    val redo: List<LayeredOp> = emptyList()
)

/**
 * Layers hub opens on one real Background layer when the stored graph has none.
 * That layer is an identity grade composited on the decoded source bitmap
 * (sample or import). It is not a UI stub. Other tools stay empty until Layers mode.
 */
internal fun initialSessionLayers(
    initialGraph: String,
    toolKey: String,
    projectId: String
): List<LayeredOp> {
    val decoded = initialGraph.decodeLayers()
    if (decoded.isNotEmpty() || defaultMode(toolKey) != SessionMode.Layers) return decoded
    return decoded.withBackgroundSeed(id = backgroundLayerId(projectId))
}

internal fun backgroundLayerId(projectId: String): String =
    if (projectId.isBlank()) "L-bg" else "L-bg-$projectId"

open class PhotoSessionViewModel(
    initialGraph: String,
    persistence: GraphPersistence,
    scheduler: GraphSaveScheduler? = null,
    toolKey: String = "",
    projectId: String = ""
) : ViewModel() {
    internal val toolModeState = mutableStateOf(defaultMode(toolKey))
    internal var toolMode by toolModeState
    internal val toolGroupState = mutableStateOf(groupForTool(toolKey))
    internal var toolGroup by toolGroupState
    internal val selectedLayerState = mutableStateOf<String?>(null)
    internal var selectedLayerId by selectedLayerState
    internal var toolEntryId: Any? = null
    internal var boundToolKey: String? = null
    internal val chromeEpochState = mutableIntStateOf(0)
    internal var chromeEpoch by chromeEpochState
    internal val controls = PhotoSessionControls(toolKey)
    internal val preview = PhotoSessionPreview(controls)

    /**
     * Soft-resets tool chrome when the session surface is new (entry, rotation)
     * or the hub tool key changed. The edit graph is left alone.
     * Returns true when chrome was reset.
     */
    internal fun syncToolChrome(nextToolKey: String, surfaceId: Any): Boolean {
        if (toolEntryId === surfaceId && boundToolKey == nextToolKey) return false
        toolEntryId = surfaceId
        bindTool(nextToolKey)
        return true
    }

    /** Fresh screen entry (including rotation and tool switch) picks slider defaults again. */
    internal fun bindTool(nextToolKey: String) {
        toolMode = defaultMode(nextToolKey)
        toolGroup = groupForTool(nextToolKey)
        selectedLayerId = null
        controls.reset(nextToolKey)
        boundToolKey = nextToolKey
        chromeEpoch += 1
    }

    /** Group rail. Shared grade stays; the active mode's drafts do not leak into another tool. */
    internal fun selectGroup(next: PhotoGroup, toolKey: String) {
        val nextMode = if (modeBelongs(next, toolMode)) toolMode else defaultModeFor(next, toolKey)
        toolGroup = next
        selectMode(nextMode)
    }

    /** Chip row inside the current group. */
    internal fun selectMode(next: SessionMode) {
        if (toolMode == next) return
        controls.abandonModeDrafts(toolMode, next)
        toolMode = next
    }

    override fun onCleared() {
        controls.releaseBitmaps()
        super.onCleared()
    }

    private val saver = DebouncedGraphSaver(
        persistence = persistence,
        scheduler = scheduler ?: CoroutineGraphSaveScheduler(viewModelScope)
    )
    val saveStatus: StateFlow<GraphSaveStatus> = saver.status

    private val _state = MutableStateFlow(
        PhotoSessionState(layers = initialSessionLayers(initialGraph, toolKey, projectId))
    )
    val state: StateFlow<PhotoSessionState> = _state

    init {
        saver.submit(_state.value.layers.encodeLayers(), quiet = true)
    }

    internal fun pushOp(op: PhotoOp) {
        val layer = LayeredOp(
            id = "L${System.currentTimeMillis()}",
            name = op.layerName(),
            op = op
        )
        push(layer)
        selectedLayerId = layer.id
        controls.status = null
        controls.statusIsError = false
        controls.pendingStamps = emptyList()
        controls.lastStamp = null
        controls.pickingHealSource = false
    }

    /**
     * Switching into Layers with an empty stack gets the same Background layer
     * the hub already has from [initialSessionLayers]. A non-empty stack is left alone
     * so deleting the base layer stays deleted until the mode changes.
     */
    internal fun seedLayers(projectId: String) {
        if (toolMode != SessionMode.Layers) return
        val layers = _state.value.layers
        if (layers.isNotEmpty()) return
        val seeded = layers.withBackgroundSeed(id = backgroundLayerId(projectId))
        replaceLayers(seeded)
        selectedLayerId = seeded.first().id
    }

    internal fun applyGrade() = preview.applyGrade(::pushOp)

    internal fun applyCurves(ys: FloatArray) = preview.applyCurves(ys, ::pushOp)

    internal fun applySelective() = preview.applySelective(::pushOp)

    internal fun applyLook(key: String) = preview.applyLook(key, ::pushOp)

    fun push(layer: LayeredOp) {
        val current = _state.value
        update(current.copy(layers = current.layers + layer, redo = emptyList()))
    }

    fun replaceLayers(layers: List<LayeredOp>, clearRedo: Boolean = true) {
        val current = _state.value
        if (layers == current.layers && (!clearRedo || current.redo.isEmpty())) return
        update(
            current.copy(
                layers = layers,
                redo = if (clearRedo) emptyList() else current.redo
            )
        )
    }

    fun undo() {
        val current = _state.value
        val layer = current.layers.lastOrNull() ?: return
        update(
            current.copy(
                layers = current.layers.dropLast(1),
                redo = current.redo + layer
            )
        )
    }

    fun redo() {
        val current = _state.value
        val layer = current.redo.lastOrNull() ?: return
        update(
            current.copy(
                layers = current.layers + layer,
                redo = current.redo.dropLast(1)
            )
        )
    }

    suspend fun flush() = saver.flush()

    fun flushInBackground() {
        viewModelScope.launch { saver.flush() }
    }

    private fun update(next: PhotoSessionState) {
        _state.value = next
        saver.submit(next.layers.encodeLayers())
    }
}

internal class BoundedUndoHistory<T>(
    private val limit: Int
) {
    private val undo = ArrayDeque<T>()
    private val redo = ArrayDeque<T>()

    init {
        require(limit > 0)
    }

    val undoSize: Int get() = undo.size
    val redoSize: Int get() = redo.size

    fun record(previous: T) {
        if (undo.lastOrNull() != previous) {
            undo.addLast(previous)
            while (undo.size > limit) undo.removeFirst()
        }
        redo.clear()
    }

    fun undo(current: T): T? {
        val previous = undo.removeLastOrNull() ?: return null
        redo.addLast(current)
        return previous
    }

    fun redo(current: T): T? {
        val next = redo.removeLastOrNull() ?: return null
        undo.addLast(current)
        while (undo.size > limit) undo.removeFirst()
        return next
    }
}

data class MovieSessionState(
    val multi: MultiTrackTimeline,
    val previewEpoch: Int = 0,
    val undoCount: Int = 0,
    val redoCount: Int = 0
) {
    val timeline: Timeline get() = multi.toFlatTimeline()
    val canUndo: Boolean get() = undoCount > 0
    val canRedo: Boolean get() = redoCount > 0
}

open class MovieSessionViewModel(
    initialTimeline: MultiTrackTimeline,
    persistence: GraphPersistence,
    scheduler: GraphSaveScheduler? = null,
    historyLimit: Int = MOVIE_HISTORY_LIMIT
) : ViewModel() {
    private val seededTimeline = initialTimeline.ensureDefaultTracks()
    internal val movieGroupState = mutableStateOf(MovieGroup.Edit)
    internal var movieGroup by movieGroupState
    internal val effectSubState = mutableStateOf(MovieEffectSub.Motion)
    internal var effectSub by effectSubState
    internal val selectedClipState = mutableStateOf(
        seededTimeline.toFlatTimeline().clips.firstOrNull()?.id
    )
    internal var selectedClipId by selectedClipState
    internal val selectedTitleState = mutableStateOf<String?>(null)
    internal var selectedTitleId by selectedTitleState
    internal var toolEntryId: Any? = null
    internal var boundToolKey: String? = null
    internal val chromeEpochState = mutableIntStateOf(0)
    internal var chromeEpoch by chromeEpochState
    internal val controls = MovieSessionControls()

    /**
     * Soft-resets tool chrome when the session surface is new (entry, rotation)
     * or the hub tool key changed. The timeline is left alone.
     * Returns true when chrome was reset.
     */
    internal fun syncToolChrome(toolKey: String, surfaceId: Any): Boolean {
        if (toolEntryId === surfaceId && boundToolKey == toolKey) return false
        toolEntryId = surfaceId
        boundToolKey = toolKey
        resetToolChrome()
        return true
    }

    /** Fresh screen entry returns to Edit / first clip and slider defaults. */
    internal fun resetToolChrome() {
        movieGroup = MovieGroup.Edit
        effectSub = MovieEffectSub.Motion
        selectedTitleId = null
        selectedClipId = _state.value.timeline.clips.firstOrNull()?.id
        controls.reset()
        chromeEpoch += 1
    }
    private val saver = DebouncedGraphSaver(
        persistence = persistence,
        scheduler = scheduler ?: CoroutineGraphSaveScheduler(viewModelScope)
    )
    val saveStatus: StateFlow<GraphSaveStatus> = saver.status
    private val history = BoundedUndoHistory<MultiTrackTimeline>(historyLimit)
    private var gestureStart: MultiTrackTimeline? = null
    private val _state = MutableStateFlow(
        MovieSessionState(multi = seededTimeline)
    )
    val state: StateFlow<MovieSessionState> = _state

    init {
        saver.submit(_state.value.multi.encode(), quiet = true)
    }

    fun setTimeline(timeline: Timeline, live: Boolean = false) {
        setMulti(_state.value.multi.withPrimaryFlat(timeline), live)
    }

    fun setMulti(timeline: MultiTrackTimeline, live: Boolean = false) {
        val current = _state.value
        val next = timeline.ensureDefaultTracks()
        if (next == current.multi) return

        if (live) {
            if (gestureStart == null) gestureStart = current.multi
        } else {
            history.record(gestureStart ?: current.multi)
            gestureStart = null
        }
        publish(
            next = next,
            previewEpoch = current.previewEpoch + if (live) 0 else 1,
            persist = !live
        )
    }

    /**
     * Rebuilds the preview if a live drag is still open.
     * A no-op when nothing is pending, so Play does not restart a settled composition.
     */
    fun settleOpenGesture() {
        if (gestureStart == null) return
        settleGesture()
    }

    /** Commits all live updates since the prior settle as one undo entry. */
    fun settleGesture() {
        val current = _state.value
        var changed = false
        gestureStart?.let { start ->
            if (start != current.multi) {
                history.record(start)
                changed = true
            }
        }
        gestureStart = null
        _state.value = current.copy(
            previewEpoch = current.previewEpoch + 1,
            undoCount = history.undoSize,
            redoCount = history.redoSize
        )
        if (changed) saver.submit(current.multi.encode())
    }

    fun undo() {
        finishInterruptedGesture()
        val current = _state.value
        val previous = history.undo(current.multi) ?: return
        publish(previous, current.previewEpoch + 1)
    }

    fun redo() {
        finishInterruptedGesture()
        val current = _state.value
        val next = history.redo(current.multi) ?: return
        publish(next, current.previewEpoch + 1)
    }

    suspend fun flush() {
        if (gestureStart != null) settleGesture()
        saver.flush()
    }

    fun flushInBackground() {
        viewModelScope.launch { flush() }
    }

    internal fun notePlaybackPosition(positionMs: Long, totalMs: Long) {
        val total = totalMs.coerceAtLeast(1)
        controls.playhead = (positionMs / total.toFloat()).coerceIn(0f, 1f)
    }

    /** Pauses transport and returns the frame the player should seek to. */
    internal fun stepPlayhead(delta: Int, fps: Float, totalMs: Long): Long {
        controls.playing = false
        val total = totalMs.coerceAtLeast(1)
        val now = (controls.playhead * total).toLong()
        return com.pixijoy.movie.MovieFrame.step(now, delta, fps, total)
    }

    internal fun confirmSteppedPlayhead(nextMs: Long, totalMs: Long) {
        val total = totalMs.coerceAtLeast(1)
        controls.playhead = (nextMs / total.toFloat()).coerceIn(0f, 1f)
    }

    internal fun togglePlaying(): Boolean {
        controls.playing = !controls.playing
        return controls.playing
    }

    internal fun setPlayhead(fraction: Float) {
        controls.playhead = fraction
    }

    internal fun setTrimStart(ms: Long) {
        controls.startMs = ms
    }

    internal fun setTrimEnd(ms: Long) {
        controls.endMs = ms
    }

    internal fun setSpeed(value: Float) {
        controls.speed = value
    }

    internal fun beginExport(message: String) {
        controls.exporting = true
        controls.progress = 0f
        controls.status = message
        controls.exportSuccessUri = null
    }

    internal fun onExportRunning(progress: Float, message: String) {
        controls.exporting = true
        controls.progress = progress
        controls.status = message
    }

    internal fun onExportSucceeded(uri: String?, message: String) {
        if (!controls.exporting) return
        controls.exporting = false
        controls.exportSuccessUri = uri
        controls.showExport = true
        controls.status = message
    }

    internal fun onExportFailed(message: String) {
        if (!controls.exporting) return
        controls.exporting = false
        controls.status = message
    }

    private fun finishInterruptedGesture() {
        val current = _state.value
        gestureStart?.let { start ->
            if (start != current.multi) history.record(start)
        }
        gestureStart = null
    }

    private fun publish(
        next: MultiTrackTimeline,
        previewEpoch: Int,
        persist: Boolean = true
    ) {
        _state.value = MovieSessionState(
            multi = next,
            previewEpoch = previewEpoch,
            undoCount = history.undoSize,
            redoCount = history.redoSize
        )
        if (persist) saver.submit(next.encode())
    }
}

