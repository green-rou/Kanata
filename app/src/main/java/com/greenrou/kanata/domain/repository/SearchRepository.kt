package com.greenrou.kanata.domain.repository

interface SearchRepository {
    suspend fun searchOnYummy(query: String): Result<String>
    suspend fun searchOnAniwave(query: String): Result<String>
}
