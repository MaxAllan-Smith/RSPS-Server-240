package org.example.app.features.woodcutting

import org.example.app.core.engine.GameContext
import org.example.app.core.items.ItemStack
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.world.WorldLocService
import kotlin.random.Random

/**
 * Owns active Woodcutting actions after the player reaches a tree.
 *
 * Responsibilities:
 *
 * - skill requirements;
 * - axe requirements;
 * - immediate chopping animation;
 * - level-scaled success rolls;
 * - bounded unlucky streaks;
 * - inventory rewards;
 * - Woodcutting XP;
 * - tree depletion.
 *
 * Tree selection and movement remain in [WoodcuttingFeature].
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
     * Begins chopping immediately after the player reaches their selected
     * interaction position.
     *
     * The animation is queued here - not when the first resource roll occurs.
     *
     * The first resource roll is intentionally delayed by
     * [INITIAL_SWING_TICKS], ensuring the player visibly swings the axe before
     * a tree is allowed to fall.
     */
    fun start(
        player: Player,
        state: WoodcuttingState,
        target: WoodcuttingTarget,
    ) {
        /*
         * Another player may have felled this tree while we were walking toward
         * it.
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
         * Oak and Willow are routable and validate their requirements, but
         * their reward loops remain disabled until the global XP service can
         * represent fractional canonical XP.
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

        /*
         * Facing and the chopping animation are both registered immediately.
         *
         * This happens in the same game cycle in which Woodcutting detects that
         * movement reached the interaction tile.
         */
        faceTree(
            player = player,
            target = target,
        )

        playAnimation(
            player = player,
            axe =
                selection.axe,
        )

        state.action =
            WoodcuttingAction(
                target =
                    target,

                approachPosition =
                    approach,

                axe =
                    selection.axe,

                /*
                 * The first resource roll cannot happen until the player has
                 * visibly been chopping for this many complete game ticks.
                 */
                ticksUntilRoll =
                    INITIAL_SWING_TICKS,

                rollAttempts =
                    0,
            )

        println(
            "[Woodcutting] '${player.username}' started chopping " +
                "${target.tree.name} using " +
                "${selection.axe.name}; " +
                "level=$woodcuttingLevel."
        )
    }

    /**
     * Advances one active Woodcutting action by one 600 ms game cycle.
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
         * Global world state takes precedence over this player's action.
         *
         * If another player successfully felled the tree first, this action
         * ends without granting a duplicate reward.
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

        /*
         * Walking away is an explicit interruption.
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
         * Resolve the tool every cycle.
         *
         * Moving an axe between inventory and equipment remains valid, while
         * losing every usable axe interrupts the action.
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

        /*
         * A better axe may become available while the action is running.
         */
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

        performResourceRoll(
            context = context,
            player = player,
            state = state,
            action = action,
        )
    }

    /**
     * Performs one actual Woodcutting resource roll.
     *
     * Success chance already scales continuously with current Woodcutting level
     * through [WoodcuttingSuccessRate].
     *
     * We additionally bound the maximum number of failed rolls. This removes
     * exceptionally long unlucky tails and makes high Woodcutting levels feel
     * consistently faster rather than merely statistically faster.
     */
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

        action.rollAttempts++

        val maximumAttempts =
            maximumAttempts(
                woodcuttingLevel =
                    level,
            )

        /*
         * Normal success roll.
         */
        val randomSuccess =
            successRate.succeeds(
                level =
                    level,

                random =
                    random,
            )

        /*
         * If RNG has been consistently unlucky, guarantee the final allowed
         * attempt.
         *
         * This preserves level-scaled probability while eliminating extreme
         * chopping times.
         */
        val guaranteedSuccess =
            action.rollAttempts >=
                maximumAttempts

        println(
            "[Woodcutting] '${player.username}' roll " +
                "${action.rollAttempts}/$maximumAttempts " +
                "for ${action.target.tree.name}: " +
                "level=$level, " +
                "success=${randomSuccess || guaranteedSuccess}."
        )

        if (
            randomSuccess ||
            guaranteedSuccess
        ) {
            reward(
                context = context,
                player = player,
                state = state,
                action = action,
            )

            return
        }

        /*
         * Continue the chopping visual while waiting for another resource
         * roll.
         */
        playAnimation(
            player = player,
            axe =
                action.axe,
        )

        action.ticksUntilRoll =
            RETRY_ROLL_INTERVAL_TICKS
    }

    /**
     * Stops an active Woodcutting action and clears its animation.
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
                    id =
                        -1,

                    delay =
                        0,
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

    /**
     * Commits one successful resource harvest.
     *
     * World depletion is performed before awarding the resource so two players
     * cannot both receive the final log from the same tree.
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
            checkNotNull(
                tree.reward
            )

        /*
         * Do not globally deplete the resource if the successful player cannot
         * receive its reward.
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
         * Replace the live resource atomically.
         *
         * false means somebody else depleted this exact location first.
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
         * Capacity was checked immediately before depletion and gameplay is
         * serialized on the authoritative game-engine thread.
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

    /**
     * Reports why an axe could not be selected.
     */
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

    /**
     * Keeps the avatar facing the selected tree while chopping.
     */
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

                sizeX =
                    1,

                sizeZ =
                    1,

                instant =
                    false,

                walkMode =
                    0,
            )
    }

    /**
     * Restarts/queues the chopping sequence.
     */
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

    /**
     * Caps an unlucky Woodcutting streak according to current skill level.
     *
     * The normal success-rate calculation is still performed on every attempt;
     * this function only defines the upper bound.
     */
    private fun maximumAttempts(
        woodcuttingLevel: Int,
    ): Int =
        when {
            woodcuttingLevel >=
                90 ->
                1

            woodcuttingLevel >=
                60 ->
                2

            woodcuttingLevel >=
                30 ->
                3

            else ->
                4
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
         * Minimum visible startup period before the very first resource roll.
         *
         * 2 × 600 ms = 1.2 seconds.
         *
         * Even a guaranteed level-90+ success therefore shows a real chopping
         * action before the tree falls.
         */
        const val INITIAL_SWING_TICKS: Int =
            2

        /**
         * Delay between subsequent failed resource rolls.
         *
         * 3 × 600 ms = 1.8 seconds.
         *
         * This is slightly quicker than the previous four-tick cycle while
         * still maintaining a readable repeated chopping animation.
         */
        const val RETRY_ROLL_INTERVAL_TICKS: Int =
            3
    }
}