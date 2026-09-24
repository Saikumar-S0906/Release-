package com.pixijoy.ai.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.Timeline
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.decodeLayers
import com.pixijoy.photo.encodeLayers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/**
 * Keeps only the newest graph until the debounce expires. [flush] cancels and joins the
 * pending task before synchronously persisting the newest graph.
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

    fun submit(graph: String) {
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
        graph?.let { persistence.persist(it) }
        synchronized(lock) {
            if (generation == token && pendingGraph == graph) {
                pendingGraph = null
            }
        }
    }

    private suspend fun persistIfCurrent(token: Long) {
        val graph = synchronized(lock) {
            if (generation == token) pendingGraph else null
        }
        graph?.let { persistence.persist(it) } ?: return
        synchronized(lock) {
            if (generation == token && pendingGraph == graph) {
                pendingGraph = null
                scheduled = null
            }
        }
    }
}

data class PhotoSessionState(
    val layers: List<LayeredOp>,
    val redo: List<LayeredOp> = emptyList()
)

class PhotoSessionViewModel(
    initialGraph: String,
    persistence: GraphPersistence,
    scheduler: GraphSaveScheduler? = null
) : ViewModel() {
    private val saver = DebouncedGraphSaver(
        persistence = persistence,
        scheduler = scheduler ?: CoroutineGraphSaveScheduler(viewModelScope)
    )

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(
        PhotoSessionState(layers = initialGraph.decodeLayers())
    )
    val state: kotlinx.coroutines.flow.StateFlow<PhotoSessionState> = _state

    init {
        saver.submit(_state.value.layers.encodeLayers())
    }

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

class MovieSessionViewModel(
    initialTimeline: MultiTrackTimeline,
    persistence: GraphPersistence,
    scheduler: GraphSaveScheduler? = null,
    historyLimit: Int = MOVIE_HISTORY_LIMIT
) : ViewModel() {
    private val saver = DebouncedGraphSaver(
        persistence = persistence,
        scheduler = scheduler ?: CoroutineGraphSaveScheduler(viewModelScope)
    )
    private val history = BoundedUndoHistory<MultiTrackTimeline>(historyLimit)
    private var gestureStart: MultiTrackTimeline? = null
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(
        MovieSessionState(multi = initialTimeline.ensureDefaultTracks())
    )
    val state: kotlinx.coroutines.flow.StateFlow<MovieSessionState> = _state

    init {
        saver.submit(_state.value.multi.encode())
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

internal fun <T : ViewModel> sessionViewModelFactory(
    create: () -> T
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
