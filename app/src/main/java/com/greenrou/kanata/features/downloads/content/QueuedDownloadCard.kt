package com.greenrou.kanata.features.downloads.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus

@Composable
internal fun QueuedDownloadCard(
    item: DownloadItem,
    onCancel: () -> Unit,
    onRetry: (() -> Unit)? = null,
    dragHandle: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isFailed = item.status == DownloadStatus.FAILED
    val isDownloading = item.status == DownloadStatus.DOWNLOADING

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                start = 12.dp,
                end = if (isFailed) 4.dp else 0.dp,
                top = 10.dp,
                bottom = if (isFailed) 4.dp else 10.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!isFailed) dragHandle()
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = item.episodeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.animeTitle} · ${item.sourceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isDownloading) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = buildProgressText(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { item.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
            if (!isFailed) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel download")
                }
            } else {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp).padding(end = 8.dp),
                )
            }
        }

        if (isFailed) {
            HorizontalDivider(color = MaterialTheme.colorScheme.errorContainer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.errorMessage ?: "Unknown error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (onRetry != null) {
                    TextButton(onClick = onRetry) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                        Text(" Retry")
                    }
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, "Remove", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun buildProgressText(item: DownloadItem): String {
    val percent = item.progressPercent
    val downloaded = item.fileSizeBytes
    if (downloaded <= 0) return "$percent%"

    val downloadedStr = formatSize(downloaded)
    return if (percent in 1..99) {
        val total = downloaded * 100L / percent
        "$percent% · $downloadedStr / ${formatSize(total)}"
    } else {
        "$percent% · $downloadedStr"
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
