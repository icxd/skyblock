# Private islands, quests and the Hub

A new profile's first minutes, as Hypixel has them: its private island, Getting Started and the
quests after it, Jerry, the portal to the Hub on Hypixel's map, and the areas found there. Built
from the recording of a new profile's first ten minutes (2026-10-24, decoded with `tools/replay`:
chat, titles, boss bar, sidebar, menus, NPCs and holograms) and the dungeon scanner's captures of
the island, the Hub and the Dungeon Hub (`/dscan world`). The code is in
`paper/src/main/java/net/icxd/dungeons/island`, `.../quest`, `.../hub` and `.../region/Areas.java`.

## The data

The maps are captures in the data repository's `worlds/` (the dungeon scanner's README, "Saving a
world"), found by the area their sidebar named, the newest of each: "Your Island" (the starter
island), "Village" (the Hub) and "Dungeon Hub". servermgr links `worlds/` into every server as
`plugins/dungeons/worlds`, where island servers copy new islands from, and makes the Hub's and the
Dungeon Hub's the main world of LOBBY and DUNGEON_HUB servers when it creates them (`servermgr map`
for older servers). That also sets config.yml's `server.map`, which is how the plugin knows a
server has Hypixel's map. Without the captures, a new island is a 5x5 grass platform, and the hubs
are flat worlds without Hypixel's spawns, portals or areas.

The Hub's NPCs are `npcs/hub.json` in the data checkout, which the replay tool's `npcs` command makes
from the recordings that went there (its README); servermgr links `npcs/` into every server. Without
it the Hub has no NPCs, and the plugin says so in its log.

The captures pushed on 2026-10-24 predate the scanner saving leaves persistent and armor stands
without gravity, and the plugin makes up for both where it matters:

- **On the hubs**, random ticks are off (servermgr sets that), so the leaves stay. Every armor stand
  loses its gravity as it loads (`MapStands`): all 813 of the Hub's had it, and 7 fell out of the
  world.
- **On islands**, random ticks stay on, so the leaves decay the way Hypixel's did in the recording
  (it got an Oak Sapling). An island recaptured with the newer scanner would have leaves that never
  decay.
- **Jerry's leftover stands.** The island's capture also has three invisible, nameless armor stands
  where Jerry's name tags float (it was saved before the names came). They're removed as an island
  loads (`IslandRules`).

## Done

- **Private islands** (`Islands`).
  - **Worlds and storage.** Each profile has its own island, a world of its own
    (`island-<profile id>`) on an island server (`ISLAND`; a `NONE` server hosts them too, next to
    its hub). A new island is a copy of the starter island's capture: its blocks, plus its one
    entity, the Cobblestone Minion. Islands are stored in MongoDB (GridFS bucket `islands`,
    one file per profile: the world's region files, zipped), so any island server can load one.
  - **Loading and saving.** An island is loaded while its player is on it, then saved and unloaded
    once they've left. Before a player's data goes to another server (a move or logging off), their
    island is saved, and the data is released only once it's stored (`UserStore.beforeRelease`), so
    the next server loads the island as they left it. Only the server holding a player's data loads
    their island. Islands are also saved every 5 minutes and when the server stops.
  - **Getting there.** Players join on their island: the proxy sends them to the emptiest island
    server (or a hub if there's none), and the island loads while they log in. `/is` (`/island`),
    the Hub's portal and a profile switch also take them there. For a new profile, "Transferring you
    to a new island..." is said before the move, and "Your profile was changed to: Strawberry" 3 s
    after arriving, both as recorded.
- **On the island** (`IslandRules`).
  - **Breaking and building.** You can break and place anything on your own island (the hubs still
    refuse both).
  - **Drops.** A block's drops fall as SkyBlock items (`VanillaItems` picks the plain item of the
    material, so an oak log becomes `LOG`) for you to pick up, as on Hypixel: the recording's first
    log popped out and was picked up 0.6 s later. They count for their collection as you pick them
    up. The first item of a collection says "COLLECTION UNLOCKED Oak Log" (a click opens the
    collection). A log gives 6 Foraging XP as it breaks; there are no experience orbs.
  - **Crafting.** A crafting table opens SkyBlock's Craft Item, and the inventory's grid makes
    SkyBlock items (`VanillaCrafting`).
  - **The portal** is on the rock 15 blocks south of the island, across the void. There's no
    bridge: you build your own, as the recording's player did with oak planks. The portal takes you
    to the Hub, under its "Travel to: / Hub Island" sign.
  - **World rules.** No mobs spawn, and time and weather stand still. Dying puts you back at the
    island's spawn with your items. Random ticks stay on, so crops grow and leaves fall. The minion
    stands as captured and can't be taken apart.
- **Getting Started** (`Quests`, `QuestBar`, `QuestListener`).
  - **The quest.** A new profile starts on it and is welcomed when it first arrives on its island
    (the recorded welcome, with the player's name in their rank's colour). Its objectives:
    - Break a log (on your own island).
    - Craft a workbench, then Craft a Wooden Pickaxe (in the grid or at a crafting table).
    - Talk to Jerry.
    - Use the teleporter (done by arriving in the Hub).
  - **What follows.** Finishing Getting Started starts Marco the Artist (Talk to Marco). Finding
    the Village starts Time to Strike (Talk to the Bartender).
  - **How the objective shows**, as recorded:
    - A boss bar, yellow and full, reading "Objective: Break a log", 0.4 s after arriving.
    - Two sidebar lines under the purse.
    - NEW OBJECTIVE in chat 2 s after each step, linked to the Quest Log, with the quest's progress
      as its hover.
  - **Which quest is followed.** The one most at home where you are: the island's on the island,
    the Hub's in the Hub, and one that belongs anywhere elsewhere. It's picked again whenever you
    arrive somewhere; a step keeps the quest you follow until that quest is done.
  - **Storage.** Progress is kept on the profile under `quests.<id>` (when it started, how many
    objectives are done, when it was completed).
- **Quest menus**, as recorded:
  - Quests & Chapters, from the SkyBlock Menu's Quests.
  - Chapters (`/chapters`).
  - The Quest Log (`/quests`, `/questlog`): the Fairy Souls, then the quests going, each with its
    progress and when it started (SkyBlock's date and time). Completed Quests appears there once a
    quest is done.
- **Jerry** (`Jerry`). He's on every island, where the starter island has him: a plains farmer
  villager under NEW UPDATE / Jerry / CLICK. The first time you talk to him (or while Getting
  Started is at Talk to Jerry), he says his three lines, timed as recorded, and the objective is
  done with the last one. After that he opens his menu, "Jerry The Assistant", as recorded.
- **The Hub** (`HubMap`, with `server.map: hub`). Players arrive at Hypixel's spawn (0.5 77 -0.5,
  facing the portal home). Two portals work, each under its recorded "Travel to:" sign: the end
  portal north of spawn takes you to your island, and the nether portal south-west of spawn
  (x -36 to -32, z 27) to the Dungeon Hub. Vanilla's portals go nowhere.
- **The Hub's NPCs** (`HubNpcs`): the 49 the recording passed close by, where they stood and facing
  the way they faced, in their skins (the villagers as their kind, the Alchemist a witch), under
  their name tags at the recorded heights, holding what they held. Four say what they said when the
  recording's player talked to them, timed as recorded and in their voices (`HubTalk`): Elizabeth,
  the Community Center's Clerk Seraphine, Minister Nancy and the Auction Master. The others don't
  answer a click yet.
- **The Dungeon Hub** (`DungeonHubMap`, with `server.map: dungeon_hub`). Players arrive where Hypixel
  put them (-28.5 121 0.5, facing west), and Ophelia stands at her recorded spot.
- **Areas** (`Areas`). The first time a profile reaches an area, it gets the area's title (such as
  "Village" over "NEW AREA DISCOVERED!") and its chat lines, as recorded, for the Village,
  Community Center, Election Room, Auction House, Bazaar Alley, Bank, Farm and Dungeon Hub. Found
  areas are kept on the profile under `areas`. The Hub's area boxes (`RegionType`) are drawn around
  those places on the map.
