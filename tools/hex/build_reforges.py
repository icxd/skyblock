#!/usr/bin/env python3
# python3 tools/hex/build_reforges.py --neu NEU-REPO [--wiki-cache DIR] [--live JSON] [--items ITEMS.JSON] [--out DIR]
#
# Builds the Hex's reforge table, reforges.json (see REFORGES.md), and reforges_report.md next to it: every basic
# reforge (the Blacksmith's random ones, in their pools) and every Reforge Stone, with each reforge's Hypixel
# modifier id, name, stats and bonus text at each rarity, what each stone goes on and costs, and what a random
# basic reforge costs. From NotEnoughUpdates-REPO (constants/reforges.json and reforgestones.json), the wiki
# (Module:Reforge/Data, Reforging/Prices and Reforging's duplicate prefixes) and, where given, what live items
# show (--live: REFORGES.md says what it is); the report lists every place they disagree and what was
# taken. All of it is Hypixel's, so the output goes to the private data repository, never into this one.
import argparse
import collections
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'collections'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'items'))
from build_collections import Lua, Wiki, default_data, git, glyph_symbols  # noqa: E402
from build_items import wrap  # noqa: E402

STAT_JAVA = os.path.join(ROOT, 'paper/src/main/java/net/icxd/dungeons/stats/Stat.java')
PUA = re.compile('[\ue000-\uf8ff]')
RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'DIVINE', 'SPECIAL', 'VERY_SPECIAL']
# Special and Very Special give Mythic's values (the wiki's Reforging/Prices): the plugin works that out, so
# no source's values for them are kept.
SAME_AS_MYTHIC = {'SPECIAL', 'VERY_SPECIAL'}
# The wiki module's rarity keys, and its names in Reforging/Prices.
WIKI_RARITY = {'c': 'COMMON', 'u': 'UNCOMMON', 'r': 'RARE', 'e': 'EPIC', 'l': 'LEGENDARY', 'm': 'MYTHIC', 'd': 'DIVINE',
               'sp': 'SPECIAL', 'vs': 'VERY_SPECIAL'}
# The wiki module's stat keys, as NEU names the stats. Its other keys are numbers its descriptions put in ({1}).
WIKI_STATS = {'str': 'strength', 'cd': 'crit_damage', 'cc': 'crit_chance', 'def': 'defense', 'hp': 'health',
              'spd': 'speed', 'int': 'intelligence', 'bas': 'bonus_attack_speed', 'as': 'bonus_attack_speed',
              'mns': 'mining_speed', 'mnf': 'mining_fortune', 'scc': 'sea_creature_chance',
              'fs': 'fishing_speed', 'forf': 'foraging_fortune', 'foraging_fortune': 'foraging_fortune',
              'farming_fortune': 'farming_fortune', 'mf': 'magic_find', 'vit': 'vitality', 'dmg': 'damage',
              'td': 'true_defense', 'heatres': 'heat_resistance', 'bp': 'breaking_power', 'fero': 'ferocity',
              'hw': 'hunting_wisdom', 'huf': 'hunting_fortune', 'swp': 'sweep', 'ad': 'ability_damage',
              'frw': 'foraging_wisdom', 'foragingwis': 'foraging_wisdom', 'foraging_wisdom': 'foraging_wisdom',
              'mw': 'mining_wisdom', 'mining_wisdom': 'mining_wisdom', 'fmw': 'farming_wisdom',
              'farming_wisdom': 'farming_wisdom', 'bonus_pest_chance': 'bonus_pest_chance', 'overbloom': 'overbloom',
              'block_fortune': 'block_fortune', 'pull': 'pull', 'fear': 'fear', 'trophy_chance': 'trophy_chance'}
