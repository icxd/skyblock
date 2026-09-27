package net.icxd.dungeons.combat;

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
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleBiFunction;

/**
 * Players' hits on SkyBlock's mobs, worked out by {@link Damage}: a sword, a fist or any other item
 * (fists crit too), and arrows with the bow they left (see {@link Shots}). Main thread.
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

    /** Who's hitting: the player's stats now, and the weapon's enchantments. */
    static Damage.Attacker attacker(Player player, NBTTagCompound weapon, boolean ranged, double travelled) {
        Stats stats = PlayerSession.of(player).stats();
        return new Damage.Attacker(stats.get(Stat.DAMAGE), stats.get(Stat.STRENGTH), stats.get(Stat.CRIT_CHANCE), stats.get(Stat.CRIT_DAMAGE),
                Skills.combatLevel(player), PlayerHealth.get(player), enchantments(weapon), ranged, travelled, multiplier(player, ranged));
    }

    /**
     * A player's hit (or arrow) on an entity. On one of SkyBlock's mobs it does SkyBlock damage; a
     * SkyBlock item's hit on anything else does nothing, and other hits stay vanilla.
     */
    public static void playerHit(EntityDamageByEntityEvent event) {
        Player player = playerBehind(event.getDamager());
        if (player == null || !(event.getEntity() instanceof LivingEntity target)) return;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(target);
        Mobs.Live mob = dungeonMob == null ? Mobs.of(target) : null;
        Projectile projectile = event.getDamager() instanceof Projectile p ? p : null;
        if (dungeonMob == null && mob == null) {
            if (skyBlockData(player.getInventory().getItemInMainHand()) != null) event.setCancelled(true);
            if (projectile != null) Shots.take(projectile);
            return;
        }

        Damage.Target on = dungeonMob != null
                ? new Damage.Target(dungeonMob.health(), dungeonMob.maxHealth(), dungeonMob.defense(), dungeonMob.magicResistance(), dungeonMob.types(),
                        DungeonMobs.hitsTaken(target))
                : mob.target();
        Damage.Attacker attacker;
        boolean critical;
        Shots.Shot shot = projectile == null ? null : Shots.take(projectile);
        if (shot != null) {
            attacker = shot.attacker(projectile.getLocation());
            critical = shot.critical();
        } else {
            attacker = attacker(player, projectile == null ? skyBlockData(player.getInventory().getItemInMainHand()) : null, projectile != null, 0);
            critical = Damage.crits(attacker.critChance(), ThreadLocalRandom.current().nextDouble());
        }
        double damage = Damage.hit(attacker, on, critical);

        boolean invulnerable = dungeonMob != null ? dungeonMob.invulnerable() : mob.type().isInvulnerable();
        if (projectile == null && !invulnerable) attackSpeed(target, PlayerSession.of(player).stats().get(Stat.ATTACK_SPEED));
        if (!invulnerable && RunManager.inRun(player)) restoreMana(player);
        if (dungeonMob != null) DungeonMobs.playerHit(event, player, dungeonMob, damage, critical);
        else Mobs.playerHit(event, player, mob, damage, critical);
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
