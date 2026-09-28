# Reforges

Reforges as Hypixel has them: every basic reforge the Blacksmith rolls, every Reforge Stone, what each gives an item
at each rarity and its bonus, and the Hex's page to apply them, "The Hex ➜ Reforges" (task 70, the reforges part;
see HEX.md for the Hex). The reforges are Hypixel's numbers, so they're data kept out of this repository: the
private data's `hex/reforges.json`, made by `tools/hex/build_reforges.py`.

The code is in `paper/src/main/java/net/icxd/dungeons/reforge` (the reforges, read from the table) and
`paper/src/main/java/net/icxd/dungeons/hex/reforge` with `hex/category/Reforges.java` (the Hex's category and page).

## Done

**Reforges are data.** The four hard-coded reforges (Heroic, Withered, Ancient, Hasty) are gone; `ReforgeTable`
has the 136 in reforges.json: 50 basic ones in their 7 pools and the 86 stones' (NEU's 85 and the wiki's
Geometric). It's read with the Hex's tables (off the main thread, see HexData) by the Reforges category.
- **An item's reforge** is its `reforge` data, as before. It's read by Hypixel's modifier id (`withered`,
  `double_bit`), which is what the Hex writes, or by the old enum's name (`WITHERED`), in any case. One the table
  doesn't have (or with no table) keeps a name made from its data ("Double Bit") and adds nothing. Nothing throws
  on any name any more (ItemBuilder's and ItemStats' `valueOf` did).
- **Rarities.** A reforge gives its numbers at the item's rarity now (recombobulated too). Special and Very Special
  give Mythic's (the wiki's Reforging/Prices), which fixes "above Mythic gives 0". Divine gives nothing, but where
  the data has Divine numbers (mining tools and a few others), as the wiki's Reforging says: "Except for mining
  tools, reforges on Divine items provide no stat buff, only the reforge abilities".
