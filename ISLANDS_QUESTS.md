# Private islands, quests and the Hub

A new profile's first minutes, as Hypixel has them: its private island, Getting Started and the
quests after it, Jerry, the portal to the Hub on Hypixel's map, the areas found there, and the Hub's
first quests. Built from the recordings of a new profile's first ten minutes (2026-10-24) and of
Marco's and the Bartender's quests (2026-10-25), decoded with `tools/replay` (chat, titles, boss bar,
sidebar, menus, NPCs, holograms, block changes and the player's path), and the dungeon scanner's
captures of the island, the Hub and the Dungeon Hub (`/dscan world`). The code is in
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
    the Village starts the Hub's quests: Marco the Artist (if it hasn't started), Time to Strike and
    Saving Up. The Quest Log had all of the Hub's quests started at the minute the Village was found.
  - **How the objective shows**, as recorded:
    - A boss bar, yellow and full, reading "Objective: Break a log", 0.4 s after arriving.
    - Two sidebar lines under the purse.
    - NEW OBJECTIVE in chat 2 s after each step, linked to the Quest Log, with the quest's progress
      as its hover. An objective that's already reached isn't announced ("Reach Combat Level I").
    - An objective that counts shows its count on the bar, which fills with it ("Kill Zombies
      (3/10)"), and under it on the sidebar. A skill's XP shows as "(150/50 XP)". One that wants
      items lists them on the sidebar ("Rose Red x5"). The Quest Log adds the counts ("Kill Zombies.
      (10/10)"), a skill's XP as it is now.
  - **Which quest is followed.** The one most at home where you are: an area's own (Marco's in the
    Flower House, Saving Up's deposit in the Bank), the island's on the island, the Hub's in the
    Hub, and one that belongs anywhere elsewhere (Marco's). An objective can have its own home:
    Saving Up's "Talk to the Banker" is followed nowhere, even in the Bank. The quest is picked again
    whenever you arrive somewhere; a step keeps the quest you follow until that quest is done.
  - **Storage.** Progress is kept on the profile under `quests.<id>` (when it started, how many
    objectives are done, what the current one has counted, when it was completed).
- **Quest menus**, as recorded:
  - Quests & Chapters, from the SkyBlock Menu's Quests.
  - Chapters (`/chapters`).
  - The Quest Log (`/quests`, `/questlog`): the Fairy Souls (FAIRY_SOULS.md), then the quests going, each with its
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
- **The Hub's NPCs** (`HubNpcs`): the ones the recordings passed close by (82 in the three so far),
  where they stood and facing the way they faced, in their skins (the villagers as their kind, the Alchemist a witch), under
  their name tags at the recorded heights, holding what they held. Four say what they said when the
  recording's player talked to them, timed as recorded and in their voices (`HubTalk`): Elizabeth,
  the Community Center's Clerk Seraphine, Minister Nancy and the Auction Master. The Banker has
  his bank, the Farm Merchant his shop, and Marco and the Bartender their quests (below). The others
  don't answer a click yet.
- **Saving Up and the Bank** (`Banker`, `bank/`), as recorded.
  - **The Banker.** Saving Up starts with the Village. While it's at Talk to the Banker, he says
    his three lines, timed as recorded, in a villager's voice (again if you left before the last).
    With the last, Talk to the Banker is done: OBJECTIVE COMPLETE and +5 SkyBlock XP, which SkyBlock
    Leveling counts as Complete Objectives ("+5 SkyBlock XP (Complete Objective)" on the action bar
    0.5 s later). The boss bar stays where it was. After that he opens the Personal Bank Account,
    speaking as it opens. A profile that found the Village before Saving Up started with it starts
    it with his last line.
  - **The menus.** The Personal Bank Account has deposit, withdraw, the last 10 transactions and
    the information, whose countdown to interest ticks every second. Bank Deposit takes the whole
    purse, half of it, or an amount typed on the recorded sign. Bank Withdrawal takes everything,
    half, 20%, or a typed amount.
  - **Amounts.** Coins go in as they are and come out whole, so 20.5 deposited leaves 0.5 after
    "Everything". The menus show a tenth (half a 20.5 purse is "10.3"); chat and the transactions
    show whole coins ("20 coins", "1 coin").
  - **How it goes.** Each menu opens the next with a click. A deposit or withdrawal says
    "Depositing coins...", chimes, says what it did and opens the account again. An empty purse
    gets "You can't deposit this little!", silently, and the menu stays open.
  - **Saving Up's end.** The first deposit completes it: QUEST COMPLETE and +10 Coins into the
    purse (its hover says so).
  - **The tab list** has "Interest: 4 Hours" under the bank's balance.
  - **Storage.** The profile's `bank`: its `balance` and its last 10 `transactions`.
- **The Farm Merchant** (`FarmMerchant`, `Shop.FARM_MERCHANT`), as recorded. The first time, his two
  lines, high-voiced; his shop opens 2.1 s after the last, as Ophelia's does. After that a click
  opens it, and he speaks as it opens.
  - **His wares**, in the recorded order and at the recorded prices: Wheat, Carrot and Potato (3 for
    30 coins), Melon Slice (10 for 40), Sugar Cane, Pumpkin, Cocoa Beans, the two mushrooms, Sand,
    Cactus, Enchanted Bone Meal, the Rookie Hoe and Rookie Farming Axe, and the Beginner Agronomy Sack.
  - **Stock** (`ShopStock`): the crops and the like have 640 a day, counted in items and shown as
    "Stock: 640 remaining". The day starts at 00:00 UTC, as the wiki says and the sell limit's does.
  - **Locks.** The Rookie tools are "Not unlocked!" until Farming Skill 1, which the items require.
    A ware now shows its item's unmet requirement ("Requires Farming Skill 1"), as recorded.
  - **Every shop** (Ophelia's too) now has the recorded sale line ("You sold Cobblestone x20 for 20
    Coins!"), the coins' chime with each sale, and "Right-click for more trading options!" on
    wares that stack.
- **Marco the Artist** (`Marco`), as recorded.
  - **Talk to Marco.** His four lines ("Hey there!" to "Could you pick 5 Poppies and turn them into
    Rose Red Dye...?"), timed as recorded; the objective is done with the last.
  - **Give Rose Red Dye to Marco.** The sidebar lists "Rose Red x5". A Poppy crafts into a Rose
    Red. With 5, a click on him has his five thanks; with the last he takes the dye, Claim Reward
    opens with the Spray Can, and QUEST COMPLETE says "REWARD / Spray Can" (its line opens the menu
    again). Then the boss bar goes to the Bartender, with NEW OBJECTIVE 2 s later.
- **The flower beds** (`FlowerGarden`). The flowers in the two beds by the Flower House (x 80 to
  91, z -4 to 22) can be picked, as recorded; nothing else in the Hub breaks (the grass between
  them didn't). A flower drops as its item to pick up and gives +1 Foraging XP. It grows back as
  itself: every 5 seconds, each picked one has a 1 in 2 chance (below). The beds have 6 Poppies.
- **Time to Strike** (`Bartender`, `Graveyard`), as recorded.
  - **Talk to the Bartender.** His four lines, timed as recorded. With the last, Claim Reward opens
    with the Undead Sword, and OBJECTIVE COMPLETE says "REWARD / Undead Sword".
  - **Kill Zombies (0/10).** Graveyard Zombies and Zombie Villagers both count, and the bar fills
    by a tenth a kill. The tenth kill's Combat XP comes first (Combat I's SKILL LEVEL UP, with its
    rewards), then OBJECTIVE COMPLETE with REWARDS: +5 SkyBlock XP and +100 Combat Experience.
  - **Talk to the Bartender again.** His four thanks, with your name in your rank's colour, then
    +100 Coins.
  - **Reach Combat Level I (150/50 XP).** It's reached already, so NEW OBJECTIVE doesn't say it,
    and it's done without a word as you next arrive in an area. Then Travel to the Spider's Den,
    which isn't here, so the quest stops there.
- **Claim Reward** (`ClaimMenu`, `Claims`, `/claimitem`), as recorded. Six rows of glass, the item in
  the middle with "Click to claim!" under its lore, and Close. It opens with a lever's click as the
  item is given. A click puts the item in your inventory, says "You claimed an Undead Sword!" (or
  "a Spray Can") with the coins' chime, and closes the menu. Until then the reward waits on the
  profile (`claims`), and the reward's chat line opens the menu again.
- **The Graveyard** (`Graveyard`, `MobKinds`). Graveyard Zombies at the 59 spots where the
  recorded ones first appeared, and at night (8pm to 6am) Zombie Villagers at their 12. One more
  of each comes every 2 seconds while there are fewer than 20 zombies and 10 villagers, as
  recorded. A villager left at 6am stays until it's killed; none come by day. Their name tags are
  as recorded ("[Lv1] ༕ Graveyard Zombie 100/100❤", the Undead symbol in its classic form).
- **The Dungeon Hub** (`DungeonHubMap`, with `server.map: dungeon_hub`). Players arrive where Hypixel
  put them (-28.5 121 0.5, facing west), and Ophelia stands at her recorded spot.
- **Areas** (`Areas`). The first time a profile reaches an area, it gets the area's title (such as
  "Village" over "NEW AREA DISCOVERED!") and its chat lines, as recorded, for the Village,
  Community Center, Election Room, Auction House, Bazaar Alley, Bank, Farm, Flower House, Combat
  Settlement, Graveyard and Dungeon Hub. A line too long for chat goes on under itself, as recorded.
  Found areas are kept on the profile under `areas`. The Hub's area boxes (`RegionType`) are drawn
  around those places on the map. The Village has two boxes, which count as one area.
- **Sounds**, as recorded (the replay tool's `[sound]` lines). Hypixel's pitches come in 63rds
  (0.492 is 31/63), so they're used as the packets had them.
  - NEW OBJECTIVE: a note block's pling, pitch 0.49. Skill XP: an orb's pickup, volume 0.5, pitch
    1.7 to 2.5. COLLECTION UNLOCKED: a level-up, pitch 2. A found area: the challenge toast's
    fanfare, volume 0.3, pitch 1.49.
  - NPC lines (`Voice`): Jerry's and the Banker's villager yes or hum, Ophelia's haggling (pitch 1.7
    to 1.9, also as her shop opens). Jerry's menu opens silently, as recorded; the Banker speaks as
    his opens.
  - OBJECTIVE COMPLETE and QUEST COMPLETE: a firework's launch, with a level-up when it gives
    SkyBlock XP. The bank's deposits and withdrawals: a pling at volume 8 and the top pitch.
  - Menus (`MenuSound`): a lever's click as one menu opens another (the SkyBlock Menu from its item
    too, and Claim Reward as a reward gives its item), and a page turning as the quest book opens;
    one opened from the world is silent. Claiming a reward chimes as coins do.
  - Picking up an item: vanilla's pop, which the client plays as the pickup's animation arrives
    (every SkyBlock item pickup was silent before).
- **Fixed along the way.**
  - **Dying keeps your items** on every server's main world. A death costs coins, as before;
    islands and runs already kept items.
  - **Mobs don't burn by day.** Since 26.2 a zombie's or skeleton's own setting no longer stops it,
    so `Mobs` refuses daylight fire for SkyBlock mobs. A hit's or a block's fire still burns.
  - **"RARE DROP!"** only for rare drops, from every mob (the Graveyard's Rotten Flesh and Poisonous
    Potato had no line, its 1% Potato had one). The line is the recorded "RARE DROP! Potato (+7 ✯
    Magic Find)", without the "%".
  - **Block drops start from the block's middle**, as vanilla's do (on islands too). From its
    corner they started inside the ground and were pushed out at random.
  - **The SkyBlock XP action bar** names the later task when two gave the same at once. The
    recorded Combat level-up and objective (+5 each) said "(Complete Objective) (10/100)".
- **No vanilla progress** (`VanillaProgress`). There are no advancements, and the recipe book learns
  nothing, so "Advancement Made!" and "New Recipe(s) Unlocked!" never pop up (Hypixel shows neither).
- **The sidebar's arrow** (`QuestText.arrow`). The "Objective" line has an arrow toward the
  objective's place while you're on its island: "Objective ⬉". It's bold yellow, and bold green
  within 50 blocks. It's one of eight, the nearest to the way from where you face, and it shows again
  each second with the sidebar.
  - **How it was read.** Fitted to about 300 recorded arrows (2026-10-24 and 10-25) against where the
    player stood and faced: their entity's look packets, read with `tools/replay`'s decoder.
  - **Which objectives.** Here only Time to Strike's have one: the Bartender (both times), the
    Graveyard (Kill Zombies) and the Spider's Den. None of Getting Started's, Marco's, Saving Up's or
    the Librarian's showed one. Farmer Rigby's did, and his quest isn't here.
- **Deaths** (`combat/Deaths`) are as on Hypixel: no death screen and no respawn. The death is called
  off, as a dungeon run's is.
  - **Where you come back.** On the next tick you're at the island's spawn: the Hub's, the Dungeon
    Hub's or the island's ("Upon dying, the Player is returned to the island's spawn", the wiki's
    Death). You keep your items and come back with full health and half your mana. Fire, the fall and
    an open menu don't carry over.
  - **What chat says.** You see "☠ You were killed by Graveyard Zombie.", and the others in your world
    see "☠ Tester was killed by Graveyard Zombie." (the shape of SkyHanni's `PlayerDeathManager`
    pattern). The other reasons are "☠ You fell into the void." and the dungeon reasons without "and
    became a ghost", such as "☠ You burnt to death.". The coins line follows.
  - **The void** kills at once. Before, vanilla's void damage only chipped at it (about 50 a second
    off 1,223 health in the test).
  - **Coins.** A fall into the void costs no coins only on a private island without a mob's hit ("Falls
    in the void on a Private Island or The Garden, without being hit by a mob", the wiki's Coins).
    Before, it cost nothing anywhere.
  - **Tested live:** `/kill`, the void on an island and off one, and the Graveyard's zombies.
  - A vanilla death that still happens, in a run's world before or after it runs, respawns at once
    with no vanilla death line.

## Approximated (UNKNOWN)

- **The arrow's targets.**
  - **The Bartender's** is his NPC: the fit lands on him, and his arrows turned green at 50 blocks to
    within a few tenths of a block.
  - **Kill Zombies'** (-104, -122) and **the Spider's Den's** (-144, -188) are where their arrows fit
    best, not known points. Kill Zombies' is the Graveyard's north-east, not its middle (20 of 21
    arrows fit). No recording went to the Spider's Den (all 187 arrows fit, colour too).
  - **The fit.** The arrow's sector fits the bearing within the second's turning (about 23° rms).
- **Deaths.**
  - **The wording.** Only the kill line's shape is known (SkyHanni's pattern). The other reasons are
    the dungeon ones without "and became a ghost", plus "You fell into the void" (the old wiki's
    Death). A fall is "You died", or for the others "fell to their death with help from" a mob that
    hit them in the 5 seconds before.
  - **The killer's colour.** It's the red of its name tag. SkyHanni's example has Ashfang in dark
    grey bold, so a mob's own colour may differ.
  - **The coins line** ("You died and lost N coins!") is unknown and comes after the death line.
  - **No sound** plays but the hit's.
  - **The void.** Hypixel's 1.8 took 4 health a tick below y -64; whether SkyBlock kills at once is
    its own rule.

- **The Hub's area boxes are rough.** Each is drawn so that every point where the recordings
  crossed into or out of an area falls on the correct side, give or take the block or two Hypixel's
  notice lags behind (the Graveyard was entered at z -97.5 and left at z -93). Its box reaches
  z -94 so that Pat stands in it. The Hub's other areas (the Mountain, the Forest, the Wilderness,
  the Library, the Thaumaturgist, Pet Care...) have no boxes yet, so the sidebar says None or the
  area round them there.
- **The flowers' regrowth.** The six recorded ones grew back on a 5-second beat, on its 1st, 2nd,
  1st, 2nd, 3rd and 4th beat after they were picked, so each has a chance every beat. Hypixel's
  chance is UNKNOWN; 1 in 2 fits.
- **The Graveyard's mobs** appear only where the map is loaded (a player is nearby), at a random
  one of their spots, so an empty Hub has none. Hypixel always has its Graveyard full. Their
  damage, coins, orbs and drops are the wiki's; Combat XP (5), level and health are as recorded.
  That both kinds count for Kill Zombies is the wiki's (no villager was killed in the recording).
- **What Marco and the Bartender say between steps** is this plugin's: Marco asks again if you
  come without 5 Rose Red and says his last thanks once it's done; the Bartender asks for help
  again while you kill zombies, then gives his last advice. Marco takes the dye with his last
  line (Hypixel took it with his first), so nothing is lost if you leave while he talks.
