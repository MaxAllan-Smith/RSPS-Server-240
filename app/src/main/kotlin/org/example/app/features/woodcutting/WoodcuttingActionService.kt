package org.example.app.features.woodcutting

import org.example.app.core.engine.GameContext
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.world.WorldLocService
import kotlin.random.Random

/**
 * Owns the active Woodcutting action loop.
 *
 * Selection/routing remains in WoodcuttingFeature. Once interaction range is
 * reached this service owns:
 *
 * - requirements;
 * - tool validation;
 * - animation;
 * - resource rolls;
 * - rewards;
 * - tree depletion.
 */
internal class WoodcuttingActionService(
    private val axeService:
        WoodcuttingAxeService,

    private val worldLocs:
        WorldLocService,

    private val random:
        Random =
        Random.Default,
) {

    /**
     * Attempts to begin chopping the selected tree.
     */
    fun start(
        player: Player,
        state: WoodcuttingState,
        target: WoodcuttingTarget,
    ) {
        /*
         * Another player may have felled the tree while this player was
         * walking toward it.
         */
        if (
            worldLocs.isOverridden(
                position =
                    target.position,
                shape =
                    target.tree.locShape,
            )
        ) {
            state.clear()

            println(
                "[Woodcutting] '${player.username}' could not start " +
                    "chopping ${target.tree.name}: tree is depleted."
            )

            return
        }

        val woodcuttingLevel =
            player.skills.currentLevel(
                Skill.WOODCUTTING
            )

        if (
            woodcuttingLevel <
            target.tree.requiredLevel
        ) {
            player.sendGameMessage(
                "You need a Woodcutting level of " +
                    "${target.tree.requiredLevel} " +
                    "to chop this tree."
            )

            println(
                "[Woodcutting] '${player.username}' cannot chop " +
                    "${target.tree.name}: " +
                    "woodcutting=$woodcuttingLevel, " +
                    "required=${target.tree.requiredLevel}."
            )

            state.clear()

            return
        }

        val selection =
            axeService.findBestUsable(
                player = player,
            )

        if (selection == null) {
            handleMissingAxe(
                player = player,
            )

            state.clear()

            return
        }

        val reward =
            target.tree.reward

        val successRate =
            WoodcuttingSuccessRates.find(
                tree =
                    target.tree,
                axe =
                    selection.axe,
            )

        /*
         * Oak and Willow are recognized/routable, but their resource loops
         * remain deliberately disabled until fractional XP is represented
         * globally.
         */
        if (
            reward == null ||
            successRate == null
        ) {
            println(
                "[Woodcutting] '${player.username}' reached " +
                    "${target.tree.name}, but its resource loop " +
                    "is not enabled yet."
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

        val extendedInfo =
            player.infos
                .playerInfo
                .avatar
                .extendedInfo

        extendedInfo.setFaceLoc(
            x =
                target.position.x,
            z =
                target.position.z,
            sizeX =
                1,
            sizeZ =
                1,
            instant =
                false,
            walkMode =
                0,
        )

        extendedInfo.setSequence(
            id =
                selection.axe.animationId,
            delay =
                0,
        )

        state.action =
            WoodcuttingAction(
                target =
                    target,
                approachPosition =
                    approach,
                axe =
                    selection.axe,
                ticksUntilRoll =
                    ROLL_INTERVAL_TICKS,
            )

        println(
            "[Woodcutting] '${player.username}' started chopping " +
                "${target.tree.name} using " +
                "${selection.axe.name}."
        )
    }

    /**
     * Advances one active Woodcutting action by one 600ms game tick.
     */
    fun cycle(
        context: GameContext,
        player: Player,
        state: WoodcuttingState,
    ) {
        val action =
            state.action
                ?: return

        /*
         * Shared world state wins over the player's local action.
         *
         * This handles another player felling the same tree first.
         */
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
                reason =
                    "tree depleted",
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
                reason =
                    "player moved away",
            )

            return
        }

        /*
         * Re-resolve the best tool while chopping.
         *
         * Moving the same axe between inventory/equipment remains valid.
         */
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
                reason =
                    "no usable axe",
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
                axe =
                    action.axe,
            )
        }

        action.ticksUntilRoll--

        if (
            action.ticksUntilRoll > 0
        ) {
            return
        }

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

        if (
            !successRate.succeeds(
                level = level,
                random = random,
            )
        ) {
            playAnimation(
                player = player,
                axe =
                    action.axe,
            )

            action.ticksUntilRoll =
                ROLL_INTERVAL_TICKS

            return
        }

        reward(
            context = context,
            player = player,
            state = state,
            action = action,
        )
    }

    /**
     * Explicitly stops any active chopping action.
     */
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

    private fun reward(
        context: GameContext,
        player: Player,
        state: WoodcuttingState,
        action: WoodcuttingAction,
    ) {
        val tree =
            action.target.tree

        val reward =
            checkNotNull(
                tree.reward
            )

        /*
         * Check capacity before committing the world-state change.
         *
         * Once we fell the tree it becomes globally depleted, so we do not
         * want to do that when the player cannot receive the reward.
         */
        if (
            !player.inventory
                .hasFreeSlot()
        ) {
            player.sendGameMessage(
                "Your inventory is too full to hold any more logs."
            )

            cancel(
                player = player,
                state = state,
                reason =
                    "inventory full",
            )

            return
        }

        val stumpId =
            tree.stumpId

        val respawnRange =
            tree.respawnTicks

        if (
            stumpId == null ||
            respawnRange == null
        ) {
            cancel(
                player = player,
                state = state,
                reason =
                    "missing depletion definition",
            )

            return
        }

        val respawnTicks =
            randomTicks(
                respawnRange
            )

        /*
         * Deplete atomically before awarding the resource.
         *
         * If another player has already depleted this exact loc/layer, this
         * player does not receive another final log from the same resource.
         */
        val depleted =
            worldLocs.replace(
                context = context,

                originalId =
                    action.target.locId,

                replacementId =
                    stumpId,

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
                    "tree already depleted",
            )

            return
        }

        /*
         * Capacity was checked immediately before depletion and the game runs
         * on one authoritative game-engine thread, so this mutation should now
         * always succeed.
         */
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

        player.skills.addExperience(
            skill =
                Skill.WOODCUTTING,
            amount =
                reward.experience,
        )

        player.sendGameMessage(
            "You get some ${reward.itemName}."
        )

        println(
            "[Woodcutting] '${player.username}' cut " +
                "${reward.itemName} " +
                "item=${reward.itemId}, " +
                "xp=${reward.experience}; " +
                "${tree.name} depleted for " +
                "$respawnTicks ticks."
        )

        cancel(
            player = player,
            state = state,
        )
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

            println(
                "[Woodcutting] '${player.username}' has no axe available."
            )

            return
        }

        val currentLevel =
            player.skills.currentLevel(
                Skill.WOODCUTTING
            )

        player.sendGameMessage(
            "You need a Woodcutting level of " +
                "${available.axe.woodcuttingLevel} " +
                "to use a ${available.axe.name.lowercase()}."
        )

        println(
            "[Woodcutting] '${player.username}' cannot use " +
                "${available.axe.name}: " +
                "woodcutting=$currentLevel, " +
                "required=${available.axe.woodcuttingLevel}."
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

        /**
         * Standard Woodcutting skilling-roll interval.
         */
        const val ROLL_INTERVAL_TICKS: Int =
            4
    }
}