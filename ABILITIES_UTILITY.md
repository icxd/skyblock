# Utility abilities

The item abilities that don't hit: movement, healing and shields, buffs, deployables and tools; worn
pieces' own abilities that don't hit ("Worn"); dungeon ghosts' abilities ("Dungeon ghosts"); and the
dungeon secret items ("Dungeon secret items"). The code is in
`paper/src/main/java/net/icxd/dungeons/item/ability/utility` (`UtilityAbilities` registers each handler
by the ability name items' ABILITY blocks carry; `UtilityListener` does what they need besides a click).
Abilities that hit are in `ABILITIES_WEAPONS.md` (this part's too).

What an ability does comes from its item's text wherever the text says it (`AbilityText` reads the
numbers: "+30❁ Strength", "for 20 seconds", "within 18 blocks", "up to 5 players", "Heal for 1,000❤"),
so an item whose numbers differ (the Weirder Tuba's 30 seconds, a Plasmaflux's 20 blocks) does what
it says. Mana, mana %, Vitality, health costs and cooldowns are the framework's (`Activations.use`), from the data.
Sources: the item data (items.json, 0.26.1's numbers); the wiki (hypixelskyblock.minecraft.wiki, "MCW",
pages named after the item); 0.26.1's release notes ("Healing Revamp", MCW Changelog/2026/July 22);
the SkyHanni, Skytils, Skyblocker and Odin mods, which read Hypixel's menus, names and messages. The
recordings have none of these abilities used. Where the sources are silent the code says UNKNOWN.

## Shared pieces

- **Healing** (`Heals`): a heal on someone else is times the healer's Mending / 100
  (`PlayerHealth.healFrom`); in the Catacombs every heal is times 1 + the healer's Catacombs stat boost
  ("all healing within the Catacombs now scales with your Dungeon Stat Boost", 0.26.1; whose boost when
  healer and healed differ is UNKNOWN). Healing others counts for the run's "Ally Healing" / "Team
  Healing Done". Ghosts aren't healed. Heals over time pulse once a second, one of a kind at a time.
- **Timed buffs** (`Buffs`): `PlayerSession.buff` under the ability's name ("Effect doesn't stack":
  again starts it again), walk speed at once. "Nearby players": UNKNOWN radius, 20 blocks.
