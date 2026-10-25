"""Writes profiles.json, a Hypixel API v2 skyblock/profiles answer for the yoink tests: three profiles (one left), items as
base64 gzipped 1.8 NBT, in the layout NEU's profile viewer reads. Made up. Run: python3 profiles.py profiles.json"""
import base64, gzip, io, json, struct, sys

def s(v): b = v.encode('utf-8'); return struct.pack('>H', len(b)) + b
class Byte(int): pass
class Short(int): pass
class Long(int): pass
class IntArr(list): pass

def tag_type(v):
    if isinstance(v, Byte): return 1
    if isinstance(v, Short): return 2
    if isinstance(v, Long): return 4
    if isinstance(v, bool): return 1
    if isinstance(v, int): return 3
    if isinstance(v, float): return 6
    if isinstance(v, str): return 8
    if isinstance(v, IntArr): return 11
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(type(v))

def payload(v):
    t = tag_type(v)
    if t == 1: return struct.pack('>b', int(v))
    if t == 2: return struct.pack('>h', v)
    if t == 3: return struct.pack('>i', v)
    if t == 4: return struct.pack('>q', v)
    if t == 6: return struct.pack('>d', v)
    if t == 8: return s(v)
    if t == 11: return struct.pack('>i', len(v)) + b''.join(struct.pack('>i', x) for x in v)
    if t == 9:
        et = tag_type(v[0]) if v else 10
        return struct.pack('>bi', et, len(v)) + b''.join(payload(x) for x in v)
    if t == 10:
        return b''.join(struct.pack('>b', tag_type(x)) + s(k) + payload(x) for k, x in v.items()) + b'\x00'

def blob(items):
    root = b'\x0a' + s('') + payload({'i': items})
    out = io.BytesIO()
    with gzip.GzipFile(fileobj=out, mode='wb', mtime=0) as g: g.write(root)
    return {'type': 0, 'data': base64.b64encode(out.getvalue()).decode()}

def item(numeric, count, extra, name='', lore=(), damage=0):
    return {'id': Short(numeric), 'Count': Byte(count), 'tag': {'ExtraAttributes': extra, 'display': {'Name': name, 'Lore': list(lore) or ['']}},
            'Damage': Short(damage)}
EMPTY = {}

aote = item(267, 1, {'id': 'ASPECT_OF_THE_END', 'uuid': '11111111-2222-3333-4444-555555555555', 'modifier': 'heroic', 'rarity_upgrades': 1,
                     'hot_potato_count': 12, 'art_of_war_count': 1, 'enchantments': {'sharpness': 6, 'ultimate_wise': 3, 'critical': 6},
                     'tuned_transmission': 4, 'timestamp': Long(1700000000000)}, '§6Heroic Aspect of the End')
hyperion = item(267, 1, {'id': 'HYPERION', 'uuid': '22222222-2222-3333-4444-555555555555', 'upgrade_level': 8, 'modifier': 'withered',
                         'ability_scroll': ['IMPLOSION_SCROLL', 'SHADOW_WARP_SCROLL', 'WITHER_SHIELD_SCROLL'],
                         'gems': {'SAPPHIRE_0': 'PERFECT', 'COMBAT_0': {'quality': 'FLAWLESS', 'uuid': 'x'}, 'COMBAT_0_gem': 'JASPER',
                                  'unlocked_slots': ['SAPPHIRE_0', 'COMBAT_0']},
                         'enchantments': {'ultimate_wise': 5, 'sharpness': 7}, 'hot_potato_count': 15, 'stats_book': 1234,
                         'champion_combat_xp': 1000.0}, '§dWithered Hyperion §d✪✪✪✪✪§c➌')
diamonds = item(264, 64, {'id': 'ENCHANTED_DIAMOND'}, '§aEnchanted Diamond')
pet = item(397, 1, {'id': 'PET', 'petInfo': json.dumps({'type': 'ENDER_DRAGON', 'tier': 'LEGENDARY', 'exp': 25000000.0})}, '§7[Lvl 100] §6Ender Dragon', damage=3)
unknown = item(1, 3, {'id': 'NOT_A_REAL_ITEM_EVER'}, '§fMystery')
helmet = item(397, 1, {'id': 'POWER_WITHER_HELMET', 'upgrade_level': 5, 'modifier': 'ancient', 'enchantments': {'protection': 5, 'growth': 5},
                       'runes': {'MUSIC': 3}, 'attributes': {'mana_pool': 5}}, '§6Ancient Necron\'s Head', damage=3)
chest = item(299, 1, {'id': 'POWER_WITHER_CHESTPLATE', 'upgrade_level': 5, 'color': '231:92:60', 'runes': {'GRAND_SEARING': 3}}, '§6Necron\'s Chestplate')
legs = item(300, 1, {'id': 'POWER_WITHER_LEGGINGS', 'upgrade_level': 5}, '§6Necron\'s Leggings')
boots = item(301, 1, {'id': 'POWER_WITHER_BOOTS', 'upgrade_level': 5}, '§6Necron\'s Boots')

inv = [EMPTY] * 36
inv[0], inv[1], inv[2], inv[3], inv[10] = aote, hyperion, diamonds, pet, unknown
armor = [boots, legs, chest, helmet]   # the API's order: boots first
ender = [EMPTY] * 90
ender[0], ender[50] = diamonds, item(264, 32, {'id': 'ENCHANTED_DIAMOND'})
wardrobe = [EMPTY] * 36
wardrobe[0], wardrobe[9], wardrobe[18], wardrobe[27] = helmet, chest, legs, boots   # set 1: a column of four
talismans = [item(397, 1, {'id': 'WOLF_TALISMAN'}, damage=3), item(397, 1, {'id': 'POWER_ARTIFACT', 'modifier': 'bizarre'}, damage=3)] + [EMPTY] * 7
backpack = [EMPTY] * 27
backpack[0] = diamonds
equipment = [item(397, 1, {'id': 'MOLTEN_NECKLACE'}), EMPTY, EMPTY, EMPTY]

member = {
    'player_id': 'b876ec32e396476ba1158438d83c67d4',
    'profile': {'first_join': 1600000000000},
    'currencies': {'coin_purse': 123456.7, 'motes_purse': 0, 'essence': {'WITHER': {'current': 1500}, 'UNDEAD': {'current': 20}}},
    'leveling': {'experience': 25080},
    'player_data': {'experience': {'SKILL_COMBAT': 55172425.5, 'SKILL_MINING': 1000000.0, 'SKILL_FORAGING': 12.0, 'SKILL_RUNECRAFTING': 300.0,
                                   'SKILL_SOCIAL': 9000.0},
                    'crafted_generators': ['WHEAT_1', 'WHEAT_2', 'COBBLESTONE_1']},
    'collection': {'LOG': 12345, 'COBBLESTONE': 500000, 'ENDER_STONE': 1000},
    'dungeons': {'dungeon_types': {'catacombs': {'experience': 569809640.0, 'highest_tier_completed': 7},
                                   'master_catacombs': {'highest_tier_completed': 5}},
                 'player_classes': {'healer': {'experience': 1000.0}, 'mage': {'experience': 2000.0}, 'berserk': {'experience': 300.0},
                                    'archer': {'experience': 40.0}, 'tank': {'experience': 5.0}},
                 'selected_dungeon_class': 'mage'},
    'mining_core': {'experience': 1500000.0, 'tokens': 20, 'tokens_spent': 15, 'powder_mithril': 100000, 'powder_gemstone': 2000,
                    'powder_glacite': 30, 'nodes': {'mining_speed': 50, 'mining_fortune': 50, 'titanium_insanium': 50, 'forge_time': 20}},
    'fairy_soul': {'total_collected': 246, 'fairy_exchanges': 49, 'unspent_souls': 1},
    'nether_island_player_data': {'selected_faction': 'mages', 'mages_reputation': 3500.0, 'barbarians_reputation': -200.0,
                                  'kuudra_completed_tiers': {'none': 12, 'hot': 3, 'highest_wave_hot': 4}},
    'accessory_bag_storage': {'selected_power': 'silky', 'tuning': {'slot_0': {'health': 3, 'critical_damage': 10, 'walk_speed': 2}}, 'highest_magical_power': 900},
    'inventory': {'inv_contents': blob(inv), 'inv_armor': blob(armor), 'equipment_contents': blob(equipment),
                  'ender_chest_contents': blob(ender), 'wardrobe_contents': blob(wardrobe), 'wardrobe_equipped_slot': 2,
                  'backpack_contents': {'0': blob(backpack)}, 'backpack_icons': {'0': blob([item(397, 1, {'id': 'JUMBO_BACKPACK'}, damage=3)])},
                  'bag_contents': {'talisman_bag': blob(talismans), 'potion_bag': blob([EMPTY] * 9), 'fishing_bag': blob([EMPTY] * 9),
                                   'sacks_bag': blob([EMPTY] * 9), 'quiver': blob([EMPTY] * 9)},
                  'sacks_counts': {'ENCHANTED_DIAMOND': 3, 'LOG': 1200}},
}
other_member = {'player_id': 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'currencies': {'coin_purse': 1.0}}
left = {'profile': {'deletion_notice': {'timestamp': 1}}}
answer = {'success': True, 'profiles': [
    {'profile_id': 'aaaa1111-0000-0000-0000-000000000000', 'cute_name': 'Strawberry', 'selected': False, 'members': {'b876ec32e396476ba1158438d83c67d4': {'currencies': {'coin_purse': 5.0}}}},
    {'profile_id': 'bbbb2222-0000-0000-0000-000000000000', 'cute_name': 'Banana', 'selected': True, 'game_mode': 'ironman',
     'members': {'b876ec32e396476ba1158438d83c67d4': member, 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa': other_member}, 'banking': {'balance': 1000000000.5}},
    {'profile_id': 'cccc3333-0000-0000-0000-000000000000', 'cute_name': 'Kiwi', 'selected': False, 'members': {'b876ec32e396476ba1158438d83c67d4': left}},
]}
json.dump(answer, open(sys.argv[1], 'w'), indent=1)
