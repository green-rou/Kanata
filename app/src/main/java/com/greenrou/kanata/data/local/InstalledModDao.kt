package com.greenrou.kanata.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledModDao {

    @Query("SELECT * FROM installed_mods ORDER BY label ASC")
    fun observeAll(): Flow<List<InstalledModEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mod: InstalledModEntity)

    @Query("DELETE FROM installed_mods WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE installed_mods SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("SELECT * FROM installed_mods WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): InstalledModEntity?
}