- **Sounds**, as recorded (the replay tool's `[sound]` lines). Hypixel's pitches come in 63rds
  (0.492 is 31/63), so they're used as the packets had them.
  - NEW OBJECTIVE: a note block's pling, pitch 0.49. Skill XP: an orb's pickup, volume 0.5, pitch
    1.7 to 2.5. COLLECTION UNLOCKED: a level-up, pitch 2. A found area: the challenge toast's
    fanfare, volume 0.3, pitch 1.49.
  - NPC lines (`Voice`): Jerry's villager yes or hum, Ophelia's haggling (pitch 1.7 to 1.9, also as
    her shop opens). Jerry's menu opens silently, as recorded.
  - Menus (`MenuSound`): a lever's click as one menu opens another (the SkyBlock Menu from its item
    too), and a page turning as the quest book opens; one opened from the world is silent.
  - Picking up an item: vanilla's pop, which the client plays as the pickup's animation arrives
    (every SkyBlock item pickup was silent before).
- **No vanilla progress** (`VanillaProgress`). There are no advancements, and the recipe book learns
  nothing, so "Advancement Made!" and "New Recipe(s) Unlocked!" never pop up (Hypixel shows neither).

## Approximated (UNKNOWN)

- **The Hub's area boxes are rough.** Each is drawn so that every point where the recording
  crossed into or out of an area falls on the correct side. The Hub's other areas (the Mountain,
  the Forest, the Wilderness...) have no boxes yet, so the sidebar says None there.
- **Island time stands at noon.** Hypixel showed midnight on the island and in the Hub at SkyBlock
  night; how its islands' time goes wasn't recorded.
- **Skill XP** is 6 Foraging XP per log of any kind (only Oak was recorded). Nothing else broken
  on the island gives Skill XP.
- **The action bar** says "+6 Foraging (12%)" where Hypixel said "+6 Foraging (6/50)": that's the
  skills' existing display.
- **The area icon** in chat is the classic ⏣; Hypixel's comes from its resource pack.
- **The welcome** is centred by the rule that gives the recorded one its 20 spaces; other name
  lengths weren't recorded.
- **Island saves** write the whole island each time (the starter island's ~1,600 chunks come to
  264 KB zipped).

## Not done (waits for)

- **Saving Up**, the Banker's quest (Talk to the Banker, then Deposit Coins in the Bank; +5 SkyBlock
  XP and +10 Coins). It's recorded, and waits for the Banker and the bank.
- **The Hub's NPCs' words and menus.** The recording didn't talk to most of them, and NPCs it only
  saw from afar came without their name tags, so they're not placed: Marco and the Bartender among
  them, so Marco the Artist and Time to Strike can't be finished. The four that talk say it on every
  click (UNKNOWN whether Hypixel's repeat it); the menus they'd open (Elizabeth's Community Shop, the
  Auction House) aren't here. Nor are the Hub's holograms (election timers, the gem shop, warps),
  apart from the portals' signs.
- **The Dungeon Hub's other NPCs and quest.** Malik, Saul and the others aren't there either, and
  Mort only appears in runs. Finding the Dungeon Hub started a quest at "Talk to Guildford" whose
  name the recording didn't show, and the Farm's quest (Rigby's) is unknown too; neither is here.
- **The way back from the Dungeon Hub.** Hypixel put the returning player at the Hub's Dungeon Hub
  portal (-33.5 72 24.5); here they arrive at the spawn. The only way back is `/hub`.
- **The Community Center's warp** down to the Election Room.
- **Island features:** Jerry's menu items, Island Properties, visits, guests and co-ops (an island
  is only ever on one server, its owner's), and minions (the starter minion stands still).
- **Fairy Souls** (always 0/289), and the Chapters, which have the recorded layout and nothing in
  them.
