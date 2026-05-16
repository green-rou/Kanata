package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadItem>>
    fun getQueuedDownloads(): Flow<List<DownloadItem>>
    fun getCompletedDownloads(): Flow<List<DownloadItem>>
    suspend fun enqueueDownload(item: DownloadItem): Long
    suspend fun getDownloadByEpisodeUrl(url: String): DownloadItem?
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null)
    suspend fun updateProgress(id: Long, progressPercent: Int, fileSizeBytes: Long)
    suspend fun setLocalFilePath(id: Long, path: String)
    suspend fun cancelDownload(id: Long)
    suspend fun deleteDownload(id: Long)
    suspend fun reorderQueue(orderedIds: List<Long>)
    suspend fun getDownloadFolder(): String
    suspend fun setDownloadFolder(path: String)
}
