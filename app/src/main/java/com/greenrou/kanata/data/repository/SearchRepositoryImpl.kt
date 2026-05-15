package com.greenrou.kanata.data.repository

import com.greenrou.kanata.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URLEncoder

class SearchRepositoryImpl : SearchRepository {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    override suspend fun searchOnYummy(query: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://yummyanime.tv/index.php?do=search&subaction=search&search_start=0&full_search=0&story=$encodedQuery"
            val document = Jsoup.connect(url).userAgent(userAgent).get()
            document.select(".movie-item__link").firstOrNull()?.attr("abs:href")
                ?: error("No results found on YummyAnime for query: $query")
        }
    }

    override suspend fun searchOnAniwave(query: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://aniwave.dk/?s=$encodedQuery"
            val document = Jsoup.connect(url).userAgent(userAgent).get()
            document.select(".listupd a").firstOrNull()?.attr("abs:href")
                ?: error("No results found on Aniwave for query: $query")
        }
    }
}
