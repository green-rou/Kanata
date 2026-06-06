package com.greenrou.kanata.features.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.core.analytics.AnalyticsManager
import com.greenrou.kanata.data.mod.DownloadFeatureRegistry
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.Translation
import com.greenrou.kanata.domain.usecase.GetAnimegongoTranslationsUseCase
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.GetEpisodeListUseCase
import com.greenrou.kanata.domain.usecase.ObserveWatchProgressUseCase
import com.greenrou.kanata.domain.usecase.StartEpisodeDownloadUseCase
import com.greenrou.kanata.features.episodes.model.EpisodeListEvent
import com.greenrou.kanata.features.episodes.model.EpisodeListState
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder

class EpisodeListViewModel(
    private val getEpisodeList: GetEpisodeListUseCase,
    private val startEpisodeDownload: StartEpisodeDownloadUseCase,
    private val getDownloadQueue: GetDownloadQueueUseCase,
    private val getCompletedDownloads: GetCompletedDownloadsUseCase,
    private val getAnimegongoTranslations: GetAnimegongoTranslationsUseCase,
    private val analytics: AnalyticsManager,
    downloadFeatureRegistry: DownloadFeatureRegistry,
    private val observeWatchProgress: ObserveWatchProgressUseCase,
    val animePageUrl: String,
    val label: String,
    val animeTitle: String,
    val animeId: Int = 0,
    val expectedEpisodes: Int = 0,
) : ViewModel() {

    val isDownloadFeatureEnabled = downloadFeatureRegistry.isEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _state = MutableStateFlow(EpisodeListState(isLoading = true))
    val state = _state.asStateFlow()

    private val _events = Channel<EpisodeListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var watchProgressJob: Job? = null

    init {
        analytics.setScreen("episode_list")
        loadEpisodes()
        observeDownloadStatuses()
    }

    fun handleEvent(event: EpisodeListEvent) {
        when (event) {
            EpisodeListEvent.BackClicked -> viewModelScope.launch {
                _events.send(EpisodeListEvent.NavigateBack)
            }
            is EpisodeListEvent.EpisodeClicked -> {
                val clickedUrl = event.urls.getOrNull(event.index) ?: return
                if ("animego.ngo" in clickedUrl) {
                    val title = event.titles.getOrNull(event.index) ?: ""
                    showTranslationSheet(clickedUrl, title)
                } else {
                    viewModelScope.launch {
                        _events.send(EpisodeListEvent.NavigateToPlayer(event.urls, event.titles, event.index))
                    }
                }
            }
            EpisodeListEvent.DismissTranslationSheet -> {
                _state.update { it.copy(
                    isTranslationSheetVisible = false,
                    isTranslationsLoading = false,
                    translations = emptyList(),
                    pendingEpisodeUrl = "",
                    pendingEpisodeTitle = "",
                )}
            }
            is EpisodeListEvent.TranslationSelected -> playWithTranslation(event.translation)
            is EpisodeListEvent.DownloadEpisode -> viewModelScope.launch {
                startEpisodeDownload(
                    animeTitle = event.animeTitle,
                    sourceName = event.sourceName,
                    episodeTitle = event.episodeTitle,
                    episodePageUrl = event.episodePageUrl,
                    animePageUrl = event.animePageUrl,
                    animeId = event.animeId,
                )
            }
            else -> Unit
        }
    }

    private fun showTranslationSheet(episodeUrl: String, episodeTitle: String) {
        _state.update { it.copy(
            isTranslationSheetVisible = true,
            isTranslationsLoading = true,
            translations = emptyList(),
            pendingEpisodeUrl = episodeUrl,
            pendingEpisodeTitle = episodeTitle,
        )}
        viewModelScope.launch {
            getAnimegongoTranslations(episodeUrl)
                .onSuccess { list ->
                    if (list.size == 1) {
                        playWithTranslation(list.first())
                    } else {
                        _state.update { it.copy(isTranslationsLoading = false, translations = list) }
                    }
                }
                .onFailure {
                    _state.update { it.copy(isTranslationSheetVisible = false, isTranslationsLoading = false) }
                    val url = _state.value.pendingEpisodeUrl
                    val title = _state.value.pendingEpisodeTitle
                    _events.send(EpisodeListEvent.NavigateToPlayer(listOf(url), listOf(title), 0))
                }
        }
    }

    private fun playWithTranslation(translation: Translation) {
        viewModelScope.launch {
            val episodeUrl = _state.value.pendingEpisodeUrl.ifBlank { return@launch }
            val episodeTitle = _state.value.pendingEpisodeTitle
            val kodikUrl = buildKodikUrl(translation, episodeUrl)
            _state.update { it.copy(
                isTranslationSheetVisible = false,
                isTranslationsLoading = false,
                translations = emptyList(),
                pendingEpisodeUrl = "",
                pendingEpisodeTitle = "",
            )}
            _events.send(EpisodeListEvent.NavigateToPlayer(listOf(kodikUrl), listOf(episodeTitle), 0))
        }
    }

    private fun buildKodikUrl(translation: Translation, referer: String): String {
        val encoded = URLEncoder.encode(referer, "UTF-8")
        return "https://kodikplayer.com/${translation.mediaType}/${translation.mediaId}/${translation.mediaHash}/720p?yref=$encoded"
    }

    private fun loadEpisodes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getEpisodeList(animePageUrl, expectedEpisodes)
                .onSuccess { episodes ->
                    _state.update { it.copy(isLoading = false, episodes = episodes) }
                    val urls = episodes.map { it.url }
                    watchProgressJob?.cancel()
                    watchProgressJob = observeWatchProgress(urls)
                        .onEach { map -> _state.update { it.copy(watchProgress = map) } }
                        .launchIn(viewModelScope)
                }
                .onFailure { e ->
                    analytics.recordError(e, "episode_list_load", mapOf("source" to label, "anime" to animeTitle))
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun observeDownloadStatuses() {
        combine(getDownloadQueue(), getCompletedDownloads()) { queued, completed ->
            (queued + completed).associateBy(DownloadItem::episodePageUrl)
        }
            .onEach { map -> _state.update { it.copy(downloadStatuses = map) } }
            .launchIn(viewModelScope)
    }
}
