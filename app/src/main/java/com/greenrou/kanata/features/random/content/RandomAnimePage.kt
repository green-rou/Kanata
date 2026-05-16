package com.greenrou.kanata.features.random.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.features.main.content.AnimeCard

@Composable
internal fun RandomAnimePage(
    anime: Anime?,
    isFavorite: Boolean,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onAnimeClick: (Int) -> Unit,
    onFavoriteClick: () -> Unit,
    bottomPadding: Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            error != null -> PageError(message = error, onRetry = onRefresh, modifier = Modifier.align(Alignment.Center))
            else -> Unit
        }

        if (!isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = PillTabTotalHeight)
                    .padding(horizontal = 32.dp)
                    .padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                if (anime != null) {
                    Text(
                        text = stringResource(R.string.random_your_pick),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    AnimeCard(
                        isFavorite = isFavorite,
                        anime = anime,
                        onClick = { onAnimeClick(anime.id) },
                        onFavoriteClick = { onFavoriteClick() },
                        modifier = Modifier.width(220.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.random_pick_another), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
