# Enchantments and the Hex's Enchantment Table

Every enchantment Hypixel has (156, 26 of them ultimate), on items and in the Hex: its categories Enchantments and
Ultimate Enchantments, the "The Hex ➜ Enchant Item" pages and Bottles of Enchanting (task 70; the Hex itself is
HEX.md). The code is in `paper/src/main/java/net/icxd/dungeons/item/enchanting` (the table and the enchantments on
items) and `hex/enchant` (the Hex's pages and rules), with the two categories in `hex/category`.

The data is Hypixel's, so it's private: the data repository's `hex/enchantments.json`, made by
`tools/hex/build_enchants.py` from NotEnoughUpdates-REPO (the in-game enchanted book dumps and
`constants/enchants.json`) and the wiki (`Module:Enchantment/Data`, `The Hex/UI/Weapon` and `/Armor`). Its README
has the format, the sources and which wins where they disagree; the generator writes `enchantments_report.md` next
to it with every disagreement. servermgr links `hex/` into every server as `plugins/dungeons/hex`.

The screens come from the wiki's The Hex/UI tabs, NEU's Hex, SkyblockAPI and Lunar Client, as HEX.md's; there's no
recording of the Hex. Where the sources say nothing the code says UNKNOWN and takes the simplest reading.

## Enchantments on items

- **The table.** `EnchantmentData` is the private file: for each enchantment its name now (Syphon shows as Drain,
  Dragon Hunter as Gravity, the `*_mana` ones as "... Vitality"), whether it's ultimate, the levels the Hex offers
  and those the Enchantment Table sells, the Exp levels and Enchanting level each costs, what it goes on, what it
  conflicts with, and each level's text (one paragraph for item lore, and the book's own lines for the Hex). It's read
  with the Hex's tables at startup, off the main thread (the Enchantments category adds it to `HexData`); when it
  comes in, the inventories of anyone already online are made again. Until then, and without the file, no
  enchantment shows on items.
- **`EnchantmentType`** is now an id that asks the table (it was 76 Java constants). So every enchantment exists as
  soon as the table is in. The only constant left is `SCAVENGER` (KillCoins uses it).
- **Ids.** Items store `{name, lvl}` as before. The name is Hypixel's id, but an ultimate's without its `ultimate_`,
  as the plugin always stored One For All (`one_for_all`). Both forms are read everywhere (the lore, stats,
  `Combat.enchantments` for damage), so an item carrying Hypixel's `ultimate_one_for_all` works too. The Hex writes
  levels as ints, as Hypixel does; the shorts `/addenchantment` writes still read.
- **Lore** is unchanged: the same layout (`ItemBuilder`) and, for the 76 enchantments the plugin had, the same text
  at every level (checked level by level against the old `enchantments.json`; `GoldenItemsTest` passes with the
  private table). Only the made-up text of levels Hypixel doesn't have is gone: Vicious I-II, Big Brain I-II,
  Tabasco I, Delicate I-IV (they started at III, III, II and V).
- **Stats** still come from the text (`EnchantmentType.getStats`), now also "Grants A and B." and a line ending
  without its gray code. New ones that grant stats that way: Absorb, Divine Gift, Pesterminator, Forest Pledge, Ice
  Cold, Respiration, Scuba, Sunder and ten Turbo enchantments (their crop's fortune; not at IV and V, whose text
  goes on "Requires Bronze in a Carrot Contest!": see Later). None of the 76 old ones reads differently (checked on
  every level).
- **Effects in code** keep going by id: Damage (Sharpness, Smite, Bane of Arthropods, Ender Slayer, Cubism,
  Smoldering, Gravity, Giant Killer, Prosecute, Execute, Titan Killer, First Strike, Triple-Strike, Power, Snipe,
  One For All), Scavenger (KillCoins), Champion (SkillGains), Overload (Shots), Growth, Protection, Sugar Rush and
  True Protection (DragonSets). The rest are text (see Later).
- **Public data removed**: `paper/src/main/resources/enchantments.json`, `tools/items/gen_enchantments.py` and
  `tools/items/data/enchant_values.json` (the new generator takes each level's values from the wiki's module).

## The Hex

