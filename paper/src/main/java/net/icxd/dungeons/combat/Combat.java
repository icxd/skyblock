package net.icxd.dungeons.combat;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

/**
 * Players' hits on SkyBlock's mobs, worked out by {@link Damage}: a sword, a fist or any other item
 * (fists crit too), and arrows with the bow they left (see {@link Shots}), and their Ferocity's extra
 * strikes. What effects hook into (EFFECTS.md): buffs on a hit as it lands ({@link #addHitBuffs}), what
 * happens once it has ({@link #addHitListener}: melee hits, arrows, Ferocity strikes and abilities' hits),
 * players' hits on other players ({@link #addPlayerHitListener}) and the Attack Speed cap. Main thread.
 */
public final class Combat {
    /**
     * Multiplicative damage buffs a player has on a hit (dungeon classes' will go here): each gives a
     * factor, 1 for none; they multiply.
     */
    private static final List<ToDoubleBiFunction<Player, Boolean>> MULTIPLIERS = new ArrayList<>();
    private static final List<HitBuffs> HIT_BUFFS = new ArrayList<>();
    private static final List<LandingBuffs> LANDING_BUFFS = new ArrayList<>();
    private static final List<HitListener> HIT_LISTENERS = new ArrayList<>();
    private static final List<PlayerHitListener> PLAYER_HIT_LISTENERS = new ArrayList<>();
    private static final List<ToDoubleFunction<Player>> ATTACK_SPEED_CAPS = new ArrayList<>();
    /** The hit listeners are hearing of a hit now (see {@link #landed}). */
    private static boolean hearing;

    /**
     * A buff on one hit that has landed, which may depend on what it hit (armor bonuses: Reaper Armor
     * deals "+100% damage to Undead mobs"): what it adds to the hit's additive buffs, in percent, a
     * multiplicative factor, 1 for none (see {@link Damage#buffed}), and damage {@code added} to it that only
     * a crit multiplies (the wiki's "Add Damage" mechanics: Soul Eater, Extreme Focus; see {@link
     * Damage#exact(Damage.Attacker, Damage.Target, boolean, double)}).
     */
    public record HitBuff(double additive, double multiplier, double added) {
        public static final HitBuff NONE = new HitBuff(0, 1);

        public HitBuff(double additive, double multiplier) {
            this(additive, multiplier, 0);
        }
    }

    /** What gives a player's hits a {@link HitBuff}: asked once for each hit of theirs that lands on a mob. */
    @FunctionalInterface
    public interface HitBuffs {
        /**
         * The buff on this hit on this target; null for none. {@code attacker} is the hit as it stands:
         * whether it's an arrow, and how far that flew.
         */
        HitBuff on(Player player, Damage.Attacker attacker, Damage.Target target);
    }

    /**
     * A hit as it lands on a mob: the mob itself, what hit it (a melee hit, an arrow, a Ferocity strike or an
     * ability's hit), whether it crits, the item it was dealt with (the held weapon's data, the bow an arrow
     * left, the item an ability was cast with; null for a fist, or anything that isn't a SkyBlock item) and
     * the arrow for an arrow's (null otherwise).
     */
    public record Landing(LivingEntity entity, HitKind kind, boolean critical, NBTTagCompound weapon, Projectile projectile) {
    }

    /**
     * {@link HitBuffs} that need the mob or the crit roll too (Livid's crits from behind, a bow's "+100%
     * damage to Undead"): asked for melee hits and arrows, as HitBuffs are.
     */
    @FunctionalInterface
    public interface LandingBuffs {
        HitBuff on(Player player, Damage.Attacker attacker, Damage.Target target, Landing landing);
    }

    /**
     * Something that happens once a player's hit on a mob has landed and done its damage: {@code target} is
     * the mob as the hit was worked out against (its health before it), {@code damage} what it did, and
     * {@code killed} whether that killed it. Heard for melee hits, arrows, each Ferocity strike and each of an
     * ability's hits ({@link Landing#kind}), not for an effect's own damage ({@link MobHits#deal}), so a
     * listener that deals damage that way can't set itself off again.
     */
    @FunctionalInterface
    public interface HitListener {
        void landed(Player player, Landing landing, Damage.Target target, double damage, boolean killed);
    }

    /**
     * A player's melee hit or arrow on another player (a teammate: the Stinger Bow's Sting), with the item
     * it was dealt with ({@link Landing#weapon}) and the arrow. It does no damage whatever the listeners do,
     * as before (a SkyBlock item's hit on a player is called off).
     */
    @FunctionalInterface
    public interface PlayerHitListener {
        void hit(Player attacker, Player target, HitKind kind, NBTTagCompound weapon, Projectile projectile);
    }

    private Combat() {
    }

    /** Adds a multiplicative buff: its factor for a player's hit (the flag says whether it's an arrow). */
    public static void addMultiplier(ToDoubleBiFunction<Player, Boolean> multiplier) {
        MULTIPLIERS.add(multiplier);
    }

    /** Adds buffs on hits that are only known once the hit lands (see {@link HitBuff}). */
    public static void addHitBuffs(HitBuffs buffs) {
        HIT_BUFFS.add(buffs);
    }

    /** Adds buffs on hits that need the mob or the crit roll (see {@link LandingBuffs}). */
    public static void addHitBuffs(LandingBuffs buffs) {
        LANDING_BUFFS.add(buffs);
    }

    /** Adds something that happens once a hit has landed (see {@link HitListener}). */
    public static void addHitListener(HitListener listener) {
        HIT_LISTENERS.add(listener);
    }

    /** Adds something that happens when a player hits another player (see {@link PlayerHitListener}). */
    public static void addPlayerHitListener(PlayerHitListener listener) {
        PLAYER_HIT_LISTENERS.add(listener);
    }

    /** The {@link HitBuff}s on this hit of theirs on this target, as one: additives and added damage summed, factors multiplied. */
    static HitBuff buffs(Player player, Damage.Attacker attacker, Damage.Target target, Landing landing) {
        double additive = 0;
        double multiplier = 1;
        double added = 0;
        for (HitBuffs buffs : HIT_BUFFS) {
            HitBuff buff = buffs.on(player, attacker, target);
            if (buff == null) continue;
            additive += buff.additive();
            multiplier *= buff.multiplier();
            added += buff.added();
        }
        for (LandingBuffs buffs : LANDING_BUFFS) {
            HitBuff buff = buffs.on(player, attacker, target, landing);
            if (buff == null) continue;
            additive += buff.additive();
            multiplier *= buff.multiplier();
            added += buff.added();
        }
        return new HitBuff(additive, multiplier, added);
    }

    /**
     * Tells the hit listeners a hit has landed (see {@link HitListener}): the hit paths call it once the
     * damage is dealt, this class for melee hits, arrows and Ferocity strikes, {@code Hits.hurt} for abilities.
     * A hit a listener deals itself (it should use {@link MobHits#deal}, but an ability's {@code Hits.hurt}
     * would do) doesn't tell them again, so no effect can set itself off over and over.
     */
    public static void landed(Player player, Landing hit, Damage.Target target, double damage, boolean killed) {
        if (!hit.kind().isHit() || hearing || HIT_LISTENERS.isEmpty()) return;
        hearing = true;
        try {
            for (HitListener listener : HIT_LISTENERS) listener.landed(player, hit, target, damage, killed);
        } finally {
            hearing = false;
        }
    }

    /** The product of the multiplicative buffs on this player's hit. */
    static double multiplier(Player player, boolean ranged) {
        double product = 1;
        for (ToDoubleBiFunction<Player, Boolean> multiplier : MULTIPLIERS) product *= multiplier.applyAsDouble(player, ranged);
        return product;
    }

    /** The player behind a hit: the attacker, or whoever shot the projectile; null for neither. */
    public static Player playerBehind(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    /** A SkyBlock item's data, or null (not one, or empty). */
    public static NBTTagCompound skyBlockData(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null || ItemRegistry.get(tag.getString("id")) == null ? null : tag;
    }

    /** The item's enchantments, id to level (none for null); by the plugin's id, also for one stored under Hypixel's ("ultimate_one_for_all"). */
    public static Map<String, Integer> enchantments(NBTTagCompound tag) {
        Map<String, Integer> enchantments = new HashMap<>();
        if (tag == null) return enchantments;
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) enchantments.put(EnchantmentData.id(list.get(i).getString("name")), list.get(i).getInt("lvl"));
        return enchantments;
    }

    /** The enchantments (id to level) of the SkyBlock item in their main hand; none for anything else. */
    public static Map<String, Integer> heldEnchantments(Player player) {
        return enchantments(skyBlockData(player.getInventory().getItemInMainHand()));
    }

    /**
     * Who's hitting: the player's stats now, and the weapon's enchantments (an ability that hits as a
     * melee hit or an arrow would works its damage out from this too).
     */
    public static Damage.Attacker attacker(Player player, NBTTagCompound weapon, boolean ranged, double travelled) {
        Stats stats = PlayerSession.of(player).stats();
        return new Damage.Attacker(stats.get(Stat.DAMAGE), stats.get(Stat.STRENGTH), stats.get(Stat.CRIT_CHANCE), stats.get(Stat.CRIT_DAMAGE),
                Skills.combatLevel(player), PlayerHealth.get(player), enchantments(weapon), ranged, travelled, multiplier(player, ranged));
    }

