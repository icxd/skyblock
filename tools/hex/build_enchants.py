#!/usr/bin/env python3
# python3 tools/hex/build_enchants.py --neu NEU-REPO [--wiki-cache DIR] [--out DIR]
#
# Builds the Hex's enchantment table, enchantments.json (format 1, see README.md), and enchantments_report.md next
# to it: every enchantment there is, with its name, what it goes on, its levels and what each says, what the Hex
# charges for it and what it can't be on with. From NotEnoughUpdates-REPO (the in-game enchanted books,
# items/<ID>;<level>.json, and constants/enchants.json: Exp costs, the Enchantment Table's levels) and
# the Hypixel SkyBlock wiki (Module:Enchantment/Data: each level's values, the Enchanting level needed, tiers; The
# Hex/UI/Weapon and /Armor: the order the Hex lists them in). The plugin reads it for the enchantments' text on
# items too. All of it is Hypixel's, so the output goes to the private data repository (hex/ in it), never into
# this one.
import argparse
import collections
import json
import os
import re
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'items'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'collections'))
import build_items  # noqa: E402  (the glyph table)
import build_collections  # noqa: E402  (the wiki's pages and Lua data)

MODULE = 'Module:Enchantment/Data'
HEX_PAGES = ['The Hex/UI/Weapon', 'The Hex/UI/Armor']
RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'DIVINE', 'SPECIAL', 'VERY SPECIAL']
WIKI_RARITY = {'C': 'COMMON', 'U': 'UNCOMMON', 'R': 'RARE', 'E': 'EPIC', 'L': 'LEGENDARY', 'M': 'MYTHIC'}
ROMAN = {'I': 1, 'V': 5, 'X': 10, 'L': 50}
# Enchantments Hypixel took out of the game that NEU still has a book of: kept for the items that have them, never
# offered in the Hex. Telekinesis became Auto-pickup (the wiki's Enchanted Book and Enchantments, 2022/April 25).
REMOVED = {'telekinesis'}
PUA = re.compile('[-]')


def roman(s):
    total, last = 0, 0
    for c in reversed(s):
        v = ROMAN[c]
        total += -v if v < last else v
        last = max(last, v)
    return total


def plain(s):
    return re.sub(r'[&§][0-9a-fk-or]', '', s or '')


def snake(name):
    """"Bobbin' Time" as "bobbin_time", as NEU keys its Exp costs by name."""
    return re.sub(r'[^a-z0-9]+', '_', name.lower().replace("'", '')).strip('_')


def plugin_id(hypixel):
    """The id the plugin stores an enchantment under: Hypixel's, but an ultimate's without "ultimate_" (as the
    plugin has always stored One For All, "one_for_all")."""
    return hypixel[len('ultimate_'):] if hypixel.startswith('ultimate_') else hypixel


# ---------- text, as tools/items made the enchantments' text before ----------

def collapse(s):
    """Colour codes that don't change anything dropped ("§7a §7b" is "§7a b"), § as &."""
    out, cur, i = [], None, 0
    while i < len(s):
        if s[i] == '§' and i + 1 < len(s):
            code = s[i:i + 2]
            if code[1] in '0123456789abcdef':
                if code != cur:
                    out.append(code)
                    cur = code
            else:
                out.append(code)
                cur = None if code[1] == 'r' else cur + code if cur else code
            i += 2
            continue
        out.append(s[i])
        i += 1
    return ''.join(out).replace('§', '&')


class Glyphs:
    """Hypixel's private-use glyphs as the classic symbols the plugin shows (tools/items/data/glyphs.tsv); one with
    none is dropped, and counted for the report. Colour codes are left as they are (§)."""

    def __init__(self):
        rows = build_items.tsv('glyphs.tsv')
        self.symbols = {chr(int(cp, 16)): symbol for cp, symbol, _how in rows if symbol}
        self.dropped = collections.defaultdict(set)

    def __call__(self, where, s):
        def one(m):
            c = m.group(0)
            if c not in self.symbols:
                self.dropped[ord(c)].add(where)
            return self.symbols.get(c, '')
        return PUA.sub(one, s)


def fmt(v):
    return ('%d' % v) if float(v).is_integer() else ('%s' % v)


