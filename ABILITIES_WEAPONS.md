# Weapon abilities

What weapons' click abilities do, as on Hypixel: the ones that hit (swords, bows, wands, staves), and the
few on weapons that don't. The code is in `paper/src/main/java/net/icxd/dungeons/item/ability/weapons`;
`WeaponAbilities.register` hands each handler to `Abilities` by the exact name the item data's ABILITY
block carries (`WeaponAbilitiesTest` checks every name against the data). Costs, cooldowns and the
"usable" checks are PlayerListener's, as for every ability.

Sources: the item data (`items.json`: names, activations, mana, cooldowns, text, each item's own
Weapon Ability Damage), the wiki (hypixelskyblock.minecraft.wiki, the item pages and Damage
Calculation), the recorded Entrance runs (Giant's Slam), and the chat patterns the SkyHanni and
Skyblocker mods filter. Where they say nothing, the code says UNKNOWN and takes the simplest reading.

## How they hit

- **Magic damage** (`Magic`, `Hits.magic`): base x (1 + Intelligence / 100 x scaling) x (1 + Ability
  Damage / 100), then the additive buffs, then the target's magic resistance, caps and Defense. The base
  is the item's own Weapon Ability Damage where its data has one (so fragged items get theirs), else the
  wiki's; the scaling is the wiki's (its item page where the page and its table differ). On a dungeon
  item in a run the base is multiplied as the item's stats are (1 + 10% a star + the Catacombs boost:
  "10000 x (100% + 400%)"). Additive: the Combat skill's Warrior bonus and the enchantments that count for
  abilities (not Sharpness, First Strike, Triple-Strike, One For All); Dragon Rage and Burning Souls have
  none ("Not affected by the Additive Multiplier"). Multiplicative buffs: none (UNKNOWN which count for
  magic). Never a crit, no Ferocity; hits aren't rounded except Giant's Slam's (its recorded totals are
  whole, Implosion's aren't).
- **Hits worked out as the weapon's** (`Hits.weaponHit`): the held weapon's melee hit or arrow, times the
  ability's share, crits as Crit Chance rolls (Salvation always), no Ferocity strikes (UNKNOWN). The
  roses leave out the enchantments that don't count for abilities; Flay keeps them all ("Melee-only
  enchantments ... work on the beam"). Shadow Fury makes a real melee hit. What's thrown (the Throw
  blades, the roses, the Bonemerang, the Tribal Spear) and a Juju arrow's impact hit with the stats and
  weapon they had when it left them (`Hits.striker`), as an arrow does with its bow, not with whatever
  they hold when it lands.
- **Dealing it**: through `Mobs.damage` / `DungeonMobs.damage`, as melee's damage is: the mob's health,
  its gray (or crit) number, kills, drops and a run's damage dealt. Only SkyBlock's mobs that can be hurt
  are hit; never players. A room mob still waiting for its room to open (behind a wall, say) opens the
  room first, as a melee hit or an arrow on it does (`RunManager.abilityHit`), so it dies with the room's
  health, not its waiting one.
- **Missiles** (`Missile`): moved by the server a step a tick, stopped by the first block (unless said)
  and by the first mob unless they pierce, carrying an item display (or a bat) along; they stop when their
  caster leaves, dies or is a dungeon ghost.
- **Chat line**: "&7Your Giant's Sword hit &c3 &7enemies for &c8,103,803 &7damage." (recorded; one decimal
  when not whole). Names: Giant's Sword (recorded), Implosion, Molten Wave, Spirit Sceptre (the mods'
  patterns); the other area spells use their ability's name (UNKNOWN); single hits have none.
- **Frozen** mobs (Ice Spray) take 10% more from abilities' hits; from melee and arrows too on Hypixel,
  not yet (Combat has no factor for what a mob takes).

## Done