# NEU's stat names the plugin's Stat calls otherwise.
STAT_NAMES = {'bonus_attack_speed': 'ATTACK_SPEED', 'trophy_chance': 'TROPHY_FISH_CHANCE'}
# The names item lore gives stats that aren't the plugin's Stat's.
LORE_STATS = {'Bonus Attack Speed': 'ATTACK_SPEED'}
# The basic pools, in the order the wiki's Reforging lists them: NEU's itemTypes of each.
POOLS = ['SWORD/ROD', 'BOW', 'ARMOR', 'EQUIPMENT', 'PICKAXE', 'AXE', 'FARMING_TOOL']
# The wiki module's categories (and a Tool's "tool") as NEU's item types, for the stones NEU doesn't have.
WIKI_TYPES = {'Sword': 'SWORD', 'Armor': 'ARMOR', 'Equipment': 'EQUIPMENT', 'Ranged Weapon': 'BOW', 'Fishing Rod': 'ROD',
              'Axe': 'AXE', 'Pickaxe, Drill': 'PICKAXE', 'Farming Tool': 'FARMING_TOOL', 'Vacuum': 'VACUUM'}
# A live stat bracket counts once this many items of the reforge and rarity show one; a stat NEU gives that
# never shows on at least this many is taken to be none.
SEEN = 3


def load_json(path):
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def modifier_of(name, override=None):
    """Hypixel's `modifier` for a reforge: NEU's nbtModifier, else its name lower case, only letters, digits and
    underscores, spaces and hyphens as underscores (SkyHanni's NeuReforgeJson does the same)."""
    if override:
        return override
    return re.sub(r'[\s-]', '_', re.sub(r'[^a-z0-9\s_-]', '', name.lower()))


def plain(text):
    """Text without colours, glyphs and symbols, to compare what sources say."""
    text = re.sub(r'[&§][0-9a-fk-or]', '', text, flags=re.I)
    # Symbols and glyphs go (the sources write a stat's symbol, its glyph or neither): the words are compared.
    text = re.sub(r'[^A-Za-z0-9\s%+.,:;!?()\'/-]', '', text)
    return ' '.join(text.split())


class Sources:
    """NEU's two tables, the wiki's module and prices, the live observations (or none), and the plugin's stats."""

    def __init__(self, neu_dir, wiki, live, stats, display, glyphs):
        self.basic = load_json(os.path.join(neu_dir, 'constants', 'reforges.json'))
        self.stones = load_json(os.path.join(neu_dir, 'constants', 'reforgestones.json'))
        self.wiki = module_table(wiki.page('Module:Reforge/Data'))
        self.prices_page = wiki.page('Reforging/Prices')
        self.reforging_page = wiki.page('Reforging')
        self.live = (live or {}).get('reforges', {})
        self.stats = stats
        self.display = display
        self.glyphs = glyphs
        self.neu_dir = neu_dir

    def stone_bonus(self, stone_id):
        """The bonus section a stone's own lore shows (NEU's in-game dump; the stone shows its Legendary
        reforge): heading first, then its lines; None if it has none."""
        path = os.path.join(self.neu_dir, 'items', stone_id + '.json')
        if not os.path.exists(path):
            return None
        lore = load_json(path).get('lore', [])
        for i, line in enumerate(lore):
            if re.fullmatch(r'§9.+ §7\(§.Legendary§7\):', line):
                end = lore.index('', i) if '' in lore[i:] else len(lore)
                # (Not the rarity line, "§9§lRARE REFORGE STONE", after a stone with no bonus.)
                if end + 1 < len(lore) and lore[end + 1].startswith('§9') and '§l' not in lore[end + 1]:
                    body = lore[end + 1:]
                    return body[:body.index('')] if '' in body else body
        return None


def module_table(module):
    """A wiki data module's table: the one it returns (a commented-out template comes before it)."""
    start = re.search(r'^\s*return\s*\{', module, re.M).end() - 1
    # Lua's "1." is 1.0, which the reader doesn't know.
    lua = Lua(re.sub(r'(=\s*-?\d+)\.(?=\s*[,}\n])', r'\1.0', module[start:]))
    lua.take('{')
    return lua.table()


