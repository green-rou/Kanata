package com.greenrou.kanata.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed_mods")
data class InstalledModEntity(
    @PrimaryKey val id: String,
    val label: String,
    val language: String,
    val version: Int,
    val apkFileName: String,
    val isEnabled: Boolean = true,
)
