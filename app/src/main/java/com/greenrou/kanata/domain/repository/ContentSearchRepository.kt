package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.ContentSource

interface ContentSearchRepository {
    suspend fun searchAll(titles: List<String>): List<ContentSource>
}
