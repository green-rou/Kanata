package com.greenrou.kanata.features.mood.model

import com.greenrou.kanata.domain.model.Anime

data class MoodState(
    val selectedMood: Mood? = null,
    val isLoading: Boolean = false,
    val animeList: List<Anime> = emptyList(),
    val error: String? = null,
)
