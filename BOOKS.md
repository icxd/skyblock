# The Hex: Books and Item Upgrades

Two of the Hex's seven categories (see HEX.md), and the item rules under them: the books that go on items, stars
on every item, and items made dungeon items. The Hex code is in `paper/src/main/java/net/icxd/dungeons/hex/category`
(`Books`, `ItemUpgrades`), `hex/book` and `hex/upgrade`; the item rules in `item/upgrade` (`Book`, `Stars`,
`BookOfStats`) and `item/DungeonItems`.

Sources: the wiki's The Hex and its UI tabs (Weapon, Armor), the official 0.14 screenshot of the Hex's pane, NEU's
custom Hex (which parses the real menus), Lunar Client's per-menu slot lists, Malik's UI on the wiki, and the live
auction house (44,726 items, September 2026) for which items carry each book and how their lore shows it. The
research is in the task's scratchpad (`hex/result_hexui.md` §7 and §10, `result_items.md` §6, `result_data.md`
§2.2 and §2.5). Where they say nothing the code says UNKNOWN and takes the simplest reading.

No private tables of their own: the books' text, star costs and conversion costs are already in the private
`items/items.json` (`lore`, `upgrade_costs`, `dungeon_conversion_cost`). The few numbers the rules need (a book's
cap and what it adds) are single constants in `Book`, as the potato books' were.

## The books (`item/upgrade/Book`)