**Categories** (`Enchantments`, `UltimateEnchantments`). No requirement. Each is for every item one of its
enchantments goes on: an enchantment goes on the item kinds its books' "Applied To" names, mapped onto the plugin's
types (`EnchantmentData.is`: "Armor" is the four pieces, "Tools" pickaxes, drills, axes, shovels, shears, gauntlets
and farming tools, "Hoe" and "Farming Tool" the farming tools, "Vacuum" vacuums, a name like "Precursor Eye" that
item). So weapons, armor, equipment, tools, fishing rods, and wands for Ultimate Wise.
- Summary: `  &7Enchantments &e<n>&7/&a<max>`: n is how many of the listed ones are on the item, max how many can be
  at once (`EnchantRules.atOnce`): one of each group of listed ones that all conflict with each other, which is
  every group but one; of Silk Touch, Fortune and Smelting Touch two, since only Silk Touch conflicts with the others.
  The wiki's 26 for a sword is that rule on its 34 enchantments of 2022 with Sharpness, Smite and Bane of Arthropods
  conflicting (tested). They don't conflict now (the books; 933 live items have all three), and a sword lists 35
  today (Pyroclasm is new), so a sword shows 29 now. The count turns green once it's full (the official screenshot's
  "10/10").
- Ultimate: `  &7Ultimate Enchantments &e<n>&7/&a1`.

