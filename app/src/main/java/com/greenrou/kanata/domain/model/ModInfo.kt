package com.greenrou.kanata.domain.model

data class ModInfo(
    val id: String,
    val label: String,
    val language: String,
    val version: Int,
    val description: String,
    val apkUrl: String,
    val parserClass: String,
    val isInstalled: Boolean,
    val isEnabled: Boolean,
    val installedVersion: Int?,
    val hasUpdate: Boolean,
)
