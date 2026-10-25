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
  - Collections: tiers reached, through `LevelingSources` (`Dungeons` gives it `Collections.tier`). Boss
    collections' tiers aren't milestones here (UNKNOWN whether Hypixel counts them).
  - Boss Collections: the SkyBlock XP the boss collections' own tiers give ("+15 SkyBlock XP" at Bonzo I; all of
    them add up to the task's 1,015), `Collections.skyBlockXp(profile, true)`.
  - Fairy Souls: 10 for each five given to Tia the Fairy (FAIRY_SOULS.md).
  - Accessory Bag Upgrades: 2 for each of Jacobus's (ISLANDS_QUESTS.md).
  - Accessory Bag: 1 a point of the most Accessory Power the bag has had (`highestAccessoryPower` on the profile,
    as the API's `highest_magical_power`), so taking accessories out takes none back; worked out without dungeons'
    doubling. The wiki says only "per Accessory Power you have"; NEU's profile viewer counts the highest
    (`CoreTaskLevel`), as here.
  - Craft Minions: each minion tier crafted once, its tier's line (Tier I +1 to Tier XII +24), from
    `minions.craftedMinions` ("WHEAT_12", as the API's `crafted_generators`). Nothing crafts minions here yet,
    so only /yoinkprofile brings any.
  - Defeat Kuudra: each tier up to the highest beaten (`crimsonIsle.kuudra.highest`, 1 for Basic to 5 for
    Infernal: a tier opens once the one before it is beaten). Mage and Barbarian Reputation: each title reached
    (Friendly 1,000, Trusted 3,000, Honored 6,000, Hero 12,000, as `FactionTitle` and NEU have them). There's
    no Crimson Isle here, so only /yoinkprofile brings these.
  - Complete Objectives: the SkyBlock XP quests' objectives gave as their reward (`Quests.skyBlockXp`: Saving Up's
    "Talk to the Banker", +5, as recorded; Getting Started's gave none).
  - Museum Donations: each Museum entry's own XP once it counts, donated or counted by a higher tier (see
    MUSEUM.md; 3,646 at most). The recorded donation said "+1 SkyBlock XP (Museum Donations) (96/100)".
  - Every task is capped at what the data says it's worth.
- **Cached per player and profile** (`SkyBlockLevels`): worked out again 0.5 s after something that gives XP (a skill
  level-up in `SkillGains`, a run's rewards in `DungeonRun`, a quest's reward: the action bar's gain came 0.5 s after
  Saving Up's, as recorded), on a profile switch, and every 10 s for everyone (for changes nothing reports, such as
  admin commands). The level-up message comes 2 s after what gave the XP, as in real chat logs. A full profile
  (every skill maxed, Catacombs and classes 50, every floor) takes about 14 µs.
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
  most; on a tie, the later in the menu's order, as recorded (2026-10-25: a Combat level-up's +5 and an objective's
  +5 at once said "+5 SkyBlock XP (Complete Objective) (10/100)"). Complete Objectives is "Complete Objective" there,
  as recorded.
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

- The other XP tasks: Pet Score, crafting minions in play, bank upgrades, fast travel, events, essence
  shops, Slayer, Bestiary, HOTM/HOTF, the Garden, Rift, consumables... each when its system exists (a
  `LevelingSources` method, or a task id in `SkyBlockXp.of`).
- Level rankings (Hypixel's Global Ranking lines) need a ranking across profiles.
- Feature rewards (Community Shop, Garden, Wardrobe, Bazaar, Auction limits...) and the Book of Progression's
  bonuses are shown, not given: their systems don't exist.
- The guide's detail menus and the Show Progress Bars view, once recorded.
