package com.greenrou.kanata.domain.model

data class AnimeEnrichment(
    val synopsis: String? = null,
    val score: Double? = null,
    val scoreLabel: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<String> = emptyList(),
)