class Builder:
    def __init__(self, src):
        self.src = src
        self.notes = collections.defaultdict(list)
        # The wiki's entries by name (its categories as a field).
        self.wiki = {}
        for category, entries in src.wiki.items():
            for name, entry in entries.items():
                self.wiki[name] = dict(entry, category=category)

    def note(self, kind, text):
        self.notes[kind].append(text)

    # ---------- stats ----------

    def stat(self, neu_name, where):
        """A stat by NEU's (or the wiki's, as NEU's) name, as the plugin's Stat; None, noted, if it has none."""
        name = STAT_NAMES.get(neu_name, neu_name.upper())
        if name not in self.src.stats:
            self.note('stat not in the plugin', f'{where}: {neu_name} (left out)')
            return None
        return name

    def lore_stat(self, lore_name):
        """A stat by the name its lore line gives it ("Crit Damage", "Bonus Attack Speed")."""
        return LORE_STATS.get(lore_name) or self.src.display.get(lore_name)

    def row(self, stats, where):
        """A rarity's stats by NEU's names as the plugin's (numbers; the wiki writes a few as text)."""
        out = {}
        for key, value in stats.items():
            try:
                value = float(value)
            except (TypeError, ValueError):
                continue
            stat = self.stat(key, where)
            if stat:
                out[stat] = value
        return out

    def wiki_stats(self, name, entry):
        """The wiki's stats by rarity (the plugin's names; its numbers only for text left out)."""
        out = {}
        subs = (entry or {}).get('substitution') or {}
        subs = set(subs.values() if isinstance(subs, dict) else subs)
        for key, stats in ((entry or {}).get('stats') or {}).items():
            rarity = WIKI_RARITY.get(key)
            if not rarity or not stats:
                continue
            # A number its bonus text names is only text (Glacial's per Cold), but for a wisdom, which items show
            # as a stat too (live Lumberjack's, Toil).
            row = self.row({WIKI_STATS[k]: v for k, v in stats.items()
                            if k in WIKI_STATS and (k not in subs or WIKI_STATS[k].endswith('_wisdom'))},
                           f'{name} {rarity} (wiki)')
            if row:
                out[rarity] = row
        return out

    def stats(self, name, modifier, neu_stats, entry, per_level):
        """Each rarity's stats (the plugin's names, in its order): NEU's, the wiki's for rarities NEU lacks, then
        corrected by what live items show."""
        rows = {}
        for rarity, stats in (neu_stats or {}).items():
            if rarity not in SAME_AS_MYTHIC:
                rows[rarity] = self.row(stats, f'{name} {rarity}')
        neu_rows = {r: dict(row) for r, row in rows.items()}
        wiki = self.wiki_stats(name, entry)
        for rarity, stats in wiki.items():
            if rarity not in rows and rarity not in SAME_AS_MYTHIC:
                rows[rarity] = dict(stats)
                if neu_stats is not None:
                    self.note('filled from the wiki', f'{name} {rarity}: {stats}')
        self.live_stats(name, modifier, rows, wiki, per_level)
        for rarity, stats in wiki.items():
            if rarity not in neu_rows:
                continue
            for stat in sorted(set(stats) | set(neu_rows[rarity])):
                neu, now = neu_rows[rarity].get(stat), (rows.get(rarity) or {}).get(stat)
                if stats.get(stat, 0) != (neu or 0) and now == neu:
                    self.note('stat: NEU and the wiki differ, no live item settles it (NEU taken)',
                              f'{name} {rarity} {stat}: NEU {neu}, wiki {stats.get(stat)}')
        out = {}
        for rarity in RARITIES:
            row = {s: rows[rarity][s] for s in self.src.stats if rarity in rows and rows[rarity].get(s)}
            if row:
                out[rarity] = {s: int(v) if float(v).is_integer() else v for s, v in row.items()}
        return out

    def live_stats(self, name, modifier, rows, wiki, per_level):
        live = self.src.live.get(modifier)
        if not live:
            return
        corrected = collections.defaultdict(dict)  # stat -> rarity -> live value
        for rarity, seen in live.items():
            if rarity in SAME_AS_MYTHIC or rarity not in RARITIES:
                continue
            values = collections.defaultdict(collections.Counter)  # a stat's lore names count together
            for lore_name, counts in (seen.get('stats') or {}).items():
                stat = self.lore_stat(lore_name)
                if stat is None:
                    self.note('live stat not in the plugin', f'{name} {rarity}: {lore_name}')
                    continue
                for v, c in counts.items():
                    values[stat][float(v)] += c
            for stat, counts in values.items():
                if stat in per_level:
                    continue  # its bracket adds the Catacombs level
                value, count = counts.most_common(1)[0]
                if count * 2 <= seen['items']:
                    # Most items of it don't show it: not what the reforge gives (an older item, say).
                    self.note('live value on too few items to count', f'{name} {rarity} {stat}: {dict(counts)} of '
                                                                      f'{seen["items"]} items')
                    continue
                have = (rows.get(rarity) or {}).get(stat)
                if have is None or abs(have - value) > 1e-9:
                    rows.setdefault(rarity, {})[stat] = value
                    corrected[stat][rarity] = value
                    self.note('live corrects NEU', f'{name} {rarity} {stat}: {have} -> {value:g} ({count} of '
                                                   f'{seen["items"]} items; the wiki says {(wiki.get(rarity) or {}).get(stat)})')
        # Where the wiki agrees with every live value NEU got wrong, its other rarities are taken too.
        for stat, fixed in corrected.items():
            if all((wiki.get(r) or {}).get(stat) == v for r, v in fixed.items()):
                for rarity, stats in wiki.items():
                    if rarity in fixed or stat not in stats or rarity in SAME_AS_MYTHIC:
                        continue
                    if (rows.get(rarity) or {}).get(stat) != stats[stat]:
                        self.note('the wiki, which live agrees with', f'{name} {rarity} {stat}: '
                                                                     f'{(rows.get(rarity) or {}).get(stat)} -> {stats[stat]:g}')
                        rows.setdefault(rarity, {})[stat] = stats[stat]
        # A stat NEU gives that items never show (and the wiki doesn't give) is the bonus's text, not a stat.
        for rarity, seen in live.items():
            if seen['items'] < SEEN or rarity not in rows:
                continue
            shown = {self.lore_stat(n) for n in (seen.get('stats') or {})}
            for stat in list(rows[rarity]):
                # Breaking Power shows under the name, never with a bracket.
                if stat in shown or stat == 'BREAKING_POWER' or not rows[rarity][stat]:
                    continue
                if any(stat in stats for stats in wiki.values()):
                    continue
                for r in rows:
                    if rows[r].pop(stat, None) is not None:
                        self.note('never shown live, so left out', f'{name} {r} {stat} ({seen["items"]} {rarity} items '
                                                                    f'show no bracket; the wiki has none)')

    # ---------- bonus text ----------

    def glyph(self, text):
        out = []
        for c in text:
            if PUA.match(c):
                symbol = self.src.glyphs.get(c)
                if symbol:
                    out.append(symbol)
                continue
            out.append(c)
        return ''.join(out).replace('§', '&')

    def wiki_bonus(self, entry, rarity):
        """The wiki's description at a rarity (its {n} as the numbers named in its substitution), as its lines."""
        if not entry or not entry.get('description'):
            return None
        text = entry['description']
        stats = (entry.get('stats') or {}).get({v: k for k, v in WIKI_RARITY.items()}[rarity]) or {}
        subs = entry.get('substitution') or {}
        subs = [subs[k] for k in sorted(subs)] if isinstance(subs, dict) else list(subs)
        for i, key in enumerate(subs, 1):
            if key not in stats:
                return None
            text = text.replace('{%d}' % i, '%g' % float(stats[key]))
        if re.search(r'\{\d+\}', text):
            return None
        lines = [line.strip() if n else line for n, line in enumerate(text.replace('\\,', ',').split('/'))]
        return [self.glyph(line) for line in lines[1:] if line] or None

    def bonus_title(self, name, seen, entry):
        """The bonus's heading when it isn't "&9<Reforge> Bonus" (Gilded's "&9Byron's Compassion &8(Gilded)"):
        as an item shows it, else as the wiki writes it; None for the usual one."""
        usual = '&9' + name + ' Bonus'
        if seen:
            title = next(iter(seen.values()))[0]
            if plain(title) != plain(usual):
                self.note('bonus heading', f'{name}: items call it "{title}"')
            return None if title == usual else title
        description = (entry or {}).get('description')
        title = self.glyph(description.split('/')[0].strip()) if description else usual
        if title != usual and not title.endswith(' Bonus'):
            return title
        if title != usual:
            self.note('bonus heading', f'{name}: the wiki writes "{title}"; "{usual}" taken')
        return None

    def neu_bonus(self, ability, rarity):
        text = ability.get(rarity) if isinstance(ability, dict) else ability
        return wrap(self.glyph(text)) if text else None

    def seen_bonus(self, modifier, stone_id):
        """The bonus sections items show, by rarity (heading, then lines): live items', and the stone's own
        Legendary one where no live Legendary item shows it."""
        seen = {}
        for rarity, observed in (self.src.live.get(modifier) or {}).items():
            if observed.get('bonus') and rarity in RARITIES and rarity not in SAME_AS_MYTHIC:
                seen[rarity] = [self.glyph(line) for line in observed['bonus'][0]['lines']]
        own = self.src.stone_bonus(stone_id) if stone_id else None
        if own and 'LEGENDARY' not in seen:
            seen['LEGENDARY'] = [self.glyph(line) for line in own]
        return seen

    def bonus(self, name, seen, rarities, ability, entry):
        """Each rarity's bonus lines: what items show at that rarity (live ones, or the stone's own), else the
        source that agrees with them (the wiki, whose text is more often right, then NEU), else, with nothing
        seen, the wiki's or NEU's."""
        live = {rarity: lines[1:] for rarity, lines in seen.items()}
        sources = [('wiki', lambda r: self.wiki_bonus(entry, r)), ('NEU', lambda r: self.neu_bonus(ability, r))]
        agrees = None
        if live:
            for source, text in sources:
                if all(text(r) is not None and plain(' '.join(text(r))) == plain(' '.join(lines)) for r, lines in live.items()):
                    agrees = source
                    break
            if agrees is None:
                said = {source: {r: text(r) and plain(' '.join(text(r))) for r in live} for source, text in sources}
                self.note('bonus text: no source says what items show',
                          f'{name}: items {dict((r, plain(" ".join(v))) for r, v in live.items())}; {said}')
        order = sorted(sources, key=lambda s: s[0] != agrees)
        out = {}
        for rarity in RARITIES:
            if rarity not in rarities:
                continue
            chosen = live.get(rarity)
            if not chosen and live and agrees is None:
                # No source says what live items do: their text where it's the same at every rarity they show,
                # else the nearest rarity below that they show.
                if len({plain(' '.join(v)) for v in live.values()}) == 1:
                    chosen = next(iter(live.values()))
                else:
                    lower = [r for r in RARITIES[:RARITIES.index(rarity)] if r in live]
                    chosen = live[lower[-1]] if lower else None
                if chosen:
                    self.note('text items show at other rarities', f'{name} {rarity}')
            for _, text in order:
                if chosen:
                    break
                chosen = text(rarity)
            if chosen:
                out[rarity] = chosen
        return out

    def per_level(self, bonus):
        """"Grants +1 ❁ Strength per Catacombs level": the stat and how much a level (Withered, Ancient)."""
        out = {}
        for lines in bonus.values():
            m = re.search(r'Grants \+(\d+(?:\.\d+)?) (.+?) per Catacombs level', plain(' '.join(lines)))
            if m:
                stat = self.src.display.get(m.group(2).strip())
                if stat:
                    out[stat] = float(m.group(1)) if '.' in m.group(1) else int(m.group(1))
        return out

    # ---------- reforges and stones ----------

    def reforge(self, name, modifier, neu, entry, rarities, stone_id=None):
        ability = (neu or {}).get('reforgeAbility')
        seen = self.seen_bonus(modifier, stone_id)
        bonus = self.bonus(name, seen, rarities, ability, entry)
        per_level = self.per_level(bonus)
        stats = self.stats(name, modifier, (neu or {}).get('reforgeStats') if neu else None, entry, per_level)
        out = {'name': name, 'stats': stats}
        if bonus:
            title = self.bonus_title(name, seen, entry)
            if title:
                out['bonus_title'] = title
            out['bonus'] = bonus
        if per_level:
            out['per_catacombs_level'] = per_level
        return out

    def costs(self, name, neu, entry):
        out = {}
        for rarity, cost in ((neu or {}).get('reforgeCosts') or {}).items():
            if rarity not in SAME_AS_MYTHIC:
                out[rarity] = cost
        for key, cost in ((entry or {}).get('costs') or {}).items():
            rarity = WIKI_RARITY[key]
            if rarity in SAME_AS_MYTHIC:
                continue
            if rarity not in out:
                out[rarity] = cost
                if neu:
                    self.note('cost filled from the wiki', f'{name} {rarity}: {cost:,}')
            elif out[rarity] != cost:
                self.note('cost: NEU and the wiki differ (NEU taken)', f'{name} {rarity}: NEU {out[rarity]:,}, wiki {cost:,}')
        return {r: out[r] for r in RARITIES if r in out}

    def target(self, name, stone_id, item_types, entry):
        """Which items a stone goes on: a pool's type (NEU's names), or item ids."""
        if isinstance(item_types, str):
            return {'type': item_types}
        ids = (item_types or {}).get('internalName')
        if ids:
            return {'items': list(ids)}
        category = WIKI_TYPES.get((entry or {}).get('tool') or (entry or {}).get('category'))
        if category:
            self.note('target from the wiki', f'{name} ({stone_id}): NEU says {item_types}, the wiki {category}')
            return {'type': category}
        raise SystemExit(f'{stone_id}: no target')

    def item_ids(self, names, neu_names):
        ids = []
        for name in names:
            found = neu_names.get(name)
            if not found:
                raise SystemExit(f'no NEU item is called {name!r}')
            ids.append(found)
        return ids

    def build(self, neu_names):
        reforges, pools, stones = {}, {p: [] for p in POOLS}, []
        for key, neu in self.src.basic.items():
            name = neu['reforgeName']
            modifier = modifier_of(name, neu.get('nbtModifier'))
            if neu['itemTypes'] not in pools:
                raise SystemExit(f'{name}: pool {neu["itemTypes"]} isn\'t one of {POOLS}')
            rarities = [r for r in RARITIES if r in neu['requiredRarities']]
            reforges[modifier] = self.reforge(name, modifier, neu, self.wiki.get(name), rarities)
            pools[neu['itemTypes']].append(modifier)
        for stone_id, neu in self.src.stones.items():
            name = neu['reforgeName']
            modifier = modifier_of(name, neu.get('nbtModifier'))
            entry = self.wiki.get(name)
            if not entry:
                self.note('not on the wiki', name)
            costs = self.costs(name, neu, entry)
            reforges[modifier] = self.reforge(name, modifier, neu, entry, list(costs), stone_id)
            stones.append({'item': stone_id, 'reforge': modifier, **self.target(name, stone_id, neu['itemTypes'], entry),
                           'costs': costs})
        # The wiki's stones NEU doesn't have (not the old accessory reforges, which no Hex offers).
        named = {r['name'] for r in reforges.values()}
        for name, entry in sorted(self.wiki.items()):
            if entry.get('source') in (None, 'Basic') or entry['category'] == 'Accessory' or name in named:
                continue
            modifier = modifier_of(name)
            stone_id = neu_names.get(entry['source'])
            if not stone_id:
                self.note('wiki stone with no item', f'{name}: {entry["source"]}')
                continue
            costs = self.costs(name, None, entry)
            reforges[modifier] = self.reforge(name, modifier, None, entry, list(costs), stone_id)
            if entry.get('target'):
                target = {'items': self.item_ids([t.strip() for t in entry['target'].replace('\\,', ',').split(',')], neu_names)}
            else:
                target = self.target(name, stone_id, None, entry)
            stones.append({'item': stone_id, 'reforge': modifier, **target, 'costs': costs})
            self.note('only on the wiki', f'{name} ({stone_id}): modifier "{modifier}" (UNKNOWN: no live item has it), '
                                          f'costs {costs}')
        stones.sort(key=lambda s: (reforges[s['reforge']]['name'], s['item']))
        return reforges, [{'type': p, 'reforges': sorted(pools[p])} for p in POOLS], stones

    # ---------- the wiki's pages ----------

    def prices(self):
        """Reforging/Prices: a random basic reforge's price at each rarity."""
        names = {r.replace('_', ' ').title(): r for r in RARITIES}
        out = {}
        for rarity, price in re.findall(r'\|\s*\{\{([A-Za-z ]+)\}\}\s*\n\s*\|\s*\{\{c\|([\d,]+)\}\}', self.src.prices_page):
            out[names[rarity.strip()]] = int(price.replace(',', ''))
        missing = [r for r in RARITIES if r not in out]
        if missing:
            raise SystemExit(f'Reforging/Prices has no price for {missing}')
        return out

    def prefixes(self, reforges):
        """Reforging's Duplicate Prefixes: the word an item whose name starts like a reforge's shows instead."""
        section = self.src.reforging_page.split('== Duplicate Prefixes ==', 1)[1].split('\n==', 1)[0]
        out = []
        for prefix, reforge, item in wikitable(section):
            if '†' in reforge:
                continue  # no longer on items
            modifier = modifier_of(reforge)
            if modifier not in reforges:
                self.note('prefix for a reforge not here', f'{prefix}: {reforge}')
                continue
            # "Wise Dragon Armor" is every piece whose name starts "Wise Dragon"; an item is its own name.
            starts = item[:-len(' Armor')] if item.endswith(' Armor') else item
            out.append({'reforge': modifier, 'name': starts, 'prefix': prefix})
        return out


