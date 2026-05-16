package com.greenrou.kanata.core.сomposable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

@Composable
fun FavoriteFab(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isFavorite)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 300),
        label = "fav_fab_container_color",
    )

    val contentColor by animateColorAsState(
        targetValue = if (isFavorite)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = tween(durationMillis = 300),
        label = "fav_fab_content_color",
    )

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        AnimatedContent(
            targetState = isFavorite,
            label = "fav_fab_icon",
        ) { favorite ->
            Icon(
                imageVector = if (favorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                contentDescription = if (favorite) "Remove from favorites" else "Add to favorites",
            )
        }
    }
}
