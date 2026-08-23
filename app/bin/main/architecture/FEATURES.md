# Feature development

The project uses a vertical-slice architecture.

## Core versus features

`org.example.app.core` contains stable server plumbing: RSProt transport,
cache/RSA bootstrap, the single-threaded game cycle, player lifecycle and the
feature registration API. Core code must not import a class from
`org.example.app.features`.

`org.example.app.features.<feature>` owns a gameplay capability end-to-end:
packet consumers, feature logic, feature-specific models/state and lifecycle
hooks.

`FeatureCatalog` is the composition list. Adding or removing a feature changes
that catalog, not the core.

## Scaffold a new feature

1. Create `org.example.app.features.<name>`.
2. Add `<Name>Feature : Feature` with a unique `id`.
3. Keep packet handlers, state and rules in that same package.
4. Register only the hooks you need in `install`:
   - `registrar.packets { addListener<Packet> { ... } }`
   - `registrar.onCycleStart { context -> ... }`
   - `registrar.beforeInfoUpdate { context, player -> ... }`
   - `registrar.afterInfoUpdate { context, player -> ... }`
5. Store per-player feature state with `player.featureState.getOrPut(...)`.
   Do not add feature flags/fields to core `Player`.
6. Add one instance of the feature to `FeatureCatalog.all`.

Example:

```kotlin
class ChatFeature : Feature {
    override val id = "chat"

    override fun install(registrar: FeatureRegistrar) {
        registrar.packets {
            addListener<SomeChatPacket> { packet ->
                // `this` is Player; call chat-slice logic here.
            }
        }
    }
}
```

## Ordering rule

Hooks with lower numeric priority run first. Prefer the default priority `0`.
Use explicit priorities only when protocol ordering genuinely requires them.

## Threading rule

Feature game-cycle hooks and registered RSProt packet consumers execute on the
single game/communication thread. Network login callbacks may occur on Netty
threads, so a feature must queue work (as `LoginFeature` does) before mutating
game-thread-owned state.
