package com.pixijoy.studio.ui.session

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Observes viewport transforms before child tool overlays. In tool modes, changes are consumed
 * only after two pointers are down, leaving every one-finger gesture for the active tool.
 */
internal fun Modifier.photoViewportTransformGestures(
    toolInputMode: Boolean,
    gestureKey: Any?,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float) -> Unit
): Modifier = composed {
    val currentGesture = rememberUpdatedState(onGesture)
    pointerInput(toolInputMode, gestureKey) {
        val minimumPointers = if (toolInputMode) 2 else 1
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressedCount = event.changes.count { it.pressed }
                if (pressedCount >= minimumPointers) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (zoom != 1f || pan != Offset.Zero) {
                        currentGesture.value(event.calculateCentroid(), pan, zoom)
                        event.changes.forEach { change ->
                            if (change.positionChanged()) change.consume()
                        }
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * Non-consuming canvas press observer. Tool modes intentionally bypass it so crop and painting
 * gestures cannot be interpreted as Compare. A stationary double tap resets the viewport.
 */
internal fun Modifier.photoViewportPressGestures(
    toolInputMode: Boolean,
    compareEnabled: Boolean,
    gestureKey: Any?,
    onCompareHeld: (Boolean) -> Unit,
    onDoubleTap: () -> Unit
): Modifier = composed {
    if (toolInputMode) return@composed this

    val currentCompare = rememberUpdatedState(onCompareHeld)
    val currentDoubleTap = rememberUpdatedState(onDoubleTap)
    pointerInput(compareEnabled, gestureKey) {
        coroutineScope {
            var previousTapTime = Long.MIN_VALUE
            var previousTapPosition = Offset.Unspecified
            var hasPreviousTap = false
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                var moved = false
                var hadMultiplePointers = false
                var upTime = down.uptimeMillis
                var compareActivated = false
                val compareJob = if (compareEnabled) {
                    launch {
                        delay(viewConfiguration.longPressTimeoutMillis)
                        compareActivated = true
                        currentCompare.value(true)
                    }
                } else {
                    null
                }
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        hadMultiplePointers = hadMultiplePointers ||
                            event.changes.count { it.pressed } > 1 ||
                            event.changes.size > 1
                        event.changes.firstOrNull { it.id == down.id }?.let { change ->
                            moved = moved ||
                                (change.position - down.position).getDistance() >
                                viewConfiguration.touchSlop
                        }
                        if (moved || hadMultiplePointers) {
                            compareJob?.cancel()
                            if (compareActivated) {
                                compareActivated = false
                                currentCompare.value(false)
                            }
                        }
                        upTime = event.changes.maxOfOrNull { it.uptimeMillis } ?: upTime
                    } while (event.changes.any { it.pressed })
                } finally {
                    compareJob?.cancel()
                    if (compareActivated) currentCompare.value(false)
                }

                val tapDuration = upTime - down.uptimeMillis
                if (!moved && !hadMultiplePointers && tapDuration <= DOUBLE_TAP_TIMEOUT_MS) {
                    val sincePrevious = down.uptimeMillis - previousTapTime
                    val nearPrevious = hasPreviousTap &&
                        (down.position - previousTapPosition).getDistance() <=
                        viewConfiguration.touchSlop * 2f
                    if (sincePrevious in DOUBLE_TAP_MIN_MS..DOUBLE_TAP_TIMEOUT_MS && nearPrevious) {
                        currentDoubleTap.value()
                        previousTapTime = Long.MIN_VALUE
                        previousTapPosition = Offset.Unspecified
                        hasPreviousTap = false
                    } else {
                        previousTapTime = upTime
                        previousTapPosition = down.position
                        hasPreviousTap = true
                    }
                }
            }
        }
    }
}

private const val DOUBLE_TAP_MIN_MS = 40L
private const val DOUBLE_TAP_TIMEOUT_MS = 300L

/** Pointer press behavior for the dedicated Before/After control. */
internal fun Modifier.photoComparePress(
    enabled: Boolean,
    onHeld: (Boolean) -> Unit
): Modifier = composed {
    val currentHeld = rememberUpdatedState(onHeld)
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consume()
            currentHeld.value(true)
            try {
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                } while (event.changes.any { it.pressed })
            } finally {
                currentHeld.value(false)
            }
        }
    }
}
