#!/usr/bin/env python3
# python3 tools/hex/build_gems.py --neu NEU-REPO [--api PATH_OR_URL] [--out DIR]
#
# Builds the gemstone table the Gemstone Grinder and gems on items need (see GEMSTONES.md), from public
# sources, into gemstones.json:
#   gems               each gem's stat, and what one gives by its quality and the item's rarity
#                      (NotEnoughUpdates-REPO's constants/gemstones.json), with Citrine doubled (below)
#   removal_costs      the coins a gem of each quality costs to take off an item (the same file)
#   chisel_percentages what a gem in a chisel's slot adds to its Fossil Excavator perk (the same file)
#   chisel_perks       those perks' words, "{}" where the percentage goes (the same file)
#   armor_sets         the armour sets the Museum counts (the items API's museum_data), for the pieces
#                      with gemstone slots: the Gemstone Guide shows a set once
# It's Hypixel's, so it goes to the private data repository (hex/ in it), never into this one. Unlock
# costs aren't here: they're in the item data already (items.json gemstone_slots), and agree with NEU's.
#
# Citrine: NEU still has the values Citrine had before patch 0.23.3 doubled them (the wiki's Gemstone
# history), the same as Peridot's. Every Citrine value on the live auction house (Sept 2026, 11 cells) is
# exactly twice NEU's, so each is doubled here; the cells not seen live are doubled on that rule too.
import argparse
import json
import os
import re
import subprocess
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
API_URL = 'https://api.hypixel.net/v2/resources/skyblock/items'

QUALITIES = ['ROUGH', 'FLAWED', 'FINE', 'FLAWLESS', 'PERFECT']
RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY', 'MYTHIC', 'DIVINE']
GEMS = ['RUBY', 'AMETHYST', 'JADE', 'SAPPHIRE', 'AMBER', 'TOPAZ', 'JASPER', 'OPAL', 'ONYX', 'AQUAMARINE', 'CITRINE', 'PERIDOT']
# What's corrected, and by how much: see the header.
DOUBLED = {'CITRINE'}
ARMOR = ['HELMET', 'CHESTPLATE', 'LEGGINGS', 'BOOTS']


def read_json(source):
    if re.match(r'https?://', source):
        with urllib.request.urlopen(urllib.request.Request(source, headers={'User-Agent': 'skyblock-build-gems'})) as r:
            return json.load(r)
    with open(source, encoding='utf-8') as f:
        return json.load(f)


def number(value):
    """1.0 as 1, so the file reads as the game writes it."""
    return int(value) if float(value).is_integer() else value


def stat_name(neu_name):
    """NEU's "True Defense" as the plugin's Stat, TRUE_DEFENSE."""
    return re.sub(r'[^A-Z]+', '_', neu_name.upper()).strip('_')


def gems(neu):
    data = read_json(os.path.join(neu, 'constants', 'gemstones.json'))
    types = data['gemstoneTypes']
    missing = [g for g in GEMS if g not in types]
    if missing:
        sys.exit('NEU has no ' + ', '.join(missing))
    out = {}
    for gem in GEMS:
        entry = types[gem]
        factor = 2 if gem in DOUBLED else 1
        values = {}
        for quality in QUALITIES:
            by_rarity = entry['stats'][quality]
            unknown = [r for r in by_rarity if r not in RARITIES]
            if unknown:
                sys.exit(f'{gem} {quality}: rarities {unknown}')
            values[quality] = {r: number(by_rarity[r] * factor) for r in RARITIES if r in by_rarity}
        out[gem] = {'stat': stat_name(entry['statName']), 'values': values}
    return data, out


def by_quality(table, what):
    if sorted(table) != sorted(QUALITIES):
        sys.exit(f'{what}: qualities {sorted(table)}')
    return {q: number(table[q]) for q in QUALITIES}


ROMAN = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII', 'XIII', 'XIV', 'XV']


def set_name(set_id):
    """
    The set's name from its id, as the wiki's copy of the guide names most: DIVAN "Divan Armor", FARM_ARMOR
    "Farm Armor", ARMOR_OF_YOG "Armor of Yog", PERFECT_TIER_12 "Perfect Armor - Tier XII". UNKNOWN: Hypixel's
    own names where they aren't the id's ("Divan's Armor").
    """
    tier = re.fullmatch(r'(.+)_TIER_(\d+)', set_id)
    if tier and int(tier.group(2)) < len(ROMAN):
        return set_name(tier.group(1)) + ' - Tier ' + ROMAN[int(tier.group(2))]
    words = ' '.join(w if w in ('of', 'the') else w.capitalize() for w in set_id.lower().split('_'))
    return words if words.startswith('Armor') or words.endswith('Armor') else words + ' Armor'


