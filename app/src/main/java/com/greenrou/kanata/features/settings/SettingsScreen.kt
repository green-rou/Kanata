package com.greenrou.kanata.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.features.settings.content.SettingsItem
import com.greenrou.kanata.features.settings.content.SettingsSection

@Composable
fun SettingsScreen(
    showAdultContent: Boolean,
    onToggleAdultContent: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SettingsSection(title = "Appearance") {
            SettingsItem(
                icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                title = if (isDarkTheme) "Dark Theme" else "Light Theme",
                subtitle = "Switch between light and dark mode",
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
            )
        }

        SettingsSection(title = "Content") {
            SettingsItem(
                icon = if (showAdultContent) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                title = if (showAdultContent) "18+ Mode" else "Adult Content",
                subtitle = if (showAdultContent) "Showing only 18+ titles" else "Show only non-adult titles",
                checked = showAdultContent,
                onCheckedChange = { onToggleAdultContent() },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
