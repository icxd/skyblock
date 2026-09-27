package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.PlayerAttributes;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** What bonuses share: cooldowns, timed buffs, the mobs around a player and hurting them. Main thread. */
final class Bonuses {
    private Bonuses() {
    }

    /**
     * Whether a bonus's cooldown is over; if it is, it starts again ({@code seconds} long). Kept with
     * the player's ability cooldowns, under the bonus's name.
     */
    static boolean ready(Player player, String name, double seconds) {
        PlayerSession session = PlayerSession.of(player);
        String key = "bonus:" + name;
        if (session.cooldownLeft(key) > 0) return false;
        if (seconds > 0) session.startCooldown(key, (long) (seconds * 1000));
        return true;
    }

    /** The cooldown its block shows ("&8Cooldown: &a3s"), on the first worn piece that has it; 0 for none. */
    static double cooldown(Bonus.Active active) {
        for (Worn.Piece piece : active.pieces()) {
            for (ItemBlock block : piece.blocks()) {
                if (active.bonus().name().equals(block.name()) && active.bonus().kind().equals(block.kind())) return block.cooldown();
            }
        }
        return 0;
    }

    /**
     * Stats they have for a while (see {@link PlayerSession#buff}), their speed following at once and
     * again when it's over.
     */
    static void buff(Player player, String source, Stats stats, long millis) {
        PlayerSession session = PlayerSession.of(player);
        session.buff(source, stats, millis);
        if (!stats.has(Stat.SPEED)) return;
        PlayerAttributes.apply(player, session.stats());
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (player.isOnline()) PlayerAttributes.apply(player, PlayerSession.of(player).stats());
        }, millis / 50 + 1);
    }

    /**
     * Their share of their max health now, from stats being worked out ({@code stats}' Health is the max
     * so far): a bonus's stats can't ask for their finished stats.
     */
    static double healthShare(Player player, Stats stats) {
        double max = stats.get(Stat.HEALTH);
        double health = PlayerSession.of(player).getHealth();
        if (health < 0 || max <= 0) return 1;
        return Math.min(1, health / max);
    }

    /** Their Catacombs level for stats (at most 50); 0 while their data isn't loaded. */
    static int catacombsLevel(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        return user == null ? 0 : DungeonProfile.catacombsStatLevel(user);
    }

    /** SkyBlock's living mobs within {@code radius} blocks of them (from their feet to the mob's), that can be hurt. */
    static List<LivingEntity> mobsNear(Player player, double radius) {
        List<LivingEntity> mobs = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living.isDead() || !hurtable(living)) continue;
            if (living.getLocation().distanceSquared(player.getLocation()) <= radius * radius) mobs.add(living);
        }
        return mobs;
    }

    private static boolean hurtable(LivingEntity entity) {
        Mobs.Live live = Mobs.of(entity);
        if (live != null) return !live.type().isInvulnerable();
        DungeonMobs.Mob mob = DungeonMobs.of(entity);
        return mob != null && !mob.invulnerable();
    }

    /** A mob's max health (1 for anything that isn't one of SkyBlock's). */
    static double maxHealth(LivingEntity entity) {
        Mobs.Live live = Mobs.of(entity);
        if (live != null) return live.type().getMaxHealth();
        DungeonMobs.Mob mob = DungeonMobs.of(entity);
        return mob == null ? 1 : mob.maxHealth();
    }

    /**
     * They deal one of SkyBlock's mobs this much damage, with no hit (as an ability's): its number shows,
     * it can die of it. Nothing for a mob that can't be hurt (the Watcher, who'd zap them for it).
     */
    static void damage(Player player, LivingEntity entity, double damage) {
        if (damage <= 0 || entity.isDead() || !hurtable(entity)) return;
        Mobs.Live live = Mobs.of(entity);
        if (live != null) Mobs.damage(live, player, damage, DamageIndicators.Look.NORMAL);
        else DungeonMobs.damage(entity, player, damage, DamageIndicators.Look.NORMAL);
    }

    /** Players within {@code radius} of them (them included) for whom this passes. */
    static int playersNear(Player player, double radius, Predicate<Player> which) {
        int count = 0;
        for (Player other : player.getWorld().getPlayers()) {
            if (other.getLocation().distanceSquared(player.getLocation()) <= radius * radius && which.test(other)) count++;
        }
        return count;
    }
}
