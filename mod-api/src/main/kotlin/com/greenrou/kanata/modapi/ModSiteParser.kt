package com.greenrou.kanata.modapi

/**
 * Contract every source mod must implement.
 *
 * Build the mod APK with this module as `compileOnly` — the interface is
 * already present in the host app's classloader at runtime.
 *
 * The APK file must be named after the mod id: `<id>.apk`.
 * The entry class is declared via the mod index JSON (field "parserClass").
 */
interface ModSiteParser {
    /** Unique stable id, e.g. "source-animedub". Must match the APK file name. */
    val id: String

    /** Human-readable label shown in the UI. */
    val label: String

    /** BCP-47 language tag, e.g. "uk", "ru", "en". */
    val language: String

    val isAdultOnly: Boolean get() = false

    /** Return true if this parser can handle the given host name. */
    fun supports(host: String): Boolean

    /**
     * Search for [query] and return the URL of the anime page on this source,
     * or a failure if not found.
     */
    suspend fun search(query: String): Result<String>

    /** Parse all episodes from [pageUrl]. */
    suspend fun getEpisodes(pageUrl: String): List<ModEpisode>
}