| Book | Data key | On | Cap | Adds (each) | Bracket |
|---|---|---|---|---|---|
| Hot Potato Book | `hot_potato_books` (Hypixel's `hot_potato_count`) | weapons, armor | 10 | weapon +2 Damage +2 Strength; armor +4 Health +2 Defense | `&e(+N)` |
| Fuming Potato Book | the same count, from 10 | the same | 5 more | the same | the same |
| Book of Stats | `stats_book` (kills) | weapons, farming tools | 1 | "&fKills: &6N" after the abilities | none |
| The Art of War | `art_of_war` | weapons | 1 | +5 Strength | `&6[+5]` |
| The Art of Peace | `art_of_peace` (Hypixel's `artOfPeaceApplied` can't be a key here) | armor | 1 | +40 Health | `&c[+40]` |
| Farming for Dummies | `farming_for_dummies_count` | farming tools, vacuums | 5 | +1 Farming Fortune | `&a(+N)` |
| Bookworm's Favorite Book | `bookworm_books` | vacuums | 5 | +20 Damage | `&6(+N)` |
| Polarvoid Book | `polarvoid` | drills | 5 | +10 Mining Speed; +5 Mining Fortune once there's one | `&9[+N]` |
| Wet Book | `wet_book_count` | fishing rods | 5 | +1 Fishing Speed | `&b(+N)` |

- "Weapons" for books are what live items carry them on: swords, longswords, bows, axes, the gauntlet and fishing
  rods; never a wand (0 of 213 live wands). Armor is the four pieces. Farming tools and vacuums are only known by
  their Hypixel type (`FARMING_TOOL`, `VACUUM`).
- Fixed: the potato books and The Art of War gave nothing on axes, the gauntlet and rods. They no longer count on
  wands (or the fishing weapons, which no item is): no live wand has them.
- Each book's stats are in both `ItemStats.of` and `ItemBuilder.statLines`, which read `Book.bonuses`; the brackets
  follow the live order (potato books, The Art of War or Peace, then the rest, then the reforge's).
- The Book of Stats counts a kill of a SkyBlock mob (`SkyBlockMobDeathEvent`) for the item the killer holds when it
  dies, if it has the book (`BookOfStats`, registered in `Dungeons`); the item is made again so its line shows it.

## Stars (`item/upgrade/Stars`)

- **Every item**: each star adds 2% of the item's own stats, with no bracket (live: the Crimson Chestplate's 230
  Health is 234.6 with one star, 257.6 with six). Every star counts, to 15. Nothing for Swing Range (as before),
  Health Regen or Vitality (live: a 10-star Gillsplash Belt's 2 Health Regen shows +2, a 5-star Reaper Mask's 60
  Vitality +60, and its 5 Health Regen +5; the live Reaper Masks are dungeon items, so that's dungeon items outside a
  dungeon too, though none in the item data has those stats and stars today). In `ItemStats.of` and
  `ItemBuilder.statLines`.
- **Dungeon items** keep their rule: up to 5 count, and in a dungeon the dungeon boost replaces the 2%. Master stars
  (past 5) look as before and add nothing (the modifiers part's).
- **Buying one**: `Stars.next` (the next star's cost from the item data's upgrade costs) and `Stars.add`, shared by
  `/upgrade` (which otherwise works as before: it still charges on Sandbox, from the inventory only) and the Hex.
  They count the older `dungeon_star` data too, so `/upgrade` no longer adds a star that doesn't show on such items.
- No coins: the API's costs have none. NEU's table adds coins from the 4th star on (10k to 10m), which is disputed
  (UNKNOWN which is live).

## Dungeon items (`item/DungeonItems`)

`DungeonItems.is(item, tag)`: the kind's `dungeon_item` or the item's own data flag (`dungeon_item`, which every
item's data has had; Convert to Dungeon Item sets it). Everything that read the kind's value reads this: the stars'
look, the gray bracket and in-dungeon stats (`ItemBuilder`, `ItemStats`), the enchantment layout, the rarity line
("LEGENDARY DUNGEON LEGGINGS", as a live converted Superior Dragon Leggings), ability damage in a run (`Hits`) and
accessories in a dungeon (`AccessoryBag`). `convertible`: the kind has a conversion cost (159 items) and it isn't
one yet.

## The Hex

**Books** (Carpentry 20; the core's gate). For any item a book goes on.
- Button and pane summary: a line a book that goes on the item, as the wiki's tabs and the screenshot have them:
  `  &5Hot Potato Book &e3&7/&a10`, `  &5Fuming Potato Book &e0&7/&a5`, `  &5Book of Stats &c✖`, `  &6The Art of
  War &a✔`. A full count is green all through, as the screenshot's "Hot Potato Book 10/10" (the spec had `&e`).
- **"The Hex ➜ Books"**: the header an anvil, "Apply Books"; the books centred in the grid (a weapon's four in
  21, 22, 24, 25; armor's three in 21, 23, 25, as the wiki's tabs). Each is its book's item: its name, its own text
  (the item data's), a blank line, then the Cost block (one of the book; "&aFree" on a Sandbox profile) and
  "&eClick to apply!". A click applies one ("You applied a Hot Potato Book to your ...!"). A full book stays,
  saying "&aItem Maxed Out!"; the Fuming Potato Book says it needs 10 Hot Potato Books until then (the 20 January
  2026 patch: "All 10 Hot Potato Books now must be applied before applying any Fuming Potato Books"; NEU's Hex
  offers it from 10 to 15). The wiki's screens, older, still show its Cost block with none on.

**Item Upgrades** (Carpentry 25). For an item that takes stars or can be made a dungeon item.
- Summary: `  &7Dungeon Item &a✔` (`&c✖` while it can still be made one; no line for an item that can't) and
  `  &7Upgrade Level  &6✪✪✪✪✪` (the stars as its name ends with them, master stars too; `&c✖` for none). The two
  spaces are the screenshot's: measured, there are two before the stars and one before "Dungeon Item"'s ✔.
- **"The Hex ➜ Item Upgrades"**: the header an anvil, "Item Upgrades"; an entry a star (5, 10 or 15), centred,
  named by its stars as the item would show them (NEU reads the ✪ in the names). Each says what a star gives (the
  Essence Guide's words), then: the next star's Cost block (its essence and items, "&eClick to upgrade!"), or that
  it's on, or which comes first. A click buys the next star.
- **Convert to Dungeon Item** in 48 (an anvil, NEU's): Malik's text on it, then its cost (the item data's conversion
  cost, essence), "&eClick to convert!"; once it's one, "This item is already a Dungeon Item" (the fragment NEU
  looks for). Only on items that are one or can be made one.

The private golden (`items/golden.json`) changes with the stars: only the 7-star renders of the 364 items that
aren't dungeon items and have stats and star costs (their stats now 14% more of their own); nothing else.

The core's tests that used a sword as an item no category is for (`HexCategoriesTest`, `HexMenuTest`) now use an
item of no type, since books go on swords.

## Approximated (UNKNOWN in the code)

- **Books page** (U4, U5): the action line "&eClick to apply!", a full book's "&aItem Maxed Out!" (after the name
  NEU's Hex skips), and the Fuming Potato Book's "&cRequires &a10 &5Hot Potato Books&c!" are our own words.
- **The books' order** past the wiki's (Farming for Dummies, Bookworm's, Polarvoid, Wet Book, after the rest), and
  their summary lines: no source shows them. Names are in their items' rarity colours.
- **The books' text** is the item data's: the wiki's Hex copies are older ("Stop quoting my brother", now
  "cousin") and wrapped differently. Menu icons can't shine, so the Hot Potato Book and the others don't.
- **Bookworm's Favorite Book** gives +20 Damage as the wiki and live vacuums do (400 base: "+525 (+100) (+25)" with
  5 and a reforge), though its own text says +10.
- **Which axes and rods take potato books**: all of them (live: 6 kinds of axe and 3 of rod carry them, including
  the Treecapitator, which has no Damage).
- **Book of Stats**: a kill counts for what they hold when the mob dies (a bow they've switched away from doesn't
  get it); any kill, an ability's too.
- **A click they can't pay for** does nothing but redraw; the lore says what's missing.
- **Item Upgrades page** (U12): the header's name and lore (the button's, ours), the stars' look (a nether star),
  their lore past the Essence Guide's line ("This upgrade has been applied!", "Upgrade to ✪✪ first!"), their
  centred placement, Convert's lore past its name (Malik's text; "&aThis item is already a Dungeon" / "&aItem!"),
  and the chat lines ("You upgraded your ...!", "You converted your ... into a Dungeon Item!").
- **The summary**: no Dungeon Item line on items that can't be made one; "&c✖" for no stars.

## Later, and what it waits for

- The Book of Stats' crop count on farming tools ("Crops Harvested: 0" shows): farming.
- Farming for Dummies' Farming Fortune and the Wet Book's Fishing Speed show and count in the stats, but nothing
  uses them yet: farming and fishing. Bookworm's Damage on a vacuum counts as it's held; its pests: the Garden.
- Master stars (the modifiers part), Hypixel's star coin costs (if they're live), Kuudra armor's prestige.

## Questions for the owner

- Wands lose potato books' stats (no live wand has them; only `/data` could put them on one). Right?
- The Book of Stats goes on farming tools and shows 0 crops until farming exists. Keep it, or only on weapons?
- Should stars from the 4th on also cost NEU's coins (10k to 10m), or stay essence and items as the API has them?
- The core's chat line reads "You applied a The Art of War to your ...!" (SkyHanni's `You applied an? .+ to your
  .+!`). Keep it, or leave out the "a" before "The"?
