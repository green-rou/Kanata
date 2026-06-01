package com.greenrou.kanata.data.mod

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import com.greenrou.kanata.domain.parser.ChapterParser
import com.greenrou.kanata.domain.parser.InfoProvider
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.modapi.ModBundle
import com.greenrou.kanata.modapi.ModChapterParser
import com.greenrou.kanata.modapi.ModContentProvider
import com.greenrou.kanata.modapi.ModDownloadFeature
import com.greenrou.kanata.modapi.ModInfoProvider
import com.greenrou.kanata.modapi.ModSiteParser
import dalvik.system.DexClassLoader
import java.io.File

class ModLoader(private val context: Context) {

    val modsDir: File
        get() = File(context.filesDir, "mods").also { it.mkdirs() }

    fun loadAll(enabledFileNames: Set<String>): List<SiteParser> =
        enabledApks(enabledFileNames).flatMap { apk ->
            runCatching {
                when (val instance = instantiate(apk)) {
                    is ModBundle -> instance.siteParsers.map { ModSiteParserAdapter(it) }
                    is ModSiteParser -> listOf(ModSiteParserAdapter(instance))
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
        }

    fun loadInfoProviders(enabledFileNames: Set<String>): List<InfoProvider> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModInfoProvider) ModInfoProviderAdapter(instance) else null
            }.getOrNull()
        }

    fun loadContentProviders(enabledFileNames: Set<String>): List<ModContentProvider> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModContentProvider) instance else null
            }.getOrNull()
        }

    fun loadChapterParsers(enabledFileNames: Set<String>): List<ChapterParser> =
        enabledApks(enabledFileNames).flatMap { apk ->
            runCatching {
                when (val instance = instantiate(apk)) {
                    is ModBundle -> instance.chapterParsers.map { ChapterParserAdapter(it) }
                    is ModChapterParser -> listOf(ChapterParserAdapter(instance))
                    else -> emptyList()
                }
            }.getOrDefault(emptyList())
        }

    fun loadDownloadFeatures(enabledFileNames: Set<String>): List<ModDownloadFeature> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModDownloadFeature) instance else null
            }.getOrNull()
        }

    fun loadModResources(apkFileName: String): ModResources? {
        return try {
            val apkFile = File(modsDir, apkFileName)
            if (!apkFile.exists()) return null
            val packageInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                ?: return null
            @Suppress("DEPRECATION")
            val assetManager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            AssetManager::class.java
                .getMethod("addAssetPath", String::class.java)
                .invoke(assetManager, apkFile.absolutePath)
            @Suppress("DEPRECATION")
            val resources = Resources(
                assetManager,
                context.resources.displayMetrics,
                context.resources.configuration,
            )
            ModResources(resources, packageInfo.packageName)
        } catch (_: Exception) {
            null
        }
    }

    private fun enabledApks(enabledFileNames: Set<String>): List<File> =
        modsDir.listFiles { f -> f.extension == "apk" && f.name in enabledFileNames }?.toList()
            ?: emptyList()

    fun tryInstantiate(apk: File): Any? = runCatching { instantiate(apk) }.getOrNull()

    private fun instantiate(apk: File): Any {
        val className = apk.nameWithoutExtension.substringAfter("__")
            .ifEmpty { error("APK '${apk.name}' missing class name (expected format: id__com.example.ClassName.apk)") }
        val loader = DexClassLoader(
            apk.absolutePath,
            context.codeCacheDir.absolutePath,
            null,
            context.classLoader,
        )
        return loader.loadClass(className).getDeclaredConstructor().newInstance()
    }
}
