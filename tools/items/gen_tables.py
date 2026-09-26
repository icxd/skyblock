# python3 gen_tables.py <NotEnoughUpdates-REPO/items>
# Writes item/items/CrimsonArmor.java and item/items/Gemstones.java from NEU-REPO's item dumps (stats and text
# as Hypixel has them), templates/, and data/ (star costs and head textures the old per-item classes had).
import json, os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
NEU = sys.argv[1]
OUT = os.path.join(HERE, '..', '..', 'paper/src/main/java/net/icxd/dungeons/item/items')
GLYPH = {'e003':'✎','e008':'❈','e00b':'⫽','e00d':'❁','e010':'❤','e015':'⸕','e01c':'✧','e024':'Ⓢ','e027':'❂','e053':'☘'}
def u(s): return re.sub(r'[-]', lambda m: GLYPH['%04x' % ord(m.group(0))], s).replace('§', '&')
def jstr(s): return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'
def neu(i): return json.load(open(f'{NEU}/{i}.json'))

# ---------- Crimson ----------
ex = json.load(open(os.path.join(HERE, 'data/crimson_stars.json')))
TIERS = [('BASIC', '', '', 22), ('HOT', 'Hot ', 'HOT_', 27), ('BURNING', 'Burning ', 'BURNING_', 32), ('FIERY', 'Fiery ', 'FIERY_', 37), ('INFERNAL', 'Infernal ', 'INFERNAL_', 42)]
PIECES = ['HELMET', 'CHESTPLATE', 'LEGGINGS', 'BOOTS']
STATS = {'Health': 'HEALTH', 'Defense': 'DEFENSE', 'Strength': 'STRENGTH', 'Crit Damage': 'CRIT_DAMAGE', 'Intelligence': 'INTELLIGENCE'}
tiers_java = []
for kt, prefix, idp, skill in TIERS:
    helmet = neu(f'{idp}CRIMSON_HELMET')
    L = [u(l) for l in helmet['lore']]
    i = next(k for k, l in enumerate(L) if 'Tiered Bonus' in l); j = next(k for k, l in enumerate(L) if 'reforged' in l)
    req = next(l for l in L if 'Combat Skill' in l)
    assert f'Combat Skill {skill}' in req, req
    stats = []
    for p in PIECES:
        x = neu(f'{idp}CRIMSON_{p}')
        row = {}
        for l in x['lore']:
            m = re.match(r'§7([A-Za-z ]+): §.\+(\d+)', l)
            if m and m.group(1) in STATS: row[STATS[m.group(1)]] = int(m.group(2))
        stats.append('new Stats()' + ''.join(f'.set({k}, {v})' for k, v in row.items()))
    stars = ex[idp]['stars']
    stars_java = ', '.join(f'star({s[0]})' if len(s) == 1 else f'star({s[0]}, {s[2]})' for s in stars)
    bonus = ',\n                    '.join(jstr(l) for l in L[i:j-1])
    tiers_java.append(f'''        {kt}({jstr(prefix)}, {jstr(idp)}, KuudraTier.{kt}, {skill}, {jstr(ex[idp]['texture'])},
                List.of({bonus}),
                List.of({stars_java}),
                {stats[0]},
                {stats[1]},
                {stats[2]},
                {stats[3]})''')
crimson = open(os.path.join(HERE, 'templates/CrimsonArmor.java.tmpl')).read().replace('@@TIERS@@', ',\n'.join(tiers_java) + ';')
open(f'{OUT}/CrimsonArmor.java', 'w').write(crimson)

# ---------- Gemstones ----------
tex = json.load(open(os.path.join(HERE, 'data/gem_textures.json')))
GEMS = ['RUBY', 'AMETHYST', 'JADE', 'SAPPHIRE', 'AMBER', 'TOPAZ', 'JASPER', 'OPAL']
QUALS = ['ROUGH', 'FLAWED', 'FINE', 'FLAWLESS', 'PERFECT']
entries = []
for g in GEMS:
    for q in QUALS:
        i = f'{q}_{g}_GEM'
        L = [u(l) for l in neu(i)['lore']]
        assert L[0] == '&8Collection Item' and L[1] == '', (i, L[:2])
        k = L.index('', 2)
        intro = L[2:k]
        entries.append(f'        entry("{i}", {jstr(tex[i])}, {", ".join(jstr(l) for l in intro)})')
gems = open(os.path.join(HERE, 'templates/Gemstones.java.tmpl')).read().replace('@@TEXT@@', ';\n'.join(entries))
open(f'{OUT}/Gemstones.java', 'w').write(gems)
print('ok')
