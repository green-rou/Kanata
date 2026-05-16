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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R
import com.greenrou.kanata.features.settings.content.ColorPickerItem
import com.greenrou.kanata.features.settings.content.LanguagePickerItem
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
    accentColor: String = "Green",
    onSetAccentColor: (String) -> Unit = {},
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
        SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
            SettingsItem(
                icon = if (isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                title = if (isDarkTheme) stringResource(R.string.settings_theme_dark)
                        else stringResource(R.string.settings_theme_light),
                subtitle = stringResource(R.string.settings_theme_subtitle),
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
            )
            SettingsItem(
                icon = if (coverFillsTopBar) Icons.Rounded.Fullscreen else Icons.Rounded.FullscreenExit,
                title = stringResource(R.string.settings_cover_title),
                subtitle = if (coverFillsTopBar) stringResource(R.string.settings_cover_extends)
                           else stringResource(R.string.settings_cover_below),
                checked = coverFillsTopBar,
                onCheckedChange = { onToggleCoverLayout() },
            )
            ColorPickerItem(
                selectedColor = accentColor,
                onColorSelected = onSetAccentColor,
            )
            LanguagePickerItem()
        }

        SettingsSection(title = stringResource(R.string.settings_section_content)) {
            SettingsItem(
                icon = if (showAdultContent) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                title = if (showAdultContent) stringResource(R.string.settings_adult_on_title)
                        else stringResource(R.string.settings_adult_off_title),
                subtitle = if (showAdultContent) stringResource(R.string.settings_adult_on_subtitle)
                           else stringResource(R.string.settings_adult_off_subtitle),
                checked = showAdultContent,
                onCheckedChange = { onToggleAdultContent() },
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_downloads)) {
            SettingsLinkItem(
                icon = Icons.Rounded.Folder,
                title = stringResource(R.string.settings_download_folder_title),
                subtitle = downloadFolder.ifBlank { stringResource(R.string.settings_download_folder_default) },
                onClick = { folderPickerLauncher.launch(null) },
            )
        }

        SettingsSection(title = stringResource(R.string.settings_section_about)) {
            SettingsLinkItem(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_version, appVersion ?: "—"),
                subtitle = stringResource(R.string.settings_made_by),
            )
            SettingsLinkItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.settings_github),
                subtitle = stringResource(R.string.settings_github_subtitle),
                onClick = { uriHandler.openUri(GITHUB_URL) },
            )
            SettingsLinkItem(
                icon = Icons.Rounded.VolunteerActivism,
                title = stringResource(R.string.settings_support),
                subtitle = stringResource(R.string.settings_support_subtitle),
                onClick = { uriHandler.openUri(DONATE_URL) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp + bottomPadding))
    }
}
