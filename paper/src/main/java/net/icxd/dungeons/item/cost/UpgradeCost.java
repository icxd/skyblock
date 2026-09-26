package net.icxd.dungeons.item.cost;

import lombok.Getter;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/** One upgrade (a dungeon star): every one of its costs. */
@Getter
public class UpgradeCost extends Cost {
    private final List<Cost> costs;

    public UpgradeCost(Cost... costs) {
        this.costs = Arrays.asList(costs);
    }

    @Override
    public boolean canPay(Player player, User user) {
        return costs.stream().allMatch(cost -> cost.canPay(player, user));
    }

    @Override
    public void pay(Player player, User user) {
        for (Cost cost : costs) cost.pay(player, user);
    }
}
