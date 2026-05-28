package com.greenrou.kanata.data.mod

import com.greenrou.kanata.data.local.InstalledModDao
import com.greenrou.kanata.modapi.ModContentProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(DelicateCoroutinesApi::class)
class MangaModRegistry(
    private val modLoader: ModLoader,
    dao: InstalledModDao,
) {
    private val _isInstalled = MutableStateFlow(false)
    val isInstalled: StateFlow<Boolean> = _isInstalled.asStateFlow()

    private val _activeProvider = MutableStateFlow<ModContentProvider?>(null)
    val activeProvider: StateFlow<ModContentProvider?> = _activeProvider.asStateFlow()

    private val _contentProviderHasStreams = MutableStateFlow(true)
    val contentProviderHasStreams: StateFlow<Boolean> = _contentProviderHasStreams.asStateFlow()

    private val _modResources = MutableStateFlow<ModResources?>(null)
    val modResources: StateFlow<ModResources?> = _modResources.asStateFlow()

    init {
        dao.observeAll()
            .map { installed ->
                val enabledEntities = installed.filter { it.isEnabled }
                val enabledFiles = enabledEntities.map { it.apkFileName }.toSet()
                val providers = modLoader.loadContentProviders(enabledFiles)
                val provider = providers.firstOrNull()
                val resources = enabledEntities.firstOrNull()
                    ?.takeIf { provider != null }
                    ?.let { modLoader.loadModResources(it.apkFileName) }
                Triple(provider, resources, Unit)
            }
            .flowOn(Dispatchers.IO)
            .onEach { (provider, resources, _) ->
                _isInstalled.value = provider != null
                _activeProvider.value = provider
                _contentProviderHasStreams.value = provider?.hasStreamSources ?: true
                _modResources.value = resources
            }
            .launchIn(GlobalScope)
    }
}
