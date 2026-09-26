#!/usr/bin/env python3
# python3 tools/items/test_build_items.py
#
# Checks of build_items.py's lore parsing on made-up lore (no Hypixel data needed).
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import build_items  # noqa: E402


class OwnerText(unittest.TestCase):
    """The copy NEU captured names its owner; every copy made from the data would show that player's name."""

    def test_owner_and_provenance_sections_left_out(self):
        parsed = build_items.parse_lore([
            '§7Obtained during the §61st Test Event', '§7for finishing it!', '',
            '§7Purchased by: §b[MVP§a+§b] SomePlayer§f', '§7Purchased for: §6§k1,000 §6 coins', '',
            '§8Auction #§k1', '§8Bid #§k1', '§8July 2022', '',
            '§7Player: §bSomePlayer', '§7Position: §a#§k10', '',
            '§7Earned by: §bSomePlayer', '§7Total Contributions: §a20', '§8Edition #§k6', '§8March 23, 2024', '',
            '§8Memento acquirer name becomes', '§8hidden if traded.', '',
            '§8This item can be reforged!', '§6§lLEGENDARY'])
        # "Obtained" is a dynamic_text pattern, but the line is the item's own text: it stays
        self.assertEqual(parsed['own'], ['§7Obtained during the §61st Test Event', '§7for finishing it!', '',
                                         '§8Memento acquirer name becomes', '§8hidden if traded.'])
        self.assertEqual(parsed['issues']['owner_text'], 4)

    def test_owner_lines_after_a_block_are_not_its_text(self):
        parsed = build_items.parse_lore([
            '§6Ability: Test  §e§lRIGHT CLICK', '§7Does a test.', '',
            '§7Purchased by: §bSomePlayer', '§7Purchased for: §6§k1,000 §6 coins', '',
            '§8Auction #§k1', '§8Bid #§k1', '§8February 2025', '',
            '§5§lEPIC WAND'])
        self.assertEqual([b['text'] for b in parsed['blocks']], [['§7Does a test.']])
        self.assertEqual(parsed['own'], [])

    def test_gift_to_and_from(self):
        parsed = build_items.parse_lore([
            '§7§oA test gift.', '', '§7To: §bSomePlayer', '§7From: §c[ADMIN] Someone', '',
            '§8Edition #1', '§8September 2020', '', '§c§lSPECIAL'])
        self.assertEqual(parsed['own'], ['§7§oA test gift.'])

    def test_live_values_stay(self):
        # a counter shows a state the item can be in: it stays, and only the report lists it
        parsed = build_items.parse_lore(['§7Your kills: §20§8/25', '', '§9§lRARE'])
        self.assertEqual(parsed['own'], ['§7Your kills: §20§8/25'])
        self.assertIn('dynamic_text', parsed['issues'])
        self.assertNotIn('owner_text', parsed['issues'])

    def test_date_in_the_items_own_text_stays(self):
        # a date is provenance only in a section of provenance lines
        parsed = build_items.parse_lore(['§7Released in', '§8July 2022', '', '§9§lRARE'])
        self.assertEqual(parsed['own'], ['§7Released in', '§8July 2022'])


if __name__ == '__main__':
    unittest.main()
