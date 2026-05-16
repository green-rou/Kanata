package com.greenrou.kanata.features.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.greenrou.kanata.features.player.content.EpisodeSideButtons
import com.greenrou.kanata.features.player.content.NextEpisodeCard
import com.greenrou.kanata.features.player.content.PlayerErrorContent
import com.greenrou.kanata.features.player.content.PlayerInfoSection
import com.greenrou.kanata.features.player.content.PlayerStatusOverlay
import com.greenrou.kanata.features.player.model.PlayerEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    episodeUrls: List<String>,
    episodeTitles: List<String>,
    startIndex: Int,
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel(
        key = episodeUrls.firstOrNull() ?: "player",
        parameters = { parametersOf(episodeUrls, episodeTitles, startIndex) },
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                PlayerEvent.NavigateBack -> onNavigateBack()
                else -> Unit
            }
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) isFullscreen = false
    }

    LaunchedEffect(isFullscreen) {
        activity?.let { act ->
            act.requestedOrientation = if (isFullscreen)
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            act.window?.let { w ->
                val ctrl = WindowCompat.getInsetsController(w, w.decorView)
                if (isFullscreen) {
                    ctrl.hide(WindowInsetsCompat.Type.systemBars())
                    ctrl.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    ctrl.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { w ->
                WindowCompat.getInsetsController(w, w.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    val exoPlayer = remember { ExoPlayer.Builder(context).build().apply { playWhenReady = true } }
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                viewModel.handleEvent(
                    PlayerEvent.PlaybackError(error.cause?.message ?: error.message ?: "Playback failed")
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(state.streamUrl) {
        state.streamUrl?.let { url ->
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    val playerFactory: (android.content.Context) -> PlayerView = { ctx ->
        PlayerView(ctx).apply {
            player = exoPlayer
            useController = true
            controllerHideOnTouch = true
            setControllerShowTimeoutMs(3000)
            setShowPreviousButton(false)
            setShowNextButton(false)
            setControllerVisibilityListener(
                PlayerView.ControllerVisibilityListener { visibility ->
                    controlsVisible = (visibility == View.VISIBLE)
                }
            )
        }
    }

    if (isFullscreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(factory = playerFactory, modifier = Modifier.fillMaxSize())

            AnimatedVisibility(
                visible = controlsVisible,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(0.7f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { viewModel.handleEvent(PlayerEvent.BackClicked) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = state.title,
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = "Exit fullscreen",
                                tint = Color.White,
                            )
                        }
                    }

                    EpisodeSideButtons(
                        state = state,
                        controlsVisible = true,
                        onPrevious = { viewModel.handleEvent(PlayerEvent.PreviousEpisode) },
                        onNext = { viewModel.handleEvent(PlayerEvent.NextEpisode) },
                    )
                }
            }

            PlayerStatusOverlay(isLoading = state.isLoading, error = null)
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.handleEvent(PlayerEvent.BackClicked) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (state.error == null) {
                            IconButton(onClick = { isFullscreen = true }) {
                                Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen")
                            }
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (state.error != null) {
                    PlayerErrorContent(
                        error = state.error!!,
                        onRetry = { viewModel.handleEvent(PlayerEvent.Retry) },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                    ) {
                        AndroidView(factory = playerFactory, modifier = Modifier.fillMaxSize())
                        EpisodeSideButtons(
                            state = state,
                            controlsVisible = controlsVisible,
                            onPrevious = { viewModel.handleEvent(PlayerEvent.PreviousEpisode) },
                            onNext = { viewModel.handleEvent(PlayerEvent.NextEpisode) },
                        )
                        PlayerStatusOverlay(isLoading = state.isLoading, error = null)
                    }
                    PlayerInfoSection(
                        title = state.title,
                        currentIndex = state.currentIndex,
                        episodeCount = state.episodeCount,
                    )
                    state.nextEpisodeTitle?.let { nextTitle ->
                        NextEpisodeCard(
                            title = nextTitle,
                            onPlayNext = { viewModel.handleEvent(PlayerEvent.NextEpisode) },
                        )
                    }
                }
            }
        }
    }
}