def armor_sets(api):
    """
    Each Museum armour set with a piece that has gemstone slots: its name and its armour pieces, helmet first. A
    piece the Museum counts in two sets (the Blaze Armor's, in Crimson Hunter's too) is its own set's, the one its
    id starts with, else the first by id.
    """
    data = read_json(api)
    items = data['items'] if isinstance(data, dict) else data
    sets = {}
    for item in items:
        museum = item.get('museum_data') or {}
        if item.get('category') not in ARMOR:
            continue
        for set_id in (museum.get('armor_set_donation_xp') or {}):
            sets.setdefault(set_id, []).append(item)
    owner = {}
    for set_id in sorted(sets):
        for piece in sets[set_id]:
            mine = piece['id'].startswith(set_id + '_')
            if piece['id'] not in owner or mine and not piece['id'].startswith(owner[piece['id']] + '_'):
                owner[piece['id']] = set_id
    out = {}
    for set_id, pieces in sorted(sets.items()):
        pieces = [p for p in pieces if owner[p['id']] == set_id]
        if not any(p.get('gemstone_slots') for p in pieces):
            continue
        pieces.sort(key=lambda p: (ARMOR.index(p['category']), p['id']))
        out[set_id] = {'name': set_name(set_id), 'pieces': [p['id'] for p in pieces]}
    # The rest (Kuudra's tiers, most Perfect Armor tiers: the Museum doesn't count them) by their ids without
    # the piece: HOT_AURORA_HELMET and HOT_AURORA_BOOTS are HOT_AURORA, named as the helmet with "Armor" for
    # "Helmet" ("Hot Aurora Armor"; the wiki's copy has "Aurora Armor" for every tier: UNKNOWN).
    groups = {}
    for item in items:
        if item.get('category') in ARMOR and item.get('gemstone_slots') and item['id'] not in owner:
            key = re.sub(r'_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)(?=_|$)', '', item['id'])
            if key != item['id']:
                groups.setdefault(key, []).append(item)
    for key, pieces in sorted(groups.items()):
        if len(pieces) < 2 or key in out:
            continue
        pieces.sort(key=lambda p: (ARMOR.index(p['category']), p['id']))
        first = re.sub(r'§.', '', pieces[0]['name'])
        name = re.sub(r'\b(Helmet|Chestplate|Leggings|Boots)\b', 'Armor', first, count=1)
        out[key] = {'name': name if name != first else set_name(key), 'pieces': [p['id'] for p in pieces]}
    return data.get('lastUpdated') if isinstance(data, dict) else None, dict(sorted(out.items()))


def neu_commit(neu):
    try:
        return subprocess.run(['git', '-C', neu, 'rev-parse', 'HEAD'], capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return None


def build(neu, api):
    raw, table = gems(neu)
    updated, sets = armor_sets(api)
    perks = {g: raw['gemstoneTypes'][g]['chiselBonus'] for g in GEMS if 'chiselBonus' in raw['gemstoneTypes'][g]}
    return {
        'format': 1,
        'about': "Gemstones: each gem's stat by quality and item rarity, the removal fee and chisel perk by quality, "
                 'and the Museum armour sets the Gemstone Guide shows once. From NotEnoughUpdates-REPO '
                 'constants/gemstones.json and ' + API_URL + ' (museum_data).',
        'source': {'neu_commit': neu_commit(neu), 'api_last_updated': updated},
        'corrections': ['CITRINE: every value doubled. NEU has the values from before 0.23.3 doubled them; the 11 cells '
                        'seen on the live auction house (Sept 2026) are all exactly twice NEU\'s.'],
        'gems': table,
        'removal_costs': by_quality(raw['removalCosts'], 'removalCosts'),
        'chisel_percentages': by_quality(raw['chiselPercentages'], 'chiselPercentages'),
        'chisel_perks': perks,
        'armor_sets': sets,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO checkout')
    parser.add_argument('--api', default=API_URL, help='the items API response, a file or the URL')
    parser.add_argument('--out', default=os.path.join(os.path.dirname(ROOT), 'skyblock-dungeon-data', 'hex'))
    args = parser.parse_args()
    data = build(args.neu, args.api)
    os.makedirs(args.out, exist_ok=True)
    path = os.path.join(args.out, 'gemstones.json')
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write('\n')
    print('wrote', path, '-', len(data['gems']), 'gems,', len(data['armor_sets']), 'armour sets')


if __name__ == '__main__':
    main()
