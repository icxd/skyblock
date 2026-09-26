#!/usr/bin/env python3
# python3 tools/items/build_items.py [--api PATH_OR_URL] [--neu NEU-REPO] [--out DIR] [--paper-api JAR]
#
# Builds the plugin's item data, items.json (format 1, see README.md), and report.md next to it, from
# Hypixel's items API (numbers, flags, costs, requirements, rarity: the API wins for these) and
# NotEnoughUpdates-REPO's in-game dumps (the text: the dark gray lines under the name, the item's own
# lines, and its ability and bonus blocks in the order Hypixel shows them). Both are Hypixel's, so the
# output goes to the private data repository, never into this one.
import argparse
import base64
import collections
import json
import os
import re
import struct
import subprocess
import sys
import time
import urllib.request
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
SRC = os.path.join(ROOT, 'paper/src/main/java/net/icxd/dungeons')
API_URL = 'https://api.hypixel.net/v2/resources/skyblock/items'
PUA = re.compile('[\ue000-\uf8ff]')


# ---------- tables and the plugin's names ----------

def tsv(name):
    """A table in data/, without its # comment lines."""
    with open(os.path.join(HERE, 'data', name), encoding='utf-8') as f:
        return [line.rstrip('\n').split('\t') for line in f if line.strip() and not line.startswith('#')]


def enum_names(path):
    """The constants of the Java enum in path: the data is checked against the plugin's own source."""
    body = re.sub(r'/\*.*?\*/', '', open(path, encoding='utf-8').read(), flags=re.S)
    body = re.sub(r'//[^\n]*', '', body)
    body = body[re.search(r'\benum\s+\w+[^{]*\{', body).end():]
    body = re.sub(r'"(\\.|[^"\\])*"', '""', body)
    body = re.sub(r"'(?:\\.|[^'\\])'", "''", body)
    body = body[:min(i for i in (body.find(';'), body.find('}'), len(body)) if i >= 0)]
    body = re.sub(r'\([^()]*(\([^()]*\)[^()]*)*\)', '', body)
    return {n.strip() for n in body.split(',') if n.strip()}


def plugin_enums():
    return {k: enum_names(os.path.join(SRC, p)) for k, p in {
        'stat': 'stats/Stat.java', 'rarity': 'item/enums/Rarity.java', 'type': 'item/enums/SpecificItemType.java',
        'gem': 'item/gemstone/GemstoneType.java', 'essence': 'item/cost/essence/EssenceType.java',
        'slayer': 'item/requirement/slayer/SlayerBossType.java', 'dungeon': 'item/requirement/dungeontier/DungeonType.java',
        'skill': 'skill/Skill.java', 'kuudra': 'crimsonisle/kuudra/KuudraTier.java',
        'activation': 'item/ability/AbilityActivation.java', 'block': 'item/ability/AbilityType.java'}.items()}


# The API requirement types item/requirement/ has a class for.
MODELLED_REQUIREMENTS = {'SKILL', 'SLAYER', 'DUNGEON_TIER', 'HEART_OF_THE_MOUNTAIN', 'KUUDRA_COMPLETION'}
# Block kinds as the plugin's AbilityType names them.
BLOCK_TYPE = {'ABILITY': 'ABILITY', 'SHORTBOW': 'SHORTBOW', 'PIECE': 'PIECE_BONUS', 'FULL_SET': 'FULL_SET_BONUS'}


def class_fields(jar, name):
    """(access flags, name, descriptor) of each field of a class in a jar, read from the class file itself."""
    with zipfile.ZipFile(jar) as z:
        data = z.read(name + '.class')
    count = struct.unpack_from('>H', data, 8)[0]
    utf8, pos, i = {}, 10, 1
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:
            n = struct.unpack_from('>H', data, pos)[0]
            utf8[i] = data[pos + 2:pos + 2 + n].decode('utf-8', 'replace')
            pos += 2 + n
        elif tag in (5, 6):  # long and double take two slots
            pos += 8
            i += 1
        else:
            pos += {3: 4, 4: 4, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}[tag]
        i += 1
    pos += 6  # access flags, this class, super class
    pos += 2 + 2 * struct.unpack_from('>H', data, pos)[0]  # interfaces
    field_count = struct.unpack_from('>H', data, pos)[0]
    pos += 2
    fields = []
    for _ in range(field_count):
        access, name_index, descriptor_index, attributes = struct.unpack_from('>HHHH', data, pos)
        pos += 8
        for _ in range(attributes):
            pos += 6 + struct.unpack_from('>I', data, pos + 2)[0]
        fields.append((access, utf8[name_index], utf8[descriptor_index]))
    return fields


def paper_api_jar():
    """The paper-api jar the plugin builds against (paper/pom.xml's version, from the local Maven repository)."""
    pom = open(os.path.join(ROOT, 'paper/pom.xml'), encoding='utf-8').read()
    version = re.search(r'<artifactId>paper-api</artifactId>\s*<version>([^<]+)</version>', pom).group(1)
    return os.path.expanduser(f'~/.m2/repository/io/papermc/paper/paper-api/{version}/paper-api-{version}.jar')


def item_materials(jar):
    """The materials an item can be: Material's modern constants that ItemType also has (not AIR)."""
    materials = {n for access, n, d in class_fields(jar, 'org/bukkit/Material')
                 if access & 0x4000 and d == 'Lorg/bukkit/Material;' and not n.startswith('LEGACY_')}
    item_types = {n for access, n, d in class_fields(jar, 'org/bukkit/inventory/ItemType')
                  if access & 0x0008 and d.startswith('Lorg/bukkit/inventory/ItemType')}
    return (materials & item_types) - {'AIR'}


# ---------- sources ----------

def load_api(source):
    if re.match(r'https?://', source):
        request = urllib.request.Request(source, headers={'User-Agent': 'skyblock-items-builder'})
        with urllib.request.urlopen(request, timeout=120) as response:
            raw = response.read()
    else:
        with open(source, 'rb') as f:
            raw = f.read()
    api = json.loads(raw)
    if not api.get('success') or not isinstance(api.get('items'), list):
        sys.exit(f'{source}: not the items API response')
    return api


def git(path, *args):
    return subprocess.run(['git', '-C', path, *args], capture_output=True, text=True, check=True).stdout.strip()


def default_out():
    # The private data checkout sits next to this repository's main checkout (from a worktree too).
    try:
        common = git(ROOT, 'rev-parse', '--path-format=absolute', '--git-common-dir')
        main = os.path.dirname(common) if os.path.basename(common) == '.git' else ROOT
    except (OSError, subprocess.CalledProcessError):
        main = ROOT
    return os.path.join(os.path.dirname(main), 'skyblock-dungeon-data', 'items')