def wikitable(text):
    """A wikitable's rows after its header row, each cell's text (links and templates as their words), a cell
    that spans rows repeated in each."""
    body = text[text.index('{|'):text.index('|}')]
    spans = {}  # column -> [rows left, text]
    rows = []
    for raw in body.split('|-')[1:]:
        cells = []
        for line in raw.strip().split('\n'):
            if not line or line[0] not in '!|':
                continue
            cell = line[1:].strip()
            span = 1
            m = re.match(r'rowspan="(\d+)"\s*\|(.*)', cell)
            if m:
                span, cell = int(m.group(1)), m.group(2).strip()
            cell = re.sub(r'\[\[(?:[^|\]]*\|)?([^\]]+)\]\]', r'\1', cell)
            cell = re.sub(r'\{\{[^}]*\}\}', '', cell).strip()
            cells.append((cell, span))
        row, column, k = [], 0, 0
        while k < len(cells) or column in spans:
            if column in spans:
                left, cell = spans[column]
                row.append(cell)
                if left <= 1:
                    del spans[column]
                else:
                    spans[column] = [left - 1, cell]
            else:
                cell, span = cells[k]
                k += 1
                row.append(cell)
                if span > 1:
                    spans[column] = [span - 1, cell]
            column += 1
        if row:
            rows.append(row)
    return rows