    /**
     * A player's hit (or arrow) on an entity. On one of SkyBlock's mobs it does SkyBlock damage, and
     * anything else they throw at one (a snowball, an egg, a pearl) does nothing; a SkyBlock item's hit
     * on anything else does nothing, and other hits stay vanilla. A hit on another player tells the
     * {@link PlayerHitListener}s first. Once a hit on a mob has done its damage, the {@link HitListener}s
     * hear of it, then its Ferocity strikes follow.
     */
    public static void playerHit(EntityDamageByEntityEvent event) {
        Player player = playerBehind(event.getDamager());
        if (player == null || !(event.getEntity() instanceof LivingEntity target)) return;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(target);
        Mobs.Live mob = dungeonMob == null ? Mobs.of(target) : null;
        Projectile projectile = event.getDamager() instanceof Projectile p ? p : null;
        Shots.Shot shot = projectile == null ? null : Shots.take(projectile);
        if (target instanceof Player other && !other.equals(player)) hitPlayer(player, other, projectile, shot);
        if (dungeonMob == null && mob == null) {
            if (skyBlockData(player.getInventory().getItemInMainHand()) != null) event.setCancelled(true);
            return;
        }
        if (projectile != null && shot == null && !Shots.hits(projectile)) {
            event.setCancelled(true);
            return;
        }

        Damage.Target on = MobHits.target(target);
        Damage.Attacker attacker;
        boolean critical;
        DamageIndicators.Look look;
        NBTTagCompound weapon;
        if (shot != null) {
            attacker = shot.attacker(projectile.getLocation());
            critical = shot.critical();
            look = DamageIndicators.Look.of(critical, shot.megaCritical());
            weapon = shot.bow();
        } else {
            weapon = projectile == null ? skyBlockData(player.getInventory().getItemInMainHand()) : null;
            attacker = attacker(player, weapon, projectile != null, 0);
            critical = Damage.crits(attacker.critChance(), ThreadLocalRandom.current().nextDouble());
            look = DamageIndicators.Look.of(critical, false);
        }
        Landing landing = new Landing(target, projectile != null ? HitKind.ARROW : HitKind.MELEE, critical, weapon, projectile);
        HitBuff buff = buffs(player, attacker, on, landing);
        Damage.Attacker buffed = Damage.buffed(attacker, on, buff.additive(), buff.multiplier());
        // What its debuffs make it take ("Frozen mobs take 10% increased damage"), on the whole hit.
        double damage = Math.floor(Damage.exact(buffed, on, critical, buff.added()) * MobDebuffs.takenFactor(target));

        boolean invulnerable = dungeonMob != null ? dungeonMob.invulnerable() : mob.type().isInvulnerable();
        if (projectile == null && !invulnerable) attackSpeed(target, PlayerSession.of(player).stats().get(Stat.ATTACK_SPEED), attackSpeedCap(player));
        if (!invulnerable && RunManager.inRun(player)) restoreMana(player);
        if (dungeonMob != null) DungeonMobs.playerHit(event, player, dungeonMob, damage, look, landing.kind(), weapon);
        else Mobs.playerHit(event, player, mob, damage, look, landing.kind(), weapon);
        if (!invulnerable) {
            landed(player, landing, on, damage, !MobHits.alive(target));
            double ferocity = shot != null ? shot.ferocity() : PlayerSession.of(player).stats().get(Stat.FEROCITY);
            ferocity(player, landing, damage, look, ferocity);
        }
    }

    /** A player's melee hit or arrow on another one: the listeners hear of it (see {@link PlayerHitListener}). */
    private static void hitPlayer(Player player, Player other, Projectile projectile, Shots.Shot shot) {
        if (PLAYER_HIT_LISTENERS.isEmpty()) return;
        NBTTagCompound weapon = shot != null ? shot.bow() : projectile == null ? skyBlockData(player.getInventory().getItemInMainHand()) : null;
        HitKind kind = projectile != null ? HitKind.ARROW : HitKind.MELEE;
        for (PlayerHitListener listener : PLAYER_HIT_LISTENERS) listener.hit(player, other, kind, weapon, projectile);
    }

