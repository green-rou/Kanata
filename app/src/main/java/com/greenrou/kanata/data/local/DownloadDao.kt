package com.greenrou.kanata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY queuePosition ASC, createdAt ASC")
    fun getAllFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED','DOWNLOADING','FAILED') ORDER BY queuePosition ASC")
    fun getQueuedFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY animeTitle ASC, episodeTitle ASC")
    fun getCompletedFlow(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE episodePageUrl = :url LIMIT 1")
    suspend fun getByEpisodeUrl(url: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String?)

    @Query("UPDATE downloads SET progressPercent = :progress, fileSizeBytes = :fileSize WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Int, fileSize: Long)

    @Query("UPDATE downloads SET localFilePath = :path WHERE id = :id")
    suspend fun setLocalFilePath(id: Long, path: String)

    @Query("UPDATE downloads SET queuePosition = :position WHERE id = :id")
    suspend fun updateQueuePosition(id: Long, position: Int)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT MAX(queuePosition) FROM downloads WHERE status IN ('QUEUED','DOWNLOADING')")
    suspend fun getMaxQueuePosition(): Int?
}
