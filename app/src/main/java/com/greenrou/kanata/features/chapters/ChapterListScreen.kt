package com.greenrou.kanata.features.chapters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.R
import com.greenrou.kanata.core.composable.KanataLoader
import com.greenrou.kanata.features.chapters.content.ChapterCard
import com.greenrou.kanata.features.chapters.content.ChapterEmptyState
import com.greenrou.kanata.features.chapters.content.ChapterErrorState
import com.greenrou.kanata.features.chapters.model.ChapterListEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListScreen(
    pageUrl: String,
    label: String,
    title: String = "",
    onNavigateBack: () -> Unit,
    onChapterClick: (chapterUrls: List<String>, chapterTitles: List<String>, startIndex: Int) -> Unit,
    viewModel: ChapterListViewModel = koinViewModel(
        key = pageUrl,
        parameters = { parametersOf(pageUrl, label, title) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDownloadFeatureEnabled by viewModel.isDownloadFeatureEnabled.collectAsStateWithLifecycle()

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.scrollIndex,
        initialFirstVisibleItemScrollOffset = state.scrollOffset,
    )
    DisposableEffect(Unit) {
        onDispose {
            viewModel.handleEvent(
                ChapterListEvent.SaveScrollPosition(
                    index = listState.firstVisibleItemIndex,
                    offset = listState.firstVisibleItemScrollOffset,
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ChapterListEvent.NavigateBack -> onNavigateBack()
                is ChapterListEvent.NavigateToReader ->
                    onChapterClick(event.chapterUrls, event.chapterTitles, event.startIndex)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.chapters.isNotEmpty()) {
                        Column {
                            Text(text = label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = stringResource(R.string.chapter_list_count, state.chapters.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(text = label)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleEvent(ChapterListEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KanataLoader()
                    if (state.retryAttempt > 0) {
                        Text(
                            text = stringResource(R.string.reader_retrying, state.retryAttempt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                state.error != null -> ChapterErrorState(
                    message = state.error ?: "",
                    onRetry = { viewModel.handleEvent(ChapterListEvent.RetryClicked) },
                )
                state.chapters.isEmpty() -> ChapterEmptyState()
                else -> LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(state.chapters) { index, chapter ->
                        ChapterCard(
                            number = index + 1,
                            title = chapter.title,
                            onClick = { viewModel.handleEvent(ChapterListEvent.ChapterClicked(index)) },
                            showDownloadButton = isDownloadFeatureEnabled,
                            downloadStatus = state.downloadStatuses[chapter.url]?.status,
                            onDownloadClick = {
                                viewModel.handleEvent(
                                    ChapterListEvent.DownloadChapter(
                                        chapterUrl = chapter.url,
                                        chapterTitle = chapter.title,
                                    )
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
