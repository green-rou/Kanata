package com.greenrou.kanata.features.episodes.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R
import com.greenrou.kanata.domain.model.DownloadStatus

@Composable
internal fun EpisodeCard(
    number: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus? = null,
    onDownloadClick: () -> Unit = {},
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDownloadClick) {
                when (downloadStatus) {
                    DownloadStatus.COMPLETED -> Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = stringResource(R.string.episode_cd_downloaded),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    DownloadStatus.QUEUED -> Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = stringResource(R.string.episode_cd_queued),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    DownloadStatus.FAILED -> Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = stringResource(R.string.episode_cd_failed),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    else -> Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = stringResource(R.string.episode_cd_download, number),
                    )
                }
            }
            FilledIconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.episode_cd_play, number),
                )
            }
        }
    }
}
