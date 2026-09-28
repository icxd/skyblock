# Collections, the Recipe Book and crafting

What's done of Hypixel's collections, Recipe Book and crafting table, what's approximated, and what
waits for other systems. The menus were built from the SkyBlock Menu tour recording (2026-09-27:
Collections 00:40-01:04, the Recipe Book 01:08-01:15, Craft Item 02:57); the rules from the wiki
(Collections, Crafting, Crafting Table, Commands, Carpentry). The code is in
`paper/src/main/java/net/icxd/dungeons/collection` and `.../recipe`.

## The data

Hypixel's collections, tiers and unlocks, and the recipes, are private data:
`collections/collections.json` and `collections/recipes.json` in the data repository, made by
`tools/collections/build_collections.py` (see its README) from Hypixel's collections API, the NEU
repository and the wiki. servermgr links that `collections/` into every server as
`plugins/dungeons/collections`; `CollectionFiles` reads it off the main thread when the plugin starts
and logs what's wrong. Without it there are no collections or recipes: the menus show none, nothing
counts. Tests read it from `-Dcollections.dir` (else the data checkout's `collections/`) and skip the
recorded-menu checks without it.

## Done

- **Counts** per profile: `collections.<ID>` (items) and `bossCollections.<ID>` (a boss's kills) in
  the profile's document, `bossCollectionRewards.<ID>` the boss tiers claimed. For SkyBlock Leveling
  (read-only): `Collections.tier(profile, id)`, `Collections.unlockedTiers(profile)` (id → tier, boss
  collections too) and `Collections.skyBlockXp(profile)` (the tiers' "+N SkyBlock XP").
- **What counts** (the wiki's Collections: items obtained "manually", not from merchants, the Bazaar
  or trades): a mob's drops and a mined block's, when they go straight into the inventory (dungeon
  mobs, mining) or when a player picks them up off the ground. Only drops the plugin itself made are
  marked collectable (an entity tag), and a marked stack never merges with an unmarked one, so what a
  player dropped, traded, bought, took from storage or got from /item or the item browser never
  counts. An enchanted item counts as what it's made of (160 Rotten Flesh). Only Normal profiles
  collect (`Collections.counts`).
- **Tier-ups**: the level-up message (the rules and header mods match on, then the old and new tier
  and the tier's rewards), the tier's Skill XP given, its stats (Obsidian's "+1☘ Mining Fortune",
  through `PlayerStats.addModifier`), its recipes unlocked; the profile is saved.
- **Boss collections**: a finished floor adds a kill to its boss (+2 in Master Mode, as the recorded
  Bonzo Collection says), from `RunEnd`. A reached tier's items and essence (onto the profile's, as
  dungeon mobs' essence is: `mob/Essences`) are claimed from its Rewards menu, once.
- **Menus**, as recorded, with the profile's numbers: Collections (the five categories, Boss
  Collections, Crafted Minions, the rankings sign), a category's collections (found or the gray dye),
  a collection's tiers (lime, yellow, red panes with the tier as the count), a tier's Rewards (a
  recipe with a crafting recipe opens it, boss items and essence are claimed). `/collection` (and the
  wiki's other names), `/viewcollection <item id>`, `/viewbosscollection <boss id>`.
- **Recipe Book**: its eleven categories, pages of 28 (unlocked first, then locked, each by name),
  "???" with what unlocks a locked one, a recipe's view (the wiki's Crafting UI: none was opened in the
  recording). `/recipes` (and the wiki's names), `/viewrecipe <item id>` (locked ones too, as on
  Hypixel). The SkyBlock Menu's Collections and Recipe Book items show the profile's numbers.
- **Crafting** ("Craft Item", `/craft` for everyone as the wiki's Commands has it, and the SkyBlock
  Menu's Crafting Table): the recorded 54-slot table; SkyBlock items from NEU's recipes once unlocked
  (and vanilla items NEU has a recipe for, like a Block of Iron), matched by SkyBlock id and amount, a
  shaped recipe anywhere it fits in the grid, and an item with no SkyBlock id in the grid in the way of
  any; a click takes one onto the cursor, a shift-click as many as the grid and the inventory allow (an
  unstackable result one to a slot, each its own item); the inputs are consumed; the result is built
  with ItemBuilder; Carpentry XP (3% of the inputs' NPC sell price, the wiki's Carpentry). Items go in
  by click, drag, number key or shift-click, and whatever is left comes back when it closes. On a
  Sandbox profile it works the same, and its items stay in the profile (SandboxStorage).
- `/setcollection <id> <amount|+amount> [player]` (staff) for testing.

## Approximated (UNKNOWN)

- Sandbox profiles collect nothing, and have every recipe unlocked (but the minions' and pets'): no
  owner decision covers either.
- The level-up chat past its header, its sound (the skills'), and what a boss tier says (nothing).
- Reward lines never recorded: Dwarven Forge recipes and trades as the wiki's Collection UI writes
  them, a stat's and other unlocks' colour (the recorded Quiver's green), "Slot" for one.
- How tiers are laid out past nine (a row of nine each); Kuudra's head; a boss's maxed lore; what a boss
  item says once it can be claimed and after; that a never-found collection does nothing on a click.
- The Collections "Unlocked" percentage at 100% (the Recipe Book's green is recorded).
- Crafting: a shaped recipe smaller than the grid fits anywhere it can be moved (vanilla's way), not
  mirrored; the bottom glass turns lime while there's a result (not recorded); a locked recipe shows as
  none ("Recipe Required"); an ingredient's enchantments, reforge and upgrades go with it, none to the
  result; vanilla recipes give Carpentry XP too (the wiki: "most vanilla recipes do not", not which);
  a ranked player's Quick Crafting slots are the recorded empty ones below Carpentry III too.
- Boss essence: only Bonzo's amounts are known (the recording's III and VI); the other bosses' essence
  rewards show no amount and add none. How a tier is claimed (a click on any of its items, all at once).
- A recipe's Go Back from the Recipe Book says the page it came from ("To (1/4) Combat Recipes").
- The Recipe Book's lists are the wiki's (966 recipes in May 2026; the data has 952 entries, the
  recording's total was about 1,016), so the counts differ: a new profile has 30 unlocked here, 111
  recorded. The Pets' Z-to-A order on a page is as recorded, reason unknown.
- Slayer recipes whose level no source gives are taken as the slayer's first (five of them; the report
  lists them). Trades Unlocked counts the trades collection tiers unlock (the recording's has two more).
- Requirement text of a locked recipe other than a collection's ("Requires Zombie Slayer 3").

## LATER (waits for)

- **Minions**: their recipes stay locked (shown), Crafted Minions and the minion limit show nothing
  crafted; minions' output would count toward collections.
- **Pets**: pet recipes stay locked (shown).
- **Slayers**: slayer recipes stay locked; the book says the level.
- **Trades menu, Search Recipes, Collection Rankings, Quick Crafting** (VIP and up, Carpentry III) and
  Supercrafting: shown as recorded, they do nothing.
- **The Dwarven Forge, enchanting (Exp Discounts), bags (Quiver, Potion Bag, Sack of Sacks slots),
  the Personal Bank (Emerald VI)**: their rewards are shown only.
- **Kuudra**: its boss collection counts nothing (no Kuudra fights). The dungeon bosses' count a
  finished Floor I-VII (the Entrance has no boss collection).
- **Sacks**: items going into a sack would count too.
- **Rift collections**: there's no Rift.
