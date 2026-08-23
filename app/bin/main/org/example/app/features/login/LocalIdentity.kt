package org.example.app.features.login

import org.example.app.core.player.normalizedUsername
import java.nio.ByteBuffer
import java.security.MessageDigest

/**
 * Temporary deterministic identity for local development accounts.
 * Replace this implementation inside the login/account slice when persistence
 * is added; no core changes are required.
 */
internal data class LocalIdentity(
    val accountHash: Long,
    val userId: Long,
    val userHash: Long,
) {
    companion object {
        fun forUsername(username: String): LocalIdentity {
            val normalized = username.normalizedUsername()

            return LocalIdentity(
                accountHash = stableLong("account:$normalized"),
                userId = stableLong("userid:$normalized"),
                userHash = stableLong("userhash:$normalized"),
            )
        }

        private fun stableLong(value: String): Long {
            val digest =
                MessageDigest.getInstance("SHA-256")
                    .digest(value.toByteArray(Charsets.UTF_8))

            val result =
                ByteBuffer.wrap(digest).long and Long.MAX_VALUE

            return if (result == 0L) 1L else result
        }
    }
}
