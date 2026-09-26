package net.icxd.dungeons.item.cost;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class UpgradeCosts {
    private final List<UpgradeCost> costs;
    public UpgradeCosts(UpgradeCost... costs) {
        this.costs = Arrays.asList(costs);
    }
}
