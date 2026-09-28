# The Hex

The Hex is Hypixel's one place to upgrade an item every way there is: enchantments, books, modifiers,
reforges, stars and gemstones. Here it opens with `/hex` (also `/hecks` and `/thehex`), because the
Hexatorum and The Handler aren't here. Its Gemstones category opens Geo's Gemstone Grinder with the item,
at the owner's wish ("I would also like to be able to access the gemstone menu from The Hex").

The screens come from the Hypixel SkyBlock wiki's The Hex/UI screens, NEU's custom Hex screen (which
parses the real menus), Lunar Client's per-menu slot lists and the official 0.14 screenshot of the pane
tooltip. There's no recording of the Hex. The research is in the task's scratchpad
(`hex/result_hexui.md`, `result_infra.md`, `result_items.md`, `result_data.md`).

The code is in `paper/src/main/java/net/icxd/dungeons/hex`. `Hex` is where the rest of the plugin starts
and opens it.

Task 70 builds the Hex in stages:
1. The core: `/hex`, the main menu, the item's life in the Hex, the page frame, costs, requirements and
   the private data plumbing, with the seven categories' buttons.
2. Five parts, built at the same time on top of the core, each filling in its own categories (see the
   table below): enchantments, reforges, gemstones and the grinder, books and item upgrades, modifiers.

## Done (stage 1)

**`/hex`** (`HexCommand`; `hecks`, `thehex`) is a DEFAULT-rank command, not a Sandbox tool. It opens The
Hex empty. It's refused on DUNGEONS servers ("You can't use The Hex in a dungeon!", UNKNOWN wording), since
the wiki's Catacombs says the Hex can't be used there even with a Booster Cookie. It says nothing until
the player's data is loaded, while their items are frozen (a hand-off) or while they're dead.

**Requirements** (`HexRequirements`). A Sandbox profile needs nothing. A Normal profile needs:
- Museum Milestone 8 (`museumMilestone`) and, for `/hex`, the Cookie Buff (`boosterCookie`). Both return
  true for now, because neither the Museum nor the Cookie Buff exists (the owner's precedent: "all can be
  unlocked for now since the NPCs are missing"). Each is one named method that can be switched on later.
- Carpentry 20 for Books and Modifiers, and Carpentry 25 for Item Upgrades and Gemstones, checked with
  `carpentry(player, level)`. A category the player doesn't have the level for keeps its button, but its
  last line is the plugin's requirement line (`&4❣ &cRequires &aCarpentry Skill 20&c.`) in place of
  "&eClick to view!". Clicking it sends that line in chat.

**The main menu, "The Hex"** (`HexMenu`), laid out as on the wiki:
- 6 rows of glass, the item's slot in 22, and Close in 49. There's no Go Back.
- The item goes in and out of slot 22 as in a chest: by click, number key, shift-click from the inventory,
  or shift-click back out. The slot takes one item at a time (see Approximated). It refuses what storage
  refuses: items that are never saved (a dungeon's map) and the SkyBlock Menu. No bundles.
- The eight panes around the item (12, 13, 14, 21, 23, 30, 31, 32) show one of three states:
  - Gray, "Give your life to The Hex!", when the slot is empty.
  - Red, "The Hex is displeased!", for a stack that isn't a SkyBlock item, or one no category applies to.
    Until the parts are built, that's every item.
  - Purple, "Give your mind to The Hex!" ("time" for an accessory). Its lore starts "Upgrade your <item> with
    a variety of bells and whistles, all in one place!", wrapped. Then comes each applicable category's
    summary group, in category order, with a blank line between groups. Then the footer: "The Hex - Your
    one-stop shop", "for personal refinement!", and the name again.
- The category buttons for the item, in order, fill the 3x3 block (15-17, 24-26, 33-35), filled
  columns-first (see Approximated). Each button is `&a<name>`, then the description, a blank line, the
  category's summary, a blank line and "&eClick to view!".
- Everything is redrawn on the tick after any click. A button's click also runs on the next tick, and
  only if the item is still the one the button was drawn for; if it was swapped in between, the click
  only redraws. So nothing worked out for one item is ever done to another.

**The item's life** (`HexSession`, `HexScreen`, `HexListener`). One session per player owns the item from
the first Hex screen they open until the last one closes. It passes the item between screens without
going through their inventory, and the item is always in exactly one place:
- On an input screen, it's the item in the screen's input slot: the main menu's 22, or the grinder's 13.
- On a page, the session holds it, and the page shows a copy in slot 19 that can't be taken.

The item goes back to the player (`ItemStash.give`: their inventory, else their stash) whenever the
current screen closes. That covers closing the menu, opening any other menu, disconnecting, a hand-off
and a profile switch, since all of them close the menu first. A quit with a session still open (a menu
that never opened) gives the item back before the save. Two cases need more:
- **Server stop.** The plugin's events are already off, so no close is heard. Instead, every save of
  their data writes the item into the profile's `storage.overflow`, replacing the copy the previous save
  put there (tracked by identity). The next join gives the overflow back. When the session ends normally,
  its copy comes out in the same moment the item goes back to the player. So any save has the item either
  in the Hex (overflow) or wherever they put it since, never both. The last autosave before a crash
  gives it back too. (`HexSession.save`, hooked in with `StoredInventory.captureWith`.)
- **Death.** Paper works out the drops before it closes the menu, and empties the inventory after. So at
  HIGH, before SandboxDrops (HIGHEST) marks the drops, a copy of the item goes into the drops. At MONITOR:
  - If the death went through and the inventory is emptied, the session forgets the item: it dropped.
  - If they keep their inventory, the copy comes back out of the drops, and the closing menu gives the
    item back.
  - If the death was called off (a dungeon ghost), the copy comes back out of the drops, and the menu
    stays open with the item.

  (Paper's drop list keeps the very stack a listener adds, so the copy is found again by identity.)

**Pages** (`HexPage`), laid out as on the wiki and in NEU's Hex:
- 6 rows of glass, the item in 19, a header in 28, Go Back in 45 ("To The Hex" or the page's own line),
  and Close in 49.
- The 5x3 grid (12-16, 21-25, 30-34) shows 15 entries a page, with Previous Page in 17 and Next Page in
  35 ("&8Page n", which is what NEU reads).
- Entries are placed one of two ways: row by row (enchantment lists), or centred (Books, Modifiers,
  Reforges, bottles). Centred uses the wiki's observed layouts for 1, 3, 4, 5, 6 and 12 entries; see
  Approximated for the rest.
- Buttons can go in 48, 50 and 51 (`extras`).

**Costs** (`HexCosts`). A cost is coins, items (by id), essence and Exp levels.
- **Where it's taken from:** coins from the purse, items from the inventory and then Storage, essence
  from the profile, and Exp levels from vanilla levels. Storage means the Ender Chest pages, then the
  backpacks (`storage/StoredItems`), and never while a storage page is open.
- **Checking and paying:** everything is checked before anything is taken; it's all taken or nothing is.
  Parts of a kind are added up first (coins twice, two parts of one item or essence), so what's checked
  is what's taken. Then it saves.
- **The Cost block:** follows the wiki's grammar without the Bazaar lines, since there's no Bazaar. A
  missing item shows "You don't have that in your inventories!", and missing coins "You don't have enough
  Coins!".
- **Sandbox:** nothing is checked or taken, and the block reads "&7Cost" and "&aFree".
- **Chat:** `HexSession.upgrade` sends "You applied a <X> to your <item>!" (the wording SkyHanni's regex
  expects). Closing the menu in the same tick as a payment sends "Trying to pull a fast one, eh?!" (the
  wiki's Hex).

**Private data** (`HexData`). The Hex's Hypixel tables go in the data repository's `hex/`. servermgr
links it into every server as `plugins/dungeons/hex` (`optionalData` in `tools/servermgr/setup.go`).
Each category registers its own files with `HexData.add`. At startup they're read off the main thread,
problems are logged, and each category gets its result on the main thread. Tests get the folder from
`-Dhex.dir` through `PrivateHex` (under test/.../hex), and are skipped without it. The core writes no
tables of its own.

## The seven categories

The button order, and the order of the panes' summary groups, are fixed (`HexCategories`). Each
category is one file under `hex/category`, owned by one part. A part replaces its stub's body (keeping
the name, the Carpentry level and, where the wiki has it, the button) and puts its pages and logic in
files of its own, preferably in its own package (say `hex/enchant`, `hex/reforge`, `hex/gem`,
`hex/upgrade`), so the parts don't collide.

| # | Category | Carpentry | Button | File (owner) | Page | Status |
|---|---|---|---|---|---|---|
| 1 | Enchantments | none | Enchantment Table (wiki) | `hex/category/Enchantments.java` (enchantments part) | "The Hex ➜ Enchant Item" | built, see ENCHANTMENTS.md |
| 2 | Ultimate Enchantments | none | Book and Quill (wiki) | `hex/category/UltimateEnchantments.java` (enchantments part) | same title, the ultimates | built, see ENCHANTMENTS.md |
| 3 | Books | 20 | Book (wiki) | `hex/category/Books.java` (books and item upgrades part) | "The Hex ➜ Books" | built, see BOOKS.md |
| 4 | Modifiers | 20 | Recombobulator 3000 head (wiki; texture from items.json) | `hex/category/Modifiers.java` (modifiers part) | "The Hex ➜ Modifiers" | built, see MODIFIERS.md |
| 5 | Reforges | none | Luxurious Spool head (wiki; texture from items.json) | `hex/category/Reforges.java` (reforges part) | "The Hex ➜ Reforges" | built, see REFORGES.md |
| 6 | Item Upgrades | 25 | Dragon Essence head (inferred; items.json, else NEU's head) | `hex/category/ItemUpgrades.java` (books and item upgrades part) | "The Hex ➜ Item Upgrades" | built, see BOOKS.md |
| 7 | Gemstones | 25 | Perfect Ruby Gemstone (inferred; as items.json has it) | `hex/category/Gemstones.java` (gemstones and grinder part) | Geo's Gemstone Grinder | built, see GEMSTONES.md |

Every category is built; each part's status document says what it does and what's UNKNOWN or LATER.

## How a part plugs in

- **`HexCategory`** (abstract): `name()`, `requiredCarpentry()` and `description()` come from the
  constructor. A part implements:
  - `look()`: the button's material and head texture. `lookOf(id, fallback)` reads an item's look from
    items.json.
  - `applies(HexItem)`: whether its button shows and its summary group is in the panes.
  - `summary(HexItem)`: its group's lines, each indented two spaces ("  &7Reforge &c✖").
  - `open(HexSession)`: usually `session.open(new MyPage(session))`.

  `applies` and `summary` are plain functions, so they can be tested with `HexFakes`. They only read
  the `HexItem`: the main menu hands the same one to every category.
- **`HexItem`**: the item's `SkyBlockItem`, a copy of its data (`tag`) and its holder, plus `name()`,
  `rarity()` and `accessory()`.
- **`HexSession`**, the item's owner:
  - `item()` (the stack; don't change it), `hexItem()`, `tag()` (a fresh copy to change),
    `skyBlockItem()`.
  - `upgrade(cost, newTag, "&6Recombobulator 3000")`: pays, rebuilds the item
    (`ItemBuilder.build(item, tag, amount, player)`), redraws, saves both together, and sends "You applied
    ...". Pass null as the name to send your own message. It returns false and takes and changes nothing
    if they can't pay, or if the tag's id isn't a SkyBlock item.
  - `upgrade(cost, newTag, name, back...)`: the same, and it also gives them `back` (what comes off the
    item, such as a gemstone removed in the grinder) in the same save.
  - `replace(tag)`: just the rebuild and redraw, with no payment and no save. It returns whether it made
    the item.
  - `open(screen)`: passes the item to another screen.
  - `sandbox()`, `user()`, `player()`, `have(id)` (inventory plus storage), `applied(name)`.
  - `HexSession.open(player, Screen::new)`: opens a screen on its own, such as the grinder without the
    Hex; the session starts empty.
- **`HexPage`** (abstract), for a page:
  - Required: `header()` and `entries()` (a list of `Entry(icon, click -> ...)`, built from the item as it
    is now on every draw). `entries()` and `extras()` are only called while there's an item
    (`session.hexItem()` isn't null). Without one, the page draws its frame alone.
  - Optional: `placement()` (`ROW_MAJOR` or `CENTRED`), `extras()` (48/50/51), `backTo()` and `back()`
    (for example "To Enchant Item" for the level page and bottles), and `page(n)` / `redraw()` after
    state changes such as sorting.
  - An entry's click runs on the next tick, only while the page is still open, and only for the item it
    was drawn for (`HexScreen.button`). So an entry's action may use a tag worked out when it was drawn.
- **`HexScreen`** (abstract), for a screen with an input slot (the grinder), or any other screen that
  keeps the item in the session (the grinder's confirmation, say):
  - Override `inputSlot()` (13) and `draw()`. The item then goes in and out of that slot by the same rules
    as slot 22, with the same refusals, cooldown, shift-click and redraw.
  - `refuses(item)`: what this slot refuses beyond those rules, and the line to say ("" says nothing).
    For example, the grinder's "Only items that can have Gemstones applied to them can be put in the
    Grinder!".
  - `button(slot, stack, action)`: a button, guarded as above.
  - `later(action)`: runs something on the next tick while the screen is open and the items aren't
    frozen, for a click that isn't a button's. An example is a gem clicked in the inventory: override
    `onPlayerInventoryClick`, cancel the click, and apply it in `later`.
  - `say(line)`: a line in chat.
  - A constructor with a size, for a screen that isn't six rows.
  - For Go Back to The Hex, call `session.open(new HexMenu(session))`.
- **`HexCosts`**:
  - Build a cost with `HexCosts.of(new Coins(n), new Items(id, n), new Essence(type, n), new Levels(n))`.
  - `lore(session, "&eClick to apply!")` gives the Cost block and the action line (or what's missing).
  - Also `check(session)`, `affordable(session)`, and `pay(session)` for a cost that doesn't change the
    item.
- **`HexData.add(name, (folder, problems) -> read, loaded -> use)`**: call it from a category's
  constructor. `HexData.json(folder, file, problems)` reads a JSON file. Tests use
  `PrivateHex.folder()`.
- Shared files every part may touch: `stats/ItemStats.of` and `ItemBuilder.statLines` (each new stat in
  both, in the live bracket order `§e(hpb) §6[AoW]|§c[AoP] §9(reforge) §d(gems) §8(dungeon)`). Keep those
  edits small, for the lead to merge.

## Approximated (UNKNOWN in the code)

- **Button slots (U1).** Only 1 button (slot 15) and 5 (15, 16, 24, 25, 33) are on the wiki. The rule
  "as many columns as three a column need, filled row by row" gives 7 → 15, 16, 17, 24, 25, 26, 33. Where
  Item Upgrades and Gemstones sit is unseen.
- **Centred placement (U3).** 2 entries → 22, 24 (4's gap in the middle); 7 and more → row by row, as 12 is.
- **Page arrows.** Plain arrows, as the plugin's other menus page. The wiki pictures them as "Arrow Up" and
  "Arrow Down".
- **A stack in the input slot.** Refused rather than split: "You can only put one item in The Hex at a
  time!" (our wording). Nothing is left over to go anywhere.
- **Carpentry gate (U7).** The button keeps its look, with the requirement line last.
- **The purple pane's wrap.** 154 pixels, with ✪ counted as 9 pixels wide (the game's Unicode font). That
  wraps the screenshot's and the gray pane's lines as they are. The exact rule is unknown.
- **Item Upgrades and Gemstones buttons (U1, U12).** The words are ours. The icons are the wiki table's
  (Dragon Essence head from NEU, since items.json has no ESSENCE_DRAGON; the Perfect Ruby is paper in
  items.json). The Item Upgrades page header is ours too.
- **Cost lines.** An item's amount past one ("&8x4"), essence ("&dWither Essence &8x30"), Exp levels
  ("&315 Exp Levels", NEU's form) and their missing lines are UNKNOWN. So is the affordable action line
  (U4), which each page words. Sandbox's "&aFree" is our own.
- **Messages.** The colours of "You applied ...". "Trying to pull a fast one" fires on a close in the same
  tick as the payment. The dungeon refusal wording.
- **Death.** The item drops with the rest in hubs (the spec's rule). What Hypixel does is unknown: it
  doesn't drop items on death.

## Later, and what it waits for

- Museum Milestone 8 and the Cookie Buff: waived until the Museum and Booster Cookies exist.
- The Handler and the Hexatorum (the Hub's NPC and altar), the altar animation, the "Gonna Put a Spell on
  You" achievement, and the Booster Cookie menu's Hex button.
- Sacks as a cost source (sacks hold nothing here), and the Bazaar (none here).
- The seven categories' contents: stage 2's parts.

## Questions for the owner

- A stack put in the Hex is refused rather than split. Is one item at a time right?
- Dying with the Hex open drops the item with the rest in hubs, as vanilla would. SkyBlock doesn't drop
  items on death. Should it go back into the inventory instead?
- Costs come from the inventory (armor and off-hand included, as the plugin's `ItemCost` does), then
  the Ender Chest, then backpacks. Should the other bags, or the stash, count too?
