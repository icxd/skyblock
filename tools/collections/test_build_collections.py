#!/usr/bin/env python3
# python3 tools/collections/test_build_collections.py
#
# Checks of build_collections.py's parsing on made-up input (no Hypixel data needed): the wiki's Lua data,
# the API's unlock lines, the slayer pages' tables and the enchanted items' second shape.
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_collections as bc  # noqa: E402


class FakeNeu:
    def __init__(self, recipes=None, crafttext=None):
        self._recipes = recipes or {}
        self._crafttext = crafttext or {}
        self.by_name = {}

    def recipes(self, item_id):
        return self._recipes.get(item_id, [])

    def crafttext(self, item_id):
        return self._crafttext.get(item_id, '')

    def texture(self, item_id):
        return None


class FakeWiki:
    def __init__(self, pages):
        self.pages = pages

    def page(self, title):
        if title not in self.pages:
            raise OSError('no page ' + title)
        return self.pages[title]


def builder(items, neu=None, wiki=None, api_items=None):
    api = {'collections': {c: {'items': {}} for c, _ in bc.CATEGORIES}}
    for category, entries in (api_items or {}).items():
        api['collections'][category]['items'] = entries
    return bc.Builder(api, items, neu or FakeNeu(), wiki or FakeWiki({}), {'': '☘'}, {})


class Basics(unittest.TestCase):
    def test_roman(self):
        self.assertEqual([bc.roman(r) for r in ('I', 'IV', 'IX', 'XIV', 'XL')], [1, 4, 9, 14, 40])
        self.assertEqual([bc.to_roman(n) for n in (1, 4, 9, 14, 40)], ['I', 'IV', 'IX', 'XIV', 'XL'])

    def test_lua_table(self):
        data = bc.Lua.first_table('local data = {\n  ["Bonzo"] = { [1] = { required = 25, reward = { {"Red Nose", type = "Reward"} } } },\n'
                                  '  -- a comment\n  list = { "a", \'b\', 3, true, nil },\n}\nreturn data')
        self.assertEqual(data['Bonzo'][1]['required'], 25)
        self.assertEqual(data['Bonzo'][1]['reward'][1], {1: 'Red Nose', 'type': 'Reward'})
        self.assertEqual(data['list'], {1: 'a', 2: 'b', 3: 3, 4: True, 5: None})

    def test_enchanted_shapes(self):
        plus = ['', 'ROTTEN_FLESH:32', '', 'ROTTEN_FLESH:32', 'ROTTEN_FLESH:32', 'ROTTEN_FLESH:32', '', 'ROTTEN_FLESH:32', '']
        shapes = bc.enchanted_shapes([{'cells': plus, 'count': 1}])
        self.assertEqual(len(shapes), 2)
        self.assertEqual(shapes[1]['cells'], ['ROTTEN_FLESH:32'] * 5 + [''] * 4)
        # Anything else keeps its one shape.
        other = ['IRON_INGOT:1'] * 9
        self.assertEqual(bc.enchanted_shapes([{'cells': other, 'count': 1}]), [{'cells': other, 'count': 1}])


class Rewards(unittest.TestCase):
    def setUp(self):
        items = {'ENCHANTED_BLAZE_POWDER': {'name': 'Enchanted Blaze Powder'}, 'BLAZE_GENERATOR_1': {'name': 'Blaze Minion I'}}
        neu = FakeNeu()
        neu.by_name = {'Blaze Minion I': ['BLAZE_GENERATOR_1']}
        self.b = builder(items, neu)

    def test_unlock_lines(self):
        r = self.b.reward
        self.assertEqual(r('+4 SkyBlock XP', 'Blaze Rod', 'I'), {'type': 'SKYBLOCK_XP', 'amount': 4})
        self.assertEqual(r('+10,000 Combat Experience', 'Rotten Flesh', 'IX'), {'type': 'SKILL_XP', 'skill': 'COMBAT', 'amount': 10000})
        self.assertEqual(r('Fire Aspect Exp Discount (-25%)', 'Blaze Rod', 'II'), {'type': 'EXP_DISCOUNT', 'name': 'Fire Aspect', 'percent': 25})
        self.assertEqual(r('+3 Quiver Slots', 'String', 'IV'), {'type': 'SLOTS', 'name': 'Quiver', 'amount': 3})
        self.assertEqual(r('Blaze Minion Recipes', 'Blaze Rod', 'I'), {'type': 'MINION_RECIPES', 'name': 'Blaze', 'item': 'BLAZE_GENERATOR_1'})
        self.assertEqual(r('Enchanted Blaze Powder Recipe', 'Blaze Rod', 'III'),
                         {'type': 'RECIPE', 'name': 'Enchanted Blaze Powder', 'item': 'ENCHANTED_BLAZE_POWDER'})
        self.assertEqual(r('[Lvl 1] Blaze Recipe', 'Blaze Rod', 'VI'), {'type': 'PET_RECIPE', 'name': 'Blaze'})
        self.assertEqual(r('+1 Mining Fortune', 'Obsidian', 'V'),
                         {'type': 'STAT', 'name': 'MINING_FORTUNE', 'amount': 1, 'text': '+1☘ Mining Fortune'})
        self.assertEqual(r('Quiver', 'String', 'III'), {'type': 'UNLOCK', 'text': 'Quiver'})

    def test_a_recipe_naming_no_item_is_reported(self):
        self.assertEqual(self.b.reward('Mana Potion Recipe', 'Raw Mutton', 'IV'), {'type': 'RECIPE', 'name': 'Mana Potion'})
        self.assertEqual(self.b.problems['recipes naming no item in items.json'], ['Mana Potion (Raw Mutton IV)'])


SLAYER_PAGE = '''== Leveling Rewards ==
{| class="wikitable ct"
|-
| rowspan="2" |{{Text anchor|I|1}}
| rowspan="2" |{{Pink|5}}
| rowspan="2" |Noob
| rowspan="2" |{{stat|hp|+2}}<br>{{SBXP|15}}
| rowspan="2" |{{Slot|Wand of Healing}}
|{{Slot|Revenant Flesh, 1-3; Revenant Flesh, 9-18}}
|-
|{{Text anchor|III|3}}
|{{Pink|200}}
|Skilled
|{{stat|hp|+3}}
|{{Slot|Revenant Falchion}}{{slot|Wisp Upgrade Stone (Epic)}}{{Slot|Revenant Minion}}
|{{slot|Undead Catalyst}}
|}
'''


class Slayers(unittest.TestCase):
    def test_items_unlocked_by_level(self):
        items = {'WAND_OF_HEALING': {'name': 'Wand of Healing'}, 'REVENANT_SWORD': {'name': 'Revenant Falchion'},
                 'REVENANT_FLESH': {'name': 'Revenant Flesh'}, 'UNDEAD_CATALYST': {'name': 'Undead Catalyst'},
                 'UPGRADE_STONE_FROST': {'name': 'Wisp Upgrade Stone', 'rarity': 'RARE'},
                 'UPGRADE_STONE_GLACIAL': {'name': 'Wisp Upgrade Stone', 'rarity': 'EPIC'}}
        b = builder(items, wiki=FakeWiki({'Zombie Slayer': SLAYER_PAGE}))
        # The Items Unlocked column only (not the boss drops), minions left out, a rarity told apart.
        self.assertEqual(b.slayer_unlocks(), {'WAND_OF_HEALING': ('ZOMBIE', 1), 'REVENANT_SWORD': ('ZOMBIE', 3),
                                              'UPGRADE_STONE_GLACIAL': ('ZOMBIE', 3)})
        # Only where nothing else says: a collection tier, then NEU's crafttext.
        self.assertEqual(b.requirement('REVENANT_SWORD', {}), {'slayer': 'ZOMBIE', 'level': 3})
        self.assertEqual(b.requirement('REVENANT_SWORD', {'REVENANT_SWORD': ('ROTTEN_FLESH', 4)}),
                         {'collection': 'ROTTEN_FLESH', 'tier': 4})
        self.assertIsNone(b.requirement('UNDEAD_CATALYST', {}))
        self.assertEqual(b.problems['wiki pages'], ['Spider Slayer: no page Spider Slayer', 'Wolf Slayer: no page Wolf Slayer',
                                                    'Enderman Slayer: no page Enderman Slayer', 'Blaze Slayer: no page Blaze Slayer',
                                                    'Vampire Slayer: no page Vampire Slayer'])

    def test_crafttext(self):
        neu = FakeNeu(crafttext={'X': 'Requires: Blaze Rod IV', 'Y': 'Requires: Wolf Slayer 7', 'Z': 'Requires: a Museum rank'})
        b = builder({}, neu, api_items={'COMBAT': {'BLAZE_ROD': {'name': 'Blaze Rod', 'tiers': []}}})
        self.assertEqual(b.requirement('X', {}), {'collection': 'BLAZE_ROD', 'tier': 4})
        self.assertEqual(b.requirement('Y', {}), {'slayer': 'WOLF', 'level': 7})
        self.assertEqual(b.requirement('Z', {}), {'other': 'Requires: a Museum rank'})


if __name__ == '__main__':
    unittest.main()