def numbers(text):
    """The number tokens that stand alone (a colour code right before one is fine; "2nd" isn't a number)."""
    masked = re.sub(r'&[0-9a-fk-or]', '\0\0', text)
    for m in re.finditer(r'\d+(?:[.,]\d+)*', masked):
        before = masked[m.start() - 1] if m.start() else ' '
        if before.isalnum():
            continue
        after = masked[m.end()] if m.end() < len(masked) else ' '
        if after.isascii() and after.isalpha() and after not in 'xsk':
            continue
        yield m


def substitute(text, old, new):
    """Each of a level's values, in order, as another level's: that level's text; None if a value isn't there."""
    pos, out = 0, text
    for a, b in zip(old, new):
        m = next((mm for mm in numbers(out) if mm.start() >= pos and float(mm.group(0).replace(',', '')) == float(a)), None)
        if m is None:
            return None
        rep = fmt(b)
        out = out[:m.start()] + rep + out[m.end():]
        pos = m.start() + len(rep)
    return out


# ---------- NEU ----------

class Book:
    """One enchanted book as the game shows it: its enchantment and level, name, description lines, the line on
    what the next tier takes, sources, what it's applied to, conflicts and rarity."""

    def __init__(self, hypixel, level, lore, glyphs):
        self.hypixel, self.level = hypixel, level
        k = next((i for i, l in enumerate(lore) if l.startswith(('§9', '§d')) and re.search(r' (?:[IVXL]+|\d+)$', plain(l))), None)
        if k is None:
            raise ValueError('no name line')
        self.name = plain(lore[k]).rsplit(' ', 1)[0]
        desc = []
        for line in lore[k + 1:]:
            if not line.strip() or line.startswith('§8Gain '):
                break
            desc.append(glyphs(line))
        # "§850k Combat XP to tier up!" closes a tiered enchantment's description.
        self.tier_up = collapse(desc.pop()) if desc and desc[-1].startswith('§8') and desc[-1].endswith('to tier up!') else None
        self.desc = desc
        self.sections = collections.defaultdict(list)
        head = None
        for line in lore[k + 1 + len(desc):]:
            if not line.strip():
                head = None
            elif line in ('§6Source:', '§6Sources:', '§6Applied To:', '§6Conflicts:'):
                head = plain(line).rstrip(':')
            elif head:
                self.sections[head].append(plain(line).lstrip('- ').strip())
        last = plain(next((l for l in reversed(lore) if l.strip()), ''))
        self.rarity = last if last in RARITIES else None

    def text(self):
        """The description as one paragraph, as item lore wraps it."""
        return collapse(' '.join(self.desc))

    def lines(self):
        return [collapse(line) for line in self.desc]

    def table_max(self):
        """The highest level the Enchantment Table sells, from its sources ("I-V: Enchantment Table,"); None."""
        for source in self.sections['Source'] + self.sections['Sources']:
            m = re.match(r'([IVXL]+)(?:-([IVXL]+))?: Enchantment Table', source)
            if m:
                return roman(m.group(2) or m.group(1))
        return None


def neu_books(path, glyphs):
    """Every enchanted book in NEU's items, by the enchantment's Hypixel id, then level."""
    books = collections.defaultdict(dict)
    items = os.path.join(path, 'items')
    for f in sorted(os.listdir(items)):
        m = re.match(r'([A-Z0-9_]+);(\d+)\.json$', f)
        if not m:
            continue
        with open(os.path.join(items, f), encoding='utf-8') as fh:
            dump = json.load(fh)
        if dump.get('itemid') != 'minecraft:enchanted_book':
            continue
        e = re.search(r'enchantments:\{([a-z0-9_]+):(\d+)\}', dump.get('nbttag', ''))
        hypixel, level = (e.group(1), int(e.group(2))) if e else (m.group(1).lower(), int(m.group(2)))
        books[hypixel][level] = Book(hypixel, level, dump['lore'], lambda s: glyphs(f[:-5], s))
    return books


# ---------- the wiki ----------

def wiki_enchantments(wiki):
    """Module:Enchantment/Data, by the enchantment's name, each list keyed by level."""
    return build_collections.Lua.first_table(wiki.page(MODULE))


