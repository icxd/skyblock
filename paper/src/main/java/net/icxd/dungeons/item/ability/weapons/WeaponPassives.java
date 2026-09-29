package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.Debuffs;
import net.icxd.dungeons.combat.HitKind;
import net.icxd.dungeons.combat.MobDebuffs;
import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunItems;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.item.ability.utility.Heals;
import net.icxd.dungeons.item.behaviour.HeldStats;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.mining.BlockListener;
import net.icxd.dungeons.mob.KillingBlow;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.SkillGains;
import net.icxd.dungeons.user.User;

/**
 * Weapons' passives: what a weapon does on hits and kills beyond its stats and its click abilities
 * (ABILITIES_WEAPONS.md, "Weapon passives"), on the effect hooks (EFFECTS.md). What its own text says
 * ({@link WeaponLore}: "Deals +100% damage to ༕ Undead mobs", "Heal 10❤ per hit") and what its passive ABILITY
 * blocks say (the Cleavers' Cleave, the Zombie Soldier Cutlass's Love Tap, the Sting's Stinger, the Tormentor's
 * Angered): each weapon's text is read once. The weapon is the one the hit was dealt with (the held one, or the bow
 * an arrow left). Its counts (the Fel Sword's kills, the Hurricane Bow's, the Zombie Commander Whip's zombies of a
 * run, the Promising Pickaxe's blocks) are kept on the item ({@link ItemCounters}), and the stats they give are its
 * behaviour's (see HeldStats). Its bows' part is {@link BowPassives}'. Registered by Dungeons. Main thread.
 */
public final class WeaponPassives implements Listener {
    /** The Flaming Sword's fire: Fire Aspect's damage over time (its source), a second's 3% of the hit (Fire Aspect I's; UNKNOWN). */
    static final String FIRE = "fire";
    static final double IGNITE_SHARE = 0.03;
    /** A Cleaver's share of its hit on the mobs around what it hit: "typically ranges from 40% to 50%", less further off (the wiki's Cleaver). */
    static final double CLEAVE_NEAR = 0.5;
    static final double CLEAVE_FAR = 0.4;
    /** "Heals you for +10❤ Health when you hit an entity while in Dungeons!" */
    private static final Pattern LOVE_TAP = Pattern.compile("Heals you for \\+([\\d.]+)❤");
    /** "You take +50% damage while holding this weapon." */
    private static final Pattern ANGERED = Pattern.compile("You take \\+([\\d.]+)% damage while holding");
    /** The Stone Blade's Mage line: "melee attacks restore 25% additional mana". */
    private static final Pattern MAGE_MANA = Pattern.compile("melee attacks restore ([\\d.]+)% additional mana");
    /** The Stone Blade's Archer line: "Your melee attacks cause enemies to take 10% more damage from your arrows for 5 seconds". */
    private static final Pattern ARCHER_MARK = Pattern.compile("cause enemies to take ([\\d.]+)% more damage from your arrows for ([\\d.]+) seconds");

    /** Triple Shot: "Shoots 3 arrows at a time! The 2 extra arrows deal 40% of the damage and home to targets." */
    private static final Pattern EXTRA_ARROWS = Pattern.compile("The (\\d+) extra arrows deal ([\\d.]+)% of the damage");

    /** What each weapon does, by item id: read from its text once (null for an id that isn't an item). */
    private static final Map<String, Weapon> WEAPONS = new HashMap<>();
    /** The Stone Blade's Archer marks: by player, the mobs their melee hit marked, until when, and the factor. */
    private static final Map<UUID, Map<UUID, double[]>> MARKED = new HashMap<>();

    /**
     * A weapon as its text has it: its lore's passives, and its passive blocks' numbers: Cleave's range ("monsters
     * in a 3 block range"), Love Tap's heal, whether it's the Sting's always-crit, the Tormentor's Angered, the Stone
     * Blade's Mage and Archer extras.
     */
    static final class Weapon {
        final SkyBlockItem item;
        final WeaponLore lore;
        double cleaveRadius;
        double loveTap;
        boolean stinger;
        double angered;
        double mageMana;
        double archerMark;
        long archerMillis;
        /** Bows (see {@link BowPassives}): Triple Shot's extra arrows and their share, Tempest, Explosive Shot, Sting, the Crypt Bow's skulls, the Sniper Bow's range. */
        int extraArrows;
        double extraShare = 1;
        boolean tempest;
        boolean explosive;
        ItemBlock sting;
        boolean skulls;
        boolean further;

