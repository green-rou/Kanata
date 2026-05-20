package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.FavoritesManager
import kotlinx.coroutines.flow.Flow

class GetFavoritesUseCase(
    private val favoritesManager: FavoritesManager
) {
    suspend fun execute(page: Int = 1, pageSize: Int = 20): Result<List<Anime>> = 
        favoritesManager.getFavoritesPaged(page, pageSize)

    fun observePaged(limit: Int): Flow<Result<List<Anime>>> = 
        favoritesManager.getFavoritesPagedFlow(limit)
    
    fun observe(): Flow<Result<List<Anime>>> = favoritesManager.getAllFavoritesFlow()
    
    fun observeIds(): Flow<Result<List<Int>>> = favoritesManager.getAllFavoriteIdsFlow()
}
