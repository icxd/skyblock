package net.icxd.dungeons.item.prestige;

import lombok.Getter;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;

import java.util.ArrayList;
import java.util.Arrays;

@Getter
public class Prestige {
    private final SkyBlockItem item;
    private final ArrayList<Cost> costs;
    public Prestige(SkyBlockItem item, Cost... costs) {
        this.item = item;
        this.costs = (ArrayList<Cost>) Arrays.asList(costs);
    }
}
