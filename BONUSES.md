# Armor bonuses

Armor's full set, tiered, piece and extra bonuses (the items' FULL_SET, TIERED, PIECE and EXTRA
blocks, 63 + 35 + 15 + 3 of them), and what armor pieces' own text says they do while worn. The
code is `paper/src/main/java/net/icxd/dungeons/item/bonus` (`SetBonuses` for the rules below, one
file per group of sets); the numbers are the items' text's, or the wiki's where the text has none.

## How it works

**What's worn** (`Worn`, `SetBonuses.worn`): the four armor slots (what's in them that is armor),
then the equipment once there are equipment slots (`SetBonuses.setEquipment`; until then none),
each piece with its blocks as its behaviour gives them, worked out at most once a tick.

**Sets** (`SetKey`): the pieces with a FULL_SET or TIERED block of the same name and piece count
are one set, so Hot and Infernal Crimson count together (the wiki's Kuudra sets take "the lowest
tier" worn), Maxor's and Necron's pieces are one Witherborn set, and Nutcracker Armor's Cold Thumb
(of 4) is apart from the Snow Suit's (of 8). A piece counts once in each set it's in (Nutcracker
Armor is in two).

**When a bonus counts** (`Bonus.needs`): a full set's with all its pieces (its header's "(0/4)");
one whose header has no count (the SNEAK ones) says how many; a tiered one's from its least (the
wiki's "tiered_bonus_required_pieces"; 1 where the wiki gives none); a piece's own (PIECE, EXTRA) for
each piece with it; an item's own text for each piece of it.

