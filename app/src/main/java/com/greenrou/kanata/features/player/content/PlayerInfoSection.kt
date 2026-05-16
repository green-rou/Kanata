package com.greenrou.kanata.features.player.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
internal fun PlayerInfoSection(
    title: String,
    currentIndex: Int,
    episodeCount: Int,
    modifier: Modifier = Modifier,
    downloadStatus: DownloadStatus? = null,
    onDownloadClick: () -> Unit = {},
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.player_episode_of, currentIndex + 1, episodeCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDownloadClick) {
            when (downloadStatus) {
                DownloadStatus.COMPLETED -> Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = stringResource(R.string.player_cd_downloaded),
                    tint = MaterialTheme.colorScheme.primary,
                )
                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                DownloadStatus.QUEUED -> Icon(
                    imageVector = Icons.Rounded.HourglassTop,
                    contentDescription = stringResource(R.string.player_cd_queued),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                DownloadStatus.FAILED -> Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = stringResource(R.string.player_cd_failed),
                    tint = MaterialTheme.colorScheme.error,
                )
                else -> Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.player_cd_download),
                )
            }
        }
    }
}