| Ability | Items | Status | What, and what's UNKNOWN | Sources |
|---|---|---|---|---|
| Implosion | Implosion scroll; the 5 Necron's Blades with it | DONE | 10,000 base, scaling 0.3, 6 blocks around them. Look/sound UNKNOWN (explosions). | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion), mods' "Your Implosion hit" |
| Wither Shield | scroll; blades | APPROX | 10% less damage taken for 5 s. Absorption shield "(12 + Cata x 0.32) x 50" LATER (players have no absorption); the unused Vitality (all 50) comes back after 5 s. | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion) |
| Shadow Warp | scroll; blades | APPROX | Teleports 10 ahead, pulls enemies within 6 into the warp, a second use within 5 s detonates it (Implosion's damage and size). That it teleports (from the wiki's trivia), the second use's mana and when the 10 s cooldown starts are UNKNOWN. | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion) |
| Wither Impact | the 5 Necron's Blades with all 3 scrolls | DONE | Teleport 10 (Instant Transmission's path), Implosion, Wither Shield only with 50 Vitality; 0.15 s between casts, silent. | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion), 0.26.1 notes |
| Giant's Slam | Giant's Sword (1) | DONE | Recorded: 5 blocks ahead of the feet, the giant sword display (5x, 3 up, 135°, 5.8 s), anvil + thunder at 0.492, whole hits and the chat line; wiki: radius 8, 100,000 / 0.05. | recordings; [Giant's Sword](https://hypixelskyblock.minecraft.wiki/w/Giant%27s_Sword) |
| Salvation | Terminator (1) | APPROX | After 3 arrow hits on mobs, a left click beams (32 blocks, 5 enemies, always a crit, the bow's arrow damage), 0.25 s cooldown; fewer hits: arrows as ever. 1 Soulflow not charged (no Soulflow). Blocks stop it, its width, look and sound UNKNOWN. Action bar T1/T2/T3! LATER (no place on the action bar). | [Terminator](https://hypixelskyblock.minecraft.wiki/w/Terminator) |
| Guided Bat | Spirit Sceptre (2) | DONE | A bat steered at the crosshair, 6 block blast; speed/range UNKNOWN. | [Spirit Sceptre](https://hypixelskyblock.minecraft.wiki/w/Spirit_Sceptre) |
| Showtime | Bonzo's Staff (2) | DONE | Balloons 4/s, 2 block blasts that throw the caster back; the launch's strength, look, speed UNKNOWN. | [Bonzo's Staff](https://hypixelskyblock.minecraft.wiki/w/Bonzo%27s_Staff) |
| Ice Spray | Ice Spray Wand (2) | APPROX | 7 x 60° cone through walls, frozen 5 s; the 10% more only from abilities (see above). | [Ice Spray Wand](https://hypixelskyblock.minecraft.wiki/w/Ice_Spray_Wand) |
| Dragon Rage | Aspect of the Dragons (1) | DONE | 7.5 x 60° cone, no additive buffs, knockback (its strength UNKNOWN). | [Aspect of the Dragons](https://hypixelskyblock.minecraft.wiki/w/Aspect_of_the_Dragons) |
| Molten Wave | Midas Staff (2) | APPROX | 14 block wave of gold, stopped by walls and carpets, not within a block of them. Greed's bonus from the Dark Auction price LATER (no Dark Auction). | [Midas Staff](https://hypixelskyblock.minecraft.wiki/w/Midas_Staff) |
| Terrain Toss | Yeti Sword (2) | APPROX | A lob to the crosshair (32), the blast's size (5) and falloff (linear to none) UNKNOWN. | [Yeti Sword](https://hypixelskyblock.minecraft.wiki/w/Yeti_Sword) |
| Rapid-fire | Jerry-chine Gun (1) | APPROX | 30 mana more a shot, reset after 4 s, 5/s, 3° spread, direct hits, knocks the user back near the impact. Shooting through 1 block walls: not. | [Jerry-chine Gun](https://hypixelskyblock.minecraft.wiki/w/Jerry-chine_Gun) |
| Dreadlord | Dreadlord Sword (1) | DONE | A wither skull, 3 block blast, 500 / 0.3. | [Dreadlord Sword](https://hypixelskyblock.minecraft.wiki/w/Dreadlord_Sword) |
| Witherlord | Crypt Witherlord Sword (1) | APPROX | 3 skulls 5° apart, Dreadlord's blast (no wiki page: spread, size UNKNOWN). | its data |
| Throw | Livid Dagger, Halberd of the Shredded (2 of 6) | APPROX | The dagger's melee hits through its path; the halberd's 10% that doubles with each consecutive throw, with its mana (max 16x; "consecutive" as within 4 s is UNKNOWN). The weapons' own passives LATER. The 4 lassos' Throw: LATER (Forest). | [Livid Dagger](https://hypixelskyblock.minecraft.wiki/w/Livid_Dagger), [Halberd](https://hypixelskyblock.minecraft.wiki/w/Axe_of_the_Shredded) |
| Shadow Fury | Shadow Fury (2) | DONE | Behind up to 5 enemies in 12 blocks, rooted, a real melee hit each; timing UNKNOWN (5 ticks). | [Shadow Fury](https://hypixelskyblock.minecraft.wiki/w/Shadow_Fury) |
| Heat-Seeking Rose | Flower of Truth (1) | DONE | Homing after 5 blocks (within 10), 3 bounces x1 x2 x3, melee-style damage, 100 health not from the last of it. | [Flower of Truth](https://hypixelskyblock.minecraft.wiki/w/Flower_of_Truth) |
| Petal Barrage | Bouquet of Lies (1) | DONE | 3 roses (10° apart, UNKNOWN), 5 bounces each, +2% per 10% health missing. | [Bouquet of Lies](https://hypixelskyblock.minecraft.wiki/w/Bouquet_of_Lies) |
| Swing | Bonemerang (2) | APPROX | 13.5 out and back, double back, pierces 10, shatters 3 s on a block or its 11th foe. The Ghast Tear in its place while shattered: not. | [Bonemerang](https://hypixelskyblock.minecraft.wiki/w/Bonemerang) |
| Flay | Soul Whip, Flaming Flay (2) | APPROX | The wiki's parabola, full melee on 3 then halved; hidden 0.5 s. Flaming Flay's Sea Creature heal LATER. | [Soul Whip](https://hypixelskyblock.minecraft.wiki/w/Soul_Whip) |
| Reaving Strike | Bone Reaver, Felthorn Reaper (3) | APPROX | 125/135% melee (off the text) in an arc (size UNKNOWN), charges 4/5 s and 5/4 s. The missing-health bonuses LATER. | [Bone Reaver](https://hypixelskyblock.minecraft.wiki/w/Bone_Reaver), [Felthorn Reaper](https://hypixelskyblock.minecraft.wiki/w/Felthorn_Reaper) |
| Rapid Fire | Machine Gun Shortbow (1) | APPROX | 5 arrows a second for 8 s at 70% (right click, as its data has it); stops once the bow leaves their hand (UNKNOWN: each arrow is worked out with what they hold then). | [Machine Gun Shortbow](https://hypixelskyblock.minecraft.wiki/w/Machine_Gun_Shortbow) |
| Ragnarock | Ragnarock Axe (1) | APPROX | 3 s channel broken by a SkyBlock hit, then 1.5x the weapon's Strength for 10 s (whether that's +1.5x or +0.5x UNKNOWN). | [Ragnarock Axe](https://hypixelskyblock.minecraft.wiki/w/Ragnarock_Axe) |
| Ice Bolt | Frozen / Glacial Scythe (3) | DONE | A bolt, slows 5 s; the Glacial's 3 block explosion off its text. | [Frozen Scythe](https://hypixelskyblock.minecraft.wiki/w/Frozen_Scythe), [Glacial Scythe](https://hypixelskyblock.minecraft.wiki/w/Glacial_Scythe) |
| Ink Bomb | Ink Wand (1) | DONE | An arc, 0.5 block hit, blindness (5 s, UNKNOWN). | [Ink Wand](https://hypixelskyblock.minecraft.wiki/w/Ink_Wand) |
| Fire Blast | Ember Rod (1) | DONE | 3 fireballs 5 ticks apart. | [Ember Rod](https://hypixelskyblock.minecraft.wiki/w/Ember_Rod) |
| Coin Conversion | Alchemist's Staff (1) | DONE | 100,000 coins (off its text) from the purse, a bolt that knocks back; the "not enough coins" words UNKNOWN. | [Alchemist's Staff](https://hypixelskyblock.minecraft.wiki/w/Alchemist%27s_Staff) |
| Ray of Hope | Staff of the Rising Sun (2 of 3) | APPROX | A ray to the crosshair, blast 3 (UNKNOWN), its % of mana off the text; scaling UNKNOWN (0.3). The Rising Moon's (Wizardman, an event's): LATER. | its data |
| Runic Zap | Aurora Staff (1) | APPROX | First enemy on the beam, decaying e^(-0.0225 x blocks past 10). Runes and Aurora Armor LATER. | [Aurora Staff](https://hypixelskyblock.minecraft.wiki/w/Aurora_Staff) |
| Bingo Blast | Bingo Blaster (1) | DONE | Piercing ray 15 blocks, halved each enemy. | [Bingo Blaster](https://hypixelskyblock.minecraft.wiki/w/Bingo_Blaster) |
| Explode | Staff of the Volcano (1) | APPROX | 4 blocks around them, 1,000 health; the fire is for show (mobs take no vanilla damage). | [Staff of the Volcano](https://hypixelskyblock.minecraft.wiki/w/Staff_of_the_Volcano) |
| Iron Punch | Golem Sword (1) | APPROX | The "hexagon" as a 4 block ball (UNKNOWN). | [Golem Sword](https://hypixelskyblock.minecraft.wiki/w/Golem_Sword) |
| Lightning Strike | Celeste Wand (1) | APPROX | At the crosshair (10), the nearest mob within 3 (UNKNOWN); base 250 / 0.3 (the page; its text says 40). | [Celeste Wand](https://hypixelskyblock.minecraft.wiki/w/Celeste_Wand) |
| Leap | Leaping / Silk-Edge Sword (2) | DONE | Leap, on landing 4 blocks around, frozen 1 s; the leap's size UNKNOWN. | [Leaping Sword](https://hypixelskyblock.minecraft.wiki/w/Leaping_Sword) |
| Fire Veil | Fire Veil Wand (1) | APPROX | 5 blocks around them for 5 s, once a second (its text; the wiki's prose says twice). | [Fire Veil Wand](https://hypixelskyblock.minecraft.wiki/w/Fire_Veil_Wand) |
| Firestorm | Fire Fury Staff (1) | DONE | A bolt, then 10 s of a tenth a second in 7 blocks. | [Fire Fury Staff](https://hypixelskyblock.minecraft.wiki/w/Fire_Fury_Staff) |
| Starfall | Starlight Wand (1) | DONE | At the crosshair (10), a quarter every 0.25 s for 5 s. The Starlight Armor's bigger cloud: the armor part's. | [Starlight Wand](https://hypixelskyblock.minecraft.wiki/w/Starlight_Wand) |
| Burning Souls | Pigman Sword (1) | DONE | +75 Defense 5 s, slow flames to each mob in 10 (UNKNOWN), 30,000 over 5 s each, no additive. | [Pigman Sword](https://hypixelskyblock.minecraft.wiki/w/Pigman_Sword) |
| Acupuncture | Voodoo Dolls (2) | APPROX | The crosshair's mob (30) and those within 2 (UNKNOWN), slowed, its damage a second for its text's seconds. | [Voodoo Doll](https://hypixelskyblock.minecraft.wiki/w/Voodoo_Doll) |
| Nasty Bite | Mosquito Shortbow (1) | APPROX | Left click with 10 Vitality: the bow's arrow, healing its text's 189 on a hit, 0.5 s apart; else arrows as ever. "Enhanced" UNKNOWN. | [Mosquito Bow](https://hypixelskyblock.minecraft.wiki/w/Mosquito_Bow) |
| Ender Warp | Ender Bow (1) | APPROX | A pearl that takes them where it lands (UNKNOWN), 10% of each monster's health in 8, max 500. | [Ender Bow](https://hypixelskyblock.minecraft.wiki/w/Ender_Bow) |
| Thwack | Tribal Spear (1) | DONE | Thrown 20 blocks through every mob (an arrow's hit), then back. | [Tribal Spear](https://hypixelskyblock.minecraft.wiki/w/Tribal_Spear) |
| Sinrecall Transmission | Sinseeker Scythe (1) | DONE | 4 block zaps hitting what the line crosses, recasts within 1 s at 1.5x the mana each, then back to the start. | [Sinseeker Scythe](https://hypixelskyblock.minecraft.wiki/w/Sinseeker_Scythe) |
| Bad Health | Sword of Bad Health (1) | DONE | +5 Strength per 5% health for 5 s (max 100), after its 100 health. | [Sword of Bad Health](https://hypixelskyblock.minecraft.wiki/w/Sword_of_Bad_Health) |
| Hellstorm | Hellstorm Wand (1) | DONE | +5 Ability Damage, double damage taken, 30 s. | its data |
| ME SMASH HEAD | Edible Mace (1) | APPROX | The next melee hit doubled. Weakening Animal mobs LATER (mobs' damage has no per-target factor). | [Edible Mace](https://hypixelskyblock.minecraft.wiki/w/Edible_Mace) |
| Gravity Storm | Gyrokinetic Wand (1 of its 2) | APPROX | A rift at the crosshair pulling mobs in 8 for 3 s (UNKNOWN). Slower mana regen LATER (no hook), Soulflow not charged. | its data |
| Juju's "Hits 3 mobs on impact" | Juju Shortbow (1) | DONE | Its lore's number: the arrow's own hit, and one each on the rest within 3 blocks of where it landed. | [Juju Shortbow](https://hypixelskyblock.minecraft.wiki/w/Juju_Shortbow) |
| Instant Transmission | Aspect of the End / Void (2) | DONE | Already built; its teleport path is reused by Wither Impact, Shadow Warp and Sinrecall. | |

The Terminator's arrows (and every player's) can't be picked up once they land (`Shots.record`); the
code keeps `setShooter` before it, since a player shooter makes an arrow pickable again (checked against
the 26.2 server's `AbstractArrow.setOwner`).

## LATER

| Ability | Items | Waits for |
|---|---|---|
| Cleave, Stinger, Love Tap, Commander Whip, Pack Mentality, Venom Mold, Angered, Bloodcrave | 4, 1, 1, 1, 1, 1, 1, 1 | on-hit passives in the melee path (Combat) |
| the weapons' own passives (Hyperion's +50% to Wither mobs and per-level stats, Livid Dagger's crits from behind, the halberd's and Reaper Falchion's heal and Undead damage, the Spirit Shortbow's and Bone Reaver's missing-health bonuses) | many | the same |
| Extreme Focus, Chain of Agony | 2, 1 | a next-hit (flat) bonus and melee chaining in Combat |
| Eye Beam | Giant's Eye Sword (an alpha item nobody has), Precursor Eye (a helmet's, armor part) | - |
| Arrow Infusion, Triple Shot, Explosive Shot, Tempest, Bouncy!/Explosive/Stuck in goo/Skewer (arrows) | 4, 1, 1, 1, arrows | ON_SHOOT activations (not loaded), vanilla bows' shots, the quiver |
| "Can damage endermen" (Terminator, Juju) | 2 | endermen (no mob is one yet) |
| Soulcry | 3 | Ferocity against one mob type (Endermen) |
| Greed | 4 | the Dark Auction's price paid |
| Spirit Bomb | 3 | Thorn (Floor IV) |
| Attunement, Burning Vengeance, Wraith Drain | 6, 3, 3 | the Blaze Slayer's Hellion Shield |
| Eggsecute, Webcutter, Sting | 2, 4, 1 | the Tarantula Slayer; players hit by teammates' arrows |
| Dragon Stalker, Dragon Tracer | 1, 1 | dragons |
| Throwing Axe, Frenzy, Park Enjoyer/Enthusiast, Vis Temperata, Absorptio | 4, 3, 2, 5, 5 | Foraging / Galatea |
| Bejeweled Blade, Reduced To Atoms, Kinetic | 1, 1, 1 | Mining Islands |
| Spooktacular | 1 | the Spooky Festival |
| A bit of Impalement | 2 | the Rift's vampires |
| Throw (lassos), Swhooomp, Phwomp | 4, 1, 1 | the Forest, the Carnival |
| Hollow Spirit | 1 | LEFT_RIGHT_CLICK activations (not loaded) |
| Gorilla Tactics | 1 | not done: the burn is a share of all damage dealt in 3 s |
| Raise Souls | 3 | necromancy (summons) |
| Salvation's T1/T2/T3!, Wither Shield's absorption, Midas' Greed | - | a place on the action bar; absorption; the Dark Auction |

Not this part's (utility abilities, or armor): Enrage (the Enrager's taunt and its 10% less damage from
the taunted mobs: the utility part's), Instant Heal, Speed Boost, Weird Transmission, Shadowstep,
Creeper Veil, Fire Freeze, Cells Alignment, Soulward, Spirit Leap, the wands of healing, Deploy, Homing
Missiles, Ground Pound. Parley (Aspect of the Jerry: a sound) and Time Warp! (Wizard Wand: "a work in
progress") are left to whoever does them.
