package com.greenrou.kanata.data.repository

import com.greenrou.kanata.data.remote.AnnApi
import com.greenrou.kanata.data.remote.dto.toDomain
import com.greenrou.kanata.data.remote.parser.AnnXmlParser
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.repository.AnimeRepository

class AnimeRepositoryImpl(
    private val api: AnnApi,
) : AnimeRepository {

    override suspend fun getAnimeList(): Result<List<Anime>> = runCatching {
        val xml = api.getAnimeList()
        val items = AnnXmlParser.parseAnimeList(xml)
        val imageMap = runCatching {
            val ids = items.joinToString("~") { it.id.toString() }
            val detailXml = api.getAnimeDetails(ids)
            AnnXmlParser.parseImageUrls(detailXml)
        }.getOrDefault(emptyMap())
        items.map { dto -> dto.toDomain().copy(imageUrl = imageMap[dto.id] ?: "") }
    }

    override suspend fun getAnimeById(id: Int): Result<Anime> = runCatching {
        val xml = api.getAnimeDetail(id)
        AnnXmlParser.parseAnimeDetail(xml)?.toDomain()
            ?: error("Anime #$id not found in response")
    }
}
