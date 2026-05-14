package com.greenrou.kanata.features.player.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.features.player.model.PlayerState

@Composable
internal fun BoxScope.EpisodeSideButtons(
    state: PlayerState,
    controlsVisible: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    if (state.episodeCount <= 1) return

    AnimatedVisibility(
        visible = controlsVisible && state.currentIndex > 0,
        modifier = Modifier.align(Alignment.CenterStart),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(52.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous episode",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }

    AnimatedVisibility(
        visible = controlsVisible,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 56.dp),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = "${state.currentIndex + 1} / ${state.episodeCount}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }

    AnimatedVisibility(
        visible = controlsVisible && state.currentIndex < state.episodeCount - 1,
        modifier = Modifier.align(Alignment.CenterEnd),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(52.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next episode",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