def wiki_list(table, key):
    """A Lua list (keyed 1..n) as a Python list, None where it has nothing."""
    value = table.get(key)
    if not isinstance(value, dict):
        return []
    n = max((k for k in value if isinstance(k, int)), default=0)
    return [value.get(i) for i in range(1, n + 1)]


def wiki_values(table):
    """Each level's values, in the order its description has them: [[5], [10], ...]; [] for none."""
    columns = [wiki_list({'v': c}, 'v') for c in wiki_list(table, 'vars') if isinstance(c, dict)]
    if not columns:
        return []
    n = max(len(c) for c in columns)
    return [[c[i] if i < len(c) else None for c in columns] for i in range(n)]


def hex_order(wiki):
    """The Hex's Default order, as far as the wiki's screens show it: the names in its Enchant Item lists, page by page,
    row by row (a sword's enchantments and ultimates, armor's ultimates)."""
    names = []
    for title in HEX_PAGES:
        text = wiki.page(title)
        for block in re.split(r'\{\{UI\|', text)[1:]:
            if not block.startswith('The Hex ➜ Enchant Item'):
                continue
            entries = []
            for m in re.finditer(r'^\|(\d), (\d)=Enchanted Book, [^,]*, ([^,]+),', block, re.M):
                entries.append((int(m.group(1)), int(m.group(2)), plain(m.group(3)).strip()))
            for _r, _c, name in sorted(entries):
                if name not in names:
                    names.append(name)
    return names


# ---------- the table ----------

class Builder:
    def __init__(self, books, constants, wiki_data, order_names):
        self.books = books
        self.constants = constants
        self.wiki = wiki_data
        self.order_names = order_names
        self.problems = collections.defaultdict(list)
        self.xp = {k.lower(): v for k, v in constants['enchants_xp_cost'].items()}
        self.table = {k.lower(): v for k, v in constants['max_xp_table_levels'].items()}
        # Every name an enchantment goes by (its name now, its id's words, the wiki's old names) to its id.
        self.by_name = {}
        for hypixel, levels in books.items():
            self.by_name[levels[max(levels)].name] = hypixel
        for hypixel in books:
            self.by_name.setdefault(plugin_id(hypixel).replace('_', ' ').title(), hypixel)

    def note(self, kind, detail):
        self.problems[kind].append(detail)

    def wiki_entry(self, hypixel, name):
        for key in (name, plugin_id(hypixel).replace('_', ' ').title()):
            if key in self.wiki:
                return self.wiki[key]
        self.note('no wiki entry (no Enchanting level, values or tiers)', f'{hypixel} ({name})')
        return {}

    def entry(self, hypixel):
        books = self.books[hypixel]
        newest = books[max(books)]
        name = newest.name
        w = self.wiki_entry(hypixel, name)
        ultimate = hypixel.startswith('ultimate_')
        values = wiki_values(w)
        tiers = wiki_list(w, 'upgrades')

        # Levels: the books'; a tiered enchantment (Champion, ...) has a book of level I only, the wiki has the rest.
        book_max = max(books)
        wiki_max = w.get('max') if isinstance(w.get('max'), int) else 0
        top = wiki_max if tiers else book_max
        if wiki_max and wiki_max != book_max and not tiers:
            self.note('the wiki\'s highest level isn\'t the books\' (the books\' is used)', f'{hypixel}: books {book_max}, wiki {wiki_max}')
        levels = {}
        for level, book in books.items():
            levels[level] = {'text': book.text(), 'lines': book.lines()}
        # Levels with no book: the nearest book's text with that level's values (the wiki's), up to the wiki's max.
        for level in range(1, max(top, wiki_max, book_max) + 1):
            if level in levels:
                continue
            base = min(books, key=lambda b: (abs(b - level), b))
            vb = values[base - 1] if base - 1 < len(values) else None
            vl = values[level - 1] if level - 1 < len(values) else None
            if vb is None or vl is None or None in vb or None in vl:
                continue
            text = substitute(levels[base]['text'], vb, vl)
            lines = substitute('\n'.join(levels[base]['lines']), vb, vl)
            if text is None or lines is None:
                self.note('a level with no book whose text couldn\'t be made (its values aren\'t in its book)', f'{hypixel} {level}')
                continue
            levels[level] = {'text': text, 'lines': lines.split('\n')}
        low = min(books)
        # Levels under the lowest book exist where the wiki has their values (Counter-Strike III, IV).
        while low > 1 and low - 1 in levels:
            low -= 1
        rarities = wiki_list(w, 'rarity')
        for level, data in levels.items():
            book = books.get(level)
            rarity = book.rarity if book else WIKI_RARITY.get(rarities[level - 1]) if level - 1 < len(rarities) else None
            if rarity:
                data['rarity'] = rarity
            tier = book.tier_up if book and book.tier_up else None
            if tier is None and tiers and level - 1 < len(tiers) and tiers[level - 1] and w.get('upgradeType'):
                tier = f'&8{tiers[level - 1]} {w["upgradeType"]} to tier up!'
            if tier:
                data['tier_up'] = tier

        # What it goes on: every book's "Applied To" (they differ a little between levels).
        applies = []
        for level in sorted(books):
            for label in books[level].sections['Applied To']:
                if label not in applies:
                    applies.append(label)

        # The Enchantment Table's highest level: the books' sources, else NEU's.
        table = next((t for t in (books[l].table_max() for l in sorted(books, reverse=True)) if t), None)
        neu_table = self.table.get(hypixel) or self.table.get(plugin_id(hypixel))
        if table and neu_table and table != neu_table:
            self.note('Enchantment Table level: the books and NEU disagree (the books\' is used)', f'{hypixel}: {table} vs {neu_table}')
        table = table or neu_table

        # Exp levels per level: NEU's (0 is none known), else the wiki's.
        keys = [plugin_id(hypixel), hypixel, snake(name)]
        neu_xp = next((self.xp[k] for k in keys if k in self.xp), [])
        wiki_xp = wiki_list(w, 'cost')
        xp = []
        for level in range(1, top + 1):
            n = neu_xp[level - 1] if level - 1 < len(neu_xp) and neu_xp[level - 1] else None
            v = wiki_xp[level - 1] if level - 1 < len(wiki_xp) and wiki_xp[level - 1] else None
            if n and v and n != v:
                self.note('Exp levels: NEU and the wiki disagree (NEU\'s is used)', f'{hypixel} {level}: {n} vs {v}')
            if level >= low and not (n or v):
                self.note('Exp levels: no source has them (UNKNOWN; 0)', f'{hypixel} {level}')
            xp.append(n or v)

        entry = {'name': name, 'hypixel': hypixel, 'ultimate': ultimate}
        if hypixel in REMOVED:
            entry['removed'] = True
        entry.update({'min': low, 'max': top, 'table': table, 'xp': xp,
                      'enchanting': w.get('req') if isinstance(w.get('req'), int) else 0,
                      'applies': applies, 'conflicts': [],
                      'levels': {str(level): levels[level] for level in sorted(levels)}})
        return entry

    def conflicts(self, entries):
        """The books' Conflicts, both ways. NEU's pools are older: where they have more (the Turbos with each other,
        Fortune with Smelting Touch, One For All with the sword enchantments) they're only reported, since live items
        carry those together (and One For All's rule is the plugin's: it goes with nothing). The pool of every
        ultimate isn't a conflict either: only one goes on an item anyway."""
        pairs = set()
        for hypixel, books in self.books.items():
            for book in books.values():
                for name in book.sections['Conflicts']:
                    other = self.by_name.get(name)
                    if other is None:
                        self.note('a conflict with no such enchantment', f'{hypixel}: {name}')
                        continue
                    pairs.add((hypixel, other))
                    pairs.add((other, hypixel))
        for pool in self.constants['enchant_pools']:
            ids = [p.lower() for p in pool]
            if sum(1 for i in ids if i.startswith('ultimate_')) > 2:
                continue
            for a in ids:
                for b in ids:
                    if a < b and (a, b) not in pairs:
                        self.note('a conflict NEU\'s pools have and the books don\'t (not used)', f'{a}: {b}')
        for a, b in sorted(pairs):
            if a not in self.books or b not in self.books:
                self.note('a conflict with an enchantment there\'s no book of', f'{a}: {b}')
                continue
            entries[plugin_id(a)]['conflicts'].append(plugin_id(b))

    def order(self, entries):
        """The wiki's order where it has one, then the rest by name."""
        ids = []
        for name in self.order_names:
            hypixel = self.by_name.get(name)
            if hypixel is None:
                self.note('a name in the wiki\'s Hex screens that no book has', name)
            elif plugin_id(hypixel) not in ids:
                ids.append(plugin_id(hypixel))
        ids += sorted((i for i in entries if i not in ids), key=lambda i: (entries[i]['name'].lower(), i))
        return ids

    def build(self):
        entries = {}
        for hypixel in sorted(self.books):
            pid = plugin_id(hypixel)
            if pid in entries:
                sys.exit(f'two enchantments would be stored as {pid}')
            entries[pid] = self.entry(hypixel)
        self.conflicts(entries)
        return self.order(entries), entries


