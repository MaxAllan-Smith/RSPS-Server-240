package org.example.app.core.feature

import net.rsprot.protocol.game.incoming.misc.user.ClientCheat
import net.rsprot.protocol.message.codec.incoming.GameMessageConsumerRepository
import net.rsprot.protocol.message.codec.incoming.GameMessageConsumerRepositoryBuilder
import org.example.app.core.engine.GameContext
import org.example.app.core.network.LoginAttempt
import org.example.app.core.network.ReconnectAttempt
import org.example.app.core.player.Player

typealias FeatureCommandHandler =
    (Player, String, List<String>) -> Boolean

class FeatureRegistry {
    private val installedFeatureIds =
        LinkedHashSet<String>()

    private var loginHandler: ((LoginAttempt) -> Unit)? =
        null

    private var reconnectHandler: ((ReconnectAttempt) -> Unit)? =
        null

    private val packetConfigurations =
        mutableListOf<
            GameMessageConsumerRepositoryBuilder<Player>.() -> Unit
        >()

    private val commandHandlers =
        mutableListOf<FeatureCommandHandler>()

    private val cycleStartHandlers =
        mutableListOf<
            OrderedHandler<(GameContext) -> Unit>
        >()

    private val beforeInfoHandlers =
        mutableListOf<
            OrderedHandler<(GameContext, Player) -> Unit>
        >()

    private val afterInfoHandlers =
        mutableListOf<
            OrderedHandler<(GameContext, Player) -> Unit>
        >()

    private var registrationSequence =
        0

    fun install(
        features: Iterable<Feature>,
    ): FeatureRuntime {
        for (feature in features) {
            install(feature)
        }

        return FeatureRuntime(
            featureIds = installedFeatureIds.toList(),
            loginHandler = loginHandler,
            reconnectHandler = reconnectHandler,
            gameMessages = buildGameMessages(),
            commandHandlers = commandHandlers.toList(),
            cycleStartHandlers = cycleStartHandlers.sorted(),
            beforeInfoHandlers = beforeInfoHandlers.sorted(),
            afterInfoHandlers = afterInfoHandlers.sorted(),
        )
    }

    private fun install(
        feature: Feature,
    ) {
        require(feature.id.isNotBlank()) {
            "Feature id cannot be blank."
        }

        check(installedFeatureIds.add(feature.id)) {
            "Feature '${feature.id}' is already installed."
        }

        feature.install(
            FeatureRegistrar(
                featureId = feature.id,
                registry = this,
            )
        )
    }

    internal fun registerLoginHandler(
        featureId: String,
        handler: (LoginAttempt) -> Unit,
    ) {
        check(loginHandler == null) {
            "Feature '$featureId' attempted to replace " +
                "the registered login handler."
        }

        loginHandler = handler
    }

    internal fun registerReconnectHandler(
        featureId: String,
        handler: (ReconnectAttempt) -> Unit,
    ) {
        check(reconnectHandler == null) {
            "Feature '$featureId' attempted to replace " +
                "the registered reconnect handler."
        }

        reconnectHandler = handler
    }

    internal fun registerPackets(
        configuration:
            GameMessageConsumerRepositoryBuilder<Player>.() -> Unit,
    ) {
        packetConfigurations += configuration
    }

    internal fun registerCommand(
        handler: FeatureCommandHandler,
    ) {
        commandHandlers += handler
    }

    internal fun registerCycleStart(
        featureId: String,
        priority: Int,
        handler: (GameContext) -> Unit,
    ) {
        cycleStartHandlers +=
            ordered(
                featureId = featureId,
                priority = priority,
                handler = handler,
            )
    }

    internal fun registerBeforeInfo(
        featureId: String,
        priority: Int,
        handler: (GameContext, Player) -> Unit,
    ) {
        beforeInfoHandlers +=
            ordered(
                featureId = featureId,
                priority = priority,
                handler = handler,
            )
    }

    internal fun registerAfterInfo(
        featureId: String,
        priority: Int,
        handler: (GameContext, Player) -> Unit,
    ) {
        afterInfoHandlers +=
            ordered(
                featureId = featureId,
                priority = priority,
                handler = handler,
            )
    }

    private fun buildGameMessages():
        GameMessageConsumerRepository<Player> {
        val builder =
            GameMessageConsumerRepositoryBuilder<Player>()

        builder.addListener<ClientCheat> { packet ->
            dispatchCommand(
                handlers = commandHandlers,
                player = this,
                rawCommand = packet.command,
            )
        }

        for (configuration in packetConfigurations) {
            builder.configuration()
        }

        return builder.build()
    }

    private fun <T> ordered(
        featureId: String,
        priority: Int,
        handler: T,
    ): OrderedHandler<T> {
        return OrderedHandler(
            priority = priority,
            sequence = registrationSequence++,
            featureId = featureId,
            handler = handler,
        )
    }
}

