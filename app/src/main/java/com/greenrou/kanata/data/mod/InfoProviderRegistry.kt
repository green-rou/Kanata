package com.greenrou.kanata.data.mod

import com.greenrou.kanata.data.local.InstalledModDao
import com.greenrou.kanata.domain.parser.InfoProvider
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
class InfoProviderRegistry(
    private val modLoader: ModLoader,
    dao: InstalledModDao,
) {
    private val _providers = MutableStateFlow<List<InfoProvider>>(emptyList())
    val providers: StateFlow<List<InfoProvider>> = _providers.asStateFlow()

    init {
        dao.observeAll()
            .map { installed ->
                val enabledFiles = installed.filter { it.isEnabled }.map { it.apkFileName }.toSet()
                modLoader.loadInfoProviders(enabledFiles)
            }
            .flowOn(Dispatchers.IO)
            .onEach { _providers.value = it }
            .launchIn(GlobalScope)
    }
}
