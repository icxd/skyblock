#!/usr/bin/env python3
# python3 tools/hex/test_build_reforges.py
#
# Checks of build_reforges.py on made-up input (no Hypixel data needed): modifier ids, the wiki's Lua module and
# tables, and how NEU's numbers, the wiki's and what live items show are put together.
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_reforges as br  # noqa: E402

STATS = ['HEALTH', 'DEFENSE', 'STRENGTH', 'CRIT_CHANCE', 'CRIT_DAMAGE', 'ATTACK_SPEED', 'INTELLIGENCE', 'FORAGING_WISDOM', 'SPEED']
DISPLAY = {'Health': 'HEALTH', 'Defense': 'DEFENSE', 'Strength': 'STRENGTH', 'Crit Chance': 'CRIT_CHANCE',
           'Crit Damage': 'CRIT_DAMAGE', 'Attack Speed': 'ATTACK_SPEED', 'Intelligence': 'INTELLIGENCE',
           'Foraging Wisdom': 'FORAGING_WISDOM', 'Speed': 'SPEED'}


class FakeSources:
    def __init__(self, basic=None, stones=None, wiki=None, live=None, stone_lore=None):
        self.basic = basic or {}
        self.stones = stones or {}
        self.wiki = wiki or {}
        self.live = live or {}
        self.stats = STATS
        self.display = DISPLAY
        self.glyphs = {'\ue00d': '❁'}
        self.prices_page = ''
        self.reforging_page = ''
        self._stone_lore = stone_lore or {}

    def stone_bonus(self, stone_id):
        return self._stone_lore.get(stone_id)


class Names(unittest.TestCase):
    def test_modifier_ids(self):
        self.assertEqual('double_bit', br.modifier_of('Double-Bit'))
        self.assertEqual('deep_fried', br.modifier_of('Deep Fried'))
        self.assertEqual('jerrys', br.modifier_of("Jerry's"))
        self.assertEqual('lumberjack', br.modifier_of("Lumberjack's", 'lumberjack'))

    def test_plain(self):
        self.assertEqual('Grants +1 Strength per Catacombs level.', br.plain('&7Grants &a+1 &c❁ Strength &7per §cCatacombs \ue00d level.'))


class Wiki(unittest.TestCase):
    def test_module(self):
        module = """--[[Quick Copy
            [''] = { stats = { c = { cc = , }, }, }
        --]]
        return {
            ['Armor'] = {
                ['Test'] = { stats = { c = { str = 1., hp = 2 } }, source = 'Test Stone', costs = {c = 5} },
            },
        }"""
        self.assertEqual({'Armor': {'Test': {'stats': {'c': {'str': 1.0, 'hp': 2}}, 'source': 'Test Stone', 'costs': {'c': 5}}}},
                         br.module_table(module))

    def test_table_with_row_spans(self):
        table = '''{| class="wikitable"
! A
! B
! C
|-
! rowspan="2" |Very
| Wise
| [[Wise Test Armor]]
|-
| Strong †
| [[Strong Test Armor]]
|-
! rowspan="2" |Even More
| rowspan="2" |Refined
| [[Refined Test Pickaxe]]
|-
| [[Polished Test Pickaxe|Polished Test Pickaxe]]
|}'''
        self.assertEqual([['Very', 'Wise', 'Wise Test Armor'], ['Very', 'Strong †', 'Strong Test Armor'],
                          ['Even More', 'Refined', 'Refined Test Pickaxe'], ['Even More', 'Refined', 'Polished Test Pickaxe']],
                         br.wikitable(table))

    def test_prices(self):
        src = FakeSources()
        src.prices_page = ''.join(f'|-\n| {{{{{r.replace("_", " ").title()}}}}}\n| {{{{c|{n}}}}}\n'
                                  for n, r in enumerate(br.RARITIES, 1))
        self.assertEqual(dict(zip(br.RARITIES, range(1, 10))), br.Builder(src).prices())


class Merging(unittest.TestCase):
    BASIC = {'Keen': {'reforgeName': 'Keen', 'itemTypes': 'SWORD/ROD', 'requiredRarities': ['COMMON', 'RARE'],
                      'reforgeStats': {'COMMON': {'strength': 2, 'intelligence': 5},
                                       'RARE': {'strength': 4, 'intelligence': 7, 'bonus_attack_speed': 1}}}}
    WIKI = {'Sword': {'Keen': {'source': 'Basic', 'stats': {'c': {'str': 2, 'int': 5}, 'u': {'str': 3}, 'r': {'str': 6, 'int': 7}}}}}

    def build(self, live=None, wiki=None, basic=None):
        b = br.Builder(FakeSources(basic=basic or self.BASIC, wiki=wiki or self.WIKI, live=live))
        reforges, pools, stones = b.build({})
        return reforges['keen'], b

    def test_neu_alone(self):
        keen, b = self.build()
        self.assertEqual({'COMMON': {'STRENGTH': 2, 'INTELLIGENCE': 5}, 'RARE': {'STRENGTH': 4, 'ATTACK_SPEED': 1, 'INTELLIGENCE': 7},
                          'UNCOMMON': {'STRENGTH': 3}}, keen['stats'])
        self.assertIn('Keen RARE STRENGTH: NEU 4.0, wiki 6.0', b.notes['stat: NEU and the wiki differ, no live item settles it (NEU taken)'])

    def test_live_items_correct_neu(self):
        live = {'keen': {'RARE': {'items': 4, 'stats': {'Strength': {'+6': 4}, 'Intelligence': {'+7': 4}, 'Attack Speed': {'+1': 3},
                                                        'Bonus Attack Speed': {'+1': 1}}}}}
        keen, b = self.build(live)
        self.assertEqual(6, keen['stats']['RARE']['STRENGTH'])
        # The wiki said 6 too, so its other rarities count as well.
        self.assertEqual(1, len(b.notes['live corrects NEU']))

    def test_a_stat_no_item_shows_is_left_out(self):
        live = {'keen': {'COMMON': {'items': 3, 'stats': {'Strength': {'+2': 3}}}}}
        keen, b = self.build(live, wiki={'Sword': {'Keen': {'source': 'Basic', 'stats': {'c': {'str': 2}}}}})
        self.assertNotIn('INTELLIGENCE', keen['stats']['COMMON'])
        self.assertNotIn('INTELLIGENCE', keen['stats']['RARE'])

    def test_a_value_few_items_show_doesnt_count(self):
        live = {'keen': {'RARE': {'items': 5, 'stats': {'Strength': {'+9': 1, '+4': 1}, 'Intelligence': {'+7': 5}}}}}
        keen, b = self.build(live)
        self.assertEqual(4, keen['stats']['RARE']['STRENGTH'])

    def test_a_stat_neu_misnamed(self):
        basic = {'Keen': dict(self.BASIC['Keen'], reforgeStats={'COMMON': {'strength': 2, 'crit_damage': 3},
                                                               'RARE': {'strength': 4, 'crit_chance': 5}})}
        wiki = {'Sword': {'Keen': {'source': 'Basic', 'stats': {'c': {'str': 2, 'cc': 3}, 'r': {'str': 4, 'cc': 5}}}}}
        keen, b = self.build(wiki=wiki, basic=basic)
        self.assertEqual({'STRENGTH': 2, 'CRIT_CHANCE': 3}, keen['stats']['COMMON'])

    def test_special_is_mythics(self):
        basic = {'Keen': dict(self.BASIC['Keen'], reforgeStats={'MYTHIC': {'strength': 9}, 'SPECIAL': {'strength': 1}})}
        live = {'keen': {'SPECIAL': {'items': 5, 'stats': {'Strength': {'+2': 5}}}}}
        keen, b = self.build(live, wiki={'Sword': {}}, basic=basic)
        self.assertEqual({'MYTHIC': {'STRENGTH': 9}}, keen['stats'])