- **Claim Reward's other words** are this plugin's: "You don't have that to claim!" and "Your
  inventory is full!".
- **The reward's Combat XP** (+100) is given as it is, without Combat Wisdom.
- **Island time stands at noon.** Hypixel showed midnight on the island and in the Hub at SkyBlock
  night; how its islands' time goes wasn't recorded.
- **Skill XP** is 6 Foraging XP per log of any kind (only Oak was recorded). Nothing else broken
  on the island gives Skill XP.
- **The action bar** says "+6 Foraging (12%)" where Hypixel said "+6 Foraging (6/50)": that's the
  skills' existing display.
- **The area icon** in chat is the classic ⏣; Hypixel's comes from its resource pack.
- **The bank's other words** are this plugin's: "You can't withdraw this little!", "You don't have
  that many coins in your purse!" (or "in the bank!"), "Your bank account is full!" and "That's not
  a valid amount!". So are these:
  - The countdown under an hour ("59 Minutes").
  - Transactions older than a minute ("3 mins ago", "2 hours ago").
  - What the sign takes (k, m and b work).
  - A deposit past the 50 million limit (what fits goes in).
- **"More trading options"** (buying several at once) weren't recorded, so a right-click on a ware
  buys one trade, as a left-click does. When a ware's stock runs out, "This item is out of stock for
  today!" is this plugin's line. The wiki says the Farm Merchant shares stock with the Windmill
  Operator; here each shop's stock is its own (he isn't here).
