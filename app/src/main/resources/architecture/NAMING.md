# Naming convention

This project follows one file/class naming convention everywhere, in both
`core` and `features`. It was already the dominant style in this codebase;
this document writes it down precisely and is the reference for enforcing it
on every file, including new ones.

The rule of thumb: **the suffix says what role a class plays in the
architecture** (rules in `README`/`FEATURES.md`), not what package it lives
in. A file's simple name should tell you, before opening it, whether it's
wiring, business logic, state, or data.

## Suffixes

- **`<Feature>Feature.kt`** -- the single entry point implementing the core
  `Feature` interface for one vertical slice. Wires its own handlers,
  services and state through `FeatureRegistrar`; contains little to no
  business logic of its own once a feature has a `Service` to delegate to.
  Examples: `WoodcuttingFeature`, `CombatFeature`, `FiremakingFeature`.

- **`<Feature>Service.kt`** -- the business logic and state transitions for a
  feature, or a core-owned piece of shared infrastructure. `Service` is used
  for both: a feature's own rules (`WoodcuttingActionService`,
  `SkillLevelUpService`) and a core capability that isn't itself a `Feature`
  (`WorldLocService`, `GroundItemService`, `ExperienceService`). Every
  feature whose `install()` would otherwise mix packet decoding with rule
  evaluation splits that logic into a `Service` the `Feature` delegates to --
  see `FiremakingFeature`/`FiremakingService` for the reference split.

- **`<Action><Feature>Handler.kt`** / **`<Feature><Aspect>Handler.kt`** --
  incoming-packet handling for one specific action or one specific UI
  component on a shared packet type. Examples: `PublicChatHandler`,
  `CombatOptionsHandler`, `SkillGuideHandler`, `JournalTabHandler`. A
  `Handler` decodes and validates one packet shape and calls into a
  `Service`; it does not itself hold cross-cycle state.

- **`<Feature>State.kt`** -- per-player (or per-world) mutable state owned by
  one feature, stored in `Player.featureState` via a private `getOrPut`
  extension property next to the class. Examples: `MovementState`,
  `CombatState`, `WoodcuttingState`. Sync-tracking state that exists purely
  to gate a resync gets its own small `...SyncState` file
  (`CombatEquipmentSyncState`, `WorldLocSyncState`).

- **`<Noun>.kt`** -- a plain domain model or value type with no behaviour of
  its own: `ItemStack`, `WorldPosition`, `GroundItem`, `Npc`. Enums describing
  a closed set of game values also live here unsuffixed (`CombatStyle`,
  `EquipmentSlot`).

- **`<Noun>Definition.kt`** -- static/cached definition data loaded from the
  cache, config, or a hard-coded table: `ItemDefinition`,
  `EquipmentDefinition`, `CombatStyleDefinition`.

- **`<Noun>Repository.kt`** -- read (and sometimes write) access to one
  definition or persisted data set: `ItemDefinitionRepository`,
  `PlayerPersistenceRepository`, `SkillUnlockRepository`.

- **Plain `<Noun>` static utility objects** -- a stateless Kotlin `object`
  holding pure lookup/formatting/protocol-wiring logic that isn't itself a
  service with a lifecycle: `SkillExperience`, `HuffmanLoader`,
  `CombatWeaponCategories`, `ZoneBroadcast`. These never hold mutable game
  state; if a "utility" starts needing state or constructor dependencies,
  it stops being a plain object and becomes a `Service`.

## Interfaces contributed by `core` for dependency injection

A capability that more than one feature needs, but that isn't generic enough
to just move into `core` outright, is exposed as a plain-named interface
owned by `core`:

- `Feature`, `ItemDefinitionSource`, `CollisionMapProvider`,
  `MovementCoordinator`, `InventoryUiSync`.

Concrete implementations are prefixed with whatever makes that
implementation concrete, when more than one implementation is plausible:

- `SqliteItemDefinitionSource`, `StaticItemDefinitionSource`,
  `RemoteCollisionMapProvider`.

When a feature's own primary service is simply *the* implementation of a
core interface (there is no other and never will be a second one), it keeps
its natural `<Feature>Service` name instead of being renamed to fit the
provider-prefix pattern -- for example `features.movement.MovementService`
implements `core.movement.MovementCoordinator` directly,
`features.inventory.InventorySyncService` implements
`core.inventory.InventoryUiSync` directly. The prefix pattern is for
interchangeable, swappable core-level sources; it is not a mandatory suffix
on every interface implementation.

## What decides `core` vs. `features`

A class belongs in `core` when it is infrastructure any current or future
feature could need and it has zero knowledge of any specific feature's
gameplay rules: the game loop, player lifecycle, persistence, cache/network
bootstrap, collision/world state (including dynamic world locs and ground
items -- see `FEATURES.md`), and the feature composition contracts
themselves.

A class belongs in a feature when it encodes a specific gameplay rule, even
a small one: an ignition success curve, a chat-mode toggle, a combat style
normalization rule. If moving a class out of a feature and into `core` would
require `core` to import something feature-specific, it stays in the
feature; if it doesn't, and other features already need it, it is a
candidate to move to `core` rather than staying feature-private and being
imported cross-feature.

## Enforcement

- No file under `features/<name>/...` may import a symbol from
  `features/<other-name>/...` directly. The one exception is
  `features/FeatureCatalog.kt`, the composition root, which is the only file
  allowed to import concrete `Feature` classes across every slice in order to
  wire them together.
- No file under `core/...` may import anything from `features/...`.
- Every `.kt` file has a one-to-few-line KDoc (or `//`) header immediately
  after its `package`/`import` block explaining what the file is for and,
  where it isn't obvious, why it exists in that form.
