# Weapon enchantments

What the enchantments of swords, longswords, gauntlets, bows and fishing weapons do, the weapon ultimates, and the
reforge bonuses that act in a fight (task 80, the weapon enchantments part; EFFECTS.md has the hooks and who owns
what). Every enchantment on a weapon is in this document, done or not.

The code:
- `paper/src/main/java/net/icxd/dungeons/item/enchanting/weapon`:
  - `WeaponEnchants`: the effects, on the core's hooks. These are hit listeners, landing buffs, the kill event, shot
    listeners, the knockback event, Infinite Quiver's shot and Rend's left click.
  - `WeaponStats`: what goes through the stat pipeline. That's the stats a text grants (Tabasco, Toxophilite), what
    the lore doesn't list (Ultimate Jerry), Fatal Tempo's Ferocity, and Ultimate Wise's mana (cost and lore).
  - `EnchantText`: the numbers read from the texts. `WeaponRules`: the sums, with no Bukkit in them.
    `WeaponEnchant`: the ids and an item's levels.
- `paper/src/main/java/net/icxd/dungeons/reforge/CombatReforges`: the reforge bonuses.
- Small hooks in shared files:
  - `EnchantmentType.getStats` and `ItemStats` call `WeaponStats`.
  - `ItemBuilder` calls it for Ultimate Wise's Mana Cost lines.
  - `SkillGains.combatXpPercent` adds Toxophilite's XP.
  - `Hits.addMagicMultiplier` is for Loving.
  - `SetBonuses.addTeleportListener` is for Hyper.
  - `Damage`: Gravity VI's 40%.

Tests (pure, no server):
- `EnchantTextTest`, `WeaponRulesTest` (the wiki's worked examples), `WeaponStatsTest`, `WeaponEnchantTest` and
  `CombatReforgesTest`.
- `PrivateWeaponTextTest` reads every level's text in the private data (skipped without it). It checks each has its
  numbers where the code takes them.
- Cases added to `SkillGainsTest` and `DamageTest`.

## How they work

- **Numbers come from the texts.** Each enchantment's numbers are read at runtime from its level's text in the
  private `hex/enchantments.json`, the text Hypixel shows. They are read by where they stand in it; see "Numbers"
  below.
  - "&7Deals &a5% &7of your damage dealt to other monsters within &a3.3 &7blocks" gives 5 and 3.3.
  - "50k", "1.5m" and "1M" are thousands and millions.
  - The numbers are read once per level and kept until another table comes in.
  - A constant a text lacks is in the code with its source: Flame's 3% a level, Inferno's 5 s, Vampirism's and
    Venomous's dungeon factors.
  - Without the table no enchantment has text, so none does anything (as items show none then).
- **What counts is the weapon the hit was dealt with.** That's the held weapon for a melee hit, and the bow the arrow
  left for an arrow (`Combat.Landing.weapon`). A kill uses what they hold when it dies, as the Book of Stats and
  Champion's XP already did.
- **Which hits count.**
  - The melee ones count melee hits and the bow ones count arrows.
  - Ferocity strikes don't count: the wiki's Ferocity says they "don't trigger Life Steal or Drain". The one
    exception is Fatal Tempo, whose wiki page says "Each Ferocity strike count as an individual hit".
  - Abilities' hits count for none (UNKNOWN; the core's rule for effects).
  - An effect's own damage (Cleave's share, lightning, a damage over time) sets off nothing (the core's `HitKind`).
- **Counts per player on each mob.** Thunderlord's every 3rd hit, Inferno's every 10th, Fanged's every 7th and Rend's
  arrows are each player's own hits on that mob (UNKNOWN whether Hypixel counts everyone's).
  - Champion's "2nd hit on a mob" is the mob's 2nd hit from anyone, as First Strike counts them.
  - Venomous stacks "globally" on the mob. Lethality is one debuff on the mob from every player's hits.
  - What's kept on a mob is forgotten when it goes (`EntityRemoveEvent`), and a player's when they leave.
- **Damage.** An effect's damage is dealt through `MobHits.deal` (health, number, kill and drops for the player, the
  run's damage dealt). It is rounded down as hits are, and a share under 1 isn't dealt.
