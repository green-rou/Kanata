package com.greenrou.kanata.features.onlinesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.model.AnimeFilter
import com.greenrou.kanata.domain.repository.SettingsManager
import com.greenrou.kanata.domain.usecase.GetAnimeListUseCase
import com.greenrou.kanata.domain.usecase.SearchOnlineUseCase
import com.greenrou.kanata.features.onlinesearch.model.OnlineSearchScreenEvent
import com.greenrou.kanata.features.onlinesearch.model.OnlineSearchScreenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class OnlineSearchViewModel(
    private val searchOnline: SearchOnlineUseCase,
    private val getAnimeList: GetAnimeListUseCase,
    private val settingsManager: SettingsManager,
    val query: String,
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineSearchScreenState(currentQuery = query))
    val state = _state.asStateFlow()

    private val _events = Channel<OnlineSearchScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val queryFlow = MutableStateFlow(query)

    init {
        viewModelScope.launch {
            combine(queryFlow.debounce(400), settingsManager.isMangaMode) { q, isManga -> q to isManga }
                .flatMapLatest { (q, isManga) -> searchOnline(q, isManga) }
                .collect { groups -> _state.update { it.copy(groups = groups) } }
        }
    }

    fun handleEvent(event: OnlineSearchScreenEvent) {
        when (event) {
            is OnlineSearchScreenEvent.QueryChanged -> {
                _state.update { it.copy(currentQuery = event.query, hiddenGroups = emptySet()) }
                queryFlow.value = event.query
            }
            is OnlineSearchScreenEvent.HideGroup ->
                _state.update { it.copy(hiddenGroups = it.hiddenGroups + event.sourceLabel) }
            is OnlineSearchScreenEvent.ResultClicked -> viewModelScope.launch {
                if (settingsManager.isMangaMode.first()) {
                    _events.send(OnlineSearchScreenEvent.NavigateToChapterList(
                        pageUrl = event.result.pageUrl,
                        label = event.result.sourceLabel,
                        title = event.result.title,
                    ))
                } else {
                    val animeId = findAniListId(event.result.title)
                    if (animeId != null) {
                        _events.send(OnlineSearchScreenEvent.NavigateToDetails(animeId))
                    } else {
                        _events.send(OnlineSearchScreenEvent.NavigateToEpisodeList(
                            pageUrl = event.result.pageUrl,
                            label = event.result.sourceLabel,
                            title = event.result.title,
                        ))
                    }
                }
            }
            OnlineSearchScreenEvent.NavigateBack -> viewModelScope.launch {
                _events.send(OnlineSearchScreenEvent.NavigateBack)
            }
            else -> Unit
        }
    }

    private suspend fun findAniListId(title: String): Int? {
        val result = getAnimeList(
            page = 1,
            perPage = 5,
            showAdultContent = true,
            filter = AnimeFilter(search = title),
        )
        return result.getOrNull()?.items?.firstOrNull()?.id
    }
}
