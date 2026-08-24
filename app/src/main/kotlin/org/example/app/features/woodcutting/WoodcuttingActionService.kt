package org.example.app.features.woodcutting

import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import kotlin.random.Random

/**
 * Owns the active Woodcutting action loop.
 *
 * Tree selection/routing remains in WoodcuttingFeature. Once the player
 * reaches the selected interaction tile, this service owns validation,
 * animation, success rolls and rewards.
 */
internal class WoodcuttingActionService(
    private val axeService:
        WoodcuttingAxeService,
    private val random:
        Random = Random.Default,
) {

    /**
     * Attempts to begin chopping the selected tree.
     */
    fun start(
        player: Player,
        state: WoodcuttingState,
        target: WoodcuttingTarget,
    ) {
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
                tree = target.tree,
                axe = selection.axe,
            )

        /*
         * Oak/Willow resource loops are intentionally enabled later.
         *
         * Their fractional XP values need a generic XP representation rather
         * than silently rounding game data.
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

        /*
         * Keep the avatar oriented toward the selected world location while
         * chopping.
         */
        extendedInfo.setFaceLoc(
            x = target.position.x,
            z = target.position.z,
            sizeX = 1,
            sizeZ = 1,
            instant = false,
            walkMode = 0,
        )

        extendedInfo.setSequence(
            id =
                selection.axe.animationId,
            delay = 0,
        )

        state.action =
            WoodcuttingAction(
                target = target,
                approachPosition = approach,
                axe = selection.axe,
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
     * Advances one active Woodcutting action by one server game tick.
     */
    fun cycle(
        player: Player,
        state: WoodcuttingState,
    ) {
        val action =
            state.action
                ?: return

        /*
         * Walking away interrupts the action.
         */
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
         * Re-resolve the tool while chopping.
         *
         * Moving the axe between equipment and inventory remains valid, while
         * actually losing every usable axe cancels the action.
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
                axe = action.axe,
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
                tree = action.target.tree,
                axe = action.axe,
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
            /*
             * Restart the swing sequence for the next four-tick roll window.
             */
            playAnimation(
                player = player,
                axe = action.axe,
            )

            action.ticksUntilRoll =
                ROLL_INTERVAL_TICKS

            return
        }

        reward(
            player = player,
            state = state,
            action = action,
        )
    }

    /**
     * Explicitly stops any active chopping action.
     *
     * Used when another tree is selected and by action interruption.
     */
    fun cancel(
        player: Player,
        state: WoodcuttingState,
        reason: String? = null,
    ) {
        if (state.action != null) {
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
        player: Player,
        state: WoodcuttingState,
        action: WoodcuttingAction,
    ) {
        val reward =
            checkNotNull(
                action.target.tree.reward
            )

        val added =
            player.inventory.add(
                ItemStack(
                    id = reward.itemId,
                    amount = 1,
                )
            )

        if (!added) {
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

        player.skills.addExperience(
            skill = Skill.WOODCUTTING,
            amount = reward.experience,
        )

        player.sendGameMessage(
            "You get some ${reward.itemName}."
        )

        println(
            "[Woodcutting] '${player.username}' cut " +
                "${reward.itemName} " +
                "item=${reward.itemId}, " +
                "xp=${reward.experience}."
        )

        /*
         * A normal tree yields one log before depleting in OSRS.
         *
         * Dynamic loc replacement/stump spawning is the next world-state step,
         * so for now the action ends after the successful log.
         */
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
                id = axe.animationId,
                delay = 0,
            )
    }

    private companion object {

        /**
         * Standard Woodcutting resource roll cadence:
         *
         * 4 server game ticks × 600 ms = 2.4 seconds between rolls.
         */
        const val ROLL_INTERVAL_TICKS: Int =
            4
    }
}