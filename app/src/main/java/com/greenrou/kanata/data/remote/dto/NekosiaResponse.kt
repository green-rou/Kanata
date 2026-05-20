package com.greenrou.kanata.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NekosiaResponse(
    val success: Boolean,
    val status: Int,
    val image: NekosiaImage,
)

@Serializable
data class NekosiaImage(
    val original: NekosiaUrl,
    val compressed: NekosiaUrl,
)

@Serializable
data class NekosiaUrl(
    val url: String,
    val extension: String,
)
