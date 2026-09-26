package net.icxd.dungeons.item.requirement;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Predicate;

public abstract class Requirement {
    /** Whether a player meets it; null if that can't be checked yet. */
    public abstract Predicate<Player> requirement();

    /** The lore lines Hypixel shows for it while it isn't met, e.g. "&4❣ &cRequires &aCombat Skill 22&c.". */
    public abstract List<String> lore();
}
