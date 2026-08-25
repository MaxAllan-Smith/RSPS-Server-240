package org.example.app.features.firemaking

import org.example.app.core.engine.GameContext
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.items.ItemStack
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
 * Standard line-Firemaking gameplay.
 *
 * Lifecycle:
 *
 * inventory log
 *      ->
 * ground log + animation
 *      ->
 * level-scaled ignition rolls
 *      ->
 * fire + XP
 *      ->
 * immediate collision-aware one-tile step
 *      ->
 * randomized burn lifetime
 *      ->
 * fire disappears
 *      ->
 * ashes ground item
 *      ->
 * ordinary ground-item garbage collection
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

    private val config:
        FiremakingConfig,

    private val random:
        Random =
        Random.Default,
) : Feature {

    override val id: String =
        "firemaking"

    /**
     * Runtime fires owned by this feature.
     *
     * WorldLocService owns the actual scene loc and performs its LocDel at the
     * same configured lifetime. This map lets Firemaking react to that expiry
     * by creating ashes.
     */
    private val activeFires =
        LinkedHashMap<
            WorldPosition,
            ActiveFire
            >()

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        registerLogInteractions()

        /*
         * Runs before normal movement.
         *
         * When ignition succeeds, the one-tile step route is installed here.
         * MovementFeature runs at priority 10, so the route advances in this
         * SAME game cycle instead of waiting another 600 ms.
         */
        registrar.onCycleStart(
            priority =
                BEFORE_MOVEMENT_PRIORITY,
        ) { context ->
            processActiveFires()

            processAttempts(
                context
            )
        }

        /*
         * Runs after movement.
         *
         * If the player manually walked away while an ignition attempt was in
         * progress, cancel the attempt and leave their log on the ground.
         */
        registrar.onCycleStart(
            priority =
                AFTER_MOVEMENT_PRIORITY,
        ) { context ->
            cancelMovedAttempts(
                context
            )
        }
    }

    private fun registerLogInteractions() {
        for (
            log in
            FiremakingLog.entries
        ) {
            itemOnItem.register(
                firstItemId =
                    TINDERBOX_ITEM_ID,

                secondItemId =
                    log.itemId,
            ) { interaction ->
                start(
                    interaction =
                        interaction,

                    log =
                        log,
                )
            }
        }
    }

    private fun start(
        interaction:
            ItemOnItemInteraction,

        log:
            FiremakingLog,
    ) {
        val player =
            interaction.player

        val state =
            player.firemakingState

        if (
            state.attempt !=
            null
        ) {
            cancelAttempt(
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
                log.itemId
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

        val serverLog =
            player.inventory[
                logSlot
            ]

        val tinderbox =
            player.inventory[
                tinderboxSlot
            ]

        if (
            serverLog?.id !=
            log.itemId ||
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
            log.requiredLevel
        ) {
            player.sendGameMessage(
                "You need a Firemaking level of " +
                    "${log.requiredLevel} to light these logs."
            )

            return
        }

        val position =
            player.position

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

        val removed =
            player.inventory.clear(
                logSlot
            )
                ?: return

        check(
            removed.id ==
                log.itemId
        ) {
            "Firemaking removed unexpected item=${removed.id} " +
                "from slot=$logSlot."
        }

        groundItems.drop(
            item =
                removed,

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
                log =
                    log,

                position =
                    position,

                ticksUntilRoll =
                    config.rollIntervalTicks,
            )

        println(
            "[Firemaking] '${player.username}' started lighting " +
                "${log.displayName} at " +
                "${position.x}," +
                "${position.z}," +
                "${position.level}; " +
                "level=$level."
        )
    }

    private fun processAttempts(
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

            processAttempt(
                context =
                    context,

                player =
                    player,
            )
        }
    }

    private fun processAttempt(
        context: GameContext,
        player: Player,
    ) {
        val state =
            player.firemakingState

        val attempt =
            state.attempt
                ?: return

        /*
         * If movement from a previous cycle already moved us away, don't
         * continue rolling.
         */
        if (
            player.position !=
            attempt.position
        ) {
            cancelAttempt(
                player =
                    player,

                state =
                    state,

                reason =
                    "player moved away",
            )

            return
        }

        if (
            worldLocs.isOverridden(
                position =
                    attempt.position,

                shape =
                    FIRE_LOC_SHAPE,
            )
        ) {
            cancelAttempt(
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
            successChance(
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
            "[Firemaking] '${player.username}' lighting roll for " +
                "${attempt.log.displayName}: " +
                "level=$level, " +
                "chance=$chance/$SUCCESS_CHANCE_SCALE, " +
                "roll=$roll, " +
                "success=$success."
        )

        if (
            !success
        ) {
            playAnimation(
                player
            )

            attempt.ticksUntilRoll =
                config.rollIntervalTicks

            return
        }

        complete(
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

    private fun complete(
        context: GameContext,
        player: Player,
        state: FiremakingState,
        attempt: FiremakingAttempt,
    ) {
        val groundLog =
            groundItems.take(
                itemId =
                    attempt.log.itemId,

                position =
                    attempt.position,
            )

        if (
            groundLog ==
            null
        ) {
            cancelAttempt(
                player =
                    player,

                state =
                    state,

                reason =
                    "ground log no longer exists",
            )

            return
        }

        val lifetime =
            randomFireLifetime()

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
                    lifetime,
            )

        if (
            !spawned
        ) {
            groundItems.drop(
                item =
                    groundLog,

                position =
                    attempt.position,
            )

            cancelAttempt(
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

        activeFires[
            attempt.position
        ] =
            ActiveFire(
                position =
                    attempt.position,

                ticksRemaining =
                    lifetime,
            )

        val xp =
            experience.award(
                player =
                    player,

                skill =
                    Skill.FIREMAKING,

                baseExperienceMilli =
                    attempt.log
                        .experienceMilli,
            )

        stopAnimation(
            player
        )

        state.clear()

        player.sendGameMessage(
            "The fire catches and the logs begin to burn."
        )

        /*
         * This route is installed before MovementFeature's priority-10 cycle,
         * allowing the first step to happen in the same game tick.
         */
        stepAway(
            player =
                player,

            firePosition =
                attempt.position,
        )

        println(
            "[Firemaking] '${player.username}' lit " +
                "${attempt.log.displayName} at " +
                "${attempt.position.x}," +
                "${attempt.position.z}," +
                "${attempt.position.level}; " +
                "fireLifetime=$lifetime ticks, " +
                "awardedXp=${xp.awardedExperience}."
        )
    }

    /**
     * Tracks Firemaking-owned loc expiry so an ordinary Ashes ground item can
     * be produced when WorldLocService removes the fire.
     *
     * World loc timers run at priority 0. This feature runs at priority 5, so
     * on the final cycle the LocDel has already been queued before the ashes
     * are staged.
     */
    private fun processActiveFires() {
        if (
            activeFires.isEmpty()
        ) {
            return
        }

        val expired =
            ArrayList<
                WorldPosition
                >()

        for (
            fire in
            activeFires.values
        ) {
            fire.ticksRemaining--

            if (
                fire.ticksRemaining <=
                0
            ) {
                expired +=
                    fire.position
            }
        }

        for (
            position in
            expired
        ) {
            activeFires.remove(
                position
            )

            groundItems.drop(
                item =
                    ItemStack(
                        id =
                            ASHES_ITEM_ID,

                        amount =
                            1,
                    ),

                position =
                    position,
            )

            println(
                "[Firemaking] Fire burned out at " +
                    "${position.x}," +
                    "${position.z}," +
                    "${position.level}; " +
                    "spawned ashes item=$ASHES_ITEM_ID."
            )
        }
    }

    private fun cancelMovedAttempts(
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

            val state =
                player.firemakingState

            val attempt =
                state.attempt
                    ?: continue

            if (
                player.position ==
                attempt.position
            ) {
                continue
            }

            cancelAttempt(
                player =
                    player,

                state =
                    state,

                reason =
                    "player moved away",
            )
        }
    }

    /**
     * OSRS ordinary-log ignition curve.
     *
     * Crowdsourced current-game behavior is believed to interpolate from
     * 65/256 at level 1 to guaranteed success at level 43.
     *
     * The log's own required level is checked separately.
     */
    private fun successChance(
        level: Int,
    ): Int {
        if (
            level >=
            GUARANTEED_SUCCESS_LEVEL
        ) {
            return SUCCESS_CHANCE_SCALE
        }

        val effective =
            level.coerceAtLeast(
                1
            )

        val progress =
            (
                effective -
                    1
                ).toDouble() /
                (
                    GUARANTEED_SUCCESS_LEVEL -
                        1
                    ).toDouble()

        return (
            LEVEL_ONE_SUCCESS_CHANCE +
                (
                    SUCCESS_CHANCE_SCALE -
                        LEVEL_ONE_SUCCESS_CHANCE
                    ) *
                progress
            )
            .roundToInt()
            .coerceIn(
                LEVEL_ONE_SUCCESS_CHANCE,
                SUCCESS_CHANCE_SCALE,
            )
    }

    private fun randomFireLifetime():
        Int {

        if (
            config.fireLifetimeMinTicks ==
            config.fireLifetimeMaxTicks
        ) {
            return config.fireLifetimeMinTicks
        }

        return random.nextInt(
            from =
                config.fireLifetimeMinTicks,

            until =
                config.fireLifetimeMaxTicks +
                    1,
        )
    }

    /**
     * Standard line-Firemaking step order:
     *
     * west -> east -> south -> north.
     */
    private fun stepAway(
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
                        "from fire to " +
                        "${destination.x}," +
                        "${destination.z}," +
                        "${destination.level}."
                )

                return
            }
        }

        println(
            "[Firemaking] '${player.username}' could not step away from " +
                "fire; all cardinal tiles were blocked."
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

    private fun cancelAttempt(
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
        val log: FiremakingLog,
        val position: WorldPosition,
        var ticksUntilRoll: Int,
    )

    private data class ActiveFire(
        val position: WorldPosition,
        var ticksRemaining: Int,
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

        /**
         * Ordinary ashes created by player-made fires.
         */
        const val ASHES_ITEM_ID: Int =
            592

        const val FIRE_LOC_ID: Int =
            26185

        const val FIRE_LOC_SHAPE: Int =
            10

        const val FIRE_LOC_ROTATION: Int =
            0

        const val FIREMAKING_ANIMATION_ID: Int =
            733

        const val SUCCESS_CHANCE_SCALE: Int =
            256

        const val LEVEL_ONE_SUCCESS_CHANCE: Int =
            65

        const val GUARANTEED_SUCCESS_LEVEL: Int =
            43

        /**
         * Firemaking completes before MovementFeature (priority 10), allowing
         * the automatic step-away route to advance immediately.
         */
        const val BEFORE_MOVEMENT_PRIORITY: Int =
            5

        /**
         * Manual movement interruption is checked after MovementFeature.
         */
        const val AFTER_MOVEMENT_PRIORITY: Int =
            20
    }
}

/**
 * Globally-sourced runtime timing configuration for Firemaking.
 */
internal data class FiremakingConfig(
    val rollIntervalTicks: Int,
    val fireLifetimeMinTicks: Int,
    val fireLifetimeMaxTicks: Int,
) {

    init {
        require(
            rollIntervalTicks >
                0
        )

        require(
            fireLifetimeMinTicks >
                0
        )

        require(
            fireLifetimeMaxTicks >=
                fireLifetimeMinTicks
        )
    }
}