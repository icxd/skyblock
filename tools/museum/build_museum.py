#!/usr/bin/env python3
# python3 tools/museum/build_museum.py --gui GUI.TXT [--api PATH_OR_URL] [--out DIR]
#
# Builds the plugin's Museum data, museum.json (format 1, see README.md), and report.md next to it: every item and
# armor set Madame Goldsworth takes, category by category, in her menus' order, with the SkyBlock XP each gives and
# what else counts as donating it, and the Museum Milestones' rewards. The entries, their names, XP and order come from a recording of Hypixel's own
# Museum menus (a replay decoded with tools/replay: its gui.txt); which items each one is (ids, a set's pieces, the
# starred versions, what counts as it) from Hypixel's items API. All of it is Hypixel's, so the output goes to the
# private data repository, never into this one.
import argparse
import collections
import json
import os
import re
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
API_URL = 'https://api.hypixel.net/v2/resources/skyblock/items'
COLOR = re.compile('[&§][0-9a-fk-or]|%%[a-z_]+%%', re.I)

# The categories as Your Museum lists them (the recording's slots), by the API's names.
CATEGORIES = [('COMBAT', 'Combat'), ('FARMING', 'Farming'), ('MINING', 'Mining'), ('FISHING', 'Fishing'),
              ('FORAGING', 'Foraging'), ('DUNGEONEERING', 'Dungeoneering'), ('HUNTING', 'Hunting')]
# Where a category page has its entries: the four middle rows, inside the glass.
ENTRY_SLOTS = [10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41,
               42, 43]
# Words a set's name and its id leave out when they're compared ("Arachne's Armor" is ARACHNE).
SET_WORDS = {'ARMOR', 'SET', 'EQUIPMENT', 'OUTFIT', 'OF', 'THE', 'S'}


def clean(s):
    return COLOR.sub('', s or '').strip()


# ---------- the recording ----------

def menus(path):
    """Each menu as it first opened: its title, and each slot's item, name and lore (the first time it was set)."""
    found = collections.OrderedDict()
    cur = last = None
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            m = re.match(r'^=== (\S+) opened: (.*)$', line)
            if m:
                cur = {'t': m.group(1), 'title': m.group(2), 'slots': {}}
                if cur['title'] in found:
                    cur = None
                else:
                    found[cur['title']] = cur
                last = None
                continue
            if line.startswith('===') or line.startswith('---'):
                cur = last = None
                continue
            if cur is None:
                continue
            m = re.match(r'^  \S+ slot (\d+) := (\S+) x(\d+) ?(.*)$', line)
            if m:
                last = {'item': m.group(2), 'count': int(m.group(3)), 'name': m.group(4), 'lore': []}
                cur['slots'].setdefault(int(m.group(1)), last)
            elif line.startswith('      ') and last is not None:
                last['lore'].append(line[6:])
            else:
                last = None
    return found


def recorded(found):
    """Each category's entries, page by page, as the menu listed them (its first sort, Ascending XP)."""
    pages = collections.defaultdict(dict)
    for title, menu in found.items():
        m = re.match(r'^(?:\((\d+)/(\d+)\) )?Museum ➜ (.+)$', title)
        if m:
            pages[m.group(3)][int(m.group(1) or 1)] = menu
    out = collections.OrderedDict()
    for _, name in CATEGORIES:
        entries = []
        for page in sorted(pages.get(name, {})):
            slots = pages[name][page]['slots']
            for slot in ENTRY_SLOTS:
                if slot not in slots:
                    continue
                item = slots[slot]
                lore = ' '.join(clean(line) for line in item['lore'])
                xp = re.search(r'Reward: \+(\d+) SkyBlock XP', lore)
                # The parent's name with its colours, as the line has it: "&9Revenant Falchion" ("Donating a &9Revenant
                # Falchion &7will count as donating this item."; a set's "Donating &9Heat Armor &7will give credit ...").
                # Its lines as they're wrapped, to the empty one after them.
                parent = parent_lines = None
                for i, line in enumerate(item['lore']):
                    if clean(line).startswith('Donating '):
                        parent_lines = []
                        for more in item['lore'][i:]:
                            if not clean(more):
                                break
                            parent_lines.append(more)
                        pm = re.search(r'Donating (?:an? )?(&.)?(.+?) &7will', ' '.join(parent_lines))
                        if pm:
                            parent = (pm.group(1) or '') + pm.group(2).strip()
                        break
                entries.append({'name': clean(item['name']), 'xp': int(xp.group(1)) if xp else None,
                                'set': 'armor set' in lore, 'parent_name': parent, 'parent_lines': parent_lines})
        out[name] = entries
    return out


