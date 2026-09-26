# Item data

The plugin's items show their text the way Hypixel's do today. That text comes from Hypixel's own
items, and the scripts here turn it into the plugin's data.

## Every item: `build_items.py`

```
git clone --depth 1 https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO /tmp/neu
python3 tools/items/build_items.py --neu /tmp/neu                          # fetches the items API
python3 tools/items/build_items.py --api items.json --neu /tmp/neu --out DIR   # a saved response, elsewhere
python3 tools/items/check_items.py [DIR/items.json]                        # checks the file against format 1
python3 tools/items/test_build_items.py                                    # the lore parser's tests
```

It turns every item Hypixel has (about 5,650) into one `items.json`, and writes `report.md` next to it.

- **Where it goes, and why not here.** By default into `items/` of the private data repository's
  checkout, `skyblock-dungeon-data/`, next to this repository's. servermgr links that `items/` folder
  into every server as `plugins/dungeons/items`, and the plugin reads `items/items.json` from its data
  folder when it starts (without it, only the Java items are there). The file is Hypixel's item text
  and numbers, and this repository is public, so only the code and tables that make it are here.
  Commit `items.json` and `report.md` in the private repository after reading the report.
- **Sources.** [Hypixel's items API](https://api.hypixel.net/v2/resources/skyblock/items) (no key)
  wins for the numbers: stats, costs, requirements, sell price, rarity, flags and material. NEU's
  in-game dumps give the text: the name, the dark gray lines under it, the item's own lines, and its
  ability and bonus blocks, in the order Hypixel shows them. NEU also gives what the API doesn't have:
  the Gear Score and Shot Cooldown shown, the rarity line's words, and whether the item says "This item
  can be reforged!". An item NEU has no dump of gets the API's description, wrapped the way `Text.wrap`
  does it. `source` in the file records the API's `lastUpdated` and the NEU commit.
- **What the plugin writes itself is left out**: stat lines, the gemstone line, cost lines under a
  block, the reforge note, requirements, soulbound and the rarity line (a stat line or cost the format
  has no number or field for stays a line of text).
- **So is what belongs to the one copy NEU captured**: the lines naming its owner (Purchased by, Earned
  by, Player:, Awarded to, Found by, Discoverer, Hunter, a gift's To and From, with the rest of their
  section, such as Position and Score) and the copy's auction, bid and edition number and date. The
  plugin doesn't write these yet; the report lists the items (`owner_text`).
- **Also left out, and not written by the plugin yet**: "Works while in Accessory Bag!", the
  Rift-Transferable line, Accessory Power and the capture menu's "Right-click to view recipes!"; the
  report counts them.
- **Kept as captured**: lines whose values change with the item's state (counters, progress, minion
  stats) show the state NEU caught; the report lists them (`dynamic_text`).
- **Tables** (`data/`): `materials.tsv` turns Hypixel's 1.8 material and data value into the 26.2
  material (rows marked `vanilla` come from vanilla's own ItemIdFix and ItemStackTheFlatteningFix, the
  rest were done by hand and checked against the item ids Hypixel sends today, in NEU's
  `itemsOverlay/`; an item whose pair isn't there is skipped, and the report says so). Every material
  is checked against `Material` and `ItemType` in the paper-api jar the plugin builds with (from
  `~/.m2`, or `--paper-api`); items Hypixel draws with its resource pack stay `PAPER`. `glyphs.tsv`
  gives each private-use glyph its classic symbol and says how that was found; a glyph with no symbol
  is dropped, and the report says where.
- **The report** lists the counts, the items whose text needs a person or code, dropped glyphs, the
  names the plugin doesn't model yet (read from its own enums in `paper/src`), what the format has no
  place for, and where the API and NEU disagree.
- It needs python3 (standard library only), git (for the NEU commit) and the paper-api jar (any build
  of the plugin downloads it). On the same inputs it writes the same bytes.

### Format 1

One JSON object: `{"format":1,"source":{...},"items":{` then one item a line, `"ID":{...}`, sorted by
id, keys sorted, and every default left out, so a changed item is a one-line diff. Only `name` and
`material` are always there.

| field | |
|---|---|
| `name` | without the rarity colour (codes only where Hypixel's name changes colour) |
| `material` | a 26.2 `Material` name |
| `rarity`, `type`, `type_label` | `Rarity` name (COMMON left out); Hypixel's category; the rarity line's words when they aren't `type`'s (the plugin adds DUNGEON) |
| `categories`, `lore` | the dark gray lines under the name; the item's own lines (`""` a blank line) |
| `texture`, `skin`, `color` | a head's texture hash (up to 64 lower-case hex digits, for `Utils.texture`), or a whole skin value; leather's `#rrggbb` |
| `glowing`, `unstackable`, `dungeon_item`, `can_have_attributes` | only when true |
| `reforgeable` | whether NEU's dump says it can be reforged (only items NEU has) |
| `soulbound`, `gear_score`, `npc_sell_price`, `shot_cooldown` | `COOP`/`SOLO`; the Gear Score shown; coins; seconds |
| `stats` | `{STAT: number}` in the plugin's `Stat` names (a name it doesn't have is kept) |
| `gemstone_slots`, `upgrade_costs`, `requirements` | `[{type, costs}]`; a list of costs per star; the API's requirement objects |
| `abilities` | the blocks in order: `kind` (ABILITY, FULL_SET, PIECE, TIERED, EXTRA, SHORTBOW), `name`, `header` (the line as Hypixel shows it), `activation`, `text`, and `mana`, `mana_percent`, `cooldown` (seconds), `soulflow`, `health_cost`, `vitality`, `pieces` |

A cost is `{"coins":N}`, `{"item":"ID","amount":N}` or `{"essence":"WITHER","amount":N}`. Text uses `&`
codes.

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

## The other scripts

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
