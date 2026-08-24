# Third-party notes

## RS Mod Routefinder

This project uses the published `org.rsmod:rsmod-routefinder:6.0.0` dependency for RuneScape-style BFS pathfinding and collision flags.

Project: https://github.com/rsmod/rsmod

## Skretzo/shortest-path collision data

At runtime the server downloads a pinned collision-map snapshot from:

https://github.com/Skretzo/shortest-path

Pinned commit: `3208646f33c8f155d0262c5fc84f8e29f7599838`

The upstream project declares the BSD 2-Clause License. The collision data is not embedded in this source archive; it is downloaded and cached by the server at runtime.
