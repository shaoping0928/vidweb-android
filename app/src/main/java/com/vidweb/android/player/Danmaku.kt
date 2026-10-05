package com.vidweb.android.player

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import com.vidweb.android.data.CommentItem

@Composable
fun DanmakuOverlay(
    comments: List<CommentItem>,
    positionProvider: () -> Long,
    enabled: Boolean,
    speedMs: Long,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(0L) }

    LaunchedEffect(enabled) {
        while (enabled) {
            withFrameNanos { now = positionProvider() }
        }
    }

    if (!enabled || comments.isEmpty()) return

    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            textSize = 38f
            color = android.graphics.Color.WHITE
            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
        }
    }

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f) return@Canvas
        val lanes = 9
        val laneHeight = height / lanes
        for (comment in comments) {
            val elapsed = now - comment.positionMs
            if (elapsed < 0 || elapsed > speedMs) continue
            val lane = (comment.id % lanes)
            val textWidth = paint.measureText(comment.text)
            val x = width - (elapsed.toFloat() / speedMs.toFloat()) * (width + textWidth)
            val y = lane * laneHeight + laneHeight * 0.75f
            drawContext.canvas.nativeCanvas.drawText(comment.text, x, y, paint)
        }
    }
}