**Lore** (`SetBonusLore`): a set bonus's header counts the holder's worn pieces of the set wherever
the item is, as Hypixel's do: "Full Set Bonus: Shadow Assassin (4/4)" on the worn set (the SkyBlock
Menu tour, Stats & Equipment, 02:18.2), a stored Necron's Helmet's "Witherborn &7(0/4)" while Shadow
Assassin Armor is worn (the Loadouts recording, Armor Sets, 01:29.4). A full set's header stays gold
with the count grey until complete; a tiered one's is dark gray until it counts, then gold, and can
go past its pieces ("(3/2)", the wiki's Armor). Tiered bonuses that grow show the numbers for what's
worn (the first tier's until they count, as the data has them). Lore is built with its item, so
`ItemBuilder.build(item, tag, holder)` takes the holder: rebuilds of a player's items (joining,
picking up, switching) pass them, their armor changing rebuilds their items with a set bonus, and
with no holder (menus, the item browser, the golden test) the lore is the data's.

**What they do** go through the plugin's hooks: stats (`PlayerStats.addModifier`: each bonus's,
then the ones worked out from the rest, then auras from nearby wearers), hits (`Combat.addHitBuffs`,
new: an additive share and a factor that can depend on what the hit landed on and how far an arrow
flew, joined to the hit's additive sum exactly), hits taken (`PlayerDamage`, new: Defense against
what hit them, factors, theirs and nearby wearers', knockback resistance, after-hit listeners), the
Speed cap (`PlayerAttributes.addSpeedCap`, new) and mana costs (`Abilities.addManaCostFactor`, new);
kills, teleports, sneaking, bow shots, fire and a tick a second in `SetBonuses`; and (task 80) what their hits did
(`Combat.addHitListener`), every hit's factor, vanilla damage by cause, the Attack Speed cap, health costs, experience,
Scavenger coins, burning damages over time (`MobDebuffs.addDotFactor`, new), mobs' targets
(`TargetNearestPlayerGoal.addIgnored`, new), arrows as they leave the bow (`Shots.addShotListener`) and potion
effects kept off them. Numbers in a bonus's own text are read from it (`BonusText`); Hypixel's tables its text lacks
(the Bulwarks' kill steps) are private data next to items.json (`BonusTables`: `items/bonus_tables.json`, the text's
own step without it). Counts a bonus keeps (Training's kills) are on the piece that shows them, rebuilt for its
holder with its lines (`CountedSets`, `BonusCounters`: the Book of Stats' way). The dead and
dungeon ghosts (who are invulnerable) get no second's tick, so auras that hurt mobs (Blazing Aura,
Unstable Blood's lightning) and heals over time stop for them, and what they started ends ("Fixed
the Witherborn full set bonus working as a ghost in Dungeons", the wiki's Wither Armor); what they
wear does nothing for others either: a Stone to Steel Chestplate on one of them covers no one, Holy
Blood reaches no one, and Shoal and Armor of the Pack don't count them.

## For the other parts (wiring after the merge)

- Abilities that teleport (Ether Transmission, Shadow Warp, Spirit Leap...) call
  `SetBonuses.teleported(player)` once they have: the Shadow Assassin pieces act on it. Instant
  Transmission does already; ender pearls count by themselves.
- The Aspect of the Dragons' ability deals 50% more with Superior Blood, and healing wands heal
  50% more with Trolling The Reaper (after 3 s worn, 0.26.1): they ask
  `SetBonuses.active(player, "Superior Blood")` / `"Trolling The Reaper"`.
- Equipment slots: `SetBonuses.setEquipment(player -> their equipment stacks)`, and
  `SetBonuses.refreshLore(player)` when they change (Bloodrush, Arachne's, the Adaptive Belt count
  then).
- Collections: `SetBonuses.setCollections((player, itemId) -> amount)` (Emerald Armor's Tank, the
  Blaze sets' extra damage per 5,000 rods).
- The Fervor Chestplate's Ground Pound (an ABILITY block, sneaking: "At 10 stacks, sneak to reset
  your stacks and perform a Ground Pound") reads the stacks with `SetBonuses.fervor(player)` and
  resets them with `SetBonuses.spendFervor(player)`. Its damage "scales with EHP", by no formula on
  any page.
- Items: the Racing Helmet's "Speed: +400" is a line of its lore in the data, not a stat, so it
  isn't counted; its +100 Speed cap is.
- The Aurora Chestplate's Homing Missiles read Arcane Energy's stacks with `SetBonuses.arcaneEnergy(player)` and
  spend them with `SetBonuses.spendArcaneEnergy(player)`; the Hollow Wand reads Spirit's with
  `SetBonuses.spirit(player)` and spends some with `SetBonuses.spendSpirit(player, n)`. With the abilities part's
  hooks merged, that's `WeaponAbilities.arcaneEnergy(SetBonuses::arcaneEnergy, SetBonuses::spendArcaneEnergy)` and
  `UtilityAbilities.hollowSpirit(SetBonuses::spirit, SetBonuses::spendSpirit)`, from `SetBonuses.enable`.
- The Tank's Seismic Wave takes `SetBonuses.seismicWaveCut(player)` off its cooldown (Super Heavy Armor); the
  abilities part's `RunItems.seismicWaveCut` (the Earth Shard) changes the same line: both cuts come off.
- The Starlight Wand's Starfall asks `SetBonuses.starfallDuration(player)` and `starfallRange(player)` (Starpower).
- The private `bonus_tables.json` goes in the data repository's items/ folder, next to items.json.

## Status

DONE: as the text and wiki say. APPROX: done in part, or with a reading marked UNKNOWN in the code.
LATER: waits for the system named.

| Bonus | Kind | Sets (items) | Status | Sources |
|---|---|---|---|---|
| Intimidate | full set | BURNING_FERVOR, FERVOR, FIERY_FERVOR, HOT_FERVOR, INFERNAL_FERVOR (20) | **APPROX** a melee hit or arrow has the text's 50% chance to turn the text's 3 mobs on them: the nearest within 10 blocks that aren't already (which and how far: UNKNOWN) | [Fervor Armor](https://hypixelskyblock.minecraft.wiki/w/Fervor_Armor) |
| Witherborn | full set | POWER_WITHER, SPEED_WITHER, TANK_WITHER, WISE_WITHER, WITHER (20) | **LATER** the wither minion: its explosion damage is on no page (question for the owner); the pieces' "10% less from withers" is done (see items' own text) | [Wither Armor](https://hypixelskyblock.minecraft.wiki/w/Wither_Armor), [Necron's Armor](https://hypixelskyblock.minecraft.wiki/w/Necron's_Armor) |
| Absorb | full set | ARMOR_OF_MAGMA, ARMOR_OF_YOG, SHARK_SCALE, SPONGE (16) | **APPROX** Sponge/Shark Scale: Defense x2 in water (DONE); Armor of Magma: +1 Health and Intelligence per 10 Magma Cubes killed, at most 200, kept on the chestplate with its lines (DONE; the Hub's Magma Cube is the only one); Yog Armor LATER (no Yogs) | [Armor of Magma](https://hypixelskyblock.minecraft.wiki/w/Armor_of_Magma) |
| Star Fisher | full set | BRONZE_HUNTER, DIAMOND_HUNTER, GOLD_HUNTER, SILVER_HUNTER (16) | **LATER** fishing |  |
| Cold Thumb | full set | NUTCRACKER, SNOW, SNOW_SUIT (12) | **LATER** Jerry's Workshop, Frosty the Snow Cannon, Gift Attack |  |
| Dashing | full set | CHEAP_TUXEDO, ELEGANT_TUXEDO, FANCY_TUXEDO (9) | **DONE** max health set to 75/150/250, +50/100/150 additive damage; mixed Tuxedos: the cheapest (UNKNOWN) | [Cheap Tuxedo](https://hypixelskyblock.minecraft.wiki/w/Cheap_Tuxedo), [Fancy Tuxedo](https://hypixelskyblock.minecraft.wiki/w/Fancy_Tuxedo), [Elegant Tuxedo](https://hypixelskyblock.minecraft.wiki/w/Elegant_Tuxedo), [Additive Sources](https://hypixelskyblock.minecraft.wiki/w/Damage_Calculation/Additive_Sources) |
| Dungeon Lord | full set | SKELETON_LORD, ZOMBIE_LORD (8) | **DONE** per whole minute of the run; no cap (UNKNOWN); mixed Skeleton/Zombie Lord pieces give a quarter of their own each (UNKNOWN) | [Skeleton Lord Armor](https://hypixelskyblock.minecraft.wiki/w/Skeleton_Lord_Armor), [Zombie Lord Armor](https://hypixelskyblock.minecraft.wiki/w/Zombie_Lord_Armor) |
| Efficient training | full set | ADAPTIVE, STARRED_ADAPTIVE (8) | **DONE** +2% of each piece's stats per 5 Catacombs levels (stat level, max 50: UNKNOWN); the class bonuses are per piece, see items' own text | [Adaptive Armor](https://hypixelskyblock.minecraft.wiki/w/Adaptive_Armor) |
| One with the Fish | full set | ABYSSAL, DIVER (8) | **DONE** their air kept full while in water, every second |  |
| Power of the Resistance | full set | ARMOR_OF_THE_RESISTANCE, GENERALS_ARMOR_OF_THE_RESISTANCE (8) | **LATER** Dante (the Rift) |  |
| Shadow Assassin | full set | SHADOW_ASSASSIN, STARRED_SHADOW_ASSASSIN (8) | **DONE** +1 Strength a kill in the run, while worn; resets each run (no cap: UNKNOWN) | [Shadow Assassin Armor](https://hypixelskyblock.minecraft.wiki/w/Shadow_Assassin_Armor) |
| Vindicate | full set | HEAVY, SUPER_HEAVY (8) | **DONE** +1 Speed per whole 50 Defense (after the rest of the stats) | [Heavy Armor](https://hypixelskyblock.minecraft.wiki/w/Heavy_Armor), [Super Heavy Armor](https://hypixelskyblock.minecraft.wiki/w/Super_Heavy_Armor) |
| Trolling The Reaper | full set | REAPER, REVENANT (6) | **DONE** +100 Defense against Undead; Reaper's +100 additive against Undead, x0.01 else (all three Reaper pieces); healing wands' +50% is the wands' (SetBonuses.active) | [Revenant Armor](https://hypixelskyblock.minecraft.wiki/w/Revenant_Armor), [Reaper Armor](https://hypixelskyblock.minecraft.wiki/w/Reaper_Armor) |
| Zombie Knight | full set | ZOMBIE_KNIGHT (5) | **DONE** +50 Defense and the sword's +30 Strength with the full set and the sword held | [Zombie Knight Armor](https://hypixelskyblock.minecraft.wiki/w/Zombie_Knight_Armor), [Zombie Knight Sword](https://hypixelskyblock.minecraft.wiki/w/Zombie_Knight_Sword) |
| Absolute Unit | full set | MASTIFF (4) | **DONE** Defense cap 300, +1 Crit Damage per 2 Defense, +1% Health per 25 Crit Damage (40% max), -20% from Animal mobs, heal 150 for 5 Vitality when hit (1 s) | [Mastiff Armor](https://hypixelskyblock.minecraft.wiki/w/Mastiff_Armor) |
| Absorption | full set | GOLEM_ARMOR (4) | **DONE** a kill gives 60 absorption (Absorption III, "60 HP", the wiki's history) for 20 s, given again by the next | [Golem Armor](https://hypixelskyblock.minecraft.wiki/w/Golem_Armor) |
| Armor of the Pack | full set | THE_PACK (4) | **DONE** +35 Strength, +80 Defense per wearer within 30 blocks (max 3, the wearer counted: UNKNOWN) | [Armor of the Pack](https://hypixelskyblock.minecraft.wiki/w/Armor_of_the_Pack) |
| Bat Powers Activate! | full set | BAT_PERSON (4) | **LATER** the Grappling Hook upgrade and Spooky Festival candy | [Bat Person Armor](https://hypixelskyblock.minecraft.wiki/w/Bat_Person_Armor) |
| Battalion | full set | NUTCRACKER (4) | **APPROX** +1 Magic Find and +50 Defense for each wearer within 30 blocks, them included (UNKNOWN, as Shoal), at most 5; the gifts and Jerry's Workshop LATER |  |
| Beginner's Boost | full set | MINER_OUTFIT (4) | **DONE** +40 Mining Speed |  |
| Blazing Aura | full set | BLAZE (4) | **DONE** 3% of max health a second to mobs within 5 blocks, at most 500 (+100 per 5,000 Blaze Rods once collections are wired, 5,000 max); per mob (UNKNOWN) | [Blaze Armor](https://hypixelskyblock.minecraft.wiki/w/Blaze_Armor) |
| Bonus Speed | full set | SPEEDSTER (4) | **DONE** +20 Speed | [Speedster Armor](https://hypixelskyblock.minecraft.wiki/w/Speedster_Armor) |
| Bouncing Arrow | full set | BOUNCY (4) | **APPROX** an arrow's hit has a 25% chance to hit the nearest other mob within 10 blocks for the same damage, as the bounce's own (range and damage UNKNOWN) | [Bouncy Armor](https://hypixelskyblock.minecraft.wiki/w/Bouncy_Armor) |
| Candy Man | full set | SPOOKY (4) | **LATER** Spooky Festival |  |
| Death Tax | full set | MERCENARY (4) | **DONE** +5 coins and heal 20 a kill of a level 10+ mob, 0.5 s apart (the wiki, July 2026) | [Mercenary Armor](https://hypixelskyblock.minecraft.wiki/w/Mercenary_Armor) |
| Deflect | full set | CACTUS (4) | **DONE** 33% of what a mob's hit took back at it | [Cactus Armor](https://hypixelskyblock.minecraft.wiki/w/Cactus_Armor) |
| Dwarf Wannabe | full set | MITHRIL (4) | **DONE** +30 Mining Speed and +5 Mining Fortune (the text's) with the four equipment pieces on, on the Dwarven Mines | [Mithril Belt](https://hypixelskyblock.minecraft.wiki/w/Mithril_Belt) |
| Expert Miner | full set | GLACITE (4) | **DONE** +2 Mining Speed per Mining level | [Glacite Armor](https://hypixelskyblock.minecraft.wiki/w/Glacite_Armor) |
| Fairy's Outfit | full set | FAIRY (4) | **LATER** fairy souls | [Fairy Armor](https://hypixelskyblock.minecraft.wiki/w/Fairy_Armor) |
| Fearsome | full set | GREAT_SPOOK (4) | **APPROX** mobs at or below their Fear (by the level their name shows) don't go for them, and run 8 blocks off from within 10 (the distances UNKNOWN) | [Great Spook Armor](https://hypixelskyblock.minecraft.wiki/w/Great_Spook_Armor) |
| Frozen Blazing Aura | full set | FROZEN_BLAZE (4) | **DONE** 300 + 3% a second, Slowness I 4 s, same most | [Frozen Blaze Armor](https://hypixelskyblock.minecraft.wiki/w/Frozen_Blaze_Armor) |
| Gemstone Gatherer | full set | AMBER, AMETHYST, JADE, SAPPHIRE (4) | **LATER** the Crystal Hollows (its gems) |  |
| Heat Shield | full set | HEAT (4) | **LATER** Heat |  |
| Holy Blood | full set | HOLY_DRAGON (4) | **DONE** +75 Health Regen to the wearer and players within 10 blocks, once | [Holy Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Holy_Dragon_Armor) |
| Nether Lord | full set | EMBER (4) | **APPROX** no fire/lava damage (DONE); obsidian over lava LATER (it changes the world's blocks; which zones it's off in is UNKNOWN: a question for the owner) |  |
| Newton's Demise | full set | THERMODYNAMIC (4) | **DONE** Attack Speed cap +50 (the text's; `Combat.addAttackSpeedCap`) | [Thermodynamic Armor](https://hypixelskyblock.minecraft.wiki/w/Thermodynamic_Armor), [Attack Speed](https://hypixelskyblock.minecraft.wiki/w/Attack_Speed) |
| Night Affinity | full set | MUSHROOM (4) | **DONE** Night Vision while worn | [Mushroom Armor](https://hypixelskyblock.minecraft.wiki/w/Mushroom_Armor) |
| Octodexterity | full set | TARANTULA (4) | **DONE** Tarantula: every 4th landed melee hit x2 (multiplicative); Venom LATER (mobs don't heal) | [Tarantula Armor](https://hypixelskyblock.minecraft.wiki/w/Tarantula_Armor) |
| Old Blood | full set | OLD_DRAGON (4) | **APPROX** the set's own Growth/Protection/Sugar Rush/True Protection +10 Health/+2 Defense/+1 Speed/+3 True Defense a level on top of their own (the wiki's Enchantment Buffs); Feather Falling LATER (the armor enchantments part's Feather Falling, to merge) | [Old Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Old_Dragon_Armor), [Growth](https://hypixelskyblock.minecraft.wiki/w/Growth) |
| Protective Blood | full set | PROTECTOR_DRAGON (4) | **DONE** the pieces' Defense +1% per whole missing percent of health | [Protector Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Protector_Dragon_Armor) |
| Refraction | full set | CRYSTAL (4) | **APPROX** each piece's Defense and Intelligence at 12.5% a light level from 12.5% (0) to 200% (15), at their head (the wiki's table; UNKNOWN whether the text's "from 0" means none at 0); the "Current Light Level" line shows theirs, refreshed when it changes; the colour doesn't change | [Crystal Armor](https://hypixelskyblock.minecraft.wiki/w/Crystal_Armor) |
| Regenerative Howl | full set | WEREWOLF (4) | **APPROX** each Ferocity strike gives a stack of +50 Defense for 5 s (the newest 10 count) and heals the players within 25 blocks, not the wearer, 1% of their Defense (the wiki's; the text doesn't say it: UNKNOWN whether it still does; a strike each an activation: UNKNOWN) | [Werewolf Armor](https://hypixelskyblock.minecraft.wiki/w/Werewolf_Armor) |
| Shoal | full set | ZOMBIE_SOLDIER (4) | **DONE** +30 Defense per full Zombie Soldier set within 30 blocks, the wearer's counted (UNKNOWN) | [Zombie Soldier Armor](https://hypixelskyblock.minecraft.wiki/w/Zombie_Soldier_Armor) |
| Sieve Body | full set | ROTTEN (4) | **DONE** +20% knockback resistance to arrows (hits by mobs' arrows) | [Rotten Armor](https://hypixelskyblock.minecraft.wiki/w/Rotten_Armor) |
| Skeleton Master | full set | SKELETON_MASTER (4) | **DONE** +25 additive arrow damage (additive: UNKNOWN, as Maxor's) | [Skeleton Master Armor](https://hypixelskyblock.minecraft.wiki/w/Skeleton_Master_Armor) |
| Skeleton Soldier | full set | SKELETON_SOLDIER (4) | **DONE** +25 additive arrow damage | [Skeleton Soldier Armor](https://hypixelskyblock.minecraft.wiki/w/Skeleton_Soldier_Armor) |
| Skeletor | full set | SKELETOR (4) | **LATER** Skeletor kills (no Skeletor mob yet; the count would be kept on the chestplate as Training's) | [Skeletor Armor](https://hypixelskyblock.minecraft.wiki/w/Skeletor_Armor) |
| Smart Miner | full set | GOBLIN (4) | **DONE** all Intelligence into Mining Speed, +1 per whole 15 (UNKNOWN: all of it) |  |
| Soul Whisper | full set | NECROMANCER_LORD (4) | **LATER** necromancy |  |
| Spooky | full set | GREAT_SPOOK (4) | **LATER** Great Spook Staff |  |
| Springsneak (SNEAK) | full set | RABBIT (4) | **DONE** Jump Boost II while sneaking |  |
| Starpower | full set | STARLIGHT (4) | **APPROX** the Starlight Wand's Starfall lasts 2x and goes 1 block further (the range taken as how far it can be put: UNKNOWN; `SetBonuses.starfallDuration`/`starfallRange`) | [Starlight Armor](https://hypixelskyblock.minecraft.wiki/w/Starlight_Armor) |
| Strong Blood | full set | STRONG_DRAGON (4) | **DONE** +75 Damage holding the Aspect of the End; its Instant Transmission +2 range, +3 s, +5 Strength for the cast's speed time (UNKNOWN); not the Aspect of the Void | [Strong Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Strong_Dragon_Armor) |
| Superior Blood | full set | SUPERIOR_DRAGON (4) | **DONE** Combat Stats (as the Stats menu groups them) and Magic Find x1.05; the Aspect of the Dragons +50% is the weapon's (it can ask SetBonuses.active) | [Superior Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Superior_Dragon_Armor) |
| Tank | full set | EMERALD_ARMOR (4) | **DONE** +1 Health and Defense per 3,000 Emeralds collected (max 350): 0 until SetBonuses.setCollections is wired | [Emerald Armor](https://hypixelskyblock.minecraft.wiki/w/Emerald_Armor) |
| Training | full set | ZOMBIE_COMMANDER (4) | **APPROX** +5 Health per 50 Zombies killed, at most 500, kept on the chestplate with its lines; a Zombie is a mob that is a vanilla zombie (UNKNOWN: a question for the owner) | [Zombie Commander Armor](https://hypixelskyblock.minecraft.wiki/w/Zombie_Commander_Armor) |
| True Dwarf | full set | TITANIUM (4) | **DONE** +50 Mining Speed and +40 Mining Fortune with the four Titanium equipment pieces on, on the Dwarven Mines | [Titanium Belt](https://hypixelskyblock.minecraft.wiki/w/Titanium_Belt) |
| Unstable Blood | full set | UNSTABLE_DRAGON (4) | **DONE** every 15 s lightning on mobs within 8 blocks for 3,000 (dealt as it is: UNKNOWN whether Defense counts) | [Unstable Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Unstable_Dragon_Armor) |
| Vivacious Darkness | full set | FINAL_DESTINATION (4) | **LATER** soulflow |  |
| Wise Blood | full set | WISE_DRAGON (4) | **DONE** abilities cost 2/3 of their mana (a factor, multiplies with others) | [Wise Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Wise_Dragon_Armor) |
| Young Blood | full set | YOUNG_DRAGON (4) | **DONE** +70 Speed above 50% health, +100 Speed cap | [Young Dragon Armor](https://hypixelskyblock.minecraft.wiki/w/Young_Dragon_Armor) |
| Enrage (SNEAK) | full set | REAPER (3) | **DONE** sneak: 6 s of +100 Speed/Damage/Strength, 25 s cooldown; the red dye is not done (UNKNOWN look) | [Reaper Armor](https://hypixelskyblock.minecraft.wiki/w/Reaper_Armor) |
| Projectile Absorption | full set | ZOMBIE (3) | **DONE** heal 10 a second for 5 s after a projectile hit |  |
| Arcane Energy | tiered | AURORA, BURNING_AURORA, FIERY_AURORA, HOT_AURORA, INFERNAL_AURORA (20) | **APPROX** a stack from an ability's magic damage (an ability's hit that doesn't crit: UNKNOWN whether Hypixel counts the others) at most every 1/0.7/0.5 s, up to 10, one lost after 4 s; the Aurora Chestplate's Homing Missiles (the abilities part's) read them with `SetBonuses.arcaneEnergy` and spend them with `spendArcaneEnergy` | [Aurora Armor](https://hypixelskyblock.minecraft.wiki/w/Aurora_Armor) |
| Dominus | tiered | BURNING_CRIMSON, CRIMSON, FIERY_CRIMSON, HOT_CRIMSON, INFERNAL_CRIMSON (20) | **APPROX** stacks from melee hits (1.5/1/0.5 s, lost after 4/7/10 s): Swing Range, Ferocity, Infernal's +10% additive; the lowest tier worn; at 10 stacks a melee hit swipes (at most once a stack's time): the hit x Swipe Strength (0.5 to 1 by tier, x1/2/3 by pieces) x 100 / its Additive Multiplier (the wiki's Additive Sources), double on the nearest, along 5 blocks a random way (length and width UNKNOWN) | [Crimson Armor](https://hypixelskyblock.minecraft.wiki/w/Crimson_Armor), [Additive Sources](https://hypixelskyblock.minecraft.wiki/w/Damage_Calculation/Additive_Sources) |
| Fervor | tiered | BURNING_FERVOR, FERVOR, FIERY_FERVOR, HOT_FERVOR, INFERNAL_FERVOR (20) | **APPROX** stacks from hits on mobs, arrows' too (1.5/1/0.5 s, lost after 4/7/10 s; UNKNOWN whether abilities' damage counts), kept for the chestplate's Ground Pound, an ability that isn't here (see the wiring above); lore numbers done | [Fervor Armor](https://hypixelskyblock.minecraft.wiki/w/Fervor_Armor) |
| Hydra Strike | tiered | BURNING_TERROR, FIERY_TERROR, HOT_TERROR, INFERNAL_TERROR, TERROR (20) | **APPROX** stacks from arrow hits (0.2 s, lost after 4/7/10 s): +k% additive a stack by tier and pieces; arrows +1% to +3% faster a stack (the lowest tier's); at 10 stacks each shot has two more arrows beside it at 20% to 60% of its damage, once a shot (10 degrees apart: UNKNOWN) | [Terror Armor](https://hypixelskyblock.minecraft.wiki/w/Terror_Armor) |
| Spirit | tiered | BURNING_HOLLOW, FIERY_HOLLOW, HOLLOW, HOT_HOLLOW, INFERNAL_HOLLOW (20) | **APPROX** stacks from the wearer's and up to 4 other players' hits within 25 blocks (melee, arrows, abilities), each player at most every 3/2/1 s, up to the lowest tier's "Max 10" to "Max 40", one lost after 4 s; the Hollow Wand (the abilities part's) reads them with `SetBonuses.spirit` and spends them with `spendSpirit`; the four others are the first four (UNKNOWN) | [Hollow Armor](https://hypixelskyblock.minecraft.wiki/w/Hollow_Armor) |
| Odger's Blessing | tiered | BRONZE_HUNTER, DIAMOND_HUNTER, GOLD_HUNTER, SILVER_HUNTER (16) | **LATER** fishing |  |
| Peace Treaty | tiered | BRONZE_HUNTER, DIAMOND_HUNTER, GOLD_HUNTER, SILVER_HUNTER (16) | **LATER** fishing (counts from 2 in lore) |  |
| Unearthed | tiered | CHALLENGER, MYTHOS (16) | **LATER** Mythological mobs (Diana) |  |
| Arachne's Faithful | tiered | ARACHNE (8) | **DONE** +5/10/20/35/50/70/100 Health and Defense from 2 to 8 pieces (armor now, equipment once wired) | [Arachne's Armor](https://hypixelskyblock.minecraft.wiki/w/Arachne's_Armor) |
| Deep Sea Diver | tiered | ABYSSAL, DIVER (8) | **LATER** fishing |  |
| Deepness Within | tiered | ANGLER (8) | **APPROX** +6 Health per Fishing level, +10 with all four (the wiki's "6-10": in between UNKNOWN), and its "Fishing: Level" line shows theirs; Fishing gives no XP here, so it's the level they have | [Angler Armor](https://hypixelskyblock.minecraft.wiki/w/Angler_Armor) |
| Depth Champion | tiered | ANGLER (8) | **LATER** Sea Creatures |  |
| Feast | tiered | FERMENTO, HELIANTHUS (8) | **LATER** farming |  |
| Swamp Soldier | tiered | BACKWATER (8) | **LATER** Sea Creatures |  |
| Carnival Craze | tiered | PARTY (5) | **LATER** the Carnival |  |
| Fireproof | tiered | MAGMA_LORD (5) | **DONE** no lava damage on the Crimson Isle (lava only: UNKNOWN whether fire counts); Kuudra's Hollow isn't here |  |
| Lord's Blessing | tiered | MAGMA_LORD (5) | **LATER** the Crimson Isle, Magmatic mobs, fishing |  |
| Static Charge | tiered | THUNDER, THUNDERBOLT (5) | **APPROX** a charge every 30/25/20/10 s from when it went on, at most 2/3/4/5; a melee hit spends them all for +15/20/25/40% each, additive (UNKNOWN), lightning on its target (a look); the Magmatic part LATER (no Magmatic mobs) | [Thunder Armor](https://hypixelskyblock.minecraft.wiki/w/Thunder_Armor) |
| Berserk | tiered | BERSERKER (4) | **DONE** Health and Defense halved; abilities' health costs 20/40/60% less (`Abilities.addHealthCostFactor`) | [Berserker Armor](https://hypixelskyblock.minecraft.wiki/w/Berserker_Armor) |
| Cropier Crops | tiered | MELON (4) | **LATER** farming |  |
| Eradicator | tiered | PESTHUNTERS (4) | **LATER** pests |  |
| Familiarity | tiered | MYTHOS (4) | **LATER** Mythological mobs |  |
| Festival Fisher | tiered | SHARK_SCALE (4) | **LATER** the Fishing Festival |  |
| Glossy Mineralworks | tiered | GLOSSY_MINERAL (4) | **LATER** the Mining Spread stat |  |
| Long Tuba | tiered | SNORKELING (4) | **DONE** +2/5/10 Respiration at 2/3/4 pieces | [Snorkeling Armor](https://hypixelskyblock.minecraft.wiki/w/Snorkeling_Armor) |
| Mento Fermento | tiered | SQUASH (4) | **LATER** farming |  |
| Mineralworks | tiered | MINERAL (4) | **LATER** the Mining Spread stat | [Mineral Armor](https://hypixelskyblock.minecraft.wiki/w/Mineral_Armor) |
| Molten Core | tiered | MOLTEN (4) | **LATER** Kuudra |  |
| Mythological Greed | tiered | MYTHOS (4) | **LATER** Mythological mobs |  |
| Octodexterity | tiered | PRIMORDIAL (4) | **APPROX** Primordial (tiered): every 3rd landed melee hit x1.5, with the whole set (what its tiers change: UNKNOWN) | [Primordial Armor](https://hypixelskyblock.minecraft.wiki/w/Primordial_Armor) |
| Rekindle | tiered | REKINDLED_EMBER (4) | **APPROX** their burning damages over time (Fire Aspect's, `MobDebuffs.addDotFactor`) +200/400/600%, and +1.2/2.5/5% for each second they've been on fire up to 50/100/200%, added up (UNKNOWN); a burning mob's melee hit sets them on fire for 3 s (UNKNOWN). The wiki says Hypixel's is "bugged to not do anything" | [Rekindled Ember Armor](https://hypixelskyblock.minecraft.wiki/w/Rekindled_Ember_Armor) |
| Riches | tiered | ELEANOR (4) | **APPROX** Scavenger coins (the enchantment's and the accessory's) x1.5/2/3 with 2/3/4 pieces (`KillCoins.addScavengerFactor`); each piece's "Scavenger Coins Gained" counts all of them made with it on (UNKNOWN: not only Riches' extra) | [Eleanor](https://hypixelskyblock.minecraft.wiki/w/Eleanor) |
| Shimmer | tiered | SHIMMERING_LIGHT (4) | **DONE** experience from mobs and ores +200/200/300% with 2/3/4 pieces | [Shimmering Light Armor](https://hypixelskyblock.minecraft.wiki/w/Shimmering_Light_Armor) |
| Squashbuckle | tiered | CROPIE (4) | **LATER** farming |  |
| Legacy | tiered | DIVAN (1) | **LATER** the Glacite Mineshafts |  |
| Spider Bulwark | piece | PRIMORDIAL, TARANTULA (8) | **DONE** each piece counts its wearer's Spider kills (spiders and cave spiders; none here yet) and gives Defense against them by the private tables' steps (the text's first without them), with its lines | [Primordial Armor](https://hypixelskyblock.minecraft.wiki/w/Primordial_Armor) |
| Zombie Bulwark | piece | REAPER, REVENANT (6) | **APPROX** each piece counts its wearer's Zombie kills and gives Defense against them by the private tables' 14 steps (`items/bonus_tables.json`; the text's first without them), with its "Piece Bonus" and "Next Upgrade" lines; a Zombie as for Training (UNKNOWN); "Maxed!" past the last step (UNKNOWN wording) | [Revenant Armor](https://hypixelskyblock.minecraft.wiki/w/Revenant_Armor) |
| Enderman Bulwark | piece | FINAL_DESTINATION (4) | **DONE** as the Spider Bulwark, for Endermen (none here yet) |  |
| Florist | piece | BLOSSOM (4) | **LATER** the Garden |  |
| Magnetic | piece | LAPIS_ARMOR (4) | **DONE** +50% experience from ores a piece (ExpOrbs' ORE source: Mithril's orbs are the mining part's) | [Lapis Armor](https://hypixelskyblock.minecraft.wiki/w/Lapis_Armor) |
| Salesperson | piece | LOTUS (4) | **LATER** the Garden |  |
| Bloodrush | piece | SHADOW_ASSASSIN, STARRED_SHADOW_ASSASSIN (2) | **APPROX** the cloak: next melee hit within 5 s +10 additive (UNKNOWN which kind); counts once equipment slots are wired | [Shadow Assassin Cloak](https://hypixelskyblock.minecraft.wiki/w/Shadow_Assassin_Cloak) |
| Fluxation | piece | SHADOW_ASSASSIN, STARRED_SHADOW_ASSASSIN (2) | **DONE** on teleport: +10 mana, 3 s cooldown | [Shadow Assassin Armor](https://hypixelskyblock.minecraft.wiki/w/Shadow_Assassin_Armor) |
| Pursuit | piece | SHADOW_ASSASSIN, STARRED_SHADOW_ASSASSIN (2) | **DONE** on teleport: Invisibility 10 s (as recorded) and +20 Speed 10 s, 3 s cooldown | [Shadow Assassin Armor](https://hypixelskyblock.minecraft.wiki/w/Shadow_Assassin_Armor) |
| Salubrious | piece | SHADOW_ASSASSIN, STARRED_SHADOW_ASSASSIN (2) | **DONE** on kill (the game's text; the wiki says teleport): heal 15, 3 s cooldown | [Shadow Assassin Armor](https://hypixelskyblock.minecraft.wiki/w/Shadow_Assassin_Armor) |
| Sinew | piece | SHADOW_ASSASSIN, STARRED_SHADOW_ASSASSIN (2) | **DONE** on teleport: +10 Strength 10 s, 3 s cooldown | [Shadow Assassin Armor](https://hypixelskyblock.minecraft.wiki/w/Shadow_Assassin_Armor) |
| Blaze Consumer | piece | VANQUISHED_BLAZE (1) | **LATER** counts on equipment (kept in storage), Blazes |  |
| Ghast Blaster | piece | VANQUISHED_GHAST (1) | **LATER** counts on equipment, Ghasts |  |
| Glowing | piece | VANQUISHED_GLOWSTONE (1) | **LATER** counts on equipment, Glowstone to mine |  |
| Magma Cube Absorber | piece | VANQUISHED_MAGMA (1) | **LATER** counts on equipment (kept in storage: ItemCounters can't put a piece back) |  |
| Anti-Toxin | extra | PRIMORDIAL, TARANTULA (2) | **LATER** the Tarantula Broodfather |  |
| Brood | extra | PRIMORDIAL, TARANTULA (2) | **LATER** pets |  |
| Radioactive | extra | PRIMORDIAL, TARANTULA (2) | **DONE** +1 (Primordial +1.5) Crit Damage per whole 10 Strength, up to 1,000 | [Tarantula Armor](https://hypixelskyblock.minecraft.wiki/w/Tarantula_Armor), [Primordial Armor](https://hypixelskyblock.minecraft.wiki/w/Primordial_Armor) |

### Items' own text

What armor pieces' own lines (not a bonus block) say they do while worn.

| What | Items | Status | Sources |
|---|---|---|---|
| Adaptive's class bonuses | ADAPTIVE, STARRED_ADAPTIVE armor and belts (10) | **DONE** each piece in the Catacombs, by selected class (which is the run's): Berserk +20 Strength, Healer +5 Mending +40 Health, Mage +50 Intelligence, Tank +30 Defense and 5% less a piece from the same monster within 10 s, Archer +5 Crit Chance +15 Crit Damage; the belts' own numbers (once equipment is wired) | [Adaptive Armor](https://hypixelskyblock.minecraft.wiki/w/Adaptive_Armor) |
| +5% arrow damage a piece | SKELETON_GRUNT, SKELETON_SOLDIER, SKELETON_MASTER, SPEED_WITHER (16) | **DONE** +5 additive a piece (Maxor's is additive on the wiki; the skeletons' taken the same, UNKNOWN) | [Additive Sources](https://hypixelskyblock.minecraft.wiki/w/Damage_Calculation/Additive_Sources) |
| "Your bows don't consume arrows." | SKELETON_MASTER_CHESTPLATE (1) | **DONE** a bow's shot takes no arrow | [Skeleton Master Armor](https://hypixelskyblock.minecraft.wiki/w/Skeleton_Master_Armor) |
| 15% arrow knockback resistance a piece | ROTTEN (4) | **DONE** with Sieve Body's 20%, 80% for the set | [Rotten Armor](https://hypixelskyblock.minecraft.wiki/w/Rotten_Armor) |
| "Reduces the damage you take from withers by 10%" (Crypt Witherlord's 5%) | WITHER, POWER/SPEED/TANK/WISE_WITHER (20), CRYPT_WITHERLORD (4) | **DONE** from Wither-type mobs; pieces add up, 40% for four Wither pieces (UNKNOWN: they might multiply) | [Wither Armor](https://hypixelskyblock.minecraft.wiki/w/Wither_Armor), [Crypt Witherlord Armor](https://hypixelskyblock.minecraft.wiki/w/Crypt_Witherlord_Armor) |
| Mending (and Vitality) while in Dungeons | MENDER_HELMET, MENDER_FEDORA, MENDER_CROWN (3) | **DONE** +50 Mending; +65 and +80 Mending and Vitality (0.26.1), in a run; the run's stat boost doesn't grow them (UNKNOWN: the text has no bracket) | [Mender Helmet](https://hypixelskyblock.minecraft.wiki/w/Mender_Helmet), [Mender Fedora](https://hypixelskyblock.minecraft.wiki/w/Mender_Fedora), [Mender Crown](https://hypixelskyblock.minecraft.wiki/w/Mender_Crown) |
| "Players within 10 blocks of you take 5% less damage" in Dungeons, 30 blocks as a Tank | STONE_CHESTPLATE, METAL_CHESTPLATE, STEEL_CHESTPLATE (3) | **DONE** 5/8/10% off every hit (traps' too) on players in the wearer's run, the wearer included; of several wearers the one that takes off most counts (UNKNOWN both) | [Stone Chestplate](https://hypixelskyblock.minecraft.wiki/w/Stone_Chestplate), [Metal Chestplate](https://hypixelskyblock.minecraft.wiki/w/Metal_Chestplate), [Steel Chestplate](https://hypixelskyblock.minecraft.wiki/w/Steel_Chestplate) |
| Arrows +1% for every 2 blocks traveled above 20 | SNIPER_HELMET (1) | **DONE** whole steps, from where the arrow was shot to where it hit (as Snipe measures), additive as Snipe's is (UNKNOWN both) | [Sniper Helmet](https://hypixelskyblock.minecraft.wiki/w/Sniper_Helmet) |
| +50 Defense against Animal mobs (the chestplate's +75) and +5 True Defense a piece | THE_PACK (4) | **DONE** the pieces' own numbers, 225 Defense against Animal mobs for the set (the wiki's set total); the True Defense always, as the text reads (UNKNOWN: the wiki has 20 against Animal mobs only) | [Armor of the Pack](https://hypixelskyblock.minecraft.wiki/w/Armor_of_the_Pack) |
| Combat Stats x2 at night, x3 in the Spooky Festival | BAT_PERSON (4) | **APPROX** each piece's Combat Stats (the Stats menu's group; its reforge's and enchantments' too) count twice from 7pm to 6am SkyBlock time, three times from Autumn 29th to 31st (the wiki's Spooky Festival); its candy LATER | [Bat Person Armor](https://hypixelskyblock.minecraft.wiki/w/Bat_Person_Armor), [Spooky Festival](https://hypixelskyblock.minecraft.wiki/w/Spooky_Festival) |
| "Arthropod mobs deal -30% damage" | SPIDER_HAT (1) | **DONE** | [Spider Hat](https://hypixelskyblock.minecraft.wiki/w/Spider_Hat) |
| "Grants +100 Speed Cap" | RACING_HELMET (1) | **DONE** the cap (its +400 Speed isn't a stat in the data, see the wiring above) | [Racing Helmet](https://hypixelskyblock.minecraft.wiki/w/Racing_Helmet) |
| "Restores +5 Health every second while worn" | GHAST_HEAD (1) | **DONE** (0.26.1's; it was 1% of max health) | [Ghast Head](https://hypixelskyblock.minecraft.wiki/w/Ghast_Head) |
| Stats doubled on the End Island | END (Ender Armor, 4) | **LATER** the End | [Ender Armor](https://hypixelskyblock.minecraft.wiki/w/Ender_Armor) |
| Seismic Wave 1 s shorter a piece | SUPER_HEAVY (4) | **DONE** the Tank's Seismic Wave asks (`SetBonuses.seismicWaveCut`) | [Super Heavy Armor](https://hypixelskyblock.minecraft.wiki/w/Super_Heavy_Armor) |
| +10 Defense for each Zombie within 8 blocks | ZOMBIE_HAT (1) | **APPROX** counted each second; a Zombie as for Training (UNKNOWN: a question for the owner) | [Zombie Hat](https://hypixelskyblock.minecraft.wiki/w/Zombie_Hat) |
| Less from and more to Magmatic mobs | FLAMING_CHESTPLATE, MOOGMA_LEGGINGS, TAURUS_HELMET (3) | **LATER** Magmatic mobs (none here yet; the hooks would do it) |  |
| Stats on the Crimson Isle | RAMPART (4) | **DONE** each piece's +50 Health, +20 Strength, +15 Crit Damage on the Crimson Isle server | [Rampart Armor](https://hypixelskyblock.minecraft.wiki/w/Rampart_Armor) |
| Mist Aura (2 pieces): ghosts' damage x0.6 | SORROW (4) | **LATER** the Dwarven Mines' ghosts |  |
| "Grants immunity to explosion damage" | CREEPER_HAT (1) | **DONE** vanilla explosions ("only applies to vanilla Minecraft explosions", the wiki) | [Creeper Hat](https://hypixelskyblock.minecraft.wiki/w/Creeper_Hat) |
| "Also reduces fall damage by 5%" | CHICKEN_HEAD (1) | **APPROX** vanilla fall damage 5% less; its eggs when sneaking are only a look (not done) | [Chicken Head](https://hypixelskyblock.minecraft.wiki/w/Chicken_Head) |
| Arrows: 20% chance to explode for 50 base Magic Damage within 8 blocks | SKELETON_HAT (1) | **APPROX** the hat's magic, the text's numbers, on the mobs around the one hit; its Intelligence scaling UNKNOWN (1) | [Skeleton Hat](https://hypixelskyblock.minecraft.wiki/w/Skeleton_Hat) |
| +1 Speed per 20 Obsidian in the inventory; immune to Wither | OBSIDIAN_CHESTPLATE (1) | **APPROX** Obsidian and Enchanted Obsidian (the wiki), counted each second; no Wither effect; its "Bonus Speed" line stays the data's (lore isn't built from the holder's inventory) | [Obsidian Chestplate](https://hypixelskyblock.minecraft.wiki/w/Obsidian_Chestplate) |
| "Grants immunity to knockback from mobs" | SLIME_HAT (1) | **DONE** | [Slime Hat](https://hypixelskyblock.minecraft.wiki/w/Slime_Hat) |
| Jump Boost while worn | RABBIT_HAT (IV), BALLOON_SNAKE (II, necklace) | **DONE** the best of theirs, given again every second | |
| "Grants immunity to de-buffs while equipped" | COW_HEAD (1) | **DONE** no harmful potion effect is given them | |
| 2x stats on their Catacombs floor | GOLD_ and DIAMOND_ BONZO to NECRON_HEAD (14) | **APPROX** the head's stats (as in the run) once more on the floor its text names, Master Mode's too (UNKNOWN) | |
| "Magic Find: +5% / All Other Stats: -5%" | CLOVER_HELMET (1) | **APPROX** Magic Find x1.05, the rest x0.95 but Breaking Power ("most other Stats", the wiki: which UNKNOWN) | [Clover Helmet](https://hypixelskyblock.minecraft.wiki/w/Clover_Helmet) |
| Stats a Farming level | FARMER_BOOTS, RANCHERS_BOOTS, ENCHANTED_JACK_O_LANTERN (3) | **APPROX** +2 Defense and +4 Speed (the Jack o' Lantern's +2 Defense, +4 Health) a Farming level; the Garden's Farming Fortune LATER | |
| "Reduces damage taken by 6%" | DOJO_WHITE to DOJO_BLACK_BELT (6) | **DONE** 0.5% to 6% off every hit (traps' too) | |
| "Heal 25❤ every 1s while worn" | ANNIHILATION_CLOAK, DESTRUCTION_CLOAK (15) | **DONE** | [Annihilation Cloak](https://hypixelskyblock.minecraft.wiki/w/Annihilation_Cloak) |
| "Deal 1.15x damage against ♨ Infernal Mobs" | DEMONLORD_GAUNTLET (1) | **DONE** multiplicative (the Hub's Magma Cube is Infernal) | |
| "Sets your Health Regen and Mending to 0, and you can no longer receive healing" | LAVA_SHELL_NECKLACE (1) | **APPROX** both stats 0 after the set bonuses (an accessory's after it can add some back); no healing at all LATER (every heal would ask: UNKNOWN which Hypixel stops) | [Lava Shell Necklace](https://hypixelskyblock.minecraft.wiki/w/Lava_Shell_Necklace) |
| "+5☘ Mining Fortune while mining Mithril" | MITHRIL_ and TITANIUM_ NECKLACE, CLOAK, BELT, GAUNTLET (8) | **DONE** as Dwarven Metal Fortune (Mining Fortune on Mithril, the only such block here) | |
| "upgrades the Aspect of the Dragons with +35❁ Damage +50❁ Strength" | DRAGONFUSE_GLOVE (1) | **APPROX** the stats while it's held; its "Very reduced ability knockback" is the ability's (not done) | |
| The rest of armor's own text | SPRING_BOOTS, SEA_LANTERN_HAT, farming helmets, TIKI_MASK, KALHUIKI_MASK, FLEX_HELMET, KRAMPUS_HELMET... | **LATER** Feather Falling (the armor enchantments part's, Spring Boots), air (Sea Lantern Hat), fishing, the Rift, gifts |  |
| The rest of equipment's own text | ANCIENT_CLOAK, SOULWEAVER_GLOVES, FLAMING_FIST, SCOVILLE_BELT, SCOURGE_CLOAK, DELIRIUM_NECKLACE, SHRIVELED_BRACELET, PELT_BELT, ENDER_ and DRAGONFADE_ pieces, gemstone equipment, fishing and Rift equipment... | **LATER** abilities' damage by their cost (Ancient Cloak), the Haunted Skull (Soulweaver), fire on players and ignited mobs, Magic Find by mob type, the Mushroom Desert's bounds, the End, the Crystal Hollows, fishing, the Rift |  |

## UNKNOWN and approximations

Each is a one-line comment with UNKNOWN where it's decided in the code.

- Hits: the skeleton sets' arrow damage, Bloodrush's +10% and the Sniper Helmet's are taken as
  additive (the Sniper Helmet's in whole 2-block steps); Octodexterity counts landed melee hits only;
  Kuudra stacks count before the hit that gains one, and Fervor's come from arrows too, not from
  abilities; auras (Blaze, Unstable Blood) deal their damage as it is, each mob's most on its own.
- Mixed variants of one set: Kuudra's lowest tier (the wiki); the Tuxedos' cheapest; Dungeon Lord a
  quarter of each piece's own; Reaper's damage part only with three Reaper pieces.
- Counting nearby wearers (Shoal, Armor of the Pack) includes the wearer; the Stone to Steel
  Chestplates cover their wearer too, and of several wearers only the most counts.
- Bat Person's "Combat Stats" are the Stats menu's Combat group (as recorded), what Superior Blood
  multiplies; the Mender helmets' Mending and Vitality aren't grown by the run's stat boost.
- Shadow Assassin's kills are while the set is worn, no cap; Dungeon Lord's minutes are the run's,
  no cap; Efficient training uses the Catacombs level for stats (at most 50).
- Stat order: bonuses' stats come with the other modifiers (the classes'), before the blessings;
  Vindicate, Radioactive, Absolute Unit, Protective Blood, Superior Blood and the Tuxedos work from
  the stats so far. Health-based ones (Young Blood, Protective Blood) read the health being worked out.
- Wither and Crypt Witherlord pieces add up (40% for four Wither pieces); Radioactive, Vindicate and
  Absolute Unit use whole steps.
- Lore: an inactive tiered header with some pieces worn is the data's dark gray (the wiki's example
  shows grey); Hydra Strike's numbers round to one decimal, halves to even (matches the data's 2.8
  and 4.2).
- Strong Blood's +5 Strength lasts as long as the cast's speed; the Aspect of the Void doesn't get it.
- Armor of the Pack's +5 True Defense a piece counts always, as its text reads (the wiki has it
  against Animal mobs only).
- Task 80's: Intimidate's 10 blocks and nearest mobs; Bouncing Arrow's 10 blocks and the hit's damage again;
  Regenerative Howl's heal (the wiki's, not the text's) and a stack a Ferocity strike; Refraction's 12.5% at light 0
  and the light at their head; Starpower's range as how far Starfall goes; Battalion counting the wearer; Fearsome's
  10 and 8 blocks; a Zombie as a vanilla zombie (Training, the Zombie Bulwark, the Zombie Hat); the Bulwarks'
  "Maxed!"; Riches' counter counting all Scavenger coins; Arcane Energy's magic damage as ability hits that don't
  crit; Spirit's four others as the first four; Static Charge's damage as additive; Rekindle's parts added up and
  its seconds on fire starting again; Dominus's swipe at most once a stack's time, 5 blocks long, its Additive
  Multiplier the base one; Hydra Strike's side arrows once a shot, 10 degrees apart; Deepness Within's 6 until four
  pieces; Fireproof as lava only; the Skeleton Hat's scaling of 1; the boss heads in Master Mode; the Clover
  Helmet's "most other Stats".

## LATER, by what they wait for

- Equipment slots are wired (storage/Equipment): Bloodrush, the Adaptive Belt, Arachne's equipment and the
  Mithril and Titanium sets count; the Gemstone sets wait for the Crystal Hollows.
- Counts on equipment (kept in storage, which has no way to put a piece back yet): the Vanquished pieces
  (Blaze Consumer, Ghast Blaster, Glowing, Magma Cube Absorber), and their mobs or blocks (Blazes, Ghasts,
  Glowstone); Skeletor's (no Skeletor mob); Yog Armor's Absorb (no Yogs).
- Witherborn's wither (its explosion damage is on no page: a question for the owner).
- Abilities: the Fervor Chestplate's Ground Pound (the stacks are kept for it), the Aurora Chestplate's Homing
  Missiles (Arcane Energy's stacks), the Hollow Wand (Spirit's), the Grappling Hook (Bat Powers Activate!): the
  abilities part's.
- Nether Lord's obsidian over lava (a change to the world's blocks: a question for the owner).
- Fishing, farming and the Garden, the Rift, the Crimson Isle, Magmatic mobs and Kuudra, Jerry's
  Workshop, the Spooky Festival, Mythological mobs, pets, necromancy, soulflow, Heat, the Mining Spread
  stat, fairy souls, the End: the bonuses named for them above.
- The armor enchantments part's Feather Falling (Old Blood's, the Spring Boots'), to merge.
