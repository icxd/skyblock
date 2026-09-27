# Utility abilities

The item abilities that don't hit: movement, healing and shields, buffs, deployables and tools. The
code is in `paper/src/main/java/net/icxd/dungeons/item/ability/utility` (`UtilityAbilities` registers
each handler by the ability name items' ABILITY blocks carry; `UtilityListener` does what they need
besides a click). Abilities that hit are the weapon-abilities part's (`ABILITIES_WEAPONS.md`).

What an ability does comes from its item's text wherever the text says it (`AbilityText` reads the
numbers: "+30❁ Strength", "for 20 seconds", "within 18 blocks", "up to 5 players", "Heal for 1,000❤"),
so an item whose numbers differ (the Weirder Tuba's 30 seconds, a Plasmaflux's 20 blocks) does what
it says. Mana, mana %, Vitality and cooldowns are the framework's (`PlayerListener`), from the data.
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
  damage but the void's), then taunted mobs' less, then the Creeper Veil, then the helmets that save
  from death. A shield that takes a whole hit leaves no number, flinch or knockback. Players who
  "can't attack" have their melee and arrows called off.

## Built

| Ability | Items (count) | Status | Notes and sources |
|---|---|---|---|
| Deploy | Radiant, Mana Flux, Overflux, Plasmaflux Power Orb; Warning, Alert, SOS Flare; Dwarven, Mithril, Titanium, Glacite Lantern, Will-o'-wisp; Umberella (13 of 14) | APPROX | One per owner ("Your previous §6Plasmaflux Power Orb §ewas removed!", SkyHanni). Buffs the nearest players in range up to its number (owner first: UNKNOWN); a player gets only the strongest, by rarity (MCW Dwarven Lantern), the later on a tie (UNKNOWN). Owner heals by "yourself", others by "others" × Mending, each second; Health Regen, stats and "+50% base mana regen" (`StatsRunnable.addManaRegenBonus`) while buffed. Stand named "§aRadiant §e57s", Plasmaflux "§d§lPlasmaflux" (Skytils, SkyHanni). Flares: heal only on the Crimson Isle server, gone 80 blocks from the owner (MCW Warning Flare); where a flare hangs and its name are UNKNOWN. Not built: Glacite Mineshaft range, Umberella's rain, Kuudra, Mana Disintegrator. The Totem of Corruption isn't put down (fishing). MCW Power Orbs, Deployables, Radiant/Plasmaflux Power Orb, SOS Flare |
| Instant Heal | Zombie Sword, Ornate, Florid (3) | DONE | "Heal for 320❤ and heal players within 7 blocks for 64❤" (0.26.1: charges gone, 25 Vitality). Sound/particles UNKNOWN. MCW Zombie Sword |
| Small / Medium / Big / Huge Heal | Wand of Healing, Mending, Restoration, Atonement (4) | DONE | "Heal 60❤ per second for 5s. Wand heals don't stack": first pulse at once, a recast renews the running heal and keeps its beat (MCW Wand of Healing: "repeatedly casting ... can heal 120 per second"). Revenant/Reaper armor's +50% is the set bonus part's. MCW Wand of Healing, Wand of Atonement |
| Spirit Leap | Spirit Leap, Infinileap (2) | APPROX | Menu "Spirit Leap" (the mods' title) of the other members' heads named as in the run, from slot 11 (Odin); dead and away ones there but not clickable; leap to them, 1 s immunity ended by a hit, one Spirit Leap used up (not the Infinileap), "You have teleported to Name!" (Odin's text; colours UNKNOWN). Cooldown between leaps, not on opening (UNKNOWN). Lore of the heads UNKNOWN. Not built: no leaping in trap rooms, the Ice puzzle's landing, the Aspiring Leap recipe outside a run. MCW Spirit Leap, Infinileap |
| Howl | Weird Tuba, Weirder Tuba (2) | DONE | You and the 4 nearest players, the text's stats and time; one buff for both tubas. Radius UNKNOWN (20). MCW Weird Tuba, Weirder Tuba |
| Second Wind (passive) | Spirit Mask, ⚚ Spirit Mask (2) | DONE | The killing hit does nothing, +50 Speed and immunity 3 s, every 30 s; fragged: 10% max health over 5 s. "&6Second Wind Activated&a! Your Spirit Mask saved your life!" (MCW). Mage's Efficient Spells on it: LATER (classes). MCW Spirit Mask |
| Clownin' Around (passive) | Bonzo's Mask, ⚚ Bonzo's Mask (2) | APPROX | In the Catacombs only: immunity and +20 Strength 3 s, full health; cooldown 360 s − 3.6 s a Catacombs level (MCW). How the Strength grows with the level: UNKNOWN (stays 20). "Your Bonzo's Mask saved your life!" (Odin, SkyHanni; colours UNKNOWN). MCW Bonzo's Mask |
| Spirit Glide (passive) | Spirit Boots, ⚚ Spirit Boots (2) | APPROX | Sneak in mid-air (UNKNOWN: so ground sneaking stays sneaking): 250 mana, 60 s; launched up, holding sneak flies forward, invincible ~5 s, no fall damage (MCW, 0.19.2's rework). Speeds and look UNKNOWN. MCW Spirit Boots |
| Aspiring Leap | Aspiring Leap (1) | APPROX | Always "&cYou can only use this item on your private island!" (MCW's message): no private islands here. LATER: private islands, co-ops |
| Creeper Veil | Wither Cloak Sword (1) | APPROX | 0.26.1's rework: layers of 1,000 × (1 + Catacombs boost in a run), 30 Vitality each, no attacking and no Vitality regen (`Vitality.addRegenPause`) while up, right click takes it down, cooldown from when it's down (halved when taken down: the reading that makes halving mean something). Out of Vitality: all of it goes, slight knockback, veil down, rest of the hit through (UNKNOWN). 6 invisible charged creepers on them (MCW). The cast's own 30 Vitality: UNKNOWN whether Hypixel charges it (it's charged). Messages MCW, SkyHanni; the out-of-Vitality one UNKNOWN. MCW Wither Cloak Sword |
| Shadowstep | Silent Death (1) | APPROX | Behind the SkyBlock mob looked at within 20 blocks (its far side, facing it: UNKNOWN), +25 Strength 10 s, cooldown back to 0 on any kill. No target: nothing, free (UNKNOWN). MCW Silent Death |
| Speed Boost | Rogue Sword (1) | DONE | +100 Speed 30 s. MCW Rogue Sword |
| Life Blood | Wand of Strength (1) | APPROX | Takes 10% of max health (the text; the data's health cost is ignored), +30 Strength 10 s to them and everyone near (the wiki's solo Bingo use has it on the caster); radius UNKNOWN (20). MCW Wand of Strength |
| Enrage | Enrager (1) | DONE | Mobs within 10 go for them; those mobs' hits on them 10% less for 10 s. The data's health cost isn't charged (the framework doesn't charge health costs). MCW/Fandom Enrager |
| Jingle Bells | Jingle Bells (1) | APPROX | Mobs within 10 go for them; takes 50% of max mana (the data has no cost; with less, it takes what there is: UNKNOWN). Fandom Jingle Bells |
| Soulward | Soul Esoward (1) | APPROX | Invulnerable and no attacking 5 s, then damage halved 2 s (`Combat.addMultiplier`). Its 10 Soulflow isn't charged: Soulflow doesn't exist (LATER). MCW Soul Esoward |
| Extreme Measures | Gloomlock Grimoire (1) | DONE | Heal for 1,000❤ for 45 Vitality. (Life Tap: LATER, Soulflow/Overflow.) MCW Gloomlock Grimoire |
| Ether Transmission | Etherwarp Conduit (1) | APPROX | To the targeted block within 57 with air in the two over it (Skyblocker's copy of Hypixel's rule); nothing, free, without one (Hypixel's message UNKNOWN). Not built: Tuned Transmission, the ethermerged Aspect of the Void's sneak click (needs an item behaviour block). MCW Etherwarp Conduit |
| Revive | Revive Stone (1) | DONE elsewhere | The run's own (`dungeons/instance/ReviveStones`, `GhostEvents`); not registered here. |
| Instant Transmission | Aspect of the End, of the Void (2) | DONE before | `item/ability/abilities/InstantTransmission`. |

## Later

| Ability | Items (count) | Waits for |
|---|---|---|
| Stun Potion, Absorption Potion, Haunt | Tank's and ghost ability items (3) | Ghost abilities: ghosts get no ability items and can't use items; players have no absorption |
| Echolocation | Secret Tracker (1) | A way to ask the run for its nearest unfound secret, and how Hypixel shows it (UNKNOWN) |
| Dungeon Breaker | Dungeonbreaker (1, DIG) | Room geometry for its rules (not in puzzle rooms, on doors or through room walls, not crypts) and block restoring |
| Raise Souls | Necromancer Sword, Reaper Scythe, Summoning Ring (3) | Summons: souls, summoned mobs that fight |
| To the Moon!, Bouncy | Spring Boots, Slug Boots (2, SNEAK) | Numbers (charge curve, bounce height) are UNKNOWN |
| Weird Transmission, Succulence, Splash Yo Face, Punch in!, Future Footsteps, Time Warp!, Spawn, Detect!, Meownar, Quantum Projection, Split or Steal, Stats | Aspect of the Leech (3), Healing Melons (3), Holy Ice, Punchcard Artifact, Wizard items, LM Eggs (4), Rift tools | The Rift (its own stats, hearts and mana) |
| Deploy (Totem of Corruption), Black Hole, Yoink, Throw, Reel In, Retentio, Capture, Certain Capture, Hotspotter, Snake Taming, Inhale | Totem, Pocket Black Holes (3), Fishing Nets (4), Lassos (4), Traps (5), Critter Capsules (2), Hotspot Radar, Frozen Water Pungi, Bubbles of Air | Fishing, hunting/foraging, Galatea |
| Hydrate, Fill, Vacuum, Pest Tracker, Stereo Harmony, Spray, Configure, Analyze, Set, Snap, Seed Storage, Farmer's Delight, Nether Wart Storage, Alchemist's Bliss, Pipeline, Farmer's Speed | Watering Cans (5), Vacuums (6), Sprayonators (3), garden tools, Rancher's Boots | The Garden, pests, farming |
| Weather, Time Skip, Place Dirt, Snow Placer, Grand... Zapper?, Area Planner, Organized Builder, Grand Architect, Built-in Storage, Climb! | Weather Stick, Enchanted Clock, builder's tools (4), scaffolding (3) | Private islands, minions, building |
| Echo, Egglocator, Rezonate, True Dwarth, Track, Seek the King, Ancient Recall, Open Shop, Wasssaaaaa?, Whassup?, Tuning 4 Dummies, Big Pull | Griffin spades (3), Egglocator, Fallen Star Tracker, Royal Compass, Talbot's Theodolite, Royal Pigeon, Recall Potion, Kuudra shop item, Maddox phones (2), Ham Radio, Moody Grappleshot | Diana, Hoppity, Dwarven Mines stars/emissaries/commissions, Crystal Hollows, Kuudra, slayers, radio, blaze slayer |
| Try Your Luck | Archfiend Dice (2) | A buff that lasts 24 hours on the profile (and the gamble itself) |
| Copycat, Spook, Spooker, Sparkle, Lazer, Swhooomp, Phwomp, Bounce Bonanza (EX), Puzzle | Ditto items (2), Great Spook Staff, Sunflower Head, Emmett's pointer, blowgun, dart tube, beach balls (2), Puzzle Cube | Cosmetics, events (Carnival, Spooky Festival, fishing festival), what they show (UNKNOWN) |
| Life Tap | Gloomlock Grimoire | Soulflow and Overflow mana |
| (a Healer's own heals with Mending) | every heal above | The run's classes telling a Healer apart ("Mending now affects Healer's self healing", 0.26.1) |
| (Efficient Spells on item cooldowns) | every cooldown above | The Mage's class passive on item abilities ("All abilities have a ...% shorter cooldown") |

## Not this part's

These hit, or boost only what the holder's own hits do, so they're the weapon-abilities part's:
Homing Missiles, Ground Pound, Water Burst, Eye Beam, Splash (Crimson/fishing armor and helmets'
sneak abilities, which also need a sneak dispatch for worn pieces), Throwing Axe, Soulcry, Attunement,
Absorptio, Arrow Infusion, Leap, Ender Warp, Sinrecall Transmission, Gorilla Tactics (its burn hits),
Parley, Extreme Focus, Bad Health, Burning Souls, Nasty Bite, Detonate, Shoot, and every staff, wand
and sword ability that deals damage; the Necron's Blade scrolls (Implosion, Shadow Warp, Wither
Shield). `PlayerDamage.addShield` is where an absorption shield (Wither Shield's) can go.
