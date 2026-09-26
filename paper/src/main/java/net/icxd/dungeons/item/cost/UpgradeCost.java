package net.icxd.dungeons.item.cost;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Getter
public class UpgradeCost extends Cost {
    private final List<Cost> costs;
    public UpgradeCost(Cost... costs) {
        this.costs = Arrays.asList(costs);
    }

    @Override
    public Predicate<Player> check() {
        return (player) -> {
            for (Cost cost : costs)
                if (!cost.check().test(player)) return false;
            return true;
        };
    }
}
