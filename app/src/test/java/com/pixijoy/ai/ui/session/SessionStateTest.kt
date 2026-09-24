package com.pixijoy.ai.ui.session

import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.TimelineClip
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.PhotoOp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateTest {
    @Test
    fun debounce_coalescesToNewestGraph() = runBlocking {
        val scheduler = ManualGraphSaveScheduler()
        val writes = mutableListOf<String>()
        val saver = DebouncedGraphSaver(
            persistence = GraphPersistence { writes += it },
            scheduler = scheduler
        )

        saver.submit("one")
        saver.submit("two")

        assertTrue(writes.isEmpty())
        scheduler.runPending()
        assertEquals(listOf("two"), writes)
    }

    @Test
    fun debounce_flushPersistsNewestGraphAndCancelsTimer() = runBlocking {
        val scheduler = ManualGraphSaveScheduler()
        val writes = mutableListOf<String>()
        val saver = DebouncedGraphSaver(
            persistence = GraphPersistence { writes += it },
            scheduler = scheduler
        )

        saver.submit("one")
        saver.submit("latest")
        saver.flush()
        scheduler.runPending()

        assertEquals(listOf("latest"), writes)
    }

    @Test
    fun photoState_ownsUndoRedoAndPersistedGraph() = runBlocking {
        val scheduler = ManualGraphSaveScheduler()
        val writes = mutableListOf<String>()
        val viewModel = PhotoSessionViewModel(
            initialGraph = "",
            persistence = GraphPersistence { writes += it },
            scheduler = scheduler
        )
        val layer = LayeredOp("L1", "Grade", op = PhotoOp.Grade(0.5f, 1f))

        viewModel.push(layer)
        assertEquals(listOf(layer), viewModel.state.value.layers)
        viewModel.undo()
        assertTrue(viewModel.state.value.layers.isEmpty())
        assertEquals(listOf(layer), viewModel.state.value.redo)
        viewModel.redo()
        viewModel.flush()

        assertEquals(listOf(layer), viewModel.state.value.layers)
        assertTrue(writes.single().contains("L1"))
    }

    @Test
    fun movieHistory_coalescesLiveTicksIntoOneEntry() {
        val initial = timeline(volume = 1f)
        val scheduler = ManualGraphSaveScheduler()
        val writes = mutableListOf<String>()
        val viewModel = MovieSessionViewModel(
            initialTimeline = initial,
            persistence = GraphPersistence { writes += it },
            scheduler = scheduler
        )
        runBlocking { scheduler.runPending() }
        val initialWriteCount = writes.size

        viewModel.setTimeline(initial.toFlatTimeline().copy(
            clips = initial.toFlatTimeline().clips.map { it.copy(volume = 0.8f) }
        ), live = true)
        viewModel.setTimeline(viewModel.state.value.timeline.copy(
            clips = viewModel.state.value.timeline.clips.map { it.copy(volume = 0.4f) }
        ), live = true)

        assertFalse(viewModel.state.value.canUndo)
        assertEquals(0, viewModel.state.value.previewEpoch)
        runBlocking { scheduler.runPending() }
        assertEquals(initialWriteCount, writes.size)
        viewModel.settleGesture()
        assertTrue(viewModel.state.value.canUndo)
        assertEquals(1, viewModel.state.value.previewEpoch)
        runBlocking { scheduler.runPending() }
        assertEquals(initialWriteCount + 1, writes.size)

        viewModel.undo()
        assertEquals(1f, viewModel.state.value.timeline.clips.single().volume)
        assertTrue(viewModel.state.value.canRedo)
        assertEquals(2, viewModel.state.value.previewEpoch)

        viewModel.redo()
        assertEquals(0.4f, viewModel.state.value.timeline.clips.single().volume)
        assertEquals(3, viewModel.state.value.previewEpoch)
    }

    @Test
    fun boundedHistory_discardsOldestEntries() {
        val history = BoundedUndoHistory<Int>(limit = 2)
        history.record(1)
        history.record(2)
        history.record(3)

        assertEquals(3, history.undo(4))
        assertEquals(2, history.undo(3))
        assertEquals(null, history.undo(2))
    }

    private fun timeline(volume: Float): MultiTrackTimeline =
        MultiTrackTimeline.fromSingleVideo(
            listOf(
                TimelineClip(
                    id = "c1",
                    mediaIndex = 0,
                    inMs = 0,
                    outMs = 1_000,
                    volume = volume
                )
            )
        )

    private class ManualGraphSaveScheduler : GraphSaveScheduler {
        private data class Task(
            val action: suspend () -> Unit,
            var cancelled: Boolean = false,
            var complete: Boolean = false
        )

        private val tasks = mutableListOf<Task>()

        override fun schedule(
            delayMs: Long,
            action: suspend () -> Unit
        ): ScheduledGraphSave {
            val task = Task(action)
            tasks += task
            return object : ScheduledGraphSave {
                override fun cancel() {
                    task.cancelled = true
                }

                override suspend fun join() = Unit
            }
        }

        suspend fun runPending() {
            tasks.filterNot { it.cancelled || it.complete }.forEach {
                it.complete = true
                it.action()
            }
        }
    }
}
