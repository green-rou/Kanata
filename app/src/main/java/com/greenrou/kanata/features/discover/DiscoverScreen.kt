package com.greenrou.kanata.features.discover

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.greenrou.kanata.R
import com.greenrou.kanata.features.mood.MoodScreen
import com.greenrou.kanata.features.random.RandomScreen

@Composable
fun DiscoverScreen(
    onNavigateToDetails: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.tab_by_mood)) },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.tab_random)) },
                icon = { Icon(Icons.Rounded.Shuffle, contentDescription = null) },
            )
        }

        AnimatedContent(
            targetState = selectedTab,
            label = "discover_tab",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.weight(1f),
        ) { tab ->
            when (tab) {
                0 -> MoodScreen(
                    onNavigateToDetails = onNavigateToDetails,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> RandomScreen(
                    onNavigateToDetails = onNavigateToDetails,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
