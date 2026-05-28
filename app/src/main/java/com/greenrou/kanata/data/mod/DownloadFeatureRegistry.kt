package com.greenrou.kanata.data.mod

import com.greenrou.kanata.data.local.InstalledModDao
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
class DownloadFeatureRegistry(
    private val modLoader: ModLoader,
    dao: InstalledModDao,
) {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    init {
        dao.observeAll()
            .map { installed ->
                val enabledFiles = installed.filter { it.isEnabled }.map { it.apkFileName }.toSet()
                modLoader.loadDownloadFeatures(enabledFiles).isNotEmpty()
            }
            .flowOn(Dispatchers.IO)
            .onEach { _isEnabled.value = it }
            .launchIn(GlobalScope)
    }
}
