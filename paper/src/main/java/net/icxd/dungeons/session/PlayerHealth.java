package net.icxd.dungeons.session;

import net.icxd.dungeons.stats.Stat;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * A player's SkyBlock health. It's kept on their session, not as vanilla health, so it isn't held to
 * the server's cap on vanilla max health (1024 by default). Vanilla health only shows it: the same
 * fraction of the (default) vanilla max, on hearts scaled to their max health. Vanilla damage and
 * healing are turned into SkyBlock health by {@link net.icxd.dungeons.listeners.HealthListener}.
 * Main thread.
 */
public final class PlayerHealth {
    private static final double VANILLA_MAX = 20;

    private PlayerHealth() {
    }

    /** Their max health: the Health stat. */
    public static double max(Player player) {
        return Math.max(1, PlayerSession.of(player).stats().get(Stat.HEALTH));
    }

    /** Their health now; never more than their max, which drops when gear comes off. */
    public static double get(Player player) {
        double health = PlayerSession.of(player).getHealth();
        double max = max(player);
        return health < 0 ? max : Math.min(health, max);
    }

    /** At 0 they die. */
    public static void damage(Player player, double amount) {
        set(player, get(player) - amount);
    }

    public static void heal(Player player, double amount) {
        set(player, get(player) + amount);
    }

    /** Between 0 and their max, and shown; at 0 they die. */
    public static void set(Player player, double health) {
        PlayerSession.of(player).setHealth(Math.max(0, Math.min(health, max(player))));
        sync(player);
    }

    /** Down to 0 without dying yet, for when vanilla does the killing (so the death has its cause). */
    public static void drain(Player player) {
        PlayerSession.of(player).setHealth(0);
    }

    /** Back to full, as after a respawn. */
    public static void refill(Player player) {
        PlayerSession.of(player).setHealth(-1);
    }

    /**
     * Shows it: vanilla health as the same fraction of vanilla max health, and hearts for their max
     * health (10, and one more for each 50 health over 100, up to 20). With none left, they die.
     */
    public static void sync(Player player) {
        if (player.isDead()) return;
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        // Earlier versions kept SkyBlock max health in the attribute, and it's saved with the player.
        if (attribute.getBaseValue() != VANILLA_MAX) attribute.setBaseValue(VANILLA_MAX);
        double vanillaMax = attribute.getValue();
        double max = max(player);
        double health = get(player);

        double scale = Math.min(40.0, 20.0 + (max - 100.0) / 25.0);
        if (!player.isHealthScaled() || player.getHealthScale() != scale) player.setHealthScale(scale);
        if (health <= 0) {
            player.setHealth(0);
            return;
        }
        // Alive shows at least half a heart (the client rounds up).
        player.setHealth(Math.min(vanillaMax, Math.max(0.001, vanillaMax * health / max)));
    }
}
