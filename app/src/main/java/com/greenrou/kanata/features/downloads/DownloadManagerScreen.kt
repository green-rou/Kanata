package com.greenrou.kanata.features.downloads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.greenrou.kanata.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.downloads.content.CompletedDownloadCard
import com.greenrou.kanata.features.downloads.content.QueuedDownloadCard
import com.greenrou.kanata.features.downloads.model.DownloadManagerEvent
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun DownloadManagerScreen(
    onPlayDownloaded: (localFilePath: String, title: String) -> Unit,
    onOpenEpisodeList: (animePageUrl: String, sourceName: String, animeTitle: String) -> Unit = { _, _, _ -> },
    onNavigateToAnimeDetails: (animeId: Int) -> Unit = {},
    onShowSnackbar: suspend (String) -> Unit = {},
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
    viewModel: DownloadManagerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DownloadManagerEvent.NavigateToPlayer ->
                    onPlayDownloaded(event.localFilePath, event.title)
                is DownloadManagerEvent.ShowSnackbar ->
                    onShowSnackbar(event.message)
                else -> Unit
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            listOf(
                stringResource(R.string.downloads_tab_downloaded, state.completedDownloads.size),
                stringResource(R.string.downloads_tab_queue, state.queuedDownloads.size),
            ).forEachIndexed { index, label ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(label) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> DownloadedTab(
                    items = state.completedDownloads,
                    onPlay = { item -> viewModel.handleEvent(DownloadManagerEvent.PlayDownloaded(item)) },
                    onDelete = { item ->
                        viewModel.handleEvent(DownloadManagerEvent.DeleteDownload(item.id, item.localFilePath))
                    },
                    onOpenEpisodeList = onOpenEpisodeList,
                    onNavigateToAnimeDetails = onNavigateToAnimeDetails,
                    bottomPadding = bottomPadding,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> QueueTab(
                    items = state.queuedDownloads,
                    onCancel = { id -> viewModel.handleEvent(DownloadManagerEvent.CancelDownload(id)) },
                    onRetry = { item -> viewModel.handleEvent(DownloadManagerEvent.RetryDownload(item)) },
                    onReorder = { orderedIds -> viewModel.handleEvent(DownloadManagerEvent.ReorderQueue(orderedIds)) },
                    bottomPadding = bottomPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun QueueTab(
    items: List<com.greenrou.kanata.domain.model.DownloadItem>,
    onCancel: (Long) -> Unit,
    onRetry: (com.greenrou.kanata.domain.model.DownloadItem) -> Unit,
    onReorder: (List<Long>) -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        DownloadsEmptyState(
            icon = Icons.Outlined.Download,
            title = stringResource(R.string.downloads_queue_empty_title),
            subtitle = stringResource(R.string.downloads_queue_empty_subtitle),
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val draggableItems = remember { items.toMutableStateList() }
    val listState = rememberLazyListState()
    var dragFromIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(items) {
        if (dragFromIndex < 0) {
            draggableItems.clear()
            draggableItems.addAll(items)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        itemsIndexed(draggableItems, key = { _, item -> item.id }) { index, item ->
            val isDragging = dragFromIndex == index
            QueuedDownloadCard(
                item = item,
                onCancel = { onCancel(item.id) },
                onRetry = { onRetry(item) },
                dragHandle = {
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = stringResource(R.string.downloads_cd_drag),
                        tint = if (isDragging) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragFromIndex = draggableItems.indexOfFirst { it.id == item.id }
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y

                                    val info = listState.layoutInfo
                                    val dragged = info.visibleItemsInfo
                                        .find { it.index == dragFromIndex } ?: return@detectDragGesturesAfterLongPress
                                    val fingerPos = dragged.offset + dragged.size / 2 + dragOffsetY.toInt()

                                    val neighbor = when {
                                        amount.y > 0 -> info.visibleItemsInfo
                                            .firstOrNull { it.index == dragFromIndex + 1 }
                                            ?.takeIf { fingerPos > it.offset + it.size / 2 }
                                        amount.y < 0 -> info.visibleItemsInfo
                                            .firstOrNull { it.index == dragFromIndex - 1 }
                                            ?.takeIf { fingerPos < it.offset + it.size / 2 }
                                        else -> null
                                    }

                                    if (neighbor != null) {
                                        val fromCenter = dragged.offset + dragged.size / 2
                                        val toCenter = neighbor.offset + neighbor.size / 2
                                        draggableItems.add(neighbor.index, draggableItems.removeAt(dragFromIndex))
                                        dragOffsetY -= (toCenter - fromCenter).toFloat()
                                        dragFromIndex = neighbor.index
                                    }
                                },
                                onDragEnd = {
                                    onReorder(draggableItems.map { it.id })
                                    dragFromIndex = -1
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggableItems.clear()
                                    draggableItems.addAll(items)
                                    dragFromIndex = -1
                                    dragOffsetY = 0f
                                },
                            )
                        },
                    )
                },
                modifier = Modifier
                    .then(if (!isDragging) Modifier.animateItem() else Modifier)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                        scaleX = if (isDragging) 1.03f else 1f
                        scaleY = if (isDragging) 1.03f else 1f
                        shadowElevation = if (isDragging) 24f else 0f
                    },
            )
        }
    }
}

@Composable
private fun DownloadedTab(
    items: List<com.greenrou.kanata.domain.model.DownloadItem>,
    onPlay: (com.greenrou.kanata.domain.model.DownloadItem) -> Unit,
    onDelete: (com.greenrou.kanata.domain.model.DownloadItem) -> Unit,
    onOpenEpisodeList: (animePageUrl: String, sourceName: String, animeTitle: String) -> Unit,
    onNavigateToAnimeDetails: (animeId: Int) -> Unit,
    bottomPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        DownloadsEmptyState(
            icon = Icons.Outlined.VideoLibrary,
            title = stringResource(R.string.downloads_empty_title),
            subtitle = stringResource(R.string.downloads_empty_subtitle),
            modifier = modifier.fillMaxSize(),
        )
        return
    }

    val grouped = items.groupBy { it.animeTitle }

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 8.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        grouped.forEach { (animeTitle, episodes) ->
            val animePageUrl = episodes.firstOrNull { it.animePageUrl.isNotBlank() }?.animePageUrl
            val animeId = episodes.firstOrNull { it.animeId > 0 }?.animeId ?: 0
            item(key = "header_$animeTitle") {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = animeTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (animeId > 0) {
                        IconButton(
                            onClick = { onNavigateToAnimeDetails(animeId) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = stringResource(R.string.downloads_cd_anime_details),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    if (animePageUrl != null) {
                        IconButton(
                            onClick = {
                                onOpenEpisodeList(
                                    animePageUrl,
                                    episodes.first().sourceName,
                                    animeTitle,
                                )
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = stringResource(R.string.downloads_cd_episode_list),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            items(episodes, key = { it.id }) { item ->
                CompletedDownloadCard(
                    item = item,
                    onPlay = { onPlay(item) },
                    onDelete = { onDelete(item) },
                )
            }
        }
    }
}

@Composable
private fun DownloadsEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
            initialScale = 0.85f,
        ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
