/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private val WavelengthPx = 44f
private val AmplitudePx = 4f
private val TrackStrokePx = 8f
private val CanvasHeight = 16.dp

/** Material 3 Expressive 风格的波浪线性进度条（确定进度）。 */
@Composable
fun WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val fraction = progress().coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().height(CanvasHeight)) {
        val stroke = Stroke(width = TrackStrokePx, cap = StrokeCap.Round)
        val centerY = size.height / 2f
        drawPath(wavePath(size.width, centerY, 0f), trackColor, style = stroke)
        if (fraction > 0f) {
            clipRect(right = size.width * fraction) {
                drawPath(wavePath(size.width, centerY, 0f), color, style = stroke)
            }
        }
    }
}

/** Material 3 Expressive 风格的波浪线性进度条（不确定进度，波浪流动）。 */
@Composable
fun WavyLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val transition = rememberInfiniteTransition(label = "wavy")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier.fillMaxWidth().height(CanvasHeight)) {
        val stroke = Stroke(width = TrackStrokePx, cap = StrokeCap.Round)
        val centerY = size.height / 2f
        drawPath(wavePath(size.width, centerY, 0f), trackColor, style = stroke)

        val segment = size.width * 0.45f
        val travel = size.width + segment
        val progressFraction = ((phase / (2 * PI).toFloat()) % 1f + 1f) % 1f
        val start = (-segment + travel * progressFraction).coerceIn(-segment, size.width)
        val end = (start + segment).coerceIn(0f, size.width)
        if (end > 0f && start < size.width) {
            clipRect(left = start.coerceAtLeast(0f), right = end) {
                drawPath(wavePath(size.width, centerY, phase), color, style = stroke)
            }
        }
    }
}

private fun DrawScope.wavePath(width: Float, centerY: Float, phase: Float): Path {
    val path = Path()
    val amplitude = AmplitudePx
    val wavelength = WavelengthPx
    if (width <= 0f) return path
    path.moveTo(0f, centerY + amplitude * sin(phase))
    var x = 0f
    while (x <= width) {
        val y = centerY + amplitude * sin((2 * PI * (x / wavelength) + phase).toFloat())
        path.lineTo(x, y)
        x += 2f
    }
    path.lineTo(width, centerY + amplitude * sin((2 * PI * (width / wavelength) + phase).toFloat()))
    return path
}
