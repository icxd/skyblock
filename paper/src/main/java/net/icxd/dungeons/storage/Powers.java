package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;

/**
 * Accessory Powers: the Accessory Bag's stats, which grow with the Accessory Power its accessories give
 * (the wiki's Accessory Power and Module:Power). Each stat of a power is
 * {@code base / 100 * multiplier * 24 * 29.97 * ln(0.0019 * AP + 1) ^ 1.2}, the multiplier being the
 * stat's own (see {@link StorageTables#multiplier}); it gives the recorded Select Power Stone menu's
 * numbers at its 571 Accessory Power (Silky's +472.03 Crit Damage). A power's Unique Power Bonus doesn't
 * grow. The five Starter Powers are everyone's, the five Intermediate ones come with Combat
 * XV; a Stone Power is learned by giving Maxwell nine of its Power Stones (at the Combat level his menu
 * asks for), and there's no Maxwell here yet, so every Stone Power counts as learned until there is (the
 * owner, 2026-09-28: "all can be unlocked for now since the NPCs are missing"; see STORAGE.md).
 */
public final class Powers {
    /** The tiers, best first, as the Select Power Stone menu lists them. */
    private static final List<String> TIERS = List.of("Marvelous Stone Power", "Grandiose Stone Power", "Master Stone Power",
            "Advanced Stone Power", "Intermediate Stone Power", "Starter Stone Power", "Intermediate Power", "Starter Power");

    private Powers() {
    }

    /** How much Accessory Power multiplies a power by: 29.97 * ln(0.0019 * AP + 1) ^ 1.2. */
    public static double multiplier(int accessoryPower) {
        return 29.97 * Math.pow(Math.log(0.0019 * Math.max(0, accessoryPower) + 1), 1.2);
    }

    /** The power's stats at this Accessory Power, without its Unique Power Bonus. */
    public static Stats stats(StorageTables.Power power, int accessoryPower, StorageTables tables) {
        Stats stats = new Stats();
        double multiplier = multiplier(accessoryPower);
        for (Map.Entry<Stat, Double> e : power.stats().entrySet()) {
            stats.add(e.getKey(), e.getValue() / 100 * tables.multiplier(e.getKey()) * 24 * multiplier);
        }
        return stats;
    }

    /** Its Unique Power Bonus, which doesn't grow with Accessory Power. */
    public static Stats bonus(StorageTables.Power power) {
        Stats stats = new Stats();
        power.bonus().forEach(stats::add);
        return stats;
    }

    /** "&9+472.03 Crit Damage": each stat at this Accessory Power, in Hypixel's order. */
    public static List<String> statLines(StorageTables.Power power, int accessoryPower, StorageTables tables) {
        return lines(stats(power, accessoryPower, tables));
    }

    /** "&e+5 Attack Speed". */
    public static List<String> bonusLines(StorageTables.Power power) {
        return lines(bonus(power));
    }

    /** A stat's line in the power's lore: its colour, the number (two decimals at most) and its name; no symbol, as recorded. */
    static List<String> lines(Stats stats) {
        List<String> lines = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            if (stats.has(stat)) lines.add("&" + stat.getColor() + Text.signed(stats.get(stat)) + " " + stat.getDisplayName());
        }
        return lines;
    }

    /**
     * The powers they may pick: the Starter ones, the Intermediate ones from Combat XV, and every Stone Power
     * (a stand-in for the ones learned from Maxwell, whose Combat level is asked for only when learning).
     */
    public static List<StorageTables.Power> unlocked(int combatLevel, StorageTables tables) {
        List<StorageTables.Power> out = new ArrayList<>();
        for (StorageTables.Power power : tables.powers().values()) {
            if (power.stonePower() || combatLevel >= power.combat()) out.add(power);
        }
        out.sort(ORDER);
        return out;
    }

    /** The recorded menu's order: by tier, the best first (Stone Powers before the rest), then by name. */
    static final Comparator<StorageTables.Power> ORDER = Comparator.comparingInt((StorageTables.Power p) -> {
        int i = TIERS.indexOf(p.tier());
        return i < 0 ? TIERS.size() : i;
    }).thenComparing(StorageTables.Power::name);
}
