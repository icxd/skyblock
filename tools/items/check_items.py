#!/usr/bin/env python3
# python3 tools/items/check_items.py [ITEMS_JSON] [--paper-api JAR]
#
# Checks an items.json against format 1 (README.md): the one-item-per-line layout with sorted keys, every
# field's type, no default values, 26.2 item materials, and no § or private-use glyph left in any text.
# Prints what's wrong and exits 1 if anything is.
import argparse
import collections
import json
import os
import re
import sys

sys.dont_write_bytecode = True  # no __pycache__ in the repository
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_items  # noqa: E402  (the materials come from the same jar reader)

BLOCK_KINDS = {'ABILITY', 'FULL_SET', 'PIECE', 'TIERED', 'EXTRA', 'SHORTBOW'}
BLOCK_NUMBERS = {'mana', 'mana_percent', 'cooldown', 'soulflow', 'health_cost', 'vitality'}
FLAGS = {'glowing', 'unstackable', 'dungeon_item', 'can_have_attributes', 'can_have_power_scroll'}


def is_number(v):
    return isinstance(v, (int, float)) and not isinstance(v, bool)


def check_cost(cost, where, problem):
    if not isinstance(cost, dict):
        return problem(f'{where}: not an object')
    keys = set(cost)
    if keys == {'coins'}:
        ok = is_number(cost['coins'])
    elif keys == {'item', 'amount'}:
        ok = isinstance(cost['item'], str) and is_number(cost['amount'])
    elif keys == {'essence', 'amount'}:
        ok = isinstance(cost['essence'], str) and is_number(cost['amount'])
    else:
        ok = False
    if not ok:
        problem(f'{where}: not a cost {cost}')


def check_block(block, where, problem):
    if not isinstance(block, dict):
        return problem(f'{where}: not an object')
    unknown = set(block) - {'kind', 'name', 'header', 'activation', 'text', 'pieces'} - BLOCK_NUMBERS
    if unknown:
        problem(f'{where}: unknown fields {sorted(unknown)}')
    if block.get('kind') not in BLOCK_KINDS:
        problem(f'{where}: kind {block.get("kind")!r}')
    for field in ('name', 'header'):
        if not isinstance(block.get(field), str) or not block[field]:
            problem(f'{where}: {field} is not a string')
    if block.get('kind') == 'ABILITY' and not isinstance(block.get('activation'), str):
        problem(f'{where}: an ABILITY without an activation')
    if 'activation' in block and block.get('kind') != 'ABILITY':
        problem(f'{where}: activation on a {block.get("kind")}')
    if 'text' in block and not (isinstance(block['text'], list) and block['text'] and all(isinstance(s, str) for s in block['text'])):
        problem(f'{where}: text is not a list of strings')
    for field in BLOCK_NUMBERS & set(block):
        if not is_number(block[field]) or block[field] == 0:
            problem(f'{where}: {field} {block[field]!r} is not a number other than 0')
    if 'pieces' in block and not (isinstance(block['pieces'], int) and not isinstance(block['pieces'], bool) and block['pieces'] > 0):
        problem(f'{where}: pieces {block["pieces"]!r}')


