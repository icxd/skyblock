# Effects: the hooks

Task 80 makes enchantments, stats and item and armor abilities work. This document is its first stage, the
core. It lists the hooks the effects are built on and the part that owns each effect. The core builds no
effects of its own, apart from one example for a few hooks where the example is the test that the hook
works (see "Examples the core built"). The parts that come after write the effects on these hooks. Each
part keeps its own status document (see "Who owns what").

Everything here runs on the main thread. The code is in `paper/src/main/java/net/icxd/dungeons`. The rules
are pure functions with tests under `paper/src/test`: `DebuffsTest`, `KillingBlowTest`, `ActivationsTest`,
`CombatStateTest`, `VanillaDamageTest`, `AbsorptionTest`, `ExpOrbsTest`, `ManaTest`, `PercentBuffTest`,
`ItemCountersTest`, and cases added to `DamageTest`, `HitBuffTest`, `PlayerDamageTest`, `SkillGainsTest`,
`MobKindsTest`, `StatsRunnableTest` and `WitherBladeTest`.

Where the sources say nothing, the code comment says UNKNOWN and takes the simplest reading. Each UNKNOWN is
also listed at the end.

## Hits on mobs

### What a hit is: `combat/HitKind`

| Kind | What deals it | Hit listeners hear it |
|---|---|---|
| `MELEE` | A melee hit or a fist | yes |
| `ARROW` | A player's arrow landing | yes |
| `FEROCITY` | Each Ferocity extra strike | yes |
| `ABILITY` | Each of an ability's hits (`Hits.hurt`) | yes |
| `DOT` | A damage-over-time tick (`MobDebuffs.dot`) | no |
| `OTHER` | Any other effect's damage (`MobHits.deal`), a set bonus's (`Bonuses.damage`), a dungeon class ability's | no |

`HitKind.isHit()` is true for the first four. An effect's own damage is `DOT` or `OTHER`, so it never sets off
the hit listeners. This keeps Cleave from cleaving its own share. Whether Hypixel's effects can set off other
on-hit effects is UNKNOWN; here they can't.

### Buffs on a hit as it lands: `Combat.addHitBuffs`

- **`addHitBuffs(HitBuffs)`** already existed: `(player, attacker, target) -> HitBuff`.
- **New: `addHitBuffs(LandingBuffs)`**: `(player, attacker, target, landing) -> HitBuff`. Use it when a buff
  needs the mob or the crit roll (F2): Livid Dagger's crits from behind, a bow's "+100% damage to Undead".
- **When they're asked:** for melee hits and arrows, after the crit roll and before the damage is worked out.
- **`Combat.Landing`** is `(LivingEntity entity, HitKind kind, boolean critical, NBTTagCompound weapon, Projectile projectile)`.
  - `weapon` is the held item's data for a melee hit and the bow an arrow left for an arrow (F5). It's null
    for a fist or a vanilla item.
  - `projectile` is null except for an arrow.
- **`HitBuff(additive, multiplier, added)`**. The two-argument form still works, with `added` 0.
  - `additive` joins the hit's additive sum, in percent.
  - `multiplier` multiplies the hit.
  - **New: `added`** is the wiki's "Add Damage" (Soul Eater, Extreme Focus). It's added after the additive and
    multiplicative buffs, only the crit multiplies it, and it comes before the caps and Defense:
    `Damage.exact(attacker, target, critical, added)`.
- **How they combine:** additives and added damage add up, multipliers multiply. SetBonuses' aggregate now
  carries `added` too.

### After a hit lands: `Combat.addHitListener`

`HitListener.landed(Player player, Landing landing, Damage.Target target, double damage, boolean killed)`
(hooks 1 and 3).

