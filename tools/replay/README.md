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
one in `servers/<name>/cache/mojang_<version>.jar`) and writes item ids, packet ids and map
colours to `data/`. It needs a JDK (`--java <path>`, else `JAVA_HOME`, else `java` on the path).
Without it, newer recordings still decode, but item types and map colours may be wrong.

## Commands

| | |
|---|---|
| `decode <rec.mcpr> [--out dir]` | `timeline.txt`, `sidebar.txt`, `tab.txt` and `gui.txt` (below) |
| `maps <rec.mcpr> <seconds>... [--map id] [--out dir]` | every map (or one) as a PNG at those moments |
| `item <rec.mcpr> <slot> <from s> <to s>` | name, lore and map id of items put in an inventory slot |
| `raw <rec.mcpr> <from s> <to s> <packet>...` | decoded packets of those types as JSON, e.g. `system_chat` |
| `scan <rec.mcpr>` | packet counts, and what couldn't be fully decoded |

Output goes to `out/<recording>` unless `--out` says otherwise. Times are from the start of the
recording. Text keeps its colours as `&` codes (`&c` red, `&l` bold, `&#rrggbb`); click actions
show as `⟦click:run_command /instancerequeue⟧`.

- **timeline.txt**: chat, action bar (when it changes in more than numbers), titles, boss bar,
  server switches, world changes, teleports, menus opened and closed, and items appearing in the
  inventory (hotbar slots are 36-44).
- **sidebar.txt** and **tab.txt**: a snapshot each time they change in more than numbers, or
  every half minute while they change. The tab list is in slot order (servers fill it with fake
  players named `!A-a`, `!A-b`, ... to order it).
- **gui.txt**: every menu's contents, slot by slot, and changes while it's open.

Find a map's id with `item` (the `map id` of the map item), then render it with `maps`.

## Versions

The Minecraft version comes from the recording. When minecraft-data doesn't know it yet, the
packet layouts of an older version are used with the differences patched in
(`lib/recording.js`, `PATCHES`). For 26.2 that's 26.1's layouts: packet ids are the same (checked
against the 26.2 packet report), and only the team packet changed. Undecodable packets are
skipped; `scan` lists them (entity metadata and particles are the known ones, which the decoder
doesn't use).

## Recordings are private

They hold other players' names, chat and profiles. Keep them, and anything decoded from them,
out of this repository (`out/` and `data/` are ignored).
