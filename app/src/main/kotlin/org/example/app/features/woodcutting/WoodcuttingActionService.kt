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
 * Resource acquisition is genuinely probability based:
 *
 * - the axe animation begins immediately;
 * - the first roll waits long enough for the animation to be visible;
 * - every resource roll is independent;
 * - Woodcutting level influences success probability;
 * - failed attempts wait a short randomized number of game ticks;
 * - there is no forced-success attempt.
 *
 * Tree selection and routing remain responsibilities of WoodcuttingFeature.
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
     * Begins chopping once the player reaches the selected interaction tile.
     */
    fun start(
        player: Player,
        state: WoodcuttingState,
        target: WoodcuttingTarget,
    ) {
        /*
         * Another player may have depleted the resource while this player was
         * travelling toward it.
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
         * Oak and Willow remain disabled until fractional XP is handled by the
         * global ExperienceService.
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
         * Face and animate immediately when interaction range is reached.
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
                 * Prevent the tree from disappearing in the same visual moment
                 * that the first chopping animation begins.
                 *
                 * 2 game ticks = 1.2 seconds.
                 */
                ticksUntilRoll =
                    INITIAL_SWING_TICKS,
            )

        println(
            "[Woodcutting] '${player.username}' started chopping " +
                "${target.tree.name} using " +
                "${selection.axe.name}; " +
                "level=$woodcuttingLevel."
        )
    }

    /**
     * Advances one active Woodcutting action by one 600ms game cycle.
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
         * Stop immediately when another player depletes this tree.
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
         * Movement away from the interaction tile interrupts chopping.
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
         * Re-evaluate the player's best usable axe while the action runs.
         *
         * This means moving an axe between inventory/equipment remains valid
         * and acquiring a better supported axe can take effect naturally.
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

        performResourceRoll(
            context = context,
            player = player,
            state = state,
            action = action,
        )
    }

    /**
     * Performs one independent random resource-success roll.
     *
     * There is no guaranteed roll count.
     *
     * Higher Woodcutting levels receive a higher probability through
     * WoodcuttingSuccessRate, so progression improves the average chopping
     * speed without making the outcome deterministic.
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

        val success =
            successRate.succeeds(
                level = level,
                random = random,
            )

        println(
            "[Woodcutting] '${player.username}' resource roll " +
                "for ${action.target.tree.name}: " +
                "level=$level, " +
                "success=$success."
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

        /*
         * Continue swinging after a failed roll.
         */
        playAnimation(
            player = player,
            axe =
                action.axe,
        )

        /*
         * Randomize the next check rather than running on an exact metronome.
         *
         * 2..4 server ticks means the next roll occurs after approximately:
         *
         * 1.2s, 1.8s or 2.4s.
         *
         * The resource outcome remains separately randomized by the success
         * chance calculation.
         */
        action.ticksUntilRoll =
            randomRetryTicks()
    }

    /**
     * Stops an active chopping action.
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
     * Awards the successful resource and atomically depletes the shared tree.
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
         * Never globally deplete a resource when the successful player cannot
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
         * Deplete before awarding the item.
         *
         * This prevents two players from receiving the same resource if they
         * succeed against the same tree during the same logical period.
         */
        val depleted =
            worldLocs.replace(
                context =
                    context,

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
     * Explains why an available axe cannot currently be used.
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
     * Keeps the player facing the tree while the action begins.
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
     * Starts or restarts the axe chopping sequence.
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
     * Random delay before the next resource roll after a failed attempt.
     */
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

        /**
         * Minimum startup time before the first random success roll.
         *
         * 2 × 600ms = 1.2 seconds.
         *
         * This prevents an apparent instant-cut immediately after reaching the
         * tree.
         */
        const val INITIAL_SWING_TICKS: Int =
            2

        /**
         * Random retry interval after a failed resource roll.
         *
         * 2..4 ticks = approximately 1.2..2.4 seconds.
         */
        const val MIN_RETRY_TICKS: Int =
            2

        const val MAX_RETRY_TICKS: Int =
            4
    }
}