package com.greenrou.kanata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPagesDao {

    @Query("SELECT * FROM saved_pages ORDER BY savedAt DESC")
    fun getAll(): Flow<List<SavedPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: SavedPageEntity)

    @Query("DELETE FROM saved_pages WHERE id = :id")
    suspend fun deleteById(id: Long)
}
