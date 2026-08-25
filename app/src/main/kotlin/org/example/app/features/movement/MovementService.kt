package org.example.app.features.movement

import net.rsprot.protocol.game.outgoing.misc.player.UpdateRunEnergy
import org.example.app.core.player.Player
import org.example.app.core.player.WorldPosition
import org.example.app.features.movement.state.MovementState
import org.example.app.features.movement.state.movementState

/**
 * Shared authoritative walking/running service.
 */
class MovementService internal constructor(
    private val planner: RoutePlanner,

    private val config:
        MovementConfig,
) {

    fun request(
        player: Player,
        x: Int,
        z: Int,
        keyCombination: Int = 0,
    ): Boolean {
        val destination =
            WorldPosition(
                x =
                    x,

                z =
                    z,

                level =
                    player.position.level,
            )

        val route =
            planner.route(
                start =
                    player.position,

                destination =
                    destination,
            )

        val state =
            player.movementState

        state.steps.clear()

        state.steps.addAll(
            route
        )

        state.requestedKeyCombination =
            keyCombination

        if (
            route.isEmpty() &&
            destination !=
            player.position
        ) {
            println(
                "[Movement] '${player.username}' could not route " +
                    "to ${destination.x}," +
                    "${destination.z}," +
                    "${destination.level}."
            )

            return false
        }

        return true
    }

    fun requestNear(
        player: Player,
        x: Int,
        z: Int,
        maximumRadius: Int,
        keyCombination: Int = 0,
    ): WorldPosition? {
        val target =
            WorldPosition(
                x =
                    x,

                z =
                    z,

                level =
                    player.position.level,
            )

        val route =
            planner.routeNear(
                start =
                    player.position,

                target =
                    target,

                maximumRadius =
                    maximumRadius,
            )
                ?: run {
                    clear(
                        player =
                            player,
                    )

                    println(
                        "[Movement] '${player.username}' could not route " +
                            "near ${target.x}," +
                            "${target.z}," +
                            "${target.level}."
                    )

                    return null
                }

        val state =
            player.movementState

        state.steps.clear()

        state.steps.addAll(
            route.steps
        )

        state.requestedKeyCombination =
            keyCombination

        return route.destination
    }

    /**
     * Initializes movement-related client state after login.
     */
    fun initializeClientState(
        player: Player,
    ) {
        val state =
            player.movementState

        if (
            state.clientStateInitialized
        ) {
            synchronizeRunEnergy(
                player
            )

            return
        }

        state.clientStateInitialized =
            true

        player.infos
            .playerInfo
            .avatar
            .extendedInfo
            .setMoveSpeed(
                if (
                    state.runEnabled
                ) {
                    RUN_MOVE_SPEED
                } else {
                    WALK_MOVE_SPEED
                }
            )

        synchronizeRunEnergy(
            player
        )

        println(
            "[Movement] Initialized '${player.username}' " +
                "runEnergy=${state.runEnergy}."
        )
    }

    /**
     * Advances one game-cycle worth of movement and updates run energy.
     */
    fun cycle(
        player: Player,
    ) {
        val state =
            player.movementState

        val firstStep =
            state.steps
                .removeFirstOrNull()

        /*
         * No movement this cycle.
         *
         * Run energy regenerates even if the run orb itself remains enabled;
         * what matters is whether the player actually ran.
         */
        if (
            firstStep ==
            null
        ) {
            restoreRunEnergy(
                player
            )

            return
        }

        val wantsToRun =
            shouldRun(
                runEnabled =
                    state.runEnabled,

                keyCombination =
                    state.requestedKeyCombination,
            )

        /*
         * Running requires energy and also requires two route tiles.
         *
         * A final one-tile route step is treated as walking rather than
         * consuming energy for no actual two-tile run movement.
         */
        val canRun =
            wantsToRun &&
                state.runEnergy >
                MovementState.MIN_RUN_ENERGY &&
                state.steps.isNotEmpty()

        val controlHeld =
            state.requestedKeyCombination ==
                CONTROL_KEY_COMBINATION

        if (
            controlHeld
        ) {
            player.infos
                .playerInfo
                .avatar
                .extendedInfo
                .setTempMoveSpeed(
                    if (
                        canRun
                    ) {
                        RUN_MOVE_SPEED
                    } else {
                        WALK_MOVE_SPEED
                    }
                )
        } else if (
            wantsToRun &&
            !canRun
        ) {
            /*
             * The persistent avatar mode may still be Run because the orb is
             * enabled. Override this single cycle to walking when there is
             * insufficient energy or only one route step remains.
             */
            player.infos
                .playerInfo
                .avatar
                .extendedInfo
                .setTempMoveSpeed(
                    WALK_MOVE_SPEED
                )
        }

        player.position =
            firstStep

        if (
            !canRun
        ) {
            restoreRunEnergy(
                player
            )

            if (
                wantsToRun &&
                state.runEnergy <=
                MovementState.MIN_RUN_ENERGY
            ) {
                disableRunningBecauseEnergyDepleted(
                    player
                )
            }

            return
        }

        val secondStep =
            state.steps
                .removeFirstOrNull()

        if (
            secondStep ==
            null
        ) {
            restoreRunEnergy(
                player
            )

            return
        }

        player.position =
            secondStep

        drainRunEnergy(
            player
        )
    }

    fun setRunning(
        player: Player,
        enabled: Boolean,
    ) {
        val state =
            player.movementState

        val actualEnabled =
            enabled &&
                state.runEnergy >
                MovementState.MIN_RUN_ENERGY

        if (
            state.runEnabled ==
            actualEnabled
        ) {
            return
        }

        state.runEnabled =
            actualEnabled

        synchronizeRunToggle(
            player
        )

        println(
            "[Movement] '${player.username}' run " +
                if (
                    actualEnabled
                ) {
                    "enabled."
                } else {
                    "disabled."
                }
        )
    }

    fun toggleRunning(
        player: Player,
    ) {
        setRunning(
            player =
                player,

            enabled =
                !player.movementState
                    .runEnabled,
        )
    }

    private fun drainRunEnergy(
        player: Player,
    ) {
        val state =
            player.movementState

        val previous =
            state.runEnergy

        state.runEnergy =
            (
                state.runEnergy -
                    config.runEnergyDrainPerRunningCycle
                )
                .coerceAtLeast(
                    MovementState.MIN_RUN_ENERGY
                )

        if (
            state.runEnergy !=
            previous
        ) {
            synchronizeRunEnergy(
                player
            )
        }

        if (
            state.runEnergy ==
            MovementState.MIN_RUN_ENERGY
        ) {
            disableRunningBecauseEnergyDepleted(
                player
            )
        }
    }

    private fun restoreRunEnergy(
        player: Player,
    ) {
        val state =
            player.movementState

        if (
            state.runEnergy >=
            MovementState.MAX_RUN_ENERGY
        ) {
            return
        }

        val previous =
            state.runEnergy

        state.runEnergy =
            (
                state.runEnergy +
                    config.runEnergyRestorePerIdleCycle
                )
                .coerceAtMost(
                    MovementState.MAX_RUN_ENERGY
                )

        if (
            state.runEnergy !=
            previous
        ) {
            synchronizeRunEnergy(
                player
            )
        }
    }

    private fun disableRunningBecauseEnergyDepleted(
        player: Player,
    ) {
        val state =
            player.movementState

        if (
            !state.runEnabled
        ) {
            return
        }

        state.runEnabled =
            false

        synchronizeRunToggle(
            player
        )

        println(
            "[Movement] '${player.username}' exhausted run energy; " +
                "falling back to walking."
        )
    }

    /**
     * Updates both the orb toggle varp and RSProt's persistent player movement
     * speed.
     */
    private fun synchronizeRunToggle(
        player: Player,
    ) {
        val enabled =
            player.movementState
                .runEnabled

        player.vars.setVarp(
            id =
                RUN_OPTION_VARP,

            value =
                if (
                    enabled
                ) {
                    1
                } else {
                    0
                },
        )

        player.infos
            .playerInfo
            .avatar
            .extendedInfo
            .setMoveSpeed(
                if (
                    enabled
                ) {
                    RUN_MOVE_SPEED
                } else {
                    WALK_MOVE_SPEED
                }
            )
    }

    /**
     * Sends run energy only when its authoritative value changed.
     */
    private fun synchronizeRunEnergy(
        player: Player,
    ) {
        val state =
            player.movementState

        if (
            state.synchronizedRunEnergy ==
            state.runEnergy
        ) {
            return
        }

        player.session.queue(
            UpdateRunEnergy(
                state.runEnergy
            )
        )

        state.synchronizedRunEnergy =
            state.runEnergy
    }

    private fun shouldRun(
        runEnabled: Boolean,
        keyCombination: Int,
    ): Boolean {
        val controlHeld =
            keyCombination ==
                CONTROL_KEY_COMBINATION

        return if (
            controlHeld
        ) {
            !runEnabled
        } else {
            runEnabled
        }
    }

    fun clear(
        player: Player,
    ) {
        val state =
            player.movementState

        state.steps.clear()

        state.requestedKeyCombination =
            0
    }

    private companion object {

        const val RUN_OPTION_VARP: Int =
            173

        const val WALK_MOVE_SPEED: Int =
            1

        const val RUN_MOVE_SPEED: Int =
            2

        const val CONTROL_KEY_COMBINATION: Int =
            1
    }
}

/**
 * Globally-configured movement tuning.
 */
internal data class MovementConfig(
    val runEnergyDrainPerRunningCycle: Int,
    val runEnergyRestorePerIdleCycle: Int,
) {

    init {
        require(
            runEnergyDrainPerRunningCycle >
                0
        )

        require(
            runEnergyRestorePerIdleCycle >=
                0
        )
    }
}