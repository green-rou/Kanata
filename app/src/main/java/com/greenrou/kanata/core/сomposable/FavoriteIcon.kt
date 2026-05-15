package com.greenrou.kanata.core.сomposable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp

@Composable
fun FavoriteIcon(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.dropShadow(
                CircleShape,
                shadow = Shadow(
                    radius = 15.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    spread = 0.dp
                )
            )
        ) {
            AnimatedContent(
                targetState = isFavorite,
                label = "favorite_animation"
            ) { favorite ->
                Icon(
                    imageVector = if (favorite)
                        Icons.Default.BookmarkRemove
                    else
                        Icons.Outlined.BookmarkAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                )
            }
        }
    }
}