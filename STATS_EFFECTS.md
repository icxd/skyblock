# Stats, drops, XP and mining effects

Task 80's part for stats and what raises drops, experience and mining: every stat the plugin has (`stats/Stat.java`)
and the ones Hypixel has that it doesn't, the enchantments on drops and experience (Looting, Chance, Luck,
Experience), Mithril mining (Mining XP, Mithril Powder, and the mining enchantments), and Stats Tuning. The hooks it
builds on are the core's (EFFECTS.md). The code:

- `mining/`: `BlockListener` (what a block broken gives), `MiningTools` (Efficiency, Aqua Affinity), `Compact`,
  `Flowstate`; `MinableBlock` and `blocks/MithrilBlock` say what a block gives.
- `dwarven/Powder`: powder on the profile.
- `mob/DropEnchants` (Looting, Chance, Luck on `Mobs.addDropChance`); `Mobs.chance` and `Mobs.copies`.
- `economy/ExpBonuses` (Experience, Conjurer on `ExpOrbs.addBonus`).
- `item/enchanting/StackingEnchants` (stacking enchantments' counts and tiers, for any of them); `EnchantmentType`
  reads what the effects need from an enchantment's text (a level's percent, its next tier, "Gain" stats).
- `storage/StatsTuning`, `StatsTuningMenu`.

Numbers come from the enchantment's own text wherever it has them (Looting's "by 15%", Compact's "0.25% chance" and
"100 blocks to tier up!", Flowstate's "+1⸕ Mining Speed", "10s" and "200 blocks"). The rest are the wiki's
(hypixelskyblock.minecraft.wiki, pages named after the thing) and the mods' (SkyHanni, Skyblocker, NEU), each
named in the code. Where the sources are silent the code says UNKNOWN and takes the simplest reading; each is
listed at the end.

DONE: works as the text and wiki say. APPROX: works, with a reading marked UNKNOWN. LATER: waits for the system
named.

## Mining (the Dwarven Mines' Gray Mithril)

Each block broken, after its drops (with their fortune):

- **Mining XP**: 45, Mithril's "Base Mining XP" (the wiki's Dwarven Metals list, a column of blocks: so per block
  broken, not per Mithril its fortune drops), times 1 + Mining Wisdom / 100 (`SkillGains.give`). It shows on the
  action bar as every skill's XP does.
