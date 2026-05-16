package com.greenrou.kanata.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.greenrou.kanata.data.local.DownloadDao
import com.greenrou.kanata.data.local.DownloadEntity
import com.greenrou.kanata.data.local.toDomain
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus
import com.greenrou.kanata.domain.repository.DownloadRepository
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DownloadRepositoryImpl(
    private val dao: DownloadDao,
    private val settings: SettingsManager,
    private val context: Context,
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadItem>> =
        dao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override fun getQueuedDownloads(): Flow<List<DownloadItem>> =
        dao.getQueuedFlow().map { list -> list.map { it.toDomain() } }

    override fun getCompletedDownloads(): Flow<List<DownloadItem>> =
        dao.getCompletedFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun enqueueDownload(item: DownloadItem): Long {
        val maxPos = dao.getMaxQueuePosition() ?: -1
        val entity = DownloadEntity(
            animeTitle = item.animeTitle,
            sourceName = item.sourceName,
            episodeTitle = item.episodeTitle,
            episodePageUrl = item.episodePageUrl,
            animePageUrl = item.animePageUrl,
            animeId = item.animeId,
            status = DownloadStatus.QUEUED.name,
            queuePosition = maxPos + 1,
            createdAt = System.currentTimeMillis(),
        )
        return dao.insert(entity)
    }

    override suspend fun getDownloadByEpisodeUrl(url: String): DownloadItem? =
        dao.getByEpisodeUrl(url)?.toDomain()

    override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) {
        dao.updateStatus(id, status.name, errorMessage)
    }

    override suspend fun updateProgress(id: Long, progressPercent: Int, fileSizeBytes: Long) {
        dao.updateProgress(id, progressPercent, fileSizeBytes)
    }

    override suspend fun setLocalFilePath(id: Long, path: String) {
        dao.setLocalFilePath(id, path)
    }

    override suspend fun cancelDownload(id: Long) {
        dao.updateStatus(id, DownloadStatus.CANCELLED.name, null)
    }

    override suspend fun deleteDownload(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun reorderQueue(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            dao.updateQueuePosition(id, index)
        }
    }

    override suspend fun getDownloadFolder(): String {
        val stored = settings.downloadFolder.first()
        if (stored.isBlank()) {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            return "${dir?.absolutePath ?: context.filesDir.absolutePath}/Kanata"
        }
        if (stored.startsWith("content://")) {
            val resolved = resolveContentUri(stored)
            if (resolved != null) return resolved
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            return "${dir?.absolutePath ?: context.filesDir.absolutePath}/Kanata"
        }
        return stored
    }

    private fun resolveContentUri(uriString: String): String? = runCatching {
        val uri = Uri.parse(uriString)
        val docId = DocumentsContract.getTreeDocumentId(uri)
        when {
            docId.startsWith("primary:") -> {
                val relative = docId.removePrefix("primary:")
                "${Environment.getExternalStorageDirectory().absolutePath}/$relative"
            }
            else -> null
        }
    }.getOrNull()

    override suspend fun setDownloadFolder(path: String) {
        settings.setDownloadFolder(path)
    }
}
