package com.greenrou.kanata.features.pagereader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.GetContentPagesUseCase
import com.greenrou.kanata.domain.usecase.GetWatchProgressUseCase
import com.greenrou.kanata.domain.usecase.SaveWatchProgressUseCase
import com.greenrou.kanata.features.pagereader.model.PageReaderEvent
import com.greenrou.kanata.features.pagereader.model.PageReaderState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_RETRIES = 3
private const val RETRY_DELAY_MS = 2_000L

class PageReaderViewModel(
    private val getContentPages: GetContentPagesUseCase,
    private val saveWatchProgress: SaveWatchProgressUseCase,
    private val getWatchProgress: GetWatchProgressUseCase,
    chapterUrls: List<String>,
    chapterTitles: List<String>,
    startIndex: Int,
    private val animeTitle: String = "",
    private val chapterPageUrls: List<String> = emptyList(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        PageReaderState(
            isLoading = true,
            chapterUrls = chapterUrls,
            chapterTitles = chapterTitles,
            currentChapterIndex = startIndex,
        )
    )
    val state = _state.asStateFlow()

    private val _events = Channel<PageReaderEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadPages()
    }

    fun refreshResumePage() {
        val pages = _state.value.pages
        if (pages.isEmpty()) return
        viewModelScope.launch {
            val resumeIndex = getWatchProgress(chapterKey())?.positionMs?.toInt()
                ?.coerceIn(0, (pages.size - 1).coerceAtLeast(0)) ?: 0
            _state.update { it.copy(resumePageIndex = resumeIndex) }
        }
    }

    fun handleEvent(event: PageReaderEvent) {
        when (event) {
            PageReaderEvent.BackClicked -> viewModelScope.launch {
                _events.send(PageReaderEvent.NavigateBack)
            }
            PageReaderEvent.ToggleBars -> _state.update { it.copy(areBarsVisible = !it.areBarsVisible) }
            PageReaderEvent.PrevChapter -> if (_state.value.hasPrevChapter) {
                _state.update { it.copy(currentChapterIndex = it.currentChapterIndex - 1) }
                loadPages()
            }
            PageReaderEvent.NextChapter -> if (_state.value.hasNextChapter) {
                _state.update { it.copy(currentChapterIndex = it.currentChapterIndex + 1) }
                loadPages()
            }
            PageReaderEvent.RetryClicked -> loadPages()
            is PageReaderEvent.SaveProgress -> viewModelScope.launch {
                val key = chapterKey()
                if (key.isNotEmpty() && event.totalPages > 0) {
                    saveWatchProgress(
                        episodeUrl = key,
                        playbackUrl = _state.value.chapterUrls.getOrElse(_state.value.currentChapterIndex) { "" },
                        episodeTitle = _state.value.currentChapterTitle,
                        animeTitle = animeTitle,
                        isManga = true,
                        positionMs = event.currentPageIndex.toLong(),
                        durationMs = event.totalPages.toLong(),
                    )
                }
            }
            else -> Unit
        }
    }

    private fun chapterKey(): String {
        val idx = _state.value.currentChapterIndex
        return chapterPageUrls.getOrElse(idx) { _state.value.chapterUrls.getOrElse(idx) { "" } }
    }

    private fun loadPages() {
        val url = _state.value.chapterUrls.getOrNull(_state.value.currentChapterIndex) ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, pages = emptyList(), retryAttempt = 0, resumePageIndex = 0) }
            repeat(MAX_RETRIES + 1) { attempt ->
                val result = getContentPages(url)
                if (result.isSuccess) {
                    val pages = result.getOrDefault(emptyList())
                    val resumeIndex = getWatchProgress(chapterKey())?.positionMs?.toInt()
                        ?.coerceIn(0, (pages.size - 1).coerceAtLeast(0)) ?: 0
                    _state.update { it.copy(isLoading = false, pages = pages, retryAttempt = 0, resumePageIndex = resumeIndex) }
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
}