**The Hex ➜ Enchant Item** (`EnchantItemPage`), the wiki's Weapon and Armor tabs: the header (Enchant Item) in 28,
the item in 19, 15 enchanted books a page row by row with the page arrows, Experience Bottles in 50, the Sort in 51,
Go Back "To The Hex". Both categories open it, with their own list. Each book: `&a<Name>` (an ultimate `&d&l<Name>`),
its lowest level's lines as its book has them, the next tier's line for a tiered one ("&850k Combat XP to tier up!",
the wiki's Champion), `  &c<Name>&c ✖`, "&eClick to view!". It opens the level page.
- Order: the wiki's for a sword (its three pages) and its ultimates, and armor's ultimates; every other enchantment
  after those, by name (UNKNOWN: a bow's or armor's own order).
- Sort: Default, Missing Enchantments First, A to Z, Z to A, cycled, the one in use "&b▶ " (the wiki's); a new sort
  goes back to page 1. The sort and page are kept through the level page and back.
- Not offered: Telekinesis (Hypixel replaced it with auto-pickup in 2022; items that have it keep it).
- 48 is glass: Bookshelf Power is gone from SkyBlock since 0.26.1.

**The Hex ➜ Enchant Item ➜ <Enchant>** (`EnchantLevelPage`): one enchanted book a level, lowest to highest, row by
row from 12, and the item's own level too if it's past them (Efficiency X from a Silex, 56 live items; a level set
with `/addenchantment`), so it can be taken off; the header, Experience Bottles in 50, no Sort, Go Back "To Enchant
Item" (to the list as it was). A book: `&a<Name> <Roman>` (ultimate `&d&l`), the level's lines, what it would take
off the item, then the Cost block (HexCosts: `&3<N> Exp Levels`) and what a click does.
- **Clicking** (`EnchantRules.choose`): the item's own level takes it off, for the same Exp levels (the wiki's
  Enchantments), a higher one upgrades it, a new one goes on. What conflicts with it comes off; an ultimate takes the
  other ultimate's place; One For All goes with no other enchantment ("Removes all other enchants"): it takes every
  other off, and any other takes it off (of 119 live items with it, none has another but the retired Telekinesis,
  where a third of swords have Champion). The data keeps each other enchantment as stored, a changed level in place,
  a new one last.
- **Levels offered**: from the lowest that exists (Counter-Strike from III, Vicious from III...) to the highest: the
  books', or a tiered enchantment's last tier (Champion, Absorb, Compact, Cultivating, Expertise, Hecatomb,
  Toxophilite to X), so Sandbox can set a tier.
- **Normal profile**: it needs the Enchanting level the wiki gives it (to put one on, not to take one off), in the
  plugin's requirement line (`&4❣ &cRequires &aEnchanting Skill 20&c.`); it takes the Exp levels from their vanilla
  levels; and it gives Enchanting XP for them, 3.5 X^1.5 (the wiki's Enchanting), also when taking one off. A level
  above what the Enchantment Table sells (every level of one the table doesn't sell) needs its enchanted book too, as on
  Hypixel: the plugin has no enchanted book of each enchantment yet, so the book's line shows ✖ with "You don't have
  that in your inventories!" and it can't be had (Later).
- **Sandbox profile**: free, any level offered, no requirement, no book, and no Enchanting XP (nothing was spent).
- Chat: "You applied a &9Sharpness VI to your <item>!" (the core's, which SkyHanni matches); taking one off, "You
  removed &9Sharpness VI from your <item>!" (ours).

**Bottles of Enchanting** (`BottlesPage`, the wiki's General tab): the four Experience Bottles in 21, 22, 24, 25 (the
wiki's centred four), header "Consume Experience Bottles" in 28, Go Back "To Enchant Item" (to the page it came
from). Each: "Grants &3<orbs> &7experience orbs.", "Your Exp Level" and "Level When Applied" (Minecraft's levels),
the Cost block (the bottle). A click drinks one of theirs (inventory, then storage) and gives its orbs as vanilla
experience. Orbs: 8, 1,500, 250,000, 500,000, 5% more a level of Enchanting, rounded down (the wiki's Experience
Bottle table: 8 at level 1 and 2, 9 at 3, 23 at 38). Sandbox: free.

## Approximated (UNKNOWN in the code)

- **An enchantment on the item** in the list (U6): `  &a<Name> <Roman>&a ✔`, the absent form in green.
- **Level books** (U8, U9): their order (lowest first from 12), their name's colour (green, as the Enchantments
  Guide's), the whole lore, and the action lines: "Click to enchant!", "Click to upgrade!", "Click to remove!".
- **A lower level than the item's** (U10): not offered: "This item has a higher level!" (NEU hides these as a "Bad
  Level"); take the higher one off first.
- **What's replaced**: "&cReplaces Life Steal, Mana Steal" under the level's text.
- **The enchanted book's cost line**: "&9Enchanted Book (Sharpness VI) &c✖", in the book's rarity's colour.
- **Not enough Exp levels** (U11): the core's "You don't have enough Exp Levels!". Taking one off: our message.
- **Exp costs**: NEU's, the wiki's where NEU has none. They disagree on 25 levels (NEU's used): Champion I (10 vs
  25), Critical VII (200 vs 100), Gravity I-V (50-250 vs 10-30: NEU's are Dragon Hunter's), Frost Walker (10/20 vs
  15/30), Growth VI-VII (95/199 vs 100/200), Impaling (30-50 vs 10-20), Power VI (95 vs 100), Quantum III (100 vs
  50), Small Brain III (100 vs 60), Drain IV (45 vs 50), Tabasco II (500 vs 300), Thunderlord I-VI (20-200 vs
  9-91). No source has 50 levels' (the Turbo enchantments' VI-VII, Karma, Rainbow II-III, Fortune IV, Experience V,
  Scavenger VI, the fishing ones' last...): they cost no Exp levels. All of them are above the Enchantment Table's
  levels, so a Normal profile can't have them anyway.
- **Which items are which kind**: "Fishing Weapon" (no item has that type), "Hoe" (Harvesting's; taken as the
  farming tools), "Tools" (the wiki's Enchantments' pickaxes, drills, axes, shovels and shears, and the gauntlets and
  farming tools live items have Efficiency and Silk Touch on), a Carnival Mask (NEU gives it a chestplate's).
- **The Experience Bottles button and the bottles' header** without their Bazaar lines; the bottles' action "Click to
  consume!" (Hypixel's is the Bazaar's).
- **Orbs** rounded down (the wiki's table); the screen's example (19 for 8) is some other multiplier.
- **Conflicts**: the books' Conflicts lists (no pair they name is on more than one live item). NEU's pools are
  older and have more, which live items carry together (Fortune and Smelting Touch on 131, the Turbos with each
  other on dozens), so they're only in the generator's report.
- **Highest levels**: the books' where the wiki says more (Efficiency X is Silex's; Drain VI, Life Steal VI,
  Vampirism VII, Karma VI, Scuba VI, Forest Pledge VI in the wiki's module only).

## Later, and what it waits for

- **Enchanted books of each enchantment** (the NEU `ENCHANTMENT;LEVEL` items, the Bazaar's): levels above the
  Enchantment Table's need them on Normal profiles; none can be had until they're items (then the cost is an `Items`
  part with the book).
- **The Bazaar**: buying a missing book or bottle.
- **Collection Exp discounts** (25% for some enchantments at a collection tier; the private collections data has
  them): the Hex charges the full Exp levels.
- **Anvil combining and the Enchantment Table block** (the Hex's Enchant Item is the only way to enchant).
- **Tiers that grow with use** (Champion's Combat XP...): the tier is only what's set, but Compact's, which grows by
  the blocks its tool mines (`StackingEnchants`, STATS_EFFECTS.md).
- **Effects** of the enchantments that are text only here, and the system each waits for:
  - Combat hooks (damage against a mob type, on-hit, on-kill, projectiles): Impaling (Aquatic), Pyroclasm
    (Magmatic), Woodsplitter (Woodland), Knockback, Punch, Flame, Piercing, Dragon Tracer, Thorns, Reflection, Tidal,
    Toxophilite (and its Crit Chance, which the text parsing doesn't read after "Gain ..."), Duplex, Rend, Inferno,
    Combo, Swarm, Fatal Tempo, Soul Eater, Chimera (pets), Legion, Last Stand, No Pain No Gain, Transylvanian, Bank
    (death), Refrigerate and Ultimate Wise (per-item mana costs; `Abilities` works per player), Wisdom (vanilla levels),
    Ultimate Jerry (Aspect of the Jerry), Habanero Tactics (wands, Slayer weapons).
  - Mining, foraging, farming and fishing (Looting, Chance, Luck, Experience, Efficiency, Compact, Flowstate, Aqua
    Affinity, and Lapidary's and Expertise's first stats work: STATS_EFFECTS.md): Silk Touch, Smelting Touch, Lapidary's
    Mining Speed, Paleontologist, Rainbow, Petalfall (Timber), Karma, First Impression and Missile (axes), Feast, Sunset,
    Crop Fever (Overbloom), Dedication (Garden milestones), Green Thumb (visitors), Bug Blender (vacuums), Turbo-Rose,
    -Sunflower and -Moonflower (fortunes with no plugin stat), Flash, Quick Bite, Bobbin' Time, Depth Strider, Frost
    Walker, Stealth (Timid mobs).
  - Others: Quantum (weekdays), Small Brain (a negative Intelligence the parsing doesn't read), The One (maxed
    collections), Great Spook (Great Spook Armor).
- **Efficiency VI-X** (Silex): no text (the book's text has no numbers to put the wiki's in). Nor Thorns V,
  Respiration V, Feather Falling XX or Sharpness X, which a few live items have past the books.
- **Jacob's Farming Contests**: Turbo IV and V "Require Bronze / Silver in a <crop> Contest"; with no contests here
  their text grants no stat (the parsing reads none from a text that goes on), though III and VI do.
- **Stats in longer texts**: Reflection's "+2 Intelligence" and "+1 True Defense" come with an effect after them, so
  the parsing reads neither.
- **Three glyphs with no classic symbol** (tools/items' table): Bug Blender's and Pesterminator's Pest (E018),
  Petalfall's (E02E), Stealth's Timid (E088): dropped from their text.
- **`/addenchantment`** checks nothing still (the rules aren't a one-liner there); it now finds an enchantment by any
  of its names and replaces one stored under Hypixel's id.

## Questions for the owner

- Exp levels aren't kept with the profile (they're the server's vanilla levels), so the Hex's Exp costs and the
  bottles depend on the server the player is on. Should the profile keep them?
- A sword shows "Enchantments 0/29", not the wiki's 26 of 2022: Sharpness, Smite and Bane of Arthropods don't conflict
  now (the books, and 933 live items) and Pyroclasm is new. Right?
- Tiered enchantments (Champion...) are offered up to their last tier (Sandbox can set any). On Hypixel only tier I is
  sold. Keep?
