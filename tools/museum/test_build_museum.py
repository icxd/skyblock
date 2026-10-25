#!/usr/bin/env python3
# python3 tools/museum/test_build_museum.py
#
# Checks of build_museum.py on a made-up recording and items API (no Hypixel data needed): reading the menus, matching
# armor sets to the API's, higher tiers and what the report says.
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_museum as bm  # noqa: E402

GUI = '''=== 00:01.0 opened: (1/2) Museum ➜ Combat
  00:01.0 slot 4 := stone_sword x1 &cCombat
      &7Items Donated: &e0&6%
  00:01.0 slot 10 := gray_dye x1 &cWhite Belt
      &7Click on this item in your inventory to
      &7add it to your &9Museum&7!

      &7Donating a &eYellow Belt &7will count as
      &7donating this item.

      &7Reward: &b+1 SkyBlock XP
  00:01.0 slot 11 := gray_dye x1 &cCeleste Armor
      &7Click on an armor piece in your
      &7inventory that belongs to this armor
      &7set to donate the full set to your
      &9Museum&7.

      &7Reward: &b+1 SkyBlock XP
--- 00:01.2 contents of "(1/2) Museum ➜ Combat"
  [10] gray_dye x1 &cNot An Entry
=== 00:02.0 opened: (2/2) Museum ➜ Combat
  00:02.0 slot 10 := gray_dye x1 &cYellow Belt
      &7Click on this item in your inventory to
      &7add it to your &9Museum&7!

      &7Reward: &b+2 SkyBlock XP
  00:02.0 slot 11 := gray_dye x1 &cSeymour's Special Armor
      &7Click on an armor piece in your
      &7inventory that belongs to this armor
      &7set to donate the full set to your
      &9Museum&7.

      &7Reward: &b+3 SkyBlock XP
  00:02.0 slot 12 := gray_dye x1 &cHardened Diamond Armor
      &7Click on an armor piece in your
      &7inventory that belongs to this armor
      &7set to donate the full set to your
      &9Museum&7.

      &7Donating &aMineral Armor &7will give credit for
      &7this armor set.

      &7Reward: &b+4 SkyBlock XP
=== 00:03.0 opened: Museum ➜ Mining
  00:03.0 slot 10 := gray_dye x1 &cMineral Armor
      &7Click on an armor piece in your
      &7inventory that belongs to this armor
      &7set to donate the full set to your
      &9Museum&7.

      &7Reward: &b+5 SkyBlock XP
'''
# A lore's empty line is six spaces in gui.txt (written here as an empty line, which editors keep).
GUI = GUI.replace('\n\n', '\n      \n')


def item(name, worn=None, **museum):
    out = {'name': name}
    if worn:
        out['category'] = worn
    if museum:
        out['museum_data'] = museum
    return out


def api():
    items = {
        'WHITE_BELT': item('White Belt', 'BELT', category='COMBAT', donation_xp=1, parent={'WHITE_BELT': 'YELLOW_BELT'}),
        'YELLOW_BELT': item('Yellow Belt', 'BELT', category='COMBAT', donation_xp=2),
        'CELESTE_HELMET': item('Celeste Helmet', 'HELMET', category='COMBAT', armor_set_donation_xp={'CELESTE': 1}),
        'CELESTE_BOOTS': item('Celeste Boots', 'BOOTS', category='COMBAT', armor_set_donation_xp={'CELESTE': 1}),
        'SEYMOUR_CAP': item('Velvet Top Hat', 'HELMET', category='COMBAT', armor_set_donation_xp={'SEYMOUR': 3}),
        'SEYMOUR_JACKET': item('Cashmere Jacket', 'CHESTPLATE', category='COMBAT', armor_set_donation_xp={'SEYMOUR': 3}),
        'HARDENED_DIAMOND_HELMET': item('Hardened Diamond Helmet', 'HELMET', category='COMBAT',
                                        armor_set_donation_xp={'HARDENED_DIAMOND': 4}, parent={'HARDENED_DIAMOND': 'MINERAL'}),
        'MINERAL_HELMET': item('Mineral Helmet', 'HELMET', category='MINING', armor_set_donation_xp={'MINERAL': 5},
                               mapped_item_ids=['STARRED_MINERAL_HELMET']),
        'LOST_SWORD': item('Lost Sword', 'SWORD', category='COMBAT', donation_xp=9),
        'CAKE_SOUL': {'name': 'Cake Soul', 'museum': True},
    }
    return {'lastUpdated': 1, 'items': [dict(v, id=k) for k, v in items.items()]}


class Builds(unittest.TestCase):
    def setUp(self):
        with tempfile.NamedTemporaryFile('w', suffix='.txt', delete=False, encoding='utf-8') as f:
            f.write(GUI)
            self.path = f.name
        self.data, self.problems = bm.build(bm.menus(self.path), bm.Api(api()))

    def tearDown(self):
        os.unlink(self.path)

    def entries(self, category):
        return next(c['entries'] for c in self.data['categories'] if c['id'] == category)

    def test_pages_in_order_first_open_only(self):
        # The later "contents" snapshot of page 1 isn't another entry.
        self.assertEqual([e['name'] for e in self.entries('COMBAT')],
                         ['White Belt', 'Celeste Armor', 'Yellow Belt', "Seymour's Special Armor", 'Hardened Diamond Armor'])

    def test_items_and_parents(self):
        belt = self.entries('COMBAT')[0]
        self.assertEqual(belt, {'id': 'WHITE_BELT', 'name': 'White Belt', 'xp': 1, 'items': ['WHITE_BELT'],
                                'parent': 'YELLOW_BELT', 'parent_name': '&eYellow Belt',
                                'parent_lines': ['&7Donating a &eYellow Belt &7will count as', '&7donating this item.']})

    def test_sets_by_pieces_words_and_id(self):
        celeste, seymour, hardened = (self.entries('COMBAT')[i] for i in (1, 3, 4))
        self.assertEqual(celeste['id'], 'CELESTE')
        self.assertEqual(celeste['pieces'], [['CELESTE_BOOTS'], ['CELESTE_HELMET']])
        self.assertEqual(seymour['id'], 'SEYMOUR')
        # A set's higher tier may be another category's.
        self.assertEqual((hardened['parent'], hardened['parent_name']), ('MINERAL', '&aMineral Armor'))
        self.assertEqual(self.entries('MINING')[0]['pieces'], [['MINERAL_HELMET', 'STARRED_MINERAL_HELMET']])

    def test_problems_and_special(self):
        self.assertEqual(self.data['special'], ['CAKE_SOUL'])
        self.assertIn("Combat: the API's LOST_SWORD (Lost Sword) isn't in the recording", self.problems)
        self.assertEqual(len(self.problems), 1)

    def test_report(self):
        text = bm.report(self.data, self.problems, 'REC')
        self.assertIn('- Combat: 5 entries (3 sets), 11 SkyBlock XP', text)
        self.assertIn('- All: 6 entries, 16 SkyBlock XP', text)


MILESTONES = '''=== 00:04.0 opened: Museum Milestones
  00:04.0 slot 28 := yellow_stained_glass_pane x1 &aMuseum Milestone 1
      &7Required XP: &e100

      &7Rewards:
      &8+&b1% Bits Multiplier

      &7Progress: &30%

      &cYou can't claim this reward!
--- 00:04.2 contents of "Museum Milestones"
  [28] yellow_stained_glass_pane x1 &aMuseum Milestone 1
      &7Required XP: &e999
  [29] red_stained_glass_pane x2 &aMuseum Milestone 2
      &7Required XP: &e200

      &7Rewards:
      &8+&dDeluxe Bank Upgrade
      &8+&b1% Bits Multiplier

      &cYou can't claim this reward!
'''.replace('\n\n', '\n      \n')


class Milestones(unittest.TestCase):
    def test_from_every_view_first_seen(self):
        with tempfile.NamedTemporaryFile('w', suffix='.txt', delete=False, encoding='utf-8') as f:
            f.write(MILESTONES)
        try:
            found = bm.milestones(f.name)
        finally:
            os.unlink(f.name)
        self.assertEqual(found, [{'milestone': 1, 'xp': 100, 'rewards': ['&8+&b1% Bits Multiplier']},
                                 {'milestone': 2, 'xp': 200, 'rewards': ['&8+&dDeluxe Bank Upgrade', '&8+&b1% Bits Multiplier']}])
        problems = []
        bm.with_milestones({}, problems, found[1:])
        self.assertEqual(problems, ['Milestones: the recording has [2], not 1 on'])


class Words(unittest.TestCase):
    def test_tokens(self):
        self.assertEqual(bm.tokens("Arachne's Armor"), {'ARACHNE'})
        self.assertEqual(bm.tokens('Crimson Hunter Set'), {'CRIMSON', 'HUNTER'})

    def test_clean(self):
        self.assertEqual(bm.clean('%%light_purple%%Cake &aSoul'), 'Cake Soul')


if __name__ == '__main__':
    unittest.main()
