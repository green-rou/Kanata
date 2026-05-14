package com.greenrou.kanata.core.network

import com.greenrou.kanata.data.remote.AnnApi
import com.greenrou.kanata.data.remote.NekosiaApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

private const val ANN_BASE_URL = "https://cdn.animenewsnetwork.com/encyclopedia/"

val networkModule = module {
    single {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    single(named("ann_retrofit")) {
        Retrofit.Builder()
            .baseUrl(ANN_BASE_URL)
            .client(get())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    single(named("nekosia_retrofit")) {
        val json = Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true
        }
        Retrofit.Builder()
            .baseUrl(NekosiaApi.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    single { get<Retrofit>(named("ann_retrofit")).create(AnnApi::class.java) }
    single { get<Retrofit>(named("nekosia_retrofit")).create(NekosiaApi::class.java) }
}
