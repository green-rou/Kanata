package com.greenrou.kanata.features.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavItem(val label: String) {
    AnimeList(label = "Anime"),
    Favorites(label = "Favorites"),
    Mood(label = "By Mood"),
    Random(label = "Random"),
    Settings(label = "Settings");

    val selectedIcon: ImageVector
        get() = when (this) {
            AnimeList -> Icons.Rounded.Home
            Favorites -> Icons.Rounded.Favorite
            Mood -> Icons.Rounded.Stars
            Random -> Icons.Rounded.Shuffle
            Settings -> Icons.Rounded.Settings
        }

    val unselectedIcon: ImageVector
        get() = when (this) {
            AnimeList -> Icons.Outlined.Home
            Favorites -> Icons.Outlined.Favorite
            Mood -> Icons.Outlined.Stars
            Random -> Icons.Outlined.Shuffle
            Settings -> Icons.Outlined.Settings
        }
}
