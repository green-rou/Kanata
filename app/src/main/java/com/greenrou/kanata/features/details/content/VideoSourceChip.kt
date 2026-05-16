package com.greenrou.kanata.features.details.content

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.domain.model.VideoSource
import com.greenrou.kanata.domain.model.VideoSourceType

@Composable
internal fun VideoSourceChip(
    source: VideoSource,
    onClick: () -> Unit,
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(source.label) },
        icon = {
            Icon(
                imageVector = source.type.toIcon(),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
    )
}

private fun VideoSourceType.toIcon(): ImageVector = when (this) {
    VideoSourceType.ARCHIVE_ORG -> Icons.Outlined.Archive
    VideoSourceType.YOUTUBE -> Icons.Outlined.SmartDisplay
    VideoSourceType.HANIME -> Icons.Outlined.PlayCircle
    VideoSourceType.ANITUBE -> Icons.Outlined.Tv
    VideoSourceType.ANIWAVE -> Icons.Outlined.Tv
    VideoSourceType.MIKAI -> Icons.Outlined.Tv
    VideoSourceType.YUMMY_ANIME -> Icons.Outlined.Tv
    VideoSourceType.UNKNOWN -> Icons.Outlined.Language
}
