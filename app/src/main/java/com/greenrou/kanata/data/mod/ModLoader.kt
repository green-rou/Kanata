package com.greenrou.kanata.data.mod

import android.content.Context
import android.util.Log
import com.greenrou.kanata.domain.parser.SiteParser
import com.greenrou.kanata.modapi.ModSiteParser
import dalvik.system.DexClassLoader
import java.io.File

class ModLoader(private val context: Context) {

    val modsDir: File
        get() = File(context.filesDir, "mods").also { it.mkdirs() }

    fun loadAll(): List<SiteParser> =
        modsDir.listFiles { f -> f.extension == "apk" }
            ?.mapNotNull { apk ->
                runCatching { loadFromApk(apk) }
                    .onFailure { Log.e("ModLoader", "Failed to load ${apk.name}", it) }
                    .getOrNull()
            }
            ?: emptyList()

    private fun loadFromApk(apk: File): SiteParser {
        val className = apk.nameWithoutExtension.substringAfter("__")
            .ifEmpty { error("APK '${apk.name}' missing class name (expected format: id__com.example.ClassName.apk)") }

        val loader = DexClassLoader(
            apk.absolutePath,
            context.codeCacheDir.absolutePath,
            null,
            context.classLoader,
        )
        val mod = loader.loadClass(className).getDeclaredConstructor().newInstance() as ModSiteParser
        return ModSiteParserAdapter(mod)
    }
}
