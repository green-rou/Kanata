package com.greenrou.kanata.features.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(val label: String) {
    AnimeList(label = "Anime"),
    Favorites(label = "Favorites"),
    Discover(label = "Discover"),
    Downloads(label = "Downloads"),
    Settings(label = "Settings");

    val selectedIcon: ImageVector
        get() = when (this) {
            AnimeList -> Icons.Rounded.Home
            Favorites -> Icons.Rounded.Favorite
            Discover -> Icons.Rounded.Explore
            Downloads -> Icons.Rounded.Download
            Settings -> Icons.Rounded.Settings
        }

    val unselectedIcon: ImageVector
        get() = when (this) {
            AnimeList -> Icons.Outlined.Home
            Favorites -> Icons.Outlined.Favorite
            Discover -> Icons.Outlined.Explore
            Downloads -> Icons.Outlined.Download
            Settings -> Icons.Outlined.Settings
        }
}
