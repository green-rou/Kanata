package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.ContentPagesRepository

class GetContentPagesUseCase(private val repository: ContentPagesRepository) {
    suspend operator fun invoke(chapterUrl: String) = repository.getPages(chapterUrl)
}
