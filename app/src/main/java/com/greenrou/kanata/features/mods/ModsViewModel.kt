package com.greenrou.kanata.features.mods

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenrou.kanata.data.local.InstalledModEntity
import com.greenrou.kanata.data.remote.dto.ModIndexDto
import com.greenrou.kanata.domain.model.ModCategory
import com.greenrou.kanata.domain.model.ModInfo
import com.greenrou.kanata.domain.repository.SettingsManager
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
import kotlinx.coroutines.flow.first
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
    private val settingsManager: SettingsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ModsState())
    val state = _state.asStateFlow()

    private val _events = Channel<ModsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var remoteIndex: List<ModIndexDto> = emptyList()
    private var installedMods: List<InstalledModEntity> = emptyList()

    init {
        observeInstalled()
        viewModelScope.launch {
            val url = settingsManager.modIndexUrl.first()
            val savedInput = settingsManager.modSourceInput.first()
            if (url.isBlank()) {
                _state.update { it.copy(isSourceConfigured = false, currentSourceUrl = "", sourceInput = "", savedSourceInput = "") }
            } else {
                _state.update { it.copy(isSourceConfigured = true, currentSourceUrl = url, sourceInput = "", savedSourceInput = savedInput) }
                loadIndex(url)
            }
        }
    }

    fun handleEvent(event: ModsEvent) {
        when (event) {
            is ModsEvent.Install -> handleInstall(event.mod)
            is ModsEvent.InstallFromFile -> handleInstallFromFile(event.uri)
            is ModsEvent.Uninstall -> handleUninstall(event.modId)
            is ModsEvent.Toggle -> handleToggle(event.modId, event.enabled)
            ModsEvent.RefreshIndex -> loadIndex(_state.value.currentSourceUrl)
            ModsEvent.ShowSourceDialog -> _state.update { it.copy(showSourceDialog = true, sourceInput = it.savedSourceInput) }
            ModsEvent.DismissSourceDialog -> _state.update { it.copy(showSourceDialog = false, sourceInput = it.savedSourceInput) }
            is ModsEvent.SourceInputChanged -> _state.update { it.copy(sourceInput = event.input) }
            ModsEvent.ConfirmSource -> {
                val rawInput = _state.value.sourceInput
                val resolved = resolveToIndexUrl(rawInput)
                viewModelScope.launch {
                    settingsManager.setModIndexUrl(resolved)
                    settingsManager.setModSourceInput(rawInput)
                    _state.update { it.copy(showSourceDialog = false, isSourceConfigured = true, currentSourceUrl = resolved, savedSourceInput = rawInput, sourceInput = rawInput) }
                    loadIndex(resolved)
                }
            }
            else -> Unit
        }
    }

    private fun loadIndex(url: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingIndex = true, indexError = null) }
            fetchRemoteMods(url)
                .onSuccess { index ->
                    remoteIndex = index
                    mergeAndUpdateState()
                }
                .onFailure { e ->
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
                category = categoryOf(dto.id, dto.isAdultOnly),
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
                    category = categoryOf(e.id, isAdultOnly = false),
                    isInstalled = true,
                    isEnabled = e.isEnabled,
                    installedVersion = e.version,
                    hasUpdate = false,
                )
            }
        _state.update { it.copy(mods = fromIndex + offlineOnly) }
    }

    private fun categoryOf(id: String, isAdultOnly: Boolean): ModCategory = when {
        id.startsWith("feature-") -> ModCategory.FEATURE
        id.startsWith("info-") -> ModCategory.INFO
        isAdultOnly -> ModCategory.SOURCE_ADULT
        id.contains("manga") || id in MANGA_SOURCE_IDS -> ModCategory.SOURCE_MANGA
        else -> ModCategory.SOURCE_ANIME
    }

    companion object {
        private const val DEFAULT_INDEX_URL =
            "https://raw.githubusercontent.com/AbaturSrc/kanata-extentions/main/index.json"
        private val MANGA_SOURCE_IDS = setOf(
            "source-frameworks",
            "source-heancms",
            "source-wpcomics",
            "source-madara",
        )
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

    private fun resolveToIndexUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.equals("extension", ignoreCase = true)) return DEFAULT_INDEX_URL
        if (trimmed.startsWith("https://raw.githubusercontent.com/")) return trimmed
        val ownerRepo = Regex("github\\.com/([\\w.-]+/[\\w.-]+)").find(trimmed)?.groupValues?.get(1)
            ?: if (trimmed.matches(Regex("[\\w.-]+/[\\w.-]+"))) trimmed else return trimmed
        return "https://raw.githubusercontent.com/$ownerRepo/main/index.json"
    }
}
