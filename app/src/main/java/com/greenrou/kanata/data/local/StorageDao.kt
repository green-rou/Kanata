package com.greenrou.kanata.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageDao {
    
    @Query("SELECT * FROM storage_items WHERE key = :key")
    suspend fun getByKey(key: String): StorageEntity?
    
    @Query("SELECT * FROM storage_items WHERE key = :key")
    fun getByKeyFlow(key: String): Flow<StorageEntity?>
    
    @Query("SELECT * FROM storage_items")
    suspend fun getAll(): List<StorageEntity>
    
    @Query("SELECT key FROM storage_items")
    suspend fun getAllKeys(): List<String>
    
    @Query("SELECT EXISTS(SELECT 1 FROM storage_items WHERE key = :key)")
    suspend fun hasKey(key: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: StorageEntity)
    
    @Delete
    suspend fun delete(entity: StorageEntity)
    
    @Query("DELETE FROM storage_items WHERE key = :key")
    suspend fun deleteByKey(key: String)
    
    @Query("DELETE FROM storage_items")
    suspend fun deleteAll()
}
