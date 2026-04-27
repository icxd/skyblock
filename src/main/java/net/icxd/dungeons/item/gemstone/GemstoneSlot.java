package net.icxd.dungeons.item.gemstone;

import lombok.Getter;
import net.icxd.dungeons.item.cost.Cost;

import java.util.Arrays;
import java.util.List;

@Getter
public class GemstoneSlot {
    private final GemstoneType type;
    private final List<Cost> costs;
    public GemstoneSlot(GemstoneType type, Cost... costs) {
        this.type = type;
        this.costs = Arrays.asList(costs);
    }
}
