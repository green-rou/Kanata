package com.greenrou.kanata.features.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.domain.model.DownloadItem
import com.greenrou.kanata.domain.model.DownloadStatus
import com.greenrou.kanata.domain.usecase.CancelDownloadUseCase
import com.greenrou.kanata.domain.usecase.DeleteCompletedDownloadUseCase
import com.greenrou.kanata.domain.usecase.GetCompletedDownloadsUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadFolderUseCase
import com.greenrou.kanata.domain.usecase.GetDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.ReorderDownloadQueueUseCase
import com.greenrou.kanata.domain.usecase.RetryDownloadUseCase
import com.greenrou.kanata.domain.usecase.SetDownloadFolderUseCase
import com.greenrou.kanata.features.downloads.model.DownloadManagerEvent
import com.greenrou.kanata.features.downloads.model.DownloadManagerState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DownloadManagerViewModel(
    private val getDownloadQueue: GetDownloadQueueUseCase,
    private val getCompletedDownloads: GetCompletedDownloadsUseCase,
    private val cancelDownload: CancelDownloadUseCase,
    private val deleteDownload: DeleteCompletedDownloadUseCase,
    private val reorderQueue: ReorderDownloadQueueUseCase,
    private val getDownloadFolder: GetDownloadFolderUseCase,
    private val setDownloadFolder: SetDownloadFolderUseCase,
    private val retryDownload: RetryDownloadUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadManagerState())
    val state = _state.asStateFlow()

    private val _events = Channel<DownloadManagerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")

    private val reportedFailedIds = mutableSetOf<Long>()
    private var firstEmission = true

    init {
        combine(
            getDownloadQueue(),
            getCompletedDownloads(),
            searchQuery,
        ) { queued, completed, query ->
            Triple(queued, completed, query)
        }.onEach { (queued, completed, query) ->
            _state.update {
                it.copy(
                    queuedDownloads = queued.filter(query),
                    completedDownloads = completed.filter(query),
                    searchQuery = query,
                )
            }
            checkForNewFailures(queued)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            val folder = getDownloadFolder()
            _state.update { it.copy(downloadFolder = folder) }
        }
    }

    fun handleEvent(event: DownloadManagerEvent) {
        when (event) {
            is DownloadManagerEvent.SearchQueryChanged -> searchQuery.value = event.query
            is DownloadManagerEvent.CancelDownload -> viewModelScope.launch {
                cancelDownload(event.id)
            }
            is DownloadManagerEvent.DeleteDownload -> viewModelScope.launch {
                deleteDownload(event.id, event.localFilePath)
            }
            is DownloadManagerEvent.ReorderQueue -> viewModelScope.launch {
                reorderQueue(event.orderedIds)
            }
            is DownloadManagerEvent.RetryDownload -> viewModelScope.launch {
                reportedFailedIds.remove(event.item.id)
                retryDownload(event.item)
            }
            is DownloadManagerEvent.PlayDownloaded -> viewModelScope.launch {
                val path = event.item.localFilePath ?: return@launch
                _events.send(DownloadManagerEvent.NavigateToPlayer("file://$path", event.item.episodeTitle))
            }
            is DownloadManagerEvent.FolderChosen -> viewModelScope.launch {
                setDownloadFolder(event.uri)
                _state.update { it.copy(downloadFolder = event.uri) }
            }
            else -> Unit
        }
    }

    private fun checkForNewFailures(queued: List<DownloadItem>) {
        if (firstEmission) {
            queued.filter { it.status == DownloadStatus.FAILED }
                .forEach { reportedFailedIds.add(it.id) }
            firstEmission = false
            return
        }
        queued.filter { it.status == DownloadStatus.FAILED && it.id !in reportedFailedIds }
            .forEach { item ->
                reportedFailedIds.add(item.id)
                viewModelScope.launch {
                    val reason = item.errorMessage?.take(120) ?: "Unknown error"
                    _events.send(
                        DownloadManagerEvent.ShowSnackbar(
                            "Failed to download \"${item.episodeTitle}\": $reason"
                        )
                    )
                }
            }
    }

    private fun List<DownloadItem>.filter(query: String): List<DownloadItem> {
        if (query.isBlank()) return this
        val q = query.trim().lowercase()
        return filter {
            it.episodeTitle.lowercase().contains(q) ||
            it.animeTitle.lowercase().contains(q) ||
            it.sourceName.lowercase().contains(q)
        }
    }
}
