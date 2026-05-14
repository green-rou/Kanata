package com.greenrou.kanata.data.remote.dto

import com.greenrou.kanata.domain.model.Anime

data class AnimeListItemDto(
    val id: Int,
    val name: String,
    val type: String,
)

data class AnimeDetailDto(
    val id: Int,
    val name: String,
    val type: String,
    val imageUrl: String,
    val score: Double,
    val synopsis: String,
    val genres: List<String>,
    val episodes: Int,
)

fun AnimeListItemDto.toDomain() = Anime(
    id = id,
    title = name,
    type = type,
    imageUrl = "",
    score = 0.0,
    synopsis = "",
    genres = emptyList(),
    episodes = 0,
)

fun AnimeDetailDto.toDomain() = Anime(
    id = id,
    title = name,
    type = type,
    imageUrl = imageUrl,
    score = score,
    synopsis = synopsis,
    genres = genres,
    episodes = episodes,
)
