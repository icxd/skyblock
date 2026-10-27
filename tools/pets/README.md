# Pets data

Pets (see PETS.md) need Hypixel's pets: each rarity's head, stats, ability text and numbers, and the pet XP tables.
That is Hypixel's, so it lives in the private data repository; the script here makes it.

## `build_pets.py`

```
node tools/replay/replay.js decode REC.mcpr --out REC                                  # the Pets menu, as gui.txt
python3 tools/pets/build_pets.py --neu NEU-REPO --gui REC/gui.txt                      # fetches the wiki's Module:Pet/Data
python3 tools/pets/build_pets.py --neu NEU-REPO --gui REC/gui.txt --wiki SAVED.json    # a copy saved with --save-wiki
python3 tools/pets/build_pets.py ... --kat KAT_COSTS.txt                               # a saved Kat/Costs page
python3 tools/pets/test_build_pets.py                                                  # its checks (no data needed)
```

It writes `pets.json` (format 1, below) and `report.md` next to it.

- **Where it goes.** By default into `pets/` of the private data checkout, `skyblock-dungeon-data/`, next to this
  repository's (`--out` for elsewhere). servermgr links that folder into every server as `plugins/dungeons/pets`, and
  the plugin reads `pets/pets.json` from its data folder when it starts, off the main thread (without it there are no
  pets; the log says why).
- **Sources.**
  - NotEnoughUpdates-REPO (`--neu`, a clone): its in-game dumps of every pet at every rarity (`items/<PET>;<n>.json`,
    0 Common to 5 Mythic: Hypixel's own lines, with `{n}` and `{STAT}` where a number changes with the level), its level
    1 and level 100 numbers (`constants/petnums.json`), and the XP tables (`constants/pets.json`).
  - The wiki's `Module:Pet/Data` (fetched, or `--wiki` for a copy saved with `--save-wiki`): each number's own formula,
    a base and so much a level.
  - A recording of Hypixel's Pets menu (`--gui`, a decoded replay's `gui.txt`): its pets are checked against what the
    data makes of them at their levels, and its Pet Score Rewards item has the Pet Score's Magic Find thresholds.
  - What Kat takes to upgrade each rarity: the wiki's `Kat/Costs` table (fetched, or `--kat` for its saved raw text),
    and for the pets it doesn't have, their own page's `{{Kat Cost table}}` (the Orchid Mantis, the Sloth). Its items'
    names become ids by NEU's item names. George's prices: NEU's `constants/george.json` (the wiki's George/Prices).
  - The pet skins: NEU's `PET_SKIN_` items (their names, rarity lines, heads, and the pets their lines say they're for).
- **The text.** The type line ("Farming Pet": the pet's skill), the stats, then each ability's name and lines and any
  other block, as NEU has them; the footer (the right-click line, Kat's, the rarity line) is the plugin's to write.
  Hypixel's private-use glyphs become the plugin's symbols (`tools/items/data/glyphs.tsv`). Lines are joined back into
  the text Hypixel wrapped, as paragraphs (a line goes on with the one before unless it starts with a space, follows
  one that does, or turns dark gray), since the numbers in them change how they wrap: the plugin wraps each at 188
  pixels, the width that re-wraps every recorded one as recorded.
- **The numbers.** Each is `base + per_level x level`. The wiki's formula when it gives NEU's level 1 and level 100
  values (exactly, as Hypixel shows them rounded, or a decimal off: NEU's are typed in at times); else the one with the
  fewest decimals that does; else the wiki's when it gives the level 100 value (the Spider's Strength: NEU's level 1
  value is its Crit Chance's, the recording agrees with the wiki). A number NEU left as it is at level 1 where the wiki
  has it go up with the level (the Mythic Spider's Web Battlefield, "+4.14" at level 69, recorded) becomes a placeholder.
  The dragons' (Golden, Jade, Rose) numbers are at levels 100 to 200 (the egg hatches at 100).
- **Decimals.** A number shows as many decimals as NEU's level 1 value has when that's the number rounded (the
  Guardian's Lucky Seven is 0.07 at level 1 and "0.1" there, so "6.6" at 94, recorded); else as many as it can have (the
  Bee's Hive, "1.16" at 4). Rounded half-even, as Java's DecimalFormat rounds a double: NEU has 26.2 for the Zombie's
  26.25. Stats always show two at most, as the plugin's other stats.
- **The report** says how each number was made, which recorded pets differ from the data (none, for the recording of
  2026-10-25), and every number no clean formula fits (NEU and the wiki disagree), pet left out (Fractured Montezuma
  Soul: a fixed level) or stat the plugin doesn't have (Overbloom).
- It needs python3 (standard library only), and `tools/items/build_items.py` next to it (the glyphs, widths and
  wrapping).

### Format 1

```
{"format": 1, "source": {"neu_commit": SHA, "wiki_revision": REVID, "kat_revision": REVID, "recording": NAME},
 "levels": [100, 110, ...], "rarity_offset": {"COMMON": 0, "UNCOMMON": 6, ...}, "max_level": 100,
 "score": {"rarity": {"COMMON": 1, ...}, "max_level": 1, "rewards": [[10, 1], [25, 2], ...]},
 "pets": {"BEE": PET}, "skins": {"ENDERMAN_SLAYER": SKIN}}
```

- `levels`: the XP each level takes from the one before; a pet of a rarity starts `rarity_offset` into them (a Rare's
  level 2 takes `levels[11]`). `max_level`: what pets reach.
- `score`: a pet type's Pet Score by its best rarity, what a maxed one adds, and the Magic Find at each threshold.
- `PET`: `name` ("Bee"), `kind` (its type line, "Farming Pet", "Mining Mount", "All Skills"), `skill` (the plugin's
  Skill it gains most from, "ALL" for every one, null for none), `rarities` (by rarity, `RARITY`), and where the pet's
  own: `max_level` (200), `extra_levels` (the XP of the levels past 100, after `levels`), `rarity_offset` (the Bingo
  pet's, all 0), `xp_multiplier` (the Reindeer's 2), `stats_from` (the level its stats and numbers start at: 100).
- `RARITY`: `texture` (its head's skin, the hash of its textures.minecraft.net URL), `name` (where not the pet's),
  `stats` (by the plugin's Stat name: `{"base", "per_level"}`), `numbers` (`{n}` in the text is `numbers[n]`:
  `{"base", "per_level", "decimals"}`), `blocks` (`{"name": "&6Hive", "text": [PARAGRAPH]}`, or without `name`: the
  lines after the stats, in order, a blank line between each), `kat` (true: Kat can upgrade it), `kat_upgrade` (what
  that takes: `{"seconds", "coins", "items": [[ID, AMOUNT]]}`), `george` (the coins George pays for it).
- `SKIN`, by its id (a pet's `skin`, its item's id less `PET_SKIN_`): `name` ("Void Conqueror Enderman Skin"), `short`
  (what a skinned pet's type line says: "Combat Pet, Void Conqueror Skin"), `rarity` ("EPIC", "ULTIMATE"), `color`
  (that rarity's colour code), `texture`, and `pets` (the types it's for).
