package com.greenrou.kanata.features.mods.model

import com.greenrou.kanata.domain.model.ModInfo

data class ModsState(
    val mods: List<ModInfo> = emptyList(),
    val isLoadingIndex: Boolean = false,
    val indexError: String? = null,
    val downloadingIds: Set<String> = emptySet(),
    val downloadProgress: Map<String, Int> = emptyMap(),
    val isInstallingFromFile: Boolean = false,
)
