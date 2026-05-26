package com.greenrou.kanata.data.mod

import android.content.Context
import android.util.Log
import com.greenrou.kanata.domain.parser.InfoProvider
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.modapi.ModInfoProvider
import com.greenrou.kanata.modapi.ModSiteParser
import dalvik.system.DexClassLoader
import java.io.File

class ModLoader(private val context: Context) {

    val modsDir: File
        get() = File(context.filesDir, "mods").also { it.mkdirs() }

    fun loadAll(enabledFileNames: Set<String>): List<SiteParser> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModSiteParser) ModSiteParserAdapter(instance) else null
            }
                .onFailure { Log.e("ModLoader", "Failed to load parser ${apk.name}", it) }
                .getOrNull()
        }

    fun loadInfoProviders(enabledFileNames: Set<String>): List<InfoProvider> =
        enabledApks(enabledFileNames).mapNotNull { apk ->
            runCatching {
                val instance = instantiate(apk)
                if (instance is ModInfoProvider) ModInfoProviderAdapter(instance) else null
            }
                .onFailure { Log.e("ModLoader", "Failed to load info provider ${apk.name}", it) }
                .getOrNull()
        }

    private fun enabledApks(enabledFileNames: Set<String>): List<File> =
        modsDir.listFiles { f -> f.extension == "apk" && f.name in enabledFileNames }?.toList()
            ?: emptyList()

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
