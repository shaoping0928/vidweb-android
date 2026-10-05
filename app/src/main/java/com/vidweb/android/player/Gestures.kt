package com.vidweb.android.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlinx.serialization.Serializable

@Serializable
data class GesturePrefs(
    val seek: Boolean = true,
    val longPress: Boolean = true,
    val doubleTap: Boolean = true,
    val brightness: Boolean = true,
    val volume: Boolean = true,
    val zoom: Boolean = true,
    val longPressSpeed: Float = 2f,
    val doubleTapMs: Long = 10000,
    val seekSensitivity: Float = 1f,
)

private enum class GestureKind { NONE, SEEK, BRIGHTNESS, VOLUME, TRANSFORM }

fun Modifier.playerGestures(
    enabled: Boolean,
    prefs: GesturePrefs,
    controlsVisible: Boolean,
    onTap: () -> Unit,
    onDoubleTapLeft: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekEnd: () -> Unit,
    onBrightness: (Float) -> Unit,
    onVolume: (Float) -> Unit,
    onTransform: (zoom: Float, panX: Float, panY: Float) -> Unit,
    onActive: (Boolean) -> Unit,
): Modifier = composed {
    var lastTapTime by remember { mutableStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }

    pointerInput(enabled, prefs, controlsVisible) {
        if (!enabled) return@pointerInput
        val excludeTop = if (controlsVisible) 56.dp.toPx() else 0f
        val excludeBottom = if (controlsVisible) 150.dp.toPx() else 0f
        coroutineScope {
            awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startPos = down.position
            val width = size.width.toFloat().coerceAtLeast(1f)
            val height = size.height.toFloat().coerceAtLeast(1f)
            val slop = viewConfiguration.touchSlop
            val inControlsZone =
                controlsVisible &&
                    (startPos.y < excludeTop || startPos.y > height - excludeBottom)
            var kind = GestureKind.NONE
            var longPressFired = false
            var total = Offset.Zero
            var longPressJob: Job? = null

            fun cancelLongPress() {
                longPressJob?.cancel()
                longPressJob = null
                if (longPressFired) {
                    longPressFired = false
                    onLongPressEnd()
                }
            }

            if (prefs.longPress && !inControlsZone) {
                longPressJob = launch {
                    delay(viewConfiguration.longPressTimeoutMillis)
                    if (kind == GestureKind.NONE) {
                        longPressFired = true
                        onLongPressStart()
                    }
                }
            }

            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.none { it.pressed }) break
                if (event.changes.any { it.isConsumed } && kind == GestureKind.NONE) {
                    cancelLongPress()
                }

                if (event.changes.size > 1 && prefs.zoom) {
                    if (kind != GestureKind.TRANSFORM) {
                        cancelLongPress()
                        if (kind == GestureKind.SEEK) onSeekEnd()
                        kind = GestureKind.TRANSFORM
                        onActive(true)
                    }
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (zoom != 1f || pan != Offset.Zero) {
                        onTransform(zoom, pan.x, pan.y)
                    }
                    event.changes.forEach { it.consume() }
                    continue
                }

                val change = event.changes.firstOrNull { it.id == down.id }
                    ?: event.changes.firstOrNull()
                    ?: continue
                val delta = change.positionChange()
                total += delta

                if (kind == GestureKind.NONE) {
                    if (total.getDistance() > slop) {
                        cancelLongPress()
                        kind = when {
                            abs(total.x) > abs(total.y) ->
                                if (prefs.seek) GestureKind.SEEK else GestureKind.NONE
                            startPos.x < width / 2 ->
                                if (prefs.brightness) GestureKind.BRIGHTNESS else GestureKind.NONE
                            else ->
                                if (prefs.volume) GestureKind.VOLUME else GestureKind.NONE
                        }
                        total = Offset.Zero
                        if (kind != GestureKind.NONE) onActive(true)
                    }
                }

                when (kind) {
                    GestureKind.SEEK -> {
                        onSeek(delta.x / width)
                        change.consume()
                    }
                    GestureKind.BRIGHTNESS -> {
                        onBrightness(-delta.y / height)
                        change.consume()
                    }
                    GestureKind.VOLUME -> {
                        onVolume(-delta.y / height)
                        change.consume()
                    }
                    else -> {}
                }
            }

            val wasLongPress = longPressFired
            cancelLongPress()
            when (kind) {
                GestureKind.SEEK -> onSeekEnd()
                GestureKind.TRANSFORM, GestureKind.BRIGHTNESS, GestureKind.VOLUME -> onActive(false)
                else -> {
                    if (!wasLongPress && !inControlsZone) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 280 && (startPos - lastTapPos).getDistance() < 120f) {
                            lastTapTime = 0L
                            when {
                                !prefs.doubleTap -> onTap()
                                startPos.x < width * 0.35f -> onDoubleTapLeft()
                                startPos.x > width * 0.65f -> onDoubleTapRight()
                                else -> onDoubleTapCenter()
                            }
                        } else {
                            lastTapTime = now
                            lastTapPos = startPos
                            onTap()
                        }
                    }
                }
            }
        }
    }
}
}
