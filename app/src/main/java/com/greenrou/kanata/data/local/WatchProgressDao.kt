package com.greenrou.kanata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchProgressEntity)

    @Query("SELECT * FROM watch_progress WHERE episodeUrl = :url LIMIT 1")
    suspend fun getByUrl(url: String): WatchProgressEntity?

    @Query("SELECT * FROM watch_progress WHERE episodeUrl IN (:urls)")
    fun observeByUrls(urls: List<String>): Flow<List<WatchProgressEntity>>

    @Query(
        "SELECT * FROM watch_progress WHERE positionMs > 0 AND " +
        "(durationMs = 0 OR CAST(positionMs AS REAL) / durationMs < 0.9) " +
        "ORDER BY updatedAt DESC LIMIT 1"
    )
    suspend fun getLastWatched(): WatchProgressEntity?
}
