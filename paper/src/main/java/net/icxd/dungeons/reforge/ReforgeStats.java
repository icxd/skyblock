package net.icxd.dungeons.reforge;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** What a reforge gives, per stat, at each rarity. */
public final class ReforgeStats {
    private final Map<Stat, ReforgeStat> stats = new EnumMap<>(Stat.class);

    /** The stat at common, uncommon, rare, epic, legendary and mythic. */
    public ReforgeStats with(Stat stat, double common, double uncommon, double rare, double epic, double legendary, double mythic) {
        stats.put(stat, new ReforgeStat(common, uncommon, rare, epic, legendary, mythic));
        return this;
    }

    /** Null if the reforge doesn't give it. */
    public ReforgeStat get(Stat stat) {
        return stats.get(stat);
    }

    public Map<Stat, ReforgeStat> all() {
        return Collections.unmodifiableMap(stats);
    }

    /** Everything it gives an item of that rarity. */
    public Stats at(Rarity rarity) {
        Stats result = new Stats();
        stats.forEach((stat, values) -> result.add(stat, values.at(rarity)));
        return result;
    }
}
