#!/usr/bin/env python3
# python3 tools/pets/build_pets.py --neu NEU-REPO [--gui GUI.TXT] [--wiki FILE] [--save-wiki FILE] [--out DIR]
#
# Builds the plugin's pet data, pets.json (format 1, see README.md), and report.md next to it: every pet's rarities
# with their head, stats, ability text and numbers, and the pet XP tables. The text is NotEnoughUpdates-REPO's
# in-game dumps (items/<PET>;<rarity>.json, Hypixel's own lines); the numbers are its level 1 and level 100 values
# (constants/petnums.json), each made a base and so much a level, the wiki's own formula (Module:Pet/Data) where it
# fits them. A recording of Hypixel's Pets menu (a replay decoded with tools/replay: its gui.txt) checks the result.
# All of it is Hypixel's, so the output goes to the private data repository, never into this one.
import argparse
import base64
import collections
import json
import os
import re
import subprocess
import sys
import urllib.parse
import urllib.request
from decimal import Decimal, ROUND_HALF_EVEN

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'items'))
import build_items  # noqa: E402  (the plugin's glyphs, text widths and wrapping)

WIKI = 'https://hypixelskyblock.minecraft.wiki'
WIKI_MODULE = 'Module:Pet/Data'
# The wiki's table of what Kat takes to upgrade each pet's rarity: its columns are Common -> Uncommon to Legendary -> Mythic.
WIKI_KAT = 'Kat/Costs'
KAT_FROM = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY']
KAT_UNITS = {'second': 1, 'minute': 60, 'hour': 3600, 'day': 86400}
# NEU's file suffixes: items/BEE;1.json is the Uncommon Bee.
RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'DIVINE', 'SPECIAL', 'VERY_SPECIAL']
COLORS = {'COMMON': 'f', 'UNCOMMON': 'a', 'RARE': '9', 'EPIC': '5', 'LEGENDARY': '6', 'MYTHIC': 'd', 'DIVINE': 'b',
          'SPECIAL': 'c', 'VERY_SPECIAL': 'c'}
# How wide Hypixel lets a pet's ability lines get: what re-wraps every recorded paragraph as recorded (the widest
# recorded line, "&7Gain &b+6.6% &7chance to find &5ultra rare", is 188 pixels in the plugin's widths).
WIDTH = 188
# The Pets menu's Pet Score (its recorded item): a unique pet type counts its best rarity, one more at max level.
SCORE_RARITY = {'COMMON': 1, 'UNCOMMON': 2, 'RARE': 3, 'EPIC': 4, 'LEGENDARY': 5, 'MYTHIC': 6}
# NEU's names for stats the plugin calls otherwise.
STAT_ALIAS = {'BONUS_ATTACK_SPEED': 'ATTACK_SPEED', 'WALK_SPEED': 'SPEED', 'CRITICAL_DAMAGE': 'CRIT_DAMAGE',
              'CRITICAL_CHANCE': 'CRIT_CHANCE', 'TROPHY_CHANCE': 'TROPHY_FISH_CHANCE'}
# The pet's own skill, from its type line ("&8Mining Mount"): the plugin's Skill names; None gains none.
KIND = re.compile(r'^(?:&8)?(Combat|Farming|Fishing|Mining|Foraging|Enchanting|Alchemy|Taming|Hunting) (?:Pet|Mount|Morph)$')
STAT_LINE = re.compile(r'^&7([A-Za-z\' ]+): &([0-9a-f])([+-]?)(?:\{([A-Z_]+)\}|(\d+(?:\.\d+)?))(%|s)?$')
CODES = re.compile(r'^(?:&[0-9a-fk-or])+')


# ---------- numbers ----------

def dec(x):
    return Decimal(repr(float(x))) if isinstance(x, float) else Decimal(int(x))


def places(x):
    """How many decimals x has ("0.25": 2, "7.0": 0)."""
    return max(0, -dec(x).normalize().as_tuple().exponent)


def shown(x, decimals):
    """x as Hypixel shows it with this many decimals at most: Java's DecimalFormat, which rounds the double's exact value
    half-even (26.25 is "26.2", NEU has; 0.07 x 94 is "6.6", recorded)."""
    return float(Decimal(float(x)).quantize(Decimal(1).scaleb(-decimals), rounding=ROUND_HALF_EVEN))


def near(true, v1):
    """Whether NEU's level 1 value is this one as it shows, give or take one of its last decimal: NEU's numbers are
    rounded where Hypixel shows them so (0.1 for the Guardian's 0.07) and sometimes typed in (1.05 for the Horse's
    1.04, 0.1 for the Sheep's 0.15; 15 for the Lion's 1.5 is too far)."""
    return abs(true - v1) <= 10 ** -max(places(v1), 1) + 1e-9


def clean(base, per):
    """Few decimals: two at most, or a whole base and up to six (the Bee's 0.0002 a level, the Golden Dragon's 0.00125)."""
    return (places(per) <= 2 and places(base) <= 2) or (places(base) == 0 and places(per) <= 6)


class Fit:
    """A number at a level: base + per_level x level, shown with `decimals` at most (None: the plugin's own two).
    `how` says where it came from (see fit): wiki (Module:Pet/Data's formula, which gives NEU's values), neu (made from
    NEU's level 1 and 100 values), either with -near (NEU's level 1 value a decimal off), wiki-not-neu (the wiki's, which
    gives NEU's level 100 value only), wiki-literal (a number NEU left as it is, see scaling_literals), rough (made from
    NEU's values, with many decimals) or interpolated (nothing cleaner gives them)."""

    def __init__(self, base, per, how, decimals=None):
        self.base, self.per, self.how, self.decimals = base, per, how, decimals

    def at(self, level):
        return self.base + self.per * level

    def json(self):
        out = {'base': num(self.base), 'per_level': num(self.per)}
        if self.decimals is not None:
            out['decimals'] = self.decimals
        return out


def num(x):
    x = round(float(x), 9)
    return int(x) if x == int(x) else x


def fit(v1, v100, wiki=None, at1s=(1,), at100=100):
    """A base and per-level amount (base + per x level) that give NEU's level 1 and level 100 values (at levels at1 and
    at100), and the level NEU's level 1 value is at. Best is one that gives NEU's value exactly, then one that does as
    Hypixel shows it (rounded: the Guardian's 0.07 is 0.1), then one a decimal off (see near); the wiki's first, then the
    cleanest of NEU's (the fewest decimals). Without any, the wiki's if it gives the level 100 value (NEU's level 1 one
    is wrong: the Spider's Strength, 0.5 a level, has 0.1, and the recording agrees with the wiki); else what fits."""
    def tier(base, per, at1):
        if abs(base + at100 * per - v100) > 1e-6:
            return None
        true = base + at1 * per
        if abs(true - v1) < 1e-9:
            return 0
        if abs(shown(true, places(v1)) - v1) < 1e-9:
            return 1
        return 2 if near(true, v1) else None

    found, rough = [], []
    for at1 in at1s:
        t = None if wiki is None else tier(*wiki, at1)
        if t is not None:
            found.append((t, 0, Fit(wiki[0], wiki[1], 'wiki'), at1))
        for k in range(0, 7):
            per = round((v100 - v1) / (at100 - at1), k)
            base = round(v100 - at100 * per, 6)
            t = tier(base, per, at1)
            if t is not None:
                (found if clean(base, per) else rough).append((t, 1, Fit(base, per, 'neu' if t < 2 else 'neu-near'), at1))
    if found:
        t, _order, f, at1 = min(found, key=lambda c: (c[0], c[1]))
        if t == 2 and f.how == 'wiki':
            f.how = 'wiki-near'
        return f, at1
    if wiki is not None and abs(wiki[0] + at100 * wiki[1] - v100) < 1e-6:
        return Fit(wiki[0], wiki[1], 'wiki-not-neu'), at1s[0]
    if rough:
        t, _order, f, at1 = min(rough, key=lambda c: c[0])
        f.how = 'rough'
        return f, at1
    per = (v100 - v1) / (at100 - at1s[0])
    return Fit(v100 - at100 * per, per, 'interpolated'), at1s[0]


def with_decimals(f, v1, at1=1):
    """How many decimals the number shows: as many as NEU's level 1 value has when that's the number rounded (the
    Guardian's Lucky Seven, 0.07 at level 1, is "0.1" there: one, and "6.6" at 94, recorded); else as many as it can
    have (the Bee's Hive, 1.04 at level 1, is "1.16" at 4, recorded)."""
    true = round(f.at(at1), 9)
    if places(v1) < places(true) and (abs(shown(true, places(v1)) - v1) < 1e-9 or near(true, v1)):
        f.decimals = places(v1)
    else:
        f.decimals = min(4, max(places(round(f.per, 9)), places(round(f.base, 9)), places(true)))
    return f


def text_number(f, level):
    v = shown(f.at(level), f.decimals if f.decimals is not None else 2)
    return '{:,}'.format(int(v)) if v == int(v) else '{:,}'.format(v).rstrip('0').rstrip('.')


# ---------- the wiki ----------

LUA = re.compile(r"""\s+|--\[\[.*?\]\]|--[^\n]*|(\[\[.*?\]\])|'((?:\\.|[^'\\])*)'|"((?:\\.|[^"\\])*)"|"""
                 r"""(-?\d+(?:\.\d+)?(?:[eE][-+]?\d+)?)|([A-Za-z_][A-Za-z0-9_]*)|(\[|\]|\{|\}|=|,|;)""", re.S)


def lua_tokens(src):
    pos, out = 0, []
    while pos < len(src):
        m = LUA.match(src, pos)
        if not m:
            raise ValueError(f'unreadable Lua at {pos}: {src[pos:pos + 40]!r}')
        pos = m.end()
        long_str, s1, s2, number, name, punct = m.groups()
        if long_str is not None:
            out.append(('str', long_str[2:-2]))
        elif s1 is not None or s2 is not None:
            out.append(('str', re.sub(r'\\(.)', r'\1', s1 if s1 is not None else s2)))
        elif number is not None:
            out.append(('num', float(number) if re.search(r'[.eE]', number) else int(number)))
        elif name is not None:
            out.append(('name', name))
        elif punct is not None:
            out.append(('p', punct))
    return out


