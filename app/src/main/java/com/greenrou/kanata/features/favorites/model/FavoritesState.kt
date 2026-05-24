package com.greenrou.kanata.features.favorites.model

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.model.SavedPage

data class FavoritesState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val favorites: List<Anime> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = true,
    val savedPages: List<SavedPage> = emptyList(),
)
