package com.greenrou.kanata.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getFavoritesPaged(limit: Int, offset: Int): List<FavoriteEntity>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC LIMIT :limit")
    fun getFavoritesPagedFlow(limit: Int): Flow<List<FavoriteEntity>>
    
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoritesFlow(): Flow<List<FavoriteEntity>>
    
    @Query("SELECT id FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllFavoriteIds(): List<Int>
    
    @Query("SELECT id FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoriteIdsFlow(): Flow<List<Int>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: Int): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    fun isFavoriteFlow(id: Int): Flow<Boolean>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)
    
    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)
    
    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Int)
    
    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavorites()
}
