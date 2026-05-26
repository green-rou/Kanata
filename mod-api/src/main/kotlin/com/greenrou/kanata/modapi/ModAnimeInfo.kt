package com.greenrou.kanata.modapi

data class ModAnimeInfo(
    val synopsis: String? = null,
    val score: Double? = null,
    val scoreLabel: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
)
