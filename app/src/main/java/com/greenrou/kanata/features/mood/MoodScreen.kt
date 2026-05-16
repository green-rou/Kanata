package com.greenrou.kanata.features.mood

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.features.mood.content.MoodResultContent
import com.greenrou.kanata.features.mood.content.MoodSelectionContent
import com.greenrou.kanata.features.mood.model.MoodEvent
import org.koin.androidx.compose.koinViewModel

@Composable
fun MoodScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateToDetails: (Int) -> Unit = {},
    viewModel: MoodViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MoodEvent.NavigateToDetails -> onNavigateToDetails(event.animeId)
                else -> Unit
            }
        }
    }

    BackHandler(enabled = state.selectedMood != null) {
        viewModel.handleEvent(MoodEvent.ClearMood)
    }

    AnimatedContent(
        targetState = state.selectedMood,
        label = "mood_transition",
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
        modifier = modifier.fillMaxSize(),
    ) { mood ->
        if (mood == null) {
            MoodSelectionContent(
                onMoodSelected = { viewModel.handleEvent(MoodEvent.SelectMood(it)) },
                topPadding = contentPadding.calculateTopPadding(),
                bottomPadding = contentPadding.calculateBottomPadding(),
            )
        } else {
            MoodResultContent(
                mood = mood,
                state = state,
                onBack = { viewModel.handleEvent(MoodEvent.ClearMood) },
                onAnimeClick = { viewModel.handleEvent(MoodEvent.AnimeClicked(it)) },
                topPadding = contentPadding.calculateTopPadding(),
                bottomPadding = contentPadding.calculateBottomPadding(),
            )
        }
    }
}
