# The Hex's data

The Hex's tables (see HEX.md) are Hypixel's, so they're kept in the private data repository's `hex/`, which
servermgr links into every server as `plugins/dungeons/hex`. The plugin reads them when it starts, off the main
thread; its tests read the folder through `-Dhex.dir` (else `skyblock-dungeon-data/hex/` next to this
repository's checkout) and skip what needs it when it isn't there.

## Enchantments: `build_enchants.py`

```
git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO /tmp/neu
python3 tools/hex/build_enchants.py --neu /tmp/neu                                   # fetches the wiki's pages
python3 tools/hex/build_enchants.py --neu /tmp/neu --wiki-cache SAVED --out DIR       # keeps them, elsewhere
python3 tools/hex/test_build_enchants.py                                             # its tests
```

It writes `enchantments.json` (format 1, below) and `enchantments_report.md` next to it, by default into
`hex/` of the private data checkout. Every enchantment there is (156), not only those the plugin has code for:
the plugin shows every one's text on items (ENCHANTMENTS.md), and the Hex offers them.

- **Sources.** NotEnoughUpdates-REPO's dumps of the enchanted books as the game shows them
  (`items/<ID>;<level>.json`): each enchantment's name now (Syphon is Drain, Dragon Hunter is Gravity), its
  description at each level (the book's lines, and as one paragraph for item lore, as items wrap it), what the
  next tier takes ("50k Combat XP to tier up!"), the book's rarity, the Enchantment Table's levels (its sources'
  "I-V: Enchantment Table"), what it goes on ("Applied To") and what it can't be with ("Conflicts"). NEU's
  `constants/enchants.json`: the Exp levels each level costs (`enchants_xp_cost`) and the table's levels where the
  books don't say. Its conflict pools (`enchant_pools`) are older than the books and only reported: they have the
  Turbos conflicting with each other and Fortune with Smelting Touch, which live items carry together. The wiki's `Module:Enchantment/Data`: each level's values
  (for the levels with no book: the nearest book's text with that level's numbers), the Enchanting level each
  needs, the highest tier of those that grow with use, their tiers, and the Exp levels NEU doesn't have. The
  wiki's `The Hex/UI/Weapon` and `The Hex/UI/Armor`: the order the Hex lists them in, as far as they show it.
  `--wiki-cache DIR` keeps the pages fetched and reads them back, so a run can be repeated offline.
- **Where sources disagree** the books win over NEU's constants, and NEU's constants over the wiki; the report
  lists every disagreement (Exp levels, the table's levels, highest levels, conflicts), the levels with no text, the
  levels no source gives the Exp levels of, and the glyphs with no classic symbol (see tools/items).
- **Taken out.** Telekinesis is kept (old items have it) but marked `removed`: Hypixel replaced it with
  auto-pickup in 2022, and the Hex doesn't offer it.
- **Ids.** An enchantment is keyed by what items store it as: Hypixel's id, but an ultimate's without its
  `ultimate_` (the plugin has always stored One For All as `one_for_all`); `hypixel` is Hypixel's.
- It needs python3 (standard library only), `tools/items/build_items.py` (the glyph table) and
  `tools/collections/build_collections.py` (the wiki's pages and Lua data). On the same inputs it writes the same
  bytes.

### Format 1

One JSON object, `{"format":1,"source":{...},"order":[...],"enchantments":{` then one enchantment a line.

| field | |
|---|---|
| `source` | the NEU commit, and the wiki pages read |
| `order` | ids in the Hex's Default order: the wiki's Hex screens' (a sword's list, its ultimates, armor's ultimates), then the rest by name |
| `name`, `hypixel`, `ultimate` | the name the game shows; Hypixel's id; whether it's an ultimate enchantment |
| `removed` | only when true: Hypixel took it out of the game |
| `min`, `max` | the lowest and highest level the Hex offers (a tiered one's highest is its last tier) |
| `table` | the highest level the Enchantment Table sells; null for none (above it, the Hex wants the book) |
| `xp` | the Exp levels by level from 1; null where no source says |
| `enchanting` | the Enchanting level it needs (0 for none) |
| `applies` | the books' "Applied To" words ("Sword", "Armor", "Farming Tool", "Precursor Eye") |
| `conflicts` | the ids it can't be on an item with (both ways; the books' Conflicts) |
| `levels` | by level: `text` (one paragraph, `&` codes), `lines` (the book's lines), `rarity` (the book's), `tier_up` (the line on the next tier) |
