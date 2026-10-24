# replay

Turns a [Replay Mod](https://www.replaymod.com/) recording (`.mcpr`) into readable text: what the
server sent, in order. That's how the Hypixel dungeon runs were studied: exact chat lines,
sidebar and tab list at every stage, every menu with its items and lore, and the maps (the
dungeon map, the score card) as images.

Recordings only hold what the server sent, not what the player did.

## Setup

Needs Node 18+.

```
cd tools/replay
npm install
node replay.js setup <vanilla server jar>     # only for versions minecraft-data doesn't know yet
```

`setup` runs Mojang's data generator on a vanilla server jar (not a Paper jar; servermgr keeps
one in `servers/<name>/cache/mojang_<version>.jar`) and writes item ids, entity type ids,
particle ids, sound ids, villager kinds, packet ids and map colours to `data/`. It needs a JDK (`--java <path>`, else
`JAVA_HOME`, else `java` on the path). Without it, newer recordings still decode, but item and
entity types, sounds and map colours may be wrong, and packets that carry particles (including
some entity metadata) don't decode: 26.2 renumbered entity types and particles, and added sounds.

## Commands

| | |
|---|---|
| `decode <rec.mcpr> [--out dir]` | `timeline.txt`, `sidebar.txt`, `tab.txt`, `gui.txt`, `sounds.txt`, `npcs.txt` and `npcs.json` (below) |
| `npcs <rec.mcpr>... --area <area> --out <file>` | the NPCs of every world whose sidebar named the area, merged across recordings, with what spawning them again needs: the plugin's `npcs/<map>.json` in the data checkout (below) |
| `maps <rec.mcpr> <seconds>... [--map id] [--out dir]` | every map (or one) as a PNG at those moments |
| `item <rec.mcpr> <slot> <from s> <to s>` | name, lore and map id of items put in an inventory slot |
| `raw <rec.mcpr> <from s> <to s> <packet>...` | decoded packets of those types as JSON, e.g. `system_chat` |
| `scan <rec.mcpr>` | packet counts, and what couldn't be fully decoded |

Output goes to `out/<recording>` unless `--out` says otherwise. Times are from the start of the
recording. Text keeps its colours as `&` codes (`&c` red, `&l` bold, `&#rrggbb`); click actions
show as `⟦click:run_command /instancerequeue⟧`.

- **timeline.txt**: chat, action bar (when it changes in more than numbers), titles, boss bar,
  server switches, world changes, teleports, menus opened and closed, items appearing in the
  inventory (hotbar slots are 36-44), and the sounds played where the player stood:
  `[sound] block.note_block.pling (block, volume 1, pitch 0.49)`. Hypixel plays its own sounds
  there (an NPC's voice, NEW OBJECTIVE's note, a menu's click); other players' and the world's
  come from wherever they happen, so they're left out (within 2 blocks counts).
- **sounds.txt**: every sound, with where it played and how far from the player. Hypixel sends
  each sound twice; the second copy is left out of both files.
- **sidebar.txt** and **tab.txt**: a snapshot each time they change in more than numbers, or
  every half minute while they change. The tab list is in slot order (servers fill it with fake
  players named `!A-a`, `!A-b`, ... to order it).
- **gui.txt**: every menu's contents, slot by slot, and changes while it's open.
- **npcs.txt**: for each world visited (named by the sidebar's server id and area), its NPCs
  with their name tag lines, position, facing, skin and what they hold; the holograms that
  stand on their own; and the named mobs, counted by name. A line that changes in more than
  numbers lists its new texts with when ("line 3 became Free Daily Reward!").
- **npcs.json**: the same, with each NPC's `textures` property (`value` and `signature`), as
  `Npc.spawn` takes it, each name tag's height over its feet (`heights`, as the lines go), a
  villager's kind, and times in milliseconds.

The Hub's NPCs for the plugin are made from the recordings that went there:

```
node replay.js npcs ../../../skyblock-dungeon-data/2026_10_24_10_56_23_1.mcpr --area Village --out ../../../skyblock-dungeon-data/npcs/hub.json
```

Name more recordings to add the NPCs they passed close by (an NPC is the same one in two
recordings when its type, spot and skin are).

Where the player is (for the sounds): Replay Mod spawns the recording player as an entity of
their own, numbered as the complement of the login packet's entity id (the replay's camera takes
that one), so it's that entity's position, or the last teleport's until it appears.

How NPCs are told apart (`lib/entities.js`): Hypixel's are players with version 2 UUIDs that
aren't in the tab list (real accounts are version 4), or mobs such as Jerry's villager, standing
still under their name tags: invisible armor stands in their column (text displays count too).
The name is the line above "CLICK", since a header like "⚑ GUIDES" can stand above it. Anything
that's hurt, dies, walks off or wears a level or health tag is a mob. Name tags are only sent near
the player, so an NPC passed at a distance comes out under "Players with no name tag", along with
dungeon mobs nobody fought. Damage numbers and holograms gone within 1.5 s are left out.

Find a map's id with `item` (the `map id` of the map item), then render it with `maps`.

## Versions

The Minecraft version comes from the recording. When minecraft-data doesn't know it yet, the
packet layouts of an older version are used with the differences patched in
(`lib/recording.js`, `PATCHES`). For 26.2 that's 26.1's layouts: packet ids are the same (checked
against the 26.2 packet report), and only the team packet changed; entity metadata's serializer
ids are the same too (checked against the 26.2 jar). Registry ids that moved come from `setup`:
item and entity types, and particles, which the decoder patches in. Undecodable packets are
skipped; `scan` lists them.

Item components from number 78 (`lock`) on moved by one in 26.2 (`sulfur_cube_content` came
before them), so an item with one of those (a lock, container loot, a mob variant...) may decode
wrong. No item in the September 2026 recordings has one: the highest they use is 72.

`npm test` runs the tests (Node's own test runner).

## Recordings are private

They hold other players' names, chat and profiles. Keep them, and anything decoded from them,
out of this repository (`out/` and `data/` are ignored).
