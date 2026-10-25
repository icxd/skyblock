# Museum

Madame Eleanor Q. Goldsworth III, Your Museum and its category pages, donating and retrieving, Museum Milestones and
the Museum Donations SkyBlock XP, as recorded on Hypixel (2026-10-25, a fresh profile's first visit: every page of
every category, the milestones scrolled end to end, an Undead Sword donated and retrieved). The code is in
`paper/src/main/java/net/icxd/dungeons/museum`; Madame Goldsworth is `.../hub/MadameGoldsworth.java`.

## The data

`museum/museum.json` in the data checkout, made by `tools/museum/build_museum.py` (see its README) from that recording
and Hypixel's items API. servermgr links `museum/` into every server; without it the Museum takes nothing, the log
says so, and Madame Goldsworth says the wiki's "&cThis Museum is temporarily disabled. Check back again later!".

- 636 entries in seven categories (Combat 245, Farming 101, Mining 74, Fishing 66, Foraging 27, Dungeoneering 98,
  Hunting 25), 3,646 SkyBlock XP in all, as the recorded menus count them. Each is an item (with its starred
  versions) or an armor set (one of each of its pieces; Monster Raider takes Spider's or Tarantula Boots), with its XP
  and its higher tier ("Donating a Yellow Belt will count as donating this item.", the menu's lines as wrapped).
- The 40 Museum Milestones' rewards, as the recorded menu lists them.
- The 337 Special Items the API marks (not used yet).

## Done

- **Madame Goldsworth** stands where the Hub's table has her (33.5 73 11.5). The first click: her seven recorded
  lines (0, 1.5, 4.5, 7.5, 10, 13 and 17 s in), in a villager's voice at pitch 2, and Your Museum opens with the last.
  After that a click opens it at once. Both open with a click, as recorded.
- **Your Museum**: the Museum's head with "Items Donated: 0.2%" and its bar, the seven categories with theirs, Special
  Items, Museum Milestones (Total XP, Milestone 0/40, the progress to the next), Close, Museum Search and the Museum
  Appraisal Service ("Unlocked at Museum Milestone 3").
- **A category's pages**, "(1/9) Museum ➜ Combat" ("Museum ➜ Foraging" with one): 28 entries a page.
  - Missing: gray dye, the name in red, how to donate it, its higher tier, "Reward: +1 SkyBlock XP".
  - Donated: the item itself (a set's helmet) with "Donated: Oct 24, 2026", "Created" (an item that knows when it was
    made: new unstackable items now keep Hypixel's `timestamp`), "Reward: +1 SkyBlock XP ✔" and "Click to retrieve
    item!". Dates are New York's (a donation at 00:31 UTC said Oct 24).
  - Retrieved: lime dye with the item's name and lore and "You have retrieved this from your Museum but can add it
    back at any time."
  - Previous and Next Page (silent, as recorded), Extra Details (toggles the dates; the menu opens again with a click,
    as recorded), Go Back, Close, Museum Search, Sort (Ascending XP, Descending XP, Missing First, A to Z, Z to A; left
    for the next, right for the one before). The sort and Extra Details are kept with the profile (`museum.sort`,
    `museum.details`).
- **Donating**: with a Museum menu open, a click on an item in the inventory opens Confirm Donation (the item, its
  Soulbound warning, Cancel). Confirming takes it (an armor set's every piece, all from the inventory), marks it
  Co-op Soulbound (Hypixel's `donated_museum`: "&8&l* &8Co-op Soulbound &8&l*" in its lore, and shops won't buy it),
  keeps it in `museum.donations.<entry>` with when, and says "&aYou donated your &fUndead Sword &ato the &cCombat
  &asection of the Museum! &b+1 SkyBlock XP" with the coin chime and a level-up. The menu it came from opens again.
- **Retrieving**: a click on a donated item gives it back at once, with the recorded two lines; it still counts. A
  retrieved one can be donated again.
- **What counts**: an entry once donated, retrieved or not, or once a higher tier that counts as it is. Its XP is the
  SkyBlock XP task Museum Donations (3,646 at most), shown on the action bar as "+1 SkyBlock XP (Museum Donations)".
- **Museum Milestones**: the 40 on a skill menu's path (scrolling, Re-Center), red past the next, yellow for the next
  with its progress, "You can't claim this reward!" until reached.
- **/yoinkprofile**: items keep Hypixel's `donated_museum` (Co-op Soulbound) and `timestamp` (Created).

## Approximated (UNKNOWN)

- What a refused click says: an item the Museum doesn't take, one already donated, one a higher tier already counts
  for, an armor set without every piece in the inventory, a retrieval with a full inventory.
- How an entry that counts only by its higher tier shows (lime dye, its tier's lines, the reward ticked).
- Which piece a donated set shows (its helmet), and a set's name in the chat lines (in its first piece's colour).
- With Extra Details off, which lines stay (all but the dates). Whether the sort and the toggle are kept, and per
  what. The sort's ties (the menus' order), and a sort going back to the first page.
- Cancel on Confirm Donation (back to the menu). A donation of a retrieved entry says no XP.
- Milestones once reached (lime glass, "UNLOCKED"): claiming them was never recorded; a right-click scrolls 4.
- The milestone bar's colour once it fills (dark aqua, as its "/").

## Not done

- **Item values**: Your Museum's Top Items, the entries' Item Value and Clean Value, the Appraisal Service (10M once
  at Milestone 3) and the Museum Browser.
- **Displays**: the Museum's display cases, the Atrium slots and Remove From Display.
- **Special Items**: their page is empty; none can be donated.
- **Museum Search** (a click does nothing) and other players' Museums.
- **Milestone rewards**: Bits, bank interest and upgrades, the Travel Scroll, the Hex at Milestone 8, Dust and Eon
  the Fish, Shen's, the Flower Minion recipe, the Collecting Top Hat emblems. The wiki's "You have earned a reward for
  donating an item! CLICK HERE to claim it!" isn't said.
- **/yoinkprofile's Museum**: the museum API (`/v2/skyblock/museum`) isn't read.
- The Curator.
