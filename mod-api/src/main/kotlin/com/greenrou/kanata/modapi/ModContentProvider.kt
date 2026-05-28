package com.greenrou.kanata.modapi

interface ModContentProvider {
    val id: String
    val label: String
    val mediaType: String
    val hasStreamSources: Boolean
}
