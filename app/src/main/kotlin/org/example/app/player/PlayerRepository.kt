package org.example.app.player

import java.util.Locale

class PlayerRepository {

    /*
     * Index 0 intentionally exists but is never allocated.
     *
     * Valid player indices:
     *
     *     1 .. 2047
     */
    private val players =
        arrayOfNulls<Player>(
            PLAYER_CAPACITY
        )

    private val usernames =
        HashMap<String, Player>()

    fun nextFreeIndex(): Int? {
        for (
            index in
            MIN_PLAYER_INDEX..MAX_PLAYER_INDEX
        ) {
            if (players[index] == null) {
                return index
            }
        }

        return null
    }

    fun add(
        player: Player,
    ) {
        require(
            player.index in
                MIN_PLAYER_INDEX..MAX_PLAYER_INDEX
        ) {
            "Invalid player index: ${player.index}"
        }

        require(
            players[player.index] == null
        ) {
            "Player index already occupied: ${player.index}"
        }

        val normalized =
            normalize(
                player.username
            )

        require(
            normalized !in usernames
        ) {
            "Username already online: ${player.username}"
        }

        players[player.index] =
            player

        usernames[normalized] =
            player
    }

    fun remove(
        player: Player,
    ): Boolean {
        if (
            players[player.index] !==
            player
        ) {
            return false
        }

        players[player.index] =
            null

        usernames.remove(
            normalize(
                player.username
            ),
            player,
        )

        return true
    }

    fun findByUsername(
        username: String,
    ): Player? {
        return usernames[
            normalize(username)
        ]
    }

    fun isOnline(
        username: String,
    ): Boolean {
        return findByUsername(
            username
        ) != null
    }

    fun snapshot(): List<Player> {
        return players
            .filterNotNull()
    }

    val size: Int
        get() = usernames.size

    private fun normalize(
        username: String,
    ): String {
        return username
            .trim()
            .lowercase(
                Locale.ROOT
            )
    }

    private companion object {
        const val PLAYER_CAPACITY =
            2048

        const val MIN_PLAYER_INDEX =
            1

        const val MAX_PLAYER_INDEX =
            PLAYER_CAPACITY - 1
    }
}