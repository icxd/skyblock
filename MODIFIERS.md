# The Hex's Modifiers

The Hex's Modifiers category ("The Hex ➜ Modifiers") and the item upgrades under it: the Recombobulator
3000, Master Stars, the Wither Scrolls, Transmission Tuners, Silex, the Wood Singularity, Mana
Disintegrators, the Jalapeno Book, Power Scrolls and Enrichments. It's one part of task 70; the Hex itself
(`/hex`, its menus, costs and the item's life in it) is in HEX.md.

The rules come from the Hypixel SkyBlock wiki (The Hex, its UI tabs, each item's page), NEU's Hex screen,
the official 0.14 screenshot of the Hex's summary, the item data (items.json: each modifier's own text, the
API's `can_recombobulate` and `can_have_power_scroll`) and live auction items (September 2026, for the data
keys and the lore). The research is in the task's scratchpad (`hex/result_hexui.md` §8,
`hex/result_items.md` §6.12-6.24, `hex/result_data.md` §2.4).

The code:
- `paper/src/main/java/net/icxd/dungeons/item/modifier`: what each modifier does to an item, wherever the
  item is (`ItemModifiers`, `PowerScroll`, `Enrichment`).
- `paper/src/main/java/net/icxd/dungeons/hex/modifier`: which modifiers the Hex offers an item and what
  applying one does to its data (`HexModifiers`), and the page (`ModifiersPage`).
- `paper/src/main/java/net/icxd/dungeons/hex/category/Modifiers.java`: the category.

No private tables: every number that's Hypixel's is read at runtime from the item data (the modifiers'
own text), or is a single constant in the code.

## Done

**The category.** Carpentry 20 on a Normal profile (the core's gate), nothing on Sandbox. Its button is
the Recombobulator 3000's head. It's for an item that takes any modifier, and its summary is a line for
each, in the wiki's order:
- `  &6Recombobulator 3000 &c✖` (or `&a✔`)
- `  &cMaster Star &c✖`, then a count, `&e1&7/&a5`
- `  &5Wither Scrolls &e0&7/&a3`
- `  &5Transmission Tuner &e0&7/&a4`
- `  &5Silex &e0&7/&a5` (levels past Efficiency V)
- `  &6Wood Singularity &c✖`
- `  &9Mana Disintegrator &e0&7/&a10`
- `  &5Jalapeno Book &c✖`
- `  &7Power Scroll &c✖`
- `  &7Enrichment &c✖`

A full count is green (`&a4&7/&a4`). Only the Recombobulator's, the Master Star's, the Power Scroll's and
the Enrichment's lines are seen on Hypixel (see Approximated).

**The page** (`ModifiersPage`): the Hex's page frame, an Anvil "&aApply Modifiers" (the button's lines)
in 28, and the modifiers the item takes, centred (1 in 23, 6 in 22-24 and 31-33, 12 row by row, as the
wiki's tabs show them). Each is the modifier's item as the item data has it:
- its look;
- its name in its rarity's colour (a Power Scroll's in its gem's, as the wiki's screen has them);
- its own text;
- the Cost block: one of the item (a Power Scroll's line in its gem's colour too, `&cRuby Power Scroll &c✖`, as on
  the wiki's screen), then "&eClick to apply!".

A click pays and applies it ("You applied a Recombobulator 3000 to your ...!"). If they don't have the
item, the Cost block says so, and a click says "You don't have that in your inventories!" in chat. On a
Sandbox profile it's free: nothing is needed or taken. One that's on, or maxed, stays listed ("&aApplied!",
"&aMaxed out!") and does nothing.

Which items take which, and what each does:

| Modifier | Items | How many | Data | What it does |
|---|---|---|---|---|
| Recombobulator 3000 | anything with a type, but those the API says can't and pet items | once | `recombobulated` (the plugin's) | rarity one up |
| Master Stars (First to Fifth, in order) | dungeon items (the kind, or the `dungeon_item` tag) with five stars | 5 | `upgrade_count` 6-10 (➊-➎, as the name already shows) | +5% each in Master Mode |
| Wither Scrolls (Implosion, Shadow Warp, Wither Shield) | Necron's Blade, Hyperion, Astraea, Scylla, Valkyrie | one of each | `implosion`, `shadow_warp`, `wither_shield` (NecronsBlade's) | their abilities (already built) |
| Transmission Tuner | items with a "... Transmission" ability | 4 (the Aspect of the Leech: 1) | `tuned_transmission` | +1 block each |
| Silex (`SIL_EX`) | pickaxes, drills and gauntlets with Efficiency V, not the Stonk | to Efficiency X | the `efficiency` enchantment | +1 Efficiency level |
| Wood Singularity | axes | once | `wood_singularity_count` | +25 Foraging Fortune |
| Mana Disintegrator | wands and deployables | 10 | `mana_disintegrator_count` | -1% mana cost each |
| Jalapeno Book | deployables | once | `jalapeno_count` | +5 Crit Damage, +1 Crit Chance in its buff |
| Power Scrolls (Ruby, Sapphire, Jasper, Amethyst, Amber, Opal) | items with a RIGHT CLICK ability, or the API's `can_have_power_scroll`; not the Egglocator or a Wither Scroll itself | one at a time | `power_ability_scroll` = the scroll's id | on each use of a RIGHT CLICK ability |
| Enrichments (the eleven) | accessories that are Legendary or better now | one at a time | `talisman_enrichment` = the stat's key | its stat |

