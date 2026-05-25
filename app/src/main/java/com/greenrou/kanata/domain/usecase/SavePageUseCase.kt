package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.SavedPagesManager

class SavePageUseCase(private val manager: SavedPagesManager) {
    suspend operator fun invoke(name: String, url: String) = manager.save(name, url)
}
