package com.greenrou.kanata.features.pagereader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.greenrou.kanata.R
import com.greenrou.kanata.core.composable.KanataLoader
import com.greenrou.kanata.features.pagereader.model.PageReaderEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageReaderScreen(
    chapterUrls: List<String>,
    chapterTitles: List<String>,
    startIndex: Int,
    onNavigateBack: () -> Unit,
    chapterPageUrls: List<String> = emptyList(),
    animeTitle: String = "",
    viewModel: PageReaderViewModel = koinViewModel(
        key = "reader_${startIndex}_${chapterUrls.firstOrNull()}",
        parameters = { parametersOf(chapterUrls, chapterTitles, startIndex, animeTitle, chapterPageUrls) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshResumePage()
        viewModel.events.collect { event ->
            when (event) {
                PageReaderEvent.NavigateBack -> onNavigateBack()
                else -> Unit
            }
        }
    }

    LaunchedEffect(state.currentChapterIndex, state.resumePageIndex) {
        val resumeIndex = state.resumePageIndex
        if (resumeIndex > 0) {
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { it > 0 }
            listState.scrollToItem(resumeIndex)
        } else {
            listState.scrollToItem(0)
        }
    }

    val currentPage by remember { derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtMost(state.pages.size.coerceAtLeast(1)) } }

    val pagesState = rememberUpdatedState(state.pages)
    DisposableEffect(Unit) {
        onDispose {
            val total = pagesState.value.size
            if (total > 0) {
                viewModel.handleEvent(PageReaderEvent.SaveProgress(listState.firstVisibleItemIndex, total))
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { index ->
                if (state.pages.isNotEmpty()) {
                    delay(2000)
                    viewModel.handleEvent(PageReaderEvent.SaveProgress(index, state.pages.size))
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            state.isLoading -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                KanataLoader(color = Color.White)
                if (state.retryAttempt > 0) {
                    Text(
                        text = stringResource(R.string.reader_retrying, state.retryAttempt),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            state.error != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.detail_error, state.error ?: ""),
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = { viewModel.handleEvent(PageReaderEvent.RetryClicked) }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            state.pages.isEmpty() -> Text(
                text = stringResource(R.string.reader_error_empty),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { viewModel.handleEvent(PageReaderEvent.ToggleBars) }
                    },
            ) {
                itemsIndexed(state.pages) { _, page ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(page.url)
                            .apply {
                                if (!page.headers.containsKey("User-Agent")) {
                                    addHeader("User-Agent", USER_AGENT)
                                }
                                page.headers.forEach { (k, v) -> addHeader(k, v) }
                            }
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                        onError = { android.util.Log.w("PageReader", "Failed to load: ${page.url} — ${it.result.throwable.message}") },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.areBarsVisible,
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = state.currentChapterTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleEvent(PageReaderEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        }

        AnimatedVisibility(
            visible = state.areBarsVisible && state.pages.isNotEmpty(),
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { viewModel.handleEvent(PageReaderEvent.PrevChapter) },
                        enabled = state.hasPrevChapter,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = stringResource(R.string.reader_cd_prev),
                            tint = if (state.hasPrevChapter) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }
                    Text(
                        text = stringResource(R.string.reader_page_of, currentPage, state.pages.size),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    IconButton(
                        onClick = { viewModel.handleEvent(PageReaderEvent.NextChapter) },
                        enabled = state.hasNextChapter,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = stringResource(R.string.reader_cd_next),
                            tint = if (state.hasNextChapter) Color.White else Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}
