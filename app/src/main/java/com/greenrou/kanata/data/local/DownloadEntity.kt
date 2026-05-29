package com.greenrou.kanata.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val animeTitle: String,
    val sourceName: String,
    val episodeTitle: String,
    val episodePageUrl: String,
    val animePageUrl: String = "",
    val animeId: Int = 0,
    val localFilePath: String? = null,
    val status: String,
    val progressPercent: Int = 0,
    val queuePosition: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0,
    val errorMessage: String? = null,
    val isManga: Int = 0,
)

fun DownloadEntity.toDomain() = DownloadItem(
    id = id,
    animeTitle = animeTitle,
    sourceName = sourceName,
    episodeTitle = episodeTitle,
    episodePageUrl = episodePageUrl,
    animePageUrl = animePageUrl,
    animeId = animeId,
    localFilePath = localFilePath,
    status = DownloadStatus.valueOf(status),
    progressPercent = progressPercent,
    queuePosition = queuePosition,
    createdAt = createdAt,
    fileSizeBytes = fileSizeBytes,
    errorMessage = errorMessage,
    isManga = isManga != 0,
)

fun DownloadItem.toEntity() = DownloadEntity(
    id = id,
    animeTitle = animeTitle,
    sourceName = sourceName,
    episodeTitle = episodeTitle,
    episodePageUrl = episodePageUrl,
    animePageUrl = animePageUrl,
    animeId = animeId,
    localFilePath = localFilePath,
    status = status.name,
    progressPercent = progressPercent,
    queuePosition = queuePosition,
    createdAt = createdAt,
    fileSizeBytes = fileSizeBytes,
    errorMessage = errorMessage,
    isManga = if (isManga) 1 else 0,
)
