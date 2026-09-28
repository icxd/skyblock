#!/usr/bin/env python3
# python3 tools/hex/test_build_gems.py
#
# Checks of build_gems.py on made-up input (no Hypixel data needed): the stat table with Citrine doubled, the
# fees by quality, and the armour sets the Gemstone Guide shows once.
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_gems as bg  # noqa: E402

QUALITIES = {'ROUGH': 1, 'FLAWED': 100, 'FINE': 10000, 'FLAWLESS': 100000, 'PERFECT': 500000}


def neu_repo(folder):
    stats = {q: {'COMMON': 1, 'UNCOMMON': 1.5, 'MYTHIC': 3} for q in QUALITIES}
    types = {g: {'statName': 'True Defense', 'stats': stats} for g in bg.GEMS}
    types['AMBER'] = {'statName': 'Mining Speed', 'stats': {q: {'MYTHIC': 45, 'DIVINE': 54} for q in QUALITIES}}
    types['PERIDOT']['chiselBonus'] = '§7Gain §a+{}% §fFossil Essence'
    os.makedirs(os.path.join(folder, 'constants'))
    with open(os.path.join(folder, 'constants', 'gemstones.json'), 'w') as f:
        json.dump({'gemstoneTypes': types, 'removalCosts': QUALITIES,
                   'chiselPercentages': {'ROUGH': 30, 'FLAWED': 40, 'FINE': 50, 'FLAWLESS': 60, 'PERFECT': 100}}, f)


def piece(item_id, category, name, sets=None, slots=True):
    item = {'id': item_id, 'category': category, 'name': name}
    if sets:
        item['museum_data'] = {'armor_set_donation_xp': {s: 10 for s in sets}}
    if slots:
        item['gemstone_slots'] = [{'slot_type': 'COMBAT'}]
    return item


class Build(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.neu = os.path.join(self.tmp.name, 'neu')
        neu_repo(self.neu)
        items = [
            piece('DIVAN_BOOTS', 'BOOTS', 'Boots of Divan', ['DIVAN']),
            piece('DIVAN_HELMET', 'HELMET', 'Helmet of Divan', ['DIVAN']),
            # The Museum counts the Blaze Armor in Crimson Hunter's set too.
            piece('BLAZE_HELMET', 'HELMET', 'Blaze Helmet', ['BLAZE', 'CRIMSON_HUNTER']),
            piece('ARMOR_OF_YOG_HELMET', 'HELMET', 'Yog Helmet', ['ARMOR_OF_YOG']),
            piece('PERFECT_HELMET_12', 'HELMET', 'Perfect Helmet - Tier XII', ['PERFECT_TIER_12']),
            piece('NO_SLOTS_HELMET', 'HELMET', 'No Slots Helmet', ['NO_SLOTS'], slots=False),
            # No Museum set: grouped by id.
            piece('HOT_AURORA_HELMET', 'HELMET', 'Hot Aurora Helmet'),
            piece('HOT_AURORA_BOOTS', 'BOOTS', 'Hot Aurora Boots'),
            piece('LONE_HELMET', 'HELMET', 'Lone Helmet'),
            piece('HYPERION', 'SWORD', 'Hyperion'),
        ]
        self.api = os.path.join(self.tmp.name, 'items.json')
        with open(self.api, 'w') as f:
            json.dump({'lastUpdated': 5, 'items': items}, f)
        self.data = bg.build(self.neu, self.api)

    def tearDown(self):
        self.tmp.cleanup()

    def test_gems(self):
        ruby = self.data['gems']['RUBY']
        self.assertEqual(ruby['stat'], 'TRUE_DEFENSE')
        self.assertEqual(ruby['values']['FINE'], {'COMMON': 1, 'UNCOMMON': 1.5, 'MYTHIC': 3})
        self.assertEqual(self.data['gems']['AMBER']['values']['PERFECT'], {'MYTHIC': 45, 'DIVINE': 54})
        # Citrine is doubled; Peridot, which NEU has the same, isn't.
        self.assertEqual(self.data['gems']['CITRINE']['values']['FINE'], {'COMMON': 2, 'UNCOMMON': 3, 'MYTHIC': 6})
        self.assertEqual(self.data['gems']['PERIDOT']['values']['FINE'], {'COMMON': 1, 'UNCOMMON': 1.5, 'MYTHIC': 3})

    def test_by_quality(self):
        self.assertEqual(self.data['removal_costs'], QUALITIES)
        self.assertEqual(self.data['chisel_percentages']['PERFECT'], 100)
        self.assertEqual(self.data['chisel_perks'], {'PERIDOT': '§7Gain §a+{}% §fFossil Essence'})

    def test_armor_sets(self):
        sets = self.data['armor_sets']
        self.assertEqual(sets['DIVAN'], {'name': 'Divan Armor', 'pieces': ['DIVAN_HELMET', 'DIVAN_BOOTS']})
        self.assertEqual(sets['BLAZE']['pieces'], ['BLAZE_HELMET'])
        self.assertNotIn('CRIMSON_HUNTER', sets)
        self.assertEqual(sets['ARMOR_OF_YOG']['name'], 'Armor of Yog')
        self.assertEqual(sets['PERFECT_TIER_12']['name'], 'Perfect Armor - Tier XII')
        self.assertNotIn('NO_SLOTS', sets)
        self.assertEqual(sets['HOT_AURORA'], {'name': 'Hot Aurora Armor', 'pieces': ['HOT_AURORA_HELMET', 'HOT_AURORA_BOOTS']})
        self.assertNotIn('LONE', sets)
        self.assertEqual(self.data['source']['api_last_updated'], 5)

    def test_names(self):
        self.assertEqual(bg.stat_name('Crit Damage'), 'CRIT_DAMAGE')
        self.assertEqual(bg.set_name('FARM_ARMOR'), 'Farm Armor')
        self.assertEqual(bg.set_name('LAVA_SEA_CREATURE'), 'Lava Sea Creature Armor')


if __name__ == '__main__':
    unittest.main()
