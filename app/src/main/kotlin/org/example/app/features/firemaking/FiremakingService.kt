package org.example.app.features.firemaking

import org.example.app.core.engine.GameContext
import org.example.app.core.experience.ExperienceService
import org.example.app.core.items.ItemOnItemDispatcher
import org.example.app.core.items.ItemOnItemInteraction
import org.example.app.core.items.ItemStack
import org.example.app.core.movement.MovementCoordinator
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.core.world.GroundItemService
import org.example.app.core.world.WorldLocService
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Business rules and state machine for standard line-Firemaking.
 *
 * Lifecycle:
 *
 * inventory log -> ground log + animation -> level-scaled ignition rolls ->
 * fire + XP -> immediate collision-aware one-tile step -> randomized burn
 * lifetime -> fire disappears -> ashes ground item -> ordinary ground-item
 * garbage collection.
 *
 * Split out of [FiremakingFeature] so that class stays limited to packet and
 * cycle-hook wiring, matching the Feature/Service split used by every other
 * gameplay slice in this codebase (e.g. Woodcutting/WoodcuttingActionService).
 */
internal class FiremakingService(
    private val itemOnItem: ItemOnItemDispatcher,
    private val worldLocs: WorldLocService,
    private val groundItems: GroundItemService,
    private val movement: MovementCoordinator,
    private val experience: ExperienceService,
    private val config: FiremakingConfig,
    private val random: Random = Random.Default,
) {

    /**
     * Runtime fires owned by this service.
     *
     * WorldLocService owns the actual scene loc and performs its own LocDel
     * at the same configured lifetime. This map lets Firemaking react to
     * that expiry by creating ashes.
     */
    private val activeFires = LinkedHashMap<WorldPosition, ActiveFire>()

    fun registerLogInteractions() {
        for (log in FiremakingLog.entries) {
            itemOnItem.register(TINDERBOX_ITEM_ID, log.itemId) { interaction -> start(interaction, log) }
        }
    }

    private fun start(interaction: ItemOnItemInteraction, log: FiremakingLog) {
        val player = interaction.player
        val state = player.firemakingState

        if (state.attempt != null) {
            cancelAttempt(player, state, reason = "new Firemaking interaction")
        }

        val logSlot = if (interaction.selectedItemId == log.itemId) interaction.selectedSlot else interaction.targetSlot
        val tinderboxSlot =
            if (interaction.selectedItemId == TINDERBOX_ITEM_ID) interaction.selectedSlot else interaction.targetSlot

        val serverLog = player.inventory[logSlot]
        val tinderbox = player.inventory[tinderboxSlot]
        if (serverLog?.id != log.itemId || tinderbox?.id != TINDERBOX_ITEM_ID) return

        val level = player.skills.currentLevel(Skill.FIREMAKING)
        if (level < log.requiredLevel) {
            player.sendGameMessage("You need a Firemaking level of ${log.requiredLevel} to light these logs.")
            return
        }

        val position = player.position
        if (worldLocs.isOverridden(position, FIRE_LOC_SHAPE)) {
            player.sendGameMessage("You can't light a fire here.")
            return
        }

        val removed = player.inventory.clear(logSlot) ?: return
        check(removed.id == log.itemId) {
            "Firemaking removed unexpected item=${removed.id} from slot=$logSlot."
        }

        groundItems.drop(removed, position)
        movement.clear(player)
        playAnimation(player)

        state.attempt = FiremakingAttempt(log, position, ticksUntilRoll = config.rollIntervalTicks)

        println(
            "[Firemaking] '${player.username}' started lighting ${log.displayName} at " +
                "${position.x},${position.z},${position.level}; level=$level.",
        )
    }

    fun processAttempts(context: GameContext) {
        for (player in context.players.snapshot()) {
            if (player.isDisconnected) continue
            processAttempt(context, player)
        }
    }

    private fun processAttempt(context: GameContext, player: Player) {
        val state = player.firemakingState
        val attempt = state.attempt ?: return

        // If movement from a previous cycle already moved us away, don't continue rolling.
        if (player.position != attempt.position) {
            cancelAttempt(player, state, reason = "player moved away")
            return
        }

        if (worldLocs.isOverridden(attempt.position, FIRE_LOC_SHAPE)) {
            cancelAttempt(player, state, reason = "tile became occupied")
            player.sendGameMessage("You can't light a fire here.")
            return
        }

        attempt.ticksUntilRoll--
        if (attempt.ticksUntilRoll > 0) return

        val level = player.skills.currentLevel(Skill.FIREMAKING)
        val chance = successChance(level)
        val roll = random.nextInt(SUCCESS_CHANCE_SCALE)
        val success = roll < chance

        println(
            "[Firemaking] '${player.username}' lighting roll for ${attempt.log.displayName}: " +
                "level=$level, chance=$chance/$SUCCESS_CHANCE_SCALE, roll=$roll, success=$success.",
        )

        if (!success) {
            playAnimation(player)
            attempt.ticksUntilRoll = config.rollIntervalTicks
            return
        }

        complete(context, player, state, attempt)
    }

    private fun complete(context: GameContext, player: Player, state: FiremakingState, attempt: FiremakingAttempt) {
        val groundLog = groundItems.take(attempt.log.itemId, attempt.position)
        if (groundLog == null) {
            cancelAttempt(player, state, reason = "ground log no longer exists")
            return
        }

        val lifetime = randomFireLifetime()
        val spawned = worldLocs.spawnTemporary(
            context = context,
            id = FIRE_LOC_ID,
            position = attempt.position,
            shape = FIRE_LOC_SHAPE,
            rotation = FIRE_LOC_ROTATION,
            lifetimeTicks = lifetime,
        )

        if (!spawned) {
            groundItems.drop(groundLog, attempt.position)
            cancelAttempt(player, state, reason = "fire location could not be created")
            player.sendGameMessage("You can't light a fire here.")
            return
        }

        activeFires[attempt.position] = ActiveFire(attempt.position, ticksRemaining = lifetime)

        val xp = experience.award(
            player = player,
            skill = Skill.FIREMAKING,
            baseExperienceMilli = attempt.log.experienceMilli,
        )

        stopAnimation(player)
        state.clear()
        player.sendGameMessage("The fire catches and the logs begin to burn.")

        // Installed before MovementFeature's priority-10 cycle, so the first step happens this same tick.
        stepAway(player, firePosition = attempt.position)

        println(
            "[Firemaking] '${player.username}' lit ${attempt.log.displayName} at " +
                "${attempt.position.x},${attempt.position.z},${attempt.position.level}; " +
                "fireLifetime=$lifetime ticks, awardedXp=${xp.awardedExperience}.",
        )
    }

    /**
     * Tracks Firemaking-owned loc expiry so an ordinary Ashes ground item can
     * be produced when WorldLocService removes the fire.
     *
     * World loc timers run at priority 0. This service's owning feature runs
     * at priority 5, so on the final cycle the LocDel has already been queued
     * before the ashes are staged.
     */
    fun processActiveFires() {
        if (activeFires.isEmpty()) return

        val expired = mutableListOf<WorldPosition>()
        for (fire in activeFires.values) {
            fire.ticksRemaining--
            if (fire.ticksRemaining <= 0) expired += fire.position
        }

        for (position in expired) {
            activeFires.remove(position)
            groundItems.drop(ItemStack(id = ASHES_ITEM_ID, amount = 1), position)

            println(
                "[Firemaking] Fire burned out at ${position.x},${position.z},${position.level}; " +
                    "spawned ashes item=$ASHES_ITEM_ID.",
            )
        }
    }

    fun cancelMovedAttempts(context: GameContext) {
        for (player in context.players.snapshot()) {
            if (player.isDisconnected) continue

            val state = player.firemakingState
            val attempt = state.attempt ?: continue
            if (player.position == attempt.position) continue

            cancelAttempt(player, state, reason = "player moved away")
        }
    }

    /**
     * OSRS ordinary-log ignition curve.
     *
     * Crowdsourced current-game behavior is believed to interpolate from
     * 65/256 at level 1 to guaranteed success at level 43. The log's own
     * required level is checked separately.
     */
    private fun successChance(level: Int): Int {
        if (level >= GUARANTEED_SUCCESS_LEVEL) return SUCCESS_CHANCE_SCALE

        val effective = level.coerceAtLeast(1)
        val progress = (effective - 1).toDouble() / (GUARANTEED_SUCCESS_LEVEL - 1).toDouble()

        return (LEVEL_ONE_SUCCESS_CHANCE + (SUCCESS_CHANCE_SCALE - LEVEL_ONE_SUCCESS_CHANCE) * progress)
            .roundToInt()
            .coerceIn(LEVEL_ONE_SUCCESS_CHANCE, SUCCESS_CHANCE_SCALE)
    }

    private fun randomFireLifetime(): Int {
        if (config.fireLifetimeMinTicks == config.fireLifetimeMaxTicks) return config.fireLifetimeMinTicks
        return random.nextInt(from = config.fireLifetimeMinTicks, until = config.fireLifetimeMaxTicks + 1)
    }

    /** Standard line-Firemaking step order: west -> east -> south -> north. */
    private fun stepAway(player: Player, firePosition: WorldPosition) {
        val destinations = arrayOf(
            WorldPosition(firePosition.x - 1, firePosition.z, firePosition.level),
            WorldPosition(firePosition.x + 1, firePosition.z, firePosition.level),
            WorldPosition(firePosition.x, firePosition.z - 1, firePosition.level),
            WorldPosition(firePosition.x, firePosition.z + 1, firePosition.level),
        )

        for (destination in destinations) {
            if (movement.request(player, destination.x, destination.z)) {
                println(
                    "[Firemaking] '${player.username}' stepping away from fire to " +
                        "${destination.x},${destination.z},${destination.level}.",
                )
                return
            }
        }

        println("[Firemaking] '${player.username}' could not step away from fire; all cardinal tiles were blocked.")
    }

    private fun playAnimation(player: Player) {
        player.infos.playerInfo.avatar.extendedInfo.setSequence(id = FIREMAKING_ANIMATION_ID, delay = 0)
    }

    private fun stopAnimation(player: Player) {
        player.infos.playerInfo.avatar.extendedInfo.setSequence(id = -1, delay = 0)
    }

    private fun cancelAttempt(player: Player, state: FiremakingState, reason: String) {
        if (state.attempt == null) return

        stopAnimation(player)
        state.clear()
        println("[Firemaking] '${player.username}' stopped lighting: $reason.")
    }

    private data class FiremakingAttempt(val log: FiremakingLog, val position: WorldPosition, var ticksUntilRoll: Int)

    private data class ActiveFire(val position: WorldPosition, var ticksRemaining: Int)

    private class FiremakingState {
        var attempt: FiremakingAttempt? = null
        fun clear() {
            attempt = null
        }
    }

    private val Player.firemakingState: FiremakingState
        get() = featureState.getOrPut(FiremakingState::class, ::FiremakingState)

    private companion object {
        const val TINDERBOX_ITEM_ID: Int = 590

        /** Ordinary ashes created by player-made fires. */
        const val ASHES_ITEM_ID: Int = 592

        const val FIRE_LOC_ID: Int = 26185
        const val FIRE_LOC_SHAPE: Int = 10
        const val FIRE_LOC_ROTATION: Int = 0
        const val FIREMAKING_ANIMATION_ID: Int = 733

        const val SUCCESS_CHANCE_SCALE: Int = 256
        const val LEVEL_ONE_SUCCESS_CHANCE: Int = 65
        const val GUARANTEED_SUCCESS_LEVEL: Int = 43
    }
}

/** Globally-sourced runtime timing configuration for Firemaking. */
internal data class FiremakingConfig(
    val rollIntervalTicks: Int,
    val fireLifetimeMinTicks: Int,
    val fireLifetimeMaxTicks: Int,
) {

    init {
        require(rollIntervalTicks > 0)
        require(fireLifetimeMinTicks > 0)
        require(fireLifetimeMaxTicks >= fireLifetimeMinTicks)
    }
}
