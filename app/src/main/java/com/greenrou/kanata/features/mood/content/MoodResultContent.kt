package com.greenrou.kanata.features.mood.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.features.main.content.AnimeGrid
import com.greenrou.kanata.features.mood.model.Mood
import com.greenrou.kanata.features.mood.model.MoodState

@Composable
internal fun MoodResultContent(
    mood: Mood,
    state: MoodState,
    onBack: () -> Unit,
    onAnimeClick: (Int) -> Unit,
    bottomPadding: Dp,
    topPadding: Dp = 0.dp,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = topPadding + 4.dp, start = 8.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(mood.color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mood.icon,
                    contentDescription = null,
                    tint = mood.color,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = mood.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.animeList.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = mood.icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = mood.color.copy(alpha = 0.4f),
                    )
                    Text(
                        text = "No anime found for this mood",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> AnimeGrid(
                    animeList = state.animeList,
                    favoriteIds = emptyList(),
                    gridState = rememberLazyGridState(),
                    isLoadingMore = false,
                    onAnimeClick = onAnimeClick,
                    onFavoriteClick = {},
                    onLoadMore = {},
                    contentPadding = PaddingValues(bottom = bottomPadding),
                )
            }
        }
    }
}
