package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.model.OnlineSearchGroup
import com.greenrou.kanata.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow

class SearchOnlineUseCase(private val repository: SearchRepository) {
    operator fun invoke(query: String, isMangaMode: Boolean): Flow<List<OnlineSearchGroup>> =
        repository.searchOnline(query, isMangaMode)
}
