package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.ContentPage

interface ContentPagesRepository {
    suspend fun getPages(chapterUrl: String): Result<List<ContentPage>>
}