- **When it fires:** once the damage has been dealt, for every melee hit, arrow, Ferocity strike and
  ability hit on one of SkyBlock's mobs that can be hurt.
  - Melee hits and arrows fire it before their Ferocity strikes are scheduled.
  - Each strike fires it again with `FEROCITY` and the same weapon and crit.
  - Each mob an ability hits fires it with `ABILITY`. The weapon is the item it was cast with where the code
    knows it (spells, heals over time, bolts), otherwise what they hold.
- **What it receives:**
  - `target` is the mob as the hit was worked out against: its health before the hit, and its Defense with
    its debuffs.
  - `damage` is what the hit did, after the debuffs' "takes more".
  - `killed` is true if the hit killed the mob.
- **It doesn't fire for** invulnerable mobs (the Watcher) or `DOT` and `OTHER` damage.
- **Rule for listeners:** a listener that deals damage should use `MobHits.deal` (see "An effect's own damage"),
  never `Hits.hurt`. `Hits.hurt` is an ability's hit and would fire the listeners again.
- `Combat.landed(...)` is the call the hit paths use. Effects don't call it.

### Players hitting players: `Combat.addPlayerHitListener` (F6)

`PlayerHitListener.hit(Player attacker, Player target, HitKind kind, NBTTagCompound weapon, Projectile projectile)`
fires for a player's melee hit or arrow on another player (the Stinger Bow's Sting on a teammate). The hit
itself is unchanged: a SkyBlock item's hit on a player is still called off, and anything else stays vanilla.

### An effect's own damage, and what a mob is: `combat/MobHits`

- `hittable(entity)`: whether it's one of SkyBlock's mobs (either kind) that can be hurt.
- `target(entity)`: its `Damage.Target` now, with its debuffs' Defense. Null for anything that isn't one of
  SkyBlock's mobs. `Hits.target` and melee hits use it.
- `deal(by, entity, damage, look, kind, weapon)`: an effect's damage (Cleave's share, a lightning strike).
  - It takes the same path as a hit: health, the damage number, the kill and drops for `by`, the run's
    damage dealt, and the combat state.
  - A room mob waiting for its room wakes first.
  - It fires no hit listeners and no Ferocity. Pass `OTHER` (or `DOT`).
  - It returns false if the mob can't be hurt.
- `alive(entity)`: whether it's still a live SkyBlock mob.

### Mob debuffs and damage over time: `combat/MobDebuffs`, rules in `combat/Debuffs` (hook 2)

