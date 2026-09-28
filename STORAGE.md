# Storage, bags and Loadouts

Storage (the Ender Chest and backpacks), Your Bags (the Accessory Bag with its Accessory Powers, the
Potion Bag, the Fishing Bag and the Sack of Sacks) and Loadouts (with the Armor and Equipment Sets),
as on Hypixel. The menus were built from Replay Mod recordings (the SkyBlock Menu tour and the
Loadouts and Storage tour, 2026-09-27, decoded with `tools/replay`), and the rules from the Hypixel
SkyBlock wiki where the recordings don't show them. The code is in
`paper/src/main/java/net/icxd/dungeons/storage` (`Storage` is where the rest of the plugin starts
and opens it); what it keeps is in the profile, next to the inventory.

## Done

**Saving.** Everything is in the selected profile's `storage` document, each item a blob the way the
inventory's are (`StoredInventory`; see `StorageDocument` for the layout), and the loadouts and the
selected power next to it in the profile. So it's saved, handed off between servers and switched with
the profile, like the inventory. A menu that holds items (an Ender Chest page, a backpack, a bag) writes
them back on the tick after every click, when it closes (a hand-off, a profile switch, a disconnect and
a stop close it first) and whenever the inventory is saved (`StoredInventory.captureWith`), so a save
has an item in exactly one place (also the last save when the server stops: the plugin's events are off
then, so the menu doesn't hear itself close). It writes to the profile it was opened on, and nothing
once the player's data has been handed off. An item that can't be read stays in `storage.unreadable`
like the inventory's; one that can't be written, or has nowhere to go, goes to the player (or their
stash). Sandbox rules hold: nothing leaves the profile, and items that are never saved (run items, the
SkyBlock Menu) can't go in.

**Storage** (`/storage`, `/st`, `/er`; the chest in the SkyBlock Menu). Nine Ender Chest pages across
the top (in purple glass, or the icon they chose, numbered by stack size; one they haven't unlocked
would be "Locked Page") and eighteen backpack slots, all of them everyone's for now (see Approximated).
A page opens with a left click and changes its icon with a right one. **Ender Chest pages**
(`/enderchest [page]`, `/ec`, `/echest`, `/viewenderchest`): the page bar (Close, Back, First, Previous,
Next, Last as recorded) and 45 slots. **Backpacks**: a backpack is put in an empty slot by clicking it
with the backpack ("Placing backpack in slot 2...", "Success!"), opened with a left click or
`/backpack <slot>` (`/bp`, `/bps`, `/backpacks`), and taken out with a right click only when it's empty
("Removed backpack from slot 2!", and "Please wait before doing this again." within a few seconds). Its
page has a row for each nine of its slots, Small 9 to Jumbo 45, by the backpack's own "A bag with N
slots" (dyed ones as the plain one); its arrows go through the slots with a backpack. Backpacks can't go
in the Ender Chest or a backpack. **Choose an Icon**: Reset and the recorded first page of icons. Items
move by click, shift-click and number key (not drag or double click) as in a chest; the page buttons
keep the menus' short click cooldown.

**Your Bags** (`/bags`; the head in the SkyBlock Menu): as recorded. A bag its collection hasn't
unlocked is gray dye with what it needs ("Requires Nether Wart Collection II.", as the Time Pocket
shows it). **Accessory Bag**: 9 slots, more with the Redstone Collection's tiers; a Sandbox profile's has all
281 there are (the wiki's sources added up); pages of 45 ("Accessory Bag (1/2)"); only accessories go in. Its accessories and those in the inventory count for their stats
(`PlayerStats.addModifier`), only one of each and only the best of a line (Talisman, Ring, Artifact,
Relic: NEU's upgrade lines; of two of the same, the recombobulated one). Accessory Power (shown as
Magical Power) comes from the bag's counted accessories by rarity (the wiki's table), the Hegemony
Artifact twice, a dungeon accessory twice in a dungeon; Tuning Points are one per 10. The selected
power's stats grow with it (the wiki's formula; the recorded Select Power Stone's numbers come out
exactly), its Unique Power Bonus doesn't. The Scavenger accessories' coins (KillCoins) count the bag's
too. **Potion Bag** (potions, God Potions), **Fishing Bag** (fish and bait), **Sack of Sacks** (`/sacks`,
`/sax`; sacks): menus that keep what they take, sized by their collections.

