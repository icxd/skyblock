# Accessories

What accessories do beyond their stats. Their stats, Accessory Power and the powers are the Accessory Bag's
(STORAGE.md); this is the rest of their text. The code is `paper/src/main/java/net/icxd/dungeons/item/accessory`
(`AccessoryEffects`, started with the set bonuses). Each accessory's numbers are read from its own text at runtime
(`AccessoryText`); the wiki is the source where the text leaves out how it works.

## How it works

**Which count** (`AccessoryBag.counted`): an accessory works in the Accessory Bag or the inventory, one of each,
and of a line (Talisman, Ring, Artifact, Relic) only the best one there, as the Accessory Bag counts them for their
stats (STORAGE.md). The set is looked at again at most once a second, so asking it on every hit is cheap; what the
counted ones do is worked out once each time it changes (`AccessoryEffects.Summary`).

**What they do** goes through the plugin's hooks (EFFECTS.md):

- stats (`PlayerStats.addModifier`, after the set bonuses');
- hits taken (`PlayerDamage.addTakenFrom`, `addShield`);
- their hits (`Combat.addHitBuffs` with the landing, `addHitListener`);
- kills (`SkyBlockMobDeathEvent`), drops (`Mobs.addDropChance`), experience (`ExpOrbs.addBonus`);
- vanilla damage (`VanillaDamage.addFactor`, and a listener that cancels what they're immune to);
- mobs' targets (`TargetNearestPlayerGoal.addIgnored`, new: a mob won't choose a player it leaves alone);
- shops (`Shop.addDiscount`, new: coin costs less a discount, in the ware's lore and when it's bought);
- a tick a second and a minute.

**Counts on an accessory** (the Blood God Crest's kills) are kept on the item, in the inventory (rebuilt there, as
`ItemCounters` does) or in the Accessory Bag (`AccessoryBag.rewrite`, new: the stored item is changed in place).
Its "Counter" line follows (`CrestCounter`); a crest that has counted nothing keeps the data's "None yet!".

## Status

DONE: as the text and wiki say. APPROX: done, with a reading marked UNKNOWN in the code. LATER: waits for the system
named. Accessories whose text is only flavour, or only stats, aren't here (their stats are STORAGE.md's).

| Accessory | Items | Status | Sources |
|---|---|---|---|
| Crystals: Strength and Defense by day or night | DAY_CRYSTAL, SUNSHINE_CRYSTAL, NIGHT_CRYSTAL, MOONLIGHT_CRYSTAL | **DONE** the text's +5/+10 while SkyBlock's clock says day (6am to 7pm) or night | [Day Crystal](https://hypixelskyblock.minecraft.wiki/w/Day_Crystal) |
| Campfire Talismans: Health Regen while burning | CAMPFIRE_TALISMAN_1 … _29 | **DONE** the text's +N Health Regen while they're on fire (the talisman's own stats count as ever) | |
| Soul Campfire Talismans: Defense while burning | SOUL_CAMPFIRE_TALISMAN_1 … _29 | **DONE** the same, +N Defense | |
| Reaper Orb | REAPER_ORB | **APPROX** +2 Strength for each kill in the last 5 s; no most (the wiki: "stacking up to {{InfoNeeded}}") | [Reaper Orb](https://hypixelskyblock.minecraft.wiki/w/Reaper_Orb) |
| Blood God Crest and Sigil | BLOOD_GOD_CREST, BLOOD_GOD_SIGIL | **APPROX** +1 (+2) Strength a digit of the kills counted while it counts (floor(log10 kills) + 1, at most 7, the wiki), kept on the item; the "Counter" line's look for a count is UNKNOWN | [Blood God Crest](https://hypixelskyblock.minecraft.wiki/w/Blood_God_Crest) |
| IQ Points: "your total Intelligence by 1%" | IQ_POINT, TWO_IQ_POINT | **APPROX** Intelligence times 1.01 (1.02) after the other modifiers (UNKNOWN where Hypixel's comes); their Rift part waits for the Rift | [IQ Point](https://hypixelskyblock.minecraft.wiki/w/IQ_Point) |
| Bluetooth and Bluertooth Rings | BLUETOOTH_RING, BLUERTOOTH_RING | **APPROX** +1 (+2) Damage with at least 6 players in their world (the island is the server's world: UNKNOWN); the Blazetekk Ham Radio part waits for it | [Bluetooth Ring](https://hypixelskyblock.minecraft.wiki/w/Bluetooth_Ring) |
| Mine Talisman | MINE_TALISMAN | **DONE** +25 Speed on the Dwarven Mines (the only mining island here) | |
| Village Talisman | VILLAGE_TALISMAN | **DONE** +10 Speed in the Village region | |
| Wood Talisman | WOOD_TALISMAN | **LATER** its regions (Forest, Graveyard, Wilderness, The Park, Moonglade Marsh, Torrhus Canyon) have no bounds yet | |
| Gravity Talisman | GRAVITY_TALISMAN | **APPROX** +10 Strength and Defense at the world's spawn, one less every 10 blocks across, at least +1 (the wiki gives no distances: UNKNOWN) | [Gravity Talisman](https://hypixelskyblock.minecraft.wiki/w/Gravity_Talisman) |
| Master Skulls | MASTER_SKULL_TIER_1 … _7 | **APPROX** Health and Strength times 1.01 to 1.10 in a Master Mode run (of the total so far: UNKNOWN); tiers 8 to 10 have no text | [Master Skull](https://hypixelskyblock.minecraft.wiki/w/Master_Skull) |
| Haste Ring and Artifact | HASTE_RING, HASTE_ARTIFACT | **APPROX** "permanent Haste II" as +50 Mining Speed a level (the wiki: "instead of the Haste being overridden, the player is given +100 Mining Speed"; III's 150 UNKNOWN); no vanilla Haste, which the plugin's mining doesn't use | [Haste Ring](https://hypixelskyblock.minecraft.wiki/w/Haste_Ring) |
| Less damage from a mob type | SKELETON_TALISMAN, WOLF_TALISMAN, WOLF_RING, ZOMBIE_TALISMAN, ZOMBIE_RING, ZOMBIE_ARTIFACT, SPIDER_TALISMAN, SPIDER_RING, SPIDER_ARTIFACT, WITHER_ARTIFACT, WITHER_RELIC, ENDER_ARTIFACT, ENDER_RELIC, BLAZE_TALISMAN | **DONE** a factor on hits from mobs of the type the text names (Skeletal, Animal, Undead, Arthropod, Wither, Ender, Infernal; no Arthropod or Ender mobs here yet); different lines multiply | [Zombie Talisman](https://hypixelskyblock.minecraft.wiki/w/Zombie_Talisman) |
| Nether Artifact | NETHER_ARTIFACT | **DONE** 5% less from Infernal mobs, from every mob on the Crimson Isle | [Nether Artifact](https://hypixelskyblock.minecraft.wiki/w/Nether_Artifact) |
| Burststopper Talisman and Artifact | BURSTSTOPPER_TALISMAN, BURSTSTOPPER_ARTIFACT | **APPROX** a hit that would take at least 50% of their health now (the wiki: "of their health"; UNKNOWN whether it's their max) takes 0.95 (0.9) of it | [Burststopper Talisman](https://hypixelskyblock.minecraft.wiki/w/Burststopper_Talisman) |
| Tarantula Talisman and Ring | TARANTULA_TALISMAN, TARANTULA_RING | **DONE** every 10th melee hit on the same mob x1.1 (x1.15), multiplicative (the wiki's Multiplicative Sources); each mob's hits counted apart | [Tarantula Talisman](https://hypixelskyblock.minecraft.wiki/w/Tarantula_Talisman), [Multiplicative Sources](https://hypixelskyblock.minecraft.wiki/w/Damage_Calculation/Multiplicative_Sources) |
| Wedding Rings | WEDDING_RING_0 … _9 | **APPROX** a 1 in N chance of +100% on a hit, additive (the wiki's Additive Sources, as the Ring of Love's); "Requires quest progress!" isn't asked (no quests here) | [Additive Sources](https://hypixelskyblock.minecraft.wiki/w/Damage_Calculation/Additive_Sources) |
| Vampire Dentist Relic | VAMPIRE_DENTIST_RELIC | **DONE** a melee hit gives a natural regeneration tick (their Health Regen's), 20 s apart; its Rift part waits for the Rift | [Vampire Dentist Relic](https://hypixelskyblock.minecraft.wiki/w/Vampire_Dentist_Relic) |
| Devour Ring | DEVOUR_RING | **DONE** heal 10 on killing an Undead mob, 0.5 s apart | [Devour Ring](https://hypixelskyblock.minecraft.wiki/w/Devour_Ring) |
| Intimidation line | INTIMIDATION_TALISMAN, INTIMIDATION_RING, INTIMIDATION_ARTIFACT, INTIMIDATION_RELIC | **APPROX** SkyBlock's mobs at or below the text's level (1, 5, 25, 30; the level their name shows) don't go for them; dungeon mobs, whose names show no level, aren't among them (UNKNOWN); the Blood Room's undead choose for themselves | [Intimidation Talisman](https://hypixelskyblock.minecraft.wiki/w/Intimidation_Talisman) |
| Experience Artifact | EXPERIENCE_ARTIFACT | **DONE** +25% experience from orbs (the kills' the plugin gives, and the rest through ExpOrbs); experience bottles wait for them | [Experience Artifact](https://hypixelskyblock.minecraft.wiki/w/Experience_Artifact) |
| Feather line | FEATHER_TALISMAN, FEATHER_RING, FEATHER_ARTIFACT | **APPROX** 5, 7 or 10 blocks more before fall damage (their safe fall distance), and fall damage 5% (15%) less; fall damage stays vanilla's (EFFECTS.md UNKNOWN) | [Feather Talisman](https://hypixelskyblock.minecraft.wiki/w/Feather_Talisman) |
| Vaccine line | VACCINE_TALISMAN, VACCINE_RING, VACCINE_ARTIFACT | **APPROX** poison's damage 10%, 25% or 50% less (vanilla poison, the only there is here) | [Vaccine Talisman](https://hypixelskyblock.minecraft.wiki/w/Vaccine_Talisman) |
| Fire Talisman | FIRE_TALISMAN | **DONE** no fire damage, 20% less of it on the Crimson Isle (the Trials of Fire aren't here) | [Fire Talisman](https://hypixelskyblock.minecraft.wiki/w/Fire_Talisman) |
| Lava Talisman | LAVA_TALISMAN | **DONE** no lava damage, except on the Crimson Isle (the wiki); Kuudra's Hollow and Floor VII's boss room aren't here | [Lava Talisman](https://hypixelskyblock.minecraft.wiki/w/Lava_Talisman) |
| Night Vision Charm | NIGHT_VISION_CHARM | **APPROX** Night Vision (II, the wiki's trivia) while it counts, in the bag too (the text says "in your inventory": UNKNOWN) | [Night Vision Charm](https://hypixelskyblock.minecraft.wiki/w/Night_Vision_Charm) |
| Emerald Ring and Artifact | EMERALD_RING, EMERALD_ARTIFACT | **DONE** +5 (+20) coins a minute while online, on any profile (as kills' coins) | [Emerald Ring](https://hypixelskyblock.minecraft.wiki/w/Emerald_Ring) |
| Shop discounts | SHADY_RING, CROOKED_ARTIFACT, SEAL_OF_THE_FAMILY | **APPROX** 1%, 2% or 3% off shops' coin costs (Ophelia's, the only shop here; "most shops": UNKNOWN which not), rounded up to a coin (UNKNOWN) | [Shady Ring](https://hypixelskyblock.minecraft.wiki/w/Shady_Ring) |
| Bucket of Dye | BUCKET_OF_DYE | **APPROX** Dye drops' chance x1.01 (UNKNOWN whether it's a point more); its "Chocolate Dye Donated" line is flavour | |
| Scavenger line | SCAVENGER_TALISMAN, SCAVENGER_RING, SCAVENGER_ARTIFACT | **DONE** coins a level of the killed mob (economy/KillCoins); Eleanor's Armor's Riches multiplies them | [Scavenger Talisman](https://hypixelskyblock.minecraft.wiki/w/Scavenger_Talisman) |
| Hegemony Artifact | HEGEMONY_ARTIFACT | **DONE** twice the Accessory Power (storage/Accessories) | |
| Power line | POWER_TALISMAN, POWER_RING, POWER_ARTIFACT, POWER_RELIC | **DONE** half-powered gemstones (GEMSTONES.md) | |
| Catacombs Expert Ring | CATACOMBS_EXPERT_RING | **LATER** a hook on a member's run experience (the armor enchantments part's RunBoosts): `AccessoryEffects.runBoost` has the +10% ready | [Catacombs Expert Ring](https://hypixelskyblock.minecraft.wiki/w/Catacombs_Expert_Ring) |
| Scarf's Studies, Thesis and Grimoire | SCARF_STUDIES, SCARF_THESIS, SCARF_GRIMOIRE | **LATER** the same hook: +2%, 4% or 6% class experience ready in `runBoost`; their Rift Time waits for the Rift | |
| Treasure line | TREASURE_TALISMAN, TREASURE_RING, TREASURE_ARTIFACT | **LATER** the end of run reward chests' loot (the chests aren't filled yet) | [Treasure Talisman](https://hypixelskyblock.minecraft.wiki/w/Treasure_Talisman) |
| Auto Recombobulator | AUTO_RECOMBOBULATOR | **LATER** a hook on the items a kill drops (to recombobulate one) | [Auto Recombobulator](https://hypixelskyblock.minecraft.wiki/w/Auto_Recombobulator) |
| General Medallion | GENERAL_MEDALLION | **LATER** a count of the secrets a player has found (a run's are kept, a player's aren't) | |
| Magnetic Talisman | MAGNETIC_TALISMAN | **LATER** item pickup range (vanilla's can't be set; it'd take pulling items in) | [Magnetic Talisman](https://hypixelskyblock.minecraft.wiki/w/Magnetic_Talisman) |
| Bat Person line | BAT_PERSON_TALISMAN, BAT_PERSON_RING, BAT_PERSON_ARTIFACT | **LATER** summoned bats (the Spirit Sceptre's are its ability's, the Witch Mask and Bat Pet aren't here) | |
| Piggy Banks | PIGGY_BANK, CRACKED_PIGGY_BANK, BROKEN_PIGGY_BANK | **LATER** purse loss on death, which came after this part's base (origin's `economy/DeathCoins`, with `DeathCoins.addSaver` for what saves coins): once merged, a saver for a loss of 20k+ and the bank cracking are what's left | |
| King Talisman | KING_TALISMAN | **LATER** commissions' Heart of the Mountain experience | |
| Bits Talisman | BITS_TALISMAN | **LATER** a hook on Bits gained | |
| Potion Affinity line | POTION_AFFINITY_TALISMAN, RING_POTION_AFFINITY, ARTIFACT_POTION_AFFINITY | **LATER** potions | |
| Candy line | CANDY_TALISMAN, CANDY_RING, CANDY_ARTIFACT, CANDY_RELIC | **LATER** the Spooky Festival's candy | |
| Draconic line | DRACONIC_TALISMAN, DRACONIC_RING, DRACONIC_ARTIFACT | **LATER** Dragons (the End) | |
| Fishing | SEA_CREATURE_TALISMAN, SEA_CREATURE_RING, SEA_CREATURE_ARTIFACT, EMPEROR_TALISMAN, EMPEROR_RING, EMPEROR_ARTIFACT, BAIT_RING, SPIKED_ATROCITY, SMALL_FISH_BOWL, MEDIUM_FISH_BOWL, LARGE_FISH_BOWL, RAGGEDY_SHARK_TOOTH_NECKLACE, DULL_SHARK_TOOTH_NECKLACE, HONED_SHARK_TOOTH_NECKLACE, SHARP_SHARK_TOOTH_NECKLACE, RAZOR_SHARP_SHARK_TOOTH_NECKLACE, JUNK_TALISMAN, JUNK_RING, JUNK_ARTIFACT | **LATER** fishing and Sea Creatures (the Emperor line's Sea Creature Chance is a text line, not a stat) | |
| Kuudra | KUUDRA_FOLLOWER_ARTIFACT, KUUDRA_FOLLOWER_RELIC, KUUDRAS_KIDNEY, KUUDRAS_LUNG, KUUDRAS_HEART, BURNING_KUUDRA_CORE, FIERY_KUUDRA_CORE, INFERNAL_KUUDRA_CORE | **LATER** Kuudra (and hunting's Hunter Fortune) | |
| Farming and the Garden | ANITA_TALISMAN, ANITA_RING, ANITA_ARTIFACT, BIOANALYSIS_TALISMAN, BIOANALYSIS_RING, BIOANALYSIS_ARTIFACT, COPPER_TALISMAN, COPPER_RING, COPPER_ARTIFACT, GRATITUDE_TALISMAN, GRATITUDE_RING, GRATITUDE_ARTIFACT, ATMOSPHERIC_FILTER, FARMER_ORB, PESTHUNTER_BADGE, PESTHUNTER_RING, PESTHUNTER_ARTIFACT, PESTHUNTER_RELIC, POTATO_TALISMAN, POTATO_RING, FRESHLY_BAKED_TALISMAN, FRESHLY_BAKED_RING, FRESHLY_BAKED_ARTIFACT, FRESHLY_BAKED_RELIC, FRESHLY_BAKED_HEIRLOOM, MAGIC_8_BALL | **LATER** farming, the Garden, pests, minions and Feast events (the Magic 8 Ball's season's bonus too: which one a season gives is UNKNOWN) | |
| Foraging | LUMBERJACK_TALISMAN, LUMBERJACK_RING, LUMBERJACK_ARTIFACT, HONEYCOMB_TALISMAN, HONEYCOMB_RING, HONEYCOMB_ARTIFACT, RUNEBLADE_TALISMAN, RUNEBLADE_RING, RUNEBLADE_ARTIFACT | **LATER** foraging (the Runeblade line: axes on Woodland mobs, none here) | |
| Mining extras | GLACIAL_TALISMAN, GLACIAL_RING, GLACIAL_ARTIFACT, ORGAN_DONOR_TALISMAN, ORGAN_DONOR_RING, ORGAN_DONOR_ARTIFACT, PERSONAL_COMPACTOR_4000, PERSONAL_COMPACTOR_5000, PERSONAL_COMPACTOR_6000, PERSONAL_COMPACTOR_7000, PERSONAL_DELETOR_4000, PERSONAL_DELETOR_5000, PERSONAL_DELETOR_6000, PERSONAL_DELETOR_7000, ACCRETION_TALISMAN, ACCRETION_RING, ACCRETION_ARTIFACT | **LATER** Jerry's Workshop, the Glacite Mineshafts' corpses, the compactors' and deletors' menus, Pocket Black Holes | |
| The Rift | CRUX_TALISMAN_1 … _7, HOCUS_POCUS_CIPHER, SATELITE, PUNCHCARD_ARTIFACT, RESPIRATION_TALISMAN, RESPIRATION_RING, RESPIRATION_ARTIFACT, GARLIC_FLAVORED_GUMMY_BEAR, HARMONIOUS_SURGERY_TOOLKIT, RIFT_PRISM | **LATER** the Rift (the Respiration line's Respiration is a stat and counts) | |
| Soulflow | SOULFLOW_PILE, SOULFLOW_BATTERY, SOULFLOW_SUPERCELL, HANDY_BLOOD_CHALICE, POCKET_ESPRESSO_MACHINE | **LATER** Soulflow and Overflow Mana | |
| Mythological, hunting, Runic | BEASTMASTER_CREST_COMMON, BEASTMASTER_CREST_UNCOMMON, BEASTMASTER_CREST_RARE, BEASTMASTER_CREST_EPIC, BEASTMASTER_CREST_LEGENDARY, TRAPPER_CREST, JAKE_PLUSHIE, RUNEBOOK | **LATER** Mythological mobs, hunting's pelts, Runic mobs | |
| Events, mayors, bags and the rest | ARTIFACT_OF_CONTROL, VOTER_BADGE, VOTER_BADGE_VIP, VOTER_BADGE_ELITE, VOTER_BADGE_SUPREME, BINGO_TALISMAN, BINGO_RING, BINGO_ARTIFACT, BINGO_RELIC, CENTURY_TALISMAN, CENTURY_RING, CENTURY_ARTIFACT, NIBBLE_CHOCOLATE_STICK, SMOOTH_CHOCOLATE_BAR, RICH_CHOCOLATE_CHUNK, GANACHE_CHOCOLATE_SLAB, PRESTIGE_CHOCOLATE_REALM, LUCKY_HOOF, ETERNAL_HOOF, WHITE_GIFT_TALISMAN, GREEN_GIFT_TALISMAN, BLUE_GIFT_TALISMAN, PURPLE_GIFT_TALISMAN, GOLD_GIFT_TALISMAN, COIN_TALISMAN, RING_OF_COINS, ARTIFACT_OF_COINS, RELIC_OF_COINS, CARNIVAL_MASK_BAG, NEW_YEAR_CAKE_BAG, PANDORAS_BOX, BOOK_OF_PROGRESSION, PULSE_RING, COMBO_MANIA_TALISMAN, SHENS_REGALIA, ARCHAEOLOGIST_COMPASS, NETHERRACK_LOOKING_SUNSHADE, DANTE_TALISMAN | **LATER** mayors, Bingo, Century Cakes, the Chocolate Factory, Trick or Treat chests, gifts, coins on public islands, Carnival Masks, New Year Cakes, challenges, the Book's modifiers, thunder bottles, the Time Chamber, Shen's room, relics, Dive Ghasts, Dante's goons | |

Only stats or flavour text (nothing to do beyond their stats): ANGUISH_TALISMAN, ANGUISH_RING, ANGUISH_ARTIFACT, APPLICANT_STATEMENT,
BLOOD_DONOR_TALISMAN, BLOOD_DONOR_RING, BLOOD_DONOR_ARTIFACT, CHUMMING_TALISMAN, DANTE_RING, DEFECTIVE_MONITOR, FRIED_FROZEN_CHICKEN,
GREAT_SPOOK_TALISMAN, GREAT_SPOOK_RING, GREAT_SPOOK_ARTIFACT, JERRY_TALISMAN_GREEN, JERRY_TALISMAN_BLUE, JERRY_TALISMAN_PURPLE,
JERRY_TALISMAN_GOLDEN, MASTER_THESIS, MELODY_HAIR (its tune-up clicks are the Rift's), MINIATURIZED_TUBULATOR, ODGERS_BRONZE_TOOTH,
ODGERS_SILVER_TOOTH, ODGERS_GOLD_TOOTH, ODGERS_DIAMOND_TOOTH, PHD_GRIMOIRE, PIGS_FOOT, RING_OF_BROKEN_LOVE, RING_OF_ETERNAL_LOVE,
SAFETY_BADGE, STUDENT_STUDIES, TEST_BUCKET_PLEASE_IGNORE, WITCH_TALISMAN, WITCH_RING, WITCH_ARTIFACT.

## UNKNOWN and approximations

Each is a one-line comment with UNKNOWN where it's decided in the code.

- The Reaper Orb has no most; the Gravity Talisman loses a point every 10 blocks from the world's spawn.
- The Blood God Crest's "Counter" line for a count, and that it counts only while it's the one that counts.
- The IQ Points' and Master Skulls' shares are of the stats as they are when the accessories' modifier comes (after
  the set bonuses', before a run's blessings).
- The Bluetooth Rings' island is the server's world; the Burststopper's 50% is of their health now.
- The Haste Ring's Haste is Mining Speed, 50 a level.
- Night Vision counts from the bag as well as the inventory.
- Shop discounts: the most of them counts, rounded up to a coin, on every shop here.
- The Bucket of Dye's 1% is a factor on the drop's chance.
- The Wedding Rings don't ask for their quest.
- The Intimidation line leaves out dungeon mobs, whose names show no level.

## For the other parts

- A run's experience: once RunBoosts (the armor enchantments part) is merged, `RunBoosts.addBoost((player, score) ->
  { double[] b = AccessoryEffects.runBoost(player); return new RunBoosts.Boost(b[0], b[1]); })` makes the Catacombs
  Expert Ring and Scarf's items work.
- `AccessoryBag.counted(player)` is the set of counted accessory ids, cheap to ask on every hit.
- The Piggy Banks ("Saves your coins from death ... Triggers when losing 20k+ coins") can plug into
  `DeathCoins.addSaver` once this part and the purse loss on death are merged.
