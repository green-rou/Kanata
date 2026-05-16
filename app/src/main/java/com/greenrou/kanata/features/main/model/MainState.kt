package com.greenrou.kanata.features.main.model

import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.model.AnimeFormat

data class MainState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val animeList: List<Anime> = emptyList(),
    val currentPage: Int = 1,
    val hasNextPage: Boolean = false,
    val error: String? = null,
    val showAdultContent: Boolean = false,
    val isDarkTheme: Boolean = false,
    val coverFillsTopBar: Boolean = true,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedGenres: Set<String> = emptySet(),
    val selectedFormats: Set<AnimeFormat> = emptySet(),
    val isFilterSheetVisible: Boolean = false,
) {
    val hasActiveFilters: Boolean get() = selectedGenres.isNotEmpty() || selectedFormats.isNotEmpty()
}
