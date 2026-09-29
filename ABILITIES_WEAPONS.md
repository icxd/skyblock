# Weapon abilities

What weapons' click abilities do, as on Hypixel: the ones that hit (swords, bows, wands, staves), and the
few on weapons that don't; what weapons do beyond their stats and clicks (their passives: "Weapon
passives"); and what worn armor and equipment do that hits ("Worn pieces that hit"). The code is in
`paper/src/main/java/net/icxd/dungeons/item/ability/weapons`; `WeaponAbilities.register` hands each
handler to `Abilities` by the exact name the item data's ABILITY block carries (`WeaponAbilitiesTest`
checks every name against the data). Costs, cooldowns and the "usable" checks are `Activations.use`'s, as
for every ability (health costs too: EFFECTS.md). Each reads its numbers from its item's text wherever the
text has them.

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
  whole, Implosion's aren't). A spell that lands later (a skull, a bat, a wave) takes its base and
  enchantments from the item it was cast with, and Intelligence and Ability Damage as they are when it
  lands (UNKNOWN which Hypixel's are).
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
- **Frozen** mobs (Ice Spray) take 10% more from every hit of a player's, abilities', melee hits' and
  arrows' ("take 10% more damage from all sources", the wiki): a mob debuff (`MobDebuffs`, EFFECTS.md).

## Done

| Ability | Items | Status | What, and what's UNKNOWN | Sources |
|---|---|---|---|---|
| Implosion | Implosion scroll; the 5 Necron's Blades with it | DONE | 10,000 base, scaling 0.3, 6 blocks around them. Look/sound UNKNOWN (explosions). | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion), mods' "Your Implosion hit" |
| Wither Shield | scroll; blades | APPROX | 10% less damage taken for 5 s, and an absorption shield "(12 + Cata x 0.32) x 50" for as long (`Absorption`, EFFECTS.md); the Vitality comes back 5 s after each cast, by the share of its absorption left when it ended (then, or when a new cast's took its place), so fast casts wait for their refunds as before: how Hypixel works the refund out is UNKNOWN. | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion), [Absorption](https://hypixelskyblock.minecraft.wiki/w/Absorption) |
| Shadow Warp | scroll; blades | APPROX | Teleports 10 ahead, pulls enemies within 6 into the warp, a second use within 5 s detonates it (Implosion's damage and size). That it teleports (from the wiki's trivia), the second use's mana and when the 10 s cooldown starts are UNKNOWN. | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion) |
| Wither Impact | the 5 Necron's Blades with all 3 scrolls | DONE | Teleport 10 (Instant Transmission's path), Implosion, Wither Shield only with 50 Vitality; 0.15 s between casts, silent. | [Scrolls](https://hypixelskyblock.minecraft.wiki/w/Implosion), 0.26.1 notes |
| Giant's Slam | Giant's Sword (1) | DONE | Recorded: 5 blocks ahead of the feet, the giant sword display (5x, 3 up, 135°, 5.8 s), anvil + thunder at 0.492, whole hits and the chat line; wiki: radius 8, 100,000 / 0.05. | recordings; [Giant's Sword](https://hypixelskyblock.minecraft.wiki/w/Giant%27s_Sword) |
| Salvation | Terminator (1) | APPROX | After 3 arrow hits on mobs, a left click beams (32 blocks, 5 enemies, always a crit, the bow's arrow damage), 0.25 s cooldown; fewer hits: arrows as ever. 1 Soulflow not charged (no Soulflow). Blocks stop it, its width, look and sound UNKNOWN. Action bar T1/T2/T3! LATER (no place on the action bar). | [Terminator](https://hypixelskyblock.minecraft.wiki/w/Terminator) |
| Guided Bat | Spirit Sceptre (2) | DONE | A bat steered at the crosshair, 6 block blast; speed/range UNKNOWN. | [Spirit Sceptre](https://hypixelskyblock.minecraft.wiki/w/Spirit_Sceptre) |
| Showtime | Bonzo's Staff (2) | DONE | Balloons 4/s, 2 block blasts that throw the caster back; the launch's strength, look, speed UNKNOWN. | [Bonzo's Staff](https://hypixelskyblock.minecraft.wiki/w/Bonzo%27s_Staff) |
| Ice Spray | Ice Spray Wand (2) | DONE | 7 x 60° cone through walls, frozen 5 s; frozen mobs take 10% more from every hit of a player's (see above). | [Ice Spray Wand](https://hypixelskyblock.minecraft.wiki/w/Ice_Spray_Wand) |
| Dragon Rage | Aspect of the Dragons (1) | DONE | 7.5 x 60° cone, no additive buffs, knockback (its strength UNKNOWN). | [Aspect of the Dragons](https://hypixelskyblock.minecraft.wiki/w/Aspect_of_the_Dragons) |
| Molten Wave | Midas Staff (2) | APPROX | 14 block wave of gold, stopped by walls and carpets, not within a block of them. Greed's bonus from the Dark Auction price LATER (no Dark Auction). | [Midas Staff](https://hypixelskyblock.minecraft.wiki/w/Midas_Staff) |
| Terrain Toss | Yeti Sword (2) | APPROX | A lob to the crosshair (32), the blast's size (5) and falloff (linear to none) UNKNOWN. | [Yeti Sword](https://hypixelskyblock.minecraft.wiki/w/Yeti_Sword) |
| Rapid-fire | Jerry-chine Gun (1) | APPROX | 30 mana more a shot, reset after 4 s, 5/s, 3° spread, direct hits, knocks the user back near the impact. Shooting through 1 block walls: not. | [Jerry-chine Gun](https://hypixelskyblock.minecraft.wiki/w/Jerry-chine_Gun) |
| Dreadlord | Dreadlord Sword (1) | DONE | A wither skull, 3 block blast, 500 / 0.3. | [Dreadlord Sword](https://hypixelskyblock.minecraft.wiki/w/Dreadlord_Sword) |
| Witherlord | Crypt Witherlord Sword (1) | APPROX | 3 skulls 5° apart, Dreadlord's blast (no wiki page: spread, size UNKNOWN). | its data |
| Throw | Livid Dagger, Halberd of the Shredded (2 of 6) | APPROX | The dagger's melee hits through its path; the halberd's 10% that doubles with each consecutive throw, with its mana (max 16x; "consecutive" as within 4 s is UNKNOWN). The weapons' own passives: see "Weapon passives". The 4 lassos' Throw: LATER (Forest). | [Livid Dagger](https://hypixelskyblock.minecraft.wiki/w/Livid_Dagger), [Halberd](https://hypixelskyblock.minecraft.wiki/w/Axe_of_the_Shredded) |
| Shadow Fury | Shadow Fury (2) | DONE | Behind up to 5 enemies in 12 blocks, rooted, a real melee hit each; timing UNKNOWN (5 ticks); only enemies they can see (UNKNOWN), so it can't take them through a wall. | [Shadow Fury](https://hypixelskyblock.minecraft.wiki/w/Shadow_Fury) |
| Heat-Seeking Rose | Flower of Truth (1) | DONE | Homing after 5 blocks (within 10), 3 bounces x1 x2 x3, melee-style damage, 100 health not from the last of it. | [Flower of Truth](https://hypixelskyblock.minecraft.wiki/w/Flower_of_Truth) |
| Petal Barrage | Bouquet of Lies (1) | DONE | 3 roses (10° apart, UNKNOWN), 5 bounces each, +2% per 10% health missing. | [Bouquet of Lies](https://hypixelskyblock.minecraft.wiki/w/Bouquet_of_Lies) |
| Swing | Bonemerang (2) | APPROX | 13.5 out and back, double back, pierces 10, shatters 3 s on a block or its 11th foe. The Ghast Tear in its place while shattered: not. | [Bonemerang](https://hypixelskyblock.minecraft.wiki/w/Bonemerang) |
| Flay | Soul Whip, Flaming Flay (2) | APPROX | The wiki's parabola, full melee on 3 then halved; hidden 0.5 s. Flaming Flay's Sea Creature heal LATER. | [Soul Whip](https://hypixelskyblock.minecraft.wiki/w/Soul_Whip) |
| Reaving Strike | Bone Reaver, Felthorn Reaper (3) | APPROX | 125/135% melee (off the text) in an arc (size UNKNOWN), charges 4/5 s and 5/4 s. The blocks' missing-health bonuses are on the weapons' melee hits (see "Weapon passives"); not on the strike's own hits (UNKNOWN whether Hypixel's are). | [Bone Reaver](https://hypixelskyblock.minecraft.wiki/w/Bone_Reaver), [Felthorn Reaper](https://hypixelskyblock.minecraft.wiki/w/Felthorn_Reaper) |
| Rapid Fire | Machine Gun Shortbow (1) | APPROX | 5 arrows a second for 8 s at 70% (right click, as its data has it); stops once the bow leaves their hand (UNKNOWN: each arrow is worked out with what they hold then). | [Machine Gun Shortbow](https://hypixelskyblock.minecraft.wiki/w/Machine_Gun_Shortbow) |
| Ragnarock | Ragnarock Axe (1) | APPROX | 3 s channel broken by a SkyBlock hit, then 1.5x the weapon's Strength for 10 s (whether that's +1.5x or +0.5x UNKNOWN). | [Ragnarock Axe](https://hypixelskyblock.minecraft.wiki/w/Ragnarock_Axe) |
| Ice Bolt | Frozen / Glacial Scythe (3) | DONE | A bolt, slows 5 s; the Glacial's 3 block explosion off its text. | [Frozen Scythe](https://hypixelskyblock.minecraft.wiki/w/Frozen_Scythe), [Glacial Scythe](https://hypixelskyblock.minecraft.wiki/w/Glacial_Scythe) |
| Ink Bomb | Ink Wand (1) | DONE | An arc, 0.5 block hit, blindness (5 s, UNKNOWN). | [Ink Wand](https://hypixelskyblock.minecraft.wiki/w/Ink_Wand) |
| Fire Blast | Ember Rod (1) | DONE | 3 fireballs 5 ticks apart. | [Ember Rod](https://hypixelskyblock.minecraft.wiki/w/Ember_Rod) |
| Coin Conversion | Alchemist's Staff (1) | DONE | 100,000 coins (off its text) from the purse, a bolt that knocks back; the "not enough coins" words UNKNOWN. | [Alchemist's Staff](https://hypixelskyblock.minecraft.wiki/w/Alchemist%27s_Staff) |
| Ray of Hope | Staff of the Rising Sun (2), Staff of the Rising Moon (1) | APPROX | A ray to the crosshair, blast 3 (UNKNOWN), its % of mana off the text; scaling UNKNOWN (0.3). The Rising Moon's moon flies and bursts but hurts nothing: it's only for Wizardman (the Rift's), who isn't here. | its data |
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
| ME SMASH HEAD | Edible Mace (1) | DONE | The next melee hit (or an ability's worked out as one) doubled; an Animal mob it lands on deals the text's 35% less to players for 30 s, a new one starting the time again ("Debuff doesn't stack"). | [Edible Mace](https://hypixelskyblock.minecraft.wiki/w/Edible_Mace) |
| Gravity Storm | Gyrokinetic Wand (1 of its 2) | APPROX | A rift at the crosshair pulling mobs in 8 for 3 s (UNKNOWN). "Regen mana 10x slower for 3s after cast": a tenth of their mana regeneration for the text's time (`StatsRunnable.addManaRegenFactor`). Its 10 Soulflow isn't charged (no Soulflow). The wand's Cells Alignment is ABILITIES_UTILITY.md's. | its data |
| Fire Freeze | Fire Freeze Staff (1) | APPROX | A 5 block circle where they cast it; 5 s on, every mob in it rooted for 10 s. Where the circle is, and the wiki's "does not work on regular Dungeon minibosses", UNKNOWN; look UNKNOWN. | [Fire Freeze Staff](https://hypixelskyblock.minecraft.wiki/w/Fire_Freeze_Staff) |
| Juju's "Hits 3 mobs on impact" | Juju Shortbow (1) | DONE | Its lore's number: the arrow's own hit, and one each on the rest within 3 blocks of where it landed. | [Juju Shortbow](https://hypixelskyblock.minecraft.wiki/w/Juju_Shortbow) |
| Instant Transmission | Aspect of the End / Void (2) | DONE | Already built; its teleport path is reused by Wither Impact, Shadow Warp and Sinrecall. | |
| Chain of Agony | Tormentor (1) | APPROX | "Toggles On/Off on use": a click switches it and costs nothing. While on, a melee hit arcs from the mob it hit to the next within 3 blocks, up to the text's 5, each jump the hit's damage for the block's 200 mana (with what makes the item cheaper), the block's 0.5 s between chains (the wiki's 2026 history: "Each jump costs 200 Mana, with a 0.5s cooldown"); it stops when their mana runs short. Look, sound and the toggle's words UNKNOWN. | [Tormentor](https://hypixelskyblock.minecraft.wiki/w/Tormentor) |
| Arrow Infusion (ON_SHOOT) | Magma, Sulphur, Slime Bow (3 of 4) | APPROX | A drawn shot uses up 1 of the item the text names from their inventory (found by its name in the data) for double damage; the Slime Bow's Slimeballs give 5x on Cubic mobs (UNKNOWN which mobs "Magma Cubes and Slimes" are; one a shot, UNKNOWN). Without one, the shot is as ever. The Quiver's: LATER (no Quiver). The Prismarine Bow's (sea creatures): LATER. | their data |
| Extreme Focus | End Stone Bow (1 of 2) | DONE | Left click: all their mana is spent and added to their next hit (melee or arrow) as damage only a crit multiplies (the wiki's "Add Damage"). The End Stone Sword's ("While in the The End"): LATER (no End). | [End Stone Bow](https://hypixelskyblock.minecraft.wiki/w/End_Stone_Bow) |
| Detonate | Miniature Nuke (1, an admin item) | DONE | "dealing 80.0% max health true damage to all enemies in a 20 block radius": each mob's share of its max health, no Defense, times Consolidated; the Nuke stays (UNKNOWN). The Creeper Pants' Detonate: see "Worn pieces that hit". | its data |
| Homing Missiles (SNEAK, worn) | Aurora Armor (20) | APPROX | At 10 stacks, a sneak spends them and shoots the text's 3/4/5 missiles of 5,000 (magic, scaling 0.3: the armor inventory's reading of the wiki), each after the nearest mob in 20 blocks. The stacks are the Aurora Armor's Arcane Energy, the armor bonuses' (BONUSES.md), given to it through `WeaponAbilities.arcaneEnergy`: until then there are none, so it never goes off. How the missiles fly and look is UNKNOWN. | [Aurora Armor](https://hypixelskyblock.minecraft.wiki/w/Aurora_Armor) |
| Ground Pound (SNEAK, worn) | Fervor Armor (20) | APPROX | At 10 Fervor stacks (the armor bonuses' tiered Fervor, `SetBonuses.fervor`), a sneak spends them and pounds (the text's 1 or 2, half a second apart) every mob within its text's blocks. "Damage scales with your EHP" has no formula anywhere: UNKNOWN, their EHP (max health x (1 + Defense / 100)) as an ability's hit, at most 25M (the wiki's "up to 25,000,000"). | [Fervor Armor](https://hypixelskyblock.minecraft.wiki/w/Fervor_Armor) |
| Eye Beam | Precursor Eye (SNEAK, worn), Giant's Eye Sword (right click) (2) | APPROX | A toggle: once a second every mob on a 30 block beam (the wiki's range; width UNKNOWN, half a block) takes its damage (4,000 growing its text's 100% a second for 5 s), as it is ("Intelligence and Ability Damage do not work", the wiki), and that second's mana (40 growing 25% a second) is paid if it hit something ("Mana is consumed on damage"); it stops when they can't pay, use it again, or take the piece off. The sword's is x1.5 with the Eye worn. Whether the growth compounds is UNKNOWN (it doesn't). | [Precursor Eye](https://hypixelskyblock.minecraft.wiki/w/Precursor_Eye) |
| Stun Potion (a ghost's) | the Tank's ghost ability item (1) | APPROX | Thrown (an arc); every mob but a boss in its text's 10 blocks of where it breaks is stunned: rooted, its hits on players do nothing, it lets go of its target (`Hits.stun`). How long is UNKNOWN (3 s). The ghost items themselves: ABILITIES_UTILITY.md, "Dungeon ghosts". | [Ghosts](https://hypixelskyblock.minecraft.wiki/w/Ghosts) |
| Spirit Bomb (a ghost's) | Spirit Sword, Spirit Shortbows (3) | APPROX | "Spirit Item: When turned into a ghost, this item becomes a Ghost Ability.": a ghost's right click shoots a spirit, and the first mob it touches takes the text's 8,000 as magic damage with no Intelligence scaling (UNKNOWN, as its look, speed and whether it's a blast are). The block's 10 s cooldown; the shortbows' blocks have none, so they take the sword's (UNKNOWN). Alive, the items do nothing more (the text says it's a ghost's). | [Spirit Sword](https://hypixelskyblock.minecraft.wiki/w/Spirit_Sword) |
| Gorilla Tactics | Tactical Insertion (1) | DONE | Marks where they are; 3 s later (the text's) they're back there with half the health they had just before, and each mob within 3 blocks burns for the text's 10% of everything they dealt in those seconds (heard through `CombatState.addDealtListener`), a sixth a second for 6 s, with no Defense ("can deal full damage to all mobs, regardless of any damage reduction", the wiki). Nothing if they're gone, dead or a ghost by then. Look UNKNOWN. | [Tactical Insertion](https://hypixelskyblock.minecraft.wiki/w/Tactical_Insertion) |

The Terminator's arrows (and every player's) can't be picked up once they land (`Shots.record`); the
code keeps `setShooter` before it, since a player shooter makes an arrow pickable again (checked against
the 26.2 server's `AbstractArrow.setOwner`).

## Weapon passives

What a weapon does beyond its stats and its click ability, from its own text, read once per item
(`WeaponLore` for its lore's lines, `WeaponPassives.Weapon` for its passive ABILITY blocks): on its hits
through the core's landing buffs and hit listeners (EFFECTS.md), for the weapon the hit was dealt with
(the held one, or the bow an arrow left). Every damage one is a multiplicative multiplier, as the wiki's
Damage Calculation/Multiplicative Sources lists them. What grows with the holder or with counts kept on the
item is its item behaviour's (`item/behaviour/HeldStats`, `NecronsBlade`), on the holder's stats, so the
Stats menu and every hit have it. Tests: `WeaponPassivesTest`, `HeldStatsTest`.

| Passive | Items | Status | What, and what's UNKNOWN |
|---|---|---|---|
| "Deals +X% damage to <type> mobs", "Deal 2.5x damage to <type> mobs" | the Undead bows and swords, Revenant/Reaper swords, the halberd, Wither Bow, the Necron's Blades, the daggers, the Arthropod and Ender weapons (~30) | DONE | A mob of several types takes the largest of the factors for them (UNKNOWN whether they'd multiply: no weapon names two a mob has). The Arthropod, Ender and Mythological ones have no such mobs to hit yet. |
| "+X% damage to <type> Mobs for every 1% of your missing health" | Spirit Sword, Spirit Shortbows | DONE | 1 + X x the share of their health missing, on that type. |
| Reaving Strike's lines | Bone Reaver ("to Undead monsters for every 1% of your missing health"), Felthorn Reaper ("of missing health on the target") (3) | DONE | On their melee hits, the same way (the Felthorn's by the target's missing share). |
| "+200% damage while in water", "All damage dealt with this bow is doubled" | Prismarine Blade, Savanna Bow | DONE | The holder in water. |
| "+100❁ Strength against enemies who are in lava" | Blade of the Volcano | DONE | As the factor that Strength makes on (5 + Damage) x (1 + Strength / 100). |
| "Heal 10❤ per hit", "Regens 3 Mana on hit" | Revenant/Reaper swords, the halberd; the Rift's mana swords | DONE | Melee hits only (UNKNOWN whether Ferocity strikes and abilities count). Heals are `Heals.give`'s (the Catacombs boost in a run). |
| "Gain +45☯ Combat Wisdom against <type> mobs" | ~15 weapons | DONE | On the kill's XP alone, for what they hold when they kill (`SkillGains.addKillWisdom`). The Endermen ones: LATER. |
| "Receive -20% damage from Animal mobs" | Shaman Sword, Pooch Sword | DONE | While held. |
| "Your Critical Hits deal 100% more damage if you are behind your target" | Livid Dagger | DONE | Behind is the half of the ground plane the mob's back faces (UNKNOWN how wide Hypixel's is). |
| "Reduces the defense of your target by 10% of their max Defense on hit, stacking up to 5 times" | Last Breath (2) | APPROX | A Defense debuff per mob, stacking to 5 for everyone's arrows (UNKNOWN across players); how long it lasts is UNKNOWN (10 minutes: a fight's worth). |
| "Your arrows have a 50% chance to bounce to another target" | Death Bow, Super Undead Bow | APPROX | A new arrow of the bow from the mob it hit to the nearest other within 8 blocks (UNKNOWN), off mobs only, once (UNKNOWN). |
| "Fires a volley of 5 arrows. Arrows apply venom ..." | Venom's Touch | APPROX | The extra arrows 5° apart (UNKNOWN); the venom a damage over time of its text's damage a second. |
| "Replaces the arrows that you shoot with exploding wither skulls!" | Crypt Bow | APPROX | A skull as the Crypt swords' in the arrow's place, their blast (500 base, 0.3: no page gives the bow's). |
| "Allows you to shoot arrows much further!" | Sniper Bow | APPROX | Its arrows half as fast again (UNKNOWN how much). |
| "Ignites enemies for 3s." | Flaming Sword | APPROX | The mob burns on Fire Aspect's damage over time ("fire", so the two don't burn twice), a second's worth Fire Aspect I's 3% of the hit (the text gives only the time: UNKNOWN), through its Defense. |
| The aura: "deals 360❁ Damage to nearby enemies instead of dealing impact damage. Arrows travel through enemies." | Spider Queen's Stinger (2) | DONE | Each mob within 2.5 blocks of the arrow takes 360 + 3.6 x Strength (the wiki's), once an arrow (UNKNOWN), as true damage; the arrow goes through mobs. |
| Cleave | the 4 Cleavers | APPROX | Each other mob within the text's blocks of the one hit takes 50% of the hit next to it down to 40% at the edge (the wiki's "typically ranges from 40% to 50%"; the line between UNKNOWN), as an effect's damage. |
| Love Tap | Zombie Soldier Cutlass | DONE | "+10❤ Health when you hit an entity while in Dungeons". |
| Stinger | Sting | DONE | Its melee hits always crit (`Combat.addAlwaysCrits`). Its Webcutter: LATER. |
| Angered | Tormentor | DONE | "You take +50% damage while holding this weapon." |
| Commander Whip | Zombie Commander Whip | APPROX | +1 Strength for each Zombie it kills in the run (counted on the whip for the run it's in; a new run starts again). Which mobs are "Zombies" is UNKNOWN: zombies and the ones named so. |
| Tempest | Hurricane Bow | APPROX | Its kills counted on it; 2, 3, 4 and 5 arrows at 20, 50, 100 and 250 kills (20 and 250 are the text's and the wiki's; 50 and 100 UNKNOWN), the extras at 7.5° and 15° with full damage (the wiki). |
| Triple Shot | Runaan's Bow | APPROX | 2 more arrows at 12.5° for the text's 40%, without Power and Piercing, homing within 10 blocks (the wiki's "10{{Confirm}}"). |
| Explosive Shot | Explosive Bow | APPROX | Every other mob within 3 blocks of where the arrow lands takes its damage (the shot's stats, each its own crit roll: UNKNOWN), times Consolidated; in a run it's Superboom TNT there (crypts and weak walls). |
| Sting | Stinger Bow | APPROX | Its arrow on a living teammate in their run: the text's +20 Speed and +30 Strength for 30 s and 1 damage (Defense doesn't count: UNKNOWN), for the block's 100 mana; without the mana, nothing. |
| Vis Temperata | the 5 hunting axes | DONE | A melee hit with one, and each of its Ferocity strikes, deals at most its text's share of the mob's max health, and never its last health (`Combat.addHitCap`). Their Absorptio: LATER. |
| Necron's Blades' per level stats | Necron's Blade, Hyperion, Astraea, Scylla, Valkyrie | DONE | "Grants +1 ❁ Damage and +2 ✎ Intelligence per Catacombs level": times the holder's Catacombs level (at most 50). |
| "Right-click to use your class ability!" | Necron's Blades without scrolls, Stone Blade, Earth Shard, Fel Sword | DONE | In a run, their class's ability, as the Dungeon Orb's right click (`RunItems.classAbility`); outside one, nothing. The Earth Shard takes its 2 s off Seismic Wave's cooldown, the Fel Sword its 10 s off Wish's. |
| Stone Blade | Stone Blade (2) | APPROX | In a run, the stats its text gives the class they play; the Mage's "melee attacks restore 25% additional mana" (of a hit's mana back), the Archer's arrows taking 10% more on mobs it hit for 5 s. The Healer's tether: LATER (no tether). |
| Stats from the holder | Shaman Sword (Damage per 50 max health), Void Sword (Strength per Ender Armor piece), Tactician's Sword (Combat collections at VII), Emerald Blade (the wiki's 2.5 x coins^(1/4), to 2B), Great Spook Sword (Fear) | DONE | Their text's numbers. The Emerald Blade's and Void Sword's "Current" lines stay their data's (an item's text isn't built for who holds it). Nothing gives Fear yet. |
| Counts on the item | Fel Sword (kills), Promising Pickaxe (blocks mined: Stored Potential) | DONE | `ItemCounters`, with their count lines. The Fel Sword's Healing Circle: LATER (no Healing Circle). |

## Worn pieces that hit

Worn armor's and equipment's passive ABILITY blocks (`WornStrikes`; the ones that don't hit are
ABILITIES_UTILITY.md's): each is a `Bonus` of the kind "ABILITY" that SetBonuses counts while a piece
with the block is worn, as it counts a piece's own bonus (`WornAbilities`). Their damage is an effect's
(`MobHits.deal`: no hit listeners, no Ferocity) unless said. Tests: `WornStrikesTest`.

| Ability | Items | Status | What, and what's UNKNOWN |
|---|---|---|---|
| Spiky | Pufferfish Hat (2) | APPROX | A melee hit's chance to hurt each other mob near them for 10 + 20% of their Strength (the text's): the chance and how near are UNKNOWN (25%, 3 blocks). |
| Rejuvenate | Vampire Mask, Vampire Witch Mask (2) | DONE | Each second, 5 from every monster within 8 (the text's), healing them that much ("The wearer heals for the amount the ability damages mobs", the wiki). Not a room's mobs still waiting for it to open, so walking past a room doesn't open it (a hit on one does). |
| Detonate | Creeper Pants (1) | APPROX | The hit that takes them below the text's 20% of their health sets it off, its 60 s cooldown the block's: what it deals is UNKNOWN (a melee hit of what they hold on each mob within 5 blocks, times Consolidated), and it knocks those mobs back. |
| Contaminate | Gauntlet of Contagion (1) | APPROX | A kill blows up for 10% of the mob's max health on every mob within 2 blocks (the text's), times Consolidated; those it doesn't kill are contaminated and blow up the same way when they die, however they die (UNKNOWN), for the wearer. A death is one blast: the wearer's kill of a contaminated mob doesn't blow it up twice (UNKNOWN whether Hypixel's is two). |
| Bat Swarm | Witch Mask, Vampire Witch Mask (2) | APPROX | Two bats circle their head; once a second, with a mob within 8 (not one still waiting for its room to open), one dives at a mob and explodes for the text's damage (4,000 or 3,000) as magic damage with no Intelligence scaling, in 3 blocks, times Consolidated, and it's back 5 s later (taking the mask off and on doesn't hurry it). The bats' movement, blast and time back are UNKNOWN. |
| Brute Force | Warden Helmet (1) | DONE | "Halves your +25✦ Speed but grants +20% base weapon damage for every +25✦ Speed": their Speed up to the cap halved, then 20% additive for each whole 25 of it and 1% more (the wiki's Warden Helmet and Additive Sources: 20 x Speed / 25 + 1). |
| Consolidated | Implosion Belt (1) | APPROX | "Increases all explosion damage dealt by 25%": x1.25 on what's an explosion, which is UNKNOWN: what the text calls one (Implosion, Shadow Warp's and Wither Impact's, the Spirit Sceptre's bat, Bonzo's balloons, Explode, the Glacial Scythe's blast, Ray of Hope, the Crypt skulls, the Explosive Bow's, both Detonates, Contaminate, the Witch masks' bats and the Dungeon Trap). |

Armor's SNEAK abilities that hit (Homing Missiles, Ground Pound, Eye Beam) are in "Done": worn pieces' SNEAK
blocks reach their handlers through the core's activation dispatch.

## Hooks this part added

Small and self-contained, each where the rule is (the lead may move them into EFFECTS.md):

- `Combat.addAlwaysCrits` (a melee hit's crit roll), `Combat.addHitCap` (the most a hit may deal).
- `CombatState.addDealtListener` (every bit of damage a player deals, from the mob sinks' `CombatState.dealt(player, damage)`).
- `StatsRunnable.addManaRegenFactor` (a factor on mana regeneration, after its bonuses).
- `SkillGains.addKillWisdom` (Combat Wisdom on one kill's XP), `BlockListener.addBrokenListener` (a block mined).
- `Hits.stun` / `Hits.stunned` (rooted, and its hits on players do nothing, through a `PlayerDamage.addTakenFrom`
  factor of 0); `Missile.byGhost` (a ghost's missile flies on).
- `dungeons/instance/RunItems`: what items ask of the run (the class ability, Superboom at a block, the nearest
  unfound secret, whether a Dungeonbreaker may break a block, a failed puzzle reset, ghosts and their items).
- For the armor bonuses to give (BONUSES.md): `WeaponAbilities.arcaneEnergy` (the Aurora Armor's Arcane Energy
  stacks, for Homing Missiles) and `UtilityAbilities.hollowSpirit` (the Hollow Armor's ⚶ Spirit stacks, for
  the Hollow Wand). Until they're given, both abilities never go off.

## LATER

| Ability | Items | Waits for |
|---|---|---|
| Souls Rebound's mark ("Marked Enemies don't take damage from you, once the mark expires the target will receive a burst of damage") | Souls Rebound (1) | holding a hit's damage back to deal later: the hit pipeline deals every hit at once |
| Pack Mentality, Venom Mold, Bloodcrave | 1, 1, 1 | wolf mobs; poison on players; the Rift's vampires |
| "Can damage endermen" (Terminator, Juju), the katanas' damage, damage cut and Wisdom against Endermen, Soulcry | 2, 4, 3 | endermen (no mob is one yet) |
| The fishing weapons' "x damage to Sea Creatures", Flaming Flay's heal | 2 | fishing |
| Greed | 4 | the Dark Auction's price paid |
| Attunement, Burning Vengeance, Wraith Drain | 6, 3, 3 | the Blaze Slayer's Hellion Shield |
| Eggsecute, Webcutter | 2, 4 | the Tarantula Slayer |
| Dragon Stalker, Dragon Tracer | 1, 1 | dragons |
| Throwing Axe, Frenzy, Park Enjoyer/Enthusiast, Absorptio | 4, 3, 2, 5 | Foraging / Galatea (Absorptio: how the absorbed weapon is chosen is UNKNOWN) |
| Bejeweled Blade, Reduced To Atoms, Kinetic | 1, 1, 1 | Mining Islands |
| Spooktacular | 1 | the Spooky Festival |
| A bit of Impalement | 2 | the Rift's vampires |
| Throw (lassos), Swhooomp, Phwomp | 4, 1, 1 | the Forest, the Carnival |
| Bouncy!, Defense Reduction, Explosive, Muscle Freeze, Skewer, Stuck in goo; Arrow Infusion from the Quiver | arrows | the Quiver (STORAGE.md) |
| Arrow Infusion (Prismarine Bow) | 1 | sea creatures, squids and guardians |
| Extreme Focus (End Stone Sword) | 1 | The End |
| Raise Souls; Evil Incarnate's souls | 3; 1 | necromancy (summons) |
| Daedalus Axe, Raider Axe, the animal axes, Conjuring Sword | many | pets and bestiary, wood collections, animal mobs, the Mage's Guided Sheep |
| Shoot | Horsezooka (1) | Farming (it clears hay) |
| The halberd's and Sinrecall's growing mana on the action bar | 2 | one line for the whole cost: the extra is taken and shown in the use, then `Activations.use`'s line for the block's own cost ("-20 Mana (Throw)") replaces it |
| Salvation's T1/T2/T3!, Midas' Greed | - | a place on the action bar; the Dark Auction |
| The Stone Blade's Healer tether, the Fel Sword's Healing Circle | 2, 1 | the Healer's tether and Healing Circle (RunClasses) |

Not this part's: Parley (Aspect of the Jerry: "Channel your inner Jerry", nothing the wiki describes) and
Time Warp! (Wizard Wand: "a work in progress") are cosmetic, left to whoever does them. What the utility
abilities are (heals, buffs, movement, shields, tools, ghosts' Haunt and Absorption Potion) is
ABILITIES_UTILITY.md; both documents are this part's.
