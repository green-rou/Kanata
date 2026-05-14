package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.Anime
import kotlinx.coroutines.flow.Flow

interface FavoritesManager {
    suspend fun addFavorite(anime: Anime): Result<Unit>
    suspend fun removeFavorite(id: Int): Result<Unit>
    suspend fun getFavoritesPaged(page: Int, pageSize: Int): Result<List<Anime>>
    fun getFavoritesPagedFlow(limit: Int): Flow<Result<List<Anime>>>
    fun getAllFavoritesFlow(): Flow<Result<List<Anime>>>
    suspend fun getAllFavoriteIds(): Result<List<Int>>
    fun getAllFavoriteIdsFlow(): Flow<Result<List<Int>>>
    suspend fun isFavorite(id: Int): Result<Boolean>
    fun isFavoriteFlow(id: Int): Flow<Result<Boolean>>
}
