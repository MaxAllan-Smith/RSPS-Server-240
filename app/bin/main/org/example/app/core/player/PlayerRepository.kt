package org.example.app.core.player

/**
 * Game-thread-owned in-memory player index.
 */
class PlayerRepository {
    private val players = arrayOfNulls<Player>(PLAYER_CAPACITY)
    private val usernames = HashMap<String, Player>()

    fun nextFreeIndex(): Int? {
        for (index in MIN_PLAYER_INDEX..MAX_PLAYER_INDEX) {
            if (players[index] == null) {
                return index
            }
        }

        return null
    }

    fun add(player: Player) {
        require(player.index in MIN_PLAYER_INDEX..MAX_PLAYER_INDEX) {
            "Invalid player index: ${player.index}"
        }
        require(players[player.index] == null) {
            "Player index already occupied: ${player.index}"
        }

        val normalized = player.username.normalizedUsername()

        require(normalized !in usernames) {
            "Username already online: ${player.username}"
        }

        players[player.index] = player
        usernames[normalized] = player
    }

    fun remove(player: Player): Boolean {
        if (players[player.index] !== player) {
            return false
        }

        players[player.index] = null
        usernames.remove(player.username.normalizedUsername(), player)
        return true
    }

    fun findByUsername(username: String): Player? {
        return usernames[username.normalizedUsername()]
    }

    fun isOnline(username: String): Boolean {
        return findByUsername(username) != null
    }

    fun snapshot(): List<Player> = players.filterNotNull()

    val size: Int
        get() = usernames.size

    private companion object {
        const val PLAYER_CAPACITY = 2048
        const val MIN_PLAYER_INDEX = 1
        const val MAX_PLAYER_INDEX = PLAYER_CAPACITY - 1
    }
}
