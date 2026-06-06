package com.greenrou.kanata.core.composable

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val DOT_COUNT = 5
private const val CYCLE_MS = 800
private const val STAGGER_MS = 160
private const val ROTATION_MS = 4000
private const val SHAPE_POINTS = 48
private val TWO_PI = (2.0 * PI).toFloat()

@Composable
fun KanataLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "morph_loader")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(
            animation = tween(ROTATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val morphT0 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, 0 * STAGGER_MS, EaseInOutSine), RepeatMode.Reverse), label = "m0")
    val morphT1 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, 1 * STAGGER_MS, EaseInOutSine), RepeatMode.Reverse), label = "m1")
    val morphT2 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, 2 * STAGGER_MS, EaseInOutSine), RepeatMode.Reverse), label = "m2")
    val morphT3 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, 3 * STAGGER_MS, EaseInOutSine), RepeatMode.Reverse), label = "m3")
    val morphT4 by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(CYCLE_MS, 4 * STAGGER_MS, EaseInOutSine), RepeatMode.Reverse), label = "m4")

    val morphTs = listOf(morphT0, morphT1, morphT2, morphT3, morphT4)

    Canvas(modifier = modifier.size(48.dp)) {
        val orbitR = size.minDimension / 2f * 0.58f
        val dotR   = size.minDimension / 2f * 0.20f
        morphTs.forEachIndexed { index, morphT ->
            val angle = TWO_PI * index / DOT_COUNT + rotation
            val dotCenter = Offset(
                x = center.x + orbitR * cos(angle),
                y = center.y + orbitR * sin(angle),
            )
            drawMorphShape(color, morphT, dotR, dotCenter)
        }
    }
}

private fun DrawScope.drawMorphShape(color: Color, t: Float, dotR: Float, dotCenter: Offset) {
    val scale = 1f - 0.35f * t
    val path = Path()
    for (i in 0 until SHAPE_POINTS) {
        val angle = (TWO_PI * i / SHAPE_POINTS)
        val cosA = cos(angle)
        val sinA = sin(angle)
        val d = (abs(cosA) + abs(sinA)).coerceAtLeast(1e-4f)
        val r = dotR * scale * (1f - t + t / d)
        val x = dotCenter.x + r * cosA
        val y = dotCenter.y + r * sinA
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

@Composable
fun KanataSmallLoader(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    CircularProgressIndicator(
        modifier = modifier.size(size),
        strokeWidth = strokeWidth,
        strokeCap = StrokeCap.Round,
        color = color,
    )
}
