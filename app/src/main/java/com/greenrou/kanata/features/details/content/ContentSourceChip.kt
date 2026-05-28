package com.greenrou.kanata.features.details.content

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.domain.model.ContentSource

@Composable
internal fun ContentSourceChip(
    source: ContentSource,
    onClick: () -> Unit,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(source.label) },
        icon = {
            Icon(
                imageVector = Icons.Outlined.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}