**Loadouts** (`/loadouts`, `/loadout`, `/ld`; the barrel in the SkyBlock Menu): three pages of twelve,
with what they wear on the left. A loadout names an armor set, an equipment set and a power (any of them
None), shows its helmet (gray dye until it's set up) and its parts, and "Left-click to equip!" unless
it's what they have on. Equipping it puts the sets on: what they wore goes back to the set it came from
(or their inventory, if it came from none; nothing happens without room), a None part stays as it is,
and the power is selected ("You equipped Loadout 1!"). Right-click edits it: each piece "Left-click to
change!" (the set menus), "Right-click to clear!", its Power Stone (Select Power Stone), Clear, and
Rename Loadout on a sign. **Armor Sets** (three pages of nine) and **Equipment Sets** (two): a set's
pieces are put in by clicking the slot with the piece (or shift-clicking it in the inventory) and taken
out by clicking it; the worn set's column is what they wear and can't be changed there; "Click to equip
to loadout!" / "Click to unequip from loadout!" and Clear Selection. **Select Power Stone**: the powers
they may pick at their Accessory Power (every Stone Power for now, see Approximated), the selected one
in lime glass, 28 a page; from Loadouts' own Power Stone it picks the selected power. The four equipment
pieces worn (necklace, cloak, belt, gloves or bracelet) count for their stats. How many of each a rank
has is the wiki's: loadouts and armor sets 4 / 10 (VIP+) / 18 (MVP+), equipment sets 2 / 5 / 9; staff
have all (27, 27, 18), and the locked ones say what unlocks them.

## Approximated (UNKNOWN in the code)

- How many Ender Chest pages and backpack slots: all nine and all eighteen for everyone, a stand-in
  (the owner, 2026-09-28: "all can be unlocked for now since the NPCs are missing"). The game starts
  with one of each; the pages come from the Community Shop, the slots from Tia the Fairy for Fairy
  Souls, neither here. The recorded player had five pages and eighteen slots.
- Stone Powers: every one can be picked without learning it from Maxwell, and without the Combat level
  his menu asks for when learning, a stand-in (the owner, same day). The Intermediate Powers still need
  Combat XV, which isn't Maxwell's.
- The messages nobody recorded: a page or slot that doesn't exist or isn't unlocked, a backpack that
  isn't empty or doesn't fit in the inventory, a loadout that doesn't fit, a set with a piece that can't
  be read. `/backpack` without a slot opens Storage. The backpack cooldown is 5 s (refused at 4.9 s,
  fine at 6.9 s).
- Choose an Icon has one page ("(1/1)"): Hypixel's six weren't recorded past the first. A chosen icon
  replaces the page bar's glass, without a tooltip as the glass has none.
- Which items the Potion Bag, Fishing Bag and Sack of Sacks take beyond potions, fish, bait and sacks.
- Accessory Power: the loadout pages showed Silky at 500 while Select Power Stone and the Stats Tuning
  item said 571 in the same minute; everything here uses the one live value. The highest-AP copy wins
  among duplicates; two of a line where neither upgrades into the other (the Abicases), the one that
  gives more.
- Select Power Stone lists only the powers they may pick; from Loadouts it has Clear Selection too; the
  Power Stone slot's head without a Stone Power is the Accessory Bag's. Past 28 powers it has pages, with
  Previous and Next Page at the bottom corners as the wiki's Accessory Bag Thaumaturgy (only one page
  was recorded).
- The set menus' glass for columns 1 to 3 (4 to 9 are recorded); the empty piece's lore in a loadout
  whose set lacks it; the rank lines in a locked slot's lore; the rename sign's lines and the 16-letter
  limit; HOTM and HOTF "Heart of the ... 1".
- A loadout with an armor set but no helmet shows a barrel.

## Later

- **The Quiver** (the owner, 2026-09-27: arrow types make it involved): its slot in Your Bags shows as
  recorded and does nothing.
- **Cookie Buff commands** (`/accessorybag`, `/potionbag`, `/fishingbag`, `/quiver`): they need the
  Booster Cookie. Your Bags says "Also accessible via" them, as recorded.
- **Earning Ender Chest pages and backpack slots**: the Community Shop and Tia the Fairy's Fairy Soul
  exchanges. Until they're here everyone has all of them (`StorageMenu.pages` and `backpackSlots`); once
  they are, what players put in the pages they haven't earned needs a way out (a backpack in a locked
  slot already shows, to be taken out; a locked page's items would stay in it unseen).
- **Collections** (the collections part): bag sizes read tiers through `Bag.setCollections` (by
  Hypixel's collection ids: `REDSTONE`, `NETHER_STALK`, `RAW_FISH`, `RAW_FISH:2`); until it's wired in,
  the Accessory Bag has its 9 and the other bags are locked. The Accessory Bag's slots from Jacobus, the
  Redstone Miner and Elizabeth aren't here either (a Sandbox profile has every slot anyway), nor Elizabeth's Account Upgrades for the Sack of Sacks
  and for more loadouts and sets.
- **A rank that drops**: Hypixel moves what's in a set that's locked then to the stash; here it stays in
  the set, unseen, until the rank comes back.
- **Learning Stone Powers**: from Maxwell (the Thaumaturgist) with nine Power Stones, at a Combat
  level; there's no Maxwell, so every Stone Power can be picked until there is (`Powers.unlocked`).
  Stats Tuning, Enrichments, Abicases' contacts and the Rift Prism aren't here.
- **Pets, saved HOTM/HOTF trees and tuning templates** in loadouts: shown as None.
- **Equipment outside loadouts**: Hypixel's right-click to equip and the Stats & Equipment menu's slots.
- **Sacks' contents**: the Sack of Sacks keeps sack items; picking up into sacks and Insert inventory
  aren't here, nor the Fishing Bag's Use Baits From Bag.
- **The Ender Chest block** opening the SkyBlock Ender Chest (vanilla's stays closed, see SandboxStorage).
- The lore Hypixel adds to accessories ("Accessory Power: +N"), which would change every accessory's
  golden.

## Data

Hypixel's numbers are kept out of this repository, in the private data checkout's `storage/`, which
servermgr links into every server as `plugins/dungeons/storage` (read at startup off the main thread;
without it there are no Accessory Powers and the bags are as small as they start, and the log says so):

- `powers.json`: each power's stats and bonus, tier, stone and Combat level; Accessory Power by rarity;
  each stat's multiplier. From the wiki's Power Stone pages (checked against the stones' own lore) and
  the recorded Select Power Stone for the powers everyone has, by the script kept next to it.
- `accessories.json`: which accessories upgrade into which: `tools/storage/build_storage.py` from the
  NotEnoughUpdates repository.
- `bags.json`: each bag's size by collection tier: `tools/storage/build_storage.py` from Hypixel's
  collections API.

The tests read them through `-Dstorage.dir` (else the checkout next to this repository) and skip what
needs them when they aren't there.
