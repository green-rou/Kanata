package com.greenrou.kanata.data.mod

import android.content.res.Resources

class ModResources(
    private val resources: Resources,
    private val packageName: String,
) {
    fun getString(name: String): String? {
        val id = resources.getIdentifier(name, "string", packageName)
        return if (id != 0) resources.getString(id) else null
    }
}
