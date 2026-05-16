package com.greenrou.kanata.features.settings.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R

private data class AccentOption(val name: String, val color: Color)

private val accentOptions = listOf(
    AccentOption("Green",  Color(0xFF15803D)),
    AccentOption("Gray",   Color(0xFF4B5563)),
    AccentOption("Blue",   Color(0xFF1D4ED8)),
    AccentOption("Red",    Color(0xFFDC2626)),
    AccentOption("Orange", Color(0xFFEA580C)),
)

@Composable
internal fun ColorPickerItem(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentOption = accentOptions.find { it.name == selectedColor } ?: accentOptions.first()
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(9.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_accent_color_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = selectedColor,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(shape)
                .background(currentOption.color)
                .border(2.dp, MaterialTheme.colorScheme.onSurface, shape),
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.Center),
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_accent_color_title)) },
            text = {
                Column {
                    accentOptions.forEach { option ->
                        val isSelected = option.name == selectedColor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onColorSelected(option.name)
                                    showDialog = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                            )
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(option.color),
                            )
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
