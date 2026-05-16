package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.DownloadRepository

class SetDownloadFolderUseCase(private val repo: DownloadRepository) {
    suspend operator fun invoke(path: String) = repo.setDownloadFolder(path)
}
