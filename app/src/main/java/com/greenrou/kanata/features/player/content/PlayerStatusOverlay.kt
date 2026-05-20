package com.greenrou.kanata.features.player.content

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import com.greenrou.kanata.core.composable.KanataLoader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun BoxScope.PlayerStatusOverlay(isLoading: Boolean, error: String?) {
    when {
        isLoading -> KanataLoader(
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
        )
        error != null -> Text(
            text = error,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
