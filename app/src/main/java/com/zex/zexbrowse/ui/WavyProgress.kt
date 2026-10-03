/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.zex.zexbrowse.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
private val MaxAmplitudePx = 5f
private val TrackStrokePx = 8f
private val CanvasHeight = 16.dp
private const val WaveCycleMillis = 1400

// 波峰相位随时间匀速推进，让确定进度的波浪也持续流动，而不是静止的曲线
@Composable
private fun rememberWavePhase(): Float {
    val transition = rememberInfiniteTransition(label = "wavy")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WaveCycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    return phase
}

/** Material 3 Expressive 风格的波浪线性进度条（确定进度，起止自动拉直并持续流动）。 */
@Composable
fun WavyLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val target = progress().coerceIn(0f, 1f)
    // 平滑跟随目标进度，避免进度跳变时波形突变
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 360, easing = LinearEasing),
        label = "fraction"
    )
    val phase = rememberWavePhase()
    // 两端收拢：0 和 1 附近振幅趋近 0（直线），中段最大振幅
    val amplitude = MaxAmplitudePx * sin(PI.toFloat() * fraction).coerceAtLeast(0f)
    Canvas(modifier.fillMaxWidth().height(CanvasHeight)) {
        val stroke = Stroke(width = TrackStrokePx, cap = StrokeCap.Round)
        val centerY = size.height / 2f
        drawPath(wavePath(size.width, centerY, phase, amplitude), trackColor, style = stroke)
        if (fraction > 0f) {
            clipRect(right = size.width * fraction) {
                drawPath(wavePath(size.width, centerY, phase, amplitude), color, style = stroke)
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
    val phase = rememberWavePhase()
    Canvas(modifier.fillMaxWidth().height(CanvasHeight)) {
        val stroke = Stroke(width = TrackStrokePx, cap = StrokeCap.Round)
        val centerY = size.height / 2f
        drawPath(wavePath(size.width, centerY, phase, MaxAmplitudePx), trackColor, style = stroke)

        val segment = size.width * 0.45f
        val travel = size.width + segment
        val progressFraction = ((phase / (2 * PI).toFloat()) % 1f + 1f) % 1f
        val start = (-segment + travel * progressFraction).coerceIn(-segment, size.width)
        val end = (start + segment).coerceIn(0f, size.width)
        if (end > 0f && start < size.width) {
            clipRect(left = start.coerceAtLeast(0f), right = end) {
                drawPath(wavePath(size.width, centerY, phase, MaxAmplitudePx), color, style = stroke)
            }
        }
    }
}

private fun DrawScope.wavePath(width: Float, centerY: Float, phase: Float, amplitude: Float): Path {
    val path = Path()
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
