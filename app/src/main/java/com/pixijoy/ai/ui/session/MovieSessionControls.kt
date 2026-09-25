package com.pixijoy.ai.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Trim, transport, and export state for one movie session.
 * Preview players stay on the screen because they are released with the composition.
 * [reset] matches the old `remember` defaults on each screen entry, including rotation.
 */
internal class MovieSessionControls {
    var startMs by mutableLongStateOf(0L)
    var endMs by mutableLongStateOf(0L)
    var speed by mutableFloatStateOf(1f)
    var playhead by mutableFloatStateOf(0f)
    var playing by mutableStateOf(true)
    var exporting by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var status by mutableStateOf<String?>(null)
    var showExport by mutableStateOf(false)
    var exportSuccessUri by mutableStateOf<String?>(null)
    var exportGif by mutableStateOf(false)
    private var endBound = false

    fun reset() {
        startMs = 0L
        endMs = 0L
        endBound = false
        speed = 1f
        playhead = 0f
        playing = true
        exporting = false
        progress = 0f
        status = null
        showExport = false
        exportSuccessUri = null
        exportGif = false
    }

    /** First composition of an entry sets the out-point. Later reads keep the user's trim. */
    fun bindEndMs(value: Long) {
        if (!endBound) {
            endMs = value
            endBound = true
        }
    }
}
