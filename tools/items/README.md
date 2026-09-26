# Item data

The plugin's items show their text the way Hypixel's do today. That text comes from Hypixel's own
items, and the scripts here turn it into the plugin's data.

## Sources

- **NotEnoughUpdates-REPO** (`items/<ID>.json`, https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO):
  in-game dumps of every item's name and lore, including each enchanted book at each level. The
  item text (gemstones, Crimson armor, the hand-written items) and the enchantment descriptions come
  from here.
- **The recordings** (the private data repository): the items in a real inventory, and what they look
  like with reforges, enchantments, stars and gemstones. The layout rules in `ItemBuilder` were checked
  against these.
- **Hypixel's auction house** (`api.hypixel.net/v2/skyblock/auctions`, September 2026): about 45,000
  live items. These settled the rules the other two can't show: when enchantments get their
  descriptions, the order of stats, the bracket after each stat, and how wide generated text wraps.
- **The wiki** (hypixel-skyblock.fandom.com): stat names, symbols and colours
  (`Module:Statname/Data`), mob types, and per-level enchantment values (`data/enchant_values.json`,
  used only for levels that have no enchanted book in-game).

Hypixel's resource pack draws icons with private-use glyphs; the plugin sends no resource pack, so it
uses each glyph's classic symbol instead (✎ for the mana icon, ❁ for strength, and so on).

## Scripts

```
git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO /tmp/neu
python3 tools/items/gen_enchantments.py /tmp/neu/items   # paper/src/main/resources/enchantments.json
python3 tools/items/gen_tables.py /tmp/neu/items         # item/items/CrimsonArmor.java, Gemstones.java
```

- `gen_enchantments.py`: each enchantment in `EnchantmentType` gets its name and its description at
  each level from the enchanted books. Where Hypixel has no book for a level, that level's values are
  put into the nearest level's text; where the values aren't known either, the level has no description.
- `gen_tables.py`: the Crimson armor (stats and tier bonus text by tier and piece) and the gemstones
  (text by type and quality) from the item dumps, filled into `templates/`. `data/` holds what the
  plugin had before and Hypixel's dumps don't: the Crimson star costs and the head textures.

Both write their output in place; `git diff` shows what changed.
