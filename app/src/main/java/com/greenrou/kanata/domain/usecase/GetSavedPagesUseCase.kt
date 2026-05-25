package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.SavedPagesManager

class GetSavedPagesUseCase(private val manager: SavedPagesManager) {
    operator fun invoke() = manager.getAll()
}
