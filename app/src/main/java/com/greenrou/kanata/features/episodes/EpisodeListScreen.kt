package com.greenrou.kanata.features.episodes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.episodes.model.EpisodeListEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeListScreen(
    animePageUrl: String,
    label: String,
    onNavigateBack: () -> Unit,
    onEpisodeClick: (urls: List<String>, titles: List<String>, index: Int) -> Unit,
    viewModel: EpisodeListViewModel = koinViewModel(key = animePageUrl, parameters = { parametersOf(animePageUrl, label) }),
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
                title = { Text("$label — Episodes") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.handleEvent(EpisodeListEvent.BackClicked) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                )
                state.episodes.isEmpty() -> Text(
                    text = "No episodes found",
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> {
                    val urls = state.episodes.map { it.url }
                    val titles = state.episodes.map { it.title }
                    LazyColumn {
                        itemsIndexed(state.episodes) { index, episode ->
                            ListItem(
                                headlineContent = { Text(episode.title) },
                                trailingContent = {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                modifier = Modifier.clickable {
                                    viewModel.handleEvent(
                                        EpisodeListEvent.EpisodeClicked(urls, titles, index)
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
