package com.greenrou.kanata.features.mood.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class Mood(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
) {
    CHILL(
        title = "Chill & Relax",
        description = "Slice of life and calming stories",
        icon = Icons.Rounded.WbSunny,
        color = Color(0xFF4ECDC4),
        genres = listOf("Slice of Life"),
        tags = listOf("Iyashikei"),
    ),
    ACTION(
        title = "Action Packed",
        description = "Epic battles and adrenaline",
        icon = Icons.Rounded.Bolt,
        color = Color(0xFFFF6B35),
        genres = listOf("Action", "Adventure"),
    ),
    ROMANCE(
        title = "Romantic",
        description = "Love stories and heart-warming moments",
        icon = Icons.Rounded.Favorite,
        color = Color(0xFFFF6B9D),
        genres = listOf("Romance"),
    ),
    SAD(
        title = "Emotional",
        description = "Bring some tissues, it's going to be sad",
        icon = Icons.Rounded.SentimentVeryDissatisfied,
        color = Color(0xFF5DADE2),
        genres = listOf("Drama"),
        tags = listOf("Tragedy"),
    ),
    HORROR(
        title = "Spooky",
        description = "Thrillers and horror to keep you awake",
        icon = Icons.Rounded.DarkMode,
        color = Color(0xFF8E44AD),
        genres = listOf("Horror", "Thriller"),
    ),
    COMEDY(
        title = "Funny",
        description = "Laugh until your stomach hurts",
        icon = Icons.Rounded.EmojiEmotions,
        color = Color(0xFFFFBB33),
        genres = listOf("Comedy"),
    ),
    FANTASY(
        title = "Magical Worlds",
        description = "Magic, isekai, and mythical creatures",
        icon = Icons.Rounded.AutoAwesome,
        color = Color(0xFF9B59B6),
        genres = listOf("Fantasy"),
        tags = listOf("Isekai"),
    ),
    MYSTERY(
        title = "Mystery",
        description = "Solve puzzles and uncover secrets",
        icon = Icons.Rounded.Search,
        color = Color(0xFF26A69A),
        genres = listOf("Mystery"),
    ),
}
