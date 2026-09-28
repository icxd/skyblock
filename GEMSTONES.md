# Gemstones and the Gemstone Grinder

Gems on items (kept in the item's data, their stats, their lore), Geo's Gemstone Grinder (which puts
them in, unlocks slots and takes gems out again) with its Gemstone Guide, and the Hex's Gemstones
category, which opens the grinder with the item (the owner's wish: "I would also like to be able to
access the gemstone menu from The Hex, which isn't usually possible"). There's no Geo yet, so the Hex is
the way in.

The wikis only show the empty grinder (Geo/UI) and the guide. The rest comes from two screenshots of the
real menu with an item in it (Skyblock-Tweaks' README: a Hyperion's locked slots, a Ring of Power's
gems), Skyblock-Tweaks' reading of the real menu's lore, the four Replay Mod recordings (items with gems,
their data and lore; no grinder window), and two recreations (Swofty's HypixelRecreation, CarsCupcake's
SbRemake). The research is in the task's scratchpad (`hex/report_grinder.md`, `result_items.md` §6.22,
`result_data.md` §2.6).

The code: `item/gemstone` (`Gem`, `GemstoneQuality`, `GemSlots`, `GemstoneTable`; `GemstoneType` got
what a slot takes), `hex/gem` (`GemstoneGrinder`, `GemRemoval`, `GemPicker`, `GemstoneGuide`) and
`hex/category/Gemstones.java`. The table's generator is `tools/hex/build_gems.py`.

## Done

**The gem table** (`GemstoneTable`) is Hypixel's, so it's private: the data repository's
`hex/gemstones.json`, which the Hex's Gemstones category reads at startup (`HexData`, off the main
thread). `tools/hex/build_gems.py --neu NEU-REPO [--api ITEMS_API] [--wiki GEO_UI] [--out DIR]` makes it
from public sources (NEU's `constants/gemstones.json`, the items API's `museum_data`, and the set names in
the wiki's copy of the guide, Geo/UI); `test_build_gems.py` checks it.
It has each gem's stat and what one gives by quality and item rarity (Common to Mythic, Divine for Amber,
Topaz and Jade as NEU has them), the removal fee by quality (Rough 1 coin up to Perfect 500,000), the
chisel percentages and perk words, and the armour sets the guide shows once. **Citrine is doubled**: NEU has the values from before 0.23.3 doubled them (the same as
Peridot's); every Citrine value on the live auction house (11 cells) is exactly twice NEU's, so all of
them are doubled. Unlock costs aren't in it: the item data has them (`gemstone_slots[].costs`, the same as
NEU's on all 1015 slots). Until the table is read, or without it, gems show but give nothing.

**Gems on items** (`GemSlots`). Each entry of the item's `gemstone_slots` (`{locked, costs}` until now)
gets `gem` (the gem, "JASPER": a Combat slot takes several) and `quality` ("FINE") once a gem is in it.
Nothing else changes, so items made before keep working, and a new item's data is as it was. A slot is
open when it isn't locked or has a gem in it (Hypixel's older items have gems in slots never paid for).
`/unlock` unlocks the same way the grinder does, without the cost.
- **Stats.** A gem gives its stat at the item's rarity now (after recombobulating: the recorded
  recombobulated Shadow Assassin pieces have Mythic's), half on the Talisman of Power, Ring, Artifact and
  Relic of Power and the Shimmersparkle Chestplate (the wiki's Gemstone Slot). It's added in
  `ItemStats.of` before the dungeon factor, so a dungeon scales it with the rest (the recorded Shadow
  Assassin Helmet: 30 + 3 + 35 + 14 = 82, (82 - 3) × 3.48 = 274.92), and it counts in the Accessory Bag.
  A gem in a chisel's slot gives no stat (its Fossil Excavator perk is LATER).
- **Lore.** The gem's `&d(+N)` bracket (with the stat's unit, `&d(+5%)`) comes after the reforge's `&9`
  and before the dungeon's `&8`, and the gem counts in the total: `&7Strength: &c+82 &9(+35) &d(+14)
  &8(+274.92)`. The `Gemstones:` line: a locked slot `&8[✎]`, an open one `&8[&7✎&8]`, one with a gem the
  slot type's icon in the gem's colour between brackets in the quality's (Rough &f, Flawed &a, Fine &9,
  Flawless &5, Perfect &6): a Fine Jasper in a Combat slot is `&9[&d⚔&9]`. Items without gems render as
  before (the golden is unchanged).

**The Gemstone Grinder** (`GemstoneGrinder`), 6 rows of black glass titled "Gemstone Grinder":
- The item in 13, put in and taken out as the Hex's item is (the core's input slot: click, number key,
  shift-click; the item is in one place at all times, and the session passes it between the grinder, the
  Hex and the grinder's own screens). Only items with gemstone slots: "Only items that can have Gemstones
  applied to them can be put in the Grinder!" (both recreations' words).
- With nothing in it, the gray "&dGemstone Slot" panes of the wiki in 28-34 and 38-42. With an item, its
  slots in its order, centred in the fourth row with the middle left empty for an even count: 1 in 31;
  2 in 30, 32 (the Hyperion screenshot); 3 in 30-32; 4 in 29, 30, 32, 33 (the Ring of Power screenshot);
  5 in 29-33; 6 in 28-30 and 32-34; 7 in 28-34; the Relic of Power's 12 in 28-34 and 38-42.
- Close in 49, the Gemstone Guide in 50 (the wiki's torch). Opened from the Hex, Go Back in 48 (&7To The
  Hex) takes the item back to the Hex's main menu.
- **A locked slot**: `&c✎ Sapphire Gemstone Slot` (red whatever the type, the screenshot), the recreations'
  words, the gems a special slot takes, then the cost: `&7Cost`, `&6250,000 Coins`, `&5✎ Flawless Sapphire
  Gemstone &8x4` (no x1), a blank line and `&eClick to unlock!`. A click pays it all or nothing (coins from
  the purse, gems and items from the inventory and Storage, as the Hex pays) and opens the slot; what
  they lack shows in place of the click line (`&cYou don't have that in your` / `&cinventories!`,
  `&cYou don't have enough Coins!`: no Bazaar) and is said on a click.
- **An open slot**: named in its colour, a pane of its colour, and what to do (the recreations').
- **A slot with a gem**: the gem's own item, then `&7Cost to Remove`, the fee and `&eClick to remove!`.
- **Putting a gem in**: click a gem in their own inventory (any quality): the first open, empty slot that
  takes it gets one, for nothing. The gem leaves the inventory in the same moment the item gets it.
- **Taking one out**: the click asks first (`GemRemoval`: Confirm, the gem, Cancel). Confirm pays the fee
  and gives the gem back (their inventory, else their stash) in the same save; a gem its slot no longer
  takes comes out free. Removing never locks the slot again.
- **Sandbox**: no costs at all (the cost blocks say `&7Cost` / `&aFree`), and a click on an open slot
  picks a gem for it (`GemPicker`, our own: the slot's gems across, the five qualities down, seven gems
  a page), put in free without having it.

**The Gemstone Guide** (`GemstoneGuide`), "(1/9) Gemstone Guide": the torch in 4, 28 items a page in rows
2-5, columns 2-8, Previous and Next Page in 45 and 53, Go Back to the grinder in 48 (with the item) and
Close in 49. Every item with slots (233 entries today), each as it's made new, then `&7Available Gemstone
Slots` and its slot types, the same ones together (`  &6⸕ Amber &8x2`). An armour set shows once, as its
first piece with slots (its helmet), by the name the wiki's copy gives it (`&6Divan's Armor`, `&6Goldor's
Armor`, `&6Aurora Armor` for each Kuudra tier, `&5⚚ Adaptive Armor`); the Shimmering Light Armor shows
piece by piece, as there. Sorted by the item's own name, not its set's, as the wiki's copy is (the Helmet
of Divan under H, the fragged pieces last).

**The Hex's Gemstones** (`hex/category/Gemstones.java`): for items with gemstone slots, Carpentry 25 on a
Normal profile (the core's gate), the Perfect Ruby Gemstone button, the summary `  &7Gemstones &8[✎]
&9[&d⚔&9]` (the lore line's glyphs), and a click opens the grinder with the item.

## Approximated (UNKNOWN in the code)

- **Locked glyph.** Hypixel writes `&8[&8✎&8]`; the plugin keeps its `&8[✎]`, which looks the same, so
  items without gems render exactly as before.
- **Above Mythic.** Divine has values for Amber, Topaz and Jade only; the other gems take Mythic's there,
  and every gem takes Mythic's at Special and Very Special. No source has these.
- **A gem its slot no longer takes** (the item's slots changed) shows, gives no stat, and comes out free.
  A gem in a slot the item no longer has at all stays in its data, unseen. A locked slot whose cost the
  data no longer has unlocks for nothing ("&aFree").
- **Halving** keeps halves (a Fine Jasper on a Mythic Ring of Power, 3.5). The Shimmersparkle Chestplate is
  matched by name: it isn't in the item data yet, so its id is unknown.
- **Slot places for 8 to 11 slots** (no item has them): the fourth row full, the rest centred in the fifth.
- **Slot items.** The materials (a gray pane for a locked slot, Swofty's; a pane of the slot's colour for
  an open one, both recreations'): the screenshots' texture pack hides Hypixel's. The words above a locked
  slot's cost and an open slot's lore are the recreations'.
- **Coins first** in an unlock cost (the screenshot's Hyperion and Swofty's), though the data lists some
  slots' gems first.
- **Which slot a gem goes in** when several could: the first in order (the recreations').
- **Messages**, all ours or a recreation's: "You cannot apply that to this item!" (Swofty's, for a gem no
  open slot takes); "You unlocked the ✎ Sapphire Gemstone Slot!", "You applied a ❁ Fine Jasper Gemstone to
  your ...!" (the Hex's line), "You removed the ... from your ...!", and no sounds.
- **The removal's confirmation**: title "Remove Gemstone", 3 rows, Confirm (green terracotta) in 11, the gem
  in 13, Cancel (red terracotta) in 15, and their words: none of it is known.
- **Without the table** a gem can't be taken out on a Normal profile ("You can't remove Gemstones right
  now!"): the fee isn't known.
- **The guide.** Which pieces are one set: the Museum's sets, else the pieces' ids without the piece
  (Kuudra's tiers, most Perfect Armor tiers). Sets newer than the wiki's copy are named from their ids
  ("Abyssal Armor", "Perfect Armor - Tier XII"). The fragged sets' "⚚" is the item data's character for
  the copy's 26.x glyph. The sort key beyond the item's name (the wiki's copy has some Mk. I to III tools
  out of order). Aquamarine's icon is ☂, as its gems' names have it (the wiki's copy of the guide has α).
  The guide's items leave out the random attributes a new item rolls.
- **The Hex's button words** ("Apply Gemstones to your item at the Gemstone Grinder!") are the core's own.
- **Stored gems** are plain gem and quality: Hypixel keeps some as `{uuid, quality}`, so a gem that comes
  out is a new item (an unstackable one gets a new uuid).

## Later, and what it waits for

- Chisel perks: a gem in a chisel's slot is kept and shown, but its Fossil Excavator perk (the table has
  its percentage and words) waits for the Fossil Excavator.
- Geo, the Dwarven Mines NPC: `GemstoneGrinder.openFor(player)` opens the grinder on its own for him.
- Gem stats that other systems read (Mining Speed, Pristine, Mining/Farming/Foraging Fortune, Fishing
  Speed) count in the stats already; what they do waits for mining, farming, foraging and fishing.
- Hypixel's own Hex gem page ("The Hex ➜ Gemstones"), whose layout isn't known.

## Private files

- `hex/gemstones.json` in the data repository (servermgr links it as `plugins/dungeons/hex/`), made by
  `tools/hex/build_gems.py`. Tests read it through `-Dhex.dir` and skip what needs it without it (also
  when the folder has only the other categories' tables).

## Questions for the owner

- Should Sandbox's gem picker stay (clicking an open slot picks any gem of the slot's in any quality, free),
  or should Sandbox players use gems from their inventory like everyone else?
- The removal's confirmation menu is our own design (Confirm / Cancel). Keep it, or remove at once?
