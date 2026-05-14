package com.greenrou.kanata.core.di

import com.apollographql.apollo.ApolloClient
import com.greenrou.kanata.data.repository.AniListRepositoryImpl
import com.greenrou.kanata.domain.repository.AniListRepository
import org.koin.dsl.module

val aniListModule = module {
    single {
        ApolloClient.Builder()
            .serverUrl("https://graphql.anilist.co")
            .build()
    }

    single<AniListRepository> { AniListRepositoryImpl(get()) }
}
