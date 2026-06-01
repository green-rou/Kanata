package com.greenrou.kanata.data.mod

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.util.Log
import com.greenrou.kanata.domain.parser.ChapterParser
import com.greenrou.kanata.domain.parser.InfoProvider
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.modapi.ModBundle
import com.greenrou.kanata.modapi.ModChapterParser
import com.greenrou.kanata.modapi.ModContentProvider
import com.greenrou.kanata.modapi.ModDownloadFeature
import com.greenrou.kanata.modapi.ModInfoProvider
import com.greenrou.kanata.modapi.ModSiteParser
import dalvik.system.InMemoryDexClassLoader
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.ZipFile

class ModLoader(private val context: Context) {

    val modsDir: File
        get() = File(context.filesDir, "mods").also { it.mkdirs() }

    companion object {
        private const val TAG = "ModLoader"
    }

    fun loadAll(enabledFileNames: Set<String>): List<SiteParser> {
        Log.d(TAG, "loadAll: requested=${enabledFileNames}, found=${enabledApks(enabledFileNames).map { it.name }}")
        return enabledApks(enabledFileNames).flatMap { apk ->
            runCatching {
                val instance = instantiate(apk)
                Log.d(TAG, "loadAll: ${apk.name} → ${instance::class.java.name}")
                when (instance) {
                    is ModBundle -> instance.siteParsers.map { ModSiteParserAdapter(it) }.also {
                        Log.d(TAG, "loadAll: ModBundle with ${it.size} site parsers")
                    }
                    is ModSiteParser -> listOf(ModSiteParserAdapter(instance)).also {
                        Log.d(TAG, "loadAll: ModSiteParser loaded")
                    }
                    else -> emptyList<SiteParser>().also {
                        Log.w(TAG, "loadAll: ${apk.name} is not ModSiteParser or ModBundle — got ${instance::class.java.name}, interfaces=${instance::class.java.interfaces.map { it.name }}")
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "loadAll: failed to load ${apk.name} — ${e::class.java.simpleName}: ${e.message}", e)
            }.getOrDefault(emptyList())
        }
    }

    fun loadInfoProviders(enabledFileNames: Set<String>): List<InfoProvider> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModInfoProvider) ModInfoProviderAdapter(instance)
                else { Log.d(TAG, "loadInfoProviders: ${apk.name} is not ModInfoProvider"); null }
            }.onFailure { e ->
                Log.e(TAG, "loadInfoProviders: failed to load ${apk.name} — ${e.message}", e)
            }.getOrNull()
        }

    fun loadContentProviders(enabledFileNames: Set<String>): List<ModContentProvider> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModContentProvider) instance
                else { Log.d(TAG, "loadContentProviders: ${apk.name} is not ModContentProvider"); null }
            }.onFailure { e ->
                Log.e(TAG, "loadContentProviders: failed to load ${apk.name} — ${e.message}", e)
            }.getOrNull()
        }

    fun loadChapterParsers(enabledFileNames: Set<String>): List<ChapterParser> {
        Log.d(TAG, "loadChapterParsers: requested=${enabledFileNames}")
        return enabledApks(enabledFileNames).flatMap { apk ->
            runCatching {
                val instance = instantiate(apk)
                when (instance) {
                    is ModBundle -> instance.chapterParsers.map { ChapterParserAdapter(it) }.also {
                        Log.d(TAG, "loadChapterParsers: ModBundle with ${it.size} chapter parsers")
                    }
                    is ModChapterParser -> listOf(ChapterParserAdapter(instance)).also {
                        Log.d(TAG, "loadChapterParsers: ModChapterParser loaded")
                    }
                    else -> emptyList<ChapterParser>().also {
                        Log.w(TAG, "loadChapterParsers: ${apk.name} is not ModChapterParser or ModBundle")
                    }
                }
            }.onFailure { e ->
                Log.e(TAG, "loadChapterParsers: failed to load ${apk.name} — ${e.message}", e)
            }.getOrDefault(emptyList())
        }
    }

    fun loadDownloadFeatures(enabledFileNames: Set<String>): List<ModDownloadFeature> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModDownloadFeature) instance
                else { Log.d(TAG, "loadDownloadFeatures: ${apk.name} is not ModDownloadFeature"); null }
            }.onFailure { e ->
                Log.e(TAG, "loadDownloadFeatures: failed to load ${apk.name} — ${e.message}", e)
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

    private fun enabledApks(enabledFileNames: Set<String>): List<File> {
        val all = modsDir.listFiles()?.map { it.name } ?: emptyList()
        Log.d(TAG, "modsDir=${modsDir.absolutePath}, all files=$all, requested=$enabledFileNames")
        return modsDir.listFiles { f -> f.extension == "apk" && f.name in enabledFileNames }?.toList()
            ?: emptyList()
    }

    fun tryInstantiate(apk: File): Any? = runCatching { instantiate(apk) }.getOrNull()

    private fun instantiate(apk: File): Any {
        val className = apk.nameWithoutExtension.substringAfter("__")
            .ifEmpty { error("APK '${apk.name}' missing class name (expected format: id__com.example.ClassName.apk)") }
        Log.d(TAG, "instantiate: ${apk.name} → class=$className")
        val loader = buildClassLoader(apk)
        return loader.loadClass(className).getDeclaredConstructor().newInstance()
    }

    private fun buildClassLoader(apk: File): ClassLoader {
        val buffers = mutableListOf<ByteBuffer>()
        ZipFile(apk).use { zip ->
            var i = 1
            while (true) {
                val entry = zip.getEntry(if (i == 1) "classes.dex" else "classes${i}.dex") ?: break
                buffers.add(ByteBuffer.wrap(zip.getInputStream(entry).readBytes()))
                i++
            }
        }
        check(buffers.isNotEmpty()) { "No classes.dex in ${apk.name}" }
        Log.d(TAG, "buildClassLoader: ${apk.name} → ${buffers.size} dex buffer(s)")
        return InMemoryDexClassLoader(buffers.toTypedArray(), ModApiClassLoader(context.classLoader))
    }

    private inner class ModApiClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name.startsWith("com.greenrou.kanata.modapi")) {
                return parent!!.loadClass(name)
            }
            return super.loadClass(name, resolve)
        }
    }
}
