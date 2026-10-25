# servermgr

Sets up and runs a local SkyBlock network: a Velocity proxy and Paper servers behind it, wired
together automatically. Run it without arguments for a dashboard, or with a command for scripts.

## Build

Needs Go 1.24+ (or use a prebuilt binary):

```
cd tools/servermgr
go build                                   # servermgr / servermgr.exe for this machine
GOOS=windows GOARCH=amd64 go build         # or for another OS
```

You also need a JDK 25 for the servers, Maven for `deploy`, and a MongoDB the plugin can reach.

## Quick start

```
servermgr init --repo . --data ../skyblock-dungeon-data   # asks to accept the Minecraft EULA
servermgr create hub01 --type LOBBY
servermgr create island01 --type ISLAND
servermgr create dungeon01 --type DUNGEONS --memory 3G
servermgr deploy                                          # build the plugins, put them on every server
servermgr start all
```

Players connect to the proxy on port 25565, and join on their private island (an ISLAND server;
a hub if there's none). Or just run `servermgr` and use the dashboard, which walks you through
setup on first run.

The network lives in `./network` unless you pass `--dir FOLDER` or set `SKYBLOCK_NETWORK`. Its
`network.json` holds the settings (Java, repository, MongoDB, ports, passwords), so keep it private.

## What it sets up

- **Proxy** (`network/proxy`): Velocity with modern forwarding and a random forwarding secret,
  online mode on. `velocity.toml` is yours to edit, except `[servers]`, which servermgr rewrites
  when servers are added or removed (and tells a running proxy to reload).
  The SkyBlock proxy plugin (parties, dungeon queue, safe server switches; see `proxy/README.md`)
  gets `plugins/skyblock/config.properties` with the network's MongoDB; `dungeons.runs-per-server`
  and `party.max-size` in there are yours to tune.
- **Paper servers** (`network/servers/NAME`): Paper 26.2, bound to 127.0.0.1 so they're only reachable
  through the proxy, offline mode with the proxy's secret, RCON on a local port with a random
  password, and a flat world (or a captured map, below). Plugins: the SkyBlock plugin from the repository build, WorldEdit and
  PacketEvents (newest releases for this Minecraft version, from Modrinth). The plugin's config gets
  the server's name and type and the MongoDB settings.
- **Private data** (`--data`, the skyblock-dungeon-data checkout): every server gets its item
  definitions linked in (`plugins/dungeons/items` → the data repository's `items/`, made by
  `tools/items/build_items.py`), its collections and recipes (`plugins/dungeons/collections` →
  `collections/`, made by `tools/collections/build_collections.py`), its SkyBlock Leveling data
  (`plugins/dungeons/leveling` → `leveling/`, made by `tools/leveling/build_leveling.py`) and, once the
  checkout has them, its storage tables (`plugins/dungeons/storage` → `storage/`, see `tools/storage`)
  and the Hex's tables (`plugins/dungeons/hex` → `hex/`, see HEX.md), its captured maps
  (`plugins/dungeons/worlds` → `worlds/`, the dungeon scanner's `/dscan world` saves: island servers
  make new islands from the starter island's), its maps' NPCs (`plugins/dungeons/npcs` → `npcs/`,
  made by the replay tool's `npcs`), what the Museum takes (`plugins/dungeons/museum` → `museum/`, made
  by `tools/museum/build_museum.py`), and dungeon servers its captured rooms
  (`plugins/dungeons/dungeon-rooms/rooms` → `rooms/`). They come from Hypixel, so they stay in that
  private repository rather than in this one or the plugin. `deploy` adds links that are missing, so
  older servers get them too, and replaces ones that lead nowhere (say the data checkout moved); a
  folder or link a server has of its own is left alone. `init` stores `--data` and `--repo` as
  absolute paths, so a relative one like the quick start's works from anywhere.
- **Maps**: a new LOBBY server's world is Hypixel's Hub and a new DUNGEON_HUB server's the Dungeon
  Hub, from the newest capture in the data checkout's `worlds/` whose sidebar named the Village or
  the Dungeon Hub (without one, it gets a flat world and says so). The capture's region and entity
  files go in the world's `dimensions/minecraft/overworld`, the world around them is void (flat,
  air, `the_void` biome, with the generator settings file Paper 26 needs to start a world that has
  region files), and the plugin's config gets `server.map` (`hub`, `dungeon_hub`), which is how it
  knows Hypixel's spawns, portals and areas are there. `map` puts one in (or takes it off) for a
  stopped server, keeping the world it had as `world-before-<time>`. See ISLANDS_QUESTS.md.
- **Per type**: dungeon servers and servers with a map get random ticks and mob spawning turned off
  on first start (on a map, trees would drop their leaves and crops grow). ISLAND servers hold
  players' private islands, a world each, stored in MongoDB. Every server gets fixed time and
  weather and no advancement messages.
- Downloads are checked against their published checksums and cached in `network/cache`.

## Commands

| | |
|---|---|
| `init` | new network with its proxy (`--java --repo --data --mongo --database --port --accept-eula`) |
| `create NAME --type TYPE` | add a Paper server (`--memory 2G`, `--start`); types: LOBBY, DUNGEON_HUB, DUNGEONS, CRIMSON_ISLE, DWARVEN_MINES, ISLAND, NONE |
| `map NAME MAP\|none` | make a stopped server's world a captured map (`hub`, `dungeon_hub`), or take it off |
| `remove NAME` | take a stopped server off the network (`--keep-files` keeps its folder) |
| `start / stop / restart [NAME\|all]` | servers start in parallel and the proxy last; stopping does the proxy first |
| `kill NAME` | end it now |
| `status [--json]` | state, players, TPS, tick time, memory, uptime; shows servers that crashed |
| `logs NAME [-f] [-n 40]` | console output (`proxy` for the proxy) |
| `cmd NAME COMMAND...` / `console NAME` | run console commands, once or interactively |
| `deploy [--no-build] [--restart]` | build with Maven (using the network's JDK as JAVA_HOME) and install the plugins everywhere |
| `update` | newest Paper, Velocity and plugin builds; a running server switches on its next start |

Servers keep running when servermgr exits; they run detached with their output in `console.log`.
Stopping goes through each server's console (RCON; the proxy plugin adds one to Velocity), so worlds
are saved; `kill` doesn't.

## Dashboard keys

`↑↓` select, `enter` run a command on the selected server, `s` start, `x` stop, `r` restart,
`a` start all, `z` stop all, `n` new server, `d` deploy, `D` deploy and restart, `u` update,
`del` remove, `K` kill, `f` follow the console, `pgup/pgdn` scroll, `?` more keys, `q` quit.
