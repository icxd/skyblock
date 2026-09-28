package net.icxd.dungeons.economy;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;

/**
 * Vanilla experience from SkyBlock (the Hex's enchanting costs Exp levels): a kill's orbs (each mob's wiki
 * page's "orbs", on its {@code MobKind} variant) and whatever else grants some (a mined block, an effect's
 * "+25 exp orbs"), through {@link #grant}, with what raises it. It reaches them the way the same source's
 * items do: a dungeon mob's straight to them, as its drops go to their inventory; any other mob's as orbs
 * where it died, as its drops land there (the wiki's Experience: sources "drop experience orbs, which may be
 * collected in the same manner as Coins", killing with Telekinesis, now Auto-pickup, excepted). Main thread.
 */
public final class ExpOrbs {
    /** Where experience comes from, for what raises only some of it (Lapis Armor's "+200% XP from mined ores"). */
    public enum Source { MOB, ORE, OTHER }

    private static final List<ToDoubleBiFunction<Player, Source>> BONUSES = new ArrayList<>();

    private ExpOrbs() {
    }

    /**
     * Adds what raises a player's experience from a source, in percent; they add up ("XP gain can be increased
     * in a wide variety of ways (stacking additively)", the wiki's Experience). The Experience enchantment's
     * chance "to drop double experience" is +100 when it rolls.
     */
    public static void addBonus(ToDoubleBiFunction<Player, Source> percent) {
        BONUSES.add(percent);
    }

    /** {@code base} with {@code bonus} percent more, rounded to a whole orb (UNKNOWN how Hypixel rounds: to the nearest). */
    public static int amount(double base, double bonus) {
        if (!(base > 0)) return 0;
        return (int) Math.round(base * Math.max(0, 1 + bonus / 100));
    }

    /** The bonus they have on experience from this source, in percent. */
    public static double bonus(Player player, Source source) {
        double sum = 0;
        for (ToDoubleBiFunction<Player, Source> bonus : BONUSES) sum += bonus.applyAsDouble(player, source);
        return sum;
    }

    /**
     * Grants {@code base} experience from {@code source}, with their bonuses: {@code direct}ly to them (a
     * dungeon mob's, a mined block's that goes to their inventory), or as an orb at {@code at}. Returns what
     * it came to.
     */
    public static int grant(Player player, double base, Source source, Location at, boolean direct) {
        int amount = amount(base, bonus(player, source));
        if (amount <= 0) return 0;
        if (direct || at == null || at.getWorld() == null) {
            player.giveExp(amount);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1f);
        } else {
            at.getWorld().spawn(at, ExperienceOrb.class, orb -> orb.setExperience(amount));
        }
        return amount;
    }
}
