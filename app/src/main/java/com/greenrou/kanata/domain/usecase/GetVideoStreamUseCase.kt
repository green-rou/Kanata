package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.VideoRepository

class GetVideoStreamUseCase(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke(siteUrl: String): Result<String> = 
        videoRepository.getVideoStream(siteUrl)
}
