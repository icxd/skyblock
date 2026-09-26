# SkyBlock proxy plugin

The Velocity side of the network. `target/skyblock-proxy-1.0-SNAPSHOT.jar` goes in Velocity's
`plugins` folder (`servermgr deploy` puts it there).

## What it does

- **Server switches keep player data safe.** Before any move (`/hub`, parties, dungeons, `/server`,
  `/send`, other plugins), the proxy asks the server the player is on to save and release their
  data, and waits for it (up to 3 s) before connecting them to the next one. If the move fails, the
  old server takes the data back and the player carries on there.
- **Routing.** New players go to the emptiest hub (`LOBBY` server). Players kicked from a server
  (it stopped or crashed) go to another hub. Players who drop out of a dungeon run and log back in
  go back into it.
- **Parties**, as on Hypixel (below). They live on the proxy and follow players between servers;
  a proxy restart clears them.
- **Dungeons.** `/joininstance` sends a party to the dungeon server with the fewest runs. That
  server builds the floor in a world of its own, so several runs share a server.
- **Ranks and permissions.** Rank prefixes show in party messages. Only STAFF can use Velocity's own
  commands (`/server`, `/send`, `/glist`, ...), so nobody gets past the dungeon queue with
  `/server`.

## Commands

| | |
|---|---|
| `/hub` (`/lobby`, `/l`) | to the emptiest hub |
| `/party <player>...` (`/p`) | invite; also `accept`, `leave`, `list`, `kick`, `kickoffline`, `disband`, `transfer`, `promote`, `demote`, `warp`, `chat`, `settings allinvite`, `help` |
| `/pc <message>`, `/pl` | party chat, party list |
| `/joininstance <floor>` (`/joindungeon`) | `CATACOMBS_FLOOR_SEVEN` and the like, or `E`, `F1`-`F7`, `M1`-`M7` |

Party rules: invites last 60 seconds; the leader can promote moderators, who may invite and kick
members; `settings allinvite` lets everyone invite. A member who disconnects has 5 minutes to come
back before they're removed (if it's the leader, the party is disbanded). If the leader leaves, the
next moderator (or member) takes over. Up to `party.max-size` players; a dungeon takes at most 5, all
online, and only the leader can start one.

## Settings

`plugins/skyblock/config.properties` (servermgr writes it with the network's MongoDB):

| | |
|---|---|
| `mongodb.uri`, `mongodb.database` | the same database as the Paper servers |
| `dungeons.runs-per-server` | how many runs share a dungeon server (default 4); when every one is full, parties wait in line and go as soon as a run ends |
| `party.max-size` | default 10 |

`remote-console.properties` is servermgr's console into the proxy (RCON, local only).

## How it fits together

- **Server list and load** come from the `servers` collection, where every Paper server writes a
  heartbeat (type, players) every 3 seconds. A server whose heartbeat is 12 s old counts as down.
  The backends themselves are still the ones in `velocity.toml`.
- **Proxy and servers talk** over the `skyblock:main` plugin-message channel, through a player's
  connection (`common/.../ProxyMessage.java`): `HANDOFF` / `HANDED_OFF` before a move, `RECLAIM`
  after a failed one, and `SEND` for a server to ask the proxy to move someone (a server type
  like `LOBBY`, or `server:NAME`). The proxy drops anything a client sends on the channel.
- **Dungeon runs** are documents in the `runs` collection (`common/.../Runs.java`). The proxy writes
  one (floor, leader, members, server) and sends the party. The dungeon server picks it up and marks
  it `running` once the floor is pasted, then `ended` a minute after everyone has left.
- **On the dungeon server** (`paper/.../dungeons/instance/RunManager.java`), run worlds are named
  `run-1`, `run-2`, ... and reused: when one is taken, the largest floor's area is emptied and the
  new floor pasted where Hypixel has it, at -200,-200. One spare world is always generated ahead,
  since generating chunks takes seconds and emptying them doesn't. Players who arrive first wait
  on a glass platform above the entrance.
