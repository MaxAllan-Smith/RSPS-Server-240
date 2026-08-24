# RSPS_RSProt_Server refactor

Target: OSRS protocol revision 240 / client patch 240.3.

## What changed

- Preserved the existing vertical-slice feature architecture and all working login, UI, chat, skills, inventory, equipment, combat, SQLite persistence, static item database and Wiki-price synchronization code.
- Removed the obsolete pre-`core` duplicate package tree (`cache`, `config`, `crypto`, `engine`, `network`, `player`). Nothing in the current application referenced it.
- Removed the abandoned hand-written revision-240 loc/collision parser and the unused XTEA bootstrap experiment.
- Kept `org.rsmod:rsmod-routefinder:6.0.0` as the pathfinding/collision engine.
- Added a small adapter that hydrates RSMod's `CollisionFlagMap` from the pre-generated OSRS collision-map format maintained by Skretzo/shortest-path.
- Pinned the collision snapshot to commit `3208646f33c8f155d0262c5fc84f8e29f7599838` (2026-08-13), immediately after the selected OpenRS2 cache timestamp (2026-08-12).
- Added player movement for `MoveGameClick` and `MoveMinimapClick`.
- Added automatic `RebuildNormalV2` scene rebuilds as the player approaches the edge of the 104x104 build area.
- Player position continues to persist through the existing SQLite player persistence repository.

## Why this collision approach

`rsmod-routefinder` is a purpose-built RuneScape BFS routefinder and collision implementation. It handles RuneScape directional clipping and reachability rules without maintaining a custom pathfinding algorithm.

The published routefinder artifact does not include RSMod's much larger cache-decoding module. Pulling the full RSMod server/cache module graph into this server just to build collision would significantly increase coupling. The refactor therefore uses a pre-generated map as the static data source and adapts it into RSMod's native `CollisionFlagMap`.

This makes the server-side stack:

```
pinned OSRS collision-map.zip
        -> PrecomputedCollisionLoader
        -> RSMod CollisionFlagMap
        -> RSMod RouteFinding
        -> MovementFeature
        -> Player.position
        -> RSProt player info
```

## First startup

The collision map is downloaded once to:

```
.data/collision/collision-map-2026-08-13.zip
```

Later starts reuse it. This makes the server deterministic even if the upstream map later changes.

Expected startup logging includes:

```
[Collision] Downloading pinned OSRS collision map...
[Collision] Collision map ready: ...
[Collision] Hydrating RSMod collision map...
[Collision] RSMod collision ready: regions=..., blockedTiles=...
```

## Movement behavior

Both game-window and minimap clicks submit an absolute destination to `MovementFeature`. RSMod computes a collision-safe route. The route is expanded into unit tile steps and one walking step is consumed per 600 ms game cycle.

A new click replaces the current route immediately.

Ctrl/Shift metadata is retained in movement state but is intentionally not interpreted as running or moderator teleportation yet. Correct run behavior depends on the server's run-toggle/run-energy state, which does not currently exist. Normal users are never granted the client's Ctrl+Shift moderator-teleport suggestion.

## Map rebuilding

The initial login scene still uses `RebuildLoginV2`.

During travel, `WorldMapService` tracks the south-west base zone of the current 104x104 scene. When the player enters the outer 16-tile margin it:

1. recenters RSProt's root build area;
2. sends `RebuildNormalV2` for the player's current zone;
3. updates the tracked build-area base.

The client then loads the newly relevant map regions and naturally drops regions outside its rebuilt scene.

## Important static-collision limitation

The Shortest Path collision dataset is intentionally optimized for pathfinding and treats many doors as traversable so global paths can pass through buildings. This is a good static world map for walls, blocked terrain, water/void and ordinary routing, but it is not a dynamic world-state system.

When doors, temporary objects, player/NPC occupancy or instanced-map collision are implemented, layer those dynamic flags on top of the shared `WorldCollision` map. Do not replace RSMod's routefinder.

## Compile and run

From the project root after replacing `app/src` and `app/build.gradle.kts` with the files in this package:

```powershell
.\gradlew :app:compileKotlin
.\gradlew :app:run --no-configuration-cache
```

## Suggested verification

1. Login in Lumbridge.
2. Click several open tiles and confirm walking is one tile per tick.
3. Click across a building wall and confirm the route goes around it instead of crossing it.
4. Click toward water/blocked terrain and confirm the route stops at or routes around the reachable boundary.
5. Walk far enough north/south/east/west to cross the scene margin and look for `[Map] Rebuilt world around ...`.
6. Logout, reconnect and confirm the final position restores from SQLite.
7. Regression-test inventory/equipment, combat switching, skills, chat and Wiki item synchronization.