def lua_value(tokens, i):
    """The value at tokens[i] (a table, string, number, true/false/nil) and where the next one starts."""
    kind, v = tokens[i]
    if (kind, v) != ('p', '{'):
        return ({'true': True, 'false': False, 'nil': None}.get(v, v) if kind == 'name' else v), i + 1
    out, n, i = {}, 1, i + 1
    while tokens[i] != ('p', '}'):
        if tokens[i] == ('p', '['):
            key, i = lua_value(tokens, i + 1)
            value, i = lua_value(tokens, i + 2)  # past ']' and '='
            out[key] = value
        elif tokens[i][0] == 'name' and tokens[i + 1] == ('p', '='):
            out[tokens[i][1]], i = lua_value(tokens, i + 2)
        else:
            out[n], i = lua_value(tokens, i)
            n += 1
        if tokens[i][0] == 'p' and tokens[i][1] in ',;':
            i += 1
    return out, i + 1


def lua_table(src):
    """Module:Pet/Data's `return { ... }`, as dicts (a Lua list's keys are 1, 2, ...)."""
    src = src.strip()
    return lua_value(lua_tokens(src[len('return'):] if src.startswith('return') else src), 0)[0]


def fetch_wiki(title):
    query = urllib.parse.urlencode({'action': 'query', 'prop': 'revisions', 'rvprop': 'content|ids', 'rvslots': 'main',
                                    'titles': title, 'format': 'json', 'redirects': 1})
    request = urllib.request.Request(WIKI + '/api.php?' + query, headers={'User-Agent': 'skyblock-pets-builder'})
    with urllib.request.urlopen(request, timeout=120) as response:
        page = next(iter(json.loads(response.read())['query']['pages'].values()))
    revision = page['revisions'][0]
    return revision['slots']['main']['*'], revision['revid']


def load_wiki(path, save):
    """Module:Pet/Data: a saved copy (from --save-wiki) or the wiki's own; by pet id, with its revision."""
    if path:
        with open(path, encoding='utf-8') as f:
            saved = json.load(f)
        text, revid = saved['text'], saved['revid']
    else:
        text, revid = fetch_wiki(WIKI_MODULE)
    if save:
        with open(save, 'w', encoding='utf-8') as f:
            json.dump({'title': WIKI_MODULE, 'revid': revid, 'text': text}, f, ensure_ascii=False)
    pets = {}
    for name, pet in lua_table(text).items():
        if isinstance(pet, dict) and pet.get('id'):
            pets[pet['id']] = dict(pet, wiki_name=name)
    return pets, revid


# ---------- Kat and George ----------

def coins(text):
    """{{c|10k}}'s "10k" as 10000 ("1.5m": 1500000, "500": 500)."""
    m = re.fullmatch(r'([\d.]+)\s*([kmb]?)', text.strip().lower())
    if not m:
        raise ValueError(f'not coins: {text!r}')
    return int(round(float(m.group(1)) * {'': 1, 'k': 1e3, 'm': 1e6, 'b': 1e9}[m.group(2)]))


def kat_costs(text):
    """The wiki's Kat/Costs table: by the pet's name, what each rarity's upgrade to the next takes ({'seconds',
    'coins', 'items': [(item name, amount)]}); a {{bc}} (no upgrade) is left out."""
    out = {}
    for row in re.split(r'\n\|-[^\n]*\n', text):
        m = re.search(r'^!\s*\[\[([^|\]]+)(?:\|([^\]]+))?\]\]', row, re.M)
        if not m:
            continue
        name = (m.group(2) or m.group(1)).strip()
        cells = [c for c in re.split(r'\n\|(?![-}])', '\n' + row[m.end():]) if c.strip()]
        upgrades = {}
        for rarity, cell in zip(KAT_FROM, cells):
            if '{{bc}}' in cell:
                continue
            seconds = sum(float(n) * KAT_UNITS[unit.lower().rstrip('s')]
                          for n, unit in re.findall(r'\{\{Green\|([\d.]+)\}\}\s*\{\{[Tt]ime\|([A-Za-z]+)\}\}', cell))
            items = [(item.strip(), int(amount or 1))
                     for item, amount in re.findall(r'\{\{Item\|([^|}]+)(?:\|amount=(\d+))?[^}]*\}\}', cell)]
            items += [(item.strip(), int(amount))
                      for amount, item in re.findall(r'\{\{Green\|(\d+)x\}\}\s*\{\{LI\|([^|}]+)', cell)]
            upgrades[rarity] = {'seconds': int(seconds), 'coins': sum(coins(c) for c in re.findall(r'\{\{c\|([^}]+)\}\}', cell)),
                                'items': items}
        out[name] = upgrades
    return out


def kat_template(text):
    """A pet's page's {{Kat Cost table}} ("|cost_common = 250k", "|mats_c = *Enchanted Mycelium, 64 *Digested Mushrooms,
    32", "|time_e = 12 Hours"), as kat_costs has a pet: for the pets its table doesn't have (the Orchid Mantis, the
    Sloth)."""
    m = re.search(r'\{\{Kat Cost table(.*?)\n\}\}', text, re.S)
    if not m:
        return {}
    params = {}
    for part in m.group(1).split('\n|')[1:]:
        key, _, value = part.partition('=')
        params[key.strip()] = value.strip()
    out = {}
    for rarity in KAT_FROM:
        key = next((k for k in (rarity.lower(), rarity[0].lower()) if any(p + k in params for p in ('cost_', 'mats_', 'time_'))), None)
        if key is None:
            continue
        cost = params.get('cost_' + key, '').replace(',', '')
        time = re.match(r'([\d.]+)\s*([A-Za-z]+)', params.get('time_' + key, ''))
        items = [(name.strip(), int(n)) for name, n in re.findall(r'([^*\n,]+),\s*(\d+)', params.get('mats_' + key, ''))]
        out[rarity] = {'seconds': int(float(time.group(1)) * KAT_UNITS[time.group(2).lower().rstrip('s')]) if time else 0,
                       'coins': coins(cost) if re.match(r'[\d.]', cost) else 0, 'items': items}
    return out


