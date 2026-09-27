package net.icxd.dungeons.stats;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

/**
 * What a player's stats do to their vanilla attributes, set from their stats every second (see
 * {@link StatsRunnable}) and put back when a stat drops: Swing Range is their melee reach.
 */
public final class PlayerAttributes {
    /** Vanilla's melee reach, and SkyBlock's base Swing Range (the wiki's Swing Range: "a base value of 3"). */
    static final double VANILLA_REACH = 3;
    /** "The maximum value for swing range is 15" (the wiki's Swing Range). */
    static final double MAX_SWING_RANGE = 15;
    /** Transient, so it isn't saved with the player and goes away with the plugin. */
    private static final NamespacedKey SWING_RANGE = new NamespacedKey("dungeons", "swing_range");

    private PlayerAttributes() {
    }

    public static void apply(Player player, Stats stats) {
        setBonus(player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE), SWING_RANGE, reachBonus(stats.get(Stat.SWING_RANGE)));
    }

    /**
     * How much further than vanilla's 3 blocks they can hit: their Swing Range, which is the distance in
     * blocks (up to 15), less the 3 vanilla has already.
     */
    static double reachBonus(double swingRange) {
        return Math.max(0, Math.min(swingRange, MAX_SWING_RANGE)) - VANILLA_REACH;
    }

    /** Their reach now: the attribute, with Swing Range's bonus. */
    public static double reach(Player player) {
        AttributeInstance range = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        return range == null ? VANILLA_REACH : range.getValue();
    }

    /** One modifier of ours on the attribute, at this amount (none for 0); left alone if it's that already. */
    private static void setBonus(AttributeInstance attribute, NamespacedKey key, double amount) {
        if (attribute == null) return;
        AttributeModifier current = attribute.getModifier(key);
        if (current != null && current.getAmount() == amount) return;
        if (current != null) attribute.removeModifier(key);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER));
    }
}
