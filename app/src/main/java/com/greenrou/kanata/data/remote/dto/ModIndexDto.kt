package com.greenrou.kanata.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModIndexDto(
    val id: String,
    val label: String,
    val language: String,
    val version: Int,
    @SerialName("minAppVersion") val minAppVersion: Int = 1,
    @SerialName("isAdultOnly") val isAdultOnly: Boolean = false,
    val parserClass: String,
    val apkUrl: String,
    val description: String = "",
)
