package com.greenrou.kanata.data.repository

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.greenrou.kanata.data.remote.anilist.GetAnimeByMoodQuery
import com.greenrou.kanata.data.remote.anilist.GetAnimeDetailQuery
import com.greenrou.kanata.data.remote.anilist.GetAnimeListQuery
import com.greenrou.kanata.data.remote.dto.stripHtml
import com.greenrou.kanata.domain.model.Anime
import com.greenrou.kanata.domain.model.AnimeListPage
import com.greenrou.kanata.domain.repository.AniListRepository

class AniListRepositoryImpl(
    private val apolloClient: ApolloClient,
) : AniListRepository {

    override suspend fun getAnimeList(page: Int, perPage: Int, showAdultContent: Boolean): Result<AnimeListPage> = runCatching {
        val isAdult = if (showAdultContent) Optional.Present(true) else Optional.Present(false)
        val response = apolloClient
            .query(GetAnimeListQuery(page = page, perPage = perPage, isAdult = isAdult))
            .execute()
        if (!response.errors.isNullOrEmpty()) {
            error(response.errors!!.first().message ?: "GraphQL error")
        }
        val pageData = response.data?.Page
        val media = pageData?.media?.filterNotNull() ?: emptyList()
        AnimeListPage(
            items = media.map { it.toListItemDomain() },
            hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
            currentPage = pageData?.pageInfo?.currentPage ?: page,
        )
    }

    override suspend fun getAnimeById(id: Int): Result<Anime> = runCatching {
        val response = apolloClient
            .query(GetAnimeDetailQuery(id = id))
            .execute()
        if (!response.errors.isNullOrEmpty()) {
            error(response.errors!!.first().message ?: "GraphQL error")
        }
        response.data?.Media?.toDetailDomain()
            ?: error("Anime #$id not found")
    }

    override suspend fun getAnimeByMood(
        page: Int,
        perPage: Int,
        genres: List<String>?,
        tags: List<String>?,
        showAdultContent: Boolean
    ): Result<AnimeListPage> = runCatching {
        val isAdult = if (showAdultContent) Optional.Present(true) else Optional.Present(false)
        val response = apolloClient
            .query(GetAnimeByMoodQuery(
                page = page,
                perPage = perPage,
                genres = Optional.presentIfNotNull(genres),
                tags = Optional.presentIfNotNull(tags),
                isAdult = isAdult
            ))
            .execute()
        if (!response.errors.isNullOrEmpty()) {
            error(response.errors!!.first().message ?: "GraphQL error")
        }
        val pageData = response.data?.Page
        val media = pageData?.media?.filterNotNull() ?: emptyList()
        AnimeListPage(
            items = media.map { it.toMoodListItemDomain() },
            hasNextPage = pageData?.pageInfo?.hasNextPage ?: false,
            currentPage = pageData?.pageInfo?.currentPage ?: page,
        )
    }

    private fun GetAnimeListQuery.Medium.toListItemDomain() = Anime(
        id = id,
        title = title?.userPreferred ?: "",
        type = format?.rawValue ?: "",
        imageUrl = coverImage?.large ?: "",
        score = (averageScore ?: 0) / 10.0,
        synopsis = "",
        genres = genres?.filterNotNull() ?: emptyList(),
        episodes = episodes ?: 0,
    )

    private fun GetAnimeByMoodQuery.Medium.toMoodListItemDomain() = Anime(
        id = id,
        title = title?.userPreferred ?: "",
        type = format?.rawValue ?: "",
        imageUrl = coverImage?.large ?: "",
        score = (averageScore ?: 0) / 10.0,
        synopsis = "",
        genres = genres?.filterNotNull() ?: emptyList(),
        episodes = episodes ?: 0,
    )

    private fun GetAnimeDetailQuery.Media.toDetailDomain() = Anime(
        id = id,
        title = title?.userPreferred ?: "",
        type = format?.rawValue ?: "",
        imageUrl = coverImage?.large ?: "",
        score = (averageScore ?: 0) / 10.0,
        synopsis = description.orEmpty().stripHtml(),
        genres = genres?.filterNotNull() ?: emptyList(),
        episodes = episodes ?: 0,
    )
}
