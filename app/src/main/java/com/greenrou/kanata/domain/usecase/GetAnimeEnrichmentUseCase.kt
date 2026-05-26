package com.greenrou.kanata.domain.usecase

import android.util.Log
import com.greenrou.kanata.data.mod.InfoProviderRegistry
import com.greenrou.kanata.domain.model.AnimeEnrichment
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.flow.first

class GetAnimeEnrichmentUseCase(
    private val registry: InfoProviderRegistry,
    private val settingsManager: SettingsManager,
) {
    suspend operator fun invoke(titles: List<String>): AnimeEnrichment? {
        val activeId = settingsManager.activeInfoProviderId.first()
        Log.d(TAG, "invoke: activeId=$activeId, titles=$titles")
        activeId ?: return null

        val availableProviders = registry.providers.value.map { it.id }
        Log.d(TAG, "invoke: availableProviders=$availableProviders")

        val provider = registry.providers.value.firstOrNull { it.id == activeId }
        if (provider == null) {
            Log.w(TAG, "invoke: provider '$activeId' not found in registry")
            return null
        }

        Log.d(TAG, "invoke: calling provider '${provider.id}'")
        val result = provider.getInfo(titles)
        result.onFailure { Log.e(TAG, "invoke: provider failed", it) }
        return result.getOrNull()
    }

    private companion object {
        const val TAG = "GetAnimeEnrichment"
    }
}
