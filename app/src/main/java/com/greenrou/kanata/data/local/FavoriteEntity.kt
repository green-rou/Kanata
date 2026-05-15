package com.greenrou.kanata.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greenrou.kanata.domain.model.Anime

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val type: String,
    val imageUrl: String,
    val score: Double,
    val synopsis: String,
    val genres: List<String>,
    val episodes: Int,
    val addedAt: Long = System.currentTimeMillis()
)

fun FavoriteEntity.toDomain(): Anime = Anime(
    id = id,
    title = title,
    type = type,
    imageUrl = imageUrl,
    score = score,
    synopsis = synopsis,
    genres = genres,
    episodes = episodes
)

fun Anime.toEntity(): FavoriteEntity = FavoriteEntity(
    id = id,
    title = title,
    type = type,
    imageUrl = imageUrl,
    score = score,
    synopsis = synopsis,
    genres = genres,
    episodes = episodes
)
