# Dungeon Scanner

A client-side Fabric mod that saves Hypixel Catacombs rooms as schematics while you play, and
any world you walk through as a Minecraft world's region files (`/dscan world`, below). It only
reads blocks the server has already sent to your client. It never sends anything or changes the
world.

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
| `/dscan reconvert` | rebuild every `.schematic` from its `.schem` with the current converter, and cut L rooms saved by older versions down to their own blocks (see below) |

Each room is saved once. Scanning the same room again in another run (even rotated) produces an
identical file, and it's skipped. To collect them all you'll need many runs across floors: some
rooms are floor-specific and the 62 regular 1x1s show up at random.

## Saving a world (`/dscan world`)

For maps rather than rooms: the islands and hubs to build on. It saves whatever world you walk
through as region files that a 26.2 server or singleplayer world loads, and only what the server
has already sent: it never asks for a chunk, changes your view distance, moves you or sends
anything. What you walk past is what you get, so walk slowly past everything you want (name tags
and signs only come close up), and save an island before you touch it.

| Command | |
|---|---|
| `/dscan world start [name]` | start saving, into `worlds/<time>[_name]/` |
| `/dscan world stop` | finish: the last world's files are written and closed (quitting the game does it too) |
| `/dscan world status` | chunks and decorations saved so far |

Hypixel puts each island and server in a world of its own; each gets its own folder, numbered and
named after the first area the sidebar shows there (`saving-<n>` while it's being saved, and after
a crash):

```
worlds/<time>[_name]/<n>_<area>/region/r.x.z.mca     blocks, biomes and block entities (sign text, heads' skins)
                                entities/r.x.z.mca   item frames, paintings, unnamed armor stands, block and item displays
                                capture.json         the areas, server id, dimension, height, times, counts and biomes
```

- A chunk is saved once, two seconds after it arrives (sign text and heads' skins can come just
  after the chunk), or as it unloads if that's sooner. What's kept is the chunk as it arrived:
  breaking a block afterwards doesn't change it. Decorations are kept as first seen.
- Name tags, holograms, NPCs and mobs aren't saved; the game spawns those. The replay tool's
  `npcs.json` has a recording's NPCs and holograms.
- Light isn't saved: the server works it out as it loads the chunks. Nor are heightmaps, which
  it makes when they're missing.
- Biomes keep the names the server sent, Hypixel's own included (`hypixel:wilderness`,
  `hypixel:graveyard`...; `capture.json` lists them). A server without them loads those chunks
  with its default biome and logs "Unknown registry key ... using default" for each section
  that has one; a datapack with Hypixel's biomes would give them their colours.

**Using a capture.** Paper 26 keeps each world's dimension in `<world>/dimensions/minecraft/<name>/`
(the main world's overworld is `overworld`): copy a capture's `region/` and `entities/` there. For
singleplayer, make a world (Superflat, preset The Void), quit it, and copy them into
`saves/<world>/dimensions/minecraft/overworld/`. The world's own generator makes the chunks you
didn't walk past, so give it a void one.

Checked with a 26.2 client on a vanilla 26.2 server, saving a flat world with a sign, an item
frame and armor stands through a trip to the Nether and back, and loading what it saved on a
fresh server: blocks and their states, the sign's text, the item frame and the unnamed armor stand
came back, the named stand and a text display were left out, chunks left before they settled
were saved as they unloaded, and the server lit everything.

## Output (`.minecraft/dungeon-scanner/`)

```
rooms/<room>/<room>_<hash>.schem       Sponge v3, modern block states (WorldEdit 7 / FAWE)
rooms/<room>/<room>_<hash>.schematic   MCEdit format, 1.8 block ids (WorldEdit 6 on a 1.8 server)
rooms/<room>/<room>_<hash>.json        metadata, see below
doors/<type>/<type>_<hash>.*           door structures (normal/wither/blood/entrance)
runs/<time>_<floor>.json               the full layout of each run
```

**Frame.** Every room is saved in its own frame: the blue terracotta roof marker (Hypixel puts
one in a corner of every room) is at the north-west corner. x is east and z is south. Straight
rooms and 2x2s aren't searched for a marker: Hypixel only ever places them with it in one spot
(north-west for horizontal ones and 2x2s, north-east for vertical ones), so the mod uses that and
only checks the marker is there. 1x1 and L rooms are searched on all four corners, over the
room's full height.

**L rooms.** The box around an L also holds the cell it's missing, which belongs to some other
room. Only the L's own cells and the gaps between them are saved; the rest is air. Rooms saved
before this was added still have their neighbour in them: `/dscan reconvert` cuts them down and
renames them to their new hash, merging copies of the same room.

**Doors.** A door's type sticks for the whole run, so a wither door someone already opened is
still saved as a wither door.

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
- **Reconvert** only rewrites `.schem` files for old L rooms; everything else keeps its `.schem`
  and gets a fresh `.schematic`, which is identical unless the 1.8 conversion changed.
- **Chests:** chest contents aren't saved, because the client doesn't have them.
- **Not tested on Hypixel.** The unit tests use a fake world. If a room has no roof marker it's
  saved unrotated, and the chat message says so. `/dscan world` was run in a 26.2 client on a
  local server, not on Hypixel.
