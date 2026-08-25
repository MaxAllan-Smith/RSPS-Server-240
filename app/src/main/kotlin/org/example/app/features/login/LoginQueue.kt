package org.example.app.features.login

import org.example.app.core.network.LoginAttempt
import java.util.concurrent.ConcurrentLinkedQueue

/** Thread-safe hand-off queue for login attempts arriving on Netty threads before the game thread processes them. */
internal class LoginQueue {
    private val requests = ConcurrentLinkedQueue<LoginAttempt>()

    fun offer(attempt: LoginAttempt) {
        requests.offer(attempt)
    }

    fun poll(): LoginAttempt? = requests.poll()
}
