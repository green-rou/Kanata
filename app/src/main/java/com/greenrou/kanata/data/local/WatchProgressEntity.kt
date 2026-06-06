package com.greenrou.kanata.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greenrou.kanata.domain.model.WatchProgress

@Entity(tableName = "watch_progress")
data class WatchProgressEntity(
    @PrimaryKey val episodeUrl: String,
    val playbackUrl: String,
    val episodeTitle: String,
    val animeTitle: String,
    val isManga: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

fun WatchProgressEntity.toDomain() = WatchProgress(
    episodeUrl = episodeUrl,
    playbackUrl = playbackUrl,
    episodeTitle = episodeTitle,
    animeTitle = animeTitle,
    isManga = isManga,
    positionMs = positionMs,
    durationMs = durationMs,
    updatedAt = updatedAt,
)
