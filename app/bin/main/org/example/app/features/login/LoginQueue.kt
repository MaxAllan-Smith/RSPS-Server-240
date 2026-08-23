package org.example.app.features.login

import org.example.app.core.network.LoginAttempt
import java.util.concurrent.ConcurrentLinkedQueue

internal class LoginQueue {
    private val requests = ConcurrentLinkedQueue<LoginAttempt>()

    fun offer(attempt: LoginAttempt) {
        requests.offer(attempt)
    }

    fun poll(): LoginAttempt? = requests.poll()
}
