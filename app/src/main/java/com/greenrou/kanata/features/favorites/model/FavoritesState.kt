package com.greenrou.kanata.features.favorites.model

import com.greenrou.kanata.domain.model.Anime

data class FavoritesState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val favorites: List<Anime> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = true,
)