def check_item(item_id, item, materials, problem):
    def p(msg):
        problem(f'{item_id}: {msg}')

    def strings(v):
        return isinstance(v, list) and v and all(isinstance(s, str) for s in v)

    known = {'name', 'material', 'rarity', 'type', 'type_label', 'categories', 'texture', 'skin', 'color', 'reforgeable',
             'soulbound', 'gear_score', 'npc_sell_price', 'stats', 'shot_cooldown', 'gemstone_slots', 'upgrade_costs',
             'requirements', 'lore', 'abilities', 'can_recombobulate', 'dungeon_conversion_cost'} | FLAGS
    if set(item) - known:
        p(f'unknown fields {sorted(set(item) - known)}')
    if not isinstance(item.get('name'), str) or not item['name']:
        p('no name')
    if item.get('material') not in materials:
        p(f'material {item.get("material")!r} is not a 26.2 item material')
    if 'rarity' in item and (not isinstance(item['rarity'], str) or item['rarity'] == 'COMMON'):
        p(f'rarity {item["rarity"]!r} (COMMON is left out)')
    for field in ('type', 'skin'):
        if field in item and (not isinstance(item[field], str) or not item[field]):
            p(f'{field} {item[field]!r}')
    if 'type_label' in item and not isinstance(item['type_label'], str):
        p(f'type_label {item["type_label"]!r}')
    if 'texture' in item and not re.fullmatch(r'[0-9a-f]{1,64}', str(item['texture'])):
        p(f'texture {item["texture"]!r} is not a hex hash')
    if 'texture' in item and 'skin' in item:
        p('both texture and skin')
    if 'color' in item and not re.fullmatch(r'#[0-9a-f]{6}', str(item['color'])):
        p(f'color {item["color"]!r}')
    for field in FLAGS & set(item):
        if item[field] is not True:
            p(f'{field} {item[field]!r} (false is left out)')
    if 'can_recombobulate' in item and item['can_recombobulate'] is not False:
        p(f'can_recombobulate {item["can_recombobulate"]!r} (true is left out)')
    if 'dungeon_conversion_cost' in item:
        costs = item['dungeon_conversion_cost']
        if not isinstance(costs, list) or not costs:
            p('dungeon_conversion_cost is not a list')
        for k, cost in enumerate(costs if isinstance(costs, list) else []):
            check_cost(cost, f'{item_id}: dungeon_conversion_cost[{k}]', problem)
    if 'reforgeable' in item and not isinstance(item['reforgeable'], bool):
        p(f'reforgeable {item["reforgeable"]!r}')
    if 'soulbound' in item and item['soulbound'] not in ('COOP', 'SOLO'):
        p(f'soulbound {item["soulbound"]!r}')
    if 'gear_score' in item and not (isinstance(item['gear_score'], int) and not isinstance(item['gear_score'], bool) and item['gear_score'] > 0):
        p(f'gear_score {item["gear_score"]!r}')
    for field in ('npc_sell_price', 'shot_cooldown'):
        if field in item and (not is_number(item[field]) or item[field] == 0):
            p(f'{field} {item[field]!r}')
    if 'stats' in item:
        s = item['stats']
        if not isinstance(s, dict) or not s or not all(re.fullmatch(r'[A-Z][A-Z0-9_]*', k) and is_number(v) and v != 0 for k, v in s.items()):
            p(f'stats {s!r}')
    for field in ('categories', 'lore'):
        if field in item and not strings(item[field]):
            p(f'{field} is not a list of strings')
    if 'gemstone_slots' in item:
        slots = item['gemstone_slots']
        if not isinstance(slots, list) or not slots:
            p('gemstone_slots is not a list')
        for n, slot in enumerate(slots if isinstance(slots, list) else []):
            if not isinstance(slot, dict) or set(slot) - {'type', 'costs'} or not isinstance(slot.get('type'), str):
                p(f'gemstone_slots[{n}] {slot!r}')
            elif 'costs' in slot:
                if not isinstance(slot['costs'], list) or not slot['costs']:
                    p(f'gemstone_slots[{n}].costs is not a list')
                for k, cost in enumerate(slot['costs'] or []):
                    check_cost(cost, f'{item_id}: gemstone_slots[{n}].costs[{k}]', problem)
    if 'upgrade_costs' in item:
        stars = item['upgrade_costs']
        if not isinstance(stars, list) or not stars or not all(isinstance(star, list) and star for star in stars):
            p('upgrade_costs is not a list of lists')
        else:
            for n, star in enumerate(stars):
                for k, cost in enumerate(star):
                    check_cost(cost, f'{item_id}: upgrade_costs[{n}][{k}]', problem)
    if 'requirements' in item:
        reqs = item['requirements']
        if not isinstance(reqs, list) or not reqs or not all(isinstance(r, dict) and isinstance(r.get('type'), str) for r in reqs):
            p(f'requirements {reqs!r}')
    if 'abilities' in item:
        blocks = item['abilities']
        if not isinstance(blocks, list) or not blocks:
            p('abilities is not a list')
        for n, block in enumerate(blocks if isinstance(blocks, list) else []):
            check_block(block, f'{item_id}: abilities[{n}]', problem)


def strings_in(v):
    if isinstance(v, str):
        yield v
    elif isinstance(v, dict):
        for k, x in v.items():
            yield k
            yield from strings_in(x)
    elif isinstance(v, list):
        for x in v:
            yield from strings_in(x)


def main():
    ap = argparse.ArgumentParser(description='Check an items.json against format 1.')
    ap.add_argument('items', nargs='?', default=os.path.join(build_items.default_out(), 'items.json'))
    ap.add_argument('--paper-api', default=None)
    args = ap.parse_args()
    materials = build_items.item_materials(args.paper_api or build_items.paper_api_jar())
    problems = []
    problem = problems.append
    with open(args.items, encoding='utf-8', newline='') as f:
        raw = f.read()

    # the layout: a head line, one item a line (sorted by id, keys sorted, compact), then "}}"
    lines = raw.split('\n')
    if lines[-1] != '' or '\r' in raw:
        problem('the file does not end in one \\n, or has \\r')
    lines = lines[:-1]
    head = re.fullmatch(r'\{"format":1,"source":(\{.*?\}),"items":\{', lines[0] if lines else '')
    if not head:
        problem('the first line is not {"format":1,"source":{...},"items":{')
    if not lines or lines[-1] != '}}':
        problem('the last line is not }}')
    ids = []
    for n, line in enumerate(lines[1:-1], 2):
        m = re.fullmatch(r'("(?:[^"\\]|\\.)*"):(\{.*\})(,?)', line)
        if not m:
            problem(f'line {n} is not one item')
            continue
        item_id, body = json.loads(m.group(1)), m.group(2)
        if (m.group(3) == ',') != (n < len(lines) - 1):
            problem(f'line {n}: a comma where there should be none, or none where there should be one')
        if json.dumps(json.loads(body), ensure_ascii=False, sort_keys=True, separators=(',', ':')) != body:
            problem(f'line {n} ({item_id}): not written with sorted keys and no spaces')
        ids.append(item_id)
    if ids != sorted(ids):
        problem('the items are not sorted by id')
    if len(ids) != len(set(ids)):
        problem('an id is listed twice')

    doc = json.loads(raw)
    if set(doc) != {'format', 'source', 'items'} or doc['format'] != 1:
        problem(f'top level {sorted(doc)} / format {doc.get("format")!r}')
    source = doc.get('source', {})
    if not (set(source) == {'api_last_updated', 'neu_commit'} and isinstance(source['api_last_updated'], int)
            and re.fullmatch(r'[0-9a-f]{7,40}', str(source['neu_commit']))):
        problem(f'source {source!r}')
    for item_id, item in doc['items'].items():
        check_item(item_id, item, materials, problem)
    texts = list(strings_in(doc['items']))
    if any('§' in s for s in texts):
        problem('a § is left in the text (codes are &)')
    left = sorted({'%04X' % ord(c) for s in texts for c in s if '\ue000' <= c <= '\uf8ff'})
    if left:
        problem(f'private-use glyphs left: {left}')

    fields = collections.Counter(k for item in doc['items'].values() for k in item)
    kinds = collections.Counter(b['kind'] for item in doc['items'].values() for b in item.get('abilities', []))
    print(f'{args.items}: {len(doc["items"])} items, {len(raw.encode("utf-8")):,} bytes')
    print('fields:', ', '.join(f'{k} {v}' for k, v in sorted(fields.items())))
    print('blocks:', ', '.join(f'{k} {v}' for k, v in sorted(kinds.items())))
    for msg in problems[:50]:
        print('PROBLEM', msg)
    print(f'{len(problems)} problems')
    sys.exit(1 if problems else 0)


if __name__ == '__main__':
    main()
