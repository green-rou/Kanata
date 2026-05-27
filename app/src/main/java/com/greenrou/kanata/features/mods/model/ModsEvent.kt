package com.greenrou.kanata.features.mods.model

import android.net.Uri
import com.greenrou.kanata.domain.model.ModInfo

sealed interface ModsEvent {
    data class Install(val mod: ModInfo) : ModsEvent
    data class InstallFromFile(val uri: Uri) : ModsEvent
    data class Uninstall(val modId: String) : ModsEvent
    data class Toggle(val modId: String, val enabled: Boolean) : ModsEvent
    data object RefreshIndex : ModsEvent

    data class ShowSnackbar(val message: String) : ModsEvent
}
