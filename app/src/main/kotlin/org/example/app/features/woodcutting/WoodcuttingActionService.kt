package org.example.app.features.woodcutting

import org.example.app.core.engine.GameContext
import org.example.app.core.experience.ExperienceService
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.world.WorldLocService
import kotlin.random.Random

/**
 * Owns active Woodcutting actions after the player reaches a resource.
 *
 * The engine supports:
 *
 * - immediate-depletion trees;
 * - multi-log timer trees;
 * - random-depletion trees;
 * - randomized independent success rolls;
 * - level-scaled success chance;
 * - global XP-rate handling;
 * - fractional canonical XP;
 * - shared-world tree depletion.
 */
internal class WoodcuttingActionService(
    private val axeService:
        WoodcuttingAxeService,

    private val worldLocs:
        WorldLocService,

    private val resources:
        WoodcuttingResourceService,

    private val experience:
        ExperienceService,

    private val random:
        Random =
        Random.Default,
) {

    fun start(
        player: Player,
        state: WoodcuttingState,
        target: WoodcuttingTarget,
    ) {
        if (
            worldLocs.isOverridden(
                position =
                    target.position,

                shape =
                    target.tree
                        .locShape,
            )
        ) {
            state.clear()

            println(
                "[Woodcutting] '${player.username}' could not start " +
                    "chopping ${target.tree.name}: resource is depleted."
            )

            return
        }

        val level =
            player.skills.currentLevel(
                Skill.WOODCUTTING
            )

        if (
            level <
            target.tree.requiredLevel
        ) {
            player.sendGameMessage(
                "You need a Woodcutting level of " +
                    "${target.tree.requiredLevel} " +
                    "to chop down this tree."
            )

            println(
                "[Woodcutting] '${player.username}' cannot chop " +
                    "${target.tree.name}: " +
                    "woodcutting=$level, " +
                    "required=${target.tree.requiredLevel}."
            )

            state.clear()

            return
        }

        val axe =
            axeService.findBestUsable(
                player = player,
            )

        if (axe == null) {
            handleMissingAxe(
                player = player,
            )

            state.clear()

            return
        }

        val successRate =
            WoodcuttingSuccessRates.find(
                tree =
                    target.tree,

                axe =
                    axe.axe,
            )

        if (successRate == null) {
            println(
                "[Woodcutting] '${player.username}' has no supported " +
                    "success-rate definition for " +
                    "${target.tree.name} using ${axe.axe.name}."
            )

            state.clear()

            return
        }

        val approach =
            checkNotNull(
                target.approachPosition
            ) {
                "Woodcutting action started without an approach position."
            }

        faceTree(
            player = player,
            target = target,
        )

        playAnimation(
            player = player,
            axe = axe.axe,
        )

        state.action =
            WoodcuttingAction(
                target = target,

                approachPosition =
                    approach,

                axe =
                    axe.axe,

                /*
                 * Always show the chopping animation before the first resource
                 * roll.
                 */
                ticksUntilRoll =
                    INITIAL_SWING_TICKS,
            )

        println(
            "[Woodcutting] '${player.username}' started chopping " +
                "${target.tree.name} using ${axe.axe.name}; " +
                "level=$level."
        )
    }

    fun cycle(
        context: GameContext,
        player: Player,
        state: WoodcuttingState,
    ) {
        val action =
            state.action
                ?: return

        if (
            worldLocs.isOverridden(
                position =
                    action.target.position,

                shape =
                    action.target.tree
                        .locShape,
            )
        ) {
            cancel(
                player = player,
                state = state,
                reason = "resource depleted",
            )

            return
        }

        if (
            player.position !=
            action.approachPosition
        ) {
            cancel(
                player = player,
                state = state,
                reason = "player moved away",
            )

            return
        }

        val selection =
            axeService.findBestUsable(
                player = player,
            )

        if (selection == null) {
            player.sendGameMessage(
                "You do not have an axe which you can use."
            )

            cancel(
                player = player,
                state = state,
                reason = "no usable axe",
            )

            return
        }

        if (
            selection.axe.id !=
            action.axe.id
        ) {
            action.axe =
                selection.axe

            playAnimation(
                player = player,
                axe = action.axe,
            )
        }

        /*
         * Timer-based trees advance their shared active-cut lifetime while at
         * least one player is actually chopping them.
         */
        resources.markActive(
            action.target
        )

        action.ticksUntilRoll--

        if (
            action.ticksUntilRoll > 0
        ) {
            return
        }

        performResourceRoll(
            context = context,
            player = player,
            state = state,
            action = action,
        )
    }

    private fun performResourceRoll(
        context: GameContext,
        player: Player,
        state: WoodcuttingState,
        action: WoodcuttingAction,
    ) {
        val successRate =
            WoodcuttingSuccessRates.find(
                tree =
                    action.target.tree,

                axe =
                    action.axe,
            )
                ?: run {
                    cancel(
                        player = player,
                        state = state,
                        reason =
                            "missing success-rate definition",
                    )

                    return
                }

        val level =
            player.skills.currentLevel(
                Skill.WOODCUTTING
            )

        val success =
            successRate.succeeds(
                level = level,
                random = random,
            )

        println(
            "[Woodcutting] '${player.username}' resource roll " +
                "for ${action.target.tree.name}: " +
                "level=$level, success=$success."
        )

        if (success) {
            reward(
                context = context,
                player = player,
                state = state,
                action = action,
            )

            return
        }

        playAnimation(
            player = player,
            axe = action.axe,
        )

        action.ticksUntilRoll =
            randomRetryTicks()
    }

    /**
     * Awards one successful Woodcutting product.
     *
     * Multi-log trees continue chopping after successful logs until their
     * depletion rule says the resource should disappear.
     */
    private fun reward(
        context: GameContext,
        player: Player,
        state: WoodcuttingState,
        action: WoodcuttingAction,
    ) {
        val tree =
            action.target.tree

        val reward =
            tree.reward

        if (
            !player.inventory
                .hasFreeSlot()
        ) {
            player.sendGameMessage(
                "Your inventory is too full to hold any more " +
                    "${reward.itemName}."
            )

            cancel(
                player = player,
                state = state,
                reason = "inventory full",
            )

            return
        }

        val shouldDeplete =
            resources.shouldDeplete(
                target =
                    action.target,

                random =
                    random,
            )

        if (shouldDeplete) {
            val stump =
                tree.stumpFor(
                    action.target.locId
                )
                    ?: run {
                        cancel(
                            player = player,
                            state = state,
                            reason =
                                "missing stump definition",
                        )

                        return
                    }

            val respawnTicks =
                randomTicks(
                    tree.respawnTicks
                )

            val depleted =
                worldLocs.replace(
                    context =
                        context,

                    originalId =
                        action.target.locId,

                    replacementId =
                        stump,

                    position =
                        action.target.position,

                    shape =
                        tree.locShape,

                    rotation =
                        tree.locRotation,

                    respawnTicks =
                        respawnTicks,

                    soundId =
                        tree.fallSoundId,
                )

            if (!depleted) {
                cancel(
                    player = player,
                    state = state,
                    reason =
                        "resource already depleted",
                )

                return
            }

            resources.clear(
                action.target
            )
        }

        check(
            player.inventory.add(
                ItemStack(
                    id =
                        reward.itemId,

                    amount =
                        1,
                )
            )
        ) {
            "Inventory capacity changed unexpectedly during Woodcutting reward."
        }

        val xpAward =
            experience.award(
                player = player,

                skill =
                    Skill.WOODCUTTING,

                baseExperienceMilli =
                    reward.experienceMilli,
            )

        player.sendGameMessage(
            "You get some ${reward.itemName}."
        )

        println(
            "[Woodcutting] '${player.username}' cut " +
                "${reward.itemName} " +
                "item=${reward.itemId}, " +
                "baseXp=${reward.formattedExperience()}, " +
                "awardedXp=${xpAward.awardedExperience}."
        )

        if (shouldDeplete) {
            println(
                "[Woodcutting] '${player.username}' depleted " +
                    "${tree.name}."
            )

            cancel(
                player = player,
                state = state,
            )

            return
        }

        /*
         * Tree is still alive.
         *
         * Continue chopping automatically rather than requiring another click.
         */
        playAnimation(
            player = player,
            axe = action.axe,
        )

        action.ticksUntilRoll =
            randomRetryTicks()
    }

    fun cancel(
        player: Player,
        state: WoodcuttingState,
        reason: String? = null,
    ) {
        if (
            state.action != null
        ) {
            player.infos
                .playerInfo
                .avatar
                .extendedInfo
                .setSequence(
                    id = -1,
                    delay = 0,
                )

            if (reason != null) {
                println(
                    "[Woodcutting] '${player.username}' stopped chopping: " +
                        "$reason."
                )
            }
        }

        state.clear()
    }

    private fun handleMissingAxe(
        player: Player,
    ) {
        val available =
            axeService.findBestAvailable(
                player = player,
            )

        if (available == null) {
            player.sendGameMessage(
                "You do not have an axe which you can use."
            )

            return
        }

        player.sendGameMessage(
            "You need a Woodcutting level of " +
                "${available.axe.woodcuttingLevel} " +
                "to use a ${available.axe.name.lowercase()}."
        )
    }

    private fun faceTree(
        player: Player,
        target: WoodcuttingTarget,
    ) {
        player.infos
            .playerInfo
            .avatar
            .extendedInfo
            .setFaceLoc(
                x =
                    target.position.x,

                z =
                    target.position.z,

                sizeX = 1,
                sizeZ = 1,
                instant = false,
                walkMode = 0,
            )
    }

    private fun playAnimation(
        player: Player,
        axe: WoodcuttingAxe,
    ) {
        player.infos
            .playerInfo
            .avatar
            .extendedInfo
            .setSequence(
                id =
                    axe.animationId,

                delay =
                    0,
            )
    }

    private fun randomRetryTicks(): Int =
        random.nextInt(
            from =
                MIN_RETRY_TICKS,

            until =
                MAX_RETRY_TICKS + 1,
        )

    private fun randomTicks(
        range: IntRange,
    ): Int {
        if (
            range.first ==
            range.last
        ) {
            return range.first
        }

        return random.nextInt(
            from =
                range.first,

            until =
                range.last + 1,
        )
    }

    private companion object {
        const val INITIAL_SWING_TICKS: Int =
            2

        const val MIN_RETRY_TICKS: Int =
            2

        const val MAX_RETRY_TICKS: Int =
            4
    }
}