package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.entity.Player;

/**
 * Strong Dragon Armor's full set bonus, Strong Blood: "Improves Aspect of the End: +75 Damage; Instant
 * Transmission: +2 teleport range, +3 seconds, +5 Strength on cast". The Damage counts while the Aspect
 * of the End is held; Instant Transmission asks for the rest when it's cast from one ({@link #range},
 * {@link #extraMillis}, {@link #onCast}): the Strength lasts as long as the cast's speed does (UNKNOWN).
 * The Aspect of the Void isn't named, so it gets none of it.
 */
public final class StrongBlood implements Bonus {
    static final String SWORD = "ASPECT_OF_THE_END";

    @Override
    public String kind() {
        return SetKey.FULL_SET;
    }

    @Override
    public String name() {
        return "Strong Blood";
    }

    @Override
    public void stats(Player player, Active active, Stats stats) {
        if (SWORD.equals(SetBonuses.held(player))) stats.add(Stat.DAMAGE, 75);
    }

    /** Whether their Instant Transmission is improved: the set is worn and they cast it from the Aspect of the End. */
    private static boolean improves(Player player) {
        return SWORD.equals(SetBonuses.held(player)) && SetBonuses.active(player, "Strong Blood");
    }

    /** How many blocks further their Instant Transmission goes. */
    public static double range(Player player) {
        return improves(player) ? 2 : 0;
    }

    /** How much longer the speed of their Instant Transmission lasts. */
    public static long extraMillis(Player player) {
        return improves(player) ? 3_000 : 0;
    }

    /** What else a cast gives them, for {@code millis}: +5 Strength. */
    public static void onCast(Player player, long millis) {
        if (improves(player)) Bonuses.buff(player, "Strong Blood", new Stats().set(Stat.STRENGTH, 5), millis);
    }
}
