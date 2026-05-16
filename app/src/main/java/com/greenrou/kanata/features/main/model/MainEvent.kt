package com.greenrou.kanata.features.main.model

import com.greenrou.kanata.domain.model.AnimeFormat

sealed interface MainEvent {
    data object LoadAnime : MainEvent
    data object Refresh : MainEvent
    data object LoadMore : MainEvent
    data class AnimeClicked(val animeId: Int) : MainEvent
    data object ToggleAdultContent : MainEvent
    data object ToggleTheme : MainEvent
    data object ToggleCoverLayout : MainEvent
    data class ToggleFavorite(val animeId: Int) : MainEvent

    data object ToggleSearch : MainEvent
    data class SearchQueryChanged(val query: String) : MainEvent
    data object ToggleFilterSheet : MainEvent
    data class GenreToggled(val genre: String) : MainEvent
    data class FormatToggled(val format: AnimeFormat) : MainEvent
    data object ClearFilters : MainEvent

    data class SetDownloadFolder(val uri: String) : MainEvent
    data class SetAccentColor(val name: String) : MainEvent

    data class NavigateToDetail(val animeId: Int) : MainEvent
    data class ShowError(val message: String) : MainEvent
}