class FeatureRegistrar internal constructor(
    val featureId: String,
    private val registry: FeatureRegistry,
) {

    // Registers the single feature responsible for accepting normal logins.
    fun onLogin(
        handler: (LoginAttempt) -> Unit,
    ) {
        registry.registerLoginHandler(
            featureId = featureId,
            handler = handler,
        )
    }

    // Registers the single feature responsible for reconnect policy.
    fun onReconnect(
        handler: (ReconnectAttempt) -> Unit,
    ) {
        registry.registerReconnectHandler(
            featureId = featureId,
            handler = handler,
        )
    }

    // Adds RSProt incoming packet consumers.
    fun packets(
        configuration:
            GameMessageConsumerRepositoryBuilder<Player>.() -> Unit,
    ) {
        registry.registerPackets(configuration)
    }

    // Registers a development command owned by this feature.
    fun command(
        handler: FeatureCommandHandler,
    ) {
        registry.registerCommand(handler)
    }

    // Runs once at the start of a server game cycle.
    fun onCycleStart(
        priority: Int = 0,
        handler: (GameContext) -> Unit,
    ) {
        registry.registerCycleStart(
            featureId = featureId,
            priority = priority,
            handler = handler,
        )
    }

    // Runs before RSProt information packets are built.
    fun beforeInfoUpdate(
        priority: Int = 0,
        handler: (GameContext, Player) -> Unit,
    ) {
        registry.registerBeforeInfo(
            featureId = featureId,
            priority = priority,
            handler = handler,
        )
    }

    // Runs after RSProt information output is queued.
    fun afterInfoUpdate(
        priority: Int = 0,
        handler: (GameContext, Player) -> Unit,
    ) {
        registry.registerAfterInfo(
            featureId = featureId,
            priority = priority,
            handler = handler,
        )
    }
}

class FeatureRuntime internal constructor(
    val featureIds: List<String>,
    private val loginHandler: ((LoginAttempt) -> Unit)?,
    private val reconnectHandler: ((ReconnectAttempt) -> Unit)?,
    val gameMessages: GameMessageConsumerRepository<Player>,
    private val commandHandlers: List<FeatureCommandHandler>,
    private val cycleStartHandlers:
        List<OrderedHandler<(GameContext) -> Unit>>,
    private val beforeInfoHandlers:
        List<OrderedHandler<(GameContext, Player) -> Unit>>,
    private val afterInfoHandlers:
        List<OrderedHandler<(GameContext, Player) -> Unit>>,
) {

    fun dispatchLogin(
        attempt: LoginAttempt,
    ): Boolean {
        val handler =
            loginHandler
                ?: return false

        handler(attempt)
        return true
    }

    fun dispatchReconnect(
        attempt: ReconnectAttempt,
    ): Boolean {
        val handler =
            reconnectHandler
                ?: return false

        handler(attempt)
        return true
    }

    fun dispatchCommand(
        player: Player,
        rawCommand: String,
    ): Boolean =
        dispatchCommand(
            handlers = commandHandlers,
            player = player,
            rawCommand = rawCommand,
        )

    fun cycleStart(
        context: GameContext,
    ) {
        for (registration in cycleStartHandlers) {
            registration.handler(context)
        }
    }

    fun beforeInfoUpdate(
        context: GameContext,
        player: Player,
    ) {
        for (registration in beforeInfoHandlers) {
            registration.handler(
                context,
                player,
            )
        }
    }

    fun afterInfoUpdate(
        context: GameContext,
        player: Player,
    ) {
        for (registration in afterInfoHandlers) {
            registration.handler(
                context,
                player,
            )
        }
    }
}

private fun dispatchCommand(
    handlers: List<FeatureCommandHandler>,
    player: Player,
    rawCommand: String,
): Boolean {
    val parts =
        rawCommand
            .trim()
            .split(" ")
            .filter(String::isNotBlank)

    val command =
        parts.firstOrNull()
            ?.lowercase()
            ?: return false

    val arguments =
        parts.drop(1)

    for (handler in handlers) {
        if (
            handler(
                player,
                command,
                arguments,
            )
        ) {
            return true
        }
    }

    return false
}

internal data class OrderedHandler<T>(
    val priority: Int,
    val sequence: Int,
    val featureId: String,
    val handler: T,
) : Comparable<OrderedHandler<T>> {

    override fun compareTo(
        other: OrderedHandler<T>,
    ): Int {
        val priorityComparison =
            priority.compareTo(
                other.priority
            )

        return if (priorityComparison != 0) {
            priorityComparison
        } else {
            sequence.compareTo(
                other.sequence
            )
        }
    }
}