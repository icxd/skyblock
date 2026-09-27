package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.stats.Stats;

/**
 * The blessings a run's team has found: each kind's level (found levels add up), what they do to every
 * member's stats while they're in the run, and the tab list footer's "Dungeon Buffs". No Bukkit in it.
 */
final class RunBlessings {
    private final double strength;
    private final Map<Blessing, Integer> levels = new EnumMap<>(Blessing.class);

    RunBlessings(DungeonFloor floor) {
        this.strength = Blessing.strength(floor);
    }

    /** How much stronger they are on this floor (see {@link Blessing#strength}). */
    double strength() {
        return strength;
    }

    void add(Blessing blessing, int level) {
        if (level > 0) levels.merge(blessing, level, Integer::sum);
    }

    int level(Blessing blessing) {
        return levels.getOrDefault(blessing, 0);
    }

    Map<Blessing, Integer> levels() {
        return Collections.unmodifiableMap(levels);
    }

    /** A member's stats with the team's blessings (see {@link Blessing#apply}). */
    void apply(Stats stats) {
        if (!levels.isEmpty()) Blessing.apply(stats, levels, strength);
    }

    /**
     * The tab list footer's part, as recorded (R1): a blank line, "Dungeon Buffs" and each blessing with its
     * level in the footer's order, or Hypixel's line for none yet.
     */
    List<String> footer() {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("&6&lDungeon Buffs");
        if (levels.isEmpty()) lines.add("&7No Buffs active. Find them by exploring the Dungeon!");
        for (Map.Entry<Blessing, Integer> e : levels.entrySet()) lines.add("&f" + e.getKey().displayName(e.getValue()));
        return lines;
    }
}
