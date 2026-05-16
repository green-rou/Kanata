package com.greenrou.kanata.features.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.unit.Dp
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.features.settings.content.SettingsItem
import com.greenrou.kanata.features.settings.content.SettingsLinkItem
import com.greenrou.kanata.features.settings.content.SettingsSection

private const val GITHUB_URL = "https://github.com/green-rou/Kanata"
private const val DONATE_URL = "https://ko-fi.com/C0C31ZLH6K"

@Composable
fun SettingsScreen(
    showAdultContent: Boolean,
    onToggleAdultContent: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    coverFillsTopBar: Boolean,
    onToggleCoverLayout: () -> Unit,
    downloadFolder: String = "",
    onSetDownloadFolder: (String) -> Unit = {},
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val appVersion = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("—")
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            onSetDownloadFolder(it.toString())
        }
    }

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
            SettingsItem(
                icon = if (coverFillsTopBar) Icons.Rounded.Fullscreen else Icons.Rounded.FullscreenExit,
                title = "Cover under top bar",
                subtitle = if (coverFillsTopBar) "Image extends behind the top bar" else "Image shown fully below the top bar",
                checked = coverFillsTopBar,
                onCheckedChange = { onToggleCoverLayout() },
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

        SettingsSection(title = "Downloads") {
            SettingsLinkItem(
                icon = Icons.Rounded.Folder,
                title = "Download folder",
                subtitle = downloadFolder.ifBlank { "Default (app external storage)" },
                onClick = { folderPickerLauncher.launch(null) },
            )
        }

        SettingsSection(title = "About") {
            SettingsLinkItem(
                icon = Icons.Rounded.Info,
                title = "Kanata v$appVersion",
                subtitle = "Made by GreenRou",
            )
            SettingsLinkItem(
                icon = Icons.Rounded.Code,
                title = "GitHub",
                subtitle = "green-rou/Kanata",
                onClick = { uriHandler.openUri(GITHUB_URL) },
            )
            SettingsLinkItem(
                icon = Icons.Rounded.VolunteerActivism,
                title = "Support me on Ko-fi",
                subtitle = "ko-fi.com/C0C31ZLH6K",
                onClick = { uriHandler.openUri(DONATE_URL) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp + bottomPadding))
    }
}
