# Feature architecture

The server uses vertical slices: gameplay lives in `features`, while stable
runtime capabilities live in `core`.

## Core

`org.example.app.core` owns infrastructure shared by multiple features:

- cache/bootstrap and RSA/network setup;
- single-threaded game/communication loop;
- player lifecycle and generic player capabilities;
- SQLite persistence and static item repositories;
- generic world collision/pathfinding capability;
- feature registration/composition contracts.

Core must not import concrete feature implementations.

## Features

`org.example.app.features.<feature>` owns one gameplay capability end-to-end.
A feature may contain its packet handlers, services, models and per-player
state without leaking those details into `Player`.

Installed slices currently include:

- login
- world bootstrap / map build-area streaming
- movement / routefinding
- skills
- inventory
- combat
- interfaces
- chat

`FeatureCatalog` is the only concrete feature composition list.
`FeatureDependencies` is the core-owned contract used to inject cross-cutting
services into feature constructors without service locators or static globals.

## Adding a feature

1. Create `org.example.app.features.<name>`.
2. Add `<Name>Feature : Feature` with a unique `id`.
3. Keep feature-specific handlers/state/rules under that package.
4. Register only required lifecycle hooks:
   - `registrar.packets { ... }`
   - `registrar.onCycleStart { ... }`
   - `registrar.beforeInfoUpdate { ... }`
   - `registrar.afterInfoUpdate { ... }`
5. Store feature-owned player state in `player.featureState`.
6. Add the feature to `FeatureCatalog.create(...)`.

## Ordering

Lower numeric hook priorities run first. Use an explicit priority only when
ordering is meaningful. Movement uses an early cycle-start priority so the new
position is visible to RSProt's root-coordinate synchronization in that same
600 ms game cycle.

## Threading

Game-cycle hooks and registered game packet consumers execute on the single
RSProt communication/game thread. Login callbacks may originate on Netty
threads, so login uses a queue before mutating game-thread-owned state.

The movement routefinder is intentionally shared and not thread-safe; this is
safe because movement packets and game cycles are serialized on that same
communication thread.
