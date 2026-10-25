# Museum data

The Museum (see MUSEUM.md) takes Hypixel's list of items and armor sets, with the SkyBlock XP each gives. That is
Hypixel's, so it lives in the private data repository; the script here makes it.

## `build_museum.py`

```
node tools/replay/replay.js decode REC.mcpr --out REC                                   # the menus, as gui.txt
python3 tools/museum/build_museum.py --gui REC/gui.txt                                  # fetches the items API
python3 tools/museum/build_museum.py --gui REC/gui.txt --api items.json --out DIR       # a saved response, elsewhere
python3 tools/museum/test_build_museum.py                                               # its checks (no data needed)
```

It writes `museum.json` (format 1, below) and `report.md` next to it.

- **Where it goes.** By default into `museum/` of the private data checkout, `skyblock-dungeon-data/`, next to this
  repository's. servermgr links that folder into every server as `plugins/dungeons/museum`, and the plugin reads
  `museum/museum.json` from its data folder when it starts, off the main thread (without it there is no Museum; the
  log says why). The plugin's tests read it with `-Dmuseum.dir=DIR` (the folder with `museum.json`), else from the
  data checkout, and skip what needs it when it isn't there.
- **Sources.** A recording of Hypixel's own Museum menus (`--gui`, a decoded replay's `gui.txt`) with every page of
  every category opened and Museum Milestones scrolled to its end (the milestones' rewards): each category's entries
  in the menu's first sort (Ascending XP), their names, the SkyBlock XP
  each gives ("Reward: +4 SkyBlock XP"), whether it's an armor set, and the higher tier that counts as it ("Donating a
  Yellow Belt will count as donating this item."). Hypixel's items API (`--api`, a file or the URL; no key) has the
  rest in its `museum_data`: an item's category, its starred versions (`mapped_item_ids`), its higher tier
  (`parent`), and for a set's pieces the sets they belong to (`armor_set_donation_xp`). The API's items marked
  `museum: true` with no `museum_data` are its Special Items.
- **What the recording wins.** Its entries, names, order and XP: an entry the API doesn't have is left out, one the
  API has and the recording doesn't is left out, and a higher tier the menu doesn't name isn't kept (Sponge Armor's
  Shark Scale); the report lists each. A recorded set is matched to the API's sets by name: first where all its pieces'
  names share its words ("Celeste Armor" is the Celeste pieces), then by the set's id ("Seymour's Special Armor" is
  SEYMOUR), then by a word's first letters ("Vanquisher Set" is VANQUISHED).
- It needs python3 (standard library only).

### Format 1

```
{"format": 1, "source": {"api": LAST_UPDATED, "recording": NAME},
 "categories": [{"id": "COMBAT", "name": "Combat", "entries": [ENTRY]}], "special": [ITEM_ID],
 "milestones": [{"milestone": 1, "xp": 100, "rewards": ["&8+&b1% Bits Multiplier", ...]}]}
```

- `ENTRY`, an item: `id` (its item id), `name`, `xp`, `items` (the ids that are it: its own, then its starred ones).
- `ENTRY`, an armor set: `id` (the API's set id, "MONSTER_RAIDER"), `set` (true), `name`, `xp`, `pieces` (what the set
  is made of, one list a piece it's worn as, each with the ids that can be that piece: Monster Raider takes Spider's
  Boots or Tarantula Boots).
- Either may have `parent` (the entry that counts as donating it, maybe in another category: Hardened Diamond Armor's
  is Mining's Mineral Armor), `parent_name` (that entry's name as the menu wrote it, colour and all, "&9Revenant
  Falchion") and `parent_lines` (the menu's lines saying so, wrapped as it wrapped them).
- `special`: the item ids of the Special Items, sorted.
- `milestones`: the Museum Milestones menu's, from every view of it the recording scrolled to: the XP each takes and
  its reward lines.
