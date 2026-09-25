# Dungeon Scanner

A client-side Fabric mod that saves Hypixel Catacombs rooms as schematics while you play. It
only reads blocks the server has already sent to your client. It never sends anything or changes
the world.

For private use only. The rooms are Hypixel's builds, and a world-reading mod isn't on Hypixel's
allowed-modifications list, so use it at your own risk.

## Install

- Minecraft **26.2**, Fabric Loader 0.19.5+, Fabric API for 26.2.
- Hypixel SkyBlock only allows the two newest Minecraft versions. When it moves on, update the
  versions in `gradle.properties` (see https://fabricmc.net/develop) and rebuild.
- Build with `./gradlew build` (needs JDK 25). Put `build/libs/dungeon-scanner-1.0.0.jar` in
  your `mods` folder.

## Use

Auto mode is on by default. When Mort says *"Here, I found this map…"* at the start of a run, the
mod starts scanning.

While scanning, every second it:
- re-reads the loaded part of the dungeon;
- saves every room whose chunks are all loaded.

Most rooms are saved in the first seconds, before anyone walks in and loots or opens things. The
far ones are saved as their chunks load. It stops once every room is saved, you leave, or 3
minutes pass.

| Command | |
|---|---|
| `/dscan` | start scanning now |
| `/dscan stop` | stop |
| `/dscan status` | how many of the 140 known rooms you have, and which are missing |
| `/dscan auto true\|false` | toggle auto mode |

Each room is saved once. Scanning the same room again in another run (even rotated) produces an
identical file, and it's skipped. To collect them all you'll need many runs across floors: some
rooms are floor-specific and the 62 regular 1x1s show up at random.

## Output (`.minecraft/dungeon-scanner/`)

```
rooms/<room>/<room>_<hash>.schem       Sponge v3, modern block states (WorldEdit 7 / FAWE)
rooms/<room>/<room>_<hash>.schematic   MCEdit format, 1.8 block ids (WorldEdit 6 on a 1.8 server)
rooms/<room>/<room>_<hash>.json        metadata, see below
doors/<type>/<type>_<hash>.*           door structures (normal/wither/blood/entrance)
runs/<time>_<floor>.json               the full layout of each run
```

**Frame.** Every room is saved in its own frame: the blue terracotta roof marker (Hypixel puts
one in a corner of every room) is at the north-west corner. x is east and z is south.

**Height.** `originY` in the JSON is the world y the schematic's bottom layer came from. Dungeon
floors are at y=68, so paste at that height to keep doors lined up.

Room JSON example:

```json
{
  "id": "catwalk", "name": "Catwalk", "type": "NORMAL", "shape": "1x3",
  "capturedRotation": "WEST", "rotationVerified": true,
  "originY": 60, "size": [95, 70, 31],
  "cells": [[0,0],[1,0],[2,0]],
  "doors": [ {"cell": [0,0], "side": "WEST", "type": "NORMAL"}, ... ]
}
```

- `cells` and `doors` are in the saved frame. They are exactly what the generator's door slots
  need: each door is the cell it's in plus the wall it's on.
- Hypixel's 1x1 rooms always have the same doors, so one capture gives their full door layout.
- Multi-cell rooms can have different doors per run. Each different capture is kept as its own
  variant.

## How it works

- **Grid:** cells are 31x31 with a 1-block gap, and the first cell's centre is at −185,−185.
- **Rooms:** the block column in the middle of a cell hashes to a "core", which Odin's
  `rooms.json` maps to a room name (bundled; BSD-3-Clause, © odtheking).
- **Doors:** a gap whose column tops out at y=73 is a doorway. The block at y=69 gives the type:
  coal = wither, infested stone = entrance, stained clay = blood. A gap filled up to the roof
  means both cells are one room. Same rules as Skytils/FunnyMap.
- **1.8 conversion:** each modern block state is turned back into its 1.8 `id:data` by inverting
  Minecraft's own world-upgrade table (`BlockStateData` plus the data fixers). Skulls (with their
  textures), signs, flower pots, banners and note blocks get their 1.8 tile entities. Anything
  with no 1.8 equivalent becomes air in the `.schematic` and is listed under
  `noLegacyEquivalent` in the JSON.

## Limits

- **Entities:** unnamed armor stands, item frames and paintings are only in the `.schem`, not the
  1.8 `.schematic`. Mobs aren't saved at all; they're spawned by the game.
- **Chests:** chest contents aren't saved, because the client doesn't have them.
- **Not tested on Hypixel.** The unit tests use a fake world. If a room has no roof marker it's
  saved unrotated, and the chat message says so.