class Neu:
    """A NotEnoughUpdates-REPO clone: items/<ID>.json (':' in an id is '-'), and itemsOverlay/<data version>/,
    which has the item id Hypixel sends to modern clients."""

    def __init__(self, path):
        self.path = path
        self.items = os.path.join(path, 'items')
        if not os.path.isdir(self.items):
            sys.exit(f'{path}: not a NotEnoughUpdates-REPO clone (no items/)')
        self.overlay = {}
        overlays = os.path.join(path, 'itemsOverlay')
        if os.path.isdir(overlays):
            for version in sorted((v for v in os.listdir(overlays) if v.isdigit()), key=int):
                for f in os.listdir(os.path.join(overlays, version)):
                    if f.endswith('.snbt'):
                        self.overlay[f[:-5]] = os.path.join(overlays, version, f)

    def item(self, item_id):
        path = os.path.join(self.items, item_id.replace(':', '-') + '.json')
        if not os.path.exists(path):
            return None
        with open(path, encoding='utf-8') as f:
            return json.load(f)

    def modern_id(self, item_id):
        path = self.overlay.get(item_id.replace(':', '-'))
        if path is None:
            return None
        with open(path, encoding='utf-8') as f:
            m = re.search(r'^\tid: "minecraft:([^"]+)"', f.read(), re.M)
        return m.group(1).upper() if m else None


# ---------- text ----------

TOKENS = {'black': '0', 'dark_blue': '1', 'dark_green': '2', 'dark_aqua': '3', 'dark_red': '4', 'dark_purple': '5',
          'gold': '6', 'gray': '7', 'dark_gray': '8', 'blue': '9', 'green': 'a', 'aqua': 'b', 'red': 'c',
          'light_purple': 'd', 'yellow': 'e', 'white': 'f', 'obfuscated': 'k', 'bold': 'l', 'strikethrough': 'm',
          'underline': 'n', 'italic': 'o', 'reset': 'r'}
# The API writes a few glyphs as tokens; this one is the Critter mob type's (glyphs.tsv E087).
GLYPH_TOKENS = {'critter': '\ue087'}


class Text:
    """Hypixel's text into the plugin's: § becomes &, and each private-use glyph its classic symbol (or nothing,
    counted for the report, when none is known)."""

    def __init__(self, glyphs):
        self.glyphs = glyphs
        self.dropped = collections.defaultdict(collections.Counter)  # codepoint -> item id -> occurrences
        self.mapped = collections.Counter()
        self.tokens = collections.defaultdict(collections.Counter)  # unknown %%token%% -> item id -> occurrences
        self.ampersands = collections.Counter()

    def __call__(self, item_id, s):
        out, k = [], 0
        while k < len(s):
            c = s[k]
            k += 1
            if not '\ue000' <= c <= '\uf8ff':
                out.append(c)
                continue
            symbol = self.glyphs.get(ord(c))
            if symbol:
                self.mapped[ord(c)] += 1
                out.append(symbol)
                continue
            self.dropped[ord(c)][item_id] += 1
            # A dropped glyph after a space takes the space after it too ("to Timid mobs", not "to  Timid mobs").
            before = re.sub(r'(?:[&§][0-9a-fk-or])+$', '', ''.join(out))
            if (before == '' or before.endswith(' ')) and s[k:k + 1] == ' ':
                k += 1
        return ''.join(out).replace('§', '&')

    def tokens_to_codes(self, item_id, s, mark='&'):
        """The API's "%%gray%%" tokens as colour codes."""
        def token(m):
            name = m.group(1)
            if name in TOKENS:
                return mark + TOKENS[name]
            if name in GLYPH_TOKENS:
                return GLYPH_TOKENS[name]
            self.tokens[name][item_id] += 1
            return ''
        return re.sub(r'%%([a-z_]+)%%', token, s)


# Text.java's width table and wrapping, as the plugin does it (Hypixel wraps its generated text the same way).
WIDTHS = {' ': 4, '!': 2, '"': 4, "'": 2, '(': 4, ')': 4, '*': 4, ',': 2, '.': 2, ':': 2, ';': 2, '<': 5, '>': 5,
          '@': 7, 'I': 4, '[': 4, ']': 4, '`': 3, 'f': 5, 'i': 2, 'k': 5, 'l': 3, 't': 4, '|': 2, '{': 4, '}': 4, '~': 7}
LORE_WIDTH = 192
CODES = '0123456789abcdefklmnorABCDEFKLMNOR'
COLORS = '0123456789abcdef'


def width(text):
    total, bold, i = 0, False, 0
    while i < len(text):
        c = text[i]
        if c in '&§' and i + 1 < len(text) and text[i + 1] in CODES:
            code = text[i + 1].lower()
            if code == 'l':
                bold = True
            elif code == 'r' or code in COLORS:
                bold = False
            i += 2
            continue
        # Java counts UTF-16 chars: a letter outside the BMP is two default-width ones.
        total += (WIDTHS.get(c, 6) + (1 if bold else 0)) * (2 if ord(c) > 0xFFFF else 1)
        i += 1
    return total


def format_at_end(text):
    color, fmt, i = '', '', 0
    while i + 1 < len(text):
        if text[i] in '&§' and text[i + 1] in CODES:
            code = text[i + 1].lower()
            if code in COLORS:
                color, fmt = '&' + code, ''
            elif code == 'r':
                color, fmt = '', ''
            else:
                fmt += '&' + code
            i += 2
            continue
        i += 1
    return color + fmt


def java_split_space(s):
    # String.split(" ") keeps leading empty strings but drops trailing ones ("" alone stays [""]).
    if s == '':
        return ['']
    parts = s.split(' ')
    while parts and parts[-1] == '':
        parts.pop()
    return parts


def wrap(text, max_width=LORE_WIDTH):
    lines = []
    for paragraph in text.split('\n'):
        line, carry = '', ''
        for word in java_split_space(paragraph):
            candidate = carry + word if line == '' else line + ' ' + word
            if line != '' and width(candidate) > max_width:
                lines.append(line)
                carry = format_at_end(carry + line)
                starts_with_color = len(word) > 1 and word[0] in '&§' and word[1].lower() in COLORS
                line = ('' if starts_with_color else carry) + word
            else:
                line = candidate
        lines.append(line)
    return lines


def description_lines(text, item_id, api_text):
    """The API's description as lines, the way Hypixel shows it: gray unless it says otherwise, wrapped."""
    s = text.tokens_to_codes(item_id, api_text)
    paragraphs = [p if p == '' or re.match(r'[&§][0-9a-fA-F]', p) else '&7' + p for p in s.split('\n')]
    lines = [text(item_id, line) for line in wrap('\n'.join(paragraphs))]
    while lines and lines[0] == '':
        lines.pop(0)
    while lines and lines[-1] == '':
        lines.pop()
    return lines


# ---------- NEU's lore, split into what the plugin generates and what it can't ----------

RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'DIVINE', 'SPECIAL', 'VERY SPECIAL',
            'UNOBTAINABLE', 'SUPREME', 'ULTIMATE', 'ADMIN']
RARITY_RE = re.compile(r'^(§[0-9a-f])§l(?:§k\S+§r )?(' + '|'.join(sorted(RARITIES, key=len, reverse=True))
                       + r')(?: (.+?))?(?: §k\S+)?\s*$')
# Something shaped like a rarity line that isn't one the plugin can write ("§9§lERAR", "§fCOMMON").
ODD_RARITY_RE = re.compile(r'^§[0-9a-f](?:§l)?[A-Z][A-Z ]*$')
STAT_LINE = re.compile(r"^§7([A-Z][A-Za-z' ]+): §[0-9a-f]([+-]?[\d,]+(?:\.\d+)?)(%|s)?[\ue000-\uf8ff]?(?: .*)?$")
BONUS_BRACKET = re.compile(r' §[9ed]\([+-]| §[69]\[[+-]')
ABILITY_RE = re.compile(r'^§6((?:[A-Z][a-z]+ )?Ability): (.+?)(?:\s{1,2}§e§l(.+?))?\s*$')
PASSIVE_RE = re.compile(r'^§6Passive: (.+?)\s*$')
BONUS_RE = re.compile(r'^§[68](Full Set Bonus|Piece Bonus|Tiered Bonus|Half Set Bonus|Extra Bonus|Set Bonus): (.+?)'
                      r'(?: (?:§7)?\((\d+)/(\d+)\))?\s*$')
SHORTBOW_RE = re.compile(r'^§[0-9a-f]Shortbow: (Instantly shoots!)\s*$')
COST_RE = re.compile(r'^§8(Mana Cost|Cooldown|Soulflow Cost|Health Cost|Vitality Cost|Coin Cost|Cost|Charges|Mana|Uses): (.+)$')
COST_FIELD = {'Mana Cost': 'mana', 'Cooldown': 'cooldown', 'Soulflow Cost': 'soulflow', 'Health Cost': 'health_cost'}
OTHER_HEADER_RE = re.compile(r'^(?:§[0-9a-fk-or])+([A-Z][A-Za-z ]{1,30}?(?: Buff| Bonus| Item| Ability| Bonuses)):')
BONUS_KIND = {'Full Set Bonus': 'FULL_SET', 'Piece Bonus': 'PIECE', 'Tiered Bonus': 'TIERED', 'Extra Bonus': 'EXTRA',
              'Half Set Bonus': 'HALF_SET', 'Set Bonus': 'SET'}
ACTIVATION = {'RIGHT CLICK': 'RIGHT_CLICK', 'LEFT CLICK': 'LEFT_CLICK', 'SNEAK RIGHT CLICK': 'SHIFT_RIGHT_CLICK',
              'SNEAK LEFT CLICK': 'SHIFT_LEFT_CLICK', '': 'PASSIVE'}

# Lines the plugin writes itself.
REQ_RE = re.compile(r'^(?:§7)?§4[❣☠] §cRequires')
SOULBOUND_RE = re.compile(r'^§8(?:§l)?\* (?:§8)?(?:Co-op )?Soulbound (?:§8§l)?\*$')
# (19 dungeon items write the reforge note with a gray code in front: "§7§8This item can be reforged!")
REFORGE_RE = re.compile(r'^(?:§7)?§8This item can be reforged!$')
# Footer lines the plugin's footer has no place for yet (they follow flags the API has: accessory, rift_transferrable).
FOOTER_DROPPED = {'§8Works while in Accessory Bag!': 'Works while in Accessory Bag!'}
RIFT_RE = re.compile(r'Rift-Transferable')
# How the capture's menu showed the item, not the item's own text.
MENU_LINES = {'§eRight-click to view recipes!'}
# Values that change with the item's state or its owner; the plugin writes these with code (nbtLore).
DYNAMIC_RES = [re.compile(p) for p in [
    r'§8/', r'\b\d[\d,]*/\d[\d,]*\b(?!\))', r'Not Installed', r'Your kills', r'Earned by', r'Purchased (by|for)',
    r'^§7Player:', r'^§7Charge:', r'^§7Fuel:', r'Time Between Actions', r'Max Storage', r'Resources Generated',
    r'Next Upgrade', r'Current Bonus', r'Selected', r'Enabled:', r'Collection:', r'§eLevel \d',
    r'Time Held', r'Kills?:', r'Stored', r'Progress', r'Obtained', r'Owner', r'§k', r'^§7From:', r'Bonus HP',
    r'Charges:', r'Uses remaining', r'Dowsing Mode', r'Vacuum Bag']]
MINION_RES = DYNAMIC_RES[9:12]
# What kinds of lore problem make an item's text need a person, and which only mean it has live values.
MANUAL_KINDS = {'no_rarity_line', 'odd_rarity_line', 'text_after_costs', 'text_after_blocks', 'generated_line_in_body',
                'cost_outside_ability', 'text_in_shortbow_block', 'cost_unparsed', 'modified_capture'}
DYNAMIC_KINDS = {'dynamic_text'}


def sections_of(lore):
    sections, current = [], []
    for line in lore:
        if line.strip() == '':
            if current:
                sections.append(current)
                current = []
        else:
            current.append(line)
    if current:
        sections.append(current)
    return sections


def block_header(line):
    """The block a header line starts, or None."""
    m = ABILITY_RE.match(line)
    if m:
        activation = (m.group(3) or '').strip()
        return {'kind': 'ABILITY', 'name': m.group(2).strip(), 'header': line, 'activation_text': activation,
                'label': m.group(1)}
    m = PASSIVE_RE.match(line)
    if m:
        return {'kind': 'ABILITY', 'name': m.group(1), 'header': line, 'activation_text': '', 'label': 'Passive'}
    m = BONUS_RE.match(line)
    if m:
        block = {'kind': BONUS_KIND[m.group(1)], 'name': re.sub('§.', '', m.group(2)).strip(), 'header': line,
                 'label': m.group(1)}
        if m.group(4):
            block['pieces'] = int(m.group(4))
        return block
    m = SHORTBOW_RE.match(line)
    if m:
        return {'kind': 'SHORTBOW', 'name': m.group(1), 'header': line, 'label': 'Shortbow'}
    return None


def cost_value(field, raw):
    """A cost line's value as (field, number), or None when it isn't a number the format has a place for."""
    value = PUA.sub('', re.sub('§.', '', raw)).replace('⸎', '').strip()
    if field == 'mana':
        m = re.fullmatch(r'([\d.]+)% of max', value)
        if m:
            return 'mana_percent', number(float(m.group(1)))
    m = re.fullmatch(r'([\d,]+(?:\.\d+)?)\s*(s|m|h|seconds?|minutes?|hours?)?', value)
    if not m:
        return None
    n = float(m.group(1).replace(',', ''))
    unit = (m.group(2) or 's')[0]
    if field == 'cooldown':
        n *= {'s': 1, 'm': 60, 'h': 3600}[unit]
    elif m.group(2):
        return None
    return field, number(n)


def number(v):
    return int(v) if float(v).is_integer() else v


def parse_lore(lore):
    """NEU's lore split the way the plugin builds it: rarity line, header, stat block, own text, blocks, footer."""
    out = {'rarity': None, 'words': None, 'rarity_color': None, 'categories': [], 'stats': {}, 'stat_lines': [], 'own': [], 'blocks': [],
           'reforgeable': any(REFORGE_RE.match(line) for line in lore), 'soulbound': None, 'requirements': [], 'dropped': collections.Counter(),
           'issues': collections.Counter(), 'notes': [], 'headers': collections.Counter()}
    issues = out['issues']

    def flag(kind, detail=None):
        issues[kind] += 1
        if detail and len(out['notes']) < 8:
            out['notes'].append(f'{kind}: {detail}')

    if any(line in MENU_LINES for line in lore):
        out['dropped']['"Right-click to view recipes!" (the capture\'s menu)'] += 1
    lore = [line for line in lore if line not in MENU_LINES]
    while lore and lore[-1].strip() == '':
        lore.pop()
    m = RARITY_RE.match(lore[-1]) if lore else None
    if m:
        lore.pop()
        out['rarity_color'], out['rarity'], out['words'] = m.group(1), m.group(2).replace(' ', '_'), m.group(3) or ''
    elif lore and ODD_RARITY_RE.match(lore[-1]):
        flag('odd_rarity_line', lore.pop())
    else:
        flag('no_rarity_line', lore[-1] if lore else '(empty)')
    sections = sections_of(lore)

    def footer(section):
        """The section's footer lines as (kind, line), or None if it isn't all footer. A requirement goes on in
        red when it wraps, or as " - " lines when it lists "Any of the following:"."""
        kinds, in_requirement = [], False
        for line in section:
            if REQ_RE.match(line):
                kind, in_requirement = 'requirement', True
            elif SOULBOUND_RE.match(line):
                kind, in_requirement = 'soulbound', False
            elif RIFT_RE.search(line):
                kind, in_requirement = 'Rift-Transferable', False
            elif line in FOOTER_DROPPED:
                kind, in_requirement = FOOTER_DROPPED[line], False
            elif REFORGE_RE.match(line):
                kind, in_requirement = 'reforge', False
            elif in_requirement and re.match(r'§c|§[0-9a-f] - ', line):
                kind = 'more requirement'
            else:
                return None
            kinds.append((kind, line))
        return kinds

    while sections and footer(sections[-1]):
        for kind, line in footer(sections.pop()):
            if kind == 'requirement':
                out['requirements'].append(line)
            elif kind == 'more requirement':
                out['requirements'][-1] += ' ' + line
            elif kind == 'soulbound':
                out['soulbound'] = 'COOP' if 'Co-op' in line else 'SOLO'
            elif kind != 'reforge':
                out['dropped'][kind] += 1

    # dark gray lines under the name ("Collection Item"); the plugin writes Breaking Power from the stats
    if sections and all(re.match(r'^§8[^§]*$', line) for line in sections[0]):
        for line in sections.pop(0):
            if re.fullmatch(r'§8Breaking Power \d+', line):
                out['stats']['Breaking Power'] = float(line.split()[-1])
            out['categories'].append(line[2:])

    # the stat block, which the plugin writes from the stats (and gear score, gemstones, shot cooldown); a long
    # gemstone line goes on in lines starting " §8["
    def stat_block_line(k, section):
        line = section[k]
        return STAT_LINE.match(line) or line.startswith('§7Gemstones:') \
            or (k > 0 and line.startswith(' §8[') and stat_block_line(k - 1, section) and not STAT_LINE.match(section[k - 1]))

    if sections and all(stat_block_line(k, sections[0]) for k in range(len(sections[0]))) \
            and not any(r.search(' '.join(sections[0])) for r in MINION_RES):
        for line in sections.pop(0):
            m = STAT_LINE.match(line)
            if m:
                out['stats'][m.group(1)] = float(m.group(2).replace(',', ''))
                out['stat_lines'].append((m.group(1), line))
                # a reforge's, potato books' or gemstones' bracket: NEU captured a changed item, not a new one
                if BONUS_BRACKET.search(line):
                    flag('modified_capture', line[:60])

    # A block that follows text without a blank line (Skeleton Master armor) is still a block; the plugin will
    # put the blank line in.
    split = []
    for section in sections:
        k = next((k for k in range(1, len(section)) if block_header(section[k])), None)
        if k is None or block_header(section[0]):
            split.append(section)
        else:
            flag('no_blank_before_block', section[k][:60])
            split += [section[:k], section[k:]]
    sections = split

    current, closed, seen_block = None, False, False

    def add_text(block, line):
        nonlocal closed
        cm = COST_RE.match(line)
        if cm and block['kind'] != 'SHORTBOW':
            block.setdefault('costs', []).append((cm.group(1), cm.group(2), line))
            closed = True
        else:
            if closed:
                flag('text_after_costs', line[:60])
            block.setdefault('text', []).append(line)

    for section in sections:
        if block_header(section[0]):
            # one section can hold several blocks (Hypixel sometimes leaves out the blank line between them)
            parts, part = [], []
            for line in section:
                if part and block_header(line):
                    parts.append(part)
                    part = []
                part.append(line)
            parts.append(part)
            for part in parts:
                block = block_header(part[0])
                out['blocks'].append(block)
                closed = False
                for line in part[1:]:
                    add_text(block, line)
                if block['kind'] == 'SHORTBOW' and len(part) > 1:
                    flag('text_in_shortbow_block')
                current = None if block['kind'] == 'SHORTBOW' else block
            seen_block = True
            continue
        om = OTHER_HEADER_RE.match(section[0])
        if om and not section[0].startswith('§7'):
            out['headers'][om.group(1)] += 1
        if current is not None and not closed:
            # a block's text with a blank line in it (or text Hypixel puts after the block: it stays there)
            current.setdefault('text', []).append('')
            for line in section:
                add_text(current, line)
            continue
        if seen_block:
            flag('text_after_blocks', section[0][:60])
        out['own'] += ([''] if out['own'] else []) + section

    # lines the plugin generates don't belong in the text
    def strip_generated(lines):
        kept = []
        for line in lines:
            if REQ_RE.match(line) or SOULBOUND_RE.match(line) or REFORGE_RE.match(line):
                flag('generated_line_in_body', line[:60])
            else:
                kept.append(line)
        return kept

    out['own'] = strip_generated(out['own'])
    for block in out['blocks']:
        if 'text' in block:
            block['text'] = strip_generated(block['text'])
    for line in out['own']:
        if COST_RE.match(line):
            flag('cost_outside_ability', line[:60])
    for line in out['own'] + [l for b in out['blocks'] for l in b.get('text', [])]:
        if any(r.search(line) for r in DYNAMIC_RES):
            flag('dynamic_text', line[:60])
    return out


# ---------- one item ----------

STAT_ALIAS = {'WALK_SPEED': 'SPEED', 'CRITICAL_DAMAGE': 'CRIT_DAMAGE', 'CRITICAL_CHANCE': 'CRIT_CHANCE',
              'HEALTH_REGENERATION': 'HEALTH_REGEN', 'ABILITY_DAMAGE_PERCENT': 'ABILITY_DAMAGE'}
# API stat names as item lore writes them (matched by value against NEU's stat lines), for the API/NEU check.
LORE_STAT = {
    'CRITICAL_CHANCE': 'Crit Chance', 'CRITICAL_DAMAGE': 'Crit Damage', 'ABILITY_DAMAGE_PERCENT': 'Ability Damage',
    'HEALTH_REGENERATION': 'Health Regen', 'WALK_SPEED': 'Speed', 'RIFT_INTELLIGENCE': 'Intelligence',
    'RIFT_WALK_SPEED': 'Speed', 'RIFT_HEALTH': 'Hearts', 'TROPHY_FISH_CHANCE': 'Trophy Chance',
    'MELON_FORTUNE': 'Melon Slice Fortune', 'NETHER_STALK_FORTUNE': 'Nether Wart Fortune'}
NOT_SHOWN_AS_LINE = {'WEAPON_ABILITY_DAMAGE'}  # the plugin skips it too: it's in the ability's text
# Stat-block lines the plugin writes from other data (gear score, shot cooldown) or doesn't write yet.
STAT_BLOCK_OTHER = {'Gear Score', 'Shot Cooldown', 'Accessory Power', 'Breaking Power'}
API_FIELDS = {  # the API fields format 1 has a place for, or which only feed the report
    'id', 'name', 'material', 'durability', 'tier', 'rarity', 'category', 'category_display', 'skin', 'color',
    'glowing', 'unstackable', 'dungeon_item', 'can_have_attributes', 'soulbound', 'gear_score', 'npc_sell_price',
    'stats', 'tiered_stats', 'gemstone_slots', 'upgrade_costs', 'requirements', 'description', 'cannot_reforge', 'item_model'}


def lore_stat_name(key):
    return LORE_STAT.get(key) or key.replace('_', ' ').title()


def api_stats_of(it):
    """The API's stats, names upper-cased. Dungeon drops of several qualities have tiered_stats instead: Hypixel
    shows the first tier's numbers on the item (NEU: Heavy Helmet Defense 62, of 62 to 130)."""
    stats = it.get('stats') or {k: v[0] for k, v in (it.get('tiered_stats') or {}).items() if v}
    return [(k.upper(), v) for k, v in stats.items()]


RARITY_COLOR = {'COMMON': '§f', 'UNCOMMON': '§a', 'RARE': '§9', 'EPIC': '§5', 'LEGENDARY': '§6', 'MYTHIC': '§d',
                'DIVINE': '§b', 'SPECIAL': '§c', 'VERY_SPECIAL': '§c', 'UNOBTAINABLE': '§4'}


def without_rarity_color(name, rarity_color):
    """The plugin writes the rarity colour before the name: a one-colour name loses its colour; a name that
    changes colour part way keeps its codes (all but a leading rarity colour)."""
    m = re.match(r'^((?:§[0-9a-fk-or])*)(.*)$', name, re.S)
    lead, rest = m.group(1), m.group(2)
    return rest if lead == rarity_color or not re.search(r'§[0-9a-fk-or]', rest) else name


def plain(s):
    return ' '.join(PUA.sub('', re.sub(r'[§&][0-9a-fk-or]', '', s)).split())


class Builder:
    def __init__(self, enums, materials, material_table, text, neu):
        self.enums, self.materials, self.material_table, self.text, self.neu = enums, materials, material_table, text, neu
        self.unknown = collections.defaultdict(lambda: collections.defaultdict(set))  # what -> value -> item ids
        self.conflicts = collections.defaultdict(list)  # kind -> [(item id, detail)]
        self.errors = collections.defaultdict(list)  # kind -> [(item id, detail)]: the item isn't written
        self.counts = collections.Counter()
        self.lore_status = {}  # item id -> clean / dynamic / manual
        self.issue_items = collections.defaultdict(list)  # issue kind -> item ids
        self.dropped_lines = collections.Counter()
        self.not_in_format = collections.defaultdict(set)  # what the format has no place for -> item ids
        self.headers = collections.defaultdict(set)
        self.type_labels = collections.Counter()
        self.description_check = collections.Counter()

    def note(self, what, value, item_id):
        self.unknown[what][value].add(item_id)

    def cost_list(self, item_id, costs):
        out = []
        for c in costs:
            t = c.get('type')
            if t == 'COINS':
                out.append({'coins': number(c['coins'])})
            elif t == 'ITEM':
                out.append({'item': c['item_id'], 'amount': number(c.get('amount', 1))})
            elif t == 'ESSENCE':
                if c['essence_type'] not in self.enums['essence']:
                    self.note('essence', c['essence_type'], item_id)
                out.append({'essence': c['essence_type'], 'amount': number(c['amount'])})
            else:
                raise ValueError(f'cost type {t}')
        return out

    def build(self, it):
        i = it['id']
        text = self.text
        neu = self.neu.item(i)
        parsed = parse_lore(neu.get('lore', [])) if neu else None
        rec = {}
        sources = [it['name'], it.get('description', '')] + ([neu.get('displayname', '')] + neu.get('lore', []) if neu else [])
        if any(re.search(r'&[0-9a-fk-orA-FK-OR]', s) for s in sources):
            text.ampersands[i] += 1  # a literal & the plugin would read as a colour code

        # rarity: the API's (a couple of items write it as "rarity")
        rarity = it.get('tier') or it.get('rarity') or 'COMMON'
        if rarity not in self.enums['rarity']:
            self.note('rarity', rarity, i)
        if rarity != 'COMMON':
            rec['rarity'] = rarity

        # name: NEU's (what the game shows; the API has typos and stray codes), without the rarity colour
        rec['name'] = self.name(it, neu, parsed)

        # material
        pair = (it['material'], int(it.get('durability', 0)))
        material = self.material_table.get(pair)
        if material is None:
            raise Skip('material', f'{pair[0]}:{pair[1]} is not in data/materials.tsv')
        if material not in self.materials:
            raise Skip('material', f'{pair[0]}:{pair[1]} -> {material} is not a 26.2 item material')
        rec['material'] = material
        model = it.get('item_model', '')
        if model.startswith('hypixel_skyblock:') and material == 'PAPER':
            self.counts['pack-only (PAPER)'] += 1
        elif model.startswith('minecraft:'):
            self.not_in_format['item_model (a vanilla model: the item shows as its material instead)'].add(i)
        modern = self.neu.modern_id(i)
        if modern and modern != material:
            self.conflicts['material'].append((i, f'API {pair[0]}:{pair[1]} -> {material}, Hypixel now sends {modern}'))

        # type and the rarity line's words
        dungeon = bool(it.get('dungeon_item'))
        if parsed and parsed['words'] is not None:
            words = parsed['words']
        elif 'category_display' in it:
            words = re.sub(r'%%[a-z_]+%%', '', it['category_display'])
        else:
            words = None
        if words is not None and dungeon:
            words = '' if words == 'DUNGEON ITEM' else words[8:] if words.startswith('DUNGEON ') else words
        category = it.get('category')
        if not category and words:
            category = re.sub(r'^DUNGEON ', '', words).replace(' ', '_')
        if category:
            rec['type'] = category
            if category not in self.enums['type']:
                self.note('type', category, i)
        default_words = category.replace('_', ' ') if category and category != 'NONE' else ''
        if words is not None and words != default_words:
            rec['type_label'] = words
            self.type_labels[(category or '-', words)] += 1

        # the look
        if 'skin' in it:
            texture = self.texture(i, it['skin'])
            if material != 'PLAYER_HEAD':
                self.not_in_format['skin on an item that is not a player head'].add(i)
            elif texture:
                rec.update(texture)
        if 'color' in it:
            rec['color'] = '#%02x%02x%02x' % tuple(int(x) for x in it['color'].split(','))
        for flag in ('glowing', 'unstackable', 'dungeon_item', 'can_have_attributes'):
            if it.get(flag) is True:
                rec[flag] = True
        if it.get('soulbound'):
            rec['soulbound'] = it['soulbound']
        if it.get('npc_sell_price'):
            rec['npc_sell_price'] = number(it['npc_sell_price'])

        # numbers
        api_stats = api_stats_of(it)
        if not it.get('stats') and api_stats:
            self.not_in_format['tiered_stats (only the first tier is written, as stats)'].add(i)
        stats = {}
        for key, value in api_stats:
            key = STAT_ALIAS.get(key, key)
            if key in stats:
                self.conflicts['stat written twice by the API'].append((i, key))
            if value == 0:
                continue
            if key not in self.enums['stat']:
                self.note('stat', key, i)
            stats[key] = number(value)
        if stats:
            rec['stats'] = stats
        # The Gear Score Hypixel shows is NEU's line; the API's gear_score is another number (Adaptive Boots: 50
        # there, 253 shown), so it only goes in the report.
        if it.get('gear_score'):
            self.not_in_format['API gear_score (not the Gear Score Hypixel shows)'].add(i)
        if parsed and parsed['stats'].get('Gear Score'):
            rec['gear_score'] = int(parsed['stats']['Gear Score'])
        if parsed and parsed['stats'].get('Shot Cooldown'):
            rec['shot_cooldown'] = number(parsed['stats']['Shot Cooldown'])
        if it.get('gemstone_slots'):
            slots = []
            for s in it['gemstone_slots']:
                if s['slot_type'] not in self.enums['gem']:
                    self.note('gemstone slot', s['slot_type'], i)
                slot = {'type': s['slot_type']}
                if s.get('costs'):
                    slot['costs'] = self.cost_list(i, s['costs'])
                if s.get('requirements'):
                    self.not_in_format['gemstone slot requirements'].add(i)
                slots.append(slot)
            rec['gemstone_slots'] = slots
        if it.get('upgrade_costs'):
            rec['upgrade_costs'] = [self.cost_list(i, star) for star in it['upgrade_costs']]
        if it.get('requirements'):
            rec['requirements'] = [dict(r) for r in it['requirements']]
            for r in it['requirements']:
                self.check_requirement(i, r)
        for field in sorted(set(it) - API_FIELDS):
            self.not_in_format[f'API field {field}'].add(i)

        # text
        if parsed:
            self.text_from_neu(i, it, rec, parsed, neu)
        else:
            self.lore_status[i] = 'none'
            if it.get('description'):
                lines = description_lines(text, i, it['description'])
                if lines:
                    rec['lore'] = lines
                    self.counts['lore from the API description'] += 1
        if 'reforgeable' not in rec and it.get('cannot_reforge'):
            rec['reforgeable'] = False
        return rec

    def name(self, it, neu, parsed):
        i = it['id']
        rarity = it.get('tier') or it.get('rarity') or 'COMMON'
        api_name = without_rarity_color(self.text.tokens_to_codes(i, it['name'], '§'), RARITY_COLOR.get(rarity))
        if not (neu and neu.get('displayname')):
            return self.text(i, api_name)
        name = without_rarity_color(neu['displayname'], parsed['rarity_color'])
        ours, theirs = plain(name), plain(api_name)
        if ours == theirs:
            return self.text(i, name)
        # NEU caught a few items in a state: "Spooky Pie (Year 69)", "Shiny Relic #1-7", or reforged ("Fortunate Stonk").
        if ours.startswith(theirs + ' (') or ours.startswith(theirs + ' #') \
                or (parsed['issues'].get('modified_capture') and ours.endswith(' ' + theirs)):
            self.conflicts['name: NEU\'s shows a state, the API\'s is used'].append((i, f'API "{theirs}", NEU "{ours}"'))
            return self.text(i, api_name)
        self.conflicts['name: NEU\'s is used'].append((i, f'API "{theirs}", NEU "{ours}"'))
        return self.text(i, name)

    def texture(self, item_id, skin):
        value = skin['value'] if isinstance(skin, dict) else skin
        try:
            url = json.loads(base64.b64decode(value + '=' * (-len(value) % 4)))['textures']['SKIN']['url']
        except (ValueError, KeyError, TypeError):
            return {'skin': value}
        m = re.fullmatch(r'https?://textures\.minecraft\.net/texture/([0-9a-f]+)', url)
        # Mojang leaves leading zeros out of the hash, so it isn't always 64 digits.
        return {'texture': m.group(1)} if m else {'skin': value}

    def check_requirement(self, item_id, r):
        t = r.get('type')
        if t not in MODELLED_REQUIREMENTS:
            self.note('requirement type', t, item_id)
        elif t == 'SKILL' and r.get('skill') not in self.enums['skill']:
            self.note('requirement skill', r.get('skill'), item_id)
        elif t == 'SLAYER' and str(r.get('slayer_boss_type', '')).upper() not in self.enums['slayer']:
            self.note('requirement slayer', r.get('slayer_boss_type'), item_id)
        elif t == 'DUNGEON_TIER' and r.get('dungeon_type') not in self.enums['dungeon']:
            self.note('requirement dungeon', r.get('dungeon_type'), item_id)
        elif t == 'KUUDRA_COMPLETION' and r.get('kuudra_tier') not in self.enums['kuudra']:
            self.note('requirement kuudra tier', r.get('kuudra_tier'), item_id)

    def text_from_neu(self, i, it, rec, parsed, neu):
        text = self.text
        self.counts['with a NEU file'] += 1
        # Breaking Power stays a header line only when the API has no number for the plugin to write it from.
        has_power = any(k == 'BREAKING_POWER' for k, _v in api_stats_of(it))
        categories = [c for c in parsed['categories'] if not (has_power and re.fullmatch(r'Breaking Power \d+', c))]
        if categories:
            rec['categories'] = [text(i, c) for c in categories]
        # A stat line the API has no number for stays a line of text (NEU's numbers can include a reforge's).
        shown_by_plugin = {lore_stat_name(k) for k, _v in api_stats_of(it)} | STAT_BLOCK_OTHER
        kept = [line for name, line in parsed['stat_lines'] if name not in shown_by_plugin]
        own = kept + ([''] if kept and parsed['own'] else []) + parsed['own']
        if own:
            rec['lore'] = [text(i, line) for line in own]
        blocks = []
        for b in parsed['blocks']:
            block = {'kind': b['kind'], 'name': text(i, b['name']), 'header': text(i, b['header'])}
            if b['kind'] not in BLOCK_TYPE or BLOCK_TYPE[b['kind']] not in self.enums['block']:
                self.note('block kind', b['kind'], i)
            if b['label'] not in ('Ability', 'Passive', 'Shortbow') and b['kind'] == 'ABILITY':
                self.note('ability header', b['label'], i)
            if b['kind'] == 'ABILITY':
                act = ACTIVATION.get(b['activation_text'])
                if act is None:
                    act = re.sub(r'[^A-Z0-9]+', '_', b['activation_text']).strip('_')
                if act not in self.enums['activation']:
                    self.note('activation', act, i)
                block['activation'] = act
            lines = list(b.get('text', []))
            for label, raw, line in b.get('costs', []):
                field = COST_FIELD.get(label)
                value = cost_value(field, raw) if field else None
                if value and value[0] not in block:
                    block[value[0]] = value[1]
                else:
                    # a cost format 1 has no field for stays a line of the block's text, where Hypixel has it
                    lines.append(line)
                    if field:
                        parsed['issues']['cost_unparsed'] += 1
                        parsed['notes'].append(f'cost_unparsed: {line[:60]}')
                    else:
                        self.not_in_format[f'block cost "{label}" (kept as a text line)'].add(i)
            if lines:
                block['text'] = [text(i, line) for line in lines]
            if 'pieces' in b:
                block['pieces'] = b['pieces']
            blocks.append(block)
        if blocks:
            rec['abilities'] = blocks
        rec['reforgeable'] = parsed['reforgeable']
        if it.get('cannot_reforge') and parsed['reforgeable']:
            self.conflicts['reforgeable'].append((i, 'API cannot_reforge, NEU shows "This item can be reforged!"'))
        for what, n in parsed['dropped'].items():
            self.dropped_lines[what] += 1
        for header in parsed['headers']:
            self.headers[header].add(i)
        if 'Accessory Power' in parsed['stats']:
            self.dropped_lines['Accessory Power (a stat-block line)'] += 1

        issues = parsed['issues']
        status = 'manual' if any(k in MANUAL_KINDS for k in issues) else 'dynamic' if any(k in DYNAMIC_KINDS for k in issues) else 'clean'
        self.lore_status[i] = status
        for kind in issues:
            self.issue_items[kind].append(i)
        self.cross_check(i, it, rec, parsed)
        if it.get('description'):
            self.check_description(i, it['description'], parsed)

    def cross_check(self, i, it, rec, parsed):
        api_rarity = it.get('tier') or it.get('rarity') or 'COMMON'
        if parsed['rarity'] and parsed['rarity'] != api_rarity:
            self.conflicts['rarity'].append((i, f'API {api_rarity}, NEU {parsed["rarity"]}'))
        dungeon_line = (parsed['words'] or '').startswith('DUNGEON')
        if parsed['words'] is not None and dungeon_line != bool(it.get('dungeon_item')):
            self.conflicts['dungeon item'].append((i, f'API dungeon_item {bool(it.get("dungeon_item"))}, NEU "{parsed["words"]}"'))
        shown = {k: v for k, v in parsed['stats'].items() if k not in STAT_BLOCK_OTHER}
        api_stats = dict(api_stats_of(it))
        names = set()
        for key, value in api_stats.items():
            if key in NOT_SHOWN_AS_LINE or value == 0:
                continue
            name = lore_stat_name(key)
            names.add(name)
            if key == 'BREAKING_POWER':
                shown_value = parsed['stats'].get('Breaking Power')
            else:
                shown_value = shown.get(name)
            if shown_value is None:
                self.conflicts['stat in the API, not in NEU\'s lore'].append((i, key))
            elif abs(shown_value - value) > 1e-6:
                self.conflicts['stat value'].append((i, f'{key}: API {number(value)}, NEU {number(shown_value)}'))
        for name, value in shown.items():
            if name not in names:
                self.conflicts['stat in NEU\'s lore, not in the API (kept as a text line)'].append((i, f'{name} {number(value)}'))
        if 'Breaking Power' in parsed['stats'] and 'BREAKING_POWER' not in api_stats:
            self.conflicts['Breaking Power in NEU\'s lore, not in the API (kept as a header line)'].append((i, number(parsed['stats']['Breaking Power'])))
        if parsed['requirements'] and not it.get('requirements'):
            self.conflicts['requirement in NEU\'s lore, not in the API'].append((i, re.sub('§.', '', parsed['requirements'][0])))
        if (parsed['soulbound'] or None) != (it.get('soulbound') or None):
            self.conflicts['soulbound'].append((i, f'API {it.get("soulbound")}, NEU {parsed["soulbound"]}'))

    def check_description(self, i, description, parsed):
        # How often the description, wrapped the plugin's way, is the text NEU shows (its own Text: no counting).
        text = Text(self.text.glyphs)
        mine = description_lines(text, i, description)
        theirs = [text(i, '§8' + c) for c in parsed['categories']] + ([''] if parsed['categories'] and parsed['own'] else [])
        theirs += [text(i, line) for line in parsed['own']]
        bare = lambda lines: [re.sub('&[0-9a-fk-or]', '', line) for line in lines]
        words = lambda lines: ' '.join(' '.join(bare(lines)).split())
        n = len(mine)
        if bare(mine) == bare(theirs):
            kind = 'the same lines' + ('' if mine == theirs else ' (other colour codes)')
        elif any(bare(theirs[k:k + n]) == bare(mine) for k in range(len(theirs) - n + 1)):
            kind = 'the same lines, among more text in NEU'
        elif words(mine) in words(theirs):
            kind = 'the same words, broken into lines elsewhere'
        else:
            kind = 'other words'
        self.description_check[kind] += 1


class Skip(Exception):
    def __init__(self, kind, detail):
        super().__init__(detail)
        self.kind, self.detail = kind, detail


# ---------- output ----------

def items_json(source, items):
    # One item per line, so a changed item is a one-line diff; keys sorted, so a rerun is byte-identical.
    head = '{"format":1,"source":' + json.dumps(source, sort_keys=True, separators=(',', ':')) + ',"items":{\n'
    body = ',\n'.join(json.dumps(k, ensure_ascii=False) + ':' + json.dumps(v, ensure_ascii=False, sort_keys=True, separators=(',', ':'))
                      for k, v in sorted(items.items()))
    return head + body + '\n}}\n'


def examples(ids, n=6):
    ids = sorted(ids)
    return ', '.join(ids[:n]) + (f', … (+{len(ids) - n})' if len(ids) > n else '')


def report(b, items, source, total, glyph_rows):
    status = collections.Counter(b.lore_status.values())
    lines = [
        '# Items build report', '',
        f'Hypixel items API lastUpdated {source["api_last_updated"]} '
        f'({time.strftime("%Y-%m-%d %H:%M UTC", time.gmtime(source["api_last_updated"] / 1000))}), '
        f'NotEnoughUpdates-REPO {source["neu_commit"]}.', '',
        '## Counts', '',
        '| | items |', '|---|---:|',
        f'| in the API | {total} |',
        f'| written to items.json | {len(items)} |',
        f'| skipped (errors below) | {sum(len(v) for v in b.errors.values())} |',
        f'| with a NEU file | {b.counts["with a NEU file"]} |',
        f'| lore clean | {status["clean"]} |',
        f'| lore with live values (dynamic: needs code for those lines) | {status["dynamic"]} |',
        f'| lore needing a person (manual) | {status["manual"]} |',
        f'| no NEU file: lore from the API description | {b.counts["lore from the API description"]} |',
        f'| no NEU file and no description: no lore | {status["none"] - b.counts["lore from the API description"]} |',
        f'| with ability or bonus blocks | {sum(1 for v in items.values() if "abilities" in v)} |',
        f'| gear_score from NEU\'s Gear Score line | {sum(1 for v in items.values() if "gear_score" in v)} |',
        f'| pack-only look (PAPER + a Hypixel model) | {b.counts["pack-only (PAPER)"]} |',
        '']
    lines += ['### Errors (item not written)', '']
    if b.errors:
        for kind, rows in sorted(b.errors.items()):
            lines += [f'- **{kind}** ({len(rows)}): ' + '; '.join(f'{i}: {d}' for i, d in rows[:20])]
    else:
        lines += ['None.']
    lines += ['', '### Lore issues (items per kind)', '',
              'manual: the text can\'t be laid out as Hypixel has it without a person; dynamic: lines with values that change '
              'with the item or its owner; note: only informational.', '',
              '| kind | class | items | examples |', '|---|---|---:|---|']
    for kind, ids in sorted(b.issue_items.items(), key=lambda kv: -len(kv[1])):
        cls = 'manual' if kind in MANUAL_KINDS else 'dynamic' if kind in DYNAMIC_KINDS else 'note'
        lines.append(f'| {kind} | {cls} | {len(ids)} | {examples(ids)} |')
    if b.headers:
        lines += ['', 'Headers that look like a block\'s but aren\'t one the format knows (kept as text): '
                  + ', '.join(f'"{h}" ({len(ids)})' for h, ids in sorted(b.headers.items(), key=lambda kv: -len(kv[1]))) + '.']
    lines += ['', 'Lines left out because the plugin writes them from data (stat block, requirements, soulbound, reforge note) '
              'are not listed. Left out and not written by the plugin yet:', '']
    for what, n in b.dropped_lines.most_common():
        lines.append(f'- {what}: {n} items')
    d = b.description_check
    lines += ['', f'API description vs NEU text, for the {sum(d.values())} items with both (a check of the wrapping used '
              'for items with no NEU file): ' + ', '.join(f'{k} {v}' for k, v in d.most_common()) + '.']

    lines += ['', '## Glyphs', '',
              f'{len(b.text.mapped)} distinct glyphs mapped to classic symbols ({sum(b.text.mapped.values())} times). '
              'Dropped (no classic symbol known, or not in data/glyphs.tsv):', '',
              '| codepoint | occurrences | items | why | examples |', '|---|---:|---:|---|---|']
    how = {int(cp, 16): h for cp, _s, h in glyph_rows}
    for cp, per_item in sorted(b.text.dropped.items()):
        lines.append(f'| {cp:04X} | {sum(per_item.values())} | {len(per_item)} | {how.get(cp, "not in glyphs.tsv")} | {examples(per_item)} |')
    if b.text.tokens:
        lines += ['', 'Unknown %%tokens%% in API text (dropped): '
                  + ', '.join(f'{t} ({examples(ids)})' for t, ids in sorted(b.text.tokens.items())) + '.']
    lines += ['', 'Literal "&" before a code letter in Hypixel\'s text (the plugin would read it as a colour code): '
              + (examples(b.text.ampersands) if b.text.ampersands else 'none') + '.']

    lines += ['', '## Names the plugin doesn\'t model', '',
              'Written as Hypixel has them; the loader warns and drops them (or skips the item, for a rarity).', '',
              '| what | value | items | examples |', '|---|---|---:|---|']
    for what in ('rarity', 'stat', 'type', 'gemstone slot', 'essence', 'requirement type', 'requirement skill',
                 'requirement slayer', 'requirement dungeon', 'requirement kuudra tier', 'activation', 'block kind', 'ability header'):
        for value, ids in sorted(b.unknown.get(what, {}).items(), key=lambda kv: (-len(kv[1]), str(kv[0]))):
            lines.append(f'| {what} | {value} | {len(ids)} | {examples(ids)} |')
    lines += ['', 'Rarity-line words written as type_label (the type, then what Hypixel shows):', '']
    lines += [f'- {t} -> "{w}": {n}' for (t, w), n in sorted(b.type_labels.items(), key=lambda kv: -kv[1])]

    lines += ['', '## What format 1 has no place for', '', '| what | items | examples |', '|---|---:|---|']
    for what, ids in sorted(b.not_in_format.items(), key=lambda kv: -len(kv[1])):
        lines.append(f'| {what} | {len(ids)} | {examples(ids)} |')

    lines += ['', '## API and NEU disagree', '',
              'The API wins for numbers, flags, rarity and material; NEU for text (name, lore, blocks).', '',
              '| kind | items | examples |', '|---|---:|---|']
    for kind, rows in sorted(b.conflicts.items(), key=lambda kv: -len(kv[1])):
        lines.append(f'| {kind} | {len({i for i, _ in rows})} | ' + '; '.join(f'{i} ({d})' for i, d in rows[:4]) + ' |')
    return '\n'.join(lines) + '\n'


