#!/usr/bin/env python3
# python3 tools/collections/build_collections.py --neu NEU-REPO [--api PATH_OR_URL] [--items ITEMS.JSON]
#                                                [--wiki-cache DIR] [--recorded JSON] [--out DIR]
#
# Builds the plugin's collection data (format 1, see README.md): collections.json (every collection with its
# tiers and what each tier unlocks, the boss collections, and which items count toward which collection) and
# recipes.json (the crafting recipes, and the Recipe Book's entries), with report.md next to them. From
# Hypixel's collections API, NotEnoughUpdates-REPO (the recipes, and which item an unlock names), the plugin's
# items.json (the items there are) and the wiki (the boss collections, the Recipe Book's categories). All of it
# is Hypixel's, so the output goes to the private data repository, never into this one.
import argparse
import collections
import json
import os
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
API_URL = 'https://api.hypixel.net/v2/resources/skyblock/collections'
WIKI = 'https://hypixelskyblock.minecraft.wiki/index.php?title={}&action=raw'
PUA = re.compile('[\ue000-\uf8ff]')
COLOR = re.compile('[&§][0-9a-fk-or]', re.I)

# The Collections menu's categories, in its order (the recording's slots 20-24). The API's Rift collections
# are only shown in the Rift, which this plugin doesn't have.
CATEGORIES = [('FARMING', 'Farming'), ('MINING', 'Mining'), ('COMBAT', 'Combat'), ('FORAGING', 'Foraging'),
              ('FISHING', 'Fishing')]
# Collections whose API id is no item: the item the menu shows (the wiki's), and which items are their base.
GROUPS = {'MUSHROOM_COLLECTION': ('RED_MUSHROOM', lambda i: i in ('RED_MUSHROOM', 'BROWN_MUSHROOM')),
          'GEMSTONE_COLLECTION': ('ROUGH_RUBY_GEM', lambda i: re.fullmatch(r'ROUGH_[A-Z]+_GEM', i) is not None)}
# The boss collections: the wiki's name, the id /viewbosscollection takes (the wiki's Commands), and the
# dungeon floor whose completion counts (none for Kuudra).
BOSSES = [('Bonzo', 'CATACOMBS_1', 1), ('Scarf', 'CATACOMBS_2', 2), ('The Professor', 'CATACOMBS_3', 3),
          ('Thorn', 'CATACOMBS_4', 4), ('Livid', 'CATACOMBS_5', 5), ('Sadan', 'CATACOMBS_6', 6),
          ('Necron', 'CATACOMBS_7', 7), ('Kuudra', 'KUUDRA', 0)]
# NEU's head for each boss (its "<NAME>_BOSS" item).
BOSS_HEADS = {'CATACOMBS_1': 'BONZO_BOSS', 'CATACOMBS_2': 'SCARF_BOSS', 'CATACOMBS_3': 'PROFESSOR_BOSS',
              'CATACOMBS_4': 'THORN_BOSS', 'CATACOMBS_5': 'LIVID_BOSS', 'CATACOMBS_6': 'SADAN_BOSS',
              'CATACOMBS_7': 'NECRON_BOSS'}
# The Recipe Book's categories (the recording's order) and the wiki pages listing each one's recipes
# (Recipe Book/UI/<page>; Slayer is five pages, one a boss).
BOOK = [('FARMING', ['Farming']), ('MINING', ['Mining']), ('COMBAT', ['Combat']), ('FISHING', ['Fishing']),
        ('FORAGING', ['Foraging']), ('ENCHANTING', ['Enchanting']), ('ALCHEMY', ['Alchemy']),
        ('CARPENTRY', ['Carpentry']), ('HUNTING', []), ('SLAYER', ['Zombie', 'Spider', 'Wolf', 'Enderman', 'Blaze']),
        ('SPECIAL', ['Special'])]
# The slayers whose wiki page ("<name> Slayer") lists, level by level, the items whose recipes it unlocks: a
# fallback for the recipes neither a collection tier nor NEU's crafttext says anything about.
SLAYERS = ['Zombie', 'Spider', 'Wolf', 'Enderman', 'Blaze', 'Vampire']
SKILLS = ['Farming', 'Mining', 'Combat', 'Foraging', 'Fishing', 'Enchanting', 'Alchemy', 'Carpentry', 'Hunting',
          'Taming', 'Runecrafting', 'Social']
ROMAN = {'I': 1, 'V': 5, 'X': 10, 'L': 50, 'C': 100}


def roman(s):
    total, last = 0, 0
    for c in reversed(s):
        v = ROMAN[c]
        total += -v if v < last else v
        last = max(last, v)
    return total


def plain(s):
    """Without colour codes or private-use glyphs, spaces trimmed."""
    return PUA.sub('', COLOR.sub('', s or '')).strip()


def load_json(source, what):
    if re.match(r'https?://', source):
        request = urllib.request.Request(source, headers={'User-Agent': 'skyblock-collections-builder'})
        with urllib.request.urlopen(request, timeout=120) as response:
            return json.loads(response.read())
    with open(source, encoding='utf-8') as f:
        return json.load(f)


def git(path, *args):
    return subprocess.run(['git', '-C', path, *args], capture_output=True, text=True, check=True).stdout.strip()


def default_data():
    # The private data checkout sits next to this repository's main checkout (from a worktree too).
    try:
        common = git(ROOT, 'rev-parse', '--path-format=absolute', '--git-common-dir')
        main = os.path.dirname(common) if os.path.basename(common) == '.git' else ROOT
    except (OSError, subprocess.CalledProcessError):
        main = ROOT
    return os.path.join(os.path.dirname(main), 'skyblock-dungeon-data')


def glyph_symbols():
    """tools/items' table of Hypixel's private-use glyphs and the classic symbols the plugin shows."""
    symbols = {}
    with open(os.path.join(ROOT, 'tools', 'items', 'data', 'glyphs.tsv'), encoding='utf-8') as f:
        for line in f:
            if line.startswith('#') or not line.strip():
                continue
            cp, symbol = line.rstrip('\n').split('\t')[:2]
            if symbol:
                symbols[chr(int(cp, 16))] = symbol
    return symbols


# ---------- the wiki ----------

class Wiki:
    """Raw wikitext of hypixelskyblock.minecraft.wiki pages, kept in a cache folder if one is given."""

    def __init__(self, cache):
        self.cache = cache
        self.fetched = []

    def page(self, title):
        path = os.path.join(self.cache, re.sub(r'[^A-Za-z0-9_.-]', '_', title) + '.txt') if self.cache else None
        if path and os.path.exists(path):
            with open(path, encoding='utf-8') as f:
                return f.read()
        url = WIKI.format(urllib.parse.quote(title.replace(' ', '_'), safe='/:'))
        request = urllib.request.Request(url, headers={'User-Agent': 'skyblock-collections-builder'})
        with urllib.request.urlopen(request, timeout=120) as response:
            text = response.read().decode('utf-8')
        self.fetched.append(title)
        if path:
            os.makedirs(self.cache, exist_ok=True)
            with open(path, 'w', encoding='utf-8') as f:
                f.write(text)
        return text


class Lua:
    """Reads a Lua table literal (the wiki's data modules): tables, strings, numbers, booleans, nil."""

    TOKEN = re.compile(r'\s+|--\[\[.*?\]\]|--[^\n]*|(\[\[.*?\]\])|("(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\')'
                       r'|(-?\d+(?:\.\d+)?)|([A-Za-z_]\w*)|([{}\[\]=,;])', re.S)

    def __init__(self, text):
        # Read as far as the table goes: what comes after it is code.
        self.text = text
        self.pos = 0
        self.tokens = []
        self.i = 0

    def read(self):
        while self.pos < len(self.text):
            m = self.TOKEN.match(self.text, self.pos)
            if not m:
                raise ValueError(f'unreadable Lua at {self.pos}: {self.text[self.pos:self.pos + 40]!r}')
            self.pos = m.end()
            long_string, string, number, name, punct = m.groups()
            if long_string:
                return 'str', long_string[2:-2]
            if string:
                return 'str', re.sub(r'\\(.)', lambda e: {'n': '\n', 't': '\t'}.get(e.group(1), e.group(1)), string[1:-1])
            if number:
                return 'num', float(number) if '.' in number else int(number)
            if name:
                return 'name', name
            if punct:
                return 'p', punct
        return None, None

    def peek(self, k=0):
        while len(self.tokens) <= self.i + k:
            self.tokens.append(self.read())
        return self.tokens[self.i + k]

    def take(self, expected=None):
        tok = self.peek()
        if expected is not None and tok != ('p', expected):
            raise ValueError(f'expected {expected!r}, got {tok!r}')
        self.i += 1
        return tok

    def value(self):
        kind, v = self.take()
        if kind in ('str', 'num'):
            return v
        if kind == 'name':
            return {'true': True, 'false': False, 'nil': None}.get(v, v)
        if (kind, v) == ('p', '{'):
            return self.table()
        raise ValueError(f'unexpected {v!r}')

    def table(self):
        array, fields = [], {}
        while self.peek() != ('p', '}'):
            if self.peek() == ('p', '['):
                self.take('[')
                key = self.value()
                self.take(']')
                self.take('=')
                fields[key] = self.value()
            elif self.peek()[0] == 'name' and self.peek(1) == ('p', '='):
                key = self.take()[1]
                self.take('=')
                fields[key] = self.value()
            else:
                array.append(self.value())
            if self.peek() in (('p', ','), ('p', ';')):
                self.take()
        self.take('}')
        for n, v in enumerate(array, 1):
            fields[n] = v
        return fields

    @staticmethod
    def first_table(text):
        """The first table literal in a module (its `local data = { ... }`)."""
        start = text.index('{')
        lua = Lua(text[start:])
        lua.take('{')
        return lua.table()


def book_pages(wiki):
    """Each Recipe Book category's recipe names, in the wiki's order (its pages list the book fully unlocked).
    The wiki's Enchanting pages list books the crafting table no longer makes (59 of them; the recording has 9),
    so that category is only what collections unlock."""
    lists = {}
    for category, pages in BOOK:
        names = []
        for page in pages if category != 'ENCHANTING' else []:
            names += book_page(wiki, page)
        lists[category] = names
    return lists


def book_page(wiki, page):
    """The recipe names on one of the wiki's Recipe Book/UI pages ("Zombie": the Revenant Horror's)."""
    names = []
    text = wiki.page('Recipe Book/UI/' + page)
    for m in re.finditer(r'^\|(\d+), ?(\d+)\s*=\s*([^,\n]+),\s*([^,\n]*),\s*([^,\n]*)', text, re.M):
        row, col, image, _link, title = m.groups()
        name = plain((image if '%inherit%' in title else title).replace('\\\'', "'"))
        if (row, col.strip()) == ('1', '5') or name in ('Next Page', 'Previous Page', 'Go Back', 'Close'):
            continue
        names.append(name)
    return names


# ---------- NEU ----------

def neu_id(raw):
    """NEU's file and recipe ids write ':' as '-' ("INK_SACK-3"); pets are "ZOMBIE;0"."""
    return re.sub(r'-(\d+)$', r':\1', raw)


class Neu:
    def __init__(self, path):
        self.path = path
        self.items = {}
        folder = os.path.join(path, 'items')
        for name in sorted(os.listdir(folder)):
            if not name.endswith('.json'):
                continue
            with open(os.path.join(folder, name), encoding='utf-8') as f:
                try:
                    self.items[neu_id(name[:-5])] = json.load(f)
                except ValueError:
                    pass
        self.by_name = collections.defaultdict(list)
        for item_id, item in self.items.items():
            self.by_name[plain(item.get('displayname'))].append(item_id)
        with open(os.path.join(path, 'constants', 'pets.json'), encoding='utf-8') as f:
            # A pet's skill ("CROW": "COMBAT"): the Recipe Book lists its recipe under that.
            self.pet_types = json.load(f).get('pet_types', {})

    def crafttext(self, item_id):
        return (self.items.get(item_id) or {}).get('crafttext', '')

    def recipes(self, item_id):
        """Each crafting recipe: (nine cells, row by row, each (id, count) or None; how many it makes)."""
        item = self.items.get(item_id) or {}
        found = []
        raw = []
        if isinstance(item.get('recipe'), dict):
            raw.append(item['recipe'])
        raw += [r for r in item.get('recipes', []) if r.get('type') == 'crafting'
                and r.get('overrideOutputId', item_id) in (item_id, item_id.replace(':', '-'))]
        for r in raw:
            cells = []
            for row in 'ABC':
                for col in '123':
                    cell = r.get(row + col, '')
                    if not cell:
                        cells.append(None)
                        continue
                    ingredient, _, count = cell.rpartition(':')
                    if not count.isdigit() or not ingredient:
                        ingredient, count = cell, '1'
                    cells.append((neu_id(ingredient), int(count)))
            count = int(r.get('count', 1) or 1)
            if any(cells) and (cells, count) not in found:
                found.append((cells, count))
        return found

    def texture(self, item_id):
        """The skin hash of a NEU item's head, from its nbttag."""
        tag = (self.items.get(item_id) or {}).get('nbttag', '')
        m = re.search(r'Value:\\?"([A-Za-z0-9+/=]+)', tag)
        if not m:
            return None
        import base64
        value = m.group(1)
        decoded = base64.b64decode(value + '=' * (-len(value) % 4)).decode('utf-8', 'replace')
        url = re.search(r'textures\.minecraft\.net/texture/([0-9a-f]+)', decoded)
        return url.group(1) if url else None


# ---------- building ----------

class Builder:
    def __init__(self, api, items, neu, wiki, glyphs, recorded):
        self.api = api
        self.items = items
        self.neu = neu
        self.wiki = wiki
        self.glyphs = glyphs
        self.recorded = recorded
        self.by_name = collections.defaultdict(list)
        for item_id, item in items.items():
            self.by_name[plain(item['name'])].append(item_id)
        self.problems = collections.defaultdict(list)
        self.counts = collections.Counter()
        # Collection name -> API id, and API id -> (category, entry).
        self.collections = {}
        self.names = {}
        for category, _ in CATEGORIES:
            for cid, c in api['collections'][category]['items'].items():
                self.collections[cid] = (category, c)
                self.names[c['name']] = cid

    def note(self, kind, detail):
        self.problems[kind].append(detail)

    def glyph(self, s):
        return PUA.sub(lambda m: self.glyphs.get(m.group(0), ''), s)

    # names to items

    def resolve(self, name, collection=None, tier=None):
        """The item an unlock names ("Enchanted Blaze Powder"); None if there's no such item."""
        name = plain(name)
        ids = self.by_name.get(name, [])
        if len(ids) == 1:
            return ids[0]
        if not ids:
            return None
        wanted = f'{collection} {tier}' if collection else None
        for pick in (lambda i: wanted and self.neu.crafttext(i).replace('Requires: ', '').replace('Requires ', '') == wanted,
                     lambda i: bool(self.neu.recipes(i)),
                     lambda i: i == re.sub(r'[^A-Z0-9]+', '_', name.upper()).strip('_')):
            chosen = [i for i in ids if pick(i)]
            if chosen:
                if len(chosen) > 1:
                    self.note('ambiguous names', f'{name}: {chosen}, took {chosen[0]}')
                return chosen[0]
        self.note('ambiguous names', f'{name}: {ids}, took {ids[0]}')
        return ids[0]

    def minion(self, name):
        """"Blaze" -> BLAZE_GENERATOR_1, the tier I minion ("Blaze Minion I" in NEU's names)."""
        for item_id in self.neu.by_name.get(name + ' Minion I', []):
            if item_id.endswith('_GENERATOR_1') and item_id in self.items:
                return item_id
        return None

    # a tier's unlocks

    def reward(self, text, collection, tier):
        """One of the API's unlock lines as a reward (what the plugin shows and does)."""
        text = text.strip()
        m = re.fullmatch(r'\+([\d,]+) SkyBlock XP', text)
        if m:
            return {'type': 'SKYBLOCK_XP', 'amount': int(m.group(1).replace(',', ''))}
        m = re.fullmatch(r'\+([\d,]+) (\w+) Experience', text)
        if m and m.group(2) in SKILLS:
            return {'type': 'SKILL_XP', 'skill': m.group(2).upper(), 'amount': int(m.group(1).replace(',', ''))}
        m = re.fullmatch(r'(.+) Exp Discount \(-(\d+)%\)', text)
        if m:
            return {'type': 'EXP_DISCOUNT', 'name': m.group(1), 'percent': int(m.group(2))}
        m = re.fullmatch(r'\+([\d,]+) (.+?) (Slots?)', text)
        if m:
            return {'type': 'SLOTS', 'name': m.group(2), 'amount': int(m.group(1).replace(',', ''))}
        m = re.fullmatch(r'(.+) Minion Recipes', text)
        if m:
            r = {'type': 'MINION_RECIPES', 'name': m.group(1)}
            item = self.minion(m.group(1))
            if item:
                r['item'] = item
            else:
                self.note('minions not in items.json', m.group(1))
            return r
        m = re.fullmatch(r'\[Lvl 1\] (.+) Recipe', text)
        if m:
            r = {'type': 'PET_RECIPE', 'name': m.group(1)}
            pet = re.sub(r'[^A-Z0-9]+', '_', m.group(1).upper()) + ';0'
            texture = self.neu.texture(pet)
            if texture:
                r['texture'] = texture
            return r
        m = re.fullmatch(r'(.+) Dwarven Forge Recipe', text)
        if m:
            return {'type': 'FORGE_RECIPE', 'name': plain(m.group(1))}
        m = re.fullmatch(r'(.+) (Recipe|Trade)', text)
        if m:
            kind = 'RECIPE' if m.group(2) == 'Recipe' else 'TRADE'
            name = plain(m.group(1))
            r = {'type': kind, 'name': name}
            item = self.resolve(name, collection, tier)
            if item:
                r['item'] = item
            else:
                self.note(f'{kind.lower()}s naming no item in items.json', f'{name} ({collection} {tier})')
            return r
        m = re.fullmatch(r'\+([\d,]+)[\ue000-\uf8ff]? ((?:Mining|Farming|Foraging|Fishing) Fortune)', text)
        if m:
            # A stat for good: the plugin's Stat names are Hypixel's, upper-cased.
            return {'type': 'STAT', 'name': m.group(2).upper().replace(' ', '_'), 'amount': int(m.group(1).replace(',', '')),
                    'text': self.glyph(text)}
        return {'type': 'UNLOCK', 'text': self.glyph(text)}

    def collection(self, cid, c, category):
        icon, _ = GROUPS.get(cid, (cid, None))
        if icon not in self.items:
            self.note('collections with no item to show', cid)
        tiers = []
        for t in sorted(c['tiers'], key=lambda t: t['tier']):
            tier_roman = to_roman(t['tier'])
            tiers.append({'amount': t['amountRequired'],
                          'rewards': [self.reward(u, c['name'], tier_roman) for u in t['unlocks']]})
            self.counts['tiers'] += 1
        if len(tiers) != c.get('maxTiers', len(tiers)):
            self.note('tier counts', f'{cid}: {len(tiers)} tiers, maxTiers {c.get("maxTiers")}')
        return {'name': c['name'], 'category': category, 'item': icon, 'tiers': tiers}

    def order(self, category):
        """A category's collections in the menu's order: the wiki's menu page (the recording's Combat is the
        same), then any it doesn't have yet, by name."""
        ids = list(self.api['collections'][category]['items'])
        by_name = {self.api['collections'][category]['items'][i]['name']: i for i in ids}
        try:
            text = self.wiki.page(category.capitalize() + '/Collection UI')
            listed = [plain(m.group(1)) for m in re.finditer(r'^\|\d+, *\d+ = [^,]*, none;[^,]*, &e([^,]*)', text, re.M)]
        except OSError as e:
            self.note('wiki pages', f'{category}/Collection UI: {e}')
            listed = []
        ordered = [by_name[n] for n in listed if n in by_name]
        rest = sorted((i for i in ids if i not in ordered), key=lambda i: self.api['collections'][category]['items'][i]['name'])
        if rest and listed:
            self.note('collections placed by name (not on the wiki\'s menu page)', f'{category}: {rest}')
        return ordered + rest

    # which items count toward a collection

    def counted_items(self):
        """Each item that adds to a collection when a player gets it: the collection's own (1 each) and, for
        items the game calls "Collection Item", what one takes to craft of it (160 for an enchanted one)."""
        base = {}
        for cid in self.collections:
            if cid in GROUPS:
                for item_id in self.items:
                    if GROUPS[cid][1](item_id):
                        base[item_id] = cid
            elif cid in self.items:
                base[cid] = cid
        counted = {i: [cid, 1] for i, cid in base.items()}
        memo = {}

        def worth(item_id, depth=0):
            """(base item, how many) one of item_id is made of, when every recipe step is one kind of item."""
            if item_id in base:
                return item_id, 1
            if item_id in memo or depth > 8:
                return memo.get(item_id)
            memo[item_id] = None
            for cells, count in self.neu.recipes(item_id):
                kinds = collections.Counter()
                for cell in cells:
                    if cell:
                        kinds[cell[0]] += cell[1]
                if len(kinds) != 1:
                    continue
                (ingredient, total), = kinds.items()
                if total % count:
                    continue
                inner = worth(ingredient, depth + 1)
                if inner:
                    memo[item_id] = (inner[0], inner[1] * total // count)
                    break
            return memo[item_id]

        for item_id, item in sorted(self.items.items()):
            if item_id in counted or 'Collection Item' not in (item.get('categories') or []):
                continue
            w = worth(item_id)
            if w:
                counted[item_id] = [base[w[0]], w[1]]
            else:
                self.note('collection items that count toward no collection', item_id)
        return dict(sorted(counted.items()))

    # boss collections

    def bosses(self):
        data = Lua.first_table(self.wiki.page('Module:Collection/Data'))
        out = []
        for name, boss_id, floor in BOSSES:
            entry = data.get(name)
            if not entry:
                self.note('boss collections', f'{name}: not in the wiki\'s data')
                continue
            tiers = []
            n = 1
            while n in entry:
                t = entry[n]
                rewards = []
                for r in t.get('reward', {}).values() if isinstance(t.get('reward'), dict) else []:
                    rewards.append(self.boss_reward(boss_id, n, r))
                tiers.append({'amount': int(t['required']), 'rewards': [r for r in rewards if r]})
                n += 1
            boss = {'id': boss_id, 'name': name, 'floor': floor, 'tiers': tiers}
            head = BOSS_HEADS.get(boss_id)
            texture = self.neu.texture(head) if head else None
            if texture:
                boss['texture'] = texture
            elif head:
                self.note('boss heads', f'{boss_id}: NEU has no {head}')
            out.append(boss)
        return out

    def boss_reward(self, boss_id, tier, r):
        name, kind = str(r.get(1, '')), r.get('type', 'Recipe')
        amount = r.get('amount')
        if kind == 'SkyBlock Experience':
            return {'type': 'SKYBLOCK_XP', 'amount': int(name)}
        if kind == 'Essence':
            recorded = self.recorded.get(boss_id, {}).get(str(tier), {}).get(name.upper())
            got = {'type': 'ESSENCE', 'essence': name.upper()}
            if recorded or amount:
                got['amount'] = int(recorded or amount)
            else:
                self.note('essence rewards with no amount', f'{boss_id} {to_roman(tier)}: {name}')
            return got
        item = self.resolve(name)
        got = {'type': 'ITEM' if kind == 'Reward' else 'RECIPE', 'name': plain(name)}
        if item:
            got['item'] = item
        else:
            self.note('boss rewards naming no item in items.json', f'{boss_id} {to_roman(tier)}: {name}')
        if amount:
            got['amount'] = int(amount)
        return got

    # recipes and the Recipe Book

    def slayer_unlocks(self):
        """Item id -> (slayer, level) from the slayers' wiki pages: their Leveling Rewards table, whose fifth
        cell of a level's row is its Items Unlocked ({{Slot|Revenant Falchion}}...). Minions are left out (their
        recipes are the minions')."""
        if hasattr(self, '_slayer_unlocks'):
            return self._slayer_unlocks
        unlocks = {}
        for slayer in SLAYERS:
            try:
                text = self.wiki.page(slayer + ' Slayer')
            except OSError as e:
                self.note('wiki pages', f'{slayer} Slayer: {e}')
                continue
            lines = text.split('\n')
            for i, line in enumerate(lines):
                m = re.search(r'\{\{Text anchor\|[IVX]+\|(\d+)\}\}', line)
                if not m:
                    continue
                cells = [l for l in lines[i + 1:i + 8] if l.startswith('|') and not l.startswith(('|-', '|}'))]
                if len(cells) < 4:
                    continue
                for slot in re.findall(r'\{\{[Ss]lot\|([^}]+)\}\}', cells[3]):
                    name = plain(slot.split(';')[0].split(',')[0])
                    if re.search(r' Minion( [IVX]+)?$', name):
                        continue
                    item = self.resolve_rarity(name)
                    if item:
                        unlocks.setdefault(item, (slayer.upper(), int(m.group(1))))
                    else:
                        self.note('slayer unlocks naming no item in items.json', f'{slayer} {m.group(1)}: {name}')
        self._slayer_unlocks = unlocks
        return unlocks

    def resolve_rarity(self, name):
        """An item named with its rarity ("Wisp Upgrade Stone (Epic)", the slayer pages' way for items that share
        a name); a plain name as resolve() takes it."""
        m = re.fullmatch(r'(.+) \((Common|Uncommon|Rare|Epic|Legendary|Mythic)\)', name)
        if not m:
            return self.resolve(name)
        ids = [i for i in self.by_name.get(m.group(1), []) if self.items[i].get('rarity') == m.group(2).upper()]
        return ids[0] if len(ids) == 1 else None

    def slayer_book_pages(self):
        """Recipe name -> slayer, for the Recipe Book's Slayer pages (one a slayer)."""
        pages = {}
        for page in dict(BOOK)['SLAYER']:
            for name in book_page(self.wiki, page):
                pages.setdefault(name, page.upper())
        return pages

    def requirement(self, item_id, unlocked_by):
        """What unlocks an item's recipe: the collection tier whose unlocks name it (the API), else what NEU's
        crafttext says ("Requires: Blaze Rod IV", "Requires: Zombie Slayer 7"), else the slayer level whose
        wiki page lists it; None if nothing does."""
        if item_id in unlocked_by:
            cid, tier = unlocked_by[item_id]
            return {'collection': cid, 'tier': tier}
        text = self.neu.crafttext(item_id)
        m = re.fullmatch(r'Requires:? (.+) ([IVXL]+)', text)
        if m and m.group(1) in self.names:
            return {'collection': self.names[m.group(1)], 'tier': roman(m.group(2))}
        m = re.fullmatch(r'Requires: (\w+) Slayer (\d+)', text)
        if m:
            return {'slayer': m.group(1).upper(), 'level': int(m.group(2))}
        if text:
            return {'other': text}
        if item_id in self.slayer_unlocks():
            slayer, level = self.slayer_unlocks()[item_id]
            return {'slayer': slayer, 'level': level}
        return None

    def recipes(self, book_lists, colls, bosses):
        unlocked_by = {}
        for cid, c in list(colls.items()) + [(b['id'], b) for b in bosses]:
            for n, t in enumerate(c['tiers'], 1):
                for r in t['rewards']:
                    if r['type'] in ('RECIPE',) and 'item' in r:
                        unlocked_by.setdefault(r['item'], (cid, n))
                    elif r['type'] == 'MINION_RECIPES' and 'item' in r:
                        base = r['item'][:-len('_1')]
                        for level in range(1, 13):
                            unlocked_by.setdefault(f'{base}_{level}', (cid, n))
        recipes = {}
        for item_id in sorted(self.neu.items):
            if item_id not in self.items:
                continue
            shapes = []
            for cells, made in self.neu.recipes(item_id):
                if any(cell and cell[0] not in self.items for cell in cells):
                    self.counts['recipes with an ingredient not in items.json'] += 1
                    continue
                shapes.append({'cells': [f'{c[0]}:{c[1]}' if c else '' for c in cells], 'count': made})
            if not shapes:
                continue
            recipe = {'shapes': enchanted_shapes(shapes)}
            requires = self.requirement(item_id, unlocked_by)
            if requires:
                recipe['requires'] = requires
            if re.fullmatch(r'[A-Z_]+_GENERATOR_\d+', item_id):
                recipe['later'] = 'minions'
            recipes[item_id] = recipe
        self.counts['recipes'] = len(recipes)

        return recipes, self.book(book_lists, colls, bosses, recipes)

    def book(self, book_lists, colls, bosses, recipes):
        """The Recipe Book's entries: what collection tiers unlock (the API; which category from the wiki's lists,
        else what the item is, else the collection's), then what the wiki's lists have that no tier unlocks (the
        slayers', and the ones a new profile has). Kinds: RECIPE (craftable here), MINION and PET (their systems
        aren't here yet: always locked), OTHER (brewing, books and the like: no crafting recipe here, always
        locked)."""
        book = {}
        wiki_category = {}
        for category, names in book_lists.items():
            for name in names:
                wiki_category.setdefault(name, category)

        def category_for(name, fallback):
            if name in wiki_category:
                return wiki_category[name]
            if name.startswith('Enchanted Book ('):
                return 'ENCHANTING'
            if name.endswith(' Potion'):
                return 'ALCHEMY'
            if name.endswith(' Shard'):
                return 'HUNTING'
            return fallback

        def add(kind, key, entry):
            if (kind, key) not in book:
                if kind != 'RECIPE':
                    entry['kind'] = kind
                book[(kind, key)] = entry

        # Not a boss collection's recipes (its Diamond Head): the recorded Combat Recipes haven't them, and no
        # other category is known to (UNKNOWN); they're crafted once unlocked all the same.
        for cid, c in colls.items():
            for n, t in enumerate(c['tiers'], 1):
                requires = {'collection': cid, 'tier': n}
                for r in t['rewards']:
                    if r['type'] == 'MINION_RECIPES':
                        entry = {'category': category_for(r['name'] + ' Minion Recipes', c['category']),
                                 'name': r['name'] + ' Minion', 'requires': requires}
                        if 'item' in r:
                            entry['item'] = r['item']
                        add('MINION', r['name'], entry)
                    elif r['type'] == 'PET_RECIPE':
                        pet = re.sub(r'[^A-Z0-9]+', '_', r['name'].upper())
                        entry = {'category': self.neu.pet_types.get(pet, c['category']), 'name': r['name'], 'requires': requires}
                        if 'texture' in r:
                            entry['texture'] = r['texture']
                        add('PET', r['name'], entry)
                    elif r['type'] == 'RECIPE':
                        item = r.get('item')
                        entry = {'category': category_for(r['name'], c['category']), 'name': r['name'], 'requires': requires}
                        if item in recipes:
                            entry['item'] = item
                            add('RECIPE', item, entry)
                        else:
                            add('OTHER', r['name'], entry)
        for category, names in book_lists.items():
            for name in names:
                if name.endswith(' Minion Recipes') or name.startswith('Mystery ') and name.endswith(' Pet'):
                    continue  # the collections' (above); pets as they were once sold
                item = self.resolve(name)
                if item in recipes:
                    entry = {'category': category, 'name': name, 'item': item}
                    if not recipes[item].get('requires') and category == 'SLAYER':
                        # A slayer's recipe that no source gives a level for: its slayer's first (UNKNOWN), so
                        # it isn't everyone's from the start.
                        recipes[item]['requires'] = {'slayer': self.slayer_book_pages()[name], 'level': 1}
                        self.note('slayer recipes with no known level (taken as 1)', f'{name} ({item})')
                    if recipes[item].get('requires'):
                        entry['requires'] = recipes[item]['requires']
                    add('RECIPE', item, entry)
                elif ('OTHER', name) not in book:
                    self.note('wiki book entries left out: no craftable item, nothing known to unlock them',
                              f'{category}: {name}')
        order = [c for c, _ in BOOK]
        entries = sorted(book.values(), key=lambda e: order.index(e['category']))
        self.counts['book entries'] = len(entries)
        return entries


def enchanted_shapes(shapes):
    """An enchanted item's recipe (five of one item in a plus) has a second way to lay it out: the first five
    slots (the wiki's Enchanted Crafting Recipe Row, its two frames)."""
    out = list(shapes)
    for shape in shapes:
        cells = shape['cells']
        filled = [i for i, c in enumerate(cells) if c]
        if filled == [1, 3, 4, 5, 7] and len({cells[i] for i in filled}) == 1:
            second = {'cells': [cells[1] if i < 5 else '' for i in range(9)], 'count': shape['count']}
            if second not in out:
                out.append(second)
    return out


def to_roman(n):
    out = ''
    for value, letters in ((50, 'L'), (40, 'XL'), (10, 'X'), (9, 'IX'), (5, 'V'), (4, 'IV'), (1, 'I')):
        while n >= value:
            out += letters
            n -= value
    return out


def dump(data):
    return json.dumps(data, ensure_ascii=False, indent=1, sort_keys=False) + '\n'


def report(b, source, colls, bosses, counted, recipes, book):
    lines = ['# Collections data report', '',
             f'- API lastUpdated {source["api_last_updated"]} (version {source["api_version"]}), NEU {source["neu_commit"][:10]}',
             f'- {len(colls)} collections, {b.counts["tiers"]} tiers; {len(bosses)} boss collections',
             f'- {len(counted)} items count toward a collection',
             f'- {len(recipes)} recipes; {len(book)} Recipe Book entries '
             f'({sum(1 for e in book if e.get("kind", "RECIPE") == "RECIPE")} craftable)',
             f'- {b.counts["recipes with an ingredient not in items.json"]} recipe shapes dropped: an ingredient isn\'t in items.json',
             '']
    per = collections.Counter(e['category'] for e in book)
    lines.append('Recipe Book: ' + ', '.join(f'{c} {per[c]}' for c, _ in BOOK))
    lines.append('')
    for kind in sorted(b.problems):
        items = b.problems[kind]
        lines.append(f'## {kind} ({len(items)})')
        lines.append('')
        for detail in items[:200]:
            lines.append(f'- {detail}')
        if len(items) > 200:
            lines.append(f'- and {len(items) - 200} more')
        lines.append('')
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser(description='Build collections.json and recipes.json (format 1) from the Hypixel '
                                             'collections API, NEU-REPO, items.json and the wiki.')
    ap.add_argument('--api', default=API_URL, help='the collections API response: a file, or a URL (default: fetch it)')
    ap.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO clone')
    ap.add_argument('--items', default=None, help='the plugin\'s items.json (default: the private data checkout\'s)')
    ap.add_argument('--wiki-cache', default=None, help='a folder to keep fetched wiki pages in (and read them from)')
    ap.add_argument('--recorded', default=None, help='what the recordings show that the sources lack (default: '
                                                      'collections/recorded.json in the private data checkout, if there)')
    ap.add_argument('--out', default=None, help='where the files go (default: collections/ of the private data checkout)')
    args = ap.parse_args()
    t0 = time.time()
    data = default_data()
    out = args.out or os.path.join(data, 'collections')
    items_path = args.items or os.path.join(data, 'items', 'items.json')
    recorded_path = args.recorded or os.path.join(data, 'collections', 'recorded.json')
    api = load_json(args.api, 'collections API')
    if not api.get('success') or not isinstance(api.get('collections'), dict):
        sys.exit(f'{args.api}: not the collections API response')
    items = load_json(items_path, 'items.json')['items']
    recorded = load_json(recorded_path, 'recorded') if os.path.exists(recorded_path) else {}
    try:
        commit = git(args.neu, 'rev-parse', 'HEAD')
    except (OSError, subprocess.CalledProcessError):
        sys.exit(f'{args.neu}: not a git clone, so its commit can\'t be recorded')
    neu = Neu(args.neu)
    wiki = Wiki(args.wiki_cache)
    b = Builder(api, items, neu, wiki, glyph_symbols(), recorded)

    categories = []
    colls = {}
    for category, name in CATEGORIES:
        ids = b.order(category)
        categories.append({'id': category, 'name': name, 'collections': ids})
        for cid in ids:
            colls[cid] = b.collection(cid, api['collections'][category]['items'][cid], category)
    bosses = b.bosses()
    counted = b.counted_items()
    recipes, book = b.recipes(book_pages(wiki), colls, bosses)

    source = {'api_last_updated': api['lastUpdated'], 'api_version': api.get('version'), 'neu_commit': commit}
    collections_json = dump({'format': 1, 'source': source, 'categories': categories, 'collections': colls,
                             'bosses': bosses, 'items': counted})
    recipes_json = dump({'format': 1, 'source': source, 'recipes': recipes, 'book': book})
    for text in (collections_json, recipes_json):
        left = sorted({'%04X' % ord(c) for c in PUA.findall(text)})
        if left:
            sys.exit(f'private-use glyphs left in the output: {left}')
    os.makedirs(out, exist_ok=True)
    for name, text in (('collections.json', collections_json), ('recipes.json', recipes_json),
                       ('report.md', report(b, source, colls, bosses, counted, recipes, book))):
        with open(os.path.join(out, name), 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
    print(f'{len(colls)} collections, {len(bosses)} bosses, {len(counted)} counted items, {len(recipes)} recipes, '
          f'{len(book)} book entries -> {out} ({time.time() - t0:.1f} s; wiki pages fetched: {len(wiki.fetched)})')


if __name__ == '__main__':
    main()
