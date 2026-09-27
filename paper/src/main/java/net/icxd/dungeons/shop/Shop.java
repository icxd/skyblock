package net.icxd.dungeons.shop;

import java.util.List;

import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.cost.item.ItemCost;

/**
 * An NPC's shop: what it sells, in the order its menu shows it. Every shop buys what players sell
 * (see {@link Selling}), whatever it sells.
 *
 * @param name the menu's title, the NPC's name
 */
public record Shop(String name, List<Ware> wares) {
    public Shop {
        wares = List.copyOf(wares);
    }

    /**
     * One trade: this many of an item for all of its costs, once they've completed this Catacombs
     * floor (0: from the start).
     */
    public record Ware(String item, int amount, int floor, List<Cost> costs) {
        public Ware {
            costs = List.copyOf(costs);
        }
    }

    /** What it shows a player whose highest completed floor is this. */
    public List<Ware> wares(int highestFloor) {
        return wares.stream().filter(w -> w.floor() <= highestFloor).toList();
    }

    /**
     * Ophelia's, in the Dungeon Hub: NEU's export of her menu (NEU repository items/OPHELIA_NPC.json, in
     * its slots' order, with the costs her lore lists, a lower tier among them), once each. The floors
     * are the wiki's (Ophelia/UI has a tab per floor completed): the first tier from the start, the
     * second from Floor III, the third from Floor VI (where the wiki shows it "Not unlocked!", for a
     * reason it doesn't give). Dungeonbreaker and the golden heads aren't on the wiki's tabs: the
     * heads come with their boss's floor here, a guess. Left out: the Dungeon I-VII Potions, which
     * items.json doesn't have (potions with a level), and "Fill your Quiver" (no quiver yet).
     */
    public static final Shop OPHELIA = new Shop("Ophelia", List.of(
            ware("SUPER_CLEAVER", 0, coins(80_000), item("CLEAVER")),
            ware("HYPER_CLEAVER", 3, coins(800_000), item("SUPER_CLEAVER")),
            ware("GIANT_CLEAVER", 6, coins(5_000_000), item("HYPER_CLEAVER")),
            ware("UNDEAD_BOW", 0, coins(80_000)),
            ware("SUPER_UNDEAD_BOW", 3, coins(800_000), item("UNDEAD_BOW")),
            ware("DEATH_BOW", 6, coins(5_000_000), item("SUPER_UNDEAD_BOW")),
            new Ware("ARROW", 20, 0, List.of(coins(200))),
            ware("DARK_GOGGLES", 0, coins(80_000)),
            ware("SHADOW_GOGGLES", 3, coins(800_000), item("DARK_GOGGLES")),
            ware("WITHER_GOGGLES", 6, coins(5_000_000), item("SHADOW_GOGGLES")),
            ware("MENDER_HELMET", 0, coins(80_000)),
            ware("MENDER_FEDORA", 3, coins(800_000), item("MENDER_HELMET")),
            ware("MENDER_CROWN", 6, coins(5_000_000), item("MENDER_FEDORA")),
            ware("SUPERBOOM_TNT", 0, coins(3_000)),
            ware("STONE_CHESTPLATE", 0, coins(80_000)),
            ware("METAL_CHESTPLATE", 3, coins(800_000), item("STONE_CHESTPLATE")),
            ware("STEEL_CHESTPLATE", 6, coins(5_000_000), item("METAL_CHESTPLATE")),
            ware("DUNGEONBREAKER", 0, new EssenceCost(EssenceType.WITHER, 500)),
            ware("GOLD_BONZO_HEAD", 1, gold()),
            ware("GOLD_SCARF_HEAD", 2, gold()),
            ware("GOLD_PROFESSOR_HEAD", 3, gold()),
            ware("GOLD_THORN_HEAD", 4, gold()),
            ware("GOLD_LIVID_HEAD", 5, gold()),
            ware("GOLD_SADAN_HEAD", 6, gold()),
            ware("GOLD_NECRON_HEAD", 7, gold())));

    private static Ware ware(String item, int floor, Cost... costs) {
        return new Ware(item, 1, floor, List.of(costs));
    }

    private static Cost coins(int coins) {
        return new CoinCost(coins);
    }

    private static Cost item(String id) {
        return new ItemCost(id, 1);
    }

    private static Cost gold() {
        return new EssenceCost(EssenceType.GOLD, 1);
    }
}