- **Items' counts and stores** are kept under Hypixel's own keys, as live items carry them: `champion_combat_xp`,
  `toxophilite_combat_xp` and `ultimateSoulEaterData` (`ItemCounters`). Nothing is written until something counts, so
  the golden test is untouched.
- **Blood-Soaked** (a cloak reforge): "Increase the enchantment effects of Life Steal, Vampirism and Drain by 1
  level". A worn piece with it adds its text's number to those three levels.
  - Past the books' last level (Life Steal VI + 1), the amount goes on growing by the level, as their texts do. Life
    Steal VII is 16.8, the wiki's history of Life Steal.

## Enchantments

Status: DONE, APPROX (built, with an UNKNOWN reading), LATER (and what it waits for), or another part's.

### Damage (these were done before this part)

| Enchantment | Status | Notes |
|---|---|---|
| Sharpness, Smite, Bane of Arthropods, Ender Slayer, Cubism, Smoldering, Giant Killer, Titan Killer, Prosecute, Execute, First Strike, Triple-Strike, Power, Snipe, One For All | DONE | `Damage.enchantment` (the books' values). Bane of Arthropods and Ender Slayer wait for mobs of their types (no Arthropod or Ender mob yet). Bane of Arthropods IV's lore shows "0%": its text in the private data has "&20%" (a colour code eats the 2) where the code, NEU and the wiki have 20% (see the questions). |
| Gravity (dragon_hunter) | DONE | VI now gives the book's 40%. It reused Smoldering's five levels, so VI gave 30%. |
| Overload | DONE | Crit stats from the text, and the mega crit (`Shots`). |
| Critical, Vicious, Divine Gift | DONE | Stats read from the text. |
| Impaling | LATER: Aquatic mobs | No mob here is Aquatic. |
| Pyroclasm (magmarizer) | LATER: Magmatic mobs | No mob here is Magmatic. |

### On a hit

| Enchantment | Status | What it does here, and what's UNKNOWN |
|---|---|---|
| Cleave | APPROX | On a melee hit, the text's share of the hit's damage to every other mob within the text's radius of the target's middle. It uses the book's radius (3.3-4.8), not the wiki's 3.5-6. UNKNOWN whether the other mobs' Defense counts: it doesn't. |
| Life Steal | APPROX | Heals the text's ❤ per melee hit on a mob, through `Heals` (so the Catacombs boost that "all healing" gets applies, UNKNOWN for enchantments). |
| Drain (syphon) | DONE | Gives the text's Vitality per melee hit, never past the pool, with the text's 1 s cooldown. |
| Mana Steal | APPROX | Gives the text's share of their mana pool per melee hit, in whole points, as dungeons' mana on hit does. UNKNOWN whether it's a share of the pool or of what they have: the pool, as "regain" reads. |
| Lethality | APPROX | A Defense debuff for the text's 4 s per melee hit, up to its 4 stacks. The level put on first is kept until it runs out ("Hitting that enemy with Lethality II or higher will not increase that 4.8%", the wiki's Damage Calculation). It is one debuff per mob from every player's hits (UNKNOWN). |
| Venomous | APPROX | Each melee hit saves its damage after additive buffs times the text's share, up to 40 stacks (from every player's hits, "stacking globally"), for 5 s from the last hit. Each second the mob takes what they saved times the Additive Multiplier worked out again without melee-only buffs (the wiki's examples 4 and 5), through its Defense, as a dark green number. There's also the text's slow, 80% weaker in the Catacombs (the wiki's trivia). UNKNOWN: the second multiplier is worked out when a hit puts the stacks on, not each second for what's held then as the wiki has it. UNKNOWN whether Defense counts for the tick. |
| Fire Aspect | APPROX | Fire for the text's seconds, each second the text's share of the hit's damage after additive buffs, through the mob's Defense, as a gold number (the wiki's example 4 and its damage indicators). Put on again, it keeps the greater and counts its time from then (example 5). It shows as flames, since 0.10 "displays fire particles instead of physically lighting the target on fire". UNKNOWN whether Defense counts (the wiki's examples are on a Training Dummy; its Toxic Arrow Poison example takes Defense off a damage over time). |
| Thunderlord | APPROX | On every 3rd melee hit on a mob, a lightning strike (its look and sound) for the text's share of that hit's damage, as a separate hit (the wiki's Multiplicative Sources, melee only). UNKNOWN whose hits count: each player's own on the mob. |
| Thunderbolt | APPROX | The same count, the text's share to up to 10 mobs within 2 blocks, the target too. |
| Knockback | APPROX | The melee hit's knockback pushes further, by vanilla Knockback's 0.5 strength for each 3 blocks the text says. UNKNOWN how Hypixel turns "3 blocks" into a push. |
| Champion | DONE | The mob's 2nd hit (the text's "2nd"; this one with the weapon) gives the text's coins into the purse and its exp orbs. The orbs go the way the mob's drops do (`ExpOrbs`: to them from a dungeon mob). Its Combat XP percent was done in `SkillGains`. Its tiers are under "Tiers". |

