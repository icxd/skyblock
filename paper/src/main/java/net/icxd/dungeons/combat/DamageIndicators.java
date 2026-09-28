package net.icxd.dungeons.combat;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

/**
 * The numbers that pop up where something was hurt, as Hypixel's (research damage.md 3): an invisible
 * marker armor stand named "&71,047", or "✧16,485,463✧" with its characters cycling white, white,
 * yellow, gold, red, red for a critical hit; there for 20 ticks. Players get them too when they're hurt.
 */
public final class DamageIndicators {
    /** Recorded: removed 1.0 s after they appear (0.9 to 1.1 s for most). */
    static final int LIFE_TICKS = 20;

    private DamageIndicators() {
    }

    /**
     * How a hit's number looks: gray; a critical hit's between ✧ in the cycling colours; an Overload Mega
     * Critical Hit's between ✯ (the mods that read Hypixel's numbers know it by that symbol; its colours
     * aren't recorded, so they're a crit's); fire damage gold and poison dark green ("&6123,456" for Fire
     * Aspect, "&2123,456" for Venomous: the wiki's Damage Calculation, Damage Indicator).
     */
    public enum Look {
        NORMAL, CRITICAL, MEGA_CRITICAL, FIRE, POISON;

        public static Look of(boolean critical, boolean mega) {
            return !critical ? NORMAL : mega ? MEGA_CRITICAL : CRITICAL;
        }
    }

    /** What a hit shows: rounded down, thousands grouped, never past what Hypixel's can show (2,147,483,647). */
    public static String text(double damage, boolean critical) {
        return text(damage, Look.of(critical, false));
    }

    public static String text(double damage, Look look) {
        String number = number(damage);
        return switch (look) {
            case NORMAL -> "&7" + number;
            case CRITICAL -> Utils.rainbowize("✧" + number + "✧");
            case MEGA_CRITICAL -> Utils.rainbowize("✯" + number + "✯");
            case FIRE -> "&6" + number;
            case POISON -> "&2" + number;
        };
    }

    private static String number(double damage) {
        return Utils.getFormattedNumber((int) Math.min(Integer.MAX_VALUE, Math.floor(Math.max(0, damage))));
    }

    /** A hit on something. */
    public static void show(Entity at, double damage, boolean critical) {
        show(at, damage, Look.of(critical, false));
    }

    public static void show(Entity at, double damage, Look look) {
        spawn(at, text(damage, look));
    }

    /** In a colour of its own (gold for fire). */
    public static void show(Entity at, double damage, char color) {
        spawn(at, "&" + color + number(damage));
    }

    /**
     * Where Hypixel puts them is UNKNOWN; the recorded ones were 0.65 to 0.87 blocks from the mob's feet
     * sideways in any direction and 0.88 to 1.38 up (their middle half), so they're put there at random.
     */
    private static void spawn(Entity at, String name) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(2 * Math.PI);
        double out = random.nextDouble(0.65, 0.87);
        Location spot = at.getLocation().add(Math.cos(angle) * out, random.nextDouble(0.88, 1.38), Math.sin(angle) * out);
        ArmorStand stand = at.getWorld().spawn(spot, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setSilent(true);
            s.setPersistent(false);
            s.customName(Text.line(name));
            s.setCustomNameVisible(true);
        });
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), stand::remove, LIFE_TICKS);
    }
}
