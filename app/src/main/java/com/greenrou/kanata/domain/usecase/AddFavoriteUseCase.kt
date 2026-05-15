package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.FavoritesManager

class AddFavoriteUseCase(
    private val favoritesManager: FavoritesManager
) {
    suspend operator fun invoke(anime: Anime): Result<Unit> = favoritesManager.addFavorite(anime)
}