- **Lore**, as live items show it:
  - The reforge's word before the name. On an item named like a reforge it's the wiki's other word instead ("Very
    Wise Dragon Helmet", "Extremely Heavy", "Not So Heavy", "Thicc Super Heavy"; live items agree).
  - `&9(+N)` after each stat it adds.
  - Its Breaking Power in the line under the name (live Scraped Gemstone Gauntlets: 9, the gauntlet's 8 and 1).
  - Its bonus section last, after the abilities and set bonuses: "&9Withered Bonus", or the bonus's own heading
    (Gilded's "&9Byron's Compassion &8(Gilded)"). An item above the data's rarities shows the nearest lower one's text.
  - "This item can be reforged!" goes once it has one.
- **Withered and Ancient** add their stat a Catacombs level (+1 Strength, +1 Crit Damage), as live items do (a
  Mythic Withered Dark Claymore: 170 and its owner's 36). Their level is the wearer's in the stats (`ItemStats`) and
  the item's owner's in the lore (as the dungeon boost is), capped at 50 (`catacombsStatLevel`). The Hex's preview
  uses the viewer's (the wiki's 147 Strength at Legendary is 135 and the editor's 12).
- **Which items**, by their Hypixel type (SkyHanni's ReforgeApi reads NEU's names the same way, `ReforgeStone.TYPES`):
  - Pools: sword and fishing rod (swords, longswords, gauntlets, rods), bow, armor (with Carnival Masks), equipment,
    pickaxe (pickaxes, drills, gauntlets), axe, farming tool.
  - A basic pool needs the item to be reforgeable (the item data's "This item can be reforged!").
  - A stone goes on its type's reforgeable items, at the rarities it has a fee for. Vacuums and fishing nets never
    say they can be reforged but take their stones (live vacuums have Beady and Buzzing, nets Sticky).
  - A stone of named items (Warped, Gilded, Coldfused, Erudite, Jerry's, Majestic, Geometric) goes on those alone.
- **The golden** doesn't change: no item there has a reforge.

**The Hex: Reforges** (no requirement; the Luxurious Spool's head). The button shows for the items a pool or a stone
is for (without the table, the items that say they can be reforged). Its summary is `  &7Reforge &c✖`, or
`  &7Reforge &a✔` and `    &9Fabled` (the official screenshot). "The Hex ➜ Reforges" is the wiki's Weapon tab:
- 28: the Anvil "&aApply Reforges".
- The grid: the stones that go on the item at its rarity now, centred and paged (HexPage). A sword has 6 at
  Legendary, an armor piece 23 (two pages).
- Each stone shows as its item, with:
  - its own lore up to what it gives (what it applies to, and its flavour);
  - "&9<Reforge> &7(<rarity>&7):" and what the reforge gives an item of that rarity (the stat colours as items have
    them now);
  - its bonus;
  - the Cost block: the fee at that rarity first, then the stone (HexCosts), and "&eClick to reforge!" or what's
    missing ("You don't have that in your inventories!", "You don't have enough Coins!").
- 48: the Anvil "&aRandom Basic Reforge", the wiki's lore, "&7Cost", its price at the item's rarity now (250 to
  50,000 Coins, the wiki's Reforging/Prices), "&eClick to reforge!". It shows when a pool is for the item. It rolls
  any reforge of the pool (all equally likely, the wiki) but the one the item has.
- Applying pays (checked first; what's missing is said in chat), sets the modifier id, makes the item again and
  saves (HexSession#upgrade). Then it says "&aYou reforged your <old name> &ainto a <new name>&a!" (SkyHanni's
  ReforgeHelper's words).
- On a Sandbox profile every stone that fits is there to apply without having it, and the stones and the random
  reforge are free ("&7Cost" / "&aFree").

## The data (private)

The plugin reads `plugins/dungeons/hex/reforges.json`: the data repository's `hex/`, which servermgr links into
every server. Tests read it from `-Dhex.dir` (`PrivateReforgesTest`; skipped without it). The file has:
- each reforge by modifier id: its name, stats by rarity (the plugin's Stat names), bonus lines by rarity, its
  heading if it isn't "&9<Name> Bonus", and a stat a Catacombs level;
- the pools;
- the stones: item, reforge, a type or item ids, and the fee by rarity;
- the random reforge's price by rarity;
- the duplicate prefixes.

`python3 tools/hex/build_reforges.py --neu NEU-REPO --wiki-cache DIR --live reforge_live.json --out DIR` makes it,
and `reforges_report.md` next to it, which lists every disagreement and what was taken. Its sources:
- NotEnoughUpdates-REPO: `constants/reforges.json` and `reforgestones.json`, and the stones' in-game dumps
  (`items/<STONE>.json`) for their Legendary bonus text.
- The wiki, fetched (or read from the cache): `Module:Reforge/Data`, `Reforging/Prices`, and `Reforging`'s
  Duplicate Prefixes table.
- `--live`: what reforged items on the live Auction House show, `reforge_live.json`. For each modifier and rarity
  it has how many items there were, each stat's `§9(+N)` values and how many items showed each, and the bonus
  sections as shown. `observe_reforges.py` makes it from the Sept 2026 pull of 44,726 auction items. It's
  Hypixel's, so it's kept with the private sources.

How they're put together (`tools/hex/test_build_reforges.py` has made-up cases of each):
1. NEU's numbers are the base; the wiki fills rarities NEU lacks, and Geometric (NEU doesn't have it). A stat NEU
   names at one rarity alone, where the wiki has the same number under the stat NEU gives at every other rarity, is
   NEU's slip: Ancient's Common Crit Damage 3 is the wiki's (and the plugin's old table's) Crit Chance 3.
2. Where most live items of a reforge and rarity show another number, that's taken. Where the wiki agrees with
   every such live number, its other rarities of that stat are taken too. That gives:
   - Groovy: half NEU's Foraging Fortune.
   - Fanged: half its Vitality.
   - Double-Bit and Green Thumb: the wiki's Speed.
   - Spicy: the wiki's Attack Speed.
   - Refined: the wiki's Mining Wisdom.
   - One rarity each: Blended Epic, Deadly Mythic, Grand Mythic, Rich (bows) Rare and Blessed Rare, where the wiki
     says NEU's number; Blazing Mythic and Lumberjack's Epic, where it says the live one.
3. A stat NEU gives that no live item shows (on at least 3 items) and the wiki doesn't give is left out: it's the
   bonus's text, not a stat. That's Suspicious's +15 Damage, Loving's +5% Ability Damage and Groovy's Foraging
   Wisdom. (Twilight Daggers with Suspicious show their base Damage.)
4. Withered's and Ancient's live brackets include the owner's Catacombs level, so they don't count as numbers.
5. Bonus text is what items show at that rarity, else the source that agrees with what they show (the wiki first,
   then NEU), else the wiki's or NEU's. NEU's Blood-Soaked, Buzzing, Squeaky, Undead, Spiritual and Scraped texts
   are out of date.
6. Fees: NEU's, the wiki's for rarities NEU lacks (Jerry's Epic, Earthy and Overpriced Mythic).

## Approximated (UNKNOWN in the code)

- **Numbers no item settles.** Where NEU and the wiki differ and no live item shows the reforge at that rarity, NEU's
  is taken. That's 41 stat cells (Rich on bows, Odd, Pure, Wise, Hasty Mythic, Rapid Mythic, Neat Mythic, Spiked
  Rare, Hyper Mythic, Magnetic and Fortified Divine, Toil Rare...) and 19 fees
  (Warped, Dimensional, Majestic, Perfect, Spiked, Necrotic, Fruitful, Undead, Sunny). Costs never show on items.
  Both lists are in reforges_report.md.
- **Firestone**: no source has its reforge, so it's not in the table.
- **Geometric**: only the wiki has it. Its modifier id `geometric` is the usual rule (no live item has it). It has no
  Common fee (Froggles start Uncommon).
- **Deep Fried**'s id `deep_fried` is SkyHanni's rule for NEU names without an id (no live item).
- **The Boo Stone** isn't in the item data, so the Hex doesn't show it.
- **Overbloom** (Thorny, Overpriced) isn't one of the plugin's stats: left out.
- **Unobtainable** items get Mythic's numbers, as Very Special does, and no random reforge (no price).
- **A gauntlet**'s random reforge: it's of both the sword-and-rod and the pickaxe pools' types. The first, sword and
  rod, is taken.
- **A Hoe** counts as a Farming Tool (no item is one).
- **The order of stones** on the page: by reforge name. The wiki's five sword stones are in no order we can tell.
- **A random reforge never rolls the one the item has.** Whether Hypixel rolls it again is unknown.
- **A stone of the reforge the item has** can't be applied: its action line and chat line are
  "&cThis item already has this reforge!" (our own words; whether Hypixel lets it be applied again is unknown).
- **A stone's action line** is "&eClick to reforge!" (U4: only the random button's is known). The chat line's
  colours are unknown, and so is a stat block for a rarity with no numbers (none is shown).
- **The preview's stat colours** are the ones items have now (the item data's stone lore). The wiki's older
  screenshot has red and green.
- **Hypixel's first reforge for 10 Coal** is the Blacksmith's. The Hex shows coins (the wiki's 250 Coins button).

## Later, and what it waits for

- **Bonus effects past a stat.** Only Withered's and Ancient's Catacombs stats work; every other bonus is lore
  only. Each waits for the system it touches:
  - combat damage: Fabled's crit bonus, Suspicious's +15 weapon damage, Fanged's 7th hit, Coldfused;
  - the stat totals: Renowned's +1% of all combat stats and Perfect's +2% Defense;
  - abilities: Loving's +5% ability damage;
  - Spiritual's decoys, Hyper's speed after teleporting, Empowered's Mending in dungeons;
  - mining, farming, fishing and pests: Ambered, Auspicious, Fleet, Glacial, Heated, Lustrous, Magnetic, Mithraic,
    Refined, Scraped, Stellar, Dimensional, Blessed, Bountiful, Buzzing, Beady, Mantid, Squeaky, Chomp, Calcified,
    Trashy, Geometric...
- **Stones' skill requirements** ("❣ Requires Mining Skill 30" on the Wither Blood) aren't checked when applying. It's
  unknown whether the Hex checks them.
- **The Blacksmith** and its Reforge Anvil, and reforging with a stone in an anvil: there's no Blacksmith yet.
- **Accessory reforges** (removed from SkyBlock in 2022): old items with one keep its name and get nothing.

## Questions for the owner

- A stone of the reforge the item already has can't be applied (it would change nothing but take the stone and the
  fee). Should it be allowed, as it may be on Hypixel?
- A random reforge never gives the item the reforge it has. Should repeats be possible?
- Stones need a skill level to apply on Hypixel (the Wither Blood Mining 30). Should the Hex check them on Normal
  profiles?
