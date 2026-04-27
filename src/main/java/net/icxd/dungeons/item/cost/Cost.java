package net.icxd.dungeons.item.cost;

import org.bukkit.entity.Player;

import java.util.function.Predicate;

public abstract class Cost {
    public abstract Predicate<Player> check();
}
