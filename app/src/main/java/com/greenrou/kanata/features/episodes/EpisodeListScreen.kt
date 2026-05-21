package com.greenrou.kanata.features.episodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.greenrou.kanata.core.composable.KanataLoader
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.greenrou.kanata.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.episodes.content.EpisodeCard
import com.greenrou.kanata.features.episodes.content.EpisodeEmptyState
import com.greenrou.kanata.features.episodes.content.TranslationBottomSheet
import com.greenrou.kanata.features.episodes.model.EpisodeListEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    animePageUrl: String,
    label: String,
    animeTitle: String = "",
    animeId: Int = 0,
    episodeCount: Int = 0,
    onNavigateBack: () -> Unit,
    onEpisodeClick: (urls: List<String>, titles: List<String>, index: Int) -> Unit,
    viewModel: EpisodeListViewModel = koinViewModel(
        key = animePageUrl,
        parameters = { parametersOf(animePageUrl, label, animeTitle, animeId, episodeCount) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EpisodeListEvent.NavigateBack -> onNavigateBack()
                is EpisodeListEvent.NavigateToPlayer -> onEpisodeClick(event.urls, event.titles, event.index)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.episodes.isNotEmpty()) {
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.episode_list_count, state.episodes.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Text(text = label)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleEvent(EpisodeListEvent.BackClicked) }) {
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
                state.isLoading -> KanataLoader(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = stringResource(R.string.detail_error, state.error ?: ""),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                )
                state.episodes.isEmpty() -> EpisodeEmptyState()
                else -> {
                    val urls = state.episodes.map { it.url }
                    val titles = state.episodes.map { it.title }
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(state.episodes) { index, episode ->
                            EpisodeCard(
                                number = index + 1,
                                title = episode.title,
                                onClick = {
                                    viewModel.handleEvent(
                                        EpisodeListEvent.EpisodeClicked(urls, titles, index)
                                    )
                                },
                                downloadStatus = state.downloadStatuses[episode.url]?.status,
                                onDownloadClick = {
                                    viewModel.handleEvent(
                                        EpisodeListEvent.DownloadEpisode(
                                            episodePageUrl = episode.url,
                                            animePageUrl = viewModel.animePageUrl,
                                            episodeTitle = episode.title,
                                            animeTitle = viewModel.animeTitle,
                                            sourceName = viewModel.label,
                                            animeId = viewModel.animeId,
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

    if (state.isTranslationSheetVisible) {
        TranslationBottomSheet(
            isLoading = state.isTranslationsLoading,
            translations = state.translations,
            onTranslationSelected = { viewModel.handleEvent(EpisodeListEvent.TranslationSelected(it)) },
            onDismiss = { viewModel.handleEvent(EpisodeListEvent.DismissTranslationSheet) },
        )
    }
}
