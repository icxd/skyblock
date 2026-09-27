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
     * floor (0: from the start). A locked one is shown with "Not unlocked!" and can't be bought.
     */
    public record Ware(String item, int amount, int floor, boolean locked, List<Cost> costs) {
        public Ware {
            costs = List.copyOf(costs);
        }
    }

    /** What it shows a player whose highest completed floor is this. */
    public List<Ware> wares(int highestFloor) {
        return wares.stream().filter(w -> w.floor() <= highestFloor).toList();
    }

    /**
     * Ophelia's, in the Dungeon Hub, in the order of the wiki's menu (Ophelia/UI, a tab per floor
     * completed): the first tier from the start, the second after it from Floor III, the third after
     * that from Floor VI, where the wiki shows it "Not unlocked!" even on the Floor VII tab. What
     * unlocks it isn't known, so it stays locked. The costs are NEU's export of her menu (NEU
     * repository items/OPHELIA_NPC.json), which adds the tier below to the coins the wiki lists.
     * Dungeonbreaker is NEU's alone (it's newer than the wiki's tabs), last, as where it goes isn't
     * known. Left out: the Dungeon I-VII Potions, which items.json doesn't have (potions with a
     * level); "Fill your Quiver" (no quiver yet); and the golden boss heads (GOLD_BONZO_HEAD to
     * GOLD_NECRON_HEAD, 1 Gold Essence each in NEU), which she sells once the reward for killing
     * their boss 100 times is claimed (wiki Golden Heads): boss kills aren't counted yet.
     */
    public static final Shop OPHELIA = new Shop("Ophelia", List.of(
            ware("UNDEAD_BOW", 0, coins(80_000)),
            new Ware("ARROW", 20, 0, false, List.of(coins(200))),
            ware("SUPER_CLEAVER", 0, coins(80_000), item("CLEAVER")),
            ware("STONE_CHESTPLATE", 0, coins(80_000)),
            ware("MENDER_HELMET", 0, coins(80_000)),
            ware("DARK_GOGGLES", 0, coins(80_000)),
            ware("SUPERBOOM_TNT", 0, coins(3_000)),
            ware("SUPER_UNDEAD_BOW", 3, coins(800_000), item("UNDEAD_BOW")),
            ware("HYPER_CLEAVER", 3, coins(800_000), item("SUPER_CLEAVER")),
            ware("METAL_CHESTPLATE", 3, coins(800_000), item("STONE_CHESTPLATE")),
            ware("MENDER_FEDORA", 3, coins(800_000), item("MENDER_HELMET")),
            ware("SHADOW_GOGGLES", 3, coins(800_000), item("DARK_GOGGLES")),
            locked("DEATH_BOW", 6, coins(5_000_000), item("SUPER_UNDEAD_BOW")),
            locked("GIANT_CLEAVER", 6, coins(5_000_000), item("HYPER_CLEAVER")),
            locked("STEEL_CHESTPLATE", 6, coins(5_000_000), item("METAL_CHESTPLATE")),
            locked("MENDER_CROWN", 6, coins(5_000_000), item("MENDER_FEDORA")),
            locked("WITHER_GOGGLES", 6, coins(5_000_000), item("SHADOW_GOGGLES")),
            ware("DUNGEONBREAKER", 0, new EssenceCost(EssenceType.WITHER, 500))));

    private static Ware ware(String item, int floor, Cost... costs) {
        return new Ware(item, 1, floor, false, List.of(costs));
    }

    private static Ware locked(String item, int floor, Cost... costs) {
        return new Ware(item, 1, floor, true, List.of(costs));
    }

    private static Cost coins(int coins) {
        return new CoinCost(coins);
    }

    private static Cost item(String id) {
        return new ItemCost(id, 1);
    }
}
