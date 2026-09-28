package net.icxd.dungeons.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;

/**
 * Vanilla damage on players (a fall, fire, lava, an explosion, a vanilla arrow...), which becomes SkyBlock
 * health 1:1 (HealthListener: SkyBlock's own hits go through {@link PlayerDamage}), and what changes it by
 * its cause: Feather Falling's "-5% fall damage" a level, Blast Protection's Defense against explosions,
 * Fire Protection's against fire. Each gives a factor on the damage of a cause (1 for none, 0 for all of it
 * gone); they multiply. What cancels a cause outright is a set bonus's {@code immune}. The void and /kill
 * never change. How SkyBlock itself works out fall and fire damage is UNKNOWN (the wiki's Damage Calculation
 * has fall damage as "Damage received when falling from a great height" and fire's and lava's as "Differs"),
 * so they stay vanilla's.
 */
public final class VanillaDamage {
    /** What vanilla damage came from, as the wiki's Damage Calculation sorts damage types. */
    public enum Cause {
        FALL, FIRE, LAVA, EXPLOSION, PROJECTILE, DROWNING, SUFFOCATION, CONTACT, MAGIC, POISON, WITHER, VOID, OTHER;

        /** Vanilla's cause as one of these. */
        public static Cause of(DamageCause cause) {
            if (cause == null) return OTHER;
            return switch (cause) {
                case FALL, FLY_INTO_WALL -> FALL;
                case FIRE, FIRE_TICK, HOT_FLOOR, CAMPFIRE -> FIRE;
                case LAVA -> LAVA;
                case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> EXPLOSION;
                case PROJECTILE -> PROJECTILE;
                case DROWNING -> DROWNING;
                case SUFFOCATION, CRAMMING -> SUFFOCATION;
                case CONTACT -> CONTACT;
                case MAGIC -> MAGIC;
                case POISON -> POISON;
                case WITHER -> WITHER;
                case VOID -> VOID;
                default -> OTHER;
            };
        }
    }

    private static final List<ToDoubleBiFunction<Player, Cause>> FACTORS = new ArrayList<>();

    private VanillaDamage() {
    }

    /**
     * Adds a factor on vanilla damage by its cause (0.7 for 30% less). It's asked once for each such hit,
     * before the hit's immunity and saves from death are, so they see the damage as it will be.
     */
    public static void addFactor(ToDoubleBiFunction<Player, Cause> factor) {
        FACTORS.add(factor);
    }

    /** The factor on this cause's damage to them: the product of the factors, never below 0; 1 for the void and /kill. */
    public static double factor(Player player, DamageCause cause) {
        if (cause == DamageCause.VOID || cause == DamageCause.KILL) return 1;
        Cause of = Cause.of(cause);
        double product = 1;
        for (ToDoubleBiFunction<Player, Cause> factor : FACTORS) product *= factor.applyAsDouble(player, of);
        return Math.max(0, product);
    }
}