def kat_upgrade(upgrade, ids, pet, problems):
    """One of kat_costs's upgrades as pets.json has it: its items by id ([id, amount]); one with no id here, left out."""
    items = []
    for name, amount in upgrade['items']:
        if name in ids:
            items.append([ids[name], amount])
        else:
            problems.append(f'{pet}: Kat\'s {name!r} isn\'t an item NEU has: left out of the cost')
    return {'seconds': upgrade['seconds'], 'coins': upgrade['coins'], 'items': items}


def wiki_rarity(pet, rarity):
    return ((pet or {}).get('variables') or {}).get(rarity.lower())


def wiki_numbers(pet, rarity):
    """The wiki's numbers for a rarity, in the order its tooltips use them: [(variable, base, per, ability)]."""
    v = wiki_rarity(pet, rarity)
    if v is None:
        return []
    tips = (pet.get('abilities') or {}).get('tooltip') or {}
    indices = v.get('ability_indices')
    indices = list(indices.values()) if isinstance(indices, dict) else indices
    if indices is None:
        indices = range(1, int(v.get('ability_count') or len(tips)) + 1)
    out = []
    for a in indices:
        for n in re.findall(r'\{(\d+)\}', tips.get(a) or ''):
            var = v.get(int(n))
            if isinstance(var, dict) and isinstance(var.get('per_lvl'), (int, float)):
                out.append((int(n), var.get('base', 0) or 0, var['per_lvl'], a))
    return out


def wiki_stats(pet, rarity):
    """The wiki's stats for a rarity, by name ("Strength": (base, per))."""
    v = wiki_rarity(pet, rarity) or {}
    stats = v.get('stats') or (pet or {}).get('stats') or {}
    return {s['name']: (s.get('base', 0) or 0, s.get('bonus', 0) or 0) for s in stats.values() if isinstance(s, dict) and 'name' in s}


# ---------- NEU's templates ----------

def texture(nbt):
    """The head's skin: the hash in its textures.minecraft.net URL."""
    m = re.search(r'Value:\\?"([A-Za-z0-9+/=]+)\\?"', nbt)
    if not m:
        return None
    value = json.loads(base64.b64decode(m.group(1) + '=' * (-len(m.group(1)) % 4)))
    url = value.get('textures', {}).get('SKIN', {}).get('url', '')
    return url.rsplit('/', 1)[-1] or None


def blocks_of(lines):
    out, cur = [], []
    for line in lines:
        if line == '':
            out.append(cur)
            cur = []
        else:
            cur.append(line)
    out.append(cur)
    return [b for b in out if b]


def list_line(line):
    return line.startswith(' ') or CODES.sub('', line).startswith(' ')


def paragraphs(lines):
    """Hypixel's wrapped lines as the text they were wrapped from. A line goes on with the one before unless it's a
    list line (starting with a space, as " &b+1.2✎ Intelligence"), comes after one, or turns dark gray when the line
    before didn't end so ("&8Max 15 players"). A line's leading colour, when it's the one carried over, is the
    wrapping's, so it goes: wrapped again, it comes back where the line breaks."""
    out = []
    for i, line in enumerate(lines):
        prev = lines[i - 1] if i else None
        if prev is None or list_line(line) or list_line(prev) or (
                line.startswith('&8') and build_items.format_at_end(prev) != '&8'):
            out.append(line.rstrip())
            continue
        carried = build_items.format_at_end(out[-1])
        lead = CODES.match(line)
        rest = line[lead.end():] if lead and lead.group(0) == carried else line
        out[-1] = out[-1] + ' ' + rest.rstrip()
    return out


class Template:
    """One rarity's NEU dump, as the plugin shows it: the type line, its stats, then its blocks (an ability: its name
    and paragraphs; anything else: paragraphs), without the footer the plugin writes itself."""

    def __init__(self, it, text, problems):
        self.id = it['internalname']
        lore = [text(self.id, line) for line in it['lore']]
        name = text(self.id, it['displayname'])
        m = re.match(r'^&7\[Lvl \{LVL\}\] &([0-9a-f])(.+)$', name)
        if not m:
            raise ValueError(f'{self.id}: name {name!r}')
        self.color, self.name = m.group(1), m.group(2)
        self.texture = texture(it['nbttag'])
        self.kind = re.sub(r'&[0-9a-fk-or]', '', lore[0])
        k = KIND.match(lore[0])
        self.skill = k.group(1).upper() if k else ('ALL' if self.kind == 'All Skills' else None)
        self.stats, self.fixed, self.blocks, self.kat = [], {}, [], False
        footer = False
        for block in blocks_of(lore[1:]):
            if footer or re.match(r'^(?:&7)?&eRight-click to add this pet', block[0]):
                footer = True
                self.kat = self.kat or any('Can be upgraded at Kat' in line for line in block)
                continue
            lines = [STAT_LINE.match(line) for line in block]
            if all(lines) and not self.stats and not self.blocks:
                for s in lines:
                    key = s.group(4) or stat_named(s.group(1))
                    if key is None:
                        problems.append(f'{self.id}: stat line "{s.group(0)}" names no stat the plugin has')
                        continue
                    self.stats.append(key)
                    if s.group(5) is not None:
                        self.fixed[key] = float(s.group(5))
                continue
            if block[0].startswith('&6') and len(block) > 1 and not list_line(block[1]):
                self.blocks.append({'name': block[0], 'text': paragraphs(block[1:])})
            else:
                self.blocks.append({'text': paragraphs(block)})
        self.placeholders = sorted({int(n) for b in self.blocks for p in b['text'] for n in re.findall(r'\{(\d+)\}', p)})


STAT_NAMES = {}


def stat_named(name):
    return STAT_NAMES.get(name)


def plugin_stats():
    """The plugin's Stat names, and their lore names ("Crit Chance": "CRIT_CHANCE"), from Stat.java."""
    src = open(os.path.join(build_items.SRC, 'stats', 'Stat.java'), encoding='utf-8').read()
    names = {}
    for key, label in re.findall(r'^\s+([A-Z_]+)\("([^"]+)"', src, re.M):
        names.setdefault(label, key)
    return set(names.values()) | set(re.findall(r'^\s+([A-Z_]+)\("', src, re.M)), names


class Neu:
    def __init__(self, path):
        self.path = path
        with open(os.path.join(path, 'constants', 'pets.json'), encoding='utf-8') as f:
            self.pets = json.load(f)
        with open(os.path.join(path, 'constants', 'petnums.json'), encoding='utf-8') as f:
            self.nums = json.load(f)
        george = os.path.join(path, 'constants', 'george.json')
        self.george = {}  # (type, rarity) -> coins: George's prices (the wiki's George/Prices, NEU's copy)
        if os.path.isfile(george):
            with open(george, encoding='utf-8') as f:
                for key, coins in json.load(f).get('prices', {}).items():
                    pet, _, n = key.partition(';')
                    if n.isdigit() and int(n) < len(RARITIES):
                        self.george[(pet, RARITIES[int(n)])] = coins
        self.templates = collections.defaultdict(dict)  # type -> rarity -> NEU item
        self.ids = {}  # an item's name, as the wiki writes it -> its id (not a pet's, an enchanted book's or a rune's)
        items = os.path.join(path, 'items')
        for f in sorted(os.listdir(items)):
            if not f.endswith('.json'):
                continue
            m = re.match(r'^([A-Z0-9_]+);(\d)\.json$', f)
            with open(os.path.join(items, f), encoding='utf-8') as fh:
                it = json.load(fh)
            if m and int(m.group(2)) < len(RARITIES) and 'petInfo' in it.get('nbttag', ''):
                self.templates[m.group(1)][RARITIES[int(m.group(2))]] = it
            elif ';' not in f:
                name = re.sub(r'§.', '', it.get('displayname', '')).strip()
                if name:
                    self.ids.setdefault(name, f[:-len('.json')])

    def commit(self):
        try:
            return subprocess.run(['git', '-C', self.path, 'rev-parse', 'HEAD'], capture_output=True, text=True,
                                  check=True).stdout.strip()
        except (OSError, subprocess.CalledProcessError):
            return None


# ---------- building ----------

def scaling_literals(t, wiki, rarity, used, numbers, problems):
    """Numbers NEU left in a rarity's text that are the wiki's per-level ones (the Mythic Spider's Web Battlefield,
    "+0.06❁ Strength", is "+4.14" at level 69, recorded): placeholders now, with the wiki's formula. A number is one
    when its ability's text and the wiki's tooltip have their numbers and placeholders in the same order, and the
    wiki's is a placeholder there with this value at level 1."""
    tips = (wiki.get('abilities') or {}).get('tooltip') or {}
    names = (wiki.get('abilities') or {}).get('name') or {}
    for variable, base, per, ability in wiki_numbers(wiki, rarity):
        if variable in used or per == 0:
            continue
        wanted = letters(names.get(ability, ''))
        for block in t.blocks:
            if 'name' not in block or letters(block['name']) != wanted:
                continue
            theirs = [m.group(0) for m in TOKEN.finditer(tips.get(ability) or '')]
            text = '\n'.join(block['text'])
            ours = list(TOKEN.finditer(text))
            if len(ours) != len(theirs) or '{%d}' % variable not in theirs:
                continue
            m = ours[theirs.index('{%d}' % variable)]
            if m.group(0).startswith('{') or abs(float(m.group(0)) - (base + per)) > 1e-9:
                continue
            text = text[:m.start()] + '{%d}' % len(numbers) + text[m.end():]
            block['text'] = text.split('\n')
            numbers.append(with_decimals(Fit(base, per, 'wiki-literal'), base + per))
            HOWS['wiki-literal'] += 1
            used.add(variable)
            problems.append(f'{t.id}: "{m.group(0)}" in {strip(block["name"])} goes up with the level (the wiki: '
                            f'{num(base)} + {num(per)} a level), as a placeholder; NEU has it as it is at level 1')
            break


# A number or placeholder in text with its colour codes: "&a20" is a 20, "&a40s" isn't one (a duration's).
TOKEN = re.compile(r'\{\d+\}|(?:(?<=&[0-9a-fk-or])|(?<![\w.{]))\d+(?:\.\d+)?(?![\w}])')


def strip(s):
    return re.sub(r'&[0-9a-fk-or]', '', s)


def letters(s):
    return re.sub(r'[^a-z]', '', strip(str(s)).lower())


def build_pet(pet, neu, wiki, problems, stat_keys, kat=None):
    templates = neu.templates[pet]
    custom = neu.pets.get('custom_pet_leveling', {}).get(pet, {})
    out = {'rarities': {}}
    nums = neu.nums.get(pet, {})
    w = wiki.get(pet)
    for rarity in RARITIES:
        it = templates.get(rarity)
        if it is None:
            continue
        t = Template(it, TEXT, problems)
        if t.color != COLORS[rarity]:
            problems.append(f'{t.id}: named in &{t.color}, not {rarity.lower()}\'s &{COLORS[rarity]}')
        out.setdefault('name', t.name)
        out.setdefault('kind', t.kind)
        out.setdefault('skill', t.skill)
        if (t.name, t.kind) != (out['name'], out['kind']):
            problems.append(f'{t.id}: "{t.name}", "{t.kind}" (its other rarities: "{out["name"]}", "{out["kind"]}")')
        r = {'texture': t.texture}
        if t.name != out['name']:
            r['name'] = t.name
        n = nums.get(rarity)
        curve = (n or {}).get('stats_levelling_curve')
        stats = {}
        if t.stats:
            ws = wiki_stats(w, rarity)
            for key in t.stats:
                plugin = STAT_ALIAS.get(key, key)
                if plugin not in stat_keys:
                    problems.append(f'{t.id}: stat {key}: not one of the plugin\'s')
                    continue
                if key in t.fixed:
                    stats[plugin] = Fit(t.fixed[key], 0, 'neu').json()
                    continue
                if n is None:
                    problems.append(f'{t.id}: no numbers for its stats (NEU has none for this rarity)')
                    break
                label = next((label for label, k in STAT_NAMES.items() if k == plugin), None)
                cand = ws.get(label)
                f, _at1 = fit_level(n['1']['statNums'][key], n['100']['statNums'][key], cand, curve)
                note(problems, t.id, 'stat ' + key, f, n['1']['statNums'][key], n['100']['statNums'][key], cand)
                stats[plugin] = Fit(f.base, f.per, f.how).json()
        r['stats'] = stats
        numbers = []
        if t.placeholders:
            if n is None:
                problems.append(f'{t.id}: no numbers for its text (NEU has none for this rarity): left out')
                continue
            if t.placeholders != list(range(len(n['1']['otherNums']))):
                problems.append(f'{t.id}: its text has {t.placeholders}, NEU {len(n["1"]["otherNums"])} numbers')
            order = wiki_numbers(w, rarity)
            for i, (v1, v100) in enumerate(zip(n['1']['otherNums'], n['100']['otherNums'])):
                cand = (order[i][1], order[i][2]) if i < len(order) else None
                f, at1 = fit_level(v1, v100, cand, curve)
                f = with_decimals(f, v1, at1)
                note(problems, t.id, '{%d}' % i, f, v1, v100, cand)
                numbers.append(f)
        if w is not None:
            used = {order_n[0] for order_n in wiki_numbers(w, rarity)[:len(numbers)]}
            scaling_literals(t, w, rarity, used, numbers, problems)
        r['numbers'] = [f.json() for f in numbers]
        r['blocks'] = t.blocks
        if t.kat:
            r['kat'] = True
        upgrade = (kat or {}).get(rarity)
        if upgrade is not None:
            r['kat_upgrade'] = kat_upgrade(upgrade, neu.ids, pet, problems)
        if (pet, rarity) in neu.george:
            r['george'] = neu.george[(pet, rarity)]
        if curve:
            out['stats_from'] = 100
        out['rarities'][rarity] = r
    if custom.get('pet_levels'):
        out['extra_levels'] = custom['pet_levels']
    if custom.get('max_level'):
        out['max_level'] = custom['max_level']
    if custom.get('rarity_offset'):
        out['rarity_offset'] = custom['rarity_offset']
    if custom.get('xp_multiplier'):
        out['xp_multiplier'] = custom['xp_multiplier']
    return out


def fit_level(v1, v100, cand, curve):
    """fit, for the pet's levels. The dragons (stats_levelling_curve "101:200:1") have NEU's numbers at levels 100 to 200
    (the wiki: the egg hatches at 100), read by NEU's own code as 101 and 200: that fits most of their numbers, 100 and
    200 their stats; the wiki's are left out (its levels are others)."""
    return fit(v1, v100, None, (101, 100), 200) if curve else fit(v1, v100, cand)


HOWS = collections.Counter()


def note(problems, pet, what, f, v1, v100, cand):
    HOWS[f.how] += 1
    if f.how in ('wiki', 'neu', 'wiki-literal', 'wiki-near', 'neu-near'):
        return
    problems.append(f'{pet} {what}: NEU {num(v1)} at level 1, {num(v100)} at 100; the wiki '
                    f'{"has " + str(num(cand[0])) + " + " + str(num(cand[1])) + " a level" if cand else "has none"}: '
                    f'{f.how}, {num(f.base)} + {num(f.per)} a level')


TEXT = None


def build(neu, wiki, problems, kat=None, kat_pages=None):
    global TEXT
    HOWS.clear()
    glyphs = {int(cp, 16): symbol for cp, symbol, _how in build_items.tsv('glyphs.tsv') if symbol}
    TEXT = build_items.Text(glyphs)
    stat_keys, names = plugin_stats()
    STAT_NAMES.clear()
    STAT_NAMES.update(names)
    pets = {}
    kat = kat or {}
    names = {}
    for pet in sorted(neu.templates):
        try:
            name = re.sub(r'§.|\[Lvl [^]]*\]', '', next(iter(neu.templates[pet].values())).get('displayname', '')).strip()
            names[name] = pet
            pets[pet] = build_pet(pet, neu, wiki, problems, stat_keys, kat.get(name))
        except (KeyError, ValueError, TypeError, IndexError) as e:
            problems.append(f'{pet}: left out ({type(e).__name__}: {e})')
    for name in sorted(set(kat) - set(names)):
        problems.append(f'Kat/Costs: {name!r} isn\'t a pet NEU has: left out')
    for name, pet in sorted(names.items()):
        missing = [r for r, v in pets.get(pet, {}).get('rarities', {}).items() if v.get('kat') and 'kat_upgrade' not in v]
        if not missing or kat_pages is None:
            continue
        upgrades = kat_template(kat_pages(name + ' Pet'))
        for rarity in missing:
            if rarity in upgrades:
                pets[pet]['rarities'][rarity]['kat_upgrade'] = kat_upgrade(upgrades[rarity], neu.ids, pet, problems)
            else:
                problems.append(f'{pet} {rarity}: "Can be upgraded at Kat", but neither Kat/Costs nor its page has the cost')
    for cp, ids in sorted(TEXT.dropped.items()):
        problems.append(f'glyph {cp:04X} has no symbol (glyphs.tsv): dropped in {", ".join(sorted(ids))}')
    return {
        'format': 1,
        'levels': neu.pets['pet_levels'],
        'rarity_offset': neu.pets['pet_rarity_offset'],
        'max_level': 100,
        'score': {'rarity': SCORE_RARITY, 'max_level': 1},
        'pets': pets,
    }


# ---------- the recording ----------

ITEM = re.compile(r'^  (?:\S+ slot \d+ := |\[\d+\] )(\S+) x\d+ (.*)$')


def recorded(gui):
    """The pets the recording shows (each look of each once): (name line, lore)."""
    seen = {}
    with open(gui, encoding='utf-8') as f:
        lines = f.read().split('\n')
    i = 0
    while i < len(lines):
        m = ITEM.match(lines[i])
        if not m:
            i += 1
            continue
        lore, j = [], i + 1
        while j < len(lines) and lines[j].startswith('      '):
            lore.append(lines[j][6:])
            j += 1
        if m.group(2).startswith('&7[Lvl ') and m.group(1) == 'player_head':
            # Hypixel's glyphs as the plugin's symbols, as the data has them.
            seen.setdefault((TEXT('recorded', m.group(2)), tuple(TEXT('recorded', line) for line in lore)), None)
        i = j
    return list(seen)


SCORE_LINE = re.compile(r'^&6(\d+) Score&8: \+&b(\d+) Magic Find(?: &5«)?$')


def score_rewards(gui):
    """The Pet Score Rewards item's thresholds, as recorded ("&650 Score&8: +&b3 Magic Find"): [[score, magic find]]."""
    with open(gui, encoding='utf-8') as f:
        lines = f.read().split('\n')
    rewards = {}
    for i, line in enumerate(lines):
        m = ITEM.match(line)
        if not m or not m.group(2).startswith('&dPet Score Rewards'):
            continue
        for lore in lines[i + 1:]:
            if not lore.startswith('      '):
                break
            s = SCORE_LINE.match(lore[6:])
            if s:
                rewards[int(s.group(1))] = int(s.group(2))
    return [[score, rewards[score]] for score in sorted(rewards)]


def render(pet, rarity, level, data):
    """A pet's stat lines and blocks at a level, as the plugin shows them (without a held item's)."""
    r = data['pets'][pet]['rarities'][rarity]
    numbers = [Fit(n['base'], n['per_level'], '', n.get('decimals')) for n in r['numbers']]
    values = [text_number(f, level) for f in numbers]
    lines = []
    for key, s in r['stats'].items():
        f = Fit(s['base'], s['per_level'], '')
        lines.append((key, f.at(level)))
    out = []
    for block in r['blocks']:
        out.append('')
        if 'name' in block:
            out.append(block['name'])
        for p in block['text']:
            out += wrapped(re.sub(r'\{(\d+)\}', lambda m: values[int(m.group(1))], p))
    return lines, out


def wrapped(paragraph):
    """A paragraph's lines at WIDTH, a list line's leading spaces kept (wrapping would drop them)."""
    text = paragraph.lstrip(' ')
    lines = build_items.wrap(text, WIDTH)
    lines[0] = paragraph[:len(paragraph) - len(text)] + lines[0]
    return lines


# Where a recorded pet's own lines end: its held item, candy, progress, or a shop's preview (Bea's).
END = r'^&6Held Item: |^&a\(\d+/10\) Pet Candy|^&7Progress to Level|^&b&lMAX LEVEL|^&cThis is a preview of Lvl'


def check(data, gui, problems):
    """Every recorded pet against what the data shows at its level: its stats and its blocks, line by line."""
    by_name = {}
    for pet, p in data['pets'].items():
        for rarity, r in p['rarities'].items():
            by_name[(COLORS[rarity], r.get('name', p['name']))] = (pet, rarity)
    _keys, names = plugin_stats()
    results = []
    for name, lore in recorded(gui):
        m = re.match(r'^&7\[Lvl (\d+)\] &([0-9a-f])(.+?)(?:&5 ✦)?$', name)
        key = (m.group(2), m.group(3)) if m else None
        if key not in by_name:
            problems.append(f'recorded "{name}": no pet of that name and rarity')
            continue
        pet, rarity = by_name[key]
        level = int(m.group(1))
        stats, blocks = render(pet, rarity, level, data)
        lore = list(lore)
        # The recorded stat lines (the first block after the type line) and the blocks up to the held item or the
        # candy or the progress.
        end = next((i for i, line in enumerate(lore) if re.match(END, line)), len(lore))
        stat_end = lore.index('', 2) if '' in lore[2:] else 2
        recorded_stats = {}
        for line in lore[2:stat_end]:
            s = STAT_LINE.match(line)
            if s and s.group(1) in names:
                recorded_stats[names[s.group(1)]] = float(s.group(5) or 'nan')
        bad = []
        for key_, value in stats:
            got = recorded_stats.get(key_)
            if got is None or abs(round(value, 2) - got) > 1e-9:
                # A held item's stat is in the line too (the Dwarf Turtle Shelmet's +10 True Defense).
                if not (got is not None and key_ == 'TRUE_DEFENSE' and any('Shelmet' in line for line in lore)):
                    bad.append(f'{key_} {num(round(value, 2))}, recorded {got}')
        want = lore[stat_end:end]
        while want and want[-1] == '':
            want.pop()
        if blocks != want:
            for a, b in zip(blocks + [None] * len(want), want + [None] * len(blocks)):
                if a != b:
                    bad.append(f'"{a}" where "{b}" was recorded')
                    break
        results.append((name, pet, rarity, level, bad))
        for b in bad:
            problems.append(f'recorded "{name}" ({pet} {rarity}, level {level}): {b}')
    return results


# ---------- output ----------

def report(data, problems, checked, source):
    pets = data['pets']
    rarities = sum(len(p['rarities']) for p in pets.values())
    lines = ['# Pets data', '', f'Made by tools/pets/build_pets.py from NEU-REPO `{source.get("neu_commit")}`, the wiki\'s '
             f'Module:Pet/Data (revision {source.get("wiki_revision")}) and the recording `{source.get("recording")}`.', '',
             f'- {len(pets)} pets, {rarities} rarities',
             '- their numbers and stats: ' + ', '.join(f'{n} {how}' for how, n in HOWS.most_common()) + ' (see Fit in the script)',
             '']
    if checked:
        good = sum(1 for c in checked if not c[4])
        lines += [f'## The recording: {good} of {len(checked)} pets as recorded', '']
        for name, pet, rarity, level, bad in checked:
            lines.append(f'- {"ok" if not bad else "**differs**"}: {pet} {rarity} at level {level}' + ('' if not bad else f' ({"; ".join(bad)})'))
        lines.append('')
    lines += ['## Problems', '']
    lines += [f'- {p}' for p in problems] or ['None.']
    return '\n'.join(lines) + '\n'


def default_out():
    return os.path.join(os.path.dirname(build_items.default_out()), 'pets')


def main():
    ap = argparse.ArgumentParser(description='Build pets.json (format 1) from NEU-REPO and the wiki\'s Module:Pet/Data, '
                                             'checked against a recording of the Pets menu')
    ap.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO clone')
    ap.add_argument('--gui', required=True, help="a decoded recording's gui.txt with the Pets menu (tools/replay decode)")
    ap.add_argument('--wiki', help='a saved Module:Pet/Data (from --save-wiki) instead of fetching it')
    ap.add_argument('--save-wiki', help='where to save the Module:Pet/Data fetched')
    ap.add_argument('--kat', help="a saved Kat/Costs page (the wiki's raw text) instead of fetching it")
    ap.add_argument('--out', default=None, help='where the files go (default: pets/ of the private data checkout)')
    args = ap.parse_args()
    problems = []
    neu = Neu(args.neu)
    wiki, revid = load_wiki(args.wiki, args.save_wiki)
    if args.kat:
        with open(args.kat, encoding='utf-8') as f:
            kat_text, kat_revid = f.read(), None
    else:
        kat_text, kat_revid = fetch_wiki(WIKI_KAT)
    data = build(neu, wiki, problems, kat_costs(kat_text), lambda title: fetch_wiki(title)[0])
    source = {'neu_commit': neu.commit(), 'wiki_revision': revid, 'kat_revision': kat_revid}
    source['recording'] = os.path.basename(os.path.dirname(os.path.abspath(args.gui)))
    checked = check(data, args.gui, problems)
    data['score']['rewards'] = score_rewards(args.gui)
    if not data['score']['rewards']:
        problems.append('the recording has no Pet Score Rewards item: no rewards')
    data = dict(format=data['format'], source=source, **{k: v for k, v in data.items() if k != 'format'})
    out = args.out or default_out()
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, 'pets.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write('\n')
    with open(os.path.join(out, 'report.md'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(report(data, problems, checked, source))
    good = sum(1 for c in checked if not c[4])
    print(f'pets.json: {len(data["pets"])} pets, {len(data["score"]["rewards"])} Pet Score rewards; the recording\'s '
          f'{good} of {len(checked)} pets as recorded; {len(problems)} problems (report.md)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
