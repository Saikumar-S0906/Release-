package com.pixijoy.ai.ui.session

import com.pixijoy.movie.MultiTrackTimeline
import com.pixijoy.movie.TimelineClip
import com.pixijoy.photo.ExportFormat
import com.pixijoy.photo.LayeredOp
import com.pixijoy.photo.PhotoOp
import com.pixijoy.photo.Stamp
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
    fun photoToolSelection_startsFromToolKey() {
        val viewModel = PhotoSessionViewModel(
            initialGraph = "",
            persistence = GraphPersistence { },
            scheduler = ManualGraphSaveScheduler(),
            toolKey = "layers"
        )
        assertEquals(SessionMode.Layers, viewModel.toolMode)
        assertEquals(PhotoGroup.Compose, viewModel.toolGroup)
        viewModel.toolMode = SessionMode.Mask
        viewModel.selectedLayerId = "L1"
        assertEquals(SessionMode.Mask, viewModel.toolMode)
        assertEquals("L1", viewModel.selectedLayerId)
        viewModel.bindTool("restore")
        assertEquals(SessionMode.Heal, viewModel.toolMode)
        assertEquals(PhotoGroup.Retouch, viewModel.toolGroup)
        assertEquals(null, viewModel.selectedLayerId)
        viewModel.controls.exposure = 0.8f
        viewModel.controls.format = ExportFormat.PNG
        viewModel.bindTool("layers")
        assertEquals(0f, viewModel.controls.exposure)
        assertEquals(ExportFormat.JPEG, viewModel.controls.format)
        viewModel.bindTool("cutout")
        assertEquals(ExportFormat.PNG, viewModel.controls.format)
        viewModel.controls.exposure = 0.4f
        viewModel.controls.contrast = 1.2f
        viewModel.controls.sharpen = 0.5f
        val pushed = mutableListOf<PhotoOp>()
        viewModel.preview.applyGrade { pushed += it }
        assertEquals(0f, viewModel.controls.exposure)
        assertEquals(1f, viewModel.controls.contrast)
        assertEquals(0f, viewModel.controls.sharpen)
        assertTrue(pushed.any { it is PhotoOp.Grade })
        assertTrue(pushed.any { it is PhotoOp.Sharpen })
        viewModel.controls.exposure = 0f
        viewModel.controls.contrast = 1f
        viewModel.controls.hue = 0f
        viewModel.controls.saturation = 1f
        val identity = mutableListOf<PhotoOp>()
        viewModel.preview.applyGrade { identity += it }
        assertTrue(identity.isEmpty())
        assertFalse(viewModel.preview.canCompare(SessionMode.Adjust, emptyList()))
    }

    @Test
    fun photoChrome_resetsOnEntryRotationAndToolSwitch_keepsGraph() {
        val viewModel = PhotoSessionViewModel(
            initialGraph = "",
            persistence = GraphPersistence { },
            scheduler = ManualGraphSaveScheduler(),
            toolKey = "editor"
        )
        val layer = LayeredOp("L1", "Grade", op = PhotoOp.Grade(0.2f, 1f))
        viewModel.push(layer)
        viewModel.controls.exposure = 0.6f
        viewModel.controls.pendingStamps = listOf(Stamp(0.2f, 0.3f, 0.05f))
        viewModel.toolMode = SessionMode.Heal
        viewModel.toolGroup = PhotoGroup.Retouch

        val surface = Any()
        assertTrue(viewModel.syncToolChrome("editor", surface))
        assertEquals(1, viewModel.chromeEpoch)
        assertEquals(SessionMode.Crop, viewModel.toolMode)
        assertEquals(PhotoGroup.Crop, viewModel.toolGroup)
        assertEquals(0f, viewModel.controls.exposure)
        assertTrue(viewModel.controls.pendingStamps.isEmpty())
        assertEquals(listOf(layer), viewModel.state.value.layers)

        viewModel.controls.exposure = 0.4f
        assertFalse(viewModel.syncToolChrome("editor", surface))
        assertEquals(1, viewModel.chromeEpoch)
        assertEquals(0.4f, viewModel.controls.exposure)

        assertTrue(viewModel.syncToolChrome("editor", Any()))
        assertEquals(2, viewModel.chromeEpoch)
        assertEquals(0f, viewModel.controls.exposure)
        assertEquals(listOf(layer), viewModel.state.value.layers)

        viewModel.controls.exposure = 0.7f
        assertTrue(viewModel.syncToolChrome("cutout", surface))
        assertEquals(SessionMode.Erase, viewModel.toolMode)
        assertEquals(PhotoGroup.Retouch, viewModel.toolGroup)
        assertEquals(ExportFormat.PNG, viewModel.controls.format)
        assertEquals(0f, viewModel.controls.exposure)
        assertEquals(listOf(layer), viewModel.state.value.layers)
    }

    @Test
    fun photoToolSwitch_dropsForeignStrokeDrafts_keepsSharedGrade() {
        val viewModel = PhotoSessionViewModel(
            initialGraph = "",
            persistence = GraphPersistence { },
            scheduler = ManualGraphSaveScheduler(),
            toolKey = "editor"
        )
        viewModel.selectGroup(PhotoGroup.Light, "editor")
        viewModel.controls.exposure = 0.55f
        viewModel.controls.hue = 12f
        viewModel.selectGroup(PhotoGroup.Color, "editor")
        assertEquals(SessionMode.Adjust, viewModel.toolMode)
        assertEquals(0.55f, viewModel.controls.exposure)
        assertEquals(12f, viewModel.controls.hue)

        viewModel.selectGroup(PhotoGroup.Retouch, "editor")
        viewModel.selectMode(SessionMode.Heal)
        viewModel.controls.pendingStamps = listOf(Stamp(0.1f, 0.2f, 0.04f))
        viewModel.controls.pickingHealSource = true
        viewModel.controls.healSource = 0.4f to 0.5f
        viewModel.selectMode(SessionMode.Fill)
        assertTrue(viewModel.controls.pendingStamps.isEmpty())
        assertFalse(viewModel.controls.pickingHealSource)

        viewModel.controls.pendingStamps = listOf(Stamp(0.3f, 0.3f, 0.04f))
        viewModel.selectGroup(PhotoGroup.Light, "editor")
        assertEquals(listOf(0.3f), viewModel.controls.pendingStamps.map { it.x })
        assertEquals(0.55f, viewModel.controls.exposure)
        viewModel.selectMode(SessionMode.Fill)
        assertEquals(listOf(0.3f), viewModel.controls.pendingStamps.map { it.x })

        viewModel.selectMode(SessionMode.Erase)
        assertTrue(viewModel.controls.pendingStamps.isEmpty())
        assertEquals(null, viewModel.controls.healSource)
    }

    @Test
    fun movieToolSelection_startsOnFirstClip() {
        val viewModel = MovieSessionViewModel(
            initialTimeline = timeline(volume = 1f),
            persistence = GraphPersistence { },
            scheduler = ManualGraphSaveScheduler()
        )
        assertEquals("c1", viewModel.selectedClipId)
        assertEquals(MovieGroup.Edit, viewModel.movieGroup)
        viewModel.movieGroup = MovieGroup.Audio
        viewModel.effectSub = MovieEffectSub.SlowMo
        viewModel.controls.playhead = 0.4f
        viewModel.controls.playing = false
        viewModel.resetToolChrome()
        assertEquals(MovieGroup.Edit, viewModel.movieGroup)
        assertEquals(MovieEffectSub.Motion, viewModel.effectSub)
        assertEquals(0f, viewModel.controls.playhead)
        assertTrue(viewModel.controls.playing)
        viewModel.controls.bindEndMs(5_000L)
        assertEquals(5_000L, viewModel.controls.endMs)
        viewModel.controls.bindEndMs(9_000L)
        assertEquals(5_000L, viewModel.controls.endMs)
        viewModel.resetToolChrome()
        viewModel.controls.bindEndMs(9_000L)
        assertEquals(9_000L, viewModel.controls.endMs)
        viewModel.notePlaybackPosition(250, 1_000)
        assertEquals(0.25f, viewModel.controls.playhead)
        viewModel.controls.playing = true
        val seek = viewModel.stepPlayhead(1, 30f, 1_000)
        assertFalse(viewModel.controls.playing)
        viewModel.confirmSteppedPlayhead(seek, 1_000)
        viewModel.beginExport("export")
        assertTrue(viewModel.controls.exporting)
        viewModel.onExportSucceeded("content://movie", "ok")
        assertFalse(viewModel.controls.exporting)
        assertEquals("content://movie", viewModel.controls.exportSuccessUri)
        assertTrue(viewModel.controls.showExport)
    }

    @Test
    fun movieChrome_resetsOnEntryRotationAndToolSwitch_keepsTimeline() {
        val viewModel = MovieSessionViewModel(
            initialTimeline = timeline(volume = 1f),
            persistence = GraphPersistence { },
            scheduler = ManualGraphSaveScheduler()
        )
        viewModel.setTimeline(viewModel.state.value.timeline.copy(
            clips = viewModel.state.value.timeline.clips.map { it.copy(volume = 0.25f) }
        ))
        viewModel.movieGroup = MovieGroup.Audio
        viewModel.effectSub = MovieEffectSub.SlowMo
        viewModel.controls.playhead = 0.8f
        viewModel.selectedTitleId = "title"

        val surface = Any()
        assertTrue(viewModel.syncToolChrome("move", surface))
        assertEquals(1, viewModel.chromeEpoch)
        assertEquals(MovieGroup.Edit, viewModel.movieGroup)
        assertEquals(MovieEffectSub.Motion, viewModel.effectSub)
        assertEquals(0f, viewModel.controls.playhead)
        assertEquals(null, viewModel.selectedTitleId)
        assertEquals(0.25f, viewModel.state.value.timeline.clips.single().volume)

        viewModel.movieGroup = MovieGroup.Color
        viewModel.controls.playhead = 0.2f
        assertFalse(viewModel.syncToolChrome("move", surface))
        assertEquals(MovieGroup.Color, viewModel.movieGroup)
        assertEquals(0.2f, viewModel.controls.playhead)

        assertTrue(viewModel.syncToolChrome("move", Any()))
        assertEquals(MovieGroup.Edit, viewModel.movieGroup)
        assertEquals(2, viewModel.chromeEpoch)

        viewModel.movieGroup = MovieGroup.Effects
        viewModel.effectSub = MovieEffectSub.Chroma
        assertTrue(viewModel.syncToolChrome("talking", surface))
        assertEquals(MovieGroup.Edit, viewModel.movieGroup)
        assertEquals(MovieEffectSub.Motion, viewModel.effectSub)
        assertEquals(0.25f, viewModel.state.value.timeline.clips.single().volume)
    }

    @Test
    fun saveStatus_staysSavedForQuietOpenThenTracksEdits() = runBlocking {
        val scheduler = ManualGraphSaveScheduler()
        val writes = mutableListOf<String>()
        val viewModel = PhotoSessionViewModel(
            initialGraph = "",
            persistence = GraphPersistence { writes += it },
            scheduler = scheduler
        )
        assertEquals(GraphSaveStatus.Saved, viewModel.saveStatus.value)
        scheduler.runPending()
        assertEquals(GraphSaveStatus.Saved, viewModel.saveStatus.value)
        assertEquals(1, writes.size)

        viewModel.push(LayeredOp("L1", "Grade", op = PhotoOp.Grade(0.5f, 1f)))
        assertEquals(GraphSaveStatus.Saving, viewModel.saveStatus.value)
        scheduler.runPending()
        assertEquals(GraphSaveStatus.Saved, viewModel.saveStatus.value)
        assertEquals(2, writes.size)
    }

    @Test
    fun saveStatus_failedWriteStaysPendingForFlush() = runBlocking {
        val scheduler = ManualGraphSaveScheduler()
        var fail = true
        val writes = mutableListOf<String>()
        val saver = DebouncedGraphSaver(
            persistence = GraphPersistence {
                if (fail) error("disk")
                writes += it
            },
            scheduler = scheduler
        )
        saver.submit("graph")
        assertEquals(GraphSaveStatus.Saving, saver.status.value)
        scheduler.runPending()
        assertEquals(GraphSaveStatus.Failed, saver.status.value)
        assertTrue(writes.isEmpty())

        fail = false
        saver.flush()
        assertEquals(listOf("graph"), writes)
        assertEquals(GraphSaveStatus.Saved, saver.status.value)
    }

    @Test
    fun saveStatus_quietFailureIsVisible() = runBlocking {
        val scheduler = ManualGraphSaveScheduler()
        val saver = DebouncedGraphSaver(
            persistence = GraphPersistence { error("disk") },
            scheduler = scheduler
        )
        saver.submit("graph", quiet = true)
        assertEquals(GraphSaveStatus.Saved, saver.status.value)
        scheduler.runPending()
        assertEquals(GraphSaveStatus.Failed, saver.status.value)
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
