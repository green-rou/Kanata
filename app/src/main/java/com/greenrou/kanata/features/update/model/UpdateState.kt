package com.greenrou.kanata.features.update.model

import com.greenrou.kanata.domain.model.AppRelease

data class UpdateState(
    val isChecking: Boolean = false,
    val pendingRelease: AppRelease? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val error: String? = null,
    val noUpdatesAvailable: Boolean = false,
)