- **Shields on hits** (`Protection`, `PlayerDamage.addShield`): immunity (whole hits, and vanilla
  damage but the void's), then taunted mobs' less, then the Creeper Veil, then worn pieces' shields
  (Block Damage, Bone Shield, Mithril's Protection) and a Gyrokinetic Wand's Aligned, then the helmets
  that save from death. A shield that takes a whole hit leaves no number, flinch or knockback. Players who
  "can't attack" have their melee and arrows called off.
- **No vanilla use** (`UtilityListener.onItemUse`): a right click that one of these abilities answers
  doesn't also do what the item's material does (a Spirit Leap is an ender pearl, a Flare a firework
  rocket, an orb a head), cast or not. What abilities put in the world (a veil's creepers, an orb's
  stand) can't be hit, targeted, used or set off.

## Built

| Ability | Items (count) | Status | Notes and sources |
|---|---|---|---|
| Deploy | Radiant, Mana Flux, Overflux, Plasmaflux Power Orb; Warning, Alert, SOS Flare; Dwarven, Mithril, Titanium, Glacite Lantern, Will-o'-wisp; Umberella (13 of 14) | APPROX | One per owner ("Your previous §6Plasmaflux Power Orb §ewas removed!", SkyHanni). Buffs the nearest players in range up to its number (owner first: UNKNOWN); a player gets only the strongest, by rarity (MCW Dwarven Lantern), the later on a tie (UNKNOWN). Owner heals by "yourself", others by "others" × Mending, each second; Health Regen, stats and "+50% base mana regen" (`StatsRunnable.addManaRegenBonus`) while buffed. Costs: the orbs' 50% mana and 50 Vitality are their data's; the Lanterns' and Umberella's "Costs 50% of max mana" only their text's, taken by the handler. Stand named "§aRadiant §e57s", Plasmaflux "§d§lPlasmaflux" (Skytils, SkyHanni). Flares: heal only on the Crimson Isle server, gone 80 blocks from the owner (MCW Warning Flare); where a flare hangs and its name are UNKNOWN. Not built: Glacite Mineshaft range, Umberella's rain, Kuudra, Mana Disintegrator. The Totem of Corruption isn't put down (fishing). MCW Power Orbs, Deployables, Radiant/Plasmaflux Power Orb, SOS Flare |
| Instant Heal | Zombie Sword, Ornate, Florid (3) | DONE | "Heal for 320❤ and heal players within 7 blocks for 64❤" (0.26.1: charges gone, 25 Vitality). Sound/particles UNKNOWN. MCW Zombie Sword |
| Small / Medium / Big / Huge Heal | Wand of Healing, Mending, Restoration, Atonement (4) | DONE | "Heal 60❤ per second for 5s. Wand heals don't stack": first pulse at once, a recast renews the running heal and keeps its beat (MCW Wand of Healing: "repeatedly casting ... can heal 120 per second"). Revenant/Reaper armor's +50% is the set bonus part's. MCW Wand of Healing, Wand of Atonement |
| Spirit Leap | Spirit Leap, Infinileap (2) | APPROX | Menu "Spirit Leap" (the mods' title) of the other members' heads named as in the run, from slot 11 (Odin); dead and away ones there but not clickable; leap to them, 1 s immunity ended by a hit, one Spirit Leap used up (not the Infinileap), "You have teleported to Name!" (Odin's text; colours UNKNOWN). Cooldown between leaps, not on opening (UNKNOWN). Lore of the heads UNKNOWN. Not from the trap room ("Spirit Leaps cannot be used in trap rooms; however, players can teleport to other players in trap rooms", MCW; what it says there is UNKNOWN). Not built: the Ice puzzle's landing, the Aspiring Leap recipe outside a run. MCW Spirit Leap, Infinileap |
| Howl | Weird Tuba, Weirder Tuba (2) | DONE | You and the 4 nearest players, the text's stats and time; one buff for both tubas. Radius UNKNOWN (20). MCW Weird Tuba, Weirder Tuba |
| Second Wind (passive) | Spirit Mask, ⚚ Spirit Mask (2) | DONE | The killing hit does nothing, +50 Speed and immunity 3 s, every 30 s; fragged: 10% max health over 5 s. "&6Second Wind Activated&a! Your Spirit Mask saved your life!" (MCW). Mage's Efficient Spells on it: LATER (classes). MCW Spirit Mask |
| Clownin' Around (passive) | Bonzo's Mask, ⚚ Bonzo's Mask (2) | APPROX | In the Catacombs only: immunity and +20 Strength 3 s, full health; cooldown 360 s − 3.6 s a Catacombs level (MCW). How the Strength grows with the level: UNKNOWN (stays 20). "Your Bonzo's Mask saved your life!" (Odin, SkyHanni; colours UNKNOWN). MCW Bonzo's Mask |
| Spirit Glide (passive) | Spirit Boots, ⚚ Spirit Boots (2) | APPROX | Sneak in mid-air (UNKNOWN: so ground sneaking stays sneaking): 250 mana, 60 s; launched up, holding sneak flies forward, invincible ~5 s, no fall damage while gliding (MCW, 0.19.2's rework). Speeds and look UNKNOWN. Not in the trap room ("no longer works in the Dungeon Trap room", 2021, MCW). MCW Spirit Boots |
| Aspiring Leap | Aspiring Leap (1) | APPROX | Always "&cYou can only use this item on your private island!" (MCW's message): no private islands here. LATER: private islands, co-ops |
| Creeper Veil | Wither Cloak Sword (1) | APPROX | 0.26.1's rework: layers of 1,000 × (1 + Catacombs boost in a run), 30 Vitality each, no attacking and no Vitality regen (`Vitality.addRegenPause`) while up, right click takes it down, cooldown from when it's down (halved when taken down: the reading that makes halving mean something). Out of Vitality: all of it goes, slight knockback, veil down, rest of the hit through (UNKNOWN). 6 invisible charged creepers on them (MCW). The cast's own 30 Vitality: UNKNOWN whether Hypixel charges it (it's charged). Messages MCW, SkyHanni; the out-of-Vitality one UNKNOWN. Down too when they die or become a ghost (its cooldown starts). Not built: off when switching armor sets (2021, MCW). MCW Wither Cloak Sword |
| Shadowstep | Silent Death (1) | APPROX | Behind the SkyBlock mob looked at within 20 blocks (its far side, facing it: UNKNOWN), +25 Strength 10 s, cooldown back to 0 on any kill. No target: nothing, free (UNKNOWN). MCW Silent Death |
| Speed Boost | Rogue Sword (1) | DONE | +100 Speed 30 s. MCW Rogue Sword |
| Life Blood | Wand of Strength (1) | APPROX | Costs 10% of max health (the text; the data's 15.6 isn't that), charged with its other costs (EFFECTS.md), +30 Strength 10 s to them and everyone near (the wiki's solo Bingo use has it on the caster); radius UNKNOWN (20). MCW Wand of Strength |
| Enrage | Enrager (1) | DONE | Mobs within 10 go for them; those mobs' hits on them 10% less for 10 s. Costs 10% of max health ("item_ability_health_cost = 10% of HP", MCW; the data's 15.6 isn't health), charged with its other costs (EFFECTS.md). MCW/Fandom Enrager |
| Jingle Bells | Jingle Bells (1) | APPROX | Mobs within 10 go for them; takes 50% of max mana (the data has no cost; with less, it takes what there is: UNKNOWN). Fandom Jingle Bells |
| Soulward | Soul Esoward (1) | APPROX | Invulnerable and no attacking 5 s, then damage halved 2 s (`Combat.addMultiplier`). Its 10 Soulflow isn't charged: Soulflow doesn't exist (LATER). MCW Soul Esoward |
| Extreme Measures | Gloomlock Grimoire (1) | DONE | Heal for 1,000❤ for 45 Vitality. (Life Tap: LATER, Soulflow/Overflow.) MCW Gloomlock Grimoire |
| Ether Transmission | Etherwarp Conduit (1) | APPROX | To the targeted block within 57 with air in the two over it (Skyblocker's copy of Hypixel's rule); nothing, free, without one (Hypixel's message UNKNOWN). Its 1 Soulflow isn't charged (no Soulflow yet). Not built: Tuned Transmission, not in the Floor VII boss room (MCW), the ethermerged Aspect of the Void's sneak click (nothing ethermerges an item: there's no Etherwarp Merger). MCW Etherwarp Conduit |
| Revive | Revive Stone (1) | DONE elsewhere | The run's own (`dungeons/instance/ReviveStones`, `GhostEvents`); not registered here. |
| Instant Transmission | Aspect of the End, of the Void (2) | DONE before | `item/ability/abilities/InstantTransmission`. |
| Cells Alignment | Gyrokinetic Wand (1) | APPROX | "Apply Aligned to yourself for 6s, plus 4 nearby players on grouped islands. Aligned: Splits incoming damage and applies it over 3s." (the text's numbers). A hit while Aligned takes a share now and the rest waits, taken a second at a time over the next 3 s and starting again with each hit ("the delayed damage will refresh until either the player stops taking further damage or the buff expires", MCW); what waits goes through absorption, then health, and can kill. The share now is UNKNOWN (a third: the hit spread evenly). The others are the run's 4 nearest living teammates within 20 blocks (UNKNOWN how far; the Catacombs are the only grouped islands). Its 2 Soulflow isn't charged (no Soulflow). MCW Gyrokinetic Wand |
| Echolocation, and the compass | Secret Tracker 3000 (1) | APPROX | "Always points towards the closest secret while in dungeons": while they carry one alive in a run, their compass points at the nearest secret the team hasn't found, of the rooms someone has walked into, once a second. The click (9 mana, 5 s): a trail towards it and a column at it for 5 s that only they see (UNKNOWN how Hypixel shows it). No run or no secret: nothing, free. MCW Secret Tracker 3000 |
| Dungeon Breaker (DIG) | Dungeonbreaker (1) | APPROX | A left click on a block, alive in a run: its text's 1 charge breaks it, 20 at a time, back after 10 s, 2 charges back a second, 20 at most. Only in the room they're in, never a puzzle room's, a door's, an unblown tomb's, or a wall between rooms (a gap between cells counts only inside a room of several): "It cannot be used in puzzle rooms, on doors, or to pass through walls into a separate room", "any block that is not part of a crypt inside the explored area" (MCW). Nothing that holds something or is used by a click (chests, levers, skulls), nor what can't be broken, is broken (UNKNOWN); a block waits to come back while a player stands in it (UNKNOWN). The charges show on the action bar when used; the item's "Charges" line stays its data's (UNKNOWN where Hypixel shows them). MCW Dungeonbreaker |
| Telecommunications, Tuning 4 Dummies (CLICK) | Blazetekk™ Ham Radio (1) | APPROX | "If exactly two players within 110 blocks are on the same channel, both gain" its text's stats; "Earn half stats while alone!"; with more on the channel, none. A click goes to the next of the wiki's 9 channels, kept on the radio and shown on its "Your channel" line, the data's 103.5 FM first; the wiki's lines when a carrier's signal changes ("Your radio is weak...", "...signal is strong!", "...lost signal. There's too many enjoyers on this channel."). A radio anywhere in their inventory counts (UNKNOWN where it must be). "Nearby: 0 enjoyers" stays the data's. The Bluetooth rings' Damage is the accessories'. MCW Blazetekk Ham Radio |
| Try Your Luck | Archfiend Dice, High Class Archfiend Dice (2) | APPROX | Its text's cost from the purse; a 6 one roll in 24, else 1 to 5 alike (the wiki's odds); 1 to 6 give -120, -80, -40, +40, +80, +120 Health (a third of the text's most a step: the wiki's table) for 24 h, kept on the profile ("Overwritten by new rolls!"), in their stats; a 6 pays the text's 15M (100M) and uses the dice up. The 7's Archfiend Dye: LATER (no dyes). The chat lines are UNKNOWN. MCW Archfiend Dice |
| Farmer's Speed (LEFT CLICK) | Rancher's Boots (1) | APPROX | "Set a maximum amount of ✦ Speed.": a sign asks for the number, kept on the boots; while they're worn, the wearer's Speed is at most that. 0 or less, or the cap (400) and more, take it off. The sign's lines are UNKNOWN; Farmer's Grace's "Current Speed Cap" line stays the data's (the boots' lines are the armor bonuses', with their Farming level stats). |
| Hollow Spirit (LEFT/RIGHT CLICK) | Hollow Wand (1) | APPROX | A left click is a stack of ✤, a right one of ✦; two make a spell, paid in its text's ⚶ Spirit: Spirit Spark (✤✤) heals them and up to 5 players in 25 blocks for 8% of max health; Hollowed Rush (✦✦) +1.3x mana regeneration for 6 s; Raging Wind (✦✤) +30% Damage and +10 Ferocity for 20 s, up to 3 stacks; Ichor Pool (✤✦) a pool of 8 blocks for 20 s, where up to 5 players get x1.2 Damage and +40 Ferocity while they stand in it (the wiki's '/wandinfo'; both Damages are the Damage stat, as its stat templates have them). One buff whoever cast it ("do not stack when used by multiple players"). How long a first click waits (3 s) and what's shown are UNKNOWN. The ⚶ Spirit stacks are the Hollow Armor's tiered Spirit, the armor bonuses', given through `UtilityAbilities.hollowSpirit`: until then there are none, so no spell is cast. MCW Hollow Wand |

## Worn

Worn armor's and equipment's own ABILITY blocks that don't hit (`WornPassives`, `Movement`, `Masks`; the
ones that hit are ABILITIES_WEAPONS.md's). A passive one is a `Bonus` of the kind "ABILITY" that SetBonuses
counts while a piece with the block is worn, as it counts a piece's own bonus (`WornAbilities`), so it acts
while worn and its stats are in the Stats menu; a SNEAK one reaches its handler through the core's activation
dispatch (EFFECTS.md). The shields are on `Protection`'s way, after the Creeper Veil and before the helmets
that save from death. Tests: `WornPassivesTest`.

| Ability | Items (count) | Status | Notes and sources |
|---|---|---|---|
| Block Damage | Guardian Chestplate (1) | DONE | "If you are at full ❤ Health, the first damage you take will be nullified", every 60 s (its block's cooldown). |
| Bone Shield | Skeleton's Helmet (1) | APPROX | 3 bones (the wiki's Skeleton's Helmet) circle them; each takes a whole hit, one back every 30 s (the text's); not on a boss's hits ("won't work on Magma Boss and new bosses anymore", the wiki's history). They're full the first time it goes on (UNKNOWN); taking it off and on again keeps what was left (so it's no way to have them all back). |
| Mithril's Protection | Mithril Coat (1) | APPROX | "Any damage taken is max 40% of the wearer's ❤ Health" (of their max: UNKNOWN whether it's their health now); "Gain Regeneration when this ability activates": +100 Health Regen for 5 s (UNKNOWN). |
| Growth | Growth Armor (4) | APPROX | "Heals you for 1% ❤ Health after killing a Monster, and also increases the ❤ Health bonus of a piece of the armor by 1 (Max 100)", its 4 s cooldown the block's: each worn piece's count on it ("Bonus HP" line), once a kill for the heal (UNKNOWN whether it's once a piece). A Monster is any mob but an Animal or Critter one (UNKNOWN). |
| Double Jump | Spider's, Tarantula, Primordial Boots (3) | APPROX | "Allows you to double jump by sneaking mid air!", for the block's mana: once each time they leave the ground. How far it throws them is UNKNOWN. |
| Depth Coating, Pressurized Coating | Tank Miner, Heat Armor (8) | DONE | Each worn piece's own Defense (and Strength) x2 (x3) on the Dwarven Mines, the only Mining Island here. |
| Gladiator's Will | Bone Necklace (2) | APPROX | +3 Defense for each of SkyBlock's mobs within 10 blocks up to +30; 30 blocks and +60 for a Tank in a run (the text's numbers). "Increases the range of your Diversion passive by 15 blocks": LATER (no Diversion). |
| Blazing Restoration | Ghast Cloak (2) | DONE | "Restores +5❤ Health every second while worn." (+15 on the Vanquished one). |
| Evil Incarnate | Reaper Mask (1) | APPROX | "Zombie Armor triggers on all hits": with the three-piece Zombie Armor's bonus on, a hit that isn't a projectile starts the same heal as its Projectile Absorption (from that set's text: the armor bonuses' own does the projectiles; UNKNOWN whether two at once heal twice). Its souls lines: LATER (necromancy). |
| Water Burst (SNEAK) | Salmon Armor (10) | APPROX | "When wearing the full set, ... burst forward when sneaking in water": only with its 4 pieces, in water, for the block's 20 mana; how far is UNKNOWN. |
| To the Moon! (SNEAK) | Spring Boots (1) | APPROX | "Charge your jump by sneaking. The longer you sneak, the higher you will jump!": starting to sneak charges, letting go on the ground jumps. The charge's curve is UNKNOWN. |
| Bouncy (SNEAK) | Slug Boots (1) | APPROX | "When falling from a great height, you'll bounce straight up!": a fall of 5 blocks or more that ends while they sneak with the boots on doesn't hurt, and bounces them by how far they fell. Whether it needs the sneak, and what a great height is, are UNKNOWN. |
| Slippery Slope, Exanimate Blessing, Party Flock | Salmon, Zombie, Parrot Mask (3) | APPROX | A mask isn't armor, so they're read off the helmet slot: the text's stats in water; +5% of the other worn Undead pieces' ("This item is Undead ༕!") own stats; +1 Strength to players within 20 blocks for each upgrade of the wearer's best Intimidation accessory (the wearer among them, and the most of several wearers: UNKNOWN). The other masks' passives: LATER (pets, Gifts, foraging). |

## Dungeon ghosts

A ghost ("function as a spectator with unique abilities", MCW Ghosts) gets Haunt and its class's ghost
ability items when it becomes one, each in the first free slot of its inventory, the hotbar first (UNKNOWN
which slots Hypixel's take; with none free it goes without); they're never saved with its items and go when
it's alive again or leaves (`RunItems`, `Ghosts`). Its own items stay; its Spirit items (Spirit Sword,
Spirit Shortbows) become ghost abilities (Spirit Bomb, ABILITIES_WEAPONS.md). The run calls off everything a
ghost clicks, so a ghost's right clicks come to the ghost abilities themselves (`GhostAbilities.clicked`),
used as every ability is (`Activations.use`).

| Ability | Items (count) | Status | Notes and sources |
|---|---|---|---|
| Haunt | Haunt (every ghost's) | APPROX | "Teleport to an alive player!": Spirit Leap's menu, titled "Teleport to Player" (the title SkyHanni, Skytils and Odin read with Spirit Leap's), the living teammates clickable; no immunity, nothing used up, its 2 s cooldown on use. The words after it are UNKNOWN (Spirit Leap's). MCW Ghosts |
| Absorption Potion | the Tank's (1) | APPROX | Thrown; each living teammate within its text's 10 blocks of where it breaks gets 200 absorption for 3 s, +10 a Tank level ("+10 Absorption from Ghost Absorption Potion" a level, 700 at 50: the Fandom wiki's Tank; the wiki's Ghosts says "multiplied by your Tank level * 10"). Code: `weapons/GhostAbilities`. |
| Stun Potion | the Tank's (1) | APPROX | ABILITIES_WEAPONS.md. |

## Dungeon secret items

What secrets hand out (`SecretRewards`) that does something when used, "Dungeons only!" (`SecretItems`,
`TrainingWeights`); none has an ability block, so they're found by id. Each is used up, only alive in a
running run. Their vanilla use (a spawn egg, a pressure plate) never happens; a click on a block that's used by a
click (a chest, a lever) is the block's, unless they sneak, as with anything else in hand. Tests: `SecretItemsTest`,
`HeldStatsTest`.

| Item | Status | Notes and sources |
|---|---|---|
| Decoy | APPROX | "Runs around and draws attention from nearby mobs": a player-shaped body where they clicked (or at their feet) wanders about, and SkyBlock's mobs within 10 blocks go for it instead of any player (a target goal over theirs) while it lasts; nothing hurts it ("immune to player damage", Fandom). "There is a chance that the Decoy will not spawn ... 'It was a dud.'" How long it lasts, how far it draws from, the dud's chance and its look are UNKNOWN (20 s, 10 blocks, 10%). Bosses, minibosses and the Blood Room's undead aren't drawn. Fandom Decoy |
| Inflatable Jerry | APPROX | "The mobs will run in the opposite direction, while text displays above their head that reads: 'JERRY, RUN'. There is a 20% chance that it will pop when used and not activate. ... Bosses and Mini-Bosses are not affected by Jerry. Furthermore, it will always fail to activate around Bosses" (MCW): mobs within 10 blocks run from where it was used for 5 s (both UNKNOWN). What it says when it pops is UNKNOWN. MCW Inflatable Jerry |
| Architect's First Draft | DONE | Resets a failed puzzle in the room they're in: a failed Tic Tac Toe or Three Weirdos starts again (the others never fail), its red cross off the map; used up. The wiki's lines: "You can only use this item in dungeons!", "There are no failed puzzles to reset... for now ;)", "You used the Architect's First Draft to reset <puzzle>!". MCW Architect's First Draft |
| Dungeon Trap | APPROX | "Place an explosive trap that triggers when mobs walk over it dealing 20,000-250,000 damage based on the dungeon floor you are on.": a right click on a block's top puts its pressure plate there; the first of SkyBlock's mobs on it sets it off, every mob within 3 blocks taking the floor's damage (the placer's, times Consolidated). How the damage goes between floors is UNKNOWN (evenly, the Entrance's 20,000 to Floor VII's 250,000); the blast's reach too. Its text |
| Training Weights | APPROX | Each minute in an inventory counts on it, with its "Time Held" and "Strength Gain" lines; the Strength grows as the wiki's table does (each +1 after the first takes 20 minutes more for each five before it, +50 at 5,500 minutes). Dropped, or clicked or dragged into another inventory (not a shift click: "moved to any storage without shift clicking, it will shatter"), it's gone and its Strength is theirs for 2 minutes, a buff that doesn't go to another server. The chat line is UNKNOWN. The Quiz maxing them out: LATER (no Quiz). MCW Training Weights |

## Later

| Ability | Items (count) | Waits for |
|---|---|---|
| The other classes' ghost abilities (Healing Potion, Revive, Buff Potion, Ghost Axe, Stun Bow, Healing Bow, Instant Wall, Fireball) | the Archer's, Healer's, Mage's and Warrior's ability items (10) | their items have no ability text in the item data (Class Details' recorded lines have some; which item is which ability, and the damage ones' numbers, are UNKNOWN): see "Questions for the owner" |
| Raise Souls | Necromancer Sword, Reaper Scythe, Summoning Ring (3) | Summons: souls, summoned mobs that fight |
| Cube Fanatic | Magma Necklace (2) | a mob that drops Magma Cream (the Hub's Magma Cube drops a Dark Claymore), and "more" is an amount, which the drop hooks don't change |
| Weird Transmission, Succulence, Splash Yo Face, Punch in!, Future Footsteps, Time Warp!, Spawn, Detect!, Meownar, Quantum Projection, Split or Steal, Stats | Aspect of the Leech (3), Healing Melons (3), Holy Ice, Punchcard Artifact, Wizard items, LM Eggs (4), Rift tools | The Rift (its own stats, hearts and mana) |
| Deploy (Totem of Corruption), Black Hole, Yoink, Throw, Reel In, Retentio, Capture, Certain Capture, Hotspotter, Snake Taming, Inhale | Totem, Pocket Black Holes (3), Fishing Nets (4), Lassos (4), Traps (5), Critter Capsules (2), Hotspot Radar, Frozen Water Pungi, Bubbles of Air | Fishing, hunting/foraging, Galatea |
| Hydrate, Fill, Vacuum, Pest Tracker, Stereo Harmony, Spray, Configure, Analyze, Set, Snap, Seed Storage, Farmer's Delight, Nether Wart Storage, Alchemist's Bliss, Pipeline, Farmer's Grace | Watering Cans (5), Vacuums (6), Sprayonators (3), garden tools, Rancher's and farming boots | The Garden, pests, farming |
| Weather, Time Skip, Place Dirt, Snow Placer, Grand... Zapper?, Area Planner, Organized Builder, Grand Architect, Built-in Storage, Climb! | Weather Stick, Enchanted Clock, builder's tools (4), scaffolding (3) | Private islands, minions, building |
| Echo, Egglocator, Rezonate, True Dwarth, Track, Seek the King, Ancient Recall, Open Shop, Wasssaaaaa?, Whassup?, Big Pull | Griffin spades (3), Egglocator, Fallen Star Tracker, Royal Compass, Talbot's Theodolite, Royal Pigeon, Recall Potion, Kuudra shop item, Maddox phones (2), Moody Grappleshot | Diana, Hoppity, Dwarven Mines stars/emissaries/commissions, Crystal Hollows, Kuudra, slayers, blaze slayer |
| Mythos' Might, Indulgence, Overindulgence, Reality Check, Hi-Vis Wormhole Helmet, Beat Saver, the "All The Way" helmets, Friendship is Power, Blazing Palisade, Forced Metamorphosis, Gravity Falls, Empyrean Intuition, Chimeric Core, Buzzy Little Bee, Feelin' Festive, Soiling!, Stick Around | worn pieces and masks | Diana's Ritual, fishing and wormholes, the Rift, fireball mobs, slayer bosses, pets, Gifts, foraging |
| The Training Weights' Quiz, Gladiator's Will's Diversion | 1, 2 | the Quiz puzzle; the Tank's Diversion passive |
| Copycat, Spook, Spooker, Sparkle, Lazer, Swhooomp, Phwomp, Bounce Bonanza (EX), Puzzle, Splash, Potato King | Ditto items (2), Great Spook Staff, Sunflower Head, Emmett's pointer, blowgun, dart tube, beach balls (2), Puzzle Cube, fish hats, Potato Crown | Cosmetics, events (Carnival, Spooky Festival, fishing festival), what they show (UNKNOWN) |
| Life Tap | Gloomlock Grimoire | Soulflow and Overflow mana |
| (a Healer's own heals with Mending) | every heal above | The run's classes telling a Healer apart ("Mending now affects Healer's self healing", 0.26.1) |
| (Efficient Spells on item cooldowns) | every cooldown above | The Mage's class passive on item abilities ("All abilities have a ...% shorter cooldown"); the hook is `Abilities.addCooldownFactor` (EFFECTS.md) |
| (Set bonuses on these abilities) | wand heals, Spirit Leap, Shadowstep, Ether Transmission, Spirit Glide | The armor bonuses part's `SetBonuses`, wired at the merge: Trolling The Reaper's +50% on wand heals (`WandHeal`), `SetBonuses.teleported` after a teleport, and its mana cost factors (Wise Dragon) on Spirit Glide's and the Lanterns' own mana costs |

## Questions for the owner

- The other classes' ghost abilities: their items (`ARCHER_DUNGEON_ABILITY_1`-`3`, `HEALER_...`,
  `MAGE_...`, `WARRIOR_DUNGEON_ABILITY_1`) are named "Drop Arrows", "Stun Bow", "Healing Bow", "Healing
  Aura", "Healing Potion", "Revive Self", "Pop-up Wall", "Guided Sheep", "Fireball" and "Strength Potion",
  with no ability text; Class Details' recorded lines describe Healing Potion, Revive, Instant Wall,
  Fireball, Buff Potion, Ghost Axe, Stun Bow and Healing Bow. Should they be built from those lines (and
  which item is which), or wait for their items' text?
- Where ghost ability items go: here, the first free slots, hotbar first. Swapping the ghost's hotbar for
  them would need its real hotbar kept safe while it's a ghost (saved in its place).

## Not this part's

What hits (weapons' click abilities and passives, bows, worn pieces that hit, armor's SNEAK abilities that
hit, the ghosts' Stun Potion and Spirit Bomb) is in ABILITIES_WEAPONS.md, this part's too. Ragnarock
(Ragnarock Axe) is there, as a weapon's. Set, piece and tiered bonuses are the armor bonuses part's
(BONUSES.md): the Aurora Armor's Arcane Energy and the Hollow Armor's Spirit, which Homing Missiles and the
Hollow Wand spend, among them.
