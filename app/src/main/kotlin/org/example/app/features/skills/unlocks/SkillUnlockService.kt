package org.example.app.features.skills.unlocks

import org.example.app.core.skills.Skill

/** Lazily-initialized holder for the loaded [SkillUnlockRepository]. */
internal class SkillUnlockService {

    private var repository:
        SkillUnlockRepository? = null

    val isInitialized: Boolean
        get() =
            repository != null

    fun initialize(
        repository: SkillUnlockRepository,
    ) {
        check(this.repository == null) {
            "Skill unlock repository already initialized."
        }

        this.repository = repository
    }

    fun hasUnlocks(
        skill: Skill,
        firstLevel: Int,
        lastLevel: Int,
    ): Boolean {
        val repository =
            checkNotNull(repository) {
                "Skill unlock repository not initialized."
            }

        return repository.hasUnlocks(
            skill = skill,
            firstLevel = firstLevel,
            lastLevel = lastLevel,
        )
    }
}