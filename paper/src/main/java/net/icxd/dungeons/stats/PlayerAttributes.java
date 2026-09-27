package net.icxd.dungeons.stats;

import net.icxd.dungeons.dungeons.instance.RunManager;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * What a player's stats do to their vanilla attributes, set from their stats every second (see
 * {@link StatsRunnable}) and put back when a stat drops: Speed is their walk speed, Swing Range their
 * melee reach and Respiration how long they can stay under water.
 */
public final class PlayerAttributes {
    /**
     * "Speed is capped at 400% normally" (the wiki's Speed); what raises it (Young Dragon Armor's "+100 Walk
     * Speed Cap") comes in through {@link #addSpeedCap}.
     */
    static final double SPEED_CAP = 400;
    private static final List<ToDoubleFunction<Player>> SPEED_CAPS = new ArrayList<>();
    /**
     * The fastest walk speed can make a player: vanilla's walk speed of 1, five times its 0.2. That's
     * vanilla's limit, not Hypixel's, whose Speed goes past 500 ("Fixed Speed Cap past 500 not actually
     * doing anything", the wiki's Speed, 0.24.4); nothing raises the cap past 500 yet (Young Dragon Armor
     * takes it to 500), and once something does, Speed past 500% will need the MOVEMENT_SPEED attribute as well.
     */
    static final double SPEED_LIMIT = 500;
    /** Vanilla's melee reach, and SkyBlock's base Swing Range (the wiki's Swing Range: "a base value of 3"). */
    static final double VANILLA_REACH = 3;
    /** "The maximum value for swing range is 15" (the wiki's Swing Range). */
    static final double MAX_SWING_RANGE = 15;
    /** Transient, so it isn't saved with the player and goes away with the plugin. */
    private static final NamespacedKey SWING_RANGE = new NamespacedKey("dungeons", "swing_range");

    private PlayerAttributes() {
    }

    /**
     * Adds what raises a player's Speed cap, by how much: the most of them counts, they don't add up
     * ("The Speed cap increase does not stack with a Black Cat Pet", the wiki's Young Dragon Armor).
     */
    public static void addSpeedCap(ToDoubleFunction<Player> raise) {
        SPEED_CAPS.add(raise);
    }

    /** Their Speed cap: 400, and the most that raises it. */
    static double speedCap(Player player) {
        double raise = 0;
        for (ToDoubleFunction<Player> cap : SPEED_CAPS) raise = Math.max(raise, cap.applyAsDouble(player));
        return SPEED_CAP + raise;
    }

    public static void apply(Player player, Stats stats) {
        float walkSpeed = walkSpeed(speed(stats.get(Stat.SPEED), speedCap(player), RunManager.inRun(player)));
        if (player.getWalkSpeed() != walkSpeed) player.setWalkSpeed(walkSpeed);
        setBonus(player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE), SWING_RANGE, reachBonus(stats.get(Stat.SWING_RANGE)));
        int maxAir = maxAir(stats.get(Stat.RESPIRATION));
        if (player.getMaximumAir() != maxAir) {
            player.setMaximumAir(maxAir);
            if (player.getRemainingAir() > maxAir) player.setRemainingAir(maxAir);
        }
    }

    /**
     * How long they can stay under water before they drown, in ticks: "For every 2 Respiration, the player
     * can stay underwater 1 second longer" (the wiki's Respiration), so the base 30 is vanilla's 15 seconds.
     */
    static int maxAir(double respiration) {
        return (int) Math.round(Math.max(0, respiration) * 10);
    }

    /**
     * How fast they walk, in percent of vanilla: their Speed up to its cap, and in the Catacombs "All Speed
     * above 100 is reduced by 33.3%" (the wiki's Catacombs rules), taken as a third after the cap (the
     * wiki doesn't say which comes first). A recorded run fits it: 291 Speed on the tab list, and running
     * at 229% of vanilla's sprint, where 100 + 191 x 2/3 is 227%. Vanilla keeps what Speed does in the air
     * (nothing: the wiki's "Speed does not apply when airborne").
     */
    static double speed(double speed, double cap, boolean dungeon) {
        double capped = Math.max(0, Math.min(speed, cap));
        return dungeon && capped > 100 ? 100 + (capped - 100) * 2 / 3 : capped;
    }

    /** Vanilla's walk speed for that: 0.2 is 100%, and 1 (500%) the most there is. */
    static float walkSpeed(double percent) {
        return (float) (Math.max(0, Math.min(percent, SPEED_LIMIT)) / 500);
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
