package com.greenrou.kanata.features.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.data.mod.DownloadFeatureRegistry
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.usecase.GetChapterListUseCase
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.StartChapterDownloadUseCase
import com.greenrou.kanata.features.chapters.model.ChapterListEvent
import com.greenrou.kanata.features.chapters.model.ChapterListState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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

private const val MAX_RETRIES = 3
private const val RETRY_DELAY_MS = 2_000L

class ChapterListViewModel(
    private val getChapterList: GetChapterListUseCase,
    private val startChapterDownload: StartChapterDownloadUseCase,
    private val getDownloadQueue: GetDownloadQueueUseCase,
    private val getCompletedDownloads: GetCompletedDownloadsUseCase,
    downloadFeatureRegistry: DownloadFeatureRegistry,
    val pageUrl: String,
    val label: String,
    val title: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChapterListState(isLoading = true))
    val state = _state.asStateFlow()

    private val _events = Channel<ChapterListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val isDownloadFeatureEnabled = downloadFeatureRegistry.isEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        loadChapters()
        observeDownloadStatuses()
    }

    fun handleEvent(event: ChapterListEvent) {
        when (event) {
            ChapterListEvent.BackClicked -> viewModelScope.launch {
                _events.send(ChapterListEvent.NavigateBack)
            }
            ChapterListEvent.RetryClicked -> loadChapters()
            is ChapterListEvent.SaveScrollPosition ->
                _state.update { it.copy(scrollIndex = event.index, scrollOffset = event.offset) }
            is ChapterListEvent.ChapterClicked -> {
                val chapters = _state.value.chapters
                viewModelScope.launch {
                    _events.send(
                        ChapterListEvent.NavigateToReader(
                            chapterUrls = chapters.map { it.url },
                            chapterTitles = chapters.map { it.title },
                            startIndex = event.index,
                        )
                    )
                }
            }
            is ChapterListEvent.DownloadChapter -> viewModelScope.launch {
                startChapterDownload(
                    mangaTitle = title,
                    sourceName = label,
                    chapterTitle = event.chapterTitle,
                    chapterUrl = event.chapterUrl,
                    mangaPageUrl = pageUrl,
                )
            }
            else -> Unit
        }
    }

    private fun loadChapters() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, retryAttempt = 0) }
            repeat(MAX_RETRIES + 1) { attempt ->
                val result = getChapterList(pageUrl)
                if (result.isSuccess) {
                    _state.update { it.copy(isLoading = false, chapters = result.getOrDefault(emptyList()), retryAttempt = 0) }
                    return@launch
                }
                if (attempt < MAX_RETRIES) {
                    _state.update { it.copy(retryAttempt = attempt + 1) }
                    delay(RETRY_DELAY_MS)
                } else {
                    val e = result.exceptionOrNull()
                    _state.update { it.copy(isLoading = false, error = e?.message ?: e?.javaClass?.simpleName ?: "Unknown error", retryAttempt = 0) }
                }
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
