package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.SavedPagesManager

class DeleteSavedPageUseCase(private val manager: SavedPagesManager) {
    suspend operator fun invoke(id: Long) = manager.delete(id)
}
