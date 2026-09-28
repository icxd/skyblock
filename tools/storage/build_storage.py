#!/usr/bin/env python3
# python3 tools/storage/build_storage.py --neu NEU-REPO [--api PATH_OR_URL] [--out DIR]
#
# Builds two of the plugin's storage data files (see STORAGE.md) from public sources:
#   accessories.json  which accessories upgrade into which (NotEnoughUpdates-REPO's constants/misc.json,
#                     "talisman_upgrades"): only the highest of a line counts in the Accessory Bag.
#   bags.json         how big each bag is at each tier of its collection (Hypixel's collections API: the
#                     "Potion Bag", "+9 Potion Bag Slots", ... unlocks), with the size a bag starts at,
#                     which the API doesn't say (the wiki's bag pages).
# Both are Hypixel's, so they go to the private data repository (storage/ in it), never into this one.
# powers.json, the third file, comes from the wiki and a recording, by a script kept there: see STORAGE.md.
import argparse
import json
import os
import re
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, '..', '..'))
API_URL = 'https://api.hypixel.net/v2/resources/skyblock/collections'

# Each bag: the words its collection unlocks name it by, and how many slots unlocking it gives (the
# wiki's Potion Bag, Fishing Bag and Sack of Sacks pages; the API only says "Potion Bag"). The
# Accessory Bag has 9 for everyone (the wiki's Accessory Bag, since 2026/Jul 22) before its collection.
BAGS = {
    'ACCESSORY_BAG': ('Accessory Bag', 9),
    'POTION_BAG': ('Potion Bag', 9),
    'FISHING_BAG': ('Fishing Bag', 9),
    'SACK_OF_SACKS': ('Small Sack of Sacks', 8),
}


def read_json(source):
    if re.match(r'https?://', source):
        with urllib.request.urlopen(urllib.request.Request(source, headers={'User-Agent': 'skyblock-build-storage'})) as r:
            return json.load(r)
    with open(source, encoding='utf-8') as f:
        return json.load(f)


def accessories(neu):
    upgrades = read_json(os.path.join(neu, 'constants', 'misc.json'))['talisman_upgrades']
    return {'about': "Each accessory's higher tiers, from NotEnoughUpdates-REPO constants/misc.json talisman_upgrades.",
            'upgrades': {k: list(v) for k, v in sorted(upgrades.items())}}


def bags(api):
    """For each bag: its collection, the tier that unlocks it, and the slots each tier adds."""
    data = read_json(api)
    out = {}
    for category in data['collections'].values():
        for collection, c in category['items'].items():
            for tier in c['tiers']:
                for unlock in tier['unlocks']:
                    for bag, (words, first) in BAGS.items():
                        entry = out.setdefault(bag, {'collection': None, 'unlock': 0, 'base': first, 'slots': {}})
                        more = re.fullmatch(r'\+(\d+) ' + re.escape(words) + ' Slots', unlock)
                        if unlock == words or more:
                            if entry['collection'] not in (None, collection):
                                sys.exit(f'{bag} is unlocked by both {entry["collection"]} and {collection}')
                            entry['collection'] = collection
                        if unlock == words:
                            entry['unlock'] = tier['tier']
                        elif more:
                            entry['slots'][str(tier['tier'])] = int(more.group(1))
    for bag, entry in out.items():
        if entry['collection'] is None:
            sys.exit(f'no collection unlocks the {bag}')
    return {'about': 'Bag sizes by collection tier, from ' + API_URL + ' (lastUpdated ' + str(data.get('lastUpdated')) + '); '
                     'base is the size a bag has once unlocked (unlock 0: from the start), slots what each tier adds.',
            'bags': out}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--neu', required=True, help='a NotEnoughUpdates-REPO checkout')
    parser.add_argument('--api', default=API_URL, help='the collections API response, a file or the URL')
    parser.add_argument('--out', default=os.path.join(os.path.dirname(ROOT), 'skyblock-dungeon-data', 'storage'))
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    for name, data in (('accessories.json', accessories(args.neu)), ('bags.json', bags(args.api))):
        path = os.path.join(args.out, name)
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write('\n')
        print('wrote', path)


if __name__ == '__main__':
    main()
