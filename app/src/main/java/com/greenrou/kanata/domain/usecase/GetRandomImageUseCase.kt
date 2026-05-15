package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.domain.repository.RandomRepository

class GetRandomImageUseCase(private val repository: RandomRepository) {
    suspend operator fun invoke(): Result<String> = repository.getRandomImage()
}
