package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.FavoritesManager
import kotlinx.coroutines.flow.Flow

class IsFavoriteUseCase(
    private val favoritesManager: FavoritesManager
) {
    suspend fun execute(id: Int): Result<Boolean> = favoritesManager.isFavorite(id)
    
    fun observe(id: Int): Flow<Result<Boolean>> = favoritesManager.isFavoriteFlow(id)
}