def dump(source, order, entries):
    """One enchantment a line, so a change is a one-line diff."""
    head = json.dumps({'format': 1, 'source': source, 'order': order}, ensure_ascii=False)[:-1]
    body = ',\n'.join(json.dumps(k, ensure_ascii=False) + ':' + json.dumps(v, ensure_ascii=False, separators=(',', ':'))
                      for k, v in entries.items())
    return head + ',"enchantments":{\n' + body + '\n}}\n'


def report(b, source, order, entries):
    labels = collections.Counter(label for e in entries.values() for label in e['applies'])
    lines = ['# Enchantments data report', '',
             f'- NEU {source["neu_commit"][:10]}; the wiki\'s {MODULE} and ' + ', '.join(HEX_PAGES),
             f'- {len(entries)} enchantments, {sum(1 for e in entries.values() if e["ultimate"])} ultimate; '
             f'{sum(len(e["levels"]) for e in entries.values())} levels with text',
             f'- {len(b.order_names)} in the order the wiki\'s Hex screens show; the other {len(order) - len(b.order_names)} by name',
             '- Applied To labels: ' + ', '.join(f'{k} {v}' for k, v in labels.most_common()), '']
    for kind in sorted(b.problems):
        items = b.problems[kind]
        lines += [f'## {kind} ({len(items)})', '']
        lines += [f'- {d}' for d in items[:300]]
        if len(items) > 300:
            lines.append(f'- and {len(items) - 300} more')
        lines.append('')
    return '\n'.join(lines)


def main():
    ap = argparse.ArgumentParser(description='Build the Hex\'s enchantments.json (format 1) from NEU-REPO and the wiki.')
    ap.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO clone')
    ap.add_argument('--wiki-cache', default=None, help='a folder to keep fetched wiki pages in (and read them from)')
    ap.add_argument('--out', default=None, help='where it goes (default: hex/ of the private data checkout)')
    args = ap.parse_args()
    t0 = time.time()
    out = args.out or os.path.join(build_collections.default_data(), 'hex')
    try:
        commit = build_collections.git(args.neu, 'rev-parse', 'HEAD')
    except (OSError, subprocess.CalledProcessError):
        sys.exit(f'{args.neu}: not a git clone, so its commit can\'t be recorded')
    glyphs = Glyphs()
    with open(os.path.join(args.neu, 'constants', 'enchants.json'), encoding='utf-8') as f:
        constants = json.load(f)
    wiki = build_collections.Wiki(args.wiki_cache)
    b = Builder(neu_books(args.neu, glyphs), constants, wiki_enchantments(wiki), hex_order(wiki))
    order, entries = b.build()
    for cp, books in sorted(glyphs.dropped.items()):
        b.note('glyphs dropped (no classic symbol in tools/items/data/glyphs.tsv)', f'{cp:04X} in {", ".join(sorted(books))}')
    source = {'neu_commit': commit, 'wiki': [MODULE] + HEX_PAGES}
    text = dump(source, order, entries)
    left = sorted({'%04X' % ord(c) for c in PUA.findall(text)})
    if left:
        sys.exit(f'private-use glyphs left in the output: {left}')
    os.makedirs(out, exist_ok=True)
    for name, content in (('enchantments.json', text), ('enchantments_report.md', report(b, source, order, entries))):
        with open(os.path.join(out, name), 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
    print(f'{len(entries)} enchantments -> {out} ({time.time() - t0:.1f} s; wiki pages fetched: {len(wiki.fetched)})')


if __name__ == '__main__':
    main()
