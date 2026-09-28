# Collection and recipe data

The plugin's collections, their tiers and what each tier gives, the boss collections, and the
crafting recipes and Recipe Book, the way Hypixel has them. That is Hypixel's data, so only the script
that makes it is here.

## `build_collections.py`

```
git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO /tmp/neu
python3 tools/collections/build_collections.py --neu /tmp/neu                 # fetches the API and the wiki pages
python3 tools/collections/build_collections.py --api collections.json --neu /tmp/neu --wiki-cache DIR --out DIR
python3 tools/collections/test_build_collections.py                           # its tests
```

It writes `collections.json`, `recipes.json` and `report.md`.

- **Where it goes, and why not here.** By default into `collections/` of the private data
  repository's checkout (`skyblock-dungeon-data/`, next to this repository's). servermgr links that
  folder into every server as `plugins/dungeons/collections`, and the plugin reads both files from
  there when it starts, off the main thread (`CollectionFiles`); without them there are no
  collections or recipes, and the menus show none. The plugin's tests read them from
  `-Dcollections.dir` (else that checkout's `collections/`) and skip the recorded-menu checks without
  them. Commit the three files together in the private repository after reading the report.
- **Sources.** [Hypixel's collections API](https://api.hypixel.net/v2/resources/skyblock/collections)
  (no key): the collections, their tiers, amounts and unlock lines. NotEnoughUpdates-REPO: the
  crafting recipes (`recipe`, and `recipes` of type `crafting`), which item an unlock line names when
  two share a name (its `crafttext`), the slayer levels its `crafttext` gives, the bosses' and pets'
  heads. The plugin's `items.json`: the items there are (a recipe with an ingredient that isn't there
  is dropped). The wiki (hypixelskyblock.minecraft.wiki, raw pages, kept in `--wiki-cache`):
  `Module:Collection/Data` for the boss collections, `<Category>/Collection UI` for the order of each
  category's collections, `Recipe Book/UI/<page>` for which recipes the book lists under which
  category, and `<Name> Slayer` for the slayer level whose Items Unlocked names a recipe nothing else
  gives a requirement for. `collections/recorded.json` in the private repository (`--recorded`) adds
  what the recordings show and the sources lack (Bonzo III's and VI's essence amounts).
- **What counts toward a collection** (the wiki's Collections): its own item, 1 each, and every item
  the game calls a "Collection Item" whose recipe chain is made of that item alone, as many as it
  takes (160 for Enchanted Rotten Flesh). The Mushroom and Gemstone collections take several items.
- **The report** lists the counts and what couldn't be matched: unlock lines naming no item, names two
  items share, collections the wiki's menu page doesn't place, Collection Items that count toward
  nothing, the wiki's book entries with no craftable item, and slayer recipes whose level no source
  gives (taken as the slayer's first).
- It needs python3 (standard library only) and git (for the NEU commit). On the same inputs it writes
  the same bytes.

### Format 1

`collections.json`: `{"format":1,"source":{...},"categories":[...],"collections":{...},"bosses":[...],"items":{...}}`.

| field | |
|---|---|
| `source` | the API's `lastUpdated` and `version`, the NEU commit |
| `categories` | the Collections menu's categories in order: `id` (FARMING), `name`, `collections` (ids, in the menu's order) |
| `collections` | by the API's id: `name`, `category`, `item` (the item the menus show), `tiers` |
| `bosses` | `id` (CATACOMBS_1, KUUDRA: `/viewbosscollection`'s), `name`, `floor` (the dungeon floor whose completions count; 0 for none), `texture` (the head's hash), `tiers` |
| `items` | item id → `[collection id, how many one of it adds]` |

A tier is `{"amount": N, "rewards": [...]}` (the amount is the total). A reward has a `type` and what
that type needs: `SKYBLOCK_XP` and `SLOTS` an `amount` (and `name`), `SKILL_XP` a `skill` and `amount`,
`EXP_DISCOUNT` a `name` and `percent`, `MINION_RECIPES` a `name` and the tier I minion's `item`,
`PET_RECIPE` a `name` and `texture`, `RECIPE`, `TRADE`, `FORGE_RECIPE` and `ITEM` a `name` and `item`
(left out when there's no such item) and maybe an `amount`, `ESSENCE` an `essence`, `amount` and its head's `texture`, `STAT`
the plugin's stat `name` (MINING_FORTUNE), `amount` and `text`, and `UNLOCK` its `text`.

`recipes.json`: `{"format":1,"source":{...},"recipes":{...},"book":[...]}`.

| field | |
|---|---|
| `recipes` | by the item made: `shapes` (each `cells`, nine `"ID:amount"` or `""` row by row, `count` made, `shapeless` when true: NEU has none), `requires`, `later` (`minions`: a system the plugin doesn't have yet) |
| `requires` | `{"collection": ID, "tier": N}` (a boss's too), `{"slayer": ZOMBIE, "level": N}` or `{"other": text}`; left out for a recipe everyone has |
| `book` | the Recipe Book's entries, by category in the book's order: `category`, `name`, `item`, `kind` (left out for `RECIPE`; `MINION`, `PET`, `OTHER` for the ones that can't be crafted here yet), `requires`, `texture` |

Text uses `&` codes, with Hypixel's private-use glyphs turned into the classic symbols
(`tools/items/data/glyphs.tsv`).
