package org.example.app.features.firemaking

import org.example.app.core.engine.GameContext
import org.example.app.core.experience.ExperienceService
import org.example.app.core.feature.Feature
import org.example.app.core.feature.FeatureRegistrar
import org.example.app.core.player.Player
import org.example.app.core.player.sendGameMessage
import org.example.app.core.skills.Skill
import org.example.app.features.itemuse.ItemOnItemDispatcher
import org.example.app.features.itemuse.ItemOnItemInteraction
import org.example.app.features.world.WorldLocService

/**
 * Initial Firemaking vertical slice.
 *
 * Supports:
 *
 * - normal Logs + Tinderbox in either selected-item order;
 * - authoritative inventory revalidation;
 * - one consumed log;
 * - Firemaking animation;
 * - temporary world fire;
 * - canonical 40 XP for normal logs;
 * - prevention of two temporary locs occupying the same world-loc layer.
 *
 * Timing, automatic player movement, ashes and additional log types can be
 * layered on after this basic action is verified.
 */
internal class FiremakingFeature(
    private val itemOnItem:
        ItemOnItemDispatcher,

    private val worldLocs:
        WorldLocService,

    private val experience:
        ExperienceService,
) : Feature {

    override val id: String =
        "firemaking"

    override fun install(
        registrar: FeatureRegistrar,
    ) {
        /*
         * Item order is normalized by ItemOnItemDispatcher, so both:
         *
         *     Logs -> Tinderbox
         *
         * and:
         *
         *     Tinderbox -> Logs
         *
         * reach this handler.
         */
        itemOnItem.register(
            firstItemId =
                LOGS_ITEM_ID,

            secondItemId =
                TINDERBOX_ITEM_ID,

            handler =
                ::queueNormalLogs,
        )

        /*
         * Packet handlers do not receive GameContext.
         *
         * The item-use callback therefore records the request and this world
         * cycle performs the actual world mutation with GameContext available.
         */
        registrar.onCycleStart(
            priority =
                FIREMAKING_PRIORITY,
        ) { context ->
            processCycle(
                context
            )
        }
    }

    private fun queueNormalLogs(
        interaction: ItemOnItemInteraction,
    ) {
        val logSlot =
            when (
                interaction.selectedItemId
            ) {
                LOGS_ITEM_ID ->
                    interaction.selectedSlot

                else ->
                    interaction.targetSlot
            }

        val tinderboxSlot =
            when (
                interaction.selectedItemId
            ) {
                TINDERBOX_ITEM_ID ->
                    interaction.selectedSlot

                else ->
                    interaction.targetSlot
            }

        interaction.player
            .firemakingState
            .pending =
            PendingFiremaking(
                logSlot =
                    logSlot,

                tinderboxSlot =
                    tinderboxSlot,
            )

        println(
            "[Firemaking] '${interaction.player.username}' selected " +
                "Logs + Tinderbox; " +
                "logsSlot=$logSlot, " +
                "tinderboxSlot=$tinderboxSlot."
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
                context = context,
                player = player,
            )
        }
    }

    private fun processPlayer(
        context: GameContext,
        player: Player,
    ) {
        val state =
            player.firemakingState

        val pending =
            state.pending
                ?: return

        /*
         * Consume the request exactly once.
         */
        state.clear()

        val logs =
            player.inventory[
                pending.logSlot
            ]

        val tinderbox =
            player.inventory[
                pending.tinderboxSlot
            ]

        /*
         * Revalidate both slots because the player may have rearranged the
         * inventory after producing the item-use packet.
         */
        if (
            logs?.id !=
            LOGS_ITEM_ID ||
            tinderbox?.id !=
            TINDERBOX_ITEM_ID
        ) {
            println(
                "[Firemaking] '${player.username}' cancelled: " +
                    "required inventory items changed."
            )

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

        val firePosition =
            player.position

        /*
         * A temporary loc already occupies this tile/layer.
         *
         * This prevents duplicate fires being layered on exactly the same
         * coordinate.
         */
        if (
            worldLocs.isOverridden(
                position =
                    firePosition,

                shape =
                    FIRE_LOC_SHAPE,
            )
        ) {
            player.sendGameMessage(
                "You can't light a fire here."
            )

            println(
                "[Firemaking] '${player.username}' could not light Logs at " +
                    "${firePosition.x}," +
                    "${firePosition.z}," +
                    "${firePosition.level}: " +
                    "temporary loc already occupies the tile."
            )

            return
        }

        /*
         * Create the world fire before consuming the log.
         *
         * If another world interaction managed to claim this dynamic loc key,
         * spawnTemporary returns false and the inventory remains untouched.
         */
        val spawned =
            worldLocs.spawnTemporary(
                context =
                    context,

                id =
                    FIRE_LOC_ID,

                position =
                    firePosition,

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
            player.sendGameMessage(
                "You can't light a fire here."
            )

            return
        }

        val removed =
            player.inventory.clear(
                pending.logSlot
            )

        check(
            removed?.id ==
                LOGS_ITEM_ID
        ) {
            "Firemaking inventory changed after successful fire spawn."
        }

        playAnimation(
            player
        )

        val xp =
            experience.award(
                player =
                    player,

                skill =
                    Skill.FIREMAKING,

                baseExperienceMilli =
                    NORMAL_LOGS_EXPERIENCE_MILLI,
            )

        player.sendGameMessage(
            "The fire catches and the logs begin to burn."
        )

        println(
            "[Firemaking] '${player.username}' lit Logs " +
                "at ${firePosition.x}," +
                "${firePosition.z}," +
                "${firePosition.level}; " +
                "consumed item=$LOGS_ITEM_ID, " +
                "awardedXp=${xp.awardedExperience}."
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

    private data class PendingFiremaking(
        val logSlot: Int,
        val tinderboxSlot: Int,
    )

    private class FiremakingState {

        var pending:
            PendingFiremaking? =
            null

        fun clear() {
            pending =
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
         * Canonical normal-log Firemaking experience: 40 XP.
         */
        const val NORMAL_LOGS_EXPERIENCE_MILLI: Int =
            40_000

        /**
         * Standard player-made fire.
         */
        const val FIRE_LOC_ID: Int =
            26185

        /**
         * Standard game-object layer.
         */
        const val FIRE_LOC_SHAPE: Int =
            10

        const val FIRE_LOC_ROTATION: Int =
            0

        /**
         * 100 x 600ms = approximately 60 seconds.
         *
         * Kept explicit/configurable at the feature boundary for now.
         */
        const val FIRE_LIFETIME_TICKS: Int =
            100

        const val FIREMAKING_ANIMATION_ID: Int =
            733

        const val FIREMAKING_PRIORITY: Int =
            20
    }
}