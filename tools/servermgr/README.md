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
servermgr create dungeon01 --type DUNGEONS --memory 3G
servermgr deploy                                          # build the plugins, put them on every server
servermgr start all
```

Players connect to the proxy on port 25565. Or just run `servermgr` and use the dashboard, which
walks you through setup on first run.

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
  password, and a flat world. Plugins: the SkyBlock plugin from the repository build, WorldEdit and
  PacketEvents (newest releases for this Minecraft version, from Modrinth). The plugin's config gets
  the server's name and type and the MongoDB settings.
- **Per type**: dungeon servers get the captured rooms linked in (`plugins/dungeons/dungeon-rooms/rooms`
  → the data repository's `rooms/`) and random ticks and mob spawning turned off on first start.
  Every server gets fixed time and weather and no advancement messages.
- Downloads are checked against their published checksums and cached in `network/cache`.

## Commands

| | |
|---|---|
| `init` | new network with its proxy (`--java --repo --data --mongo --database --port --accept-eula`) |
| `create NAME --type TYPE` | add a Paper server (`--memory 2G`, `--start`); types: LOBBY, DUNGEONS, CRIMSON_ISLE, DWARVEN_MINES, NONE |
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
