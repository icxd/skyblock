# /yoinkprofile

`/yoinkprofile <player>` copies a real Hypixel SkyBlock profile onto the one you play on: its items, storage
and progress, as far as this plugin has them. It's a staff command for setting up a profile to test with. It
reads Hypixel's public API (v2) with this server's key and never connects to Hypixel's game servers. The code
is in `paper/src/main/java/net/icxd/dungeons/yoink`; the command is
`command/commands/admin/YoinkProfileCommand.java`.

## Setup

The API needs a key, from developer.hypixel.net. Put it in each server's `plugins/dungeons/config.yml`:

```yaml
hypixel:
    api-key: 'your-key'
```

Or leave that empty and set the `HYPIXEL_API_KEY` environment variable where servermgr starts. Every server
inherits it from servermgr's environment. Both are read as the server starts, so a new key takes a restart.
Keep it out of the repository. A key that has expired or is wrong gets "Hypixel says
the API key isn't valid".

`hypixel.api-url` is where the API is. Change it only to point at a stand-in for testing.

## Done

**Looking them up.** The player's name is looked up with Mojang (`api.minecraftservices.com`,
`profile/lookup/name/<name>`). An id, with or without dashes, also works; it's looked up for the name to
show. Then the command asks for `skyblock/profiles?uuid=` with the key in its `API-Key` header. Both requests
run off the main thread. "Looking up X's profiles..." shows first, then either the menu or what went wrong:
no such player, an invalid key, too many requests (429), Hypixel's own `cause`, or no profiles.

**Yoink from \<player\>** (4 rows) lists their profiles, the one they play on first (an emerald block; the
others are grass). Each shows its mode (Ironman, Stranded, Bingo), co-op members, SkyBlock Level, purse, and
bank ("not in the API" when the banking API setting is off). Profiles they left (a `deletion_notice`) aren't
listed.

