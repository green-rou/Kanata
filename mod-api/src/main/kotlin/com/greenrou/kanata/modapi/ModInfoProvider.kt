package com.greenrou.kanata.modapi

interface ModInfoProvider {
    val id: String
    val label: String
    suspend fun getInfo(titles: List<String>): Result<ModAnimeInfo>
}
