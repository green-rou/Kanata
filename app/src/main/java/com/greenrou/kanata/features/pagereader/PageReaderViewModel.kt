package com.greenrou.kanata.features.pagereader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.GetContentPagesUseCase
import com.greenrou.kanata.features.pagereader.model.PageReaderEvent
import com.greenrou.kanata.features.pagereader.model.PageReaderState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PageReaderViewModel(
    private val getContentPages: GetContentPagesUseCase,
    chapterUrls: List<String>,
    chapterTitles: List<String>,
    startIndex: Int,
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
            else -> Unit
        }
    }

    private fun loadPages() {
        val url = _state.value.chapterUrls.getOrNull(_state.value.currentChapterIndex) ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, pages = emptyList()) }
            getContentPages(url)
                .onSuccess { pages -> _state.update { it.copy(isLoading = false, pages = pages) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: e.javaClass.simpleName) } }
        }
    }
}
