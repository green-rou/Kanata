package com.greenrou.kanata.features.mods

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.data.local.InstalledModEntity
import com.greenrou.kanata.data.remote.dto.ModIndexDto
import com.greenrou.kanata.domain.model.ModInfo
import com.greenrou.kanata.domain.usecase.FetchRemoteModsUseCase
import com.greenrou.kanata.domain.usecase.GetInstalledModsUseCase
import com.greenrou.kanata.domain.usecase.InstallModFromFileUseCase
import com.greenrou.kanata.domain.usecase.InstallModUseCase
import com.greenrou.kanata.domain.usecase.ToggleModUseCase
import com.greenrou.kanata.domain.usecase.UninstallModUseCase
import com.greenrou.kanata.features.mods.model.ModsEvent
import com.greenrou.kanata.features.mods.model.ModsState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ModsViewModel(
    private val fetchRemoteMods: FetchRemoteModsUseCase,
    private val getInstalled: GetInstalledModsUseCase,
    private val installMod: InstallModUseCase,
    private val installModFromFile: InstallModFromFileUseCase,
    private val uninstallMod: UninstallModUseCase,
    private val toggleMod: ToggleModUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ModsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ModsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var remoteIndex: List<ModIndexDto> = emptyList()
    private var installedMods: List<InstalledModEntity> = emptyList()

    init {
        observeInstalled()
        loadIndex()
    }

    fun handleEvent(event: ModsEvent) {
        when (event) {
            is ModsEvent.Install -> handleInstall(event.mod)
            is ModsEvent.InstallFromFile -> handleInstallFromFile(event.uri)
            is ModsEvent.Uninstall -> handleUninstall(event.modId)
            is ModsEvent.Toggle -> handleToggle(event.modId, event.enabled)
            ModsEvent.RefreshIndex -> loadIndex()
            else -> Unit
        }
    }

    private fun loadIndex() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingIndex = true, indexError = null) }
            fetchRemoteMods()
                .onSuccess { index ->
                    remoteIndex = index
                    mergeAndUpdateState()
                }
                .onFailure { e ->
                    Log.e(TAG, "loadIndex failed", e)
                    _state.update { it.copy(indexError = e.message ?: "Failed to load extensions") }
                }
            _state.update { it.copy(isLoadingIndex = false) }
        }
    }

    private fun observeInstalled() {
        getInstalled().onEach { installed ->
            installedMods = installed
            mergeAndUpdateState()
        }.launchIn(viewModelScope)
    }

    private fun mergeAndUpdateState() {
        val installedById = installedMods.associateBy { it.id }
        val fromIndex = remoteIndex.map { dto ->
            val inst = installedById[dto.id]
            ModInfo(
                id = dto.id,
                label = dto.label,
                language = dto.language,
                version = dto.version,
                description = dto.description,
                apkUrl = dto.apkUrl,
                parserClass = dto.parserClass,
                isInstalled = inst != null,
                isEnabled = inst?.isEnabled ?: false,
                installedVersion = inst?.version,
                hasUpdate = inst != null && inst.version < dto.version,
            )
        }
        val indexIds = remoteIndex.map { it.id }.toSet()
        val offlineOnly = installedMods
            .filter { it.id !in indexIds }
            .map { e ->
                ModInfo(
                    id = e.id,
                    label = e.label,
                    language = e.language,
                    version = e.version,
                    description = "",
                    apkUrl = "",
                    parserClass = "",
                    isInstalled = true,
                    isEnabled = e.isEnabled,
                    installedVersion = e.version,
                    hasUpdate = false,
                )
            }
        _state.update { it.copy(mods = fromIndex + offlineOnly) }
    }

    private fun handleInstall(mod: ModInfo) {
        if (mod.apkUrl.isEmpty()) return
        val dto = remoteIndex.firstOrNull { it.id == mod.id } ?: return
        viewModelScope.launch {
            _state.update { it.copy(downloadingIds = it.downloadingIds + mod.id) }
            installMod(dto) { progress ->
                _state.update { it.copy(downloadProgress = it.downloadProgress + (mod.id to progress)) }
            }.onSuccess {
                _events.send(ModsEvent.ShowSnackbar("${mod.label} installed."))
            }.onFailure { e ->
                Log.e(TAG, "handleInstall failed for ${mod.id}", e)
                _events.send(ModsEvent.ShowSnackbar("Failed to install ${mod.label}: ${e.message}"))
            }
            _state.update {
                it.copy(
                    downloadingIds = it.downloadingIds - mod.id,
                    downloadProgress = it.downloadProgress - mod.id,
                )
            }
        }
    }

    private fun handleInstallFromFile(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isInstallingFromFile = true) }
            installModFromFile(uri)
                .onSuccess {
                    _events.send(ModsEvent.ShowSnackbar("Extension installed from file."))
                }
                .onFailure { e ->
                    Log.e(TAG, "handleInstallFromFile failed", e)
                    _events.send(ModsEvent.ShowSnackbar("Failed to install: ${e.message}"))
                }
            _state.update { it.copy(isInstallingFromFile = false) }
        }
    }

    private fun handleUninstall(modId: String) {
        viewModelScope.launch {
            uninstallMod(modId)
                .onSuccess {
                    _events.send(ModsEvent.ShowSnackbar("Extension removed."))
                }
                .onFailure { e ->
                    _events.send(ModsEvent.ShowSnackbar("Failed to remove extension: ${e.message}"))
                }
        }
    }

    private fun handleToggle(modId: String, enabled: Boolean) {
        viewModelScope.launch {
            toggleMod(modId, enabled)
            _events.send(ModsEvent.ShowSnackbar(if (enabled) "Extension enabled." else "Extension disabled."))
        }
    }

    private companion object {
        const val TAG = "ModsViewModel"
    }
}
