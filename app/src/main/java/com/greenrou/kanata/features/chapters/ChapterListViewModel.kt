package com.greenrou.kanata.features.chapters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.usecase.GetChapterListUseCase
import com.greenrou.kanata.features.chapters.model.ChapterListEvent
import com.greenrou.kanata.features.chapters.model.ChapterListState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChapterListViewModel(
    private val getChapterList: GetChapterListUseCase,
    val pageUrl: String,
    val label: String,
    val title: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ChapterListState(isLoading = true))
    val state = _state.asStateFlow()

    private val _events = Channel<ChapterListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadChapters()
    }

    fun handleEvent(event: ChapterListEvent) {
        when (event) {
            ChapterListEvent.BackClicked -> viewModelScope.launch {
                _events.send(ChapterListEvent.NavigateBack)
            }
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
            else -> Unit
        }
    }

    private fun loadChapters() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getChapterList(pageUrl)
                .onSuccess { chapters -> _state.update { it.copy(isLoading = false, chapters = chapters) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
