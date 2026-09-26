package net.icxd.dungeons.item.requirement;

import org.bukkit.entity.Player;

import java.util.function.Predicate;

public abstract class Requirement {
    public abstract Predicate<Player> requirement();
}
