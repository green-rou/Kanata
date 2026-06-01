package com.greenrou.kanata.modapi

interface ModBundle {
    val siteParsers: List<ModSiteParser> get() = emptyList()
    val chapterParsers: List<ModChapterParser> get() = emptyList()
}