        Weapon(SkyBlockItem item) {
            this.item = item;
            this.lore = WeaponLore.of(item.lore());
            for (ItemBlock block : item.blocks()) {
                if (!block.isAbility()) continue;
                String plain = AbilityText.plain(block.text());
                switch (block.name()) {
                    case "Cleave" -> cleaveRadius = AbilityText.blocks(plain).orElse(3);
                    case "Love Tap" -> loveTap = number(LOVE_TAP, plain);
                    case "Stinger" -> stinger = true;
                    case "Angered" -> angered = number(ANGERED, plain) / 100;
                    case "Triple Shot" -> {
                        Matcher m = EXTRA_ARROWS.matcher(plain);
                        if (m.find()) {
                            extraArrows = Integer.parseInt(m.group(1));
                            extraShare = Double.parseDouble(m.group(2)) / 100;
                        }
                    }
                    case "Tempest" -> tempest = true;
                    case "Explosive Shot" -> explosive = true;
                    case "Sting" -> sting = block;
                    default -> {
                    }
                }
            }
            String plain = AbilityText.plain(item.lore());
            skulls = plain.contains("Replaces the arrows that you shoot with exploding wither skulls");
            further = plain.contains("Allows you to shoot arrows much further");
            mageMana = number(MAGE_MANA, plain) / 100;
            Matcher m = ARCHER_MARK.matcher(plain);
            if (m.find()) {
                archerMark = 1 + Double.parseDouble(m.group(1)) / 100;
                archerMillis = (long) (Double.parseDouble(m.group(2)) * 1000);
            }
        }

        String id() {
            return item.id();
        }
    }

    public WeaponPassives() {
        Combat.addHitBuffs((Combat.LandingBuffs) WeaponPassives::buff);
        Combat.addHitListener(WeaponPassives::landed);
        Combat.addAlwaysCrits((player, tag) -> {
            Weapon weapon = weapon(tag);
            return weapon != null && weapon.stinger;
        });
        PlayerDamage.addTakenMultiplier(WeaponPassives::angered);
        PlayerDamage.addTakenFrom(WeaponPassives::takenFrom);
        // A stunned mob's hits do nothing (the Stun Potion's).
        PlayerDamage.addTakenFrom((player, by) -> Hits.stunned(PlayerDamage.attacker(by)) ? 0 : 1);
        SkillGains.addKillWisdom(WeaponPassives::killWisdom);
        BlockListener.addBrokenListener((player, block) -> {
            Weapon held = held(player);
            if (held != null && "PROMISING_PICKAXE".equals(held.id())) ItemCounters.addHeld(player, HeldStats.PROMISING_PICKAXE_BLOCKS, 1);
        });
        BowPassives.register();
        Tormentor.register();
        WornStrikes.register();
        GorillaTactics.register();
    }