- **The bank's Go Back** clicks, as menus do. The recording's one return to the account came with
  the Banker's voice, but that was most likely a click on him after closing the menu (a client's
  close isn't in a recording).
- **The welcome** is centred by the rule that gives the recorded one its 20 spaces; other name
  lengths weren't recorded.
- **Island saves** write the whole island each time (the starter island's ~1,600 chunks come to
  264 KB zipped).

## Not done (waits for)

- **The bank's interest.** The countdown is right (it matched the recording's to the second, once
  the recording PC's clock is allowed for), but nothing is paid. No recording has a season
  starting, so its message and transaction line are unknown; the rates are in the menu (2% to 10M,
  1% to 15M). Also missing: the vault, account upgrades, the SkyBlock Menu's Personal Bank (locked
  until Emerald Collection VI on a new profile) and co-op members' names on transactions.
- **The Hub's NPCs' words and menus.** The recordings didn't talk to most of them, and NPCs they
  only saw from afar came without their name tags, so they're not placed. The four that talk say it
  on every click (UNKNOWN whether Hypixel's repeat it); the menus they'd open (Elizabeth's Community
  Shop, the Auction House) aren't here. Nor are the Hub's holograms (election timers, the gem shop,
  warps), apart from the portals' signs. Marco and the Bartender need `npcs/hub.json` made again
  from all three recordings (with `setup`'s tables, see the replay tool's README).
- **The rest of Time to Strike and the Hub's quests.** The Spider's Den isn't here, so "Travel to
  the Spider's Den" is where Time to Strike stops. The Hub's other quests (Carpentry, the Foraging
  and Fishing tutorials, Safety First, First Harvest, the Auctioneer's, Hub Explorer, Time to Mine,
  Library Card) aren't here either.
- **The Spray Can** is only an item: its menu (the Ink Manager and Spray Selector) and spraying
  your island's walls aren't here, nor is Marco's Canvas Room.
- **The Dungeon Hub's other NPCs and quest.** Malik, Saul and the others aren't there either, and
  Mort only appears in runs. Finding the Dungeon Hub started a quest at "Talk to Guildford" whose
  name the recording didn't show, and the Farm's quest (Rigby's) is unknown too; neither is here.
- **The way back from the Dungeon Hub.** Hypixel put the returning player at the Hub's Dungeon Hub
  portal (-33.5 72 24.5); here they arrive at the spawn. The only way back is `/hub`.
- **The Community Center's warp** down to the Election Room.
- **Island features:** Jerry's menu items, Island Properties, visits, guests and co-ops (an island
  is only ever on one server, its owner's), and minions (the starter minion stands still).
- **The Chapters**, which have the recorded layout and nothing in them.
