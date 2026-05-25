package com.greenrou.kanata.features.mods

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.greenrou.kanata.core.composable.KanataLoader
import com.greenrou.kanata.domain.model.ModInfo
import com.greenrou.kanata.features.mods.model.ModsEvent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ModsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is ModsEvent.ShowSnackbar) {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isLoadingIndex) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        IconButton(onClick = { viewModel.handleEvent(ModsEvent.RefreshIndex) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoadingIndex && state.mods.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        KanataLoader()
                        Text(
                            text = "Loading extensions…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.indexError != null && state.mods.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    ModsErrorState(
                        onRetry = { viewModel.handleEvent(ModsEvent.RefreshIndex) },
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                ) {
                    if (state.mods.isEmpty() && !state.isLoadingIndex) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Rounded.Extension,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "No extensions available",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    items(state.mods, key = { it.id }) { mod ->
                        ModCard(
                            mod = mod,
                            isDownloading = mod.id in state.downloadingIds,
                            downloadProgress = state.downloadProgress[mod.id],
                            onInstall = { viewModel.handleEvent(ModsEvent.Install(mod)) },
                            onUninstall = { viewModel.handleEvent(ModsEvent.Uninstall(mod.id)) },
                            onToggle = { enabled -> viewModel.handleEvent(ModsEvent.Toggle(mod.id, enabled)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModCard(
    mod: ModInfo,
    isDownloading: Boolean,
    downloadProgress: Int?,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmUninstall by remember { mutableStateOf(false) }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Extension,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = mod.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                LanguageBadge(language = mod.language)
                if (mod.isInstalled && mod.installedVersion != null) {
                    VersionBadge(
                        version = "v${mod.installedVersion}",
                        hasUpdate = mod.hasUpdate,
                    )
                }
            }

            if (mod.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = mod.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isDownloading) {
                Spacer(Modifier.height(12.dp))
                if (downloadProgress != null && downloadProgress > 0) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "$downloadProgress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (mod.isInstalled) {
                    Text(
                        text = if (mod.isEnabled) "Enabled" else "Disabled",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = mod.isEnabled,
                        onCheckedChange = onToggle,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (mod.hasUpdate) {
                        Button(
                            onClick = onInstall,
                            enabled = !isDownloading,
                        ) {
                            Text("Update to v${mod.version}")
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (showConfirmUninstall) {
                        Button(
                            onClick = {
                                showConfirmUninstall = false
                                onUninstall()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text("Confirm remove")
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { showConfirmUninstall = false }) {
                            Text("Cancel")
                        }
                    } else {
                        OutlinedButton(onClick = { showConfirmUninstall = true }) {
                            Text("Remove")
                        }
                    }
                } else {
                    Button(
                        onClick = onInstall,
                        enabled = !isDownloading && mod.apkUrl.isNotEmpty(),
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Install")
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageBadge(language: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = language.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ModsErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Couldn't load extensions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Check your internet connection and try again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Button(onClick = onRetry) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Try again")
        }
    }
}

@Composable
private fun VersionBadge(version: String, hasUpdate: Boolean, modifier: Modifier = Modifier) {
    val containerColor = if (hasUpdate)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (hasUpdate)
        MaterialTheme.colorScheme.onTertiaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
    ) {
        Text(
            text = if (hasUpdate) "$version ↑" else version,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
