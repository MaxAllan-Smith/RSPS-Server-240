package org.example.app.features.firemaking

import org.example.app.core.engine.GameContext
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.grounditems.GroundItemService
import org.example.app.features.itemuse.ItemOnItemDispatcher
import org.example.app.features.itemuse.ItemOnItemInteraction
import org.example.app.features.movement.MovementService
import org.example.app.features.world.WorldLocService
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Firemaking vertical slice.
 *
 * Current behavior:
 *
 * - Logs + Tinderbox works in either selected-item order.
 * - One log is removed from inventory and placed on the ground.
 * - The player begins the Firemaking animation.
 * - A level-scaled success roll occurs every four game ticks.
 * - Failed rolls keep the action running.
 * - Successful rolls:
 *      - remove the ground log;
 *      - create the fire;
 *      - award Firemaking XP;
 *      - stop the animation;
 *      - move the player one tile away from the new fire.
 * - Moving away manually cancels the lighting attempt.
 */
internal class FiremakingFeature(
    private val itemOnItem:
        ItemOnItemDispatcher,

    private val worldLocs:
        WorldLocService,

    private val groundItems:
        GroundItemService,

    private val movement:
        MovementService,

    private val experience:
        ExperienceService,

    private val random:
        Random =
        Random.Default,
) : Feature {

    override val id: String =
        "firemaking"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        itemOnItem.register(
            firstItemId =
                LOGS_ITEM_ID,

            secondItemId =
                TINDERBOX_ITEM_ID,

            handler =
                ::startNormalLogs,
        )

        registrar.onCycleStart(
            priority =
                FIREMAKING_PRIORITY,
        ) { context ->
            processCycle(
                context
            )
        }
    }

    /**
     * Begins lighting normal logs.
     *
     * The inventory log is consumed immediately into a server-authoritative
     * ground item. It is not yet destroyed: a successful Firemaking roll
     * converts that ground log into a fire.
     */
    private fun startNormalLogs(
        interaction:
            ItemOnItemInteraction,
    ) {
        val player =
            interaction.player

        val state =
            player.firemakingState

        /*
         * A new attempt replaces any previous active attempt.
         */
        if (
            state.attempt !=
            null
        ) {
            cancel(
                player =
                    player,

                state =
                    state,

                reason =
                    "new Firemaking interaction",
            )
        }

        val logSlot =
            if (
                interaction.selectedItemId ==
                LOGS_ITEM_ID
            ) {
                interaction.selectedSlot
            } else {
                interaction.targetSlot
            }

        val tinderboxSlot =
            if (
                interaction.selectedItemId ==
                TINDERBOX_ITEM_ID
            ) {
                interaction.selectedSlot
            } else {
                interaction.targetSlot
            }

        val logs =
            player.inventory[
                logSlot
            ]

        val tinderbox =
            player.inventory[
                tinderboxSlot
            ]

        /*
         * Revalidate authoritative inventory state.
         */
        if (
            logs?.id !=
            LOGS_ITEM_ID ||
            tinderbox?.id !=
            TINDERBOX_ITEM_ID
        ) {
            return
        }

        val level =
            player.skills.currentLevel(
                Skill.FIREMAKING
            )

        if (
            level <
            NORMAL_LOGS_REQUIRED_LEVEL
        ) {
            player.sendGameMessage(
                "You need a Firemaking level of " +
                    "$NORMAL_LOGS_REQUIRED_LEVEL to light these logs."
            )

            return
        }

        val position =
            player.position

        /*
         * This currently covers runtime locs such as another player-made fire.
         *
         * Static map-loc occupancy can be tightened separately once dynamic
         * clipping/world-loc occupancy is unified.
         */
        if (
            worldLocs.isOverridden(
                position =
                    position,

                shape =
                    FIRE_LOC_SHAPE,
            )
        ) {
            player.sendGameMessage(
                "You can't light a fire here."
            )

            return
        }

        /*
         * Convert the inventory log into a ground log.
         *
         * The Tinderbox remains in the inventory.
         */
        val removedLogs =
            player.inventory.clear(
                logSlot
            )
                ?: return

        check(
            removedLogs.id ==
            LOGS_ITEM_ID
        ) {
            "Firemaking removed an unexpected item from slot $logSlot."
        }

        groundItems.drop(
            item =
                removedLogs,

            position =
                position,
        )

        movement.clear(
            player =
                player
        )

        playAnimation(
            player
        )

        state.attempt =
            FiremakingAttempt(
                position =
                    position,

                ticksUntilRoll =
                    LIGHTING_ROLL_INTERVAL_TICKS,
            )

        println(
            "[Firemaking] '${player.username}' started lighting Logs " +
                "at ${position.x}," +
                "${position.z}," +
                "${position.level}; " +
                "level=$level."
        )
    }

    private fun processCycle(
        context: GameContext,
    ) {
        for (
            player in
            context.players.snapshot()
        ) {
            if (
                player.isDisconnected
            ) {
                continue
            }

            processPlayer(
                context =
                    context,

                player =
                    player,
            )
        }
    }

    private fun processPlayer(
        context: GameContext,
        player: Player,
    ) {
        val state =
            player.firemakingState

        val attempt =
            state.attempt
                ?: return

        /*
         * Lighting happens on the tile where the logs were placed.
         *
         * If the player manually moves away, the attempt is interrupted and
         * the log remains as an ordinary ground item.
         */
        if (
            player.position !=
            attempt.position
        ) {
            cancel(
                player =
                    player,

                state =
                    state,

                reason =
                    "player moved away",
            )

            return
        }

        /*
         * Another temporary loc may have claimed the tile while this player
         * was still attempting to light their log.
         */
        if (
            worldLocs.isOverridden(
                position =
                    attempt.position,

                shape =
                    FIRE_LOC_SHAPE,
            )
        ) {
            cancel(
                player =
                    player,

                state =
                    state,

                reason =
                    "tile became occupied",
            )

            player.sendGameMessage(
                "You can't light a fire here."
            )

            return
        }

        attempt.ticksUntilRoll--

        if (
            attempt.ticksUntilRoll >
            0
        ) {
            return
        }

        val level =
            player.skills.currentLevel(
                Skill.FIREMAKING
            )

        val chance =
            normalLogsSuccessChance(
                level =
                    level
            )

        val roll =
            random.nextInt(
                SUCCESS_CHANCE_SCALE
            )

        val success =
            roll <
                chance

        println(
            "[Firemaking] '${player.username}' lighting roll: " +
                "level=$level, " +
                "chance=$chance/$SUCCESS_CHANCE_SCALE, " +
                "roll=$roll, " +
                "success=$success."
        )

        if (
            !success
        ) {
            /*
             * Keep the animation running and try again after another standard
             * four-tick Firemaking attempt.
             */
            playAnimation(
                player
            )

            attempt.ticksUntilRoll =
                LIGHTING_ROLL_INTERVAL_TICKS

            return
        }

        completeFire(
            context =
                context,

            player =
                player,

            state =
                state,

            attempt =
                attempt,
        )
    }

    private fun completeFire(
        context: GameContext,
        player: Player,
        state: FiremakingState,
        attempt: FiremakingAttempt,
    ) {
        /*
         * The log must still exist on the ground.
         *
         * If somebody picked it up while the player was attempting to light
         * it, no fire can be produced.
         */
        val groundLog =
            groundItems.take(
                itemId =
                    LOGS_ITEM_ID,

                position =
                    attempt.position,
            )

        if (
            groundLog == null
        ) {
            cancel(
                player =
                    player,

                state =
                    state,

                reason =
                    "ground log no longer exists",
            )

            return
        }

        val spawned =
            worldLocs.spawnTemporary(
                context =
                    context,

                id =
                    FIRE_LOC_ID,

                position =
                    attempt.position,

                shape =
                    FIRE_LOC_SHAPE,

                rotation =
                    FIRE_LOC_ROTATION,

                lifetimeTicks =
                    FIRE_LIFETIME_TICKS,
            )

        if (
            !spawned
        ) {
            /*
             * The log has already left the ground-item repository at this
             * point. Restore it as a ground item rather than silently deleting
             * the player's resource.
             */
            groundItems.drop(
                item =
                    groundLog,

                position =
                    attempt.position,
            )

            cancel(
                player =
                    player,

                state =
                    state,

                reason =
                    "fire location could not be created",
            )

            player.sendGameMessage(
                "You can't light a fire here."
            )

            return
        }

        val xp =
            experience.award(
                player =
                    player,

                skill =
                    Skill.FIREMAKING,

                baseExperienceMilli =
                    NORMAL_LOGS_EXPERIENCE_MILLI,
            )

        /*
         * Fire has now appeared. Stop the looping lighting animation before
         * moving the player away.
         */
        stopAnimation(
            player
        )

        state.clear()

        player.sendGameMessage(
            "The fire catches and the logs begin to burn."
        )

        stepAwayFromFire(
            player =
                player,

            firePosition =
                attempt.position,
        )

        println(
            "[Firemaking] '${player.username}' lit Logs " +
                "at ${attempt.position.x}," +
                "${attempt.position.z}," +
                "${attempt.position.level}; " +
                "awardedXp=${xp.awardedExperience}."
        )
    }

    /**
     * Normal-log success chance.
     *
     * The revision-current OSRS success curve begins at 65/256 at level 1 and
     * reaches guaranteed success at level 43.
     *
     * Intermediate values are linearly interpolated onto the same 0..256
     * probability scale.
     */
    private fun normalLogsSuccessChance(
        level: Int,
    ): Int {
        if (
            level >=
            NORMAL_LOGS_GUARANTEED_LEVEL
        ) {
            return SUCCESS_CHANCE_SCALE
        }

        val effectiveLevel =
            level.coerceAtLeast(
                NORMAL_LOGS_REQUIRED_LEVEL
            )

        val progress =
            (
                effectiveLevel -
                    NORMAL_LOGS_REQUIRED_LEVEL
                ).toDouble() /
                (
                    NORMAL_LOGS_GUARANTEED_LEVEL -
                        NORMAL_LOGS_REQUIRED_LEVEL
                    ).toDouble()

        return (
            NORMAL_LOGS_LEVEL_ONE_CHANCE +
                (
                    SUCCESS_CHANCE_SCALE -
                        NORMAL_LOGS_LEVEL_ONE_CHANCE
                    ) *
                progress
            )
            .roundToInt()
            .coerceIn(
                NORMAL_LOGS_LEVEL_ONE_CHANCE,
                SUCCESS_CHANCE_SCALE,
            )
    }

    /**
     * After creating a fire OSRS attempts to step:
     *
     * west -> east -> south -> north.
     *
     * MovementService performs the collision-aware route validation for each
     * adjacent tile.
     */
    private fun stepAwayFromFire(
        player: Player,
        firePosition: WorldPosition,
    ) {
        val destinations =
            arrayOf(
                WorldPosition(
                    x =
                        firePosition.x -
                            1,

                    z =
                        firePosition.z,

                    level =
                        firePosition.level,
                ),

                WorldPosition(
                    x =
                        firePosition.x +
                            1,

                    z =
                        firePosition.z,

                    level =
                        firePosition.level,
                ),

                WorldPosition(
                    x =
                        firePosition.x,

                    z =
                        firePosition.z -
                            1,

                    level =
                        firePosition.level,
                ),

                WorldPosition(
                    x =
                        firePosition.x,

                    z =
                        firePosition.z +
                            1,

                    level =
                        firePosition.level,
                ),
            )

        for (
            destination in
            destinations
        ) {
            if (
                movement.request(
                    player =
                        player,

                    x =
                        destination.x,

                    z =
                        destination.z,
                )
            ) {
                println(
                    "[Firemaking] '${player.username}' stepping away " +
                        "from fire to ${destination.x}," +
                        "${destination.z}," +
                        "${destination.level}."
                )

                return
            }
        }

        println(
            "[Firemaking] '${player.username}' could not step away from fire; " +
                "all cardinal tiles were blocked."
        )
    }

    private fun playAnimation(
        player: Player,
    ) {
        player.infos
            .playerInfo
            .avatar
            .extendedInfo
            .setSequence(
                id =
                    FIREMAKING_ANIMATION_ID,

                delay =
                    0,
            )
    }

    private fun stopAnimation(
        player: Player,
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
    }

    private fun cancel(
        player: Player,
        state: FiremakingState,
        reason: String,
    ) {
        if (
            state.attempt ==
            null
        ) {
            return
        }

        stopAnimation(
            player
        )

        state.clear()

        println(
            "[Firemaking] '${player.username}' stopped lighting: $reason."
        )
    }

    private data class FiremakingAttempt(
        val position: WorldPosition,
        var ticksUntilRoll: Int,
    )

    private class FiremakingState {

        var attempt:
            FiremakingAttempt? =
            null

        fun clear() {
            attempt =
                null
        }
    }

    private val Player.firemakingState:
        FiremakingState
        get() =
            featureState.getOrPut(
                FiremakingState::class,
                ::FiremakingState,
            )

    private companion object {

        const val TINDERBOX_ITEM_ID: Int =
            590

        const val LOGS_ITEM_ID: Int =
            1511

        const val NORMAL_LOGS_REQUIRED_LEVEL: Int =
            1

        /**
         * Normal logs are guaranteed to ignite from level 43 onward.
         */
        const val NORMAL_LOGS_GUARANTEED_LEVEL: Int =
            43

        /**
         * Level-one normal-log chance = 65 / 256.
         */
        const val NORMAL_LOGS_LEVEL_ONE_CHANCE: Int =
            65

        const val SUCCESS_CHANCE_SCALE: Int =
            256

        /**
         * Four 600ms game cycles = 2.4 seconds between lighting rolls.
         */
        const val LIGHTING_ROLL_INTERVAL_TICKS: Int =
            4

        const val NORMAL_LOGS_EXPERIENCE_MILLI: Int =
            40_000

        const val FIRE_LOC_ID: Int =
            26185

        const val FIRE_LOC_SHAPE: Int =
            10

        const val FIRE_LOC_ROTATION: Int =
            0

        const val FIRE_LIFETIME_TICKS: Int =
            100

        const val FIREMAKING_ANIMATION_ID: Int =
            733

        const val FIREMAKING_PRIORITY: Int =
            20
    }
}