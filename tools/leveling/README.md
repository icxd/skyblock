# SkyBlock Leveling data

SkyBlock Leveling (`/levels`) shows Hypixel's XP tasks, level rewards, emblems and SkyBlock Guide. That is Hypixel's
text and numbers, so it lives in the private data repository; the script here makes it.

## `build_leveling.py`

```
node tools/replay/replay.js decode REC.mcpr --out REC                                   # the menus, as gui.txt
node tools/replay/replay.js raw REC.mcpr 0 600 open_window window_items > windows.json   # their heads' skins
python3 tools/leveling/build_leveling.py --gui REC/gui.txt --raw windows.json --neu /tmp/neu --save-wiki SAVED
python3 tools/leveling/build_leveling.py --gui REC/gui.txt --raw windows.json --wiki SAVED --out DIR   # again, offline
```

It writes `leveling.json` (format 1, below) and `report.md` next to it.

- **Where it goes.** By default into `leveling/` of the private data checkout, `skyblock-dungeon-data/`, next to
  this repository's. servermgr links that folder into every server as `plugins/dungeons/leveling`, and the plugin
  reads `leveling/leveling.json` from its data folder when it starts, off the main thread (without it there is no
  SkyBlock XP; the log says why). The plugin's tests read it with `-Dleveling.dir=DIR` (the folder with
  `leveling.json`), else from the data checkout, and skip what needs it when it isn't there.
- **Sources.** A recording of Hypixel's own menus, decoded with `tools/replay` (`--gui`, its `gui.txt`; `--raw`,
  the same recording's raw `open_window`/`window_items` packets, for heads' skins): the nine categories of Ways to
  Level Up with their tasks, descriptions and totals (Hypixel's today), the four Rewards lists (features, prefix
  colours, emblems, bonuses), the Emblems menu, and the SkyBlock Guide's stages that were opened. The Hypixel
  SkyBlock wiki (`SkyBlock Levels`, `SkyBlock Levels/Tasks`, `SkyBlock Guide` and its `Tasks/` pages; the revisions
  go in `source`) gives each task's XP lines and parts, the emblems and what unlocks them, and the guide stages
  nobody recorded. Hypixel's collections API (`--collections`, no key) names the collections the guide asks for.
  NEU's `constants/sblevels.json` (`--neu`) is only checked against the rest: it's older (the report says where it
  differs). `--save-wiki DIR` keeps the pages fetched, and `--wiki DIR` reads them back, so a run can be repeated.
- **What the recording wins.** Its categories, their task lists and what each category is worth: a task the wiki
  doesn't have is kept with no XP lines, one the recording doesn't list is left out, and the report lists both. A
  recorded guide stage's tasks are the recorded ones, in the recorded order, with the wiki's parts where the names
  match.
- **What the plugin can tell** is written as `unlock`s (below): a skill's, Catacombs' or a class's level, the class
  average, a floor completed, a SkyBlock level, a collection's tier. Anything else (a Slayer level, a Museum
  donation) has none, and the plugin never counts it done.
- It needs python3 (standard library only), `tools/items/build_items.py` (text widths and glyphs) and the paper-api
  jar the plugin builds with (from `~/.m2`, or `--paper-api`), which the wiki's images are checked against as
  materials.

### Format 1

```
{"format": 1, "source": {"wiki": {PAGE: REVISION}, "recording": NAME, "neu": COMMIT},
 "categories": [CATEGORY], "rewards": [REWARD], "emblems": [EMBLEM_CATEGORY], "guide": [STAGE]}
```

- `CATEGORY`: `id` ("core"), `name` ("&3Core Tasks"), `icon`, `description` (lines), `max` (its XP in all, as
  recorded), `tasks`.
- `TASK`: `id` ("skill_level_up"; a part's is its parent's, a dot and its own), `name`, `icon`, `description` (the
  wiki's, to wrap), `xp` (lines: `text` "Level 1-10", `xp` 5, `from`/`to` for level lines, `note` the wiki's
  small print), `max` (the most it gives), `tasks` (parts), `floor` (a completion's floor: `ENTRANCE`, `FLOOR_1`,
  `MASTER_FLOOR_1`...).
- `REWARD`: `kind` (`FEATURE`, `PREFIX`, `EMBLEM`, `BONUS`), `level`, `name`, `icon`, `lore` (lines), `text`
  (a bonus's paragraphs, `{bonus}` for its size at the viewer's level), `color` (a prefix's colour code), `emblem`
  (an emblem reward's emblem id).
- `EMBLEM_CATEGORY`: `id`, `name`, `icon`, `description`, `emblems`: `id`, `name`, `symbol` ("&7♦"),
  `requirement` (the wiki's words), `unlock`.
- `STAGE`: `stage` ("starter"), `name` ("&aStarter"), `subtitle`, `lines` (recorded) or `description` (the wiki's,
  to wrap), `tasks` (how many it has in all), `groups`: `name` (in its colours), `task` (the Ways to Level Up task
  it belongs to), `lines` or `description`, `xp`, `icon`, `count` (its parts), `listed` (whether its item lists
  them), `items` (`text`, `unlock`), `progress` and `max` (a one-part task's "Progress to LVL 1", to 1), `unlock`.
- `icon`: `material` (a Bukkit material), `texture` (a head's skin hash on textures.minecraft.net).
- `unlock`: one of `{"skill": "MINING", "level": 50}`, `{"catacombs": 40}`, `{"class": "MAGE", "level": 50}`,
  `{"classAverage": 30}`, `{"floor": "FLOOR_7"}`, `{"level": 100}`, `{"collection": "CARROT_ITEM", "tier": 3}`.