def main():
    ap = argparse.ArgumentParser(description='Build items.json (format 1) and report.md from the Hypixel items API and NEU-REPO.')
    ap.add_argument('--api', default=API_URL, help='the items API response: a file, or a URL (default: fetch it)')
    ap.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO clone')
    ap.add_argument('--out', default=None, help='where items.json and report.md go (default: the private data checkout next to this repository)')
    ap.add_argument('--paper-api', default=None, help='the paper-api jar to check materials against (default: the pom\'s, from ~/.m2)')
    args = ap.parse_args()
    t0 = time.time()
    out = args.out or default_out()
    jar = args.paper_api or paper_api_jar()
    if not os.path.exists(jar):
        sys.exit(f'{jar} not found: build the plugin once (mvn package) or pass --paper-api')

    materials = item_materials(jar)
    material_table = {(name, int(data)): material for name, data, material, _how in tsv('materials.tsv')}
    glyph_rows = tsv('glyphs.tsv')
    glyphs = {int(cp, 16): symbol for cp, symbol, _how in glyph_rows if symbol}
    bad = sorted({m for m in material_table.values() if m not in materials})
    if bad:
        sys.exit(f'data/materials.tsv names materials 26.2 items can\'t be: {bad}')
    enums = plugin_enums()
    api = load_api(args.api)
    try:
        commit = git(args.neu, 'rev-parse', 'HEAD')
    except (OSError, subprocess.CalledProcessError):
        sys.exit(f'{args.neu}: not a git clone, so its commit can\'t be recorded')
    neu = Neu(args.neu)
    t_load = time.time()

    builder = Builder(enums, materials, material_table, Text(glyphs), neu)
    items = {}
    for it in sorted(api['items'], key=lambda x: x['id']):
        if it['id'] in items:
            builder.errors['duplicate id'].append((it['id'], 'listed twice by the API'))
            continue
        try:
            items[it['id']] = builder.build(it)
        except Skip as e:
            builder.errors[e.kind].append((it['id'], e.detail))
        except (KeyError, ValueError, TypeError) as e:
            builder.errors['unreadable'].append((it['id'], f'{type(e).__name__}: {e}'))
    t_build = time.time()

    source = {'api_last_updated': api['lastUpdated'], 'neu_commit': commit}
    data = items_json(source, items)
    left = sorted({'%04X' % ord(c) for c in PUA.findall(data)})
    if left:
        sys.exit(f'private-use glyphs left in the output: {left}')
    os.makedirs(out, exist_ok=True)
    with open(os.path.join(out, 'items.json'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(data)
    t_write = time.time()
    with open(os.path.join(out, 'report.md'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(report(builder, items, source, len(api['items']), glyph_rows))

    status = collections.Counter(builder.lore_status.values())
    print(f'{len(items)} items -> {os.path.join(out, "items.json")} ({len(data.encode("utf-8")):,} bytes); '
          f'{sum(len(v) for v in builder.errors.values())} skipped')
    print(f'lore: {status["clean"]} clean, {status["dynamic"]} dynamic, {status["manual"]} manual, '
          f'{builder.counts["lore from the API description"]} from the API description')
    print(f'glyphs: {len(builder.text.mapped)} mapped, {len(builder.text.dropped)} dropped')
    print(f'time: load {t_load - t0:.2f} s, build {t_build - t_load:.2f} s, write {t_write - t_build:.2f} s, '
          f'total {time.time() - t0:.2f} s')


if __name__ == '__main__':
    main()