The data keys are Hypixel's (as live items carry them), but for the plugin's own older ones:
`recombobulated`, `upgrade_count`, the scroll flags and `enchantments`. They're only written when a
modifier is applied (new items don't get them).

**Recombobulator 3000.** Very Special now stays Very Special (`Rarity.upgrade`, the wiki's table); it went
to Unobtainable. `/recombobulate` (the staff toggle) is as it was.

**Master Stars.** In a Master Mode run (`DungeonRun.masterMode`), each adds 5% of the item's stats, to its
stars' 10% each (`ItemBuilder.dungeonFactor`, `ItemStats`). Elsewhere they do nothing, as on Hypixel
("Master Stars do not provide any bonuses outside dungeons"). The lore's dungeon bracket (`&8(...)`) is as
it was.

**Transmission Tuners.** Instant Transmission (8 blocks), Ether Transmission (its text's 57) and the
Sinseeker Scythe's Sinrecall Transmission (4) go a block further for each. The lore's number counts them:
"Teleport &a12 blocks" on an Aspect of the Void with 4, "to &a61 blocks" on an Etherwarp Conduit, as live
items show.

**Silex.** Each raises the item's `efficiency` enchantment by a level, kept in its enchantments as
`/addenchantment` keeps them. The plugin has no Efficiency enchantment yet (the enchantments part adds it),
so until then the level isn't in the lore.

**Wood Singularity.** +25 Foraging Fortune in the stats and the lore, with its own bracket after the potato
books' and the Art of War's: `&7Foraging Fortune: &6+91 &6(+25) &9(+12)` (a live Moonglade Treecapitator's).

**Mana Disintegrators.** An ability's mana cost is 1% less for each (`Abilities.manaCost`, and the Staff of
the Rising Sun's in `Bolts`). A cost that's a
share of max mana is 2% less for each: the power orbs' in their data, and the Lanterns' and the
Umberella's in their text ("Costs 50% of max mana", which Deployables reads). The lore marks the mana cost
as live items do, `&8Mana Cost: &b300✎&8 (&910&9ᛃ&8)`, and shows a share less: "40% of max" with 10.

**Jalapeno Book.** The buff of the deployable it's on gets +5 Crit Damage and +1 Crit Chance
(`Deployables`), and its lore gets two lines at the end of the buff, in its bullets' colour:
`&5• &7Grants &9+5☠ Crit Damage&7. &a⒥`. The numbers are the wiki's and a live Overflux Power Orb's; the
book's own text has none.

**Power Scrolls.** Using a RIGHT CLICK ability (not a sneak one) does what the scroll's own text says
(`PowerScroll.used`, read from the item data):
- Ruby: heals 1% of their missing Health, at most every 5s.
- Sapphire: +5 Mana, at most every 5s.
- Jasper, Amethyst, Amber, Opal: +10 Strength, +10 Defense, +30 Mining Speed or +10 True Defense for 5s.

The ability's header gets the scroll's coloured "⦾": `&b&l⦾ &6Ability: Instant Transmission  &e&lRIGHT
CLICK`.

**Enrichments.** The stat counts in the item's stats (`ItemStats`), so in the Accessory Bag too, and in the
lore's total, with no bracket. The lore's first line is `&7&8Enriched with Magic Find`, then a blank one
(live lore). How much each gives is read from its item's text ("power of +0.5✯ Magic Find").

## Approximated (UNKNOWN in the code)

- **Summary lines.** Only four are seen: the Recombobulator's, the Master Star's and the Power Scroll's on
  the official screenshot, the Enrichment's on the wiki's accessory tab. The other six are ours, in the
  same grammar, in the items' rarity colours. Master Star is ✖ at none (the screenshot's five-star Livid
  Dagger), then a count (ours). Whether a full count is green is unknown (the screenshot's "10/10" looks
  it).
- **The action line** "&eClick to apply!" (U4). "&aApplied!" and "&aMaxed out!" on one that's done (U5:
  where Hypixel's "Item Maxed Out" goes). The chat line when they don't have the item.
- **Where 2 and 7-11 entries sit** (the core's U3). A weapon with a RIGHT CLICK ability has 7 or 8, so
  they go row by row.
- **Recombobulator 3000.** "Any item with a named category" (the wiki's table) is items.json's type,
  minus the API's 89 that can't. Pet items are left out too: the wiki says they can't be, and none of 925
  live ones is. Live, no cosmetic (of 1,331), travel scroll, rod part, reforge stone and several other
  types is recombobulated either (see Questions). Unobtainable (the API's rarity for admin items) stays as
  it was, rather than the wiki's cosmetics' rarity going to Very Special.
- **Master Stars.** How the 5% adds up with the stars' and the Catacombs boost: added to the stars' 10%
  each. Not in ability damage (`Hits.dungeonFactor`). Not in the lore's dungeon bracket either, as live
  items show (Hyperions with ➊ to ➎ mostly have the same Ferocity bracket as five-star ones, 1.4 times).
  The next star is offered only once the item has its five; before that the line shows and nothing is
  offered. It costs the star alone: an anvil also takes 300 to 500 Exp for one (the wiki's Master Star),
  and whether the Hex does is unseen (see Questions).
- **Silex on gauntlets.** The wiki says pickaxes and drills; live Gemstone Gauntlets have Efficiency VI
  to X, which only a Silex gives, so they take it too. Below Efficiency V it isn't offered (as NEU's Hex
  lists it).
- **Transmission Tuners.** Items with two Transmission abilities take 4. The first number of blocks in
  the ability's text is the one raised.
- **Mana Disintegrators.** Rounding: to the nearest. The lore keeps the cost's number (live Alert Flares
  and Gyrokinetic Wands with 10 show their full cost; a live Fire Veil Wand's is lower, by a rule not
  found). A text cost's 2% is from live Umberellas and Will-o'-wisps, which show "40% of max" with 10.
  The Staff of the Rising Sun's cost is only in its text ("Costs 10% of your total mana", Bolts): it's 1%
  less a disintegrator, as a wand's (0.20.5 fixed them "not working on the Staff Of The Rising Sun despite
  it being a wand"); whether it's 2%, as a power orb's share, is unknown. Its text isn't changed.
- **Power Scrolls.** A new one takes the old one's place, and the old one is gone (whether Hypixel gives
  it back is unknown). A Ruby's or Sapphire's 5s cooldown is the player's, one a scroll, whichever item
  it's on. Each RIGHT CLICK ability's header gets the "⦾" (no live item with two has a scroll).
  Eligibility is the wiki's "RIGHT CLICK ability" in the item data (with a Necron's blade's scroll
  abilities) or the API's flag, but not the Egglocator (0.20.5: "Fixed Power Scrolls being applicable on
  Eggolocators") or a Wither Scroll itself (its RIGHT CLICK is the one it gives). Live items also have
  them on drills and the Jungle Pickaxe, whose HOTM ability isn't in the data. Live, only swords,
  longswords, a spade and mining tools carry one (none of 197 wands, 259 deployables, 182 axes or 54 bows
  with a RIGHT CLICK ability does), and 0.20.4 took them off Abiphones too, so the real rule may be
  narrower (see Questions).
- **Enrichments.** A new one takes the old one's place, the old one gone. Offered only on accessories
  that are Legendary or better now (their text: "a Legendary or Mythic accessory"). The "Enriched with"
  line goes first, above anything else.
- **Jalapeno Book.** Its numbers aren't in its own text: the wiki's and one live orb's. On a deployable
  whose buff has no bullets, nothing shows.

## Later, and what it waits for

- **Tool Exp Capsule** (added to the Hex in December 2025): farming tools' levels, which aren't here.
- **Divan's Powder Coating**: not in the Hex's sources.
- **The Wither Scrolls' lore**: the "&aScroll Abilities:" line above them, and Wither Impact's newer text
  (result_items.md §6.14). The first needs a line in an ability's lore before its header, which the lore's
  blocks don't have.
- **Efficiency** in the lore and its effect: the enchantments part.
- **The `dungeon_item` tag everywhere else** (a converted item's stars, stats and lore): the books and
  item upgrades part. Master stars already go on a converted item.

## Questions for the owner

- Live, nothing of these types is recombobulated: cosmetics, travel scrolls, rod parts, reforge stones,
  lassos, masks, watering cans, mementos, shears, nets, traps, portals, ores, chisels. The wiki says "any
  item with a named category". Should the Recombobulator be left off those too? (Pet items already are.)
- A Power Scroll or an Enrichment put over another: should the old one come back to them?
- Power Scrolls go on anything with a RIGHT CLICK ability (the wiki's rule), so wands, deployables, axes,
  bows and gadgets take them. Live, none of those has one (only swords, longswords, a spade and mining
  tools do). Should they be for weapons and tools only?
- A master star costs the star alone in the Hex here. An anvil also takes 300 to 500 Exp levels for one
  (the wiki's Master Star). Should the Hex take them too, on a Normal profile?
