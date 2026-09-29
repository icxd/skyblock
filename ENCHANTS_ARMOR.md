# Armor and equipment enchantments

What the enchantments on helmets, chestplates, leggings, boots and equipment (necklaces, cloaks, belts, gloves,
bracelets) do past their text's plain stats, their ultimates, and the armor and equipment reforges' bonuses (task
80's armor part, on the hooks in EFFECTS.md). The code is `paper/src/main/java/net/icxd/dungeons/item/enchanting/armor`
(`ArmorEnchants` registers everything) and `reforge/ArmorReforgeBonuses`. The rules are pure functions with tests
(`ArmorEnchantsTest`, `ArmorTextsTest` on the private table, `ArmorReforgeBonusesTest`, `RunBoostsTest`, and
`EnchantmentDataTest`'s new cases).

## How it works

- **What's worn** (`WornEnchants`): the pieces `SetBonuses.worn` has (the armor slots' armor, helmet first, then
  the equipment), each with its enchantments by the plugin's id (`legion`, not `ultimate_legion`), its reforge and
  its rarity, worked out at most once a tick. Armor held in the hand never counts.
- **Each piece is its own.** An enchantment on four pieces does its thing four times, each with its own cap: the
  wiki's Refrigerate says "up to 150 Defense per armor piece, up to a maximum of 600 (4 armor pieces)", and its Last
  Stand "will stack across multiple armor pieces". The same is taken for the rest (UNKNOWN where no page says).
- **Numbers are the text's** (`EnchantNumbers`): every number in a level's text, in order, colour codes left out
  (Thorns' "50% chance to rebound 12%" is 50 and 12). So what an item does is what its lore says, whichever
  source's number the table took (Transylvanian V says 4 and 40 in the table, 3 and 30 on the wiki). A level with
  no text (Thorns V, Feather Falling XX, which a few live items have past the books) does nothing.
  `ArmorTextsTest` checks every level's text has the numbers where the code reads them.
- **Plain stats** stay the items' own (`ItemStats`, through `EnchantmentType.getStats`), so lore, the Stats menu
  and combat agree. Two more texts read now: Small Brain's "-15 Intelligence and +3 True Defense" (a minus), and
  Reflection's two "Grants" sentences before its arrow effect. A Turbo's "Grants .... Requires Bronze in a ...
  Contest!" still grants nothing.
- **Stats that depend on something** (`StatEnchants`) are a `PlayerStats` modifier after the set bonuses': the
  flat ones first, then the shares more (Legion with Renowned and Perfect).

### Hooks this part added (small, self-contained)

| Hook | Where | For |
|---|---|---|
| `Heals.addKindFactor((healer, kind) -> factor)`: a factor on a kind of heal over time a healer casts | `item/ability/utility/Heals` | Habanero Tactics' "more from wands" |
| `RunBoosts.addBoost((player, score) -> Boost(catacombs%, classes%))` and `addRewardedListener((player, floor, score, failed) -> ...)`: a member's own boost to a run's experience, and hearing that they got it | `dungeons/instance/RunBoosts`, called from `DungeonRun.rewardAndSummarize`; `RunEnd.award`/`awardFailed` take the boost | Hecatomb's XP and its tiers |
| `FleeGoal.setUnnoticed((mob, player) -> stays)`: asked once each time a player comes near a mob that would flee | `mob/goals/FleeGoal` | Stealth |
| `EnchantmentType`'s grants: a minus, "&7 and", and two "Grants" sentences | `item/enchanting/EnchantmentType` | Small Brain, Reflection |

### For the other parts (wiring after the merge)

- **Old Blood** (Old Dragon Armor): its Feather Falling part is here (`Protections`, the wiki's "+3% Fall damage
  reduction Per Level"). BONUSES.md's Old Blood row and `DragonSets`' comment ("Not here: ... Old Blood's Feather
  Falling") can point here.
- **Hyper, Loving and Empowered** are armor reforges, but EFFECTS.md gives them to the weapon part, and the cloaks'
  **Blood-Soaked** is theirs too (it raises their Life Steal, Vampirism and Drain a level): ENCHANTS_WEAPONS.md.
- **Hecatomb's count**: the stats part's `StackingEnchants` (the held items' stacking enchantments, their count in
  lore and a tier-up message) keys Hecatomb's count as `hecatomb_s_runs` too, as `Hecatomb` here writes it on the
  worn helmet. After the merge the helmet's count and tier-up could go through it.

## Status

DONE: as the text and wiki say. APPROX: done, with a reading marked UNKNOWN in the code (see below). LATER: waits
for the system named. Every enchantment the table applies to armor or equipment is here.

### Enchantments

| Enchantment (id) | On | Status | What it does here | Sources |
|---|---|---|---|---|
| Thorns (`thorns`) | armor | **APPROX** | Each piece rolls its chance; what they rebound of what the hit took (after Defense) hits the mob behind it (an arrow's shooter) as an effect's damage (`MobHits.deal`, its kill theirs) | text; [Thorns](https://hypixelskyblock.minecraft.wiki/w/Thorns) |
| Reflection (`reflection`) | chestplate | **APPROX** | Its Intelligence and True Defense are its own stats; a mob's arrow's hit deals the text's "Nx your Intelligence" to the shooter | text; [Reflection](https://hypixelskyblock.minecraft.wiki/w/Reflection) |
| Counter-Strike (`counter_strike`) | chestplate | **APPROX** | Each mob's first hit on them gives the Defense for its seconds (a new one takes the last one's place) | text; [Counter-Strike](https://hypixelskyblock.minecraft.wiki/w/Counter-Strike) |
| Projectile Protection (`projectile_protection`) | armor | **APPROX** | Defense against a mob's projectile's hit; a vanilla projectile's damage lessened as that Defense would | text |
| Blast Protection (`blast_protection`) | armor | **APPROX** | Vanilla explosions' damage lessened as that Defense would (100 / (100 + it)); SkyBlock has no explosion hits here but the Undead Flamer's, which ignores Defense | text; [Blast Protection](https://hypixelskyblock.minecraft.wiki/w/Blast_Protection) |
| Fire Protection (`fire_protection`) | armor | **APPROX** | Vanilla fire and lava damage lessened as that True Defense would | text; [Fire Protection](https://hypixelskyblock.minecraft.wiki/w/Fire_Protection) |
| Feather Falling (`feather_falling`) | boots | **DONE** | That many more blocks of safe fall (the SAFE_FALL_DISTANCE attribute), and fall damage cut by its percent; Old Blood adds 3% a level | text; [Old Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Old_Dragon_Armor) |
| Depth Strider (`depth_strider`) | boots | **DONE** | Its percent as water movement efficiency (100% is vanilla's Depth Strider III) | text |
| Frost Walker (`frost_walker`) | boots | **APPROX** | Walking on something solid, still water with air above within its radius turns to frosted ice, water again 5 s after they've left it; not in dungeon runs. Hypixel removed it in 0.22 (question below) | text; [Frost Walker](https://hypixelskyblock.minecraft.wiki/w/Frost_Walker) |
| Stealth (`stealth`) | boots | **APPROX** | A Timid mob (the Scared Skeleton) rolls it once each time they come near; if it comes up, it doesn't flee from them, with the wiki's message | text; [Stealth](https://hypixelskyblock.minecraft.wiki/w/Stealth) |
| Respite (`respite`) | armor | **APPROX** | Its Health Regen while out of combat (`CombatState`: 5 s, the core's UNKNOWN) | text; [Respite](https://hypixelskyblock.minecraft.wiki/w/Respite) |
| Transylvanian (`transylvanian`) | helmet | **APPROX** | A heal of their own each second: the text's amount per SkyBlock mob within its blocks, up to its most | text; [Transylvanian](https://hypixelskyblock.minecraft.wiki/w/Transylvanian) |
| Hardened Vitality (`hardened_mana`) | armor | **APPROX** | Its share of Vitality spent as Defense for its seconds, up to its max, per piece; spending again adds to it and starts its time again | text; [Hardened Vitality](https://hypixelskyblock.minecraft.wiki/w/Hardened_Vitality) |
| Strong Vitality (`strong_mana`) | armor | **APPROX** | The same, as Strength | text; [Strong Vitality](https://hypixelskyblock.minecraft.wiki/w/Strong_Vitality) |
| Vivacious Vitality (`ferocious_mana`) | armor | **APPROX** | The same, as Attack Speed | text; [Vivacious Vitality](https://hypixelskyblock.minecraft.wiki/w/Vivacious_Vitality) |
| Vampiric Vitality (`mana_vampire`) | armor | **APPROX** | Heals the text's ❤ per Vitality spent, a heal of their own (the Catacombs boost counts, as for other heals) | text; [Vampiric Vitality](https://hypixelskyblock.minecraft.wiki/w/Vampiric_Vitality) |
| Small Brain (`small_brain`) | helmet | **DONE** | Its text's stats, less Intelligence and more True Defense (lore, Stats menu and combat) | text |
| Cayenne (`cayenne`) | equipment | **DONE** | Health and True Defense per digit of their Accessory Power (the wiki: four Cayenne V are +16, +8) | text; [Cayenne](https://hypixelskyblock.minecraft.wiki/w/Cayenne) |
| Quantum (`quantum`) | necklace | **APPROX** | Vitality Monday to Friday; at weekends the text's amount of one Wisdom, the same for everyone, picked for each weekend from its date (New York's days) | text; [Quantum](https://hypixelskyblock.minecraft.wiki/w/Quantum) |
| Hecatomb (`hecatomb`) | helmet | **APPROX** | Its Catacombs and class XP percents on a run's experience, doubled on S+; Health per 10 Catacombs levels (the stat level, at most 50); a completed S or S+ run counts on the helmet (`hecatomb_s_runs`) and it tiers up at each level's "N S runs to tier up!" | text; [Hecatomb](https://hypixelskyblock.minecraft.wiki/w/Hecatomb) |
| Growth, Protection, True Protection, Sugar Rush, Rejuvenate, Respiration, Big Brain, Smarty Pants, Prosperity | armor, equipment | **DONE** | Before this part: their text's stats (Old Blood's extra too) | ENCHANTMENTS.md |
| Aqua Affinity (`aqua_affinity`) | helmet | see STATS_EFFECTS.md | Mining, the stats part's | |
| Ice Cold (`ice_cold`) | armor | **LATER** | Cold Resistance is counted; nothing uses it: the Glacite Tunnels' cold | |
| Scuba (`scuba`) | armor | **LATER** | Pressure Resistance: deep water's pressure (Galatea) | |
| Forest Pledge (`forest_pledge`) | armor | **LATER** | Foraging Fortune: foraging | |
| Pesterminator (`pesterminator`) | armor | **LATER** | Farming Fortune and Bonus Pest Chance: the Garden and pests | |
| Tidal (`tidal`) | leggings | **LATER** | Defense against Sea Creatures: fishing | |
| Green Thumb (`green_thumb`) | equipment | **LATER** | Farming Fortune per Garden visitor served: the Garden | |
| Great Spook (`great_spook`) | armor | **LATER** | Fear: the Spooky Festival's Great Spook (its "Fear on Great Spook Armor" text isn't read either) | |

### Ultimates

| Enchantment (id) | Status | What it does here | Sources |
|---|---|---|---|
| Last Stand (`last_stand`) | **APPROX** | A hit or vanilla damage that takes them from 40% or more of their health to below it (and doesn't kill them): the pieces' Defense percents for its seconds (`buffPercent`) and their Vitality back, then its cooldown for all of them | text; [Last Stand](https://hypixelskyblock.minecraft.wiki/w/Last_Stand) |
| Legion (`legion`) | **APPROX** | Its share more of every Combat stat and Magic Find per other player within its blocks, up to its most; the pieces' add up, with Renowned's and Perfect's | text; [Legion](https://hypixelskyblock.minecraft.wiki/w/Legion) |
| Wisdom (`wisdom`) | **DONE** | Intelligence per 5 of their vanilla Exp levels, up to its cap, per piece | text |
| No Pain No Gain (`no_pain_no_gain`) | **APPROX** | Each hit a SkyBlock mob lands on them (its arrows too), each piece rolls its chance for its orbs, straight to them (`ExpOrbs`) | text; [No Pain No Gain](https://hypixelskyblock.minecraft.wiki/w/No_Pain_No_Gain) |
| Refrigerate (`refrigerate`) | **APPROX** | Its share of the mana spent as Defense for its seconds, up to its max per piece (the wiki's "per armor piece"); spending again adds to it | text; [Refrigerate](https://hypixelskyblock.minecraft.wiki/w/Refrigerate) |
| The One (`the_one`) | **APPROX** | Health and Strength per collection at its last tier (not boss collections); its lore keeps the book's "&k73" | text; [The One](https://hypixelskyblock.minecraft.wiki/w/The_One) |
| Habanero Tactics (`habanero_tactics`) | **APPROX** | Healing Wands' heals its percent more; its damage an additive buff on melee hits and arrows with a Slayer weapon (the wiki's Damage Calculation: additive); its Combat Wisdom while holding one. The Smoldering Polarization line is **LATER** (the Re-heated Gummy Polar Bear's effect) | text; [Habanero Tactics](https://hypixelskyblock.minecraft.wiki/w/Habanero_Tactics), [Additive Sources](https://hypixelskyblock.minecraft.wiki/w/Damage_Calculation/Additive_Sources) |
| Bank (`bank`) | **APPROX** | Its share of what a death takes from the purse is saved (`DeathCoins.addSaver`: the pieces' shares add up, two Bank V save it all); each kill of theirs gives each piece's coins into the purse | text; [Bank (Enchantment)](https://hypixelskyblock.minecraft.wiki/w/Bank_(Enchantment)), [Death](https://hypixelskyblock.minecraft.wiki/w/Death) |
| Bobbin' Time (`bobbin_time`) | **LATER** | Fishing (bobbers) | |
| Sunset (`sunset`) | **LATER** | The Garden (Overbloom, Visitor Cooldown) | |

### Armor and equipment reforges' bonuses

| Reforge (stone) | Status | What it does here |
|---|---|---|
| Renowned (Dragon Horn) | **DONE** | "Increases all Combat stats and Magic Find by +1%", per piece, with Legion's |
| Perfect (Diamond Atom) | **DONE** | "Increases Defense by +2%", per piece |
| Undead (Premium Flesh) | **DONE** | Hits by Undead mobs (the type, as the wiki's 0.23.4 fix has it) take 2% less, per piece |
| Cubic (Molten Cube) | **APPROX** | Hits by "Nether mobs" take 2% less, per piece: the Infernal, Magmatic and Arcane types (the wiki's Mob Types: "commonly found in the Crimson Isle") |
| Ridiculous (Red Nose) | **APPROX** | Sneaking with at least its Crit Chance: 20 Crit Chance less for 20 s, +30 Defense for 5 s, +50 mana, and not again while the Crit Chance is down; a low note for the fart |
| Ancient (Precursor Gear) | **DONE** | Before this part: +1 Crit Damage a Catacombs level (it has nothing else) |
| Bloodshot (Shriveled Cornea), on belts | **LATER** | "2.5% chance to cocoon" a kill outside the Dungeons (the wiki's Cocooning: the mob comes back after 6 s): the plugin spawns mobs only in dungeon rooms (and an admin's `/spawnentity`), so it waits for mobs outside the dungeons |
| Hyper (End Stone Geode), Loving (Red Scarf), Empowered (Sadan's Brooch), Blood-Soaked (Presumed Gallon of Red Paint) | the weapon part's | EFFECTS.md, ENCHANTS_WEAPONS.md |
| Calcified | **LATER** | Sea Creatures: fishing |
| Candied | **LATER** | The Spooky Festival's candy |
| Greater Spook | **LATER** | Fear: the Great Spook |
| Majestic | **LATER** | The Critter Safari |
| Mantid, Squeaky | **LATER** | The Garden's pests |
| Marshy | **LATER** | Forest Whispers: foraging |
| Royal, Dimensional, Blazing | **LATER** | Mining events, Titanium, Heat and Worms (mining the plugin doesn't have) |
| Thorny | **LATER** | Overbloom: the Garden |
| The rest of the armor and equipment pools and stones | **DONE** | Only stats (REFORGES.md) |

## UNKNOWN and approximations

Each is marked UNKNOWN in the code, with the reading taken:

- **How pieces combine**, for all but Refrigerate and Last Stand: each piece's adds up, each with its own cap.
- **Thorns**: "damage dealt" is what the hit took from them (after Defense); arrows' hits count; each piece rolls
  its own chance; the mob's Defense doesn't lessen the rebound.
- **Reflection**: nothing multiplies its damage; every arrow counts.
- **Counter-Strike**: "first hit from an enemy" is each mob's first, for as long as that mob is alive and they
  don't die (or switch profiles); a new one takes the last one's place and starts its seven seconds again.
- **Blast, Fire and Projectile Protection on vanilla damage**: their Defense (True Defense) lessens it as it would
  a hit, 100 / (100 + it), and nothing else does (SkyBlock's own fire and explosion rules: the wiki's "Differs").
- **Frost Walker**: 5 s before the ice melts; only on something solid; not in dungeon runs.
- **Stealth**: rolled once each time they come near (until they've gone half as far again away); the message each
  time the roll goes their way.
- **The Vitality enchantments and Refrigerate**: spending again while it lasts adds to it (up to the max) and starts
  its time again (a profile switch, which starts a new session without the buffs, starts it from none).
- **Vampiric Vitality and Transylvanian**: heals of their own, so the Catacombs boost counts in a run.
- **Respite**: "out of combat" is the core's `CombatState` (5 s since they last dealt or took a mob's damage).
- **Last Stand**: one cooldown for all its pieces; any hit or vanilla damage (traps and true damage too); the
  Defense is a share of their Defense as the rest makes it.
- **Legion**: "players within 30 blocks of you" are the others (alive, not spectators); the shares of Legion,
  Renowned and Perfect add up, on the stats after the set bonuses (Superior Blood's included); "all Combat stats"
  is the Stats menu's list (as for Superior Blood).
- **No Pain No Gain**: a mob's arrow is one of its hits.
- **The One**: boss collections don't count.
- **Quantum**: America/New_York's weekdays (the sidebar's); the weekend's Wisdom picked from its Saturday's date.
- **Hecatomb**: S+ is a score of 300 or more (the Score's grade); S+ runs count as S runs; its boost multiplies the
  rest of the run's experience; no message when it tiers up (none recorded).
- **Habanero Tactics**: a "Slayer weapon" is a sword, longsword or bow that asks for a Slayer level (Hypixel names
  no list: the daggers, katanas, scythes, the Halberd of the Shredded, the Pooch Sword; also the Terminator and the
  Juju, which ask for Enderman Slayer; not the Voidwalker Katana, which asks for nothing); its wand heal multiplies
  Reaper Armor's +50%.
- **Cubic**: "Nether mobs" are the Infernal, Magmatic and Arcane types.
- **Ridiculous**: 20 Crit Chance off (not a fifth of it); no fart while it's off; its sound.
- **Bank**: each piece's coins for a kill add up, as their savings on death do (only the savings are sourced).

## LATER, by what they wait for

- **Mobs outside the dungeons** (the plugin spawns them only in dungeon rooms): Bloodshot's cocooning.
- **Fishing**: Tidal, Bobbin' Time, Calcified.
- **The Garden** (farming, pests, visitors, Overbloom): Pesterminator, Green Thumb, Sunset, Mantid, Squeaky, Thorny.
- **Foraging** and Galatea's pressure: Forest Pledge, Scuba, Marshy.
- **The Glacite Tunnels' cold**: Ice Cold.
- **Events**: Great Spook and Greater Spook (the Great Spook), Candied (the Spooky Festival), Majestic (the Critter
  Safari).
- **Re-heated Gummy Polar Bear**: Habanero Tactics' Smoldering Polarization.
- **Mining the plugin doesn't have**: Royal, Dimensional, Blazing.
- **Enchanting-level gating** (the wiki's Enchantments: an enchantment you lack the Enchanting level for "will be
  greyed out and will not work"): every enchantment still works whatever the wearer's Enchanting level. It changes
  lore and the golden test (question below).
- **Counters on equipment** (the core's `ItemCounters`): none of these count on equipment, so nothing waits yet.

## Questions for the owner

- **Frost Walker** was removed from Hypixel in 0.22 (the wiki), but the Hex still offers it and its text says what
  it does, so it's built (frosted ice that melts back, outside dungeon runs). Keep it, or mark it removed so it does
  nothing and the Hex stops offering it?
- **Enchanting-level gating**: should enchantments stop working (and grey out in lore, with "Some of your
  enchantments require a higher Enchanting level!", as 353 live items show) for a wearer below their Enchanting
  level? It would change the golden items and Sandbox profiles too.
- **Habanero Tactics' Slayer weapons**: is "asks for a Slayer level" the right list? It takes in the Terminator and
  the Juju Shortbow (Enderman Slayer requirements) and leaves out the Voidwalker Katana.
- **Ridiculous**: a fart each sneak would give +50 mana each time; here it can't go again while its Crit Chance
  penalty lasts (20 s). Right?
