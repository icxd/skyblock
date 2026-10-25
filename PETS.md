# Pets

The Pets menu, pet items, summoning a pet that follows you, its stats, pet XP and levels with Taming, the Pet Score and
its SkyBlock XP and Magic Find, and /yoinkprofile's pets, as recorded on Hypixel (2026-10-25: the Pets menu of a
profile with nine pets, a Bee bought from Bea, levelled on wheat and upgraded by Kat, its menu actions, the Lion
summoned and followed round the Hub). The code is in `paper/src/main/java/net/icxd/dungeons/pet`.

## The data

`pets/pets.json` in the data checkout, made by `tools/pets/build_pets.py` (see its README) from NEU-REPO's dumps of every
pet at every rarity, the wiki's per-level formulas and the recording. servermgr links `pets/` into every server; without
it there are no pets (the log says so).

- 89 pets at 325 rarities: each one's head, type line, stats and ability text, and the numbers in it as a base and so
  much a level, with their decimals. The nine recorded pets' lore comes out as recorded, line for line (PetTextTest).
- The pet XP table (NEU's), each rarity's offset into it, the dragons' levels to 200 (1,886,700 each past 100), the
  Bingo pet's offsets and the Reindeer's 2x XP.
- The Pet Score's points (the wiki's: Common 1 to Mythic 6, one more at max level) and its Magic Find thresholds (the
  recorded Pet Score Rewards item: 10 Score +1 Magic Find to 500 Score +13).

## Done

- **Pet items**: Hypixel's PET item, whichever pet its `petInfo` says (kept as `pet_info`): "&7[Lvl 1] &fBee", its head,
  its stats, abilities, held item, candy and progress, "&eRight-click to add this pet to your pet menu!", Kat's line
  when there's a rarity above, "&f&lCOMMON PET". A right click adds it to the menu ("&aSuccessfully added &fBee &ato
  your pet menu!", an arrow's hit, and a level-up when the Pet Score goes up), as recorded. It's never placed.
- **The Pets menu** (`/pets`, `/petsmenu`, `/petmenu`, `/viewpetsmenu`, or the SkyBlock Menu's Pets, which shows the
  summoned pet's progress), as recorded: the Pets item, Exp Sharing, +28 Pet Slots (5,000 Bits, twice: 112, 140,
  168), the pets 28 a page ("(1/2) Pets" with more than one, the wiki's), Pet Score Rewards (the threshold reached
  marked "&5«"), Go Back, Close, Search, Pet visibility and Sort.
  - A click summons a pet ("&aYou summoned your &6Lion&a!", an egg's pop, the menu closes) or despawns the one out
    (its line is "&cClick to despawn!"). Shift-click: a favourite ("&e⭐ " before its name, first in the menu). Right
    click: an item again, into the inventory (the Pet Score goes down, its SkyBlock XP doesn't).
  - Sort: Rarity (the best first, then the most XP, as recorded), A to Z, Z to A, Pet Exp, Skill; a left click the
    next, a right click the one before, with the recorded lever click. Visibility: Show all pets, Hide all pets,
    Only your pet (a left click the next, as recorded), and a right click toggles name tags, with the recorded pling.
    Both are kept with the profile.
- **Summoned pets** follow their owner, as recorded: an invisible marker armor stand holding the pet's head in its
  right hand, and the name over it, "&8[&7Lv91&8] &6Mithril Golem" (others see "Bea's Bee", as Bea's was). It goes
  after you faster the farther it is, stops short of you, faces you, and bobs 0.55 up and down standing near you. It
  comes along to another world, catches up from far, and is out again when you come back.
- **Stats**: the summoned pet's at its level, its held item's ("Grants &f+10❂ True Defense", in the pet's True Defense
  line, as recorded), and the Pet Score's Magic Find, added to yours.
- **Pet XP**: skill XP before Wisdom x the pet's share x (1 + Taming level / 100) x (1 + its held item's boost) x its
  own (the Reindeer's 2). Its share (the wiki's): all of its own skill's (every skill's for All Skills), a third of
  another's, a twelfth of Alchemy's or Enchanting's when they aren't its own, 1.5x Fishing's; none from Carpentry,
  Taming, Runecrafting and Social, or for a pet that's fed (the Wisp). The recording agrees: its Bee levelled on 6.4
  a wheat, the wheat's 4 Farming XP x 1.6, where the action bar showed 4.8 with Wisdom. Each level: "&aYour &aBee
  &aleveled up to level &92&a!" and the recorded four-note chime; the name tag shows the new level. XP past the most
  level is kept, as Hypixel keeps it.
- **Taming XP**: 30% of the pet's share, not shown as it comes in; its levels are said as every skill's.
- **Pet Score**: each pet type once at its best (rarity, then level), Special and Very Special none. Its SkyBlock XP
  (Pet Score, 3 a point, 1,527 at most) counts the highest it has been: a pet taken out and put back gives none
  again, as recorded.
- **/yoinkprofile**: a Pets part (the API's `pets_data.pets` and the one out), and pet items wherever they are.
- `/givepet BEE [RARITY] [LEVEL] [XP]`: a pet as an item, for staff (not a Sandbox tool).

## Approximated (UNKNOWN)

- **Number decimals**: as many as NEU's level 1 value shows when that's rounded, else all of them (two numbers have
  been checked at a level between: the Guardian's 6.6 and the Bee's 1.16). 43 numbers NEU and the wiki disagree on are
  listed in the data's report; their level 1 and 100 values are right, the levels between may be off.
- **Taming XP's 30%**: a 2020 forum measurement ("Calculating the best skill for Taming EXP", hypixel.net), before
  Wisdom existed.
- **Despawning**: "&aYou despawned your &6Lion&a!" and the egg's pop (never recorded).
- **The following**: its speed, how near it stops and the bob are fitted to the recorded Bee and Lion; when it gives
  up and comes to you (24 blocks here) isn't recorded.
- Name tags off says "&cDisabled"; a full menu "&cYour pet menu is full!"; no room for a pet item "&cYour inventory is
  full!"; +28 Pet Slots you can afford "&eClick to purchase!". Pet Score Rewards with no threshold reached marks none.
- The dragons' egg (below level 100) shows no stats or abilities, under the dragon's name.

## Not done

- **Ability effects**: the text is shown, none of it does anything yet.
- **Pet items' other effects**: a share of the pet's stats (Gold Claws, Minos Relic), the Tier Boost's rarity, Exp Share,
  and putting one on a pet (right-clicking the summoned pet); only "Grants +N Stat" and "pet exp for" work.
- **Exp Sharing** (a click does nothing), **Search** (nothing), Autopet, pet skins (kept, not shown), Pet Candy (shown,
  not used), Kat, Bea, George, Fann.
- Chimera and the other things that copy pet stats.
