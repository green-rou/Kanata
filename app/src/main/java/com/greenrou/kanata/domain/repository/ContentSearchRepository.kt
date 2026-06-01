package com.greenrou.kanata.domain.repository

import com.greenrou.kanata.domain.model.ContentSource
import kotlinx.coroutines.flow.Flow

interface ContentSearchRepository {
    fun searchAll(titles: List<String>): Flow<ContentSource>
}
