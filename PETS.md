# Pets

The Pets menu, pet items, summoning a pet that follows you, its stats and abilities, pet XP and levels with Taming, the
Pet Score and its SkyBlock XP and Magic Find, and /yoinkprofile's pets, as recorded on Hypixel (2026-10-25: the Pets
menu of a profile with nine pets, a Bee bought from Bea, levelled on wheat and upgraded by Kat, its menu actions, the
Lion summoned and followed round the Hub). The code is in `paper/src/main/java/net/icxd/dungeons/pet`, the abilities in
its `ability` package.

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
- **Summoned pets** follow their owner: a small invisible marker armor stand wearing the pet's head, and the name just
  over it, "&8[&7Lv91&8] &6Mithril Golem" (others see "Bea's Bee", as Bea's was). Hypixel's stand is full-size and
  holds the head in its right hand (recorded); a small one wearing it looks nicer. The name is on a marker stand of
  its own, where the small stand's own name would be if it had a hitbox (a marker's shows under its head). It goes
  after you faster the farther it is, stops short of you, faces you, and bobs 0.55 up and down standing near you. It
  comes along to another world, catches up from far, and is out again when you come back.
- **Stats**: the summoned pet's at its level, its held item's ("Grants &f+10❂ True Defense", in the pet's True Defense
  line, as recorded), and the Pet Score's Magic Find, added to yours.
