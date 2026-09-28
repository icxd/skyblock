package net.icxd.dungeons.reforge;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * What a reforge gives, per stat, at each rarity it has numbers for. Special and Very Special give Mythic's (the
 * wiki's Reforging/Prices); a rarity with none gives nothing, as Divine does but on mining tools (the wiki's
 * Reforging: "reforges on Divine items provide no stat buff, only the reforge abilities").
 */
public final class ReforgeStats {
    public static final ReforgeStats NONE = new ReforgeStats(Map.of());

    private final Map<Rarity, Map<Stat, Double>> byRarity;

    public ReforgeStats(Map<Rarity, Map<Stat, Double>> byRarity) {
        Map<Rarity, Map<Stat, Double>> copy = new EnumMap<>(Rarity.class);
        byRarity.forEach((rarity, stats) -> {
            Map<Stat, Double> row = new EnumMap<>(Stat.class);
            row.putAll(stats);
            copy.put(rarity, Collections.unmodifiableMap(row));
        });
        this.byRarity = Collections.unmodifiableMap(copy);
    }

    /** The rarity whose numbers an item of this one gets: its own, or Mythic's above Divine (UNKNOWN: Unobtainable, taken as Very Special). */
    static Rarity numbersFor(Rarity rarity) {
        return switch (rarity) {
            case SPECIAL, VERY_SPECIAL, UNOBTAINABLE -> Rarity.MYTHIC;
            default -> rarity;
        };
    }

    /** Everything it gives an item of that rarity, in Stat's order. */
    public Map<Stat, Double> row(Rarity rarity) {
        return byRarity.getOrDefault(numbersFor(rarity), Map.of());
    }

    /** One stat at that rarity (0 for none). */
    public double get(Stat stat, Rarity rarity) {
        return row(rarity).getOrDefault(stat, 0.0);
    }

    /** Everything it gives an item of that rarity. */
    public Stats at(Rarity rarity) {
        Stats result = new Stats();
        row(rarity).forEach(result::add);
        return result;
    }

    /** Every rarity's, as the data has them. */
    public Map<Rarity, Map<Stat, Double>> all() {
        return byRarity;
    }
}
