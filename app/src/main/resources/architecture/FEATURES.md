# Feature architecture

The server uses vertical slices: gameplay lives in `features`, while stable
runtime capabilities live in `core`. See `NAMING.md` for the file/class
naming convention this document assumes.

## Core

`org.example.app.core` owns infrastructure shared by multiple features:

- cache/bootstrap and RSA/network setup;
- single-threaded game/communication loop;
- player lifecycle and generic player capabilities (inventory, equipment,
  skills, vars);
- SQLite persistence and static item/varbit repositories;
- generic world state: collision/routefinding (`core.world.collision`),
  dynamic world locs (`WorldLocService`), and ground items
  (`GroundItemService`) -- these are world state, not any one feature's
  business rules, so they live in `core.world` even though several gameplay
  features drive them;
- the generic item-on-item interaction bus (`core.items.ItemOnItemDispatcher`);
- feature registration/composition contracts (`core.feature`), including the
  `MovementCoordinator` and `InventoryUiSync` capability interfaces features
  use to reach movement and inventory-resync without depending on those
  features' concrete classes.

Core must not import concrete feature implementations. Nothing in `core`
knows Woodcutting, Firemaking, Combat, etc. exist.

## Features

`org.example.app.features.<feature>` owns one gameplay capability end-to-end.
A feature may contain its packet handlers, services, models and per-player
state without leaking those details into `Player` or into another feature.

Installed slices currently include:

- login
- world bootstrap / map build-area streaming
- movement / routefinding
- npcs
- skills
- inventory
- item-use (generic item-on-item packet decoding; gameplay interactions like
  Firemaking register themselves into it)
- firemaking
- ground items
- woodcutting
- combat
- interfaces (game-frame chrome: journal, social, logout, world switcher)
- chat

`FeatureCatalog` is the only concrete feature composition list, and the only
file allowed to import multiple features' concrete classes at once.
`FeatureDependencies` is the core-owned contract used to inject cross-cutting
core services into feature constructors without service locators or static
globals -- everything on it is constructed once by `ServerApplication` before
any feature exists.

### Why movement, world locs, ground items and item-on-item aren't
### feature-private

Woodcutting, Firemaking and Ground Items all need to move a player and to
touch dynamic world locs or ground items; Firemaking also needs the
item-on-item bus. Rather than importing each other's concrete classes (which
would violate the no-cross-feature-import rule), the pattern is:

- **Generic world state that any feature can touch** (`WorldLocService`,
  `GroundItemService`, `core.world.collision.RoutePlanner`,
  `ItemOnItemDispatcher`) is promoted to `core` outright. It has zero
  knowledge of any specific feature, so this doesn't violate "core never
  imports a feature."
- **A feature-owned capability other features must drive** (walking a player)
  stays in its owning feature, but is exposed through a narrow interface
  `core` declares (`MovementCoordinator`) and the owning feature's service
  implements (`features.movement.MovementService`). Consuming features
  depend on the interface type; `FeatureCatalog` wires the concrete instance
  in.

The same pattern resolved the equipment/inventory UI resync that used to be
wired from the `interfaces` feature into combat's own handler classes: it's
now owned entirely by `CombatFeature`, which reaches inventory only through
the core-owned `InventoryUiSync` interface.

### The shared `If3Button` interface-click packet

Many unrelated interfaces (journal, social, combat style, equipment,
inventory wielding, the skill guide, ...) all arrive on the same RSProt
`If3Button` game message, and only one game-message listener can be
registered per message type. Rather than one feature owning routing for
everyone else's clicks, every feature that owns an interface registers its
own handler via `registrar.onInterfaceButton { player, packet -> ... }`;
`FeatureRegistry` fans the packet out to every registered handler in
priority order. This is the same "many features register, one registry
dispatches" shape already used for commands (`registrar.command { ... }`).

## Adding a feature

1. Create `org.example.app.features.<name>`.
2. If the feature has any business logic beyond decoding a packet, write it
   as `<Name>Service` first; keep `<Name>Feature` limited to wiring.
3. Add `<Name>Feature : Feature` with a unique `id`.
4. Keep feature-specific handlers/state/rules under that package. If the
   feature needs a capability another feature owns (movement, inventory
   resync, ...), depend on the `core`-owned interface for it, not the
   concrete feature class. If the feature needs a capability that's really
   generic world state, check whether it belongs in `core` instead of being
   invented as feature-private state two features end up needing.
5. Register only the lifecycle hooks you need:
   - `registrar.packets { ... }` for packet types unique to this feature;
   - `registrar.onInterfaceButton { player, packet -> ... }` if this feature
     owns part of an `If3Button`-driven interface;
   - `registrar.onCycleStart { ... }`
   - `registrar.beforeInfoUpdate { ... }`
   - `registrar.afterInfoUpdate { ... }`
   - `registrar.command { ... }` for a development command.
6. Store feature-owned player state in `player.featureState` via a private
   `<Name>State` class and a `Player.<name>State` extension property, same
   as the existing features.
7. Add the feature to `FeatureCatalog.create(...)`, constructing any
   feature-local services it needs and passing shared `core` services
   straight from `dependencies`.
8. Give every new file a short header comment (see `NAMING.md`).
9. Compile (`./gradlew :app:compileKotlin`) before moving on.

### Worked example: a two-minute walkthrough

Say you're adding a `fishing` feature that needs to walk the player to a
fishing spot (an existing world loc) and needs no new core state:

1. `features/fishing/FishingService.kt` -- the catch-roll/xp logic, taking
   `movement: MovementCoordinator`, `worldLocs: WorldLocService` and
   `experience: ExperienceService` as constructor parameters (all injected,
   none of them concrete feature classes from another slice).
2. `features/fishing/FishingFeature.kt` -- implements `Feature`, id
   `"fishing"`, constructs `FishingService`, registers the `OpLocV2` listener
   for fishing spots and an `onCycleStart` hook that delegates to the
   service, exactly like `WoodcuttingFeature` does for trees.
3. `features/fishing/FishingState.kt` -- per-player in-progress catch state,
   following the same `featureState.getOrPut` pattern as
   `WoodcuttingState`.
4. In `FeatureCatalog.create(...)`, construct
   `FishingFeature(movement = movement, worldLocs = dependencies.worldLocs,
   experience = experience)` and add it to the returned list.

No other file changes. `fishing` never imports `woodcutting`, `combat`, or
any other slice, and nothing in `core` needed to change.

## Ordering

Lower numeric hook priorities run first. Use an explicit priority only when
ordering is meaningful. Movement uses an early cycle-start priority so the
new position is visible to RSProt's root-coordinate synchronization in that
same 600 ms game cycle.

## Threading

Game-cycle hooks and registered game packet consumers execute on the single
RSProt communication/game thread. Login callbacks may originate on Netty
threads, so login uses a queue before mutating game-thread-owned state.

The movement routefinder is intentionally shared and not thread-safe; this is
safe because movement packets and game cycles are serialized on that same
communication thread.