def milestones(path):
    """The Museum Milestones menu's milestones, from every view of it the recording scrolled to: each one's number,
    the XP it takes ("Required XP: 1,000") and its reward lines ("&8+&b1% Bits Multiplier"), in order."""
    found = {}
    current = None
    with open(path, encoding='utf-8') as f:
        for line in f:
            line = line.rstrip('\n')
            m = re.match(r'^  (?:\S+ slot \d+ :=|\[\d+\]) \S+ x\d+ &aMuseum Milestone (\d+)$', line)
            if m:
                number = int(m.group(1))
                current = [] if number not in found else None
                if current is not None:
                    found[number] = current
            elif line.startswith('      ') and current is not None:
                current.append(line[6:])
            else:
                current = None
    out = []
    for number in sorted(found):
        lore = found[number]
        xp = next((int(clean(l).split(':')[1].replace(',', '')) for l in lore if clean(l).startswith('Required XP:')), None)
        rewards = []
        if '&7Rewards:' in lore:
            for line in lore[lore.index('&7Rewards:') + 1:]:
                if not clean(line):
                    break
                rewards.append(line)
        out.append({'milestone': number, 'xp': xp, 'rewards': rewards})
    return out


# ---------- the items API ----------

def load_api(where):
    if os.path.exists(where):
        with open(where, encoding='utf-8') as f:
            return json.load(f)
    request = urllib.request.Request(where, headers={'User-Agent': 'skyblock-museum-builder'})
    with urllib.request.urlopen(request, timeout=120) as response:
        return json.loads(response.read())


def tokens(s):
    s = s.upper().replace("'S", '').replace("'", '')
    return {t for t in re.split(r'[^A-Z0-9]+', s) if t and t not in SET_WORDS}


class Api:
    def __init__(self, answer):
        self.updated = answer.get('lastUpdated')
        self.items = {i['id']: i for i in answer['items']}
        self.singles = collections.defaultdict(dict)
        self.sets = collections.defaultdict(lambda: collections.defaultdict(list))
        self.special = sorted(k for k, v in self.items.items() if v.get('museum') is True and 'museum_data' not in v)
        for k, v in self.items.items():
            data = v.get('museum_data')
            if not data:
                continue
            if 'armor_set_donation_xp' in data:
                for set_id in data['armor_set_donation_xp']:
                    self.sets[data['category']][set_id].append(k)
            else:
                self.singles[data['category']].setdefault(clean(v.get('name')), k)

    def data(self, item):
        return self.items[item].get('museum_data') or {}

    def name(self, item):
        return clean(self.items[item].get('name'))

    def shared(self, category, set_id):
        """The words all of a set's pieces' names have ("Celeste" of Celeste Boots and the rest)."""
        words = None
        for piece in self.sets[category][set_id]:
            words = tokens(self.name(piece)) if words is None else words & tokens(self.name(piece))
        return words or set()

    def slots(self, category, set_id):
        """A set's pieces by what they're worn as (the API's category: HELMET, BELT...), each with what counts as it
        (its starred version): one of each is the full set, so Monster Raider takes Spider's or Tarantula Boots."""
        by = collections.OrderedDict()
        for piece in sorted(self.sets[category][set_id]):
            worn = self.items[piece].get('category') or piece
            by.setdefault(worn, []).extend([piece] + list(self.data(piece).get('mapped_item_ids') or []))
        return list(by.values())

    def match_sets(self, category, names):
        """Each recorded set's id: first where its pieces' names share its words, or its id's words are its words
        (all of them, so "Miner Armor" goes to the Miner pieces before the Prospecting ones' MINER_OUTFIT gets it);
        then where its id's words are among its words ("Seymour's Special Armor" is SEYMOUR); then where a word starts
        the same ("Vanquisher Set" is VANQUISHED, "Prospector's Outfit" MINER_OUTFIT's Prospecting pieces)."""
        left = list(self.sets[category])
        found = {}
        def prefix(a, b):
            return any(x[:7] == y[:7] and len(x) >= 7 and len(y) >= 7 for x in a for y in b)
        passes = [lambda w, s: self.shared(category, s) == w,
                  lambda w, s: tokens(s) == w,
                  lambda w, s: bool(tokens(s)) and tokens(s) <= w,
                  lambda w, s: bool(w) and (w <= self.shared(category, s) or w <= tokens(s)),
                  lambda w, s: prefix(w, self.shared(category, s) | tokens(s))]
        for test in passes:
            for name in names:
                if name in found:
                    continue
                hit = next((s for s in left if test(tokens(name), s)), None)
                if hit:
                    found[name] = hit
                    left.remove(hit)
        return found, left


def build(found, api):
    rec = recorded(found)
    problems = []
    categories = []
    for cat_id, cat_name in CATEGORIES:
        entries = []
        found_sets, left_sets = api.match_sets(cat_id, [e['name'] for e in rec[cat_name] if e['set']])
        for e in rec[cat_name]:
            if e['set']:
                set_id = found_sets.get(e['name'])
                if set_id is None:
                    problems.append('%s: no set in the API for "%s"' % (cat_name, e['name']))
                    continue
                members = sorted(api.sets[cat_id][set_id])
                api_xp = sorted({api.data(p)['armor_set_donation_xp'][set_id] for p in members})
                parent = next((api.data(p).get('parent', {}).get(set_id) for p in members
                               if api.data(p).get('parent', {}).get(set_id)), None)
                entry = {'id': set_id, 'set': True, 'name': e['name'], 'xp': e['xp'], 'pieces': api.slots(cat_id, set_id)}
            else:
                item = api.singles[cat_id].get(e['name'])
                if item is None:
                    problems.append('%s: no item in the API named "%s"' % (cat_name, e['name']))
                    continue
                data = api.data(item)
                api_xp = [data.get('donation_xp')]
                parent = (data.get('parent') or {}).get(item)
                entry = {'id': item, 'name': e['name'], 'xp': e['xp'], 'items': [item] + list(data.get('mapped_item_ids') or [])}
            if parent and not e['parent_name']:
                # The menu says nothing of it, and the menu is what's kept.
                problems.append('%s: the API has %s count as "%s", the recording doesn\'t (left out)' % (cat_name, parent, e['name']))
            elif parent:
                entry['parent'] = parent
                entry['parent_name'] = e['parent_name']
                entry['parent_lines'] = e['parent_lines']
            elif e['parent_name']:
                problems.append('%s: "%s" names a parent (%s) the API hasn\'t' % (cat_name, e['name'], e['parent_name']))
            if e['xp'] not in api_xp:
                problems.append('%s: "%s" gives %s SkyBlock XP as recorded, %s in the API (the recording\'s is kept)'
                                % (cat_name, e['name'], e['xp'], '/'.join(map(str, api_xp))))
            entries.append(entry)
        for set_id in left_sets:
            problems.append('%s: the API\'s set %s isn\'t in the recording' % (cat_name, set_id))
        recorded_singles = {e['name'] for e in rec[cat_name] if not e['set']}
        for name, item in api.singles[cat_id].items():
            if name not in recorded_singles:
                problems.append('%s: the API\'s %s (%s) isn\'t in the recording' % (cat_name, item, name))
        categories.append({'id': cat_id, 'name': cat_name, 'entries': entries})
    # Parents are entries, of any category (Hardened Diamond Armor's is Mining's Mineral Armor).
    ids = {e['id'] for c in categories for e in c['entries']}
    for c in categories:
        for e in c['entries']:
            if 'parent' in e and e['parent'] not in ids:
                problems.append('%s: %s\'s parent %s isn\'t an entry' % (c['name'], e['id'], e['parent']))
    return {'format': 1, 'source': {'api': api.updated}, 'categories': categories, 'special': api.special}, problems


def with_milestones(data, problems, recorded_milestones):
    """The recorded milestones, one for every 100 XP from 100 (the menu's 40); what's missing or odd is a problem."""
    data['milestones'] = recorded_milestones
    numbers = [m['milestone'] for m in recorded_milestones]
    if numbers != list(range(1, len(numbers) + 1)):
        problems.append('Milestones: the recording has %s, not 1 on' % numbers)
    for m in recorded_milestones:
        if m['xp'] != 100 * m['milestone']:
            problems.append('Milestones: %d takes %s XP, not %d' % (m['milestone'], m['xp'], 100 * m['milestone']))
    if not recorded_milestones:
        problems.append('Milestones: the recording never opened Museum Milestones')
    return data


def report(data, problems, recording):
    lines = ['# Museum data report', '', 'From the recording %s and the items API (lastUpdated %s).' % (recording, data['source']['api']), '']
    total = 0
    for c in data['categories']:
        xp = sum(e['xp'] for e in c['entries'])
        total += xp
        sets = sum(1 for e in c['entries'] if e.get('set'))
        lines.append('- %s: %d entries (%d sets), %d SkyBlock XP' % (c['name'], len(c['entries']), sets, xp))
    lines += ['- All: %d entries, %d SkyBlock XP' % (sum(len(c['entries']) for c in data['categories']), total),
              '- Special Items the API marks: %d' % len(data['special']),
              '- Milestones: %d' % len(data.get('milestones', [])), '', '## Problems', '']
    lines += ['- ' + p for p in problems] or ['None.']
    return '\n'.join(lines) + '\n'


def main():
    ap = argparse.ArgumentParser(description='Build museum.json (format 1) from a recording of the Museum menus and '
                                             'the Hypixel items API')
    ap.add_argument('--gui', required=True, help='the recording\'s gui.txt (tools/replay decode)')
    ap.add_argument('--api', default=API_URL, help='the items API response: a file, or a URL (default: fetch it)')
    ap.add_argument('--out', default=None, help='where the files go (default: museum/ of the private data checkout)')
    args = ap.parse_args()
    found = menus(args.gui)
    data, problems = build(found, Api(load_api(args.api)))
    with_milestones(data, problems, milestones(args.gui))
    data['source']['recording'] = os.path.basename(os.path.dirname(os.path.abspath(args.gui)))
    out = args.out or os.path.join(os.path.dirname(ROOT), 'skyblock-dungeon-data', 'museum')
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, 'museum.json'), 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=1, ensure_ascii=False)
        f.write('\n')
    with open(os.path.join(out, 'report.md'), 'w', encoding='utf-8') as f:
        f.write(report(data, problems, data['source']['recording']))
    entries = sum(len(c['entries']) for c in data['categories'])
    print('museum.json: %d entries in %d categories, %d Special Items, %d milestones; %d problems (report.md)'
          % (entries, len(data['categories']), len(data['special']), len(data['milestones']), len(problems)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