def neu_item_names(neu_dir):
    """NEU's items by their name without colours (the first one of a name)."""
    names = {}
    folder = os.path.join(neu_dir, 'items')
    for file in sorted(os.listdir(folder)):
        if not file.endswith('.json') or ';' in file:
            continue
        try:
            item = load_json(os.path.join(folder, file))
        except (OSError, ValueError):
            continue
        name = re.sub(r'§.', '', item.get('displayname', '')).strip()
        names.setdefault(name, item.get('internalname', file[:-5]))
    return names


def plugin_stats():
    """The plugin's stats in their order (the order item lore lists them), and each by the name item lore gives it
    (the first stat of a name: the Rift's come after)."""
    with open(STAT_JAVA, encoding='utf-8') as f:
        constants = re.findall(r'^\s*([A-Z_]+)\("([^"]+)"', f.read(), re.M)
    names = {}
    for const, display in constants:
        names.setdefault(display, const)
    return [const for const, _ in constants], names


def report(b, data, stones_missing):
    lines = ['# reforges.json: what was taken where the sources disagree', '',
             f'{len(data["reforges"])} reforges, {sum(len(p["reforges"]) for p in data["pools"])} basic in '
             f'{len(data["pools"])} pools, {len(data["stones"])} stones. Sources: {json.dumps(data["source"])}.', '']
    if stones_missing:
        lines += ['## Stones the plugin has no item for (the Hex can\'t show them)', ''] + [f'- {s}' for s in stones_missing] + ['']
    for kind, notes in b.notes.items():
        lines += [f'## {kind[0].upper() + kind[1:]} ({len(notes)})', ''] + [f'- {n}' for n in notes] + ['']
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser(description='Build the Hex\'s reforges.json from NEU-REPO, the wiki and live items.')
    ap.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO clone')
    ap.add_argument('--wiki-cache', default=None, help='a folder to keep fetched wiki pages in (and read them from)')
    ap.add_argument('--live', default=None, help='what live items show (reforge_live.json; see REFORGES.md); none: '
                                                 'NEU and the wiki alone')
    ap.add_argument('--items', default=None, help='the plugin\'s items.json, to report stones it lacks (default: the '
                                                  'private data checkout\'s, if there)')
    ap.add_argument('--out', default=None, help='where the files go (default: hex/ of the private data checkout)')
    args = ap.parse_args()
    data_dir = default_data()
    out = args.out or os.path.join(data_dir, 'hex')
    items_path = args.items or os.path.join(data_dir, 'items', 'items.json')
    try:
        commit = git(args.neu, 'rev-parse', 'HEAD')
    except (OSError, subprocess.CalledProcessError):
        commit = None
    stats, display = plugin_stats()
    src = Sources(args.neu, Wiki(args.wiki_cache), load_json(args.live) if args.live else None, stats, display,
                  glyph_symbols())
    b = Builder(src)
    neu_names = neu_item_names(args.neu)
    reforges, pools, stones = b.build(neu_names)
    data = {'format': 1,
            'source': {'neu_commit': commit, 'wiki': ['Module:Reforge/Data', 'Reforging/Prices', 'Reforging'],
                       'live': os.path.basename(args.live) if args.live else None},
            'about': 'The Hex\'s reforges (REFORGES.md): each by its Hypixel modifier id with its name, stats by rarity '
                     '(the plugin\'s Stat names; Special and Very Special use Mythic\'s, and a rarity with none gives '
                     'none), bonus lines by rarity and any stat it grants per Catacombs level; the basic pools; the '
                     'stones, each with its reforge, what it goes on (a pool\'s type or item ids) and its coin cost by '
                     'rarity (the rarities it goes on); a random basic reforge\'s price by rarity; and the names an item '
                     'shows in place of a reforge\'s when its own name starts the same.',
            'random_price': b.prices(),
            'reforges': dict(sorted(reforges.items())),
            'pools': pools,
            'stones': stones,
            'prefixes': b.prefixes(reforges)}
    text = json.dumps(data, ensure_ascii=False, indent=1) + '\n'
    left = sorted({'%04X' % ord(c) for c in PUA.findall(text)})
    if left:
        sys.exit(f'private-use glyphs left in the output: {left}')
    stones_missing = []
    if os.path.exists(items_path):
        items = load_json(items_path)['items']
        stones_missing = [s['item'] for s in stones if s['item'] not in items]
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, 'reforges.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(text)
    with open(os.path.join(out, 'reforges_report.md'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(report(b, data, stones_missing))
    print(f'{len(reforges)} reforges, {len(stones)} stones -> {out}')


if __name__ == '__main__':
    main()
