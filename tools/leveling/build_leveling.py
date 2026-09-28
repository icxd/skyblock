#!/usr/bin/env python3
# python3 tools/leveling/build_leveling.py --gui GUI.TXT [--raw WINDOWS.JSON] [--wiki DIR] [--save-wiki DIR]
#                                          [--neu NEU-REPO] [--collections PATH_OR_URL] [--out DIR] [--paper-api JAR]
#
# Builds the plugin's SkyBlock Leveling data, leveling.json (format 1, see README.md), and report.md next to it:
# the XP tasks by category and their XP, the rewards of each level, the emblems and the SkyBlock Guide's stages.
# The tasks, emblems and guide come from the Hypixel SkyBlock wiki; the categories, their totals and the rewards
# lists from a recording of Hypixel's own menus (a replay decoded with tools/replay: its gui.txt, and the raw
# window packets for the heads' skins). All of it is Hypixel's, so the output goes to the private data repository,
# never into this one.
import argparse
import collections
import json
import os
import re
import subprocess
import sys
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
sys.path.insert(0, os.path.join(ROOT, 'tools', 'items'))
import build_items  # noqa: E402  (the plugin's text widths, wrapping and materials)

WIKI = 'https://hypixelskyblock.minecraft.wiki'
PAGES = ['SkyBlock Levels', 'SkyBlock Levels/Tasks', 'SkyBlock Levels/UI/Leveling', 'SkyBlock Guide'] + [
    'SkyBlock Guide/Tasks/' + s for s in ('Starter', 'Amateur', 'Intermediate', 'Skilled', 'Expert', 'Professional', 'Master')]
COLLECTIONS_URL = 'https://api.hypixel.net/v2/resources/skyblock/collections'
# How wide Hypixel lets these menus' generated lines get (the guide's "Reach 50% completion of Intermediate" fits at
# 188 pixels, "... of Professional" at 190 doesn't): the plugin wraps descriptions the same way.
MENU_WIDTH = 188

# The wiki's colour templates and their codes.
COLOR_TEMPLATES = {
    'black': '0', 'darkblue': '1', 'darkgreen': '2', 'darkaqua': '3', 'turquoise': '3', 'darkred': '4', 'darkpurple': '5',
    'purple': '5', 'gold': '6', 'gray': '7', 'grey': '7', 'g': 'a', 'darkgray': '8', 'dg': '8', 'blue': '9', 'green': 'a',
    'aqua': 'b', 'red': 'c', 'pink': 'd', 'lightpurple': 'd', 'yellow': 'e', 'white': 'f',
    'common': 'f', 'uncommon': 'a', 'rare': '9', 'epic': '5', 'legendary': '6', 'mythic': 'd', 'divine': 'b', 'special': 'c',
}
# {{Stat|mf}} and the like, as the plugin's stats are written.
STAT_NAMES = {'hp': '&c❤ Health', 'health': '&c❤ Health', 'str': '&c❁ Strength', 'strength': '&c❁ Strength',
              'mf': '&b✯ Magic Find', 'bp': '&2Ⓟ Breaking Power', 'def': '&a❈ Defense', 'int': '&b✎ Intelligence'}
ROMAN = {'I': 1, 'V': 5, 'X': 10, 'L': 50, 'C': 100, 'D': 500, 'M': 1000}


def roman(s):
    """"XXIV" as 24; None if it isn't a Roman numeral."""
    if not s or any(c not in ROMAN for c in s):
        return None
    total = 0
    for i, c in enumerate(s):
        v = ROMAN[c]
        total += -v if i + 1 < len(s) and ROMAN[s[i + 1]] > v else v
    return total


def snake(name):
    """"Skill Level Up" as "skill_level_up": a task's id."""
    return re.sub(r'[^a-z0-9]+', '_', strip_codes(name).lower().replace("'", '')).strip('_')


def strip_codes(s):
    return re.sub(r'[&§][0-9a-fk-or]', '', s)


# ---------- the wiki ----------

def fetch_wiki(title):
    """A page's wikitext and revision id, from the wiki's API."""
    query = urllib.parse.urlencode({'action': 'query', 'prop': 'revisions', 'rvprop': 'content|ids', 'rvslots': 'main',
                                    'titles': title, 'format': 'json', 'redirects': 1})
    request = urllib.request.Request(WIKI + '/api.php?' + query, headers={'User-Agent': 'skyblock-leveling-builder'})
    with urllib.request.urlopen(request, timeout=120) as response:
        data = json.loads(response.read())
    page = next(iter(data['query']['pages'].values()))
    revision = page['revisions'][0]
    return revision['slots']['main']['*'], revision['revid']


def load_wiki(directory, save):
    """Every page this needs: from saved copies (directory/<title with / as __>.json) or the wiki itself."""
    pages = {}
    for title in PAGES:
        name = title.replace('/', '__') + '.json'
        if directory:
            with open(os.path.join(directory, name), encoding='utf-8') as f:
                saved = json.load(f)
            pages[title] = (saved['text'], saved['revid'])
        else:
            pages[title] = fetch_wiki(title)
        if save:
            os.makedirs(save, exist_ok=True)
            with open(os.path.join(save, name), 'w', encoding='utf-8') as f:
                json.dump({'title': title, 'revid': pages[title][1], 'text': pages[title][0]}, f, ensure_ascii=False)
    return pages


def split_top(s, sep):
    """s split at sep where it isn't inside a {{template}} or [[link]]."""
    parts, depth, start, i = [], 0, 0, 0
    while i < len(s):
        two = s[i:i + 2]
        if two in ('{{', '[['):
            depth += 1
            i += 2
            continue
        if two in ('}}', ']]'):
            depth = max(0, depth - 1)
            i += 2
            continue
        if depth == 0 and s.startswith(sep, i):
            parts.append(s[start:i])
            i += len(sep)
            start = i
            continue
        i += 1
    parts.append(s[start:])
    return parts


def template_args(inner):
    """{{Name|a|b=c}}'s inner text as (name, positional args, named args)."""
    parts = split_top(inner, '|')
    positional, named = [], {}
    for p in parts[1:]:
        m = re.match(r'^\s*([A-Za-z_]+)\s*=(.*)$', p, flags=re.S)
        if m:
            named[m.group(1).lower()] = m.group(2).strip()
        else:
            positional.append(p.strip())
    return parts[0].strip(), positional, named


def text(markup, color='7'):
    """Wiki markup as the plugin writes text, & colour codes included; color is the text's own colour (restored
    after a coloured part)."""
    s = re.sub(r'<!--.*?-->', '', markup, flags=re.S)
    s = re.sub(r'<br\s*/?>', '\n', s)
    s = re.sub(r'</?(?:span|small|nowiki|sup|sub|b|i|u)[^>]*>', '', s)
    s = re.sub(r"'''?", '', s)
    out, i = [], 0
    while i < len(s):
        if s.startswith('{{', i):
            depth, j = 0, i
            while j < len(s):
                if s.startswith('{{', j):
                    depth += 1
                    j += 2
                elif s.startswith('}}', j):
                    depth -= 1
                    j += 2
                    if depth == 0:
                        break
                else:
                    j += 1
            out.append(template(s[i + 2:j - 2], color))
            i = j
        elif s.startswith('[[', i):
            j = s.find(']]', i)
            j = len(s) if j < 0 else j
            inner = s[i + 2:j]
            if inner.lower().startswith(('file:', 'category:')):
                out.append('')
            else:
                out.append(text(split_top(inner, '|')[-1], color))
            i = j + 2
        else:
            out.append(s[i])
            i += 1
    return re.sub(r'[ \t]+', ' ', ''.join(out)).strip()


def template(inner, color):
    name, args, named = template_args(inner)
    key = name.lower().replace(' ', '').replace('_', '')
    if key in COLOR_TEMPLATES:
        # {{Green|text}}; {{Epic}} alone is the rarity's name.
        shown = text(args[0], COLOR_TEMPLATES[key]) if args else name.title()
        return '&' + COLOR_TEMPLATES[key] + shown + '&' + color
    if key == 'skyblockxp':
        return (args[0] + ' ' if args else '') + 'SkyBlock XP'
    if key in ('skl', 'skill'):
        return ' '.join(a for a in args[:2] if a)
    if key in ('coll', 'mobtype', 'stier', 'rmt', 'rn', 'rankname'):
        return args[-1] if args else ''
    if key == 'skyblocklevel':
        return 'SkyBlock Level ' + args[0] if args else 'SkyBlock Level'
    if key == 'stat':
        stat = STAT_NAMES.get(args[0].lower(), args[0]) if args else ''
        return (args[1] + ' ' if len(args) > 1 else '') + stat + '&' + color
    if key == 'chocolate':
        return (args[0] + ' ' if args else '') + named.get('alt', 'Chocolate')
    if key in ('c', 'coins'):
        return (args[0] + ' ' if args else '') + 'Coins'
    if key in ('npcsprite', 'plainlist', 'mctxt'):
        return text(args[-1], color) if args else ''
    return text(args[0], color) if args else ''


class Cell:
    def __init__(self, content, rowspan=1, colspan=1, header=False):
        self.content, self.rowspan, self.colspan, self.header = content, rowspan, colspan, header


def parse_cell(raw, header):
    """"rowspan="4" colspan="2" | Skill Level Up" as a Cell."""
    parts = split_top(raw, '|')
    attrs, content = ('', parts[0]) if len(parts) == 1 else (parts[0], '|'.join(parts[1:]))
    if not re.search(r'\b(?:rowspan|colspan|class|style|align)\s*=', attrs):
        attrs, content = '', raw
    rowspan = re.search(r'rowspan\s*=\s*"?(\d+)', attrs)
    colspan = re.search(r'colspan\s*=\s*"?(\d+)', attrs)
    return Cell(content.strip(), int(rowspan.group(1)) if rowspan else 1, int(colspan.group(1)) if colspan else 1, header)


def tables(wikitext):
    """Every {| ... |} table as a grid: rows of cells, a rowspan's or colspan's cell repeated where it reaches (the
    same object, so a task's rows can be told apart from the next task's)."""
    out = []
    for body in re.findall(r'\{\|(.*?)\n\|\}', wikitext, flags=re.S):
        rows, current = [], None
        for line in body.split('\n')[1:]:
            if line.startswith('|-'):
                current = []
                rows.append(current)
                continue
            if not line.startswith(('|', '!')) or line.startswith('|}') or line.startswith('|+'):
                if current and line.strip() and current[-1] is not None:
                    current[-1].content += '\n' + line
                continue
            if current is None:
                current = []
                rows.append(current)
            header = line.startswith('!')
            for raw in split_top(line[1:], '!!' if header else '||'):
                current.append(parse_cell(raw, header))
        grid, pending = [], {}
        for row in rows:
            if not row:
                continue
            placed, col, cells = {}, 0, list(row)
            while cells or any(c >= col for c in pending):
                if col in pending:
                    cell, left = pending[col]
                    placed[col] = cell
                    if left > 1:
                        pending[col] = (cell, left - 1)
                    else:
                        del pending[col]
                    col += 1
                    continue
                if not cells:
                    col += 1
                    if col > 64:
                        break
                    continue
                cell = cells.pop(0)
                for k in range(cell.colspan):
                    placed[col + k] = cell
                    if cell.rowspan > 1:
                        pending[col + k] = (cell, cell.rowspan - 1)
                col += cell.colspan
            grid.append([placed[c] for c in sorted(placed)])
        out.append(grid)
    return out


def tabs(wikitext):
    """<tabber> tabs as (name, wikitext)."""
    parts = re.split(r'^\|-\|\s*(.+?)\s*=\s*$', wikitext, flags=re.M)
    return [(parts[i], parts[i + 1]) for i in range(1, len(parts), 2)]


def xp_of(markup):
    """The XP a cell gives: 5 for "{{SkyBlock XP|+5|short=y}}" (None if it has none)."""
    m = re.search(r'\{\{SkyBlock XP\|\+?([0-9,]+)', markup)
    return int(m.group(1).replace(',', '')) if m else None


def image_of(markup):
    m = re.search(r'\[\[File:([^|\]]+?)\.(?:png|gif|jpg|webp)', markup, flags=re.I)
    return m.group(1).strip() if m else None


# ---------- materials ----------

# Wiki image names that aren't a material's name.
IMAGE_MATERIALS = {
    'Block of Gold': 'GOLD_BLOCK', 'Block of Iron': 'IRON_BLOCK', 'Block of Diamond': 'DIAMOND_BLOCK',
    'Block of Emerald': 'EMERALD_BLOCK', "Bottle o' Enchanting": 'EXPERIENCE_BOTTLE', 'Empty Map': 'MAP',
    'Book and Quill': 'WRITABLE_BOOK', 'Raw Mutton': 'MUTTON', 'Gold Horse Armor': 'GOLDEN_HORSE_ARMOR',
    'Redstone': 'REDSTONE', 'Dandelion Yellow': 'YELLOW_DYE', 'Cactus Green': 'GREEN_DYE', 'Rose Red': 'RED_DYE',
    'Lapis Lazuli': 'LAPIS_LAZULI', 'Raw Chicken': 'CHICKEN', 'Raw Rabbit': 'RABBIT', 'Clock': 'CLOCK',
    'Enchanted Book': 'ENCHANTED_BOOK', 'Sign': 'OAK_SIGN', 'Map': 'FILLED_MAP', 'Golden Sword': 'GOLDEN_SWORD',
    'Golden Apple': 'GOLDEN_APPLE', 'Golden Chestplate': 'GOLDEN_CHESTPLATE', 'Spawner': 'SPAWNER',
    'Water Bottle': 'POTION', 'Leather Tunic': 'LEATHER_CHESTPLATE',
}


class Materials:
    def __init__(self, jar):
        self.names = build_items.item_materials(jar)

    def of(self, image):
        """A wiki image as a material; None if it's no item (a head, a pet, an icon)."""
        if not image:
            return None
        name = IMAGE_MATERIALS.get(image) or re.sub(r'[^A-Z0-9]+', '_', image.upper()).strip('_')
        return name if name in self.names else None


# ---------- the recording ----------

# Hypixel's private-use glyphs (its resource pack's) as the classic symbols the plugin shows: tools/items' table.
GLYPHS = {chr(int(cp, 16)): symbol for cp, symbol, *_ in build_items.tsv('glyphs.tsv')}


def glyphs(line):
    """A recorded line with its glyphs as the plugin's symbols ("&c\ue010 Health" as "&c❤ Health"); a glyph with no
    known symbol is dropped."""
    return build_items.PUA.sub(lambda m: GLYPHS.get(m.group(0), ''), line)


def menus(path):
    """A decoded gui.txt's menus, in order: (title, {slot: (material, name, lore)}) for each "contents of" block."""
    out, current, item = [], None, None
    with open(path, encoding='utf-8') as f:
        for raw in f:
            line = raw.rstrip('\n')
            m = re.match(r'^--- \S+ contents of "(.*)"$', line)
            if m:
                current = {}
                out.append((m.group(1), current))
                item = None
                continue
            if line.startswith(('===', '---')) or (not line.startswith(' ') and line.strip()):
                current, item = None, None
                continue
            if current is None:
                continue
            m = re.match(r'^  \[(\d+)\] (\S+) x\d+ ?(.*)$', line)
            if m:
                item = (m.group(2).upper(), glyphs(m.group(3)), [])
                current[int(m.group(1))] = item
                continue
            if item is not None and line.startswith('      '):
                item[2].append(glyphs(line[6:]))
            elif not line.strip():
                item = None
    return out


def last_menu(recorded, title, test=None):
    found = None
    for t, slots in recorded:
        if t == title and (test is None or test(slots)):
            found = slots
    return found


def textures(path):
    """Player head skins in raw window packets (tools/replay's raw ... open_window window_items), by the
    item's name without colours."""
    out = {}
    if not path:
        return out
    import base64
    with open(path, encoding='utf-8') as f:
        for line in f:
            parts = line.split(' ', 2)
            if len(parts) < 3 or parts[1] != 'window_items':
                continue
            for item in json.loads(parts[2]).get('items', []):
                name, skin = None, None
                for c in (item or {}).get('components', []) or []:
                    if c['type'] == 'custom_name':
                        name = ''.join(re.findall(r'"text": \{"type": "string", "value": "([^"]*)"', json.dumps(c['data'], ensure_ascii=False)))
                    if c['type'] == 'profile':
                        for p in c['data'].get('properties', []):
                            if p['name'] == 'textures':
                                value = json.loads(base64.b64decode(p['value'] + '=' * (-len(p['value']) % 4)))
                                skin = value['textures']['SKIN']['url'].split('/')[-1]
                if name and skin:
                    out.setdefault(strip_codes(name), skin)
    return out


def icon(material, name, skins):
    """{"material", "texture"} for an item; a head's skin by its name."""
    out = {'material': material}
    if material == 'PLAYER_HEAD' and strip_codes(name) in skins:
        out['texture'] = skins[strip_codes(name)]
    return out


# ---------- categories and tasks ----------

# The category items' slots in the recorded Ways to Level Up, in order, and the wiki tab each one's tasks are on.
CATEGORY_SLOTS = [11, 12, 13, 14, 15, 20, 21, 23, 24]
CATEGORY_TABS = {'core': 'Core', 'event': 'Event', 'dungeon': 'Dungeon', 'essence_shop': 'Essence Shop',
                 'slaying': 'Slaying', 'skill_related': 'Skill Related', 'miscellaneous': 'Miscellaneous', 'story': 'Story',
                 'consumables': 'Consumables'}
# Recorded task names the wiki calls something else.
TASK_ALIASES = {'museum donations': 'museum progression', 'mythological kills': 'mythological creatures',
                'harvest feast perk shop': 'harvest feast shop', 'starlyn sister levels': 'starlyn sisters levels',
                'refined jyrre': 'refined bottle of jyrre', 'refined dark cacao': 'refined dark cacao truffle',
                'moby-duck': "moby-duck: collector's edition", 'core of the mountain': 'peak of the mountain',
                'frozen corpse milestones': 'corpse milestones', 'community shop upgrades': 'community shop',
                'reaper peppers': 'reaper pepper'}
# The plugin's floors, by the wiki's names for them.
FLOOR_NAMES = ['Entrance', 'Floor I', 'Floor II', 'Floor III', 'Floor IV', 'Floor V', 'Floor VI', 'Floor VII']


def category_from_item(material, name, lore, skins):
    """A recorded Ways to Level Up item: its name, icon, description, task names and the XP it's out of."""
    lines = list(lore)
    # "&89 Tasks", "", the description, "", "&e▶ ..." lines, "", progress, bar, "", click.
    description, tasks, maximum, i = [], [], None, 2
    while i < len(lines) and lines[i].strip():
        description.append(lines[i])
        i += 1
    for line in lines[i:]:
        if line.startswith('&e▶ '):
            tasks.append(line[len('&e▶ '):])
        m = re.search(r'&6/&e([0-9,]+) XP', line)
        if m:
            maximum = int(m.group(1).replace(',', ''))
    cid = snake(name).removesuffix('_tasks')
    return {'id': cid, 'name': name, 'icon': icon(material, name, skins), 'description': description,
            'max': maximum, 'recorded': tasks}


# Skill Related's first name column only groups its tasks by skill.
SKILL_GROUPS = {'Mining', 'Farming', 'Fishing', 'Foraging'}


def xp_line(cell):
    """An XP cell as a line: "Level 1-10" and 5 (and the levels it's for), or "Per 5 Fairy Souls" and 10, with what
    follows the number ("(Caps at 50 runs!)") as its note."""
    content = cell.content
    xp = xp_of(content)
    parts = re.split(r'\{\{SkyBlock XP[^}]*\}\}', content, maxsplit=1)
    label = text(parts[0]).rstrip(': ').strip()
    note = text(parts[1]).strip() if len(parts) > 1 else ''
    line = {'text': label}
    if xp is not None:
        line['xp'] = xp
    if note:
        line['note'] = note
    span = re.match(r'^Level (\d+)(?:-(\d+))?$', label)
    if span:
        line['from'] = int(span.group(1))
        line['to'] = int(span.group(2) or span.group(1))
    return line


def wiki_tasks(tab_text, problems):
    """A Tasks tab's tasks, in the wiki's order: name, description, image, XP lines, the most XP and sub-tasks (a
    sub-task can have its own: Defeat Slayers, Defeat Revenant Horror, Tier I)."""
    tasks = []
    for grid in tables(tab_text):
        if not grid or not any(c.header for c in grid[0]):
            continue
        # Image | Name (1-4 columns) | Description | XP | Max
        columns = len(grid[0])
        names = columns - 4
        nodes, maxes = {}, collections.defaultdict(dict)
        for row in grid[1:]:
            if len(row) < columns:
                problems.append(f'a short row in the tasks table: {[c.content[:30] for c in row]}')
                continue
            name_cells = row[1:1 + names]
            if names > 1 and text(name_cells[0].content) in SKILL_GROUPS and name_cells[1] is not name_cells[0]:
                name_cells = name_cells[1:]
            # The distinct name cells, outermost first: the task, then any groups, the last one the row's own.
            chain = []
            for c in name_cells:
                if not chain or c is not chain[-1]:
                    chain.append(c)
            parent = None
            for depth, cell in enumerate(chain):
                node = nodes.get(id(cell))
                if node is None:
                    node = {'name': text(cell.content), 'image': image_of(row[0].content), 'xp': [], 'tasks': []}
                    if depth == 0:
                        node['description'] = text(row[1 + names].content)
                        tasks.append(node)
                    else:
                        parent['tasks'].append(node)
                    nodes[id(cell)] = node
                parent = node
            line = xp_line(row[2 + names])
            if line.get('xp') is not None or line['text']:
                if line not in parent['xp']:
                    parent['xp'].append(line)
            maximum = xp_of(row[3 + names].content)
            if maximum is not None:
                # A max cell covers the rows it spans, for the node the rows belong to at the level it's drawn.
                owner = nodes[id(chain[0])] if row[3 + names].rowspan > 1 or len(chain) == 1 else parent
                maxes[id(owner)][id(row[3 + names])] = maximum
        for node in list(walk(tasks)):
            node['max'] = sum(maxes[id(node)].values()) if maxes.get(id(node)) else None
    for task in tasks:
        fill_max(task)
    return tasks


def walk(tasks):
    for t in tasks:
        yield t
        yield from walk(t.get('tasks', []))


def fill_max(task):
    """A task's most XP: its own cell, else what its parts add up to."""
    for sub in task.get('tasks', []):
        fill_max(sub)
    if task.get('max') is None:
        own = sum(x.get('xp', 0) for x in task.get('xp', []))
        parts = sum(s.get('max') or 0 for s in task.get('tasks', []))
        task['max'] = own + parts


def match_tasks(recorded, wiki, problems, category):
    """The recorded tasks (the menu's list, which is Hypixel's today) with the wiki's details where it has them."""
    by_name = {}
    for t in wiki:
        by_name.setdefault(strip_codes(t['name']).lower(), t)
    out = []
    for name in recorded:
        key = name.lower()
        t = by_name.get(key) or by_name.get(TASK_ALIASES.get(key, key))
        if t is None:
            problems.append(f'{category}: "{name}" is in the recorded menu but not on the wiki (no XP for it)')
            out.append({'id': snake(name), 'name': name, 'xp': [], 'max': 0, 'tasks': []})
            continue
        entry = dict(t)
        entry['id'] = snake(name)
        entry['name'] = name
        out.append(entry)
    missing = [t['name'] for t in wiki if all(strip_codes(t['name']).lower() not in (n.lower(), TASK_ALIASES.get(n.lower())) for n in recorded)]
    for name in missing:
        problems.append(f'{category}: the wiki has "{name}", which the recorded menu doesn\'t list (left out)')
    return out


def finish_task(task, materials, fallback, path=()):
    """Ids, icons and the plugin's own keys (a floor for each dungeon completion) on a task and its parts."""
    task.setdefault('id', snake(task['name']))
    material = materials.of(task.pop('image', None))
    task['icon'] = {'material': material} if material else dict(fallback)
    parents = path + (task['name'],)
    if 'complete dungeons' in ' '.join(p.lower() for p in parents) or task['name'].startswith('Complete Catacombs'):
        m = re.match(r'^Complete Catacombs (Entrance|Floor [IVX]+)$', task['name'])
        if m:
            number = FLOOR_NAMES.index(m.group(1))
            master = any('master' in p.lower() for p in parents)
            task['floor'] = ('MASTER_FLOOR_' if master else 'FLOOR_') + str(number) if number else 'ENTRANCE'
            if master and not number:
                task.pop('floor')
    for i, sub in enumerate(task.get('tasks', [])):
        sub['id'] = task['id'] + '.' + snake(sub['name'])
        finish_task(sub, materials, task['icon'], parents)
    if not task.get('tasks'):
        task.pop('tasks', None)
    if not task.get('xp'):
        task.pop('xp', None)
    if not task.get('description'):
        task.pop('description', None)


# ---------- rewards ----------

REWARD_KINDS = {'NETHER_STAR': 'FEATURE', 'NAME_TAG': 'EMBLEM', 'BOOK': 'BONUS'}


def paragraphs(lines):
    """Lore lines as the paragraphs they were wrapped from: blank lines split them, and a line's leading colour that
    only carried the one before it over the break is dropped (the plugin wraps them again)."""
    out, current = [], ''
    for line in lines + ['']:
        if not line.strip():
            if current:
                out.append(current)
            current = ''
            continue
        if current:
            carry = build_items.format_at_end(current)
            current += ' ' + (line[len(carry):] if carry and line.startswith(carry) else line)
        else:
            current = line
    return out


def bonus_number(paragraph):
    """A bonus's sentence with the recorded level's number (0.9 at level 88: 0.01% a level) as {bonus}."""
    masked = re.sub(r'[&§][0-9a-fk-or]', '\x00\x00', paragraph)
    m = re.search(r'(\d+(?:\.\d+)?)%', masked)
    if not m or not paragraph.startswith('&7When'):
        return paragraph
    return paragraph[:m.start(1)] + '{bonus}' + paragraph[m.end(1):]


def rewards(recorded, skins, problems):
    """Every level's reward but its stats, from the four recorded Rewards menus (features, prefix colours, emblems,
    bonuses): each item's level, name, icon and what it says."""
    out, kinds = [], set()
    for title, slots in recorded:
        if title != 'Rewards' or 4 not in slots:
            continue
        head = slots[4][0]
        kind = REWARD_KINDS.get(head, 'PREFIX' if head.endswith('_DYE') or head in ('BONE_MEAL', 'LAPIS_LAZULI', 'REDSTONE') else None)
        if kind is None or kind in kinds:
            continue
        kinds.add(kind)
        for slot in sorted(slots):
            if slot == 4 or slot >= 45:
                continue
            material, name, lore = slots[slot]
            if not lore or not re.match(r'^&8Level \d+$', lore[0]):
                continue
            entry = {'kind': kind, 'level': int(lore[0][len('&8Level '):]), 'name': name, 'icon': icon(material, name, skins)}
            body = []
            for line in lore[2:]:
                if line.startswith(('&a&lUNLOCKED', '&7Levels left', '&7Progress to Unlock')) or line.startswith('&3&l&m') or line.startswith('&f&l&m'):
                    break
                body.append(line)
            while body and not body[-1].strip():
                body.pop()
            if kind == 'PREFIX':
                m = re.search(r'&8\[&([0-9a-f])\d+&8\]', ' '.join(body))
                entry['color'] = m.group(1) if m else None
                body = []
            if kind == 'EMBLEM':
                entry['emblem'] = snake(re.sub(r' Emblem .*$', '', name))
            if kind == 'BONUS' and body:
                entry['text'] = [bonus_number(p) for p in paragraphs(body)]
                body = []
            if body:
                entry['lore'] = body
            out.append(entry)
    for kind in ('FEATURE', 'PREFIX', 'EMBLEM', 'BONUS'):
        if kind not in kinds:
            problems.append(f'no recorded {kind} rewards menu')
    return out


# ---------- emblems ----------

SKILLS = ['Combat', 'Farming', 'Fishing', 'Mining', 'Foraging', 'Enchanting', 'Alchemy', 'Carpentry', 'Runecrafting',
          'Taming', 'Social', 'Hunting']
CLASSES = ['Healer', 'Mage', 'Berserk', 'Archer', 'Tank']
# Emblems chat shows otherwise than the wiki: real chat logs have "§7⚔ " and "§6⚔ " (not bold) before names.
SYMBOL_FIXES = {'Catacombs Swords': '&7⚔', 'Golden Catacombs Swords': '&6⚔', 'Healer Staff': '&7⚚', 'Healer Master': '&6⚚'}


def unlock_of(requirement):
    """What unlocks an emblem, where the plugin can tell: a skill's level, Catacombs, a class, a floor's
    completion, the class average or the SkyBlock level. None for the rest."""
    m = re.search(r'\{\{Skl\|([A-Za-z]+)\|([IVXLC]+)\}\}', requirement)
    if m:
        level = roman(m.group(2))
        if m.group(1) == 'Catacombs':
            return {'catacombs': level}
        if m.group(1) in SKILLS:
            return {'skill': m.group(1).upper(), 'level': level}
        return None
    m = re.search(r'\[\[(' + '|'.join(CLASSES) + r')\]\] ([IVXLC]+)', requirement)
    if m:
        return {'class': m.group(1).upper(), 'level': roman(m.group(2))}
    m = re.search(r'\[\[The Catacombs - Floor ([IVX]+)(\|Master Mode[^\]]*)?\]\] Completion', requirement)
    if m:
        return {'floor': ('MASTER_FLOOR_' if m.group(2) else 'FLOOR_') + str(roman(m.group(1)))}
    m = re.search(r'Class Average ([IVXLC]+)', requirement)
    if m:
        return {'classAverage': roman(m.group(1))}
    m = re.search(r'\{\{SkyBlock Level\|(\d+)\}\}', requirement)
    if m:
        return {'level': int(m.group(1))}
    return None


def emblems(levels_page, recorded, skins, problems):
    """The emblem categories as the recorded Emblems menu shows them, each with the wiki's emblems."""
    menu = last_menu(recorded, 'Emblems')
    body = levels_page[levels_page.find('== Emblems =='):]
    body = body[:body.find('</tabber>')]
    lists = {}
    for name, tab in tabs(body):
        grid = tables(tab)[0]
        entries = []
        for row in grid[1:]:
            symbol = re.search(r'\{\{Rmt\|(.*?)\}\}', row[0].content)
            ename = text(row[1].content)
            entry = {'id': snake(ename), 'name': ename, 'symbol': SYMBOL_FIXES.get(ename, symbol.group(1).strip() if symbol else ''),
                     'requirement': strip_codes(text(row[2].content))}
            unlock = unlock_of(row[2].content)
            if unlock:
                entry['unlock'] = unlock
            if not strip_codes(entry['symbol']).strip():
                problems.append(f'emblem {ename}: no symbol on the wiki')
            entries.append(entry)
        lists[name.strip()] = entries
    out = []
    for slot in sorted(menu or {}):
        material, name, lore = menu[slot]
        if slot >= 27 or not strip_codes(name).strip() or material.endswith('GLASS_PANE'):
            continue
        cname = strip_codes(name)
        description = [l for l in lore[2:] if l.strip() and not l.startswith('&eClick')]
        entries = lists.pop(cname, None)
        if entries is None:
            problems.append(f'emblem category {cname}: not on the wiki')
            entries = []
        out.append({'id': snake(cname), 'name': name, 'icon': icon(material, name, skins), 'description': description,
                    'emblems': entries})
    for name in lists:
        problems.append(f'the wiki\'s emblem category {name} isn\'t in the recorded menu (left out)')
    return out


# ---------- the guide ----------

STAGES = ['Starter', 'Amateur', 'Intermediate', 'Skilled', 'Expert', 'Professional', 'Master']
# Recorded group names the wiki calls something else.
GROUP_ALIASES = {'revenant horror tier ii': 'revenant horror ii', '[lv300] arachne': 'arachne', 'zombie lvl 1': 'zombie lvl 1'}
# How many of a group's parts its item lists, where the recording doesn't say (the recorded ones list up to 11, but
# not a 10-part Abiphone Contacts): UNKNOWN, taken as up to 10.
LISTED = 10


def unlock_of_text(text, collections_by_name):
    """What completes a guide part, from its name alone ("Farming Skill XII", "Healer IX", "Carrot III")."""
    m = re.match(r'^(\w+) Skill ([IVXLC]+)$', text)
    if m and m.group(1) in SKILLS:
        return {'skill': m.group(1).upper(), 'level': roman(m.group(2))}
    m = re.match(r'^(' + '|'.join(CLASSES) + r') ([IVXLC]+)$', text)
    if m:
        return {'class': m.group(1).upper(), 'level': roman(m.group(2))}
    return None


def guide_items(markup, task, collections_by_name, problems, group):
    """A wiki guide group's listed parts, each with what completes it where the plugin can tell."""
    items = []
    for raw in re.findall(r'^\* (.+)$', markup, flags=re.M):
        entry = {'text': strip_codes(text(raw))}
        unlock = None
        m = re.search(r'\{\{Skl\|([A-Za-z]+)\|([IVXLC]+)\}\}', raw)
        if m and m.group(1) in SKILLS:
            unlock = {'skill': m.group(1).upper(), 'level': roman(m.group(2))}
            entry['text'] = f'{m.group(1)} Skill {m.group(2)}'
        m = re.search(r'\{\{Coll\|(.+?) ([IVXLC]+)\}\}', raw)
        if m and task == 'Collections':
            # The wiki's "Oak Wood" is the API's "Oak Log".
            cid = collections_by_name.get(m.group(1).lower()) or collections_by_name.get(re.sub(r' wood$', ' log', m.group(1).lower()))
            if cid:
                unlock = {'collection': cid, 'tier': roman(m.group(2))}
            else:
                problems.append(f'guide {group}: no collection called {m.group(1)}')
        m = re.match(r'^\[\[(' + '|'.join(CLASSES) + r')\]\] ([IVXLC]+)$', raw.strip())
        if m:
            unlock = {'class': m.group(1).upper(), 'level': roman(m.group(2))}
        m = re.search(r'\[\[The Catacombs - Floor ([IVX]+)\|(Master )?Catacombs Floor [IVX]+\]\]', raw)
        if m:
            unlock = {'floor': ('MASTER_FLOOR_' if m.group(2) else 'FLOOR_') + str(roman(m.group(1)))}
        if unlock:
            entry['unlock'] = unlock
        items.append(entry)
    return items


def group_unlock(name):
    """A group with nothing listed that the plugin can tell by its name: "Catacombs Level XII", "Catacombs Floor V"."""
    m = re.match(r'^Catacombs Level ([IVXLC]+)$', name)
    if m:
        return {'catacombs': roman(m.group(1))}
    m = re.match(r'^(Master )?Catacombs Floor ([IVX]+)$', name)
    if m:
        return {'floor': ('MASTER_FLOOR_' if m.group(1) else 'FLOOR_') + str(roman(m.group(2)))}
    return None


def stage_table(page):
    """The wiki's SkyBlock Guide stages: each one's subtitle, description and how many tasks it has."""
    out = {}
    grids = tables(page)
    for row in (grids[0][1:] if grids else []):
        if len(row) < 3:
            continue
        parts = re.split(r'<br\s*/?>', row[1].content, maxsplit=1)
        tasks = re.sub(r'[^0-9]', '', row[2].content)
        out[strip_codes(text(row[0].content))] = {'subtitle': '&8' + strip_codes(text(parts[0])),
                                                  'description': text(parts[1]) if len(parts) > 1 else '',
                                                  'tasks': int(tasks) if tasks else 0}
    return out


def wiki_groups(page, materials, collections_by_name, task_icons, problems):
    """A stage's groups as the wiki lists them (older than the recording where there is one)."""
    groups = []
    grids = tables(page)
    for row in (grids[0][1:] if grids else []):
        if len(row) < 5:
            continue
        name = strip_codes(text(row[1].content)).replace('Catacobs', 'Catacombs')
        task = strip_codes(text(row[2].content))
        entry = {'name': name, 'task': snake(task), 'description': text(re.split(r'\{\{Plainlist', row[3].content)[0]),
                 'xp': xp_of(row[4].content.replace('short=t|', '')),
                 'items': guide_items(row[3].content, task, collections_by_name, problems, name)}
        material = materials.of(image_of(row[0].content))
        # The wiki's image, else the Ways to Level Up task's icon (UNKNOWN which Hypixel shows).
        entry['icon'] = {'material': material} if material else dict(task_icons.get(entry['task'], {'material': 'PAPER'}))
        unlock = group_unlock(name)
        if unlock and not entry['items']:
            entry['unlock'] = unlock
        groups.append(entry)
    return groups


def recorded_group(material, name, lore, color, skins, wiki, collections_by_name):
    """A recorded guide group: its name (in the stage's colour unless it has its own), icon, how many parts it has,
    what it says, the parts it lists, what it's worth, and for a one-part group what its progress counts to."""
    shown = re.sub(r'^&[ac][✔✖] ', '', name)
    shown = shown if shown.startswith('&') else '&' + color + shown
    plain = strip_codes(shown).strip()
    entry = {'name': shown}
    lines, i = list(lore), 0
    m = re.match(r'^&8(\d+) tasks?$', lines[0]) if lines else None
    count = 1
    if m:
        count = int(m.group(1))
        i = 2
    description = []
    while i < len(lines) and lines[i].strip():
        description.append(lines[i])
        i += 1
    items, worth, progress, maximum = [], None, None, None
    for j in range(i, len(lines)):
        line = lines[j]
        m = re.match(r'^&([ac]) ([✔✖]) (.*)$', line)
        if m:
            part = m.group(3)
            part = part[2:] if part.startswith(('&8', '&f')) else part
            items.append({'text': part})
            continue
        m = re.match(r'^&7Total Worth: &b\+([0-9,]+) XP$', line)
        if m:
            worth = int(m.group(1).replace(',', ''))
        m = re.match(r'^&7Progress to (.+): ', line)
        if m:
            progress = m.group(1)
            nxt = re.search(r'/&.([0-9,]+)$', lines[j + 1]) if j + 1 < len(lines) else None
            maximum = int(nxt.group(1).replace(',', '')) if nxt else 1
    key = plain.lower()
    match = wiki.get(key) or wiki.get(GROUP_ALIASES.get(key, key))
    entry['task'] = match['task'] if match else snake(plain)
    entry['lines'] = description
    entry['xp'] = worth
    entry['icon'] = icon(material, name, skins)
    entry['count'] = count
    wiki_items = {strip_codes(t['text']).lower(): t for t in (match or {}).get('items', [])}
    if items:
        entry['listed'] = True
        for item in items:
            known = wiki_items.get(strip_codes(item['text']).strip().lower())
            unlock = (known or {}).get('unlock') or unlock_of_text(strip_codes(item['text']).strip(), collections_by_name)
            if unlock:
                item['unlock'] = unlock
        entry['items'] = items
    elif match and len(match.get('items', [])) == count and count > 1:
        # The same parts as the wiki's, not listed on the item.
        entry['listed'] = False
        entry['items'] = match['items']
    if progress:
        entry['progress'] = progress
        entry['max'] = maximum
    unlock = (match or {}).get('unlock') or group_unlock(plain)
    if unlock and count == 1:
        entry['unlock'] = unlock
    return entry, match


def guide(pages, recorded, materials, skins, collections_by_name, task_icons, problems):
    """Each stage: its item (name, subtitle, description, how many tasks) and its groups of tasks, as recorded where
    the recording has the stage (Hypixel's today), else as the wiki lists them."""
    table = stage_table(pages['SkyBlock Guide'][0])
    out = []
    colors = {}
    for stage in STAGES:
        menu = last_menu(recorded, 'Guide ➜ ' + stage)
        for slot, (material, name, lore) in (menu or {}).items():
            if 1 <= slot <= 7 and strip_codes(name) in STAGES:
                colors[strip_codes(name)] = (name[1], lore)
    for stage in STAGES:
        info = table.get(stage, {'subtitle': '', 'description': '', 'tasks': 0})
        color, lore = colors.get(stage, ('7', None))
        entry = {'stage': snake(stage), 'name': '&' + color + stage, 'subtitle': info['subtitle']}
        if stage not in colors:
            problems.append(f'guide {stage}: no recorded stage item (its colour is UNKNOWN)')
        # The recorded description (an unlocked stage's), else the wiki's to wrap.
        description = []
        if lore and len(lore) > 2 and not lore[2].startswith('&7Reach'):
            for line in lore[2:]:
                if not line.strip():
                    break
                description.append(line)
        if description:
            entry['lines'] = description
        else:
            entry['description'] = '&7' + info['description']
        entry['tasks'] = info['tasks']
        wiki = {g['name'].lower(): g for g in wiki_groups(pages['SkyBlock Guide/Tasks/' + stage][0], materials, collections_by_name,
                                                         task_icons, problems)}
        menu = last_menu(recorded, 'Guide ➜ ' + stage)
        groups = []
        if menu:
            used = set()
            for slot in sorted(menu):
                material, name, lore = menu[slot]
                if not (19 <= slot <= 43 and 1 <= slot % 9 <= 7) or not strip_codes(name).strip():
                    continue
                group, match = recorded_group(material, name, lore, color, skins, wiki, collections_by_name)
                if match:
                    used.add(match['name'].lower())
                groups.append(group)
            counted = sum(g['count'] for g in groups)
            if counted != info['tasks']:
                problems.append(f'guide {stage}: the recorded groups have {counted} tasks, the wiki says {info["tasks"]}')
            for name in wiki:
                if name not in used:
                    problems.append(f'guide {stage}: the wiki has "{wiki[name]["name"]}", which the recorded menu doesn\'t (left out)')
        else:
            for g in wiki.values():
                group = {'name': '&' + color + g['name'], 'task': g['task'], 'description': g['description'], 'xp': g['xp'],
                         'icon': g['icon'], 'count': max(1, len(g['items']))}
                if g['items']:
                    group['listed'] = len(g['items']) <= LISTED
                    group['items'] = g['items']
                if 'unlock' in g:
                    group['unlock'] = g['unlock']
                groups.append(group)
        entry['groups'] = groups
        out.append(entry)
    return out