- **Debuffs:** `MobDebuffs.add(mob, new Debuffs.Spec(source, kind, perStack, maxStacks, millis), byPlayer)`.
  - **Kinds:**
    - `DEFENSE`: a share of its Defense taken off. They add up, to at most all of it.
    - `TAKEN`: it takes a share more damage from players. Each source is a factor, and they multiply
      (UNKNOWN whether Hypixel's add).
    - `SLOW`: a share of its movement speed taken off. They add up.
  - **One per source per mob.** Putting it on again adds a stack, up to `maxStacks`, and starts its time again.
    The stronger per-stack value stays, and so does who put it on ("refreshing never lowers it", the wiki's
    Damage Calculation).
  - **Separate stacks per player:** stacks are per mob across all players. To keep each player's apart, use a
    per-player source (`"lethality:" + uuid`). How Hypixel stacks them across players is UNKNOWN.
  - **Queries:** `stacks(mob, source)`, `remove(mob, source)`, `takenFactor(mob)`, `target(mob, rawTarget)`.
- **Where hits read them:**
  - Defense: `MobHits.target`, which melee hits, arrows, Ferocity strikes and every ability use.
  - "Takes more": multiplied onto melee and arrow hits (in `Combat.playerHit`) and ability hits (`Hits.takenFactor`).
- **Slows:** a transient modifier on the mob's movement speed attribute. The Blood Room's player-shaped undeads
  steer themselves, so they aren't slowed (UNKNOWN).
- **Damage over time:** `MobDebuffs.dot(mob, source, byPlayer, damage, everyTicks, times, look)`.
  - Each tick deals `damage` through `MobHits.deal` as `DOT`, credited to the player: their kill, drops and
    damage dealt.
  - Its number looks as `look`: `DamageIndicators.Look.FIRE` is gold and `POISON` is dark green, as the wiki's
    Damage Indicator table has them.
  - Putting it on again restarts its count, keeps its beat, and keeps the greater damage and whose it is (the
    wiki's Fire Aspect example).
  - The damage is dealt as given, so the caller works in Defense and the rest (the wiki's Fire Aspect and
    Venomous formulas differ).
  - `hasDot(mob, source)` tells whether it's still running.
- **Lifecycle:** `MobDebuffs.tick()` runs every tick from `Mobs.tick`. Mobs that die or go are forgotten
  (`Mobs.remove` and `DungeonMobs.remove`).
- **Vanilla effects:** vanilla fire and poison on mobs stay cancelled (`Mobs.onHurt`). These debuffs are
  SkyBlock's version.

### Arrows: `combat/Shots` (F5)

- A recorded arrow keeps the bow it left, so hit listeners and buffs get it as `Landing.weapon`.
- `Shots.bow(projectile)` returns that bow.
- `Shots.scale(projectile, factor)` multiplies the recorded arrow's damage (Arrow Infusion's "double the
  damage").
- `Shots.addShotListener((player, projectile, bow, fullyDrawn) -> ...)`:
  - It fires when a player shoots a drawn bow (vanilla's `EntityShootBowEvent`), after the arrow is recorded.
  - Shortbows shoot through their ability and don't fire it.
  - The ON_SHOOT abilities come through it.

### Kills: `SkyBlockMobDeathEvent` (hook 18)

- `killer()` already existed.
- **New: `killingBlow()`** returns `KillingBlow(HitKind kind, NBTTagCompound weapon, double damage, double overkill)`.
  - `weapon` is the held weapon, the bow an arrow left, or the item an ability was cast with.
  - `overkill` is how much of the blow was more than the mob had left.
  - It's null when nobody killed the mob (/kill).
- **New: `killedBy(HitKind)`**.
- **Where the blow comes from:** the sinks take it as `Mobs.damage(live, player, damage, look, kind, weapon)` and
  `DungeonMobs.damage(entity, player, damage, look, kind, weapon)`. The old forms say `OTHER`. The Blood Room's
  undeads die inside their own `hurt`, so `DungeonMobs.damage` sets the blow around that call
  (`KillingBlow.dealing`).

### Attack Speed cap: `Combat.addAttackSpeedCap` (hook 12)

- `addAttackSpeedCap(player -> raise)` adds a raise (Newton's Demise: +50). The most of the raises counts, as
  for the Speed cap (UNKNOWN whether raises add).
- `Combat.attackSpeedCap(player)` is 100 plus the raise. Melee invulnerability ticks and shortbow shot
  cooldowns use it (`Damage.invulnerabilityTicks(as, cap)`, `Damage.shotCooldownTicks(s, as, cap)`).
- The wiki's Attack Speed page confirms the cap can be raised to 150.

### Shared helpers, public now (hook 16)

These were package-private. They're public now with no change in behaviour, so effects outside the ability
packages can use them:

- **`item/ability/weapons/Hits`**:
  - finding mobs: `near`, `inCone`, `along`, `find`, `aimed`, `aimedMob`
  - damage: `magic`, `spell`, `weaponHit`, `striker`, `Strike`, `hurt`, `overTime`, `report`
  - ability data and costs: `base`, `spellOf`, `enoughMana`, `takeMana`, `canPayHealth`, `payHealth`
  - status: `canStillHit`, `freeze`, `root`, `takenFactor`
- `Magic`: `Spell`, `Caster`, `damage`, `additive`, `forAbilities`.
- `Tally`: an ability's chat line.
- `item/ability/utility/Heals`: `give`, `overTime`, `amount`.
- `Buffs`: `give`, `youAndNearby`.
- `Protection`: `immunity`, `noAttack`, `dealt`, `taunted`, `dealtFactor`.
- `AbilityText`: numbers from an ability's text.
- `Combat.skyBlockData(ItemStack)`.

## Players: resources and hits taken

### Mana spent: `session/Mana` (hook 4)

- `Mana.addSpentListener((player, amount, source) -> ...)` hears of every spend that takes more than none.
- Every ability's mana goes through `Mana.spend(player, cost, source)`:
  - the click and sneak uses (`Activations.use`)
  - the extra costs (`Hits.takeMana`)
  - deployables
  - Spirit Glide
  - Jingle Bells' share
- `amount` is what was actually taken (never more than they had).
- `Mana.get(player)` returns their mana, full until the pool is known.

### Vitality spent: `Vitality.addSpentListener` (hook 5)

`(player, amount) -> ...` fires after every `Vitality.spend`, which every Vitality cost already went
through. Wither Shield's refund is not a spend.

### Absorption: `session/Absorption` (hook 17)

- **Giving it:** `Absorption.give(player, source, amount, millis)` replaces what that source gave before.
  Different sources add up (UNKNOWN whether Hypixel's stack).
- **Queries:** `get(player)`, `left(player, source)`, `remove(player, source)`, `clear(player)`.
- **SkyBlock hits:** absorption takes a hit before health: after Defense, the factors and the shields
  (immunity, veils), in `PlayerDamage.hit`.
  - The hit is still a hit: its number, flinch and knockback happen, and the hurt listeners hear the whole
    amount.
  - The source that runs out soonest goes first (UNKNOWN).
  - Last Stand's saves from death count absorption too.
- **Vanilla damage** (`HealthListener`) is absorbed the same way, except the void and /kill.
- **Action bar:** health and absorption together in gold, over the max ("§66,171/4,422❤", the SkyHanni mod's
  action bar patterns).
- **Respawn** clears it.
- **Not built:** the vanilla gold hearts (UNKNOWN whether Hypixel shows them, and they'd take vanilla damage
  themselves).
- The wiki's Absorption says it's "affected by Defense". Here it takes what's left after Defense.

### Vanilla damage by cause: `combat/VanillaDamage` (hook 8)

- `VanillaDamage.addFactor((player, cause) -> factor)`:
  - It's a factor on a vanilla hit of that cause: fall, fire, lava, explosion, projectile, drowning,
    suffocation, contact, magic, poison, wither, void, other (`VanillaDamage.Cause.of(DamageCause)`).
  - The factors multiply. The void and /kill are never changed.
  - `HealthListener.onCause` applies them to the event at NORMAL priority, so set bonus immunity and Last
    Stand's saves (HIGH) and the health taken (HIGHEST) all see the same amount.
  - A factor must be deterministic: it's asked for each hit.
- **To cancel a cause** outright (Creeper Hat's explosions), use a set bonus's `immune`.
- **Fall damage:** the wiki's "Fall Damage" and "Damage" pages don't settle SkyBlock's rule. They give only
  "Damage received when falling from a great height", and "Differs" for fire and lava. So vanilla's amounts
  stay, 1:1 as before (UNKNOWN).
- **Feather Falling's extra safe blocks:** these can go through Paper's `SAFE_FALL_DISTANCE` attribute (the
  armor inventory's note).

### Combat state: `combat/CombatState` (hook 9)

- `inCombat(player)` is true within `WINDOW_MILLIS` of the player dealing a mob damage or taking a mob's hit.
- `inCombat(player, window)` checks against a window the effect gives.
- `sinceCombat(player)` returns the time since they were last in combat.
- **What counts as dealt:** any damage through the mob sinks (`Mobs.damage`, `DungeonMobs.damage`).
- **What counts as taken:** a `PlayerDamage.hit` with a mob behind it that took health. Traps and vanilla
  damage don't count (UNKNOWN).
- **The window is UNKNOWN: 5 s.** The wiki's Respite, Health Regen, Mana, Soulflow and Combat pages say only
  "in combat". The Soulflow items' "per 5s in combat" is the one number near it.

### Timed percent buffs: `PlayerSession.buffPercent`

`buffPercent(source, stat, percent, millis)` gives a share more of a stat for a while (Last Stand's "+12.5%
Defense for 10s"). A new buff from the same source replaces the old one. It applies on the stat as the rest
(flat buffs included) makes it. Several on one stat add up (UNKNOWN). `PlayerSession.buff` still adds flat stats.

## Abilities

### Every activation, and how each is used (hook 7, F1)

`AbilityActivation` now has every activation in items.json. `ActivationsTest` reads the private items.json and
checks them all. None is dropped when items load any more.

| Activation | Used on | Dispatch |
|---|---|---|
| `LEFT_CLICK`, `RIGHT_CLICK`, `SHIFT_*` | a click | as before (`Abilities.forClick`) |
| `LEFT_RIGHT_CLICK`, `CLICK` | either click | `forClick`; `Trigger.right` says which |
| `HOLD_RIGHT_CLICK` | a right click; held down, the client repeats it every 4 ticks, each a use | `forClick` |
| `DIG` | a left click on a block | `PlayerListener.onAbilityUse`, before the clicks; `Trigger.block` |
| `SNEAK` | starting to sneak | `Activations.onSneak` (see below) |
| `ON_SHOOT` | a drawn bow's shot | `Activations`, through `Shots.addShotListener`; `Trigger.projectile` is the arrow |

- **Handlers** register by ability name as before (`WeaponAbilities.register`, `UtilityAbilities.register`).
- **SNEAK dispatch:** it takes the held item's SNEAK abilities, then the worn pieces', from
  `SetBonuses.worn`: armor helmet first, then the equipment. Each ability name is used once per sneak, so a
  full set's four "Homing Missiles" blocks make one use. The handler gets the item and data of the piece
  that carries the ability.
  - Armor, equipment and accessories held in the hand don't use theirs (`Activations.fromHand`: the items
    whose stats don't count in the hand either). Every SNEAK ability in the data is on armor, so it's worn.
  - It checks a full-set requirement itself ("When wearing the full set": `SetBonuses.worn`).
  - Letting go of sneak is its own to watch, if it needs that (To the Moon!'s charge): use
    `PlayerToggleSneakEvent` or `Bonus.sneaked`.
  - The dead and dungeon ghosts don't use them.
- **`AbilityHandler.Trigger`** is `(activation, right, projectile, block)`. It reaches
  `use(player, item, tag, block, vitalityPaid, trigger)` and `usable(player, item, tag, block, trigger)`. Both
  default to the old forms, so existing handlers are unchanged.
- **Payment: `Activations.use(player, item, tag, block, trigger)`**, which every use goes through. It was
  `PlayerListener.useAbility`. The order is: cooldown, mana, Vitality, health, `usable`. Then it starts the
  cooldown, takes the mana, Vitality and health, and calls `use`, then the Power Scroll.

### Ability costs (hooks 6, 13, 14)

- **`Abilities.addItemManaCostFactor((player, itemData) -> factor)`**: an item's own factor (Ultimate Wise:
  1 − 0.1 × level). It multiplies with the rest (the wiki's Wise Dragon Armor).
  - `manaCost(block, max, player, tag)` counts it.
  - Spirit Glide and `Hits.enoughMana` now pass the item too.
  - The lore's "Mana Cost" line still shows the block's cost. Changing it is the owning effect's job.
- **`Abilities.addCooldownFactor((player, block) -> factor)`**: a factor on the block's cooldown (the Mage's
  cooldown reduction). The factors multiply (UNKNOWN whether Hypixel's add).
  - `Abilities.cooldownMillis(block, player)` works it out.
  - The use path and Spirit Glide use it.
- **Health costs are charged now**, with the other costs. Before this, only three handlers charged their own.
  - The cost is `AbilityHandler.healthCost(player, item, tag, block)`: the block's by default, or a share of
    max health where the text says so. `Abilities.addHealthCostFactor(player -> factor)` factors multiply it
    (Berserker Armor's cut).
  - It can't take the last of their health (`Abilities.canPayHealth(health, cost)`: the wiki's Flower of
    Truth), and nothing is said when it can't be paid.
  - Bad Health, Explode, Heat-Seeking Rose and Petal Barrage no longer charge their own.

## Skills, experience, items

### Wisdom for every skill: `SkillGains.give` (hook 15)

- `give(player, skill, xp)` multiplies by the skill's own Wisdom: `SkillGains.wisdom(skill)` gives Combat
  Wisdom for Combat, Enchanting Wisdom for Enchanting, and so on for all 12. The rule is
  `withWisdom(xp, wisdom)` = xp × (1 + Wisdom / 100) (the wiki's Wisdom).
  - Kills: Champion's bonus is on the base, then Wisdom.
  - The Hex's Enchanting XP and crafting's Carpentry XP get their Wisdom now.
- `giveFlat(player, skill, xp)` gives XP with no Wisdom. A collection tier's reward XP goes through it, as
  before (UNKNOWN whether Hypixel's gets Wisdom).

### Experience orbs: `economy/ExpOrbs` (hook 10)

- **Kills:** each `MobKind.Variant` has its wiki page's `orbs` (`.orbs(n)` after `.variant(...)`), and
  `SkyBlockMob.getOrbs()` reads it.
  - Entrance mobs, the Watcher's undeads and secret bats: 30.
  - Lost Adventurer: 100 (Lv80) and 125 (Lv90).
  - Angry Archaeologist: 100, 115 and 130.
  - The Hub's Magma Cube: 200. Bladesoul: 5,000. Bosses give none here, as they give no drops.
- **How kill experience arrives:** it follows the plugin's rule for items. A dungeon mob's goes straight to
  the killer (`giveExp`), as its drops go to the inventory. Any other mob's is an orb where it died, as its
  drops land there. See the wiki's Experience: sources "drop experience orbs, which may be collected in the
  same manner as Coins".
- **`ExpOrbs.grant(player, base, source, at, direct)`** is the helper for other sources (Mithril mining,
  Champion's "+25 exp orbs"). Source is `MOB`, `ORE` or `OTHER`.
- **`ExpOrbs.addBonus((player, source) -> percent)`**: bonuses in percent, which add up ("stacking additively",
  the wiki's Experience). The Experience enchantment's chance "to drop double experience" is +100 when it
  rolls. How Hypixel rounds is UNKNOWN; here it's to the nearest.

### Per-item counters: `item/ItemCounters` (F3)

- Counts live in the item's data, under a key of the effect's own: `get(tag, key)` and `add(tag, key, amount)`,
  never below 0.
- `add(stack, holder, key, amount)`, `addHeld(player, key, amount)` and `addWorn(player, slot, key, amount)`
  rebuild the item for its holder, so a lore line from `ItemBehaviour.lore` follows. They don't count while
  the player's inventory is being handed to another server.
- Nothing is written until something counts, so new items and the golden test are untouched.
- Counting on equipment (kept in storage) is LATER: `Equipment` has no way to put a piece back.

### Held items that know their holder: `ItemBehaviour.whileHeld(Player, NBTTagCompound, Stats)` (F4)

It's called with the holder and the item's data (for stats by Catacombs level: `ItemBuilder.catacombsLevel`).
By default it calls the old `whileHeld(Stats)`.

## Examples the core built

Each example is the test that its hook works. The owning part marks the row done in its status document:

| Example | Hook | Owner's doc |
|---|---|---|
| Ice Spray's Frozen: "take 10% more damage from all sources" (the wiki), now for melee hits and arrows too, as a `TAKEN` debuff | mob debuffs | ABILITIES_WEAPONS.md |
| Wither Shield's absorption, "(12 + Cata × 0.32) × 50". Its Vitality comes back 5 s after each cast, by the share of its absorption left when it ended: then, or when a new cast took its place (UNKNOWN how Hypixel works it out) | absorption | ABILITIES_WEAPONS.md |
| Enrage's health cost, 10% of max health (the wiki's Enrager; the data's 15.6 isn't health, and Life Blood's is also 15.6 though its text says 10%). Life Blood's 10% is charged the same way | health-cost charging | ABILITIES_UTILITY.md |
| Enchanting and Carpentry Wisdom (and Mining Wisdom, once Mithril gives Mining XP) | per-skill Wisdom | STATS_EFFECTS.md |
| Experience orbs from kills | XP orbs | STATS_EFFECTS.md (Experience, Champion's orbs build on it) |

The docs these touched are updated: ABILITIES_WEAPONS.md, ABILITIES_UTILITY.md and BONUSES.md's
Absorption and Berserk rows.

## Who owns what

Every effect below is built by its part, on the hooks above. A part that needs a hook that's missing adds the
smallest version in its own files and says so.

| Part | Status doc | Owns | Hooks it mostly uses |
|---|---|---|---|
| Weapon enchantments | ENCHANTS_WEAPONS.md | Every sword, longsword, bow, gauntlet and fishing-weapon enchantment the inventory marks TODO-NOW (see the list after this table). Also the weapons' reforge bonus effects (REFORGES.md "Later": Fabled, Suspicious, Fanged, Coldfused, Loving, Spiritual, Hyper, Empowered, ...). | hit listeners, landing buffs (with `added`), debuffs and damage over time, the killing blow, `Shots`, item counters, the item mana factor, `ExpOrbs.grant`, the Attack Speed cap, `MobHits.deal` |
| Armor enchantments | ENCHANTS_ARMOR.md | Every armor and equipment enchantment the inventory marks TODO-NOW (see the list after this table). Also armor reforge bonuses (Renowned, Perfect, ...). | `PlayerDamage` (hurt listeners, Defense against, shields), `VanillaDamage`, the Mana and Vitality spent listeners, `buffPercent`, `CombatState`, `ExpOrbs`, item counters |
| Drops, XP, mining and stats | STATS_EFFECTS.md | Looting, Luck, Chance (in `Mobs.drop` and `MobDrop`: the core added no drop hook), Experience (an `ExpOrbs` bonus), Efficiency, Compact, Flowstate and Aqua Affinity on Mithril, Mining XP and Mining Wisdom, Mithril Powder, and the other TODO-NOW stats. Champion's coins and orbs are the weapon part's. | per-skill Wisdom, `ExpOrbs`, item counters |
| Item abilities | ABILITIES_WEAPONS.md, ABILITIES_UTILITY.md | The TODO-NOW ABILITY blocks and item passives (see the list after this table). | the activations and `Trigger`, `Activations.use`, hit listeners, landing buffs, `Absorption`, debuffs, `Shots.scale`, item counters, `whileHeld` with the holder, the player hit listener |
| Armor bonuses and accessories | BONUSES.md, ACCESSORIES.md | The TODO-NOW set, piece and tiered bonuses (see the list after this table) and the ~50 buildable accessory effects (inv_systems.md "Accessories"). | `Bonus` hooks, the Attack Speed cap, health cost and cooldown factors, the Ferocity strike kind, `Absorption`, `CombatState`, item counters, `ExpOrbs` |

What each part owns in full:

- **Weapon enchantments.**
  - Cleave, Life Steal, Mana Steal and Drain (syphon).
  - Thunderlord, Thunderbolt, Lethality, Venomous, Fire Aspect, Knockback, Vampirism and Tabasco.
  - Champion's coins and orbs.
  - Bow enchantments: Flame, Punch, Piercing, Infinite Quiver (arrow saving) and Toxophilite.
  - Ultimates: Inferno, Fatal Tempo, Combo, Soul Eater, Swarm, Rend, Duplex, Ultimate Wise and Ultimate Jerry.
  - Impaling and Magmarizer stay LATER: no Aquatic or Magmatic mobs.
- **Armor enchantments.**
  - Stats and defense: Small Brain, Projectile, Blast and Fire Protection, and Feather Falling.
  - Hits taken: Thorns, Reflection, Counter-Strike and Last Stand.
  - Regen and movement: Respite, Transylvanian, Depth Strider and Stealth.
  - Spent Vitality and mana: Hardened, Strong and Vivacious Vitality, Vampiric Vitality and Refrigerate.
  - Stats from other things: Cayenne, The One, Quantum, Wisdom and Legion.
  - Also No Pain No Gain, Habanero Tactics' first three lines, and Hecatomb.
  - Bank waits for purse loss on death (see "Questions for the owner").
- **Item abilities.**
  - SNEAK and worn: Ground Pound, Homing Missiles, Water Burst, To the Moon!, Bouncy, Eye Beam and Double
    Jump.
  - Weapon passives: Cleave (the Cleavers' ability), Love Tap, Stinger, Angered, Chain of Agony, and the
    mob-type multipliers.
  - Counters: Commander Whip, Tempest, Growth and Stored Potential.
  - Bows: Triple Shot, Explosive Shot, Arrow Infusion, Extreme Focus and Sting.
  - Dungeon items: the class and ghost items, Echolocation and Dungeon Breaker.
  - Also Hollow Spirit, Tuning 4 Dummies, and the Necron's Blades' stats by level.
- **Armor bonuses.**
  - Intimidate, Golem Armor's Absorption, Bouncing Arrow, Dwarf Wannabe and True Dwarf.
  - Newton's Demise, Refraction, Regenerative Howl, Starpower, Training and Arcane Energy.
  - Spirit, Berserk's health-cost cut, Rekindle, Riches and Shimmer.
  - The Spider and Zombie Bulwarks, and Magnetic.

## Not in the core

- **Purse loss on death** (inv_systems.md Table 3, #11) changes a game rule, so it's left for the owner.
  It blocks the Bank enchantment.
- **Drop modifiers** (Looting's and Luck's chances) are the drops part's to add in `Mobs.drop` and `MobDrop`.
- **Enchanting-level gating** ("will be greyed out and will not work", armor inventory X1) is left to the
  enchantment parts. It changes lore and the golden test.
- **Counters on equipment pieces**, the vanilla absorption hearts, and SkyBlock's own fall and fire formulas
  (UNKNOWN).

## UNKNOWN (and the reading taken)

- Whether effects' own damage sets off on-hit effects or Ferocity: it doesn't.
- How debuffs from several players stack: per mob; use a per-player source to keep them apart.
- Whether "takes more" factors add or multiply: they multiply.
- How mobs that steer themselves are slowed: they aren't.
- The out-of-combat window: 5 s.
- Whether traps and vanilla damage put a player in combat: they don't.
- The order absorption sources are used in: soonest to run out first.
- Whether absorption sources stack: different sources add up.
- Whether percent buffs on one stat add: they add.
- Whether cooldown and health-cost factors add: they multiply.
- Whether Attack Speed cap raises add: the most counts.
- How an item mana factor and an orb bonus round: to the nearest.
- SkyBlock's fall damage: vanilla's amount stays.
- DIG: a left click on a block, not per block broken.
- Whether a collection reward's XP gets Wisdom: it doesn't.
- How Wither Shield's Vitality refund is worked out: 5 s after each cast, by the share of its absorption left
  when it ended (a new cast ends the one before).
- Class dungeon abilities (Seismic Wave and others) deal `OTHER` damage, not `ABILITY`: UNKNOWN whether
  Hypixel counts them as ability hits.
