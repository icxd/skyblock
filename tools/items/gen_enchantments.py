# python3 gen_enchantments.py <NotEnoughUpdates-REPO/items>
# Builds paper/src/main/resources/enchantments.json: each enchantment's name and description at each level,
# from NEU-REPO's enchanted book dumps (the in-game text), and for levels with no book, that level's values
# (data/enchant_values.json, from the wiki) put into the nearest level's text.
import json, os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..', '..')
N = sys.argv[1]
GLYPH = {'e001':'⚔','e003':'✎','e007':'☠','e008':'❈','e00b':'⫽','e00c':'☂','e00d':'❁','e010':'❤','e011':'❣','e014':'☄','e015':'⸕','e01a':'✯','e01c':'✧','e021':'α','e022':'✦','e024':'Ⓢ','e025':'⛃','e027':'❂','e028':'♨','e02c':'☣','e050':'❁','e053':'☘','e054':'☘'}
# Our enchantments and their max levels, from EnchantmentType.java.
src = open(os.path.join(ROOT, 'paper/src/main/java/net/icxd/dungeons/item/enchanting/EnchantmentType.java')).read()
ours = [(m.group(1), int(m.group(3))) for m in re.finditer(r'new EnchantmentType\("[^"]*", "([a-z_]+)", (true, )?(\d+)', src)]
values = json.load(open(os.path.join(HERE, 'data/enchant_values.json')))
NEU_ID = {'one_for_all': 'ULTIMATE_ONE_FOR_ALL'}

def collapse(s):
    # drop colour codes that don't change anything (e.g. "§7a §7b" -> "§7a b")
    out, cur, i = [], None, 0
    while i < len(s):
        if s[i] == '§' and i + 1 < len(s):
            code = s[i:i+2]
            if code[1] in '0123456789abcdef':
                if code != cur: out.append(code); cur = code
            else:
                out.append(code); cur = None if code[1] == 'r' else cur + code if cur else code
            i += 2; continue
        out.append(s[i]); i += 1
    return ''.join(out).replace('§', '&')

def glyphs(s):
    return re.sub(r'[-]', lambda m: GLYPH.get('%04x' % ord(m.group(0)), ''), s)

def fmt(v):
    return ('%d' % v) if float(v).is_integer() else ('%s' % v)

def numbers(text):
    # number tokens that stand alone (a colour code like "&a" right before one is fine; "2nd" isn't a number)
    masked = re.sub(r'&[0-9a-fk-or]', '\0\0', text)  # colour codes aren't numbers
    for m in re.finditer(r'\d+(?:[.,]\d+)*', masked):
        before = masked[m.start() - 1] if m.start() else ' '
        if before.isalnum(): continue
        after = masked[m.end()] if m.end() < len(masked) else ' '
        if after.isascii() and after.isalpha() and after not in 'xsk': continue
        yield m

def substitute(text, old, new):
    # each of the base level's values, in order, becomes the new level's
    pos, out = 0, text
    for a, b in zip(old, new):
        m = next((mm for mm in numbers(out) if mm.start() >= pos and float(mm.group(0).replace(',', '')) == float(a)), None)
        if m is None: return None
        rep = fmt(b)
        out = out[:m.start()] + rep + out[m.end():]; pos = m.start() + len(rep)
    return out

result, report = {}, []
for ns, max_level in ours:
    nid = NEU_ID.get(ns, ns.upper())
    levels, display = {}, None
    for f in os.listdir(N):
        m = re.match(re.escape(nid) + r';(\d+)\.json$', f)
        if not m: continue
        L = json.load(open(f'{N}/{f}'))['lore']
        k = next((i for i, l in enumerate(L) if l.startswith(('§9', '§d')) and re.search(r' [IVXL]+$', re.sub(r'§.', '', l))), None)
        if k is None: continue
        display = re.sub(r'§.', '', L[k]).rsplit(' ', 1)[0]
        desc = []
        for l in L[k+1:]:
            if not l.strip() or l.startswith('§8Gain '): break
            desc.append(l)
        levels[int(m.group(1))] = collapse(glyphs(' '.join(desc)))
    vals = values.get(ns) or []
    for lvl in range(1, max_level + 1):
        if lvl in levels or not levels: continue
        base = min(levels, key=lambda b: abs(b - lvl))
        vb = vals[base - 1] if base - 1 < len(vals) else None
        vl = vals[lvl - 1] if lvl - 1 < len(vals) else None
        if vb is not None and vl is not None:
            t = levels[base] if vb == [] and vl == [] else substitute(levels[base], vb, vl)
            if t is not None:
                levels[lvl] = t; report.append(f'{ns} {lvl}: from {base}'); continue
        report.append(f'{ns} {lvl}: no text')
    result[ns] = {'name': display, 'levels': {str(k): v for k, v in sorted(levels.items()) if k <= max_level}}
json.dump(result, open(os.path.join(ROOT, 'paper/src/main/resources/enchantments.json'), 'w'), indent=1, ensure_ascii=False)
print('\n'.join(report))
