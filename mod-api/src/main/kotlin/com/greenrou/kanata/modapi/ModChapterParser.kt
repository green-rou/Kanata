package com.greenrou.kanata.modapi

interface ModChapterParser {
    val id: String
    val label: String
    val language: String
    fun supports(host: String): Boolean
    suspend fun search(query: String): Result<String>
    suspend fun getChapters(pageUrl: String): List<ModChapter>
    suspend fun getPages(chapterUrl: String): List<ModPage>
}
