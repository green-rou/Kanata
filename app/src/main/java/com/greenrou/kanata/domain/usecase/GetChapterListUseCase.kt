package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.ChapterListRepository

class GetChapterListUseCase(private val repository: ChapterListRepository) {
    suspend operator fun invoke(pageUrl: String) = repository.getChapters(pageUrl)
}