- **Giving a pet an item** ("To use them, equip a pet and right-click the pet while holding a pet item", the wiki):
  looking at its head within your reach, as its marker stand can't be clicked. With none held it takes it ("Your pet is
  now holding Burnt Texts.", SkyHanni's test lines); with another, Swap Pet Item opens (the wiki's 0.24.1 screenshot:
  the two items, Confirm Swap's "Replace ... with ... You will be refunded your ... Cost 250,000 Coins Click to swap!"),
  for 25,000 to 2,500,000 coins by the replaced item's rarity, and the old one comes back (to the stash if there's no
  room). A Super Scrubber takes it off the same way ("You removed Quick Claw from your pet!"; "&cYour Pet isn't holding
  any item!" with none). One for another pet, an egg, the Snail's Unalloyed Speed or the Kuudra's Exp Share: "&cThis
  item is not compatible with your pet!". Pet Items are never placed.
- **What held items do**, read from their own lines: every stat they grant ("Grants +30❁ Strength and +20✦ Speed.", not
  those "while" or "when" make sometimes, less what "reduces your ☂ Fishing Speed by 50"), shares of the pet's own
  stats (Minos and Hephaestus Relics' "all pet stats", Gold and Iron Claws, Textbook, the Remedies), Quick Claw's stats
  every 2 levels, the Shelmets' knockback immunity, Unalloyed Speed's speed cap, the Reaper Gem's Ferocity on a kill,
  Grandma's Knitting Needle's Magic Find at a 50 Kill Combo, and the Tier Boost: the pet is one rarity higher (its
  stats, abilities, level table, colour and head) "until the Pet Item held by the Pet is changed", up to Legendary, and
  not the Griffin or the Wisps (the wiki; SkyCrypt's `ignoresTierBoost`), as NEU works its level out. Before, a stat
  after "and" and the conditional ones were read wrong (the Spooky Cupcake had no Speed, the Titanium Minecart its
  Mining Fortune everywhere).
- **What copies a pet's stats**, by their texts: Chimera on what you hold (its book's 20% a level, to 100% at V), the
  Manticore Claw worn (10% of the pet's Combat stats), and the Daedalus Blades held (+4❁ Damage a Taming level, +5 for
  the ⚚ one, and the pet's Combat stats). They copy the pet's stats with its item's, not its abilities' ("It does not
  copy stats granted by pet abilities, but it does copy stats added to the pet's base stats by Pet Items", the wiki's
  Chimera). The Bee Mask worn doubles a Bee's own stats (before its item's share of them, UNKNOWN which comes first).
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
- **Bea** (the Hub's Pet Care): her shop, as recorded: a Bee Pet for 50 coins (shown as its level 100, "&cThis is a
  preview of Lvl 100." "&cNew pets are lowest level!"; bought a Common one, level 1) and the four Honey Dippers Kat takes,
  500 to 500,000 coins, opening as she speaks (a villager's, high). Every shop's line now says what it cost, as recorded:
  "&aYou bought &fBee Pet &afor &650 Coins&a!", "&aYou bought &fExperience Bottle&8 x8 &afor &6240 Coins&a!".
- `/givepet BEE [RARITY] [LEVEL] [XP]`: a pet as an item, for staff (not a Sandbox tool).

## Abilities

An ability is its pet's, its block's name and its text with `#` for each number (`pet/ability`). Its numbers are the
text's at the pet's level: the data's `{n}`s and the ones written in. A test checks every ability's text against the
private data at every rarity; an ability counts only where the text is its own (High Stride's Jump Boost II, III and IV
are three).

- **Which count**: the summoned pet's; a dragon's egg (below level 100) has none. The Grandma Wolf's, Spirit's, Owl's
  and Kuudra's ("This pet's perks are active even when the pet is not summoned!") count from the Pets menu as well: each
  type's best, as the Pet Score picks it, looked at once a second. The Bingo Pet's don't: they "do not work on
  non-Bingo profiles" (the wiki), and there are none here.
- **Stats**: what one gives the pet itself ("to your pet": Shining Scales, Tyrant) is the pet's, where Chimera will copy
  it from. Then the flat ones on yours, and once your flat buffs are in, those worked out from your other stats (Armored
  Tank, Bulk, Stomp, Slow and Steady: "Every Speed buff ... counts", the wiki's Snail), the auras of other players' pets
  near you (Bird Discourse, Genius Amniote: each once, the highest level's) and Web Battlefield's battlefields; percent
  buffs come after all that.
- **Every ability works** for: Ankylosaurus (Unyielding on Last Stand; there's no Lifeline), Blaze, Blue Whale, Crow,
  Ender Dragon, Giraffe, Golem, Grandma Wolf, Horse, Hound, Jerry (its three do nothing, as they say), Lion, Mithril
  Golem, Phoenix, Rift Ferret, Sheep, Silverfish, Skeleton, Skeleton Horse, Snowman, T-Rex, Turtle, Wither Skeleton,
  Wolf and Zombie.
- **Some work** for 46 more: every one their pet has but those below. The Black Cat's Omen and Supernatural and the
  Bee's Busy Buzz Buzz are the pets' own stat lines (the same numbers), and the Reindeer's Winter Sprint is in its pet
  XP.
- **Waiting on what isn't built** (146 of the data's 303 ability names; 153 work):
  - Fishing: Ammonite's Expert Cave Fisher, Dolphin's Splash Surprise, Flying Fish's Rapid Decay, Frog's Home Sweet Home
    and Hunting Enjoyer, Hermit Crab's other three, Penguin's Thick Blubber, Seal, Slug's Slow and Steady, Spinosaurus.
  - The Garden, pests, crop drops and minions: Bee's Honey Harvester and Powered by Pollen, Elephant's Abundant
    Harvest, Hedgehog, Mooshroom Cow's Mushroom Eater and Bovine Blessing, Mosquito, Orchid Mantis' other two, Rabbit's
    other three, Rose Dragon's other three, Slug's Repugnant Aroma; Magma Cube's, Pigman's, Spider's, Ocelot's and
    Snail's minion ones.
  - Foraging and its places: Ocelot's Tree Essence, Sloth, Monkey's two in The Park, Jade Dragon's Apex Predator
    (attributes), Eagle (the Critter Safari).
  - Mining's other places and blocks: the Crystal Hollows (Bal, Goblin, Mole, Scatha's Burrowing, Armadillo), the
    Glacite Tunnels and Mineshafts (Glacite Golem's other two, Mammoth's other two, Penguin's Subzero Hero), Heart of
    the Mountain levels (Ammonite's Heart of the Sea), Mining Spread (Snail's Slimy Reach), End Stone and the Draconic
    Altar (Endermite's other three).
  - Mounts and morphs: Armadillo and Rock (riding), Rat.
  - Events and calendar years: the Spooky Festival (Bat's three, Eerie), the Years of the Witch, Pig and Seal (Witch,
    Pig, Seal), the Fishing Festival (Megalodon's other two), Jerry's Workshop and the Gift Attack (Baby Yeti's other
    three, Reindeer), the Mythological Ritual (Griffin's other three).
  - Fights and things to fight: Kuudra (Kuudra's other three), Slayers (Tarantula's Webbed Cells; the Wisps' Bulwark,
    Icehammer and Drophammer), the Crimson Isle's pools (the Wisps' Extinguish), special Zealots (Enderman's Zealot
    Madness), mob healing (Tiger's Hemorrhage), souls (Ghoul's other two), Midas' Greed (Golden Dragon's Gold's
    Power), Power Orbs and Dungeon Potions (Jellyfish's other two), the Spirit coming to a ghost (Spirit Assistance).
  - Potions and brewing: Parrot's two, Rabbit's Happy Feet, Witch's Alchemism.
  - Others: Superpairs (Guardian's Lucky Seven), Fann (Owl's other two), Drone Mods (Precursor Drone), squids told
    apart in drops (Squid's More Ink), Bingo profiles (Bingo).
- **Hooks added** for them (the smallest versions, as EFFECTS.md asks): stats once flat buffs are in
  (`PlayerStats.addBuffedModifier`), kill XP factors (`SkillGains.addKillXpFactor`), max mana factors
  (`PlayerSession.addMaxManaFactor`), Mining Speed by block (`BlockListener.addMiningSpeed`), powder bonuses
  (`Powder.addBonus`), Intimidation levels and the accessories' Haste (`AccessoryEffects.addIntimidationLevels`,
  `hasteLevel`), Last Stand's effectiveness (`ArmorEnchants.addLastStandBoost`), and in a run, Half Life's score
  (`DungeonRun.setHalfLife`), Ally Healing (`addAllyHealing`) and cutting an ultimate's cooldown (`cutUltimate`).
- **Approximated (UNKNOWN)**:
  - "For every": whole steps (Bulk, Stomp, Walking Fortress, Tusk Luck, Dragon's Pride, Dragon's Greed, Higher Ground),
    but Farming Strength, which the wiki rounds down once (⌊Strength / 20 × 0.7⌋).
  - How long and how far where the text doesn't say: Web-weaver's slow 3s, Ferocious Roar's stun a second, Turtle
    Shell's Vitality 10s, Snow Cannon 8 blocks, Skeletal Defense 3, Laserbeam 16, "near water or lava" 3, a "nearby
    wolf monster" 10. Turtle Tactics' "standing still" is where you were a second before.
  - Kill Combo: a kill within the time of the tier reached keeps it; its lines are "+5 Kill Combo" and "Your Kill Combo
    has expired!" (Danker's Skyblock Mod hides lines with those), the rest of them made up.
  - Stacks: Stampede's each last their own 5s, the oldest going past 20; a Hop jump starts its 20s again; Overheal's
    shield comes once you have none of it left.
  - Stored Energy: every whole 2,000 Ally Healing takes its seconds off a Wish that's cooling down (none while it's
    ready), 30s off one cooldown at most. Unyielding makes both Last Stand's Defense and its Vitality more.
  - Whose counts: an aura's owner gets Bird Discourse too, Genius Amniote goes to the four nearest others; Happy Tree
    Friends counts the other players in the world with a Frog out; Pod Tactics, Chilly Reception and Crab Rave the
    others within reach.
  - Where: "Mining Islands" are the Dwarven Mines, Mithril Affinity's Mithril its blocks, powder "from all sources" what
    you mine (the only source here), and Dexterity's Haste is Mining Speed as the Haste Ring's is (50 a level), not on
    top of the ring's.

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
- **Pet items**: the lines' colours; "already holding" ("&cYour pet is already holding this item!") and too few coins
  ("&cYou don't have enough coins!"); Swap Pet Item's Cancel (hidden under the screenshot's tooltip) and the Super
  Scrubber's menu (this one: "Confirm Removal", "Remove ... from your pet."); anything rarer than Divine costing
  Divine's; looking at the head (its middle 0.75 over the stand's feet, 0.45 to either side); a hidden pet taking none;
  the stat lines showing a share item's stats; the Pet Score counting a Tier Boost's rarity; Unalloyed Speed's +50 not
  adding to a Black Cat's +100 (the most counts, as the speed caps here do); a Reaper Gem kill starting its 5s again.
  The wiki's "it will always consume the item in the slot closest to the first" isn't copied: it's the one in your hand.
  The wiki says a Minos Relic "does not increase the perks of a pet" (the Black Cat's Magic Find); here it increases the
  data's stat lines, the Black Cat's included.

## Not done

- **Ability effects** that need what isn't built (see Abilities).
- **Pet items** that need what isn't built: Exp Share (the Exp Sharing slots), trophy fishing's Barrel of Riches and
  Lotus Crown, the Brown Bandana (pests), Bubblegum (Deployables), the Titanium Minecart (no Titanium), the Four-Eyed
  Fish's coins (Griffin Burrows), the Rubber Snorkel's double on Foraging Islands, and the Kat upgrade items.
- **Exp Sharing** (a click does nothing), **Search** (nothing), Autopet, pet skins (kept, not shown), Pet Candy (shown,
  not used), Kat, George, Fann.
- The Daedalus Blades' Damage and Magic Find on Mythological mobs (Diana's), and their Damage line showing the Taming
  levels' (it counts, but the lore shows the item's own).