**Yoink \<profile\>** (6 rows) shows a button for each part, which picks it or leaves it (picked ones glint).
A part whose data the API doesn't have shows red: "Not in the API: its API setting is off". The bottom row
has Everything (picks all or none), Go Back, Yoink! (it lists the parts picked and warns "These replace
yours!") and Close. Yoink! closes the menu. It says "Yoinking \<profile\>...", then lists what each part
brought and what couldn't come over, by id and count, most first. It also lists the enchantments this
plugin doesn't have. Each yoink is logged, with how long it took.

Each part replaces that part of the profile you play on:

- **Inventory**: the 36 slots and the armor, onto you. Hypixel's SkyBlock Menu in slot 9 is replaced by this
  plugin's.
- **Equipment**: necklace, cloak, belt and gloves, in no Equipment Set.
- **Wardrobe**: Hypixel's pages of 9 sets, each set helmet to boots, as sets 1 onward. The set they wear on
  Hypixel is the one worn here when the inventory comes over too. Otherwise its pieces are put back in the
  set.
- **Ender Chest**: pages of 45.
- **Backpacks**: each in the same slot (Hypixel's 0 is slot 1), its icon being the backpack itself, with
  its contents.
- **Accessory Bag**: the bag's contents, the selected power (Hypixel's "silky" is the tables' "Silky"),
  Stats Tuning, the most Accessory Power the bag has had (`highest_magical_power`, which SkyBlock XP counts;
  the bag here only raises it), and Jacobus's upgrades (`bag_upgrades_purchased`, NEU's profile viewer's key).
  Tuning comes from slot 0's `walk_speed`, `critical_damage` and the rest.
- **Other Bags**: the Potion Bag, the Fishing Bag and the Sack of Sacks.
- **Coins**: the purse, and the bank when the API has it.
- **Skills**: each skill's XP, `SKILL_COMBAT` and the rest.
- **Collections**: every collection by Hypixel's id. The plugin's collections use the same ids.
- **Dungeons**: Catacombs XP, each class's XP, the selected class, the highest floor and Master floor, and
  each essence.
- **Heart of the Mountain**: tokens left to spend, HotM XP, powder (Mithril, Gemstone and Glacite), and the
  levels of the four perks here. Hypixel's node ids are `mining_speed`, `mining_fortune`, `titanium_insanium`
  and `forge_time`, per SkyCrypt's `constants/hotm.js`; levels are capped at each perk's most here.
- **Crimson Isle**: Mage and Barbarian reputation (`mages_reputation`, `barbarians_reputation`) and the highest
  Kuudra tier beaten (the last of `kuudra_completed_tiers`' `none`, `hot`, `burning`, `fiery` and `infernal` with
  a completion; the keys are NEU's profile viewer's). Both count for SkyBlock XP. Not the faction picked: the
  tab list's lines for a faction aren't finished (its progress bar).
- **Crafted Minions**: the minion tiers crafted.
- **Fairy Souls**: how many were found and given to Tia (`fairy_soul.total_collected`, `fairy_exchanges`). The
  API doesn't say which, so here they're on no island and the Hub's can be found again (FAIRY_SOULS.md).

**Items** (`HypixelItems`). The API's items are base64 of gzipped Minecraft 1.8 NBT, read by `Nbt`. Each
becomes the item of the same SkyBlock id (`ExtraAttributes.id`), with what was done to it written the way
this plugin keeps it:

- the reforge (`modifier`)
- Recombobulated (`rarity_upgrades`)
- stars: the larger of `upgrade_level` and `dungeon_item_level`, so Master stars come over too
- Hot and Fuming Potato Books (`hot_potato_count`)
- The Art of War and The Art of Peace
- the Book of Stats with its kills
- Farming for Dummies, Bookworm's, Polarvoid and the Wet Book
- enchantments, an ultimate without its `ultimate_`. Ones this plugin doesn't have are listed.
- gems: each slot by type and number (`COMBAT_0`), a special slot's gem from `COMBAT_0_gem`, older items'
  quality as text and newer items' `{quality}`, and the slots in `unlocked_slots` opened
- the three scrolls on a Necron's blade
- attributes: the first two this plugin has
- the Grand Searing rune, on a chestplate
- Transmission Tuners, Mana Disintegrators, the Jalapeno Book, the Wood Singularity, the Power Scroll and the
  Enrichment
- stacking enchantments' counts
- a dungeonized item's flag
- the item's uuid, kept when it doesn't stack

The amount is kept. Inventory items that have requirements are made yours, so their requirement lines go by
you. Storage items get an owner the way items always do here.

**On the server.** A big profile's storage is a thousand-odd items. A test profile with 1,400 (a full Ender
Chest and 18 full backpacks) took 1.7 s on the main thread when done all at once. So the storage's items
are made and stored off the main thread. Then everything is written on it together: 131 ms for those
1,400. Before writing, any open storage menu is closed, since it writes back as it closes. Nothing is
written if you've switched profile or are moving servers by then. The inventory is then rebuilt, so items'
requirement lines go by the skills and floors just yoinked, and saved. You can run only one yoink at a time.

## Approximated (UNKNOWN in the code)

- **Left out.** Pets aren't brought over: there are no pets here, so they're listed as "ENDER_DRAGON Pet".
  Potions aren't either: this plugin's `POTION` is a Water Bottle. What's in the Quiver, an unknown
  backpack, or a backpack slot past 18 is listed as not brought over.
- **Not copied.** Skins and dyes: an item looks as its kind does here. The rest of `ExtraAttributes` isn't
  copied: drill parts, `timestamp`, `originTag`, Soul Eater's count (its key can't be stored here) and the
  rest.
- **No sacks.** There is nowhere to keep sacks' contents here (see STORAGE.md), so there's no Sacks part.
- **Bags past capacity.** A bag with more items than its capacity here keeps them, but they're hidden until
  the collection that gives the capacity is reached (see StorageDocument).
- **Ironman and co-op.** These are copied as they are; the profile it goes onto keeps its own mode. The
  bank is the co-op's, as Hypixel's is.
- **SkyBlock Level.** Not copied. It's worked out here from what's done, so it levels up after a yoink by
  itself ("SKYBLOCK LEVEL UP Level 0 ➡ [22]" for the test profile).
- **Not tested on a live profile.** This was built from the paths NEU's profile viewer reads and from
  SkyCrypt's constants, not tested against a live answer (no key here). It was tested against a stand-in
  serving `paper/src/test/resources/yoink/profiles.json`, made by `profiles.py` there.

## Later

- Pets (with `petInfo`), when there are pets: the pet menu and `pets_data.pets`.
- Sacks, slayers, fairy souls, the museum, and the rest of the profile this plugin doesn't keep yet.
- Skins and dyes, when items here can have their own.