    /**
     * A hit's extra strikes (see {@link Ferocity}), {@link Ferocity#STRIKE_DELAY_TICKS} apart. Each does the
     * hit's damage again, crit or not as the hit was (the wiki doesn't say they're worked out anew; they
     * don't roll a crit of their own), with its own damage number looking as the hit's did, and a red
     * slash across the target; they stop once it's dead, and one can kill it. They aren't hits: no mana
     * back, no knockback, and First Strike and the like don't count them; the hit listeners hear of each as
     * {@link HitKind#FEROCITY} ("Each Ferocity strike counts as a hit" for Fatal Tempo, the wiki). None for a
     * melee hit from more than 6 blocks (from their feet to its: the wiki doesn't say where it measures
     * from), or for a Berserk in a dungeon run.
     */
    private static void ferocity(Player player, Landing hit, double damage, DamageIndicators.Look look, double ferocity) {
        LivingEntity target = hit.entity();
        if (ferocity <= 0 || !player.getWorld().equals(target.getWorld())) return;
        if (!Ferocity.inRange(hit.kind() == HitKind.ARROW, player.getLocation().distance(target.getLocation())) || ferocityDisabled(player)) return;
        int strikes = Ferocity.extraStrikes(ferocity, ThreadLocalRandom.current().nextDouble());
        if (strikes <= 0) return;
        Landing strike = new Landing(target, HitKind.FEROCITY, hit.critical(), hit.weapon(), null);
        for (int i = 1; i <= strikes; i++) {
            Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> strike(player, strike, damage, look),
                    (long) i * Ferocity.STRIKE_DELAY_TICKS);
        }
    }

    /**
     * "Ferocity is disabled for Berserkers in dungeons" (the wiki's Ferocity, Trivia): the class they've
     * picked (in the Ready Up menu), while they're in a run.
     */
    static boolean ferocityDisabled(Player player) {
        if (!RunManager.inRun(player)) return false;
        User user = User.ifLoaded(player.getUniqueId());
        return user != null && DungeonProfile.selectedClass(user) == DungeonClass.BERSERK;
    }

    /** One extra strike, if the target is still one of SkyBlock's mobs that can be hurt; then the hit listeners hear of it. */
    private static void strike(Player player, Landing strike, double damage, DamageIndicators.Look look) {
        LivingEntity target = strike.entity();
        if (!player.isOnline() || !target.isValid() || target.isDead()) return;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(target);
        Mobs.Live mob = dungeonMob == null ? Mobs.of(target) : null;
        if (dungeonMob != null ? dungeonMob.invulnerable() : mob == null || mob.type().isInvulnerable()) return;
        Damage.Target on = MobHits.target(target);
        slash(player, target);
        if (dungeonMob != null) DungeonMobs.damage(target, player, damage, look, HitKind.FEROCITY, strike.weapon());
        else Mobs.damage(mob, player, damage, look, HitKind.FEROCITY, strike.weapon());
        landed(player, strike, on, damage, !MobHits.alive(target));
    }

    /** A strike's red slash: dust in a line across the target, sideways to the player, one way or the other at random. */
    private static void slash(Player player, LivingEntity target) {
        BoundingBox box = target.getBoundingBox();
        Vector from = target.getLocation().toVector().subtract(player.getLocation().toVector());
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1);
        boolean falling = ThreadLocalRandom.current().nextBoolean();
        for (Vector point : Ferocity.slash(box.getCenter(), from, box.getWidthX() + 0.4, box.getHeight(), falling)) {
            target.getWorld().spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1, 0, 0, 0, 0, red);
        }
    }

    /**
     * Attack Speed shortens how long the mob can't be hurt again: vanilla lets a hit through once the
     * invulnerability left is half its maximum, so the maximum is twice the ticks.
     */
    private static void attackSpeed(LivingEntity target, double attackSpeed, double cap) {
        int ticks = 2 * Damage.invulnerabilityTicks(attackSpeed, cap);
        target.setMaximumNoDamageTicks(ticks);
        target.setNoDamageTicks(ticks);
    }

    /**
     * Adds what raises a player's Attack Speed cap (100), by how much (Newton's Demise's "+50 Attack Speed
     * cap"): the most of them counts, as for the Speed cap (UNKNOWN whether raises add up; only one source
     * is known).
     */
    public static void addAttackSpeedCap(ToDoubleFunction<Player> raise) {
        ATTACK_SPEED_CAPS.add(raise);
    }

    /** Their Attack Speed cap: 100, and the most that raises it. Melee hits' and shortbows' Attack Speed stop there. */
    public static double attackSpeedCap(Player player) {
        double raise = 0;
        for (ToDoubleFunction<Player> cap : ATTACK_SPEED_CAPS) raise = Math.max(raise, cap.applyAsDouble(player));
        return Damage.ATTACK_SPEED_CAP + raise;
    }

    /** In dungeons each melee or arrow hit restores mana (fractions of a point are dropped: the pool is whole). */
    private static void restoreMana(Player player) {
        PlayerSession session = PlayerSession.of(player);
        int pool = session.maxMana();
        int mana = session.getMana() < 0 ? pool : session.getMana();
        session.setMana(Math.min(pool, mana + (int) Damage.manaOnHit(pool)));
    }
}
