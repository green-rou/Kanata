package com.greenrou.kanata.features.onlinesearch.model

import com.greenrou.kanata.domain.model.OnlineSearchResult

sealed interface OnlineSearchScreenEvent {
    data class ResultClicked(val result: OnlineSearchResult) : OnlineSearchScreenEvent
    data class QueryChanged(val query: String) : OnlineSearchScreenEvent
    data class HideGroup(val sourceLabel: String) : OnlineSearchScreenEvent

    data object NavigateBack : OnlineSearchScreenEvent
    data class NavigateToDetails(val animeId: Int) : OnlineSearchScreenEvent
    data class NavigateToEpisodeList(
        val pageUrl: String,
        val label: String,
        val title: String,
    ) : OnlineSearchScreenEvent
    data class NavigateToChapterList(
        val pageUrl: String,
        val label: String,
        val title: String,
    ) : OnlineSearchScreenEvent
}
