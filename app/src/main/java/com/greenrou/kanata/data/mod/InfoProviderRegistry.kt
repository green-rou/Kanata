package com.greenrou.kanata.data.mod

import android.util.Log
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
                Log.d(TAG, "DB update: ${installed.size} mods total, ${installed.count { it.isEnabled }} enabled")
                val enabledFiles = installed
                    .filter { it.isEnabled }
                    .map { it.apkFileName }
                    .toSet()
                Log.d(TAG, "Loading info providers from files: $enabledFiles")
                val loaded = modLoader.loadInfoProviders(enabledFiles)
                Log.d(TAG, "Loaded ${loaded.size} info provider(s): ${loaded.map { it.id }}")
                loaded
            }
            .flowOn(Dispatchers.IO)
            .onEach { _providers.value = it }
            .launchIn(GlobalScope)
    }

    private companion object {
        const val TAG = "InfoProviderRegistry"
    }
}