class Bonus(unittest.TestCase):
    STONES = {'TEST_STONE': {'reforgeName': 'Grim', 'itemTypes': 'SWORD', 'requiredRarities': ['RARE', 'EPIC', 'LEGENDARY'],
                             'reforgeCosts': {'RARE': 10, 'EPIC': 20, 'LEGENDARY': 30},
                             'reforgeStats': {'RARE': {'strength': 5}, 'EPIC': {'strength': 7}, 'LEGENDARY': {'strength': 9}},
                             'reforgeAbility': 'Grants +1 ❁ Strength per Catacombs level.'}}

    def build(self, live=None, stone_lore=None, wiki=None):
        b = br.Builder(FakeSources(stones=self.STONES, live=live, stone_lore=stone_lore, wiki=wiki or {'Sword': {}}))
        reforges, pools, stones = b.build({})
        return reforges['grim'], stones[0], b

    def test_neu_text_wrapped(self):
        grim, stone, b = self.build()
        # In gray: NEU leaves it out, and lore would show the line purple.
        self.assertEqual(['&7Grants +1 ❁ Strength per Catacombs', '&7level.'], grim['bonus']['RARE'])
        self.assertEqual({'STRENGTH': 1}, grim['per_catacombs_level'])
        self.assertEqual({'item': 'TEST_STONE', 'reforge': 'grim', 'type': 'SWORD', 'costs': {'RARE': 10, 'EPIC': 20, 'LEGENDARY': 30}},
                         stone)

    def test_what_items_show_wins(self):
        live = {'grim': {'EPIC': {'items': 2, 'stats': {'Strength': {'+30': 2}},
                                  'bonus': [{'lines': ['§9Grim Bonus', '§7Grants §a+1 §c\ue00d Strength §7per', '§cCatacombs §7level.'],
                                             'count': 2}]}}}
        grim, stone, b = self.build(live)
        self.assertEqual(['&7Grants &a+1 &c❁ Strength &7per', '&cCatacombs &7level.'], grim['bonus']['EPIC'])
        # NEU says the same words, so NEU's for the rest, as the items show those words (their lines and colours);
        # and the bracket's Catacombs level isn't the stat.
        self.assertEqual(['&7Grants &a+1 &c❁ Strength &7per', '&cCatacombs &7level.'], grim['bonus']['RARE'])
        self.assertEqual(7, grim['stats']['EPIC']['STRENGTH'])

    def test_the_stones_own_lore_and_heading(self):
        own = ['§9Grim Pact §8(Grim)', '§7Something else entirely.']
        grim, stone, b = self.build(stone_lore={'TEST_STONE': own})
        self.assertEqual('&9Grim Pact &8(Grim)', grim['bonus_title'])
        self.assertEqual(['&7Something else entirely.'], grim['bonus']['LEGENDARY'])
        # No source says so: the text items show, which is the same at every rarity seen.
        self.assertEqual(['&7Something else entirely.'], grim['bonus']['RARE'])

    def test_live_items_over_the_stones_own_lore(self):
        # The stone's dump says what live items do, with a glyph out of date: the items' lines at Legendary too.
        own = ['§9Grim Bonus', '§7Grants §a+1 §c\ue099 Strength §7per', '§cCatacombs §7level.']
        live = {'grim': {'EPIC': {'items': 2, 'stats': {'Strength': {'+30': 2}},
                                  'bonus': [{'lines': ['§9Grim Bonus', '§7Grants §a+1 §c\ue00d Strength §7per', '§cCatacombs §7level.'],
                                             'count': 2}]}}}
        grim, stone, b = self.build(live, stone_lore={'TEST_STONE': own})
        self.assertEqual(['&7Grants &a+1 &c❁ Strength &7per', '&cCatacombs &7level.'], grim['bonus']['LEGENDARY'])

    def test_a_sources_text_as_lore_has_it(self):
        # The wiki's one long line wrapped, and its line with no colour in the one the line before ends in.
        self.assertEqual(['&7Grants a &a0.1% &7chance to drop an', '&7enchanted item when mining &6Ores&7.', '&7Only on tests.'],
                         br.Builder.as_shown(['&7Grants a &a0.1% &7chance to drop an enchanted item when mining &6Ores&7.',
                                              'Only on tests.'], {}, 'COMMON'))
        # The same words items show at other rarities: the nearest one's lines.
        self.assertEqual(['&7Grants a', '&7test.'], br.Builder.as_shown(['&7Grants a test.'], {
            'EPIC': ['&7Grants', '&7a &atest&7.'], 'RARE': ['&7Grants a', '&7test.']}, 'COMMON'))


if __name__ == '__main__':
    unittest.main()
