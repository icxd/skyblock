package net.icxd.dungeons.item.behaviour;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** The Terminator: "Divides your ☣ Crit Chance by 4!" (its lore; the wiki: "divides the player's Crit Chance by 4"). */
final class Terminator implements ItemBehaviour {
    static final String ID = "TERMINATOR";

    @Override
    public void whileHeld(Stats stats) {
        stats.set(Stat.CRIT_CHANCE, stats.get(Stat.CRIT_CHANCE) / 4);
    }
}
