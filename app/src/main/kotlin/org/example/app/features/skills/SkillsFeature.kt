package org.example.app.features.skills

import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar

internal class SkillsFeature(
    private val skillService: SkillService =
        SkillService(),
) : Feature {

    override val id: String = "skills"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registrar.beforeInfoUpdate(
            priority = SKILLS_PRIORITY,
        ) { _, player ->
            skillService.syncInitial(player)
        }
    }

    private companion object {
        const val SKILLS_PRIORITY: Int = 90
    }
}