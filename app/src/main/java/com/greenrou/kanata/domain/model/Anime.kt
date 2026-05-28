package com.greenrou.kanata.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Anime(
    val id: Int,
    val title: String,
    val titleRomaji: String = "",
    val titleEnglish: String = "",
    val type: String,
    val imageUrl: String,
    val score: Double,
    val synopsis: String,
    val genres: List<String>,
    val episodes: Int,
    val chapters: Int = 0,
    val volumes: Int = 0,
)
