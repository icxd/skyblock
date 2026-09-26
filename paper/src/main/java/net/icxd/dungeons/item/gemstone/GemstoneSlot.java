package net.icxd.dungeons.item.gemstone;

import lombok.Getter;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.item.ItemCost;

import java.util.Arrays;
import java.util.List;

/** A gemstone slot an item has; with costs, it starts locked and they unlock it. */
@Getter
public class GemstoneSlot {
    private final GemstoneType type;
    private final List<Cost> costs;

    public GemstoneSlot(GemstoneType type, Cost... costs) {
        this.type = type;
        this.costs = Arrays.asList(costs);
    }

    /** The usual combat slot: 250,000 coins and a flawless jasper, sapphire, ruby and amethyst to unlock. */
    public static GemstoneSlot combat() {
        return new GemstoneSlot(GemstoneType.COMBAT, new CoinCost(250000), new ItemCost("FLAWLESS_JASPER_GEM", 1),
                new ItemCost("FLAWLESS_SAPPHIRE_GEM", 1), new ItemCost("FLAWLESS_RUBY_GEM", 1), new ItemCost("FLAWLESS_AMETHYST_GEM", 1));
    }
}
