#!/usr/bin/env python3
# python3 tools/pets/test_build_pets.py
#
# Checks of build_pets.py on made-up NEU dumps, wiki data and a recording (no Hypixel data needed): the numbers'
# formulas and decimals, the templates' blocks and paragraphs, the wiki's Lua, and the check against the recording.
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_pets as bp  # noqa: E402


def neu_item(internal, name, lore, texture='abc123'):
    value = __import__('base64').b64encode(json.dumps(
        {'textures': {'SKIN': {'url': 'http://textures.minecraft.net/texture/' + texture}}}).encode()).decode()
    return {'internalname': internal, 'displayname': name, 'lore': lore,
            'nbttag': '{ExtraAttributes:{petInfo:"{}"},SkullOwner:{Properties:{textures:[0:{Value:"%s"}]}}}' % value}


BEE = neu_item('BEE;1', '§7[Lvl {LVL}] §aBee', [
    '§8Farming Pet', '',
    '§7Strength: §c+{STRENGTH}', '§7Speed: §f+{SPEED}', '',
    '§6Hive', '§7For each player within §a25 §7blocks:', ' §b+{0} Intelligence', '§8Max 15 players', '',
    '§6Busy Buzz Buzz', '§7Has §a{1}% §7chance for flowers to drop', '§7an extra one', '',
    '§eRight-click to add this pet to your pet menu!', '', '§8Can be upgraded at Kat in The Hub!', '§a§lUNCOMMON PET'])

WIKI_LUA = """return {
	-- a comment
	['Bee'] = {
		id = 'BEE',
		stats = {
			{ name = 'Strength', base = 5, bonus = 0.25 },
			{ name = 'Speed', bonus = 0.1 },
		},
		abilities = {
			name = { [1] = 'Hive', [2] = 'Busy Buzz Buzz' },
			tooltip = {
				[1] = '&7For each player within &a25 &7blocks:/ &b+{1} Intelligence/&8Max 15 players',
				[2] = '&7Has &a{2}% &7chance for flowers to drop/&7an extra one',
			},
		},
		variables = {
			uncommon = {
				ability_count = 2,
				[1] = { base = 1, per_lvl = 0.05, color = 'Aqua' },
				[2] = { per_lvl = 0.07, color = 'Green', suffix = '%%' },
			},
		},
	},
}"""


class Numbers(unittest.TestCase):
    def test_java_rounding(self):
        # Half-even on the double's own value, as DecimalFormat: 26.25 and 2.25 are exact, 0.15 is a little under.
        self.assertEqual(26.2, bp.shown(26.25, 1))
        self.assertEqual(0.1, bp.shown(0.15, 1))
        self.assertEqual(6.6, bp.shown(0.07 * 94, 1))
        self.assertEqual(1.16, bp.shown(1 + 0.04 * 4, 2))

    def test_the_wikis_formula_when_it_fits(self):
        f, at1 = bp.fit(26.2, 150, (25, 1.25))  # the Zombie's Rotten Blade: NEU has 26.25 as shown
        self.assertEqual(('wiki', 25, 1.25, 1), (f.how, f.base, f.per, at1))

    def test_neus_own_when_the_wiki_has_none(self):
        f, _ = bp.fit(0.1, 7)  # the Guardian's Lucky Seven: 0.07 a level, shown as 0.1 at level 1
        self.assertEqual(('neu', 0, 0.07), (f.how, f.base, f.per))
        self.assertEqual(1, bp.with_decimals(f, 0.1).decimals)
        self.assertEqual('6.6', bp.text_number(f, 94))

    def test_a_typo_at_level_1(self):
        f, _ = bp.fit(1.05, 5)  # the Horse: 1 + 0.04 a level, NEU's level 1 typed as 1.05
        self.assertEqual(('neu-near', 1, 0.04), (f.how, f.base, f.per))

    def test_neus_level_1_wrong(self):
        # The Spider's Strength: NEU has 0.1 at level 1 (its Crit Chance's), the wiki 0.5 a level, as recorded.
        f, _ = bp.fit(0.1, 50, (0, 0.5))
        self.assertEqual(('wiki-not-neu', 0, 0.5), (f.how, f.base, f.per))

    def test_decimals_as_many_as_there_are(self):
        f = bp.with_decimals(bp.fit(1.04, 5)[0], 1.04)  # the Bee's Hive: "1.16" at level 4, recorded
        self.assertEqual(2, f.decimals)
        self.assertEqual('1.16', bp.text_number(f, 4))
        self.assertEqual('5', bp.text_number(f, 100))

    def test_the_dragons_levels(self):
        # NEU's level 1 and 100 values are the Golden Dragon's at 100 (stats) or 101 (most numbers) and 200.
        f, at1 = bp.fit_level(25, 50, None, '101:200:1')
        self.assertEqual((0, 0.25, 100), (f.base, f.per, at1))
        f, at1 = bp.fit_level(5.05, 10, None, '101:200:1')
        self.assertEqual((0, 0.05, 101), (f.base, f.per, at1))


class Templates(unittest.TestCase):
    def setUp(self):
        bp.STAT_NAMES.update(bp.plugin_stats()[1])
        self.text = bp.build_items.Text({0xE003: '✎'})

    def test_paragraphs(self):
        lines = ['&7Applies &c+11.9❁ Strength &7to all', '&4Arachnal Ж &7weapons, armor, and', '&7equipment.',
                 ' &b+1.2✎ Intelligence', '&8Max 15 players']
        self.assertEqual(['&7Applies &c+11.9❁ Strength &7to all &4Arachnal Ж &7weapons, armor, and equipment.',
                          ' &b+1.2✎ Intelligence', '&8Max 15 players'], bp.paragraphs(lines))
        # Dark gray carried on is the wrapping's; turning dark gray is a new line.
        self.assertEqual(['&7Killing grants &8Stacks up to 10 times.'], bp.paragraphs(['&7Killing grants &8Stacks up to 10', '&8times.']))
        self.assertEqual(['&7in your &6Gold Collection&7.', '&8(Max 100M collection)'],
                         bp.paragraphs(['&7in your &6Gold Collection&7.', '&8(Max 100M collection)']))

    def test_a_template(self):
        t = bp.Template(BEE, self.text, [])
        self.assertEqual(('Bee', 'a', 'Farming Pet', 'FARMING', 'abc123'), (t.name, t.color, t.kind, t.skill, t.texture))
        self.assertEqual(['STRENGTH', 'SPEED'], t.stats)
        self.assertEqual([{'name': '&6Hive', 'text': ['&7For each player within &a25 &7blocks:', ' &b+{0}✎ Intelligence',
                                                       '&8Max 15 players']},
                          {'name': '&6Busy Buzz Buzz', 'text': ['&7Has &a{1}% &7chance for flowers to drop an extra one']}],
                         t.blocks)
        self.assertTrue(t.kat, '"Can be upgraded at Kat" in its footer')
        self.assertEqual([0, 1], t.placeholders)

    def test_wrapping_keeps_a_list_lines_indent(self):
        self.assertEqual(['  &b+3% ✯ Magic Find'], bp.wrapped('  &b+3% ✯ Magic Find'))


class Wiki(unittest.TestCase):
    def test_lua(self):
        data = bp.lua_table(WIKI_LUA)
        bee = data['Bee']
        self.assertEqual('BEE', bee['id'])
        self.assertEqual({'name': 'Strength', 'base': 5, 'bonus': 0.25}, bee['stats'][1])
        self.assertEqual([(1, 1, 0.05, 1), (2, 0, 0.07, 2)], bp.wiki_numbers(bee, 'UNCOMMON'))
        self.assertEqual({'Strength': (5, 0.25), 'Speed': (0, 0.1)}, bp.wiki_stats(bee, 'UNCOMMON'))

    def test_a_number_neu_left_as_it_is(self):
        bp.STAT_NAMES.update(bp.plugin_stats()[1])
        item = neu_item('BEE;1', '§7[Lvl {LVL}] §aBee', ['§8Farming Pet', '', '§6Busy Buzz Buzz',
                                                         '§7Has §a0.07% §7chance for flowers to drop', '§7an extra one'])
        t = bp.Template(item, bp.build_items.Text({}), [])
        wiki = dict(bp.lua_table(WIKI_LUA)['Bee'])
        numbers, problems = [], []
        bp.scaling_literals(t, wiki, 'UNCOMMON', {1}, numbers, problems)
        self.assertEqual(['&7Has &a{0}% &7chance for flowers to drop an extra one'], t.blocks[0]['text'])
        self.assertEqual([(0, 0.07)], [(f.base, f.per) for f in numbers])
        self.assertEqual(1, len(problems))


class Recording(unittest.TestCase):
    def test_built_and_checked(self):
        with tempfile.TemporaryDirectory() as d:
            neu = os.path.join(d, 'neu')
            os.makedirs(os.path.join(neu, 'items'))
            os.makedirs(os.path.join(neu, 'constants'))
            with open(os.path.join(neu, 'items', 'BEE;1.json'), 'w', encoding='utf-8') as f:
                json.dump(BEE, f)
            with open(os.path.join(neu, 'constants', 'pets.json'), 'w', encoding='utf-8') as f:
                json.dump({'pet_levels': [100, 110], 'pet_rarity_offset': {'COMMON': 0}, 'custom_pet_leveling': {}}, f)
            with open(os.path.join(neu, 'constants', 'petnums.json'), 'w', encoding='utf-8') as f:
                json.dump({'BEE': {'UNCOMMON': {'1': {'otherNums': [1.05, 0.1], 'statNums': {'STRENGTH': 5.25, 'SPEED': 0.1}},
                                                '100': {'otherNums': [6, 7], 'statNums': {'STRENGTH': 30, 'SPEED': 10}}}}}, f)
            problems = []
            data = bp.build(bp.Neu(neu), {'BEE': bp.lua_table(WIKI_LUA)['Bee']}, problems)
            r = data['pets']['BEE']['rarities']['UNCOMMON']
            self.assertEqual({'STRENGTH': {'base': 5, 'per_level': 0.25}, 'SPEED': {'base': 0, 'per_level': 0.1}}, r['stats'])
            self.assertEqual([{'base': 1, 'per_level': 0.05, 'decimals': 2}, {'base': 0, 'per_level': 0.07, 'decimals': 1}],
                             r['numbers'])
            gui = os.path.join(d, 'gui.txt')
            with open(gui, 'w', encoding='utf-8') as f:
                f.write('\n'.join([
                    '=== 00:01.0 opened: Pets',
                    '  00:01.0 slot 10 := player_head x1 &7[Lvl 4] &aBee',
                    '      &8Farming Pet', '      ', '      &7Strength: &c+6', '      &7Speed: &f+0.4', '      ',
                    '      &6Hive', '      &7For each player within &a25 &7blocks:', '       &b+1.2 Intelligence',
                    '      &8Max 15 players', '      ', '      &6Busy Buzz Buzz',
                    '      &7Has &a0.3% &7chance for flowers to drop', '      &7an extra one', '      ',
                    '      &7Progress to Level 5: &e0%',
                    '  00:01.0 slot 47 := diamond x1 &dPet Score Rewards',
                    '      &610 Score&8: +&b1 Magic Find', '      &625 Score&8: +&b2 Magic Find &5«', '']))
            checked = bp.check(data, gui, problems)
            self.assertEqual([('&7[Lvl 4] &aBee', 'BEE', 'UNCOMMON', 4, [])], checked)
            self.assertEqual([[10, 1], [25, 2]], bp.score_rewards(gui))
            report = bp.report(data, problems, checked, {'neu_commit': 'x', 'wiki_revision': 1, 'recording': 'r'})
            self.assertIn('1 of 1 pets as recorded', report)


if __name__ == '__main__':
    unittest.main()
