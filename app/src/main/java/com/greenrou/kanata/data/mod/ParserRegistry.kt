package com.greenrou.kanata.data.mod

import android.util.Log
import com.greenrou.kanata.data.local.InstalledModDao
import com.greenrou.kanata.domain.parser.SiteParser
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
class ParserRegistry(
    private val builtIn: List<SiteParser>,
    private val modLoader: ModLoader,
    dao: InstalledModDao,
) {
    private val _parsers = MutableStateFlow<List<SiteParser>>(builtIn)
    val parsers: StateFlow<List<SiteParser>> = _parsers.asStateFlow()

    init {
        dao.observeAll()
            .map { installed ->
                Log.d(TAG, "DB mods: total=${installed.size}, enabled=${installed.count { it.isEnabled }} — ${installed.map { "${it.id}(enabled=${it.isEnabled})" }}")
                val enabledFiles = installed
                    .filter { it.isEnabled }
                    .map { it.apkFileName }
                    .toSet()
                builtIn + modLoader.loadAll(enabledFiles)
            }
            .flowOn(Dispatchers.IO)
            .onEach { parsers ->
                Log.d(TAG, "Parsers updated: ${parsers.map { it.label }}")
                _parsers.value = parsers
            }
            .launchIn(GlobalScope)
    }

    private companion object {
        const val TAG = "ParserRegistry"
    }
}