def collection_names(source):
    """Collection ids by name, lower case ("carrot" -> "CARROT_ITEM"), from Hypixel's collections API."""
    if not source:
        return {}
    if re.match(r'https?://', source):
        request = urllib.request.Request(source, headers={'User-Agent': 'skyblock-leveling-builder'})
        with urllib.request.urlopen(request, timeout=120) as response:
            api = json.loads(response.read())
    else:
        with open(source, encoding='utf-8') as f:
            api = json.load(f)
    out = {}
    for category in api.get('collections', {}).values():
        for cid, c in category.get('items', {}).items():
            out[c['name'].lower()] = cid
    return out


# ---------- output ----------

def default_out():
    # The private data checkout sits next to this repository's main checkout (from a worktree too).
    try:
        common = subprocess.run(['git', '-C', ROOT, 'rev-parse', '--path-format=absolute', '--git-common-dir'],
                                capture_output=True, text=True, check=True).stdout.strip()
        main = os.path.dirname(common) if os.path.basename(common) == '.git' else ROOT
    except (OSError, subprocess.CalledProcessError):
        main = ROOT
    return os.path.join(os.path.dirname(main), 'skyblock-dungeon-data', 'leveling')


def check_neu(neu, categories, problems):
    """NEU's sblevels.json against the tasks: what it says a category is worth (older than the recording)."""
    if not neu:
        return None
    path = os.path.join(neu, 'constants', 'sblevels.json')
    with open(path, encoding='utf-8') as f:
        sb = json.load(f)
    ours = {c['id']: c.get('max') for c in categories}
    for key, xp in sb.get('category_xp', {}).items():
        cid = key.removesuffix('_task')
        if cid in ours and ours[cid] != xp:
            problems.append(f'NEU says {cid} is worth {xp:,} XP, the recording {ours[cid]:,}')
    try:
        return subprocess.run(['git', '-C', neu, 'rev-parse', 'HEAD'], capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return None


def build(args):
    jar = args.paper_api or build_items.paper_api_jar()
    if not os.path.exists(jar):
        sys.exit(f'{jar} not found: build the plugin once (mvn package) or pass --paper-api')
    materials = Materials(jar)
    problems = []
    pages = load_wiki(args.wiki, args.save_wiki)
    recorded = menus(args.gui)
    skins = textures(args.raw)

    ways = last_menu(recorded, 'Ways to Level Up')
    if not ways:
        sys.exit(f'{args.gui}: no Ways to Level Up menu')
    task_tabs = dict(tabs(pages['SkyBlock Levels/Tasks'][0]))
    categories = []
    for slot in CATEGORY_SLOTS:
        if slot not in ways:
            problems.append(f'no category in slot {slot} of the recorded Ways to Level Up')
            continue
        material, name, lore = ways[slot]
        category = category_from_item(material, name, lore, skins)
        tab = CATEGORY_TABS.get(category['id'])
        wiki = wiki_tasks(task_tabs.get(tab, ''), problems) if tab else []
        tasks = match_tasks(category.pop('recorded'), wiki, problems, category['id'])
        for task in tasks:
            finish_task(task, materials, category['icon'])
        category['tasks'] = tasks
        worth = sum(t.get('max') or 0 for t in tasks)
        if category['max'] is None:
            category['max'] = worth
        elif worth != category['max']:
            problems.append(f'{category["id"]}: the wiki\'s tasks add up to {worth:,} XP, the recorded category to {category["max"]:,}')
        categories.append(category)

    levels_page = pages['SkyBlock Levels'][0]
    data = {
        'format': 1,
        'source': {'wiki': {t: rev for t, (text_, rev) in pages.items()}, 'recording': os.path.basename(os.path.dirname(os.path.abspath(args.gui))),
                   'neu': check_neu(args.neu, categories, problems)},
        'categories': categories,
        'rewards': rewards(recorded, skins, problems),
        'emblems': emblems(levels_page, recorded, skins, problems),
        'guide': guide(pages, recorded, materials, skins, collection_names(args.collections),
                       {t['id']: t['icon'] for c in categories for t in c['tasks']}, problems),
    }
    return data, problems


def write(data, problems, out):
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, 'leveling.json'), 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=1, sort_keys=False)
        f.write('\n')
    counts = collections.Counter(r['kind'] for r in data['rewards'])
    lines = ['# SkyBlock Leveling data', '', f'Made by tools/leveling/build_leveling.py from the wiki (revisions in `source`) and the '
             f'recording `{data["source"]["recording"]}`.', '',
             f'- {len(data["categories"])} categories, {sum(len(c["tasks"]) for c in data["categories"])} tasks, '
             f'{sum(c["max"] for c in data["categories"]):,} XP in all',
             f'- rewards: ' + ', '.join(f'{n} {k.lower()}' for k, n in sorted(counts.items())),
             f'- emblems: ' + ', '.join(f'{len(c["emblems"])} {c["id"]}' for c in data['emblems']),
             f'- guide: ' + ', '.join(f'{len(s["groups"])} {s["stage"]}' for s in data['guide']), '', '## Problems', '']
    lines += [f'- {p}' for p in problems] or ['None.']
    with open(os.path.join(out, 'report.md'), 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('--gui', required=True, help="a decoded recording's gui.txt with the SkyBlock Leveling menus (tools/replay decode)")
    ap.add_argument('--raw', help="the same recording's window packets (tools/replay raw ... open_window window_items), for heads' skins")
    ap.add_argument('--wiki', help='saved wiki pages (from --save-wiki) instead of fetching them')
    ap.add_argument('--save-wiki', help='where to save the wiki pages fetched')
    ap.add_argument('--neu', help='a NotEnoughUpdates-REPO clone, to check its sblevels.json against the recording')
    ap.add_argument('--collections', default=COLLECTIONS_URL, help="Hypixel's collections API (a URL or a saved response)")
    ap.add_argument('--out', default=None, help='output folder (default: the private data checkout\'s leveling/)')
    ap.add_argument('--paper-api', default=None, help="the paper-api jar to check materials against (default: the pom's, from ~/.m2)")
    args = ap.parse_args()
    data, problems = build(args)
    out = args.out or default_out()
    write(data, problems, out)
    print(f'{out}/leveling.json: {len(data["categories"])} categories, {len(data["rewards"])} rewards, '
          f'{sum(len(c["emblems"]) for c in data["emblems"])} emblems, {len(problems)} problems (report.md)')


if __name__ == '__main__':
    main()
