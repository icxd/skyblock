#!/usr/bin/env python3
# python3 tools/hex/test_build_enchants.py
#
# Checks of build_enchants.py on made-up input (no Hypixel data needed): the books' lore, the text made for levels
# with no book, the ids, the wiki's Hex screens' order, and a whole enchantment put together.
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_enchants as be  # noqa: E402


def book(hypixel, level, desc, applied=('Sword',), conflicts=(), sources=('I-V: Enchantment Table,',), rarity='§f§lCOMMON',
         name=None, ultimate=False):
    head = ('§d§l' if ultimate else '§9') + (name or hypixel.replace('_', ' ').title()) + ' ' + ['I', 'II', 'III', 'IV', 'V', 'VI'][level - 1]
    lore = ['§8Combinable in Anvil', '', head] + list(desc) + ['']
    if sources:
        lore += ['§6Source:'] + ['§a' + s for s in sources] + ['']
    lore += ['§6Applied To:'] + ['§7- §f' + a for a in applied] + ['']
    if conflicts:
        lore += ['§6Conflicts:'] + ['§7- §c' + c for c in conflicts] + ['']
    lore.append(rarity)
    return be.Book(hypixel, level, lore, lambda s: s)


class Text(unittest.TestCase):
    def test_roman_and_ids(self):
        self.assertEqual([be.roman(r) for r in ('I', 'IV', 'V', 'IX', 'X')], [1, 4, 5, 9, 10])
        self.assertEqual(be.plugin_id('ultimate_one_for_all'), 'one_for_all')
        self.assertEqual(be.plugin_id('dragon_hunter'), 'dragon_hunter')
        self.assertEqual(be.snake("Bobbin' Time"), 'bobbin_time')

    def test_collapse(self):
        # A colour repeated where it's already in effect goes.
        self.assertEqual(be.collapse('§7Gain §a3% §7extra. The §72nd hit'), '&7Gain &a3% &7extra. The 2nd hit')

    def test_substitute(self):
        self.assertEqual(be.substitute('&7Gain &a3% &7and &6+1.4 coins', [3, 1.4], [3.78, 1.8]), '&7Gain &a3.78% &7and &6+1.8 coins')
        # "2nd" isn't a number; a value that isn't in the text makes nothing.
        self.assertEqual(be.substitute('The 2nd hit grants &a5', [5], [10]), 'The 2nd hit grants &a10')
        self.assertIsNone(be.substitute('&7No numbers here.', [5], [10]))


class Books(unittest.TestCase):
    def test_a_book(self):
        b = book('toxophilite', 1, ['§7Gain §a3% §7extra Combat XP. Grants', '§9+3.7☣ Crit Chance§7.', '§850k Combat XP to tier up!'],
                 applied=('Bow',), sources=('I: Community Shop',))
        self.assertEqual(b.name, 'Toxophilite')
        self.assertEqual(b.lines(), ['&7Gain &a3% &7extra Combat XP. Grants', '&9+3.7☣ Crit Chance&7.'])
        self.assertEqual(b.text(), '&7Gain &a3% &7extra Combat XP. Grants &9+3.7☣ Crit Chance&7.')
        self.assertEqual(b.tier_up, '&850k Combat XP to tier up!')
        self.assertEqual(b.sections['Applied To'], ['Bow'])
        self.assertEqual(b.rarity, 'COMMON')
        self.assertIsNone(b.table_max())

    def test_table_levels(self):
        self.assertEqual(book('looting', 4, ['§7Loot.'], sources=('I-III: Enchantment Table', 'IV: Bazaar')).table_max(), 3)
        self.assertEqual(book('silk_touch', 1, ['§7Silk.'], sources=('I: Enchantment Table,',)).table_max(), 1)


class Order(unittest.TestCase):
    def test_hex_screens(self):
        page = ('{{UI|The Hex ➜ Enchant Item\n|2, 5=Enchanted Book, none, &aChampion, &7Gain.\n'
                '|2, 4=Enchanted Book, none, &aSharpness, &7Hits.\n|3, 4=Enchanted Book, none, &aSmite, &7Undead.\n}}'
                '{{UI|The Hex ➜ Books\n|2, 4=Book, none, &5Hot Potato Book, &7Stats.\n}}')

        class Wiki:
            def page(self, title):
                return page if title == be.HEX_PAGES[0] else ''
        self.assertEqual(be.hex_order(Wiki()), ['Sharpness', 'Champion', 'Smite'])


class Entries(unittest.TestCase):
    def builder(self):
        books = {
            'sharpness': {1: book('sharpness', 1, ['§7Increases damage by', '§a5%']),
                          5: book('sharpness', 5, ['§7Increases damage by', '§a30%'], rarity='§a§lUNCOMMON')},
            'life_steal': {1: book('life_steal', 1, ['§7Heals §a0.5%§7.'], conflicts=('Drain',), sources=('I-III: Enchantment Table',))},
            'syphon': {1: book('syphon', 1, ['§7Regen §a0.5§7.'], name='Drain', conflicts=('Life Steal',))},
            'ultimate_one_for_all': {1: book('ultimate_one_for_all', 1, ['§7Removes all other enchants.'], name='One For All',
                                            ultimate=True, sources=('I: Dungeons',))},
        }
        constants = {'enchants_xp_cost': {'sharpness': [10, 15, 20, 25, 30], 'one_for_all': [50]},
                     'max_xp_table_levels': {'sharpness': 5}, 'enchant_pools': [['ultimate_one_for_all', 'sharpness']]}
        wiki = {'Sharpness': {'max': 5, 'req': 0, 'vars': {1: {1: 5, 2: 10, 3: 15, 4: 20, 5: 30}}},
                'Drain': {'max': 1, 'req': 15, 'cost': {1: 20}}}
        return be.Builder(books, constants, wiki, ['One For All', 'Sharpness', 'Nothing Like It'])

    def test_levels_with_no_book_get_their_values(self):
        order, entries = self.builder().build()
        sharpness = entries['sharpness']
        self.assertEqual(sharpness['levels']['3']['text'], '&7Increases damage by &a15%')
        self.assertEqual(sharpness['levels']['3']['lines'], ['&7Increases damage by', '&a15%'])
        self.assertEqual(sharpness['levels']['5']['rarity'], 'UNCOMMON')
        self.assertEqual((sharpness['min'], sharpness['max'], sharpness['table']), (1, 5, 5))
        self.assertEqual(sharpness['xp'], [10, 15, 20, 25, 30])

    def test_ids_names_conflicts_and_order(self):
        b = self.builder()
        order, entries = b.build()
        ofa = entries['one_for_all']
        self.assertEqual((ofa['hypixel'], ofa['ultimate'], ofa['name'], ofa['table']), ('ultimate_one_for_all', True, 'One For All', None))
        self.assertEqual(entries['syphon']['name'], 'Drain')
        self.assertEqual(entries['syphon']['enchanting'], 15)
        # The wiki's Exp levels where NEU has none.
        self.assertEqual(entries['syphon']['xp'], [20])
        # Both ways, from the books and the pools.
        self.assertEqual(entries['life_steal']['conflicts'], ['syphon'])
        self.assertEqual(entries['syphon']['conflicts'], ['life_steal'])
        self.assertEqual(entries['sharpness']['conflicts'], ['one_for_all'])
        # The wiki's order first, then by name.
        self.assertEqual(order, ['one_for_all', 'sharpness', 'syphon', 'life_steal'])
        self.assertIn('Nothing Like It', b.problems["a name in the wiki's Hex screens that no book has"])


if __name__ == '__main__':
    unittest.main()