### On a kill

| Enchantment | Status | What it does here, and what's UNKNOWN |
|---|---|---|
| Vampirism | APPROX | Heals the text's ❤ on a kill with the weapon held, with the text's 0.5 s cooldown. It is 10 times less in the Catacombs (the wiki's trivia). UNKNOWN whether the Catacombs boost that `Heals` gives all healing also applies to it: both do. |

### Bows

| Enchantment | Status | What it does here, and what's UNKNOWN |
|---|---|---|
| Flame | APPROX | Fire as Fire Aspect's (the same fire, the greater stays), for the text's seconds (3 s at I; the wiki says 3.5 s), at the wiki's 3% of the shooter's damage after additive buffs a level per second (the text gives no share). It's worked out from the shooter as they were when the arrow left, as the arrow's own hit is (switching to a sword before it lands doesn't change it). UNKNOWN which damage Hypixel's takes. |
| Punch | APPROX | The arrow's knockback pushes further, by vanilla Punch's 0.6 a level for each 3 blocks the text says, the way the arrow flew. UNKNOWN as Knockback. |
| Piercing | APPROX | The arrow goes through 1 more mob after the first, which takes the text's 25% of the shot's damage (`Shots.pierce`). UNKNOWN how many: the text says only "Arrows travel through enemies", so it's vanilla Piercing I's one. |
| Infinite Quiver | DONE | The text's chance that a drawn bow's shot takes no arrow. Shortbows take none anyway. The Quiver itself is STORAGE.md's (LATER). |
| Toxophilite | DONE | Champion's Combat XP percents on a kill with the bow held (`SkillGains`). The text's Crit Chance ("Grants +3.7☣ Crit Chance" after its first sentence, which the stats reading missed) counts in the lore and the stats, as live items show. Its tiers are under "Tiers". |
| Chance | the stats part's | STATS_EFFECTS.md. |
| Dragon Tracer (aiming) | LATER: dragons | Arrows home to dragons; there are none. |

### Tiers (stacking enchantments)

| Enchantment | Status | What it does here, and what's UNKNOWN |
|---|---|---|
| Champion's and Toxophilite's tiers | APPROX | A kill's Combat XP (as the kill gives it: the mob's, with Combat Wisdom and the weapon's percent) is added to the held item's `champion_combat_xp` or `toxophilite_combat_xp`. The level goes up for each tier-up text it reaches ("&850k Combat XP to tier up!"; several at once if it has, the wiki's 0.19.7 fix), to the last tier. Only while their data is here (as the XP is), and not while their inventory is being handed on. UNKNOWN: what Hypixel says when it tiers up (nothing here). The lore's progress after the name ("§9Toxophilite VIII §82.1M", a Hypixel setting) isn't shown. |

### Ultimates

| Ultimate | Status | What it does here, and what's UNKNOWN |
|---|---|---|
| One For All | DONE | Before this part: `Damage`, and the Hex takes the others off. |
| Ultimate Wise | DONE | The item's abilities cost the text's share less mana (`Abilities.addItemManaCostFactor`), multiplied with the other factors. Their Mana Cost lines show it rounded as the cost is: a live Aspect of the Void with V shows 23 for 45 and 90 for 180. It's on any item with it (wands, tools and rods too). |
| Ultimate Jerry | APPROX | The Aspect of the Jerry's base Damage times 1 + the text's percent, in its stats but not its lore (a live Signature Edition with V shows its own "Damage: +2"). UNKNOWN whether the Signature Edition counts: it does. Not grown by a dungeon's boost (UNKNOWN; the item isn't a dungeon item). |
| Swarm | APPROX | An additive buff (the wiki's Additive Sources) of the text's percent for each mob that can be hurt within 10 blocks, up to 10. It works on melee hits and arrows. UNKNOWN: counted around the player, not the target. |
| Combo | APPROX | An additive buff (the wiki's Additive Sources) of the text's percent for each of their kills within the text's seconds, up to its kills. UNKNOWN: any kill of theirs counts, and each kill counts for its own window (not a window that each kill starts again). |
| Soul Eater | APPROX | A kill with it held stores the text's multiple of the mob's Damage on the item (`ultimateSoulEaterData`), in place of what it had, capped at the text's 1M outside a run. The next critical melee hit or arrow adds it as the wiki's "Add Damage" (only the crit multiplies it), and it's gone. An arrow gets it only while they hold the item with that soul on it, so it's added once: a shortbow's three arrows, or others shot before it landed, don't each bring it. UNKNOWN: the private data's book says "adds it at the end of your next critical hit (Max 1M outside Dungeons)", which this follows; live items and the wiki now say "adds it as Strength ... (Max 5k outside Dungeons)", and the wiki's history says the cap was removed in January 2026. A Ferocity strike repeats the hit's damage with it (UNKNOWN). |
| Fatal Tempo | APPROX | Each melee hit, arrow and Ferocity strike with it adds the text's percent Ferocity, up to 200%. It lasts 3 s after the last one (a `PlayerStats` modifier while they hold it, so the Stats menu shows it). UNKNOWN whether it goes past Ferocity's cap of 500: the cap still stops the strikes. |
| Inferno | APPROX | Every 10th melee hit or arrow of theirs on a mob roots it (can't walk) for 5 s. It deals the text's percent of that hit over those 5 s, a second at a time, as magic damage its magic resistance takes from (the wiki's Inferno: "counts as Magic Damage"; its 5 s). Flames show it. UNKNOWN how often Hypixel's ticks, and whether "trap" is more than rooted. |
| Rend | APPROX | A left click with the bow, if it's off its 2 s cooldown, pulls their arrows out of each mob within 10 blocks. Each arrow from this bow since the last Rend (up to 5) deals the text's percent of their last critical arrow on that mob (Rend's own crits only, the wiki's 2021 change). UNKNOWN: how far "nearby" is (10 blocks here), and whether arrows count that didn't stay in the mob (all that hit count). A Terminator's left click shoots too. |
| Duplex (reiterate) | APPROX | Each shot (a shortbow's arrows too) fires a second arrow at once with the first's aim and speed. It deals the text's share of the first's damage, with its own crit roll, and makes no third arrow. Mobs its arrows hit take the text's 1.1x from this part's fire (Fire Aspect, Flame) for 60 s. UNKNOWN: when the second arrow leaves. The text's 1.1x is followed; the wiki's Multiplicative Sources says the live multiplier is 0.1 (fire damage goes down, "might be a bug"). See the questions. |
| Chimera | LATER: pets | "Copies 20% of your active pet's stats": there are no pets. |
| Flash | LATER: fishing | Fishing rods and fishing weapons. |

### Fishing weapons and mining gauntlets

- **Fishing weapons.** Angler (its Sea Creature Chance is read as a stat), Blessing, Caster, Expertise (tiers by sea
  creature kills), Lure and Magnet are LATER: fishing. No item here is a "Fishing Weapon"; the weapon enchantments
  above work on one once there is.
- **Other parts' enchantments on these items.**
  - The Gemstone Gauntlet's mining ones (Compact, Fortune, Lapidary, Paleontologist, Prismatic, Smelting Touch,
    Flowstate) are the stats part's (STATS_EFFECTS.md).
  - So are Looting, Luck, Chance and Experience.
  - Scavenger was done before (`KillCoins`).
  - Telekinesis is retired (auto-pickup).

## Numbers read from the texts

The index of each number in its text, starting from 0 (`PrivateWeaponTextTest` checks every level):

| Enchantment | Numbers |
|---|---|
| Cleave | 0 share %, 1 radius |
| Life Steal | 0 ❤ |
| Drain | 0 Vitality, 1 cooldown s |
| Mana Steal | 0 % of mana |
| Thunderlord | 0 every Nth hit, 1 % |
| Thunderbolt | 0 every Nth hit, 1 %, 2 most mobs, 3 radius |
| Lethality | 0 Defense %, 1 seconds, 2 most stacks |
| Venomous | 0 slow %, 1 % per stack, 2 most stacks, 3 seconds |
| Fire Aspect | 0 seconds, 1 % per second |
| Knockback, Punch | 0 blocks |
| Vampirism | 0 ❤, 1 cooldown s |
| Tabasco | 0 Damage |
| Champion | 0 Combat XP %, 1 "2nd", 2 coins, 3 orbs |
| Toxophilite | 0 Combat XP %, 1 Crit Chance |
| Flame | 0 seconds |
| Piercing | 0 % |
| Infinite Quiver | 0 % chance |
| Inferno | 0 every Nth hit, 1 % |
| Fatal Tempo | 0 % per hit, 1 cap %, 2 seconds |
| Combo | 0 % per kill, 1 most kills, 2 seconds |
| Soul Eater | 0 multiplier, 1 cap outside dungeons |
| Swarm | 0 % per enemy, 1 radius, 2 most enemies |
| Rend | 0 % per arrow, 1 most arrows, 2 cooldown s |
| Duplex | 0 second arrow %, 1 fire taken x, 2 seconds |
| Ultimate Wise | 0 % less mana |
| Ultimate Jerry | 0 % more base damage |

## Reforges

What reforges do past their stats in a fight (REFORGES.md's "Later"). The numbers are the bonus text's at the item's
rarity, as its lore shows them.

| Reforge | Status | What it does here, and what's UNKNOWN |
|---|---|---|
| Fabled (Dragon Claw, swords) | APPROX | A melee crit's damage times 1 + 15% x a roll from 0 to 1 (the wiki's Multiplicative Sources, "ConfirmationNeeded"). |
| Suspicious (Suspicious Vial, swords) | APPROX | "+15" weapon damage in the stats, not the lore: live Twilight Daggers with it show their own Damage (REFORGES.md). Not grown by a dungeon's boost (UNKNOWN). |
| Fanged (Full-Jaw Fanging Kit, swords) | APPROX | Every 7th melee hit of theirs on a mob gets +100% as an additive buff. UNKNOWN: additive or multiplicative (the wiki lists it in neither; its other reforges' "+X% damage" are additive). |
| Precise (Optical Lens, bows), Headstrong (bows) | APPROX | An arrow that hits a mob's head gets +10% or +8% (additive, the wiki's Additive Sources). The head is from the mob's eyes, less as far again as its top is above them. UNKNOWN where Hypixel puts a head. |
| Loving (Red Scarf, chestplates) | DONE | A worn chestplate's +5% multiplies their magic damage ("a Multiplicative bonus to the player's Base Ability Damage", the wiki's Red Scarf), through `Hits.magic`. |
| Hyper (Endstone Geode, armor; "warped") | APPROX | "Gain +6✦ Speed for 5s after teleporting", from any teleport the set bonuses hear of (an ability's, an ender pearl's). Each worn piece's Speed adds up (UNKNOWN). |
| Empowered (Sadan's Brooch, armor) | DONE | +10 Mending a worn piece while in a run. |
| Blood-Soaked (Presumed Gallon of Red Paint, cloaks) | DONE | One more level of Life Steal, Vampirism and Drain (see "How they work"). |
| Withered, Ancient | DONE | Before this part (ItemStats: a stat a Catacombs level). |
| Coldfusion (Entropy Suppressor, the daggers) | LATER: pets | "Only if Wisp is equipped": there are no pets (nor fire pillars). |
| Spiritual (Spirit Stone, bows) | LATER: decoys | "a 10% chance to spawn a Spirit Decoy when you kill an enemy in a dungeon". The decoy "runs to the nearest mob, tries to damage it and takes aggro" (the wiki's Spirit Stone), and there is no decoy here. |
| Gilded (Midas Jewel, Midas items) | LATER: see the questions | "Upon killing an enemy, you have a rare chance to grant coins to a player around you". The wiki gives 1% and a payout table up to 50m coins. That's Hypixel's table (private data), and it hands out coins. |
| Chomp, Trashy (rods) | LATER: fishing | |

The armor part has the other armor and equipment reforges (Renowned, Perfect, Undead, Cubic, Blood Shot's cocoons, ...).

## Not built, and what it waits for

- **Enchanting-level gating** ("will be greyed out and will not work" without the Enchanting level, the armor
  inventory's X1). It is every enchantment's rule, weapons' and armor's, and changes the lore and the golden, so it
  isn't built here. See the questions.
- The mob types, pets, dragons, fishing and decoys named above.

## UNKNOWN (and the reading taken)

- Abilities' hits set off no on-hit enchantment. Ferocity strikes set off only Fatal Tempo.
- The hit counts (Thunderlord, Inferno, Fanged, Rend) are each player's own hits on the mob.
- Cleave's other mobs take the share as it is, with no Defense. The book's radius is used, not the wiki's.
- Fire Aspect's, Flame's and Venomous's ticks go through the mob's Defense. Venomous's second multiplier is worked out
  as a hit puts the stacks on. Flame uses the shooter as they were when the arrow left.
- Life Steal and Vampirism get the Catacombs boost `Heals` gives all healing, and Vampirism is 10x less there too.
- Mana Steal is a share of the pool, in whole points.
- Knockback and Punch: 0.5 and 0.6 more strength (vanilla's own per level) for each 3 blocks.
- Piercing goes through 1 more mob.
- Swarm counts mobs around the player. Combo's kills each count for their own window, and any kill counts.
- Soul Eater follows the data's book: added damage, 1M cap outside a run. Ferocity strikes repeat it.
- Fatal Tempo doesn't pass Ferocity's cap. It lasts the text's 3 s.
- Inferno roots for 5 s and ticks once a second.
- Rend reaches 10 blocks, and every arrow that hit counts.
- Duplex's second arrow leaves at once, rolls its own crit, and takes the text's 1.1x fire damage, not the wiki's
  reported 0.1.
- Ultimate Jerry counts for the Signature Edition.
- Champion and Toxophilite tier up silently.
- Fabled's roll is even from 0 to 15%. Fanged is additive. A mob's head is its top band. Hyper's pieces add up.
- Suspicious's and Ultimate Jerry's damage isn't grown by a dungeon's boost.

## Questions for the owner

1. **Duplex's fire damage.** The text says targets "take 1.1x fire damage". The wiki says the live multiplier is 0.1,
   so fire damage goes down ("might be a bug", August 2026). This follows the text. Should it copy the live bug?
2. **Soul Eater's text.** The private data's book says "adds it at the end of your next critical hit (Max 1M outside
   Dungeons)". Live items say "adds it as Strength on your next critical hit (Max 5k outside Dungeons)", and the
   wiki's history says the cap was removed in January 2026. This follows the data's book, which the lore shows.
   Should the book (the data) be updated to the live text, and the effect with it?
3. **Gilded** hands out coins from a payout table (1% a kill, 100 to 50m coins, the wiki's Midas Jewel). The table
   would have to be private data. Should it be built, and should Sandbox profiles give or get those coins?
4. **Enchanting-level gating** (enchantments that "will be greyed out and will not work" below their Enchanting
   level) is every enchantment's rule and changes lore and the golden. Which part should build it, or should it wait?
   The armor part left it too, so it would be built once, after both are merged.
5. **Bane of Arthropods IV's text** in the private data reads "&20%", which items show as "0%". Should the data say
   "&a20%", as its other levels do?
