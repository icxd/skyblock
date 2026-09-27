package net.icxd.dungeons.combat;

import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * SkyBlock damage to players: taken from their SkyBlock health directly (vanilla armor doesn't count
 * again), with the hurt animation, sound, knockback and damage indicator a hit has. Main thread.
 */
public final class PlayerDamage {
    /** How a hit gets through. */
    public enum Kind {
        /** Less their Defense. */
        NORMAL,
        /** Ignores Defense; less their True Defense instead (the wiki's True Damage). */
        TRUE,
        /** A share of their max health, which Defense doesn't reduce: the amount is the share (traps: 0.15). */
        MAX_HEALTH
    }

    /** The knockback a mob's hit gives, sideways and up (as the plugin's mobs have had it). */
    private static final double KNOCKBACK = 0.4;
    private static final double KNOCKBACK_UP = 0.36;
    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL =
            ThreadLocal.withInitial(() -> new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(Locale.US)));

    private PlayerDamage() {
    }

    /** What a hit of this kind takes from a player with these stats and max health. */
    public static double taken(double amount, Kind kind, double defense, double trueDefense, double maxHealth) {
        return switch (kind) {
            case NORMAL -> Damage.taken(amount, defense);
            case TRUE -> Damage.taken(amount, trueDefense);
            case MAX_HEALTH -> Math.max(0, amount) * maxHealth;
        };
    }

    /** A mob's ordinary hit: less their Defense, knocked back from {@code by}. */
    public static double hit(Player player, double damage, Entity by) {
        return hit(player, damage, Kind.NORMAL, by, 1);
    }

    /**
     * Hits a player. {@code by} is what they're knocked away from (null for no knockback), and
     * {@code knockback} how much of the usual knockback they get. Returns the health it took (0 if they
     * can't be hurt: dead, or in creative or spectator).
     */
    public static double hit(Player player, double amount, Kind kind, Entity by, double knockback) {
        if (player.isDead() || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return 0;
        Stats stats = PlayerSession.of(player).stats();
        double taken = taken(amount, kind, stats == null ? 0 : stats.get(Stat.DEFENSE), stats == null ? 0 : stats.get(Stat.TRUE_DEFENSE),
                PlayerHealth.max(player));
        PlayerHealth.damage(player, taken);
        DamageIndicators.show(player, taken, false);
        if (player.isDead()) return taken;
        if (by != null && knockback > 0) {
            Vector away = player.getLocation().toVector().subtract(by.getLocation().toVector()).setY(0);
            if (away.lengthSquared() > 0) away.normalize().multiply(KNOCKBACK * knockback);
            player.setVelocity(away.setY(KNOCKBACK_UP * Math.min(knockback, 1.5)));
        }
        player.playHurtAnimation(0);
        player.getWorld().playSound(player, Sound.ENTITY_PLAYER_HURT, 1, 1);
        return taken;
    }

    /**
     * A dungeon trap: a share of their max health that Defense doesn't reduce, and the chat line
     * Hypixel sends ("The Arrow Trap hit you for 785.8 damage!": 15% of a 5,238.7 max).
     */
    public static void trap(Player player, String trap, double share) {
        double taken = hit(player, share, Kind.MAX_HEALTH, null, 0);
        if (taken > 0) player.sendMessage(Utils.color(trapMessage(trap, taken)));
    }

    /** "&cThe Arrow Trap hit you for 785.8 damage!": one decimal at most (as recorded), thousands grouped. */
    public static String trapMessage(String trap, double damage) {
        return "&cThe " + trap + " hit you for " + ONE_DECIMAL.get().format(damage) + " damage!";
    }
}
