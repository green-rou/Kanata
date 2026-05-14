package com.greenrou.kanata.data.remote.parser

import com.greenrou.kanata.data.remote.dto.AnimeDetailDto
import com.greenrou.kanata.data.remote.dto.AnimeListItemDto
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

object AnnXmlParser {

    fun parseAnimeList(xml: String): List<AnimeListItemDto> {
        val items = mutableListOf<AnimeListItemDto>()
        val parser = newParser(xml)

        var inItem = false
        var currentTag: String? = null
        val currentText = StringBuilder()
        var id: Int? = null
        var name: String? = null
        var type = ""
        var precision = ""

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> {
                        inItem = true
                        id = null; name = null; type = ""; precision = ""
                    }
                    else -> if (inItem) {
                        currentTag = parser.name
                        currentText.clear()
                    }
                }
                XmlPullParser.TEXT -> if (inItem) currentText.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "item" -> {
                        if (id != null && !name.isNullOrBlank()) {
                            items.add(AnimeListItemDto(id!!, name!!, precision.ifEmpty { type }))
                        }
                        inItem = false
                    }
                    else -> if (inItem) {
                        val v = currentText.toString().trim()
                        when (currentTag) {
                            "id" -> id = v.toIntOrNull()
                            "name" -> name = v
                            "type" -> type = v
                            "precision" -> precision = v
                        }
                        currentTag = null
                        currentText.clear()
                    }
                }
            }
            parser.next()
        }
        return items
    }

    fun parseAnimeDetail(xml: String): AnimeDetailDto? {
        val parser = newParser(xml)

        var id: Int? = null
        var name: String? = null
        var type: String = ""
        var imageUrl: String = ""
        var score: Double = 0.0
        var synopsis: String = ""
        val genres = mutableListOf<String>()
        var episodes: Int = 0

        var inAnime = false
        var currentInfoType: String? = null
        val currentText = StringBuilder()

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {

                XmlPullParser.START_TAG -> when (parser.name) {
                    "anime" -> {
                        inAnime = true
                        id = parser.getAttributeValue(null, "id")?.toIntOrNull()
                        name = parser.getAttributeValue(null, "name")
                        type = parser.getAttributeValue(null, "type") ?: ""
                    }
                    "info" -> if (inAnime) {
                        currentInfoType = parser.getAttributeValue(null, "type")
                        currentText.clear()
                    }
                    "img" -> if (inAnime && currentInfoType == "Picture") {
                        imageUrl = parser.getAttributeValue(null, "src") ?: ""
                    }
                    "ratings" -> if (inAnime) {
                        score = parser.getAttributeValue(null, "weighted_score")
                            ?.toDoubleOrNull() ?: 0.0
                    }
                }

                XmlPullParser.TEXT -> if (inAnime && currentInfoType != null) {
                    currentText.append(parser.text)
                }

                XmlPullParser.END_TAG -> when (parser.name) {
                    "info" -> {
                        val content = currentText.toString().trim()
                        when (currentInfoType) {
                            "Plot Summary" -> synopsis = content
                            "Number of episodes" -> episodes = content.toIntOrNull() ?: 0
                            "Genres" -> if (content.isNotEmpty()) genres.add(content)
                        }
                        currentInfoType = null
                        currentText.clear()
                    }
                    "anime" -> inAnime = false
                }
            }
            parser.next()
        }

        return if (id != null && !name.isNullOrBlank()) {
            AnimeDetailDto(id!!, name!!, type, imageUrl, score, synopsis, genres, episodes)
        } else null
    }

    fun parseImageUrls(xml: String): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val parser = newParser(xml)
        var currentId: Int? = null
        var inAnime = false
        var currentInfoType: String? = null

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "anime" -> {
                        inAnime = true
                        currentId = parser.getAttributeValue(null, "id")?.toIntOrNull()
                    }
                    "info" -> if (inAnime) {
                        currentInfoType = parser.getAttributeValue(null, "type")
                    }
                    "img" -> if (inAnime && currentInfoType == "Picture" && currentId != null) {
                        val src = parser.getAttributeValue(null, "src")
                        if (!src.isNullOrEmpty() && !result.containsKey(currentId)) {
                            result[currentId!!] = src
                        }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "anime" -> {
                        inAnime = false
                        currentId = null
                        currentInfoType = null
                    }
                    "info" -> currentInfoType = null
                }
            }
            parser.next()
        }
        return result
    }

    private fun newParser(xml: String): XmlPullParser =
        XmlPullParserFactory.newInstance().newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
}
