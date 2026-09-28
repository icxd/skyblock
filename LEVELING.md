# SkyBlock Leveling

SkyBlock XP, levels and what they give, as on Hypixel. The code is in `paper/src/main/java/net/icxd/dungeons/leveling`;
the data (Hypixel's tasks, rewards, emblems and guide) is private, in the data repository's `leveling/leveling.json`,
made by `tools/leveling/build_leveling.py` (its README has the format and sources). servermgr links that folder into
every server as `plugins/dungeons/leveling`; without it everyone is level 0 and the log says why.

Sources: the SkyBlock Menu tour recording (`rec3`: SkyBlock Leveling at level 88 and at 0, Level 88 and 90 Rewards,
Ways to Level Up, Guide ➜ Starter and Amateur, Leveling Rewards and its four lists, Emblems; the tab list), the wiki
(SkyBlock Levels, its Tasks and UI pages, SkyBlock Guide), real chat logs (SkyHanni's, for the level-up message and
the chat prefix), NEU's `sblevels.json` (checked against, older) and the swofty remake (the emblem list's title).
Where they say nothing the code says UNKNOWN and takes the simplest reading.

## The model

- **SkyBlock XP is worked out from what the profile has done** (`SkyBlockXp.of`), never counted up: each task's XP
  from the data and what the plugin keeps. 100 XP a level (the wiki's SkyBlock Levels). Counted now:
  - Skill Level Up: every level of the non-cosmetic skills (5 to 10, 10 to 25, 20 to 50, 30 to 60).
  - Catacombs Level Up (20 a level to 39, 40 to 50, none past), Class Level Up (4 a level, each class).
  - Complete Dungeons: each floor's first completion (Entrance to VII: 20/20/20/20/20/30/30/30, Master: 50).
  - Collections: tiers reached, through `LevelingSources` (none until the collections code is wired in:
    `SkyBlockLevels.sources(...)` with `collectionTiers` and `collectionTier` from `Collections.tier(...)`).
  - Every task is capped at what the data says it's worth.
- **Cached per player and profile** (`SkyBlockLevels`): worked out again 2 s after something that gives XP (a skill
  level-up in `SkillGains`, a run's rewards in `DungeonRun`: the level-up message comes 2 s after, as in real chat
  logs), on a profile switch, and every 10 s for everyone (for changes nothing reports, such as admin commands). A
  full profile (every skill maxed, Catacombs and classes 50, every floor) takes about 14 µs.
- **The old field**: `User.getSkyBlockXp()` was only the skill levels' part and was never saved; it now reads the
  model, and every reader (the SkyBlock Menu, the dungeon tab list) goes through it or `SkyBlockXp`. The old number
  is the Skill Level Up task's part of the new (tested).

## Levels

- **Stats**: +5 Health every level and +1 Strength every 5th (the recorded Stat Rewards item; level 88's Health
  breakdown is +440), through `PlayerStats.addModifier`, everywhere including dungeons.
- **Level-up**: the chat as real logs have it (blank, centred "SKYBLOCK LEVEL UP", "Level 170 ➡ [171]" in the new
  level's colour, REWARDS and what the levels gave, blank; a jump of several levels is one message), and a sound
  (UNKNOWN: a skill's). The profile keeps the last level it was told of (`leveling.level`); a profile without one is
  told nothing the first time, so progress from before this existed isn't announced.
- **Action bar**: "+20 SkyBlock XP (Skill Level Up) (34/100)" in place of Defense for 2 s, for the task that gave the
  most (UNKNOWN how Hypixel shows two).
- **Colours**: every 40 levels, gray to dark red at 480 (the recorded Prefix Color rewards, NEU's `sblevel_colours`).
- **Chat**: "[88] ♦ [MVP+] Name: text" (a real chat log's order), for viewers with SkyBlock Levels in Chat on (the
  switch in SkyBlock Leveling, saved on the account as `settings.levelsInChat`).
- **Tab list**: "[88] Name" in the players column (recorded; the emblem after the name is UNKNOWN), and " SB Level:
  [88] 34/100 XP" under Profile (recorded).
- **Emblems**: chosen per profile (`leveling.emblem`; UNKNOWN whether Hypixel keeps it per account); shown only
  while unlocked. Unlocks the plugin can tell: SkyBlock level, skill levels, Catacombs, classes, class average,
  floor completions. Slayer, achievement, MVP++ and Discord emblems stay locked.

## Menus

| Menu | Status | Notes |
|---|---|---|
| SkyBlock Leveling (`/levels`, `/skyblocklevels`, `/sblevels`, `/level`...; SkyBlock Menu slot 22) | DONE | As recorded, the player's own numbers. No level rankings here: the ranking item says what Hypixel's says before its rank loads. Its "completed x% of the total" uses the categories' 57,874 XP; the recorded 15.2% at 8,834 needs more (UNKNOWN what: the recorded categories' XP also add up to only 8,025 of the 8,834, so Hypixel counts something outside them). |
| Level N Rewards | DONE | As recorded for one and three rewards (every other slot); more than five, only level 50's six, side by side (UNKNOWN). |
| Leveling Rewards, and its Rewards lists | DONE | As recorded (features, prefix colours with a preview of their own name, emblems with Prefix Emblems, bonuses sized at their level). |
| Ways to Level Up | DONE | As recorded, with Recently Viewed (while they're on the server: UNKNOWN whether Hypixel keeps it). Show Progress Bars: the item as recorded, does nothing (LATER: never recorded on). |
| Tasks ➜ Category, Category ➜ Task | APPROX | Never recorded: the wiki's copies (older), with the recorded task lists: a border of glass in the category's colour (Skill Related's and Consumables' are UNKNOWN: black), Sort in a category's list (Unlocked keeps the data's order), none in a task's parts, which keep their order. A part done once (a bank upgrade, a floor) says it can only be done once, as the wiki's, but its wording (the XP alone), COMPLETED on a done one, layouts past what the wiki shows, the done part's tick and Skill Related's 28 in one list (the wiki's older menu split them by skill) are UNKNOWN. |
| Guide ➜ Stage (`/skyblockxp`) | APPROX | Starter and Amateur as recorded (their frame of glass in the stage's colour, tasks, order, descriptions, parts); the other stages from the wiki (older), their glass in their colour (UNKNOWN). A stage opens at half the one before it done; undone tasks first. Past 21 tasks (five stages have up to 35) they take pages, "(1/2) Guide ➜ Skilled" with arrows as Hypixel's paged menus (UNKNOWN for the guide). "Click to view more!" opens nothing (LATER: never recorded). |
| Emblems | DONE | As recorded. |
| Emblems - Kind (n/m) | APPROX | Never recorded: the swofty remake's title; the items, pages and choosing message are UNKNOWN. |

## Later, and what it waits for

- The other XP tasks: Museum, Fairy Souls, Accessory Bag and its upgrades, Pet Score, minions, bank upgrades, fast
  travel, events, essence shops, Slayer, Bestiary, Kuudra, HOTM/HOTF, the Garden, Rift, consumables... each when
  its system exists (a `LevelingSources` method, or a task id in `SkyBlockXp.of`).
- Collections: wire `LevelingSources` to the collections code (the guide's collection parts count then too).
- Level rankings (Hypixel's Global Ranking lines) need a ranking across profiles.
- Feature rewards (Community Shop, Garden, Wardrobe, Bazaar, Auction limits...) and the Book of Progression's
  bonuses are shown, not given: their systems don't exist.
- The guide's detail menus and the Show Progress Bars view, once recorded.
