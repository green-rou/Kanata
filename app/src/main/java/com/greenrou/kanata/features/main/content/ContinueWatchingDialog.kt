package com.greenrou.kanata.features.main.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R
import com.greenrou.kanata.domain.model.WatchProgress

@Composable
fun ContinueWatchingDialog(
    progress: WatchProgress,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
    isManga: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isManga) Icons.AutoMirrored.Filled.MenuBook else Icons.Rounded.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                text = stringResource(if (isManga) R.string.continue_reading_dialog_title else R.string.continue_watching_dialog_title),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (isManga) R.string.continue_reading_dialog_body else R.string.continue_watching_dialog_body,
                    progress.episodeTitle,
                    progress.animeTitle,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.continue_watching_dialog_confirm))
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.continue_watching_dialog_dismiss))
                }
            }
        },
    )
}
