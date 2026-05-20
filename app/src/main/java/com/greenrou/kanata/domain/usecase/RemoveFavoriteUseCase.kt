package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.FavoritesManager

class RemoveFavoriteUseCase(
    private val favoritesManager: FavoritesManager
) {
    suspend operator fun invoke(id: Int): Result<Unit> = favoritesManager.removeFavorite(id)
}
