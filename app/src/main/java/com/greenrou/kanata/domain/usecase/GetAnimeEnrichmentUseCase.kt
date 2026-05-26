package com.greenrou.kanata.domain.usecase

import com.greenrou.kanata.data.mod.InfoProviderRegistry
import com.greenrou.kanata.domain.model.AnimeEnrichment
import com.greenrou.kanata.domain.repository.SettingsManager
import kotlinx.coroutines.flow.first

class GetAnimeEnrichmentUseCase(
    private val registry: InfoProviderRegistry,
    private val settingsManager: SettingsManager,
) {
    suspend operator fun invoke(titles: List<String>): AnimeEnrichment? {
        val activeId = settingsManager.activeInfoProviderId.first() ?: return null
        val provider = registry.providers.value.firstOrNull { it.id == activeId } ?: return null
        return provider.getInfo(titles).getOrNull()
    }
}
