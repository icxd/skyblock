package net.icxd.dungeons.combat;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemRegistry;
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

/**
 * Players' hits on SkyBlock's mobs, worked out by {@link Damage}: a sword, a fist or any other item
 * (fists crit too), and arrows with the bow they left (see {@link Shots}), and their Ferocity's extra
 * strikes. Main thread.
 */
public final class Combat {
    /**
     * Multiplicative damage buffs a player has on a hit (dungeon classes' will go here): each gives a
     * factor, 1 for none; they multiply.
     */
    private static final List<ToDoubleBiFunction<Player, Boolean>> MULTIPLIERS = new ArrayList<>();

    private Combat() {
    }

    /** Adds a multiplicative buff: its factor for a player's hit (the flag says whether it's an arrow). */
    public static void addMultiplier(ToDoubleBiFunction<Player, Boolean> multiplier) {
        MULTIPLIERS.add(multiplier);
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
    static NBTTagCompound skyBlockData(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag == null || ItemRegistry.get(tag.getString("id")) == null ? null : tag;
    }

    /** The item's enchantments, id to level (none for null). */
    static Map<String, Integer> enchantments(NBTTagCompound tag) {
        Map<String, Integer> enchantments = new HashMap<>();
        if (tag == null) return enchantments;
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) enchantments.put(list.get(i).getString("name").toLowerCase(), list.get(i).getInt("lvl"));
        return enchantments;
    }

    /** The enchantments (id to level) of the SkyBlock item in their main hand; none for anything else. */
    public static Map<String, Integer> heldEnchantments(Player player) {
        return enchantments(skyBlockData(player.getInventory().getItemInMainHand()));
    }

    /** Who's hitting: the player's stats now, and the weapon's enchantments. */
    static Damage.Attacker attacker(Player player, NBTTagCompound weapon, boolean ranged, double travelled) {
        Stats stats = PlayerSession.of(player).stats();
        return new Damage.Attacker(stats.get(Stat.DAMAGE), stats.get(Stat.STRENGTH), stats.get(Stat.CRIT_CHANCE), stats.get(Stat.CRIT_DAMAGE),
                Skills.combatLevel(player), PlayerHealth.get(player), enchantments(weapon), ranged, travelled, multiplier(player, ranged));
    }

    /**
     * A player's hit (or arrow) on an entity. On one of SkyBlock's mobs it does SkyBlock damage, and
     * anything else they throw at one (a snowball, an egg, a pearl) does nothing; a SkyBlock item's hit
     * on anything else does nothing, and other hits stay vanilla.
     */
    public static void playerHit(EntityDamageByEntityEvent event) {
        Player player = playerBehind(event.getDamager());
        if (player == null || !(event.getEntity() instanceof LivingEntity target)) return;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(target);
        Mobs.Live mob = dungeonMob == null ? Mobs.of(target) : null;
        Projectile projectile = event.getDamager() instanceof Projectile p ? p : null;
        Shots.Shot shot = projectile == null ? null : Shots.take(projectile);
        if (dungeonMob == null && mob == null) {
            if (skyBlockData(player.getInventory().getItemInMainHand()) != null) event.setCancelled(true);
            return;
        }
        if (projectile != null && shot == null && !Shots.hits(projectile)) {
            event.setCancelled(true);
            return;
        }

        Damage.Target on = dungeonMob != null
                ? new Damage.Target(dungeonMob.health(), dungeonMob.maxHealth(), dungeonMob.defense(), dungeonMob.magicResistance(), dungeonMob.types(),
                        DungeonMobs.hitsTaken(target))
                : mob.target();
        Damage.Attacker attacker;
        boolean critical;
        DamageIndicators.Look look;
        if (shot != null) {
            attacker = shot.attacker(projectile.getLocation());
            critical = shot.critical();
            look = DamageIndicators.Look.of(critical, shot.megaCritical());
        } else {
            attacker = attacker(player, projectile == null ? skyBlockData(player.getInventory().getItemInMainHand()) : null, projectile != null, 0);
            critical = Damage.crits(attacker.critChance(), ThreadLocalRandom.current().nextDouble());
            look = DamageIndicators.Look.of(critical, false);
        }
        double damage = Damage.hit(attacker, on, critical);

        boolean invulnerable = dungeonMob != null ? dungeonMob.invulnerable() : mob.type().isInvulnerable();
        if (projectile == null && !invulnerable) attackSpeed(target, PlayerSession.of(player).stats().get(Stat.ATTACK_SPEED));
        if (!invulnerable && RunManager.inRun(player)) restoreMana(player);
        if (dungeonMob != null) DungeonMobs.playerHit(event, player, dungeonMob, damage, look);
        else Mobs.playerHit(event, player, mob, damage, look);
        if (!invulnerable) {
            double ferocity = shot != null ? shot.ferocity() : PlayerSession.of(player).stats().get(Stat.FEROCITY);
            ferocity(player, target, damage, look, ferocity, projectile != null);
        }
    }

    /**
     * A hit's extra strikes (see {@link Ferocity}), {@link Ferocity#STRIKE_DELAY_TICKS} apart. Each does the
     * hit's damage again, crit or not as the hit was (the wiki doesn't say they're worked out anew; they
     * don't roll a crit of their own), with its own damage number looking as the hit's did, and a red
     * slash across the target; they stop once it's dead, and one can kill it. They aren't hits: no mana
     * back, no knockback, and First Strike and the like don't count them. None for a melee hit from more
     * than 6 blocks (from their feet to its: the wiki doesn't say where it measures from), or for a
     * Berserk in a dungeon run.
     */
    private static void ferocity(Player player, LivingEntity target, double damage, DamageIndicators.Look look, double ferocity, boolean ranged) {
        if (ferocity <= 0 || !player.getWorld().equals(target.getWorld())) return;
        if (!Ferocity.inRange(ranged, player.getLocation().distance(target.getLocation())) || ferocityDisabled(player)) return;
        int strikes = Ferocity.extraStrikes(ferocity, ThreadLocalRandom.current().nextDouble());
        for (int i = 1; i <= strikes; i++) {
            Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> strike(player, target, damage, look),
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

    /** One extra strike, if the target is still one of SkyBlock's mobs that can be hurt. */
    private static void strike(Player player, LivingEntity target, double damage, DamageIndicators.Look look) {
        if (!player.isOnline() || !target.isValid() || target.isDead()) return;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(target);
        Mobs.Live mob = dungeonMob == null ? Mobs.of(target) : null;
        if (dungeonMob != null ? dungeonMob.invulnerable() : mob == null || mob.type().isInvulnerable()) return;
        slash(player, target);
        if (dungeonMob != null) DungeonMobs.damage(target, player, damage, look);
        else Mobs.damage(mob, player, damage, look);
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
    private static void attackSpeed(LivingEntity target, double attackSpeed) {
        int ticks = 2 * Damage.invulnerabilityTicks(attackSpeed);
        target.setMaximumNoDamageTicks(ticks);
        target.setNoDamageTicks(ticks);
    }

    /** In dungeons each melee or arrow hit restores mana (fractions of a point are dropped: the pool is whole). */
    private static void restoreMana(Player player) {
        PlayerSession session = PlayerSession.of(player);
        int pool = session.maxMana();
        int mana = session.getMana() < 0 ? pool : session.getMana();
        session.setMana(Math.min(pool, mana + (int) Damage.manaOnHit(pool)));
    }
}
