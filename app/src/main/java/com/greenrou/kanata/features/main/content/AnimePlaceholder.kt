package com.greenrou.kanata.features.main.content

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.greenrou.kanata.core.util.UiConstants

@Composable
internal fun AnimePlaceholder(title: String) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )

    val shimmerBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to UiConstants.PlaceholderGradientStart,
            0.4f to UiConstants.PlaceholderGradientStart,
            0.5f to Color(0xFF6B47C8),
            0.6f to UiConstants.PlaceholderGradientStart,
            1.0f to UiConstants.PlaceholderGradientStart,
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 600f, 900f),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(shimmerBrush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.take(1).uppercase(),
            style = MaterialTheme.typography.displayLarge,
            color = Color.White.copy(alpha = 0.2f),
            fontWeight = FontWeight.Black,
        )
    }
}