    private static double number(Pattern pattern, String plain) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? Double.parseDouble(m.group(1).replace(",", "")) : 0;
    }

    /** What the weapon with this data does (see {@link Weapon}); null for none, or for what isn't a SkyBlock item. */
    static Weapon weapon(NBTTagCompound tag) {
        if (tag == null) return null;
        String id = tag.getString("id");
        if (WEAPONS.containsKey(id)) return WEAPONS.get(id);
        SkyBlockItem item = ItemRegistry.get(id);
        Weapon weapon = item == null ? null : new Weapon(item);
        WEAPONS.put(id, weapon);
        return weapon;
    }

    /** What they hold in their main hand does; null for nothing that's a SkyBlock item. */
    static Weapon held(Player player) {
        return weapon(ItemNBT.read(player.getInventory().getItemInMainHand()));
    }

    // ---------- hits ----------

    /**
     * A melee hit or arrow as it lands: the weapon's factors against the mob's types, from missing health, in water,
     * on a mob in lava, on a crit from behind (the Livid Dagger), and a Stone Blade's Archer mark on the mob for an
     * arrow; then the bows' own ({@link BowPassives#factor}) and a waiting Extreme Focus ({@link BowPassives#focus}).
     */
    private static Combat.HitBuff buff(Player player, Damage.Attacker attacker, Damage.Target target, Combat.Landing landing) {
        double factor = 1;
        Weapon weapon = weapon(landing.weapon());
        if (weapon != null && weapon.lore.onHit()) {
            WeaponLore lore = weapon.lore;
            factor *= lore.factor(target.types(), missing(player), player.isInWater());
            if (lore.strengthInLava > 0 && landing.entity().isInLava()) factor *= WeaponLore.strengthFactor(attacker.strength(), lore.strengthInLava);
            if (lore.behindCrit != 1 && landing.critical() && behind(player.getLocation(), landing.entity().getLocation())) factor *= lore.behindCrit;
        }
        if (landing.kind() == HitKind.ARROW) factor *= marked(player, landing.entity()) * BowPassives.factor(landing, target);
        double added = BowPassives.focus(player);
        return factor == 1 && added == 0 ? null : new Combat.HitBuff(0, factor, added);
    }

    /** The share of their max health they're missing (0 to 1). */
    static double missing(Player player) {
        double max = PlayerHealth.max(player);
        return max <= 0 ? 0 : Math.max(0, Math.min(1, 1 - PlayerHealth.get(player) / max));
    }

    /**
     * Whether {@code from} is behind someone at {@code at} (the Livid Dagger's "if you are behind your target"): in
     * the half of the ground plane their back faces (UNKNOWN how wide Hypixel's is).
     */
    static boolean behind(Location from, Location at) {
        return behind(at.getYaw(), from.getX() - at.getX(), from.getZ() - at.getZ());
    }

    /** The same from a target's yaw (Minecraft's: 0 faces +z) and the offset to who's hitting it. */
    static boolean behind(float yaw, double dx, double dz) {
        double radians = Math.toRadians(yaw);
        double facingX = -Math.sin(radians);
        double facingZ = Math.cos(radians);
        return facingX * dx + facingZ * dz < 0;
    }

    /** After a melee hit or arrow has done its damage: the melee passives, then the bows' (see {@link BowPassives#landed}). */
    private static void landed(Player player, Combat.Landing landing, Damage.Target target, double damage, boolean killed) {
        Weapon weapon = weapon(landing.weapon());
        if (weapon == null) return;
        if (landing.kind() == HitKind.MELEE) {
            melee(player, weapon, landing, damage);
            Tormentor.landed(player, weapon, landing, damage);
        } else if (landing.kind() == HitKind.ARROW) {
            BowPassives.landed(player, weapon, landing, damage);
        }
    }

    /**
     * A melee hit's passives: "Heal 10❤ per hit", "Regens 3 Mana on hit", Love Tap in a run, Cleave, and the Stone
     * Blade's Mage mana and Archer mark in a run. Heals are {@link Heals#give}'s (with the Catacombs boost in a run).
     * UNKNOWN whether Ferocity strikes and abilities' hits count as "hits" for these: only melee hits do.
     */
    private static void melee(Player player, Weapon weapon, Combat.Landing landing, double damage) {
        WeaponLore lore = weapon.lore;
        if (lore.igniteSeconds > 0) ignite(player, landing, lore.igniteSeconds);
        if (lore.healPerHit > 0) Heals.give(player, player, lore.healPerHit);
        if (lore.manaPerHit > 0) giveMana(player, lore.manaPerHit);
        if (weapon.loveTap > 0 && RunManager.inRun(player)) Heals.give(player, player, weapon.loveTap);
        if (weapon.cleaveRadius > 0) cleave(player, landing, damage, weapon.cleaveRadius);
        if (weapon.mageMana > 0 || weapon.archerMark > 0) {
            DungeonClass playing = playing(player);
            if (playing == DungeonClass.MAGE && weapon.mageMana > 0) giveMana(player, weapon.mageMana * Damage.manaOnHit(PlayerSession.of(player).maxMana()));
            if (playing == DungeonClass.ARCHER && weapon.archerMark > 0) mark(player, landing.entity(), weapon.archerMark, weapon.archerMillis);
        }
    }

    /**
     * The Flaming Sword's "Ignites enemies for 3s.": the mob burns (fire's look, and the fire damage over time Fire
     * Aspect's is, "fire", so the two don't burn it twice), a second at a time for the text's seconds. What a second
     * deals is UNKNOWN (the text gives only the time): Fire Aspect I's 3% of the hit's damage after additive buffs,
     * through the mob's Defense.
     */
    private static void ignite(Player player, Combat.Landing landing, int seconds) {
        LivingEntity mob = landing.entity();
        if (!MobHits.alive(mob)) return;
        Damage.Target target = MobHits.target(mob);
        if (target == null) return;
        Damage.Attacker attacker = Combat.attacker(player, landing.weapon(), false, 0);
        double postAdditive = Damage.initial(attacker.damage(), attacker.strength())
                * (landing.critical() ? Damage.critMultiplier(attacker.critDamage()) : 1) * (1 + Damage.additive(attacker, target) / 100);
        double tick = postAdditive * IGNITE_SHARE * Damage.defenseMultiplier(target.defense());
        mob.setFireTicks(seconds * 20);
        MobDebuffs.dot(mob, FIRE, player, tick, 20, seconds, DamageIndicators.Look.FIRE);
    }

    /** The class they play in the run they're in; null outside one. */
    static DungeonClass playing(Player player) {
        if (!RunManager.inRun(player)) return null;
        User user = User.ifLoaded(player.getUniqueId());
        return user == null ? null : DungeonProfile.selectedClass(user);
    }

    /** Mana back, never past their pool (whole points: the pool is whole). */
    static void giveMana(Player player, double amount) {
        PlayerSession session = PlayerSession.of(player);
        int pool = session.maxMana();
        int mana = session.getMana() < 0 ? pool : session.getMana();
        session.setMana(Math.min(pool, mana + (int) amount));
    }

    /**
     * Cleave: "When hitting an entity, monsters in a 3 block range will be hit for a portion of that damage too":
     * each other mob within the range of what was hit takes 50% of the hit's damage next to it down to 40% at the
     * edge (the wiki's "typically ranges from 40% to 50% ... as the distance increases the damage decreases"; the
     * line between is UNKNOWN), as an effect's damage (no hit listeners, no Ferocity). A crit's hit is bigger, so
     * its share is too ("Critical strikes do increase the damage dealt to adjacent mobs").
     */
    private static void cleave(Player player, Combat.Landing landing, double damage, double radius) {
        LivingEntity hit = landing.entity();
        Location at = hit.getLocation();
        for (LivingEntity other : Hits.near(at, radius)) {
            if (other.equals(hit)) continue;
            double share = cleaveShare(other.getLocation().distance(at), radius);
            MobHits.deal(player, other, Math.floor(damage * share), DamageIndicators.Look.NORMAL, HitKind.OTHER, landing.weapon());
        }
    }

    /** A Cleaver's share at {@code distance} from what it hit, of {@code radius}: 50% next to it, 40% at the edge. */
    static double cleaveShare(double distance, double radius) {
        double t = radius <= 0 ? 1 : Math.max(0, Math.min(1, distance / radius));
        return CLEAVE_NEAR + (CLEAVE_FAR - CLEAVE_NEAR) * t;
    }

    /** The Stone Blade's Archer mark: their arrows on this mob are times {@code factor} for {@code millis}. */
    private static void mark(Player player, LivingEntity mob, double factor, long millis) {
        MARKED.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .put(mob.getUniqueId(), new double[] {System.currentTimeMillis() + millis, factor});
    }

    private static double marked(Player player, LivingEntity mob) {
        Map<UUID, double[]> marks = MARKED.get(player.getUniqueId());
        if (marks == null) return 1;
        double[] mark = marks.get(mob.getUniqueId());
        if (mark == null) return 1;
        if (mark[0] > System.currentTimeMillis()) return mark[1];
        marks.remove(mob.getUniqueId());
        return 1;
    }

    // ---------- hits on them ----------

    /** The Tormentor's Angered: "You take +50% damage while holding this weapon." */
    private static double angered(Player player) {
        Weapon held = held(player);
        return held == null ? 1 : 1 + held.angered;
    }

    /** "Receive -20% damage from ☮ Animal mobs" while it's held. */
    private static double takenFrom(Player player, Entity by) {
        Weapon held = held(player);
        if (held == null || held.lore.animalTaken == 1) return 1;
        return types(PlayerDamage.attacker(by)).contains(MobType.ANIMAL) ? held.lore.animalTaken : 1;
    }

    /** The mob types of one of SkyBlock's mobs (none for anything else). */
    static Set<MobType> types(Entity entity) {
        Mobs.Live live = Mobs.of(entity);
        if (live != null) return live.type().getTypes();
        DungeonMobs.Mob mob = DungeonMobs.of(entity);
        return mob == null ? Set.of() : mob.types();
    }

    // ---------- kills ----------

    /** "Gain +45☯ Combat Wisdom against ༕ Undead mobs", for the weapon they hold when they kill one. */
    private static double killWisdom(Player killer, SkyBlockMobDeathEvent event) {
        Weapon held = held(killer);
        return held == null || held.lore.wisdom.isEmpty() ? 0 : held.lore.wisdomAgainst(Set.copyOf(event.kind().types()));
    }

    /**
     * A kill: the Fel Sword's and the Hurricane Bow's kills with them, and the Zombie Commander Whip's zombies in a run
     * ("Every 1 Zombies killed during a dungeon run by this weapon"). Each is counted on the item that dealt the killing
     * blow, if they still hold it (the one with the same data); UNKNOWN whether Hypixel counts a kill made with an item
     * put away since (an arrow still flying): it doesn't here.
     */
    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        WornStrikes.killHappened();
        Player killer = event.killer();
        KillingBlow blow = event.killingBlow();
        if (killer == null || !killer.isOnline() || blow == null || blow.weapon() == null) return;
        String id = blow.weapon().getString("id");
        if (!holding(killer, blow.weapon())) return;
        switch (id) {
            case "FEL_SWORD" -> ItemCounters.addHeld(killer, HeldStats.FEL_SWORD_KILLS, 1);
            case "HURRICANE_BOW" -> ItemCounters.addHeld(killer, HeldStats.HURRICANE_KILLS, 1);
            case "ZOMBIE_COMMANDER_WHIP" -> {
                if (zombie(event.kind())) countForRun(killer);
            }
            default -> {
            }
        }
    }

    /** Whether they hold the item with this data now: the same one (its uuid, where it has one), not another of its kind. */
    private static boolean holding(Player player, NBTTagCompound tag) {
        NBTTagCompound held = ItemNBT.read(player.getInventory().getItemInMainHand());
        if (held == null || !held.getString("id").equals(tag.getString("id"))) return false;
        return !tag.hasKey("uuid") || tag.getString("uuid").equals(held.getString("uuid"));
    }

    /**
     * Whether a kind of mob is a "Zombie" for the Zombie Commander Whip: a zombie (its entity) or one named one (the
     * Zombie Grunt, Tank Zombie and Crypt Lurker are zombies). UNKNOWN which mobs Hypixel counts: the same question is
     * open for the Zombie Hat and Training (BONUSES.md).
     */
    static boolean zombie(MobKind kind) {
        return kind != null && (kind.entityType() == EntityType.ZOMBIE || kind.name().contains("Zombie"));
    }

    /** One more zombie on the whip they hold, for the run they're in: a new run's count starts again from none. */
    private static void countForRun(Player player) {
        String run = RunItems.runKey(player);
        if (run == null || InventorySyncListener.frozen(player)) return;
        ItemStack stack = player.getInventory().getItemInMainHand();
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null) return;
        if (!run.equals(tag.getString(HeldStats.COMMANDER_WHIP_RUN))) {
            tag.setString(HeldStats.COMMANDER_WHIP_RUN, run);
            tag.setDouble(HeldStats.COMMANDER_WHIP_ZOMBIES, 0);
        }
        ItemCounters.add(tag, HeldStats.COMMANDER_WHIP_ZOMBIES, 1);
        player.getInventory().setItemInMainHand(ItemBuilder.build(item, tag, stack.getAmount(), player));
    }

    // ---------- the class ability ----------

    /**
     * "Right-click to use your class ability!": a right click with Necron's Blade (or a sword made from it) without
     * scrolls, the Stone Blade, the Earth Shard or the Fel Sword uses their class's ability in a run, as the Dungeon
     * Orb does (see RunItems); outside a run, nothing. After the click's own ability (none of them has one then).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClassAbility(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        NBTTagCompound tag = ItemNBT.read(event.getPlayer().getInventory().getItemInMainHand());
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null || !RunItems.usesClassAbility(item, tag)) return;
        RunItems.classAbility(event.getPlayer());
    }

    /**
     * A dungeon ghost's right click: its ghost abilities (see GhostAbilities). The run calls a ghost's clicks off
     * before anything else sees them, so this hears them anyway.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onGhostClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        GhostAbilities.clicked(event.getPlayer());
    }

    /** What's kept about them goes with them. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        MARKED.remove(player);
        BowPassives.forget(player);
        Tormentor.forget(player);
        WornStrikes.forget(player);
        GorillaTactics.forget(player);
        ArmorAbilities.EyeBeam.forget(player);
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == Dungeons.getInstance()) WornStrikes.removeAll();
    }
}