- **Mithril Powder**: 1 (the wiki's Mithril Powder: "Gray Wool: 1"), into the profile's `dwarvenMines.powder.MITHRIL`,
  which the Heart of the Mountain menu shows; at most 2 billion (the same page). What they hold raises it by its
  text: the Mithril Drills' "Grants +20% Mithril Powder." (+40% for the SX-R326; `Powder.mined`), the share of one
  powder being the chance of one more (UNKNOWN how Hypixel gives it). APPROX. Its other raises (Core of the
  Mountain, Powder Buff, Sky Mall, 2x Powder, Fallen Stars, pets) wait for the Heart of the Mountain's perks, events
  and pets. The Gemstone Drills' "+5% Gemstone Powder" reads the same way and waits for gemstone blocks.
- **HotM XP**: none. The wiki's Heart of the Mountain gives HotM XP only for commissions, mining events and
  nucleus runs.
- **Compact** and **Flowstate** count it (see the enchantments).

Breaking a block checks the player's Breaking Power stat, so a reforge's (Scraped's +1) counts as the tool's lore
says. Mining Speed is theirs (tool, armor and all), Aqua Affinity's rate on top.

## Enchantments

| Enchantment | Status | What it does here, and sources |
|---|---|---|
| Looting | DONE | Each drop's own chance times 1 + its text's percent ("by 15%" at I: the wiki's "BaseDropChance × (1 + 0.15 × LootingLevel)"), before Magic Find, whose 5% rule goes by what that makes it ("Looting applies BEFORE Magic Find": Looting V makes a 3% drop 5.25%, and Magic Find doesn't apply). Past 100% the drop drops again, the rest being the chance of once more (the wiki's "40% chance to obtain 2 and 60% chance to obtain 4"). It's the killer's held item's when the mob dies ("the death is attributed to the player holding the weapon with Looting"). No slayers or pets drop here, which it skips. |
| Chance | APPROX | Bows: the same text and the same as Looting, from the held item (UNKNOWN: the wiki says it of Looting). |
| Luck | APPROX | "Increases the chance for Monsters to drop their armor by 5%" a level: on drops whose items are all armor (the Rotten, Heavy and Skeleton Grunt pieces), times 1 + 5% a level, as Looting's (UNKNOWN: the wiki's Luck gives only "+5%"), from the held item. It multiplies with Looting. |
| Experience | DONE (mobs), LATER (ores) | The held item's: at its text's chance (12.5% a level) a kill's experience orbs are doubled (+100% on ExpOrbs' bonuses, which add up). Ores too, but no ore here gives experience: Gray Mithril gives none (UNKNOWN: no source says it does), and the vanilla ores wait for their blocks. |
| Efficiency | DONE | On a pickaxe, drill or gauntlet everywhere but the Hub (the wiki's "on other islands": here the Dungeon Hub, the Catacombs, the Crimson Isle and the Dwarven Mines), +30 Mining Speed at I to +110 at V (the wiki's table, 10 + 20 a level; the book's text has no number), in the item's stats and lore, whose text there is live lore's "Grants +110 ⸕ Mining Speed." (lore follows where the item was last made, as on Hypixel's auction house). LATER: the Hub's vanilla breaking speed (no block breaking there), VI-X (Silex, which isn't an item here). |
| Compact | APPROX | Its Mining Wisdom is its text's stat, now read ("Gain +8☯ Mining Wisdom", live lore's "Mining Wisdom: +8" at VIII). Each block broken with it counts on the tool (`compact_blocks`, Hypixel's key: a live drill's data) and tiers it up at its text's next tier ("100 blocks to tier up!"; the wiki: "Stacking Enchantment"), with a message (UNKNOWN: Hypixel's words). Each block rolls its text's chance for one Enchanted Mithril, the wiki's "next material" of Mithril (UNKNOWN whether it's what Hypixel gives), on top of the drop (UNKNOWN; no fortune on it), and says "&6&lCOMPACT! &fYou found an &9Enchanted Mithril&f!" (SkyHanni's words for another block; the colours UNKNOWN). Lore: the count after its name while there's a tier to go ("&9Compact VIII &8202,861") and the next tier's line after its description, as live items have them (any stacking enchantment's). |
| Flowstate | APPROX | Its text's Mining Speed (+1 at I) for each block of a streak, up to its 200, while they hold a tool with it, through their stats (the Stats menu shows it). The streak is blocks broken with a Flowstate tool; it ends its text's 10 s after the last, and starts again in another world (the wiki's Mining Speed: "after mining 200 blocks in the same world"). Holding another tool doesn't end it (the wiki's Flowstate). Blocks broken with another tool don't count (UNKNOWN; SkyHanni's Flowstate helper counts that way). |
| Aqua Affinity | APPROX | The wiki's "+100%" mining rate: Mining Speed doubled for the break while their head is under water. UNKNOWN whether SkyBlock's own mining is slower under water without it (vanilla's is); it isn't here. |
| Fortune | DONE | Mining Fortune from its text, on Mithril (as before). |
| Scavenger | DONE | As before (KillCoins). |
| Champion | DONE (Combat XP) | As before (SkillGains). Its coins and orbs are the weapon enchantments' part (ENCHANTS_WEAPONS.md). Its tier-up by Combat XP isn't counted: `StackingEnchants.addHeld(player, "champion", xp)` is the call for whoever builds it (see the questions). |
| Cultivating | DONE (stats), LATER (the rest) | Its "Gain +1☯ Farming Wisdom and +2☘ Farming Fortune" are now read as stats, as live lore counts them. Its counter and using the stats wait for farming. |
| Prismatic (`pristine`) | DONE (stat), LATER | Its Pristine shows and sums; gemstone blocks use it. |
| Lapidary | DONE (stat), LATER (the rest) | Its Gemstone Fortune is read as a stat, the first of its two "while mining Gemstones", as live lore counts it in the tool's stats (59 of 62 live Lapidary tools; the rest show none); its Mining Speed isn't in live lore's stats and waits, with using the fortune, for gemstone blocks. |
| Expertise | DONE (stat), LATER (the rest) | The same reading: its Sea Creature Chance, not its Fishing Wisdom "when killing Sea Creatures", as live lore counts it. Its counter and the rest are ENCHANTS_WEAPONS.md's, waiting for fishing. |
| Smelting Touch, Silk Touch, Paleontologist | LATER | Vanilla ore, log and sand blocks; the Glacite Mineshafts. |

## Other things that raise experience and stats

- **Conjurer**, the Enchanting skill's perk ("Gain 5% more experience orbs from any source" a level; the wiki's
  Experience: "+5% XP per level, up to a maximum of +300% at level 60"): on every orb grant (kills now). DONE. The
  Hex's bottles already counted it.
- **Stats Tuning** (Loadouts' Stats Tuning, the wiki's Maxwell/UI, its newer tab): the eight stats, a point each
  giving +5 Health, +1 Defense, +1.5 Speed, +1 Strength, +1 Crit Damage, +0.2 Crit Chance, +0.3 Attack Speed and +2
  Intelligence (the menu's "Per point"; Attack Speed's +0.3 is the newer tab's, NEU's and Skyblocker's profile
  viewers', the older tab says +0.2). Right-click puts a point in, shift for 10; left-click takes one out (UNKNOWN:
  the wiki's shows a stat with none). The points are on the profile (`statsTuning`) and the Accessory Bag adds their
  stats; with fewer points than they've put in, only as many count, in the menu's order (UNKNOWN). Loadouts'
  "Unassigned Points" follow. APPROX: the templates (four, "First template is now free" since 0.26, the rest 10,000
  Bits) are LATER; the crit stats' skulls are plain (their skins are UNKNOWN); Go Back goes to Loadouts (Hypixel's
  to the Accessory Bag Thaumaturgy, which isn't here).

## Stats

The Stats menu (`/stats`) lists each category's stats that aren't 0, in the recorded order; every stat below that
works shows there. The recording's Mining Spread and Charm Chance aren't stats here (see the next table).

| Stat | Status | What it does here, or what it waits for |
|---|---|---|
| Health | DONE | Max health (PlayerHealth); Stats Tuning's +5 a point |
| Defense | DONE | Damage × 100 / (100 + Defense); Stats Tuning's +1 |
| True Defense | DONE | True damage (Stormy's lightning is the only one) |
| Damage | DONE | (5 + Damage) in melee and bow hits |
| Strength | DONE | × (1 + Strength / 100); Stats Tuning's +1 |
| Crit Chance | DONE | The crit roll, over 100 Overload's mega crits; Stats Tuning's +0.2 |
| Crit Damage | DONE | Stats Tuning's +1 |
| Attack Speed | DONE | Invulnerability and shortbow cooldowns, the cap and its raises (`Combat.addAttackSpeedCap`; Newton's Demise is BONUSES.md's); Stats Tuning's +0.3 |
| Ferocity | DONE | |
| Swing Range | DONE | |
| Intelligence | DONE | Mana and spell damage; Stats Tuning's +2 |
| Ability Damage | DONE | |
| Health Regen | DONE | |
| Vitality | DONE | |
| Mending | DONE | |
| Breaking Power | DONE | Which blocks they can break, the tool's with its reforge's now. Glacial mobs' ×150: LATER (Glacite) |
| Mining Speed | DONE | Break time on Gray Mithril, with Efficiency's and Flowstate's |
| Pristine | LATER | Gemstone blocks |
| Mining Fortune | DONE | More drops, 100 a drop |
| Ore Fortune, Block Fortune, Gemstone Fortune | LATER | Ores, blocks and gemstone blocks (each needs only its block's `fortune()`) |
| Dwarven Metal Fortune | DONE | On Mithril. Titanium: LATER (the block) |
| Bonus Pest Chance, Farming Fortune, the ten crop fortunes | LATER | Farming and the Garden |
| Fishing Speed, Sea Creature Chance, Double Hook Chance, Trophy Chance, Treasure Chance | LATER | Fishing |
| Sweep, Foraging Fortune, Fig, Mangrove and Helix Fortune | LATER | Foraging |
| Hunting Fortune, Pull, Tracking | LATER | Hunting |
| Speed | DONE | Walk speed and its caps; Stats Tuning's +1.5. The Racing Helmet's +400 is only a lore line in the item data (LATER: the items' data; see the questions) |
| Magic Find | DONE | × (1 + Magic Find / 100) on drops under 5%, after Looting, Chance and Luck |
| Pet Luck | LATER | Pets (its formula is in, as before) |
| Fear, Heat Resistance, Cold Resistance, Pressure Resistance | LATER | The Great Spook, the Crystal Hollows, Glacite, Galatea's diving |
| Respiration | DONE | |
| Combat Wisdom | DONE | Kills' Combat XP |
| Mining Wisdom | DONE | Mithril's Mining XP; Compact gives it |
| Enchanting Wisdom, Carpentry Wisdom | DONE | The Hex's Enchanting XP and crafting's Carpentry XP (the core's per-skill Wisdom) |
| Farming, Fishing, Foraging, Alchemy, Runecrafting, Taming, Social, Hunting Wisdom | LATER | Their skills' XP: farming, fishing, foraging, brewing, runecrafting, pets, visiting, hunting |
| Rift Time, Hearts, Rift Damage, Intelligence and Speed (Rift), Mana Regen | LATER | The Rift |
| Weapon Ability Damage | DONE | The plugin's own (an item's spell damage), not Hypixel's |

### Hypixel's stats the enum doesn't have

| Stat | Status | What it waits for |
|---|---|---|
| Mining Spread | LATER | Nothing gives it on Mithril here: its sources are the HotM perks (not built), Lustrous "during Mining Fiesta" (no events), Fleet "when mining Blocks" and Mineralworks "on Ores" (neither block is here) |
| Gemstone Spread | LATER | Gemstone blocks |
| Overbloom; Sunflower, Moonflower and Wild Rose Fortune; Crop Fortune | LATER | Farming, the Garden and its contests |
| Timber, Toughness | LATER | Foraging and Galatea |
| Charm Chance | LATER | Hunting |
| Global Wisdom | LATER | Potions (the Celestial Mason Jar) |
| Crux Fortune | LATER | The Rift |

### Other stat-like things

| Thing | Status | Notes |
|---|---|---|
| Stats Tuning | APPROX | Above; templates LATER |
| Absorption | DONE | The core's (`session/Absorption`) |
| Accessory (Magical) Power, Mana | DONE | As before |
| Mithril Powder | DONE (earned), LATER (spent) | Mining gives it; the Heart of the Mountain menu's upgrades aren't made yet (a click names the perk), so nothing spends it |
| HotM XP | LATER | Commissions, mining events, nucleus runs |
| Stats only in an item's lore | LATER | The data keeps 40 items' stats as a lore line because Hypixel's API lacks them (the data's report); most such lines are reforge stones' texts, the real ones (the Racing Helmet's Speed +400, the Clover Helmet's Magic Find, the Emperor accessories', the Freshly Baked ones', the Kuudra cores') need the item data |
| Effective Health | COSMETIC | Not shown (the stat breakdown pages aren't built) |
| Soulflow and Overflow Mana; Heat, Cold and Pressure | LATER | Soulflow; the Crystal Hollows, Glacite, Galatea |

## UNKNOWN (and the reading taken)

- Mining XP per block broken, not per Mithril dropped: the Dwarven Metals list gives it by block.
- Whether Mithril gives experience orbs: it doesn't (no source says so).
- A drill's powder raise on 1 powder a block (1.2 with +20%): the share of one is the chance of one more.
- Compact: which enchanted item (Enchanted Mithril, the wiki's next material); on top of the drop; no fortune on it;
  its message's colours; the tier-up message's words.
- Flowstate: blocks broken with another tool don't count; another world starts a new streak.
- Aqua Affinity: SkyBlock's mining isn't slower under water; with it, twice as fast there.
- Luck multiplies as Looting does (1 + 5% a level), on drops whose items are all armor; it multiplies with Looting.
- Chance and Luck are the held item's when the mob dies, as the wiki says of Looting.
- A drop past 100%: each copy is the drop's amount again (its own roll of min to max).
- Efficiency grants Mining Speed on every server but the Hub.
- Stats Tuning: points past what they have count in the menu's order; a left click takes a point out; "You have"
  doesn't count the tuning's bonus (shown after it: "+ 50 ❤", SkyHanni's pattern); "Stat has" stays red; Attack Speed
  +0.3 a point.

## Questions for the owner

1. **Mithril Powder can be earned but not spent**: the Heart of the Mountain menu's upgrades aren't made (a click
   names the perk). Should they be? And the mining islands' sidebar shows powder ("᠅ Mithril: 35,448", SkyHanni's
   patterns), but where in the sidebar isn't recorded: add it?
2. **Stats Tuning's templates** (four slots; the first free since 0.26, the others 10,000 Bits each): build them?
3. **Stats that are only lore lines in the item data** (the Racing Helmet's +400 Speed and a few more): fix them in
   the item generator (the private data) or per item in code?
4. **Champion's (and Toxophilite's, Expertise's, Hecatomb's) tier-ups**: `StackingEnchants.addHeld` counts and tiers
   any stacking enchantment, as Compact's does; the weapon and armor enchantment parts own those. Wire them there?
