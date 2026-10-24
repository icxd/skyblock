package net.icxd.dungeons.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

import org.bukkit.entity.Player;

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
     * floor (0: from the start). A locked one is shown with "Not unlocked!" and can't be bought, as
     * isn't one whose item has a requirement they don't meet (the Farm Merchant's Rookie Hoe, until
     * Farming Skill 1). {@code stock} is how many of the item they may buy a day (0 for no limit, see
     * {@link ShopStock}).
     */
    public record Ware(String item, int amount, int floor, boolean locked, List<Cost> costs, int stock) {
        public Ware {
            costs = List.copyOf(costs);
        }

        public Ware(String item, int amount, int floor, boolean locked, List<Cost> costs) {
            this(item, amount, floor, locked, costs, 0);
        }
    }

    /** What it shows a player whose highest completed floor is this. */
    public List<Ware> wares(int highestFloor) {
        return wares.stream().filter(w -> w.floor() <= highestFloor).toList();
    }

    private static final List<ToDoubleFunction<Player>> DISCOUNTS = new ArrayList<>();

    /**
     * Adds a discount a player has on shops' coin costs, in percent (the Shady Ring's "Get 1% off on most shops");
     * of several, the most counts (UNKNOWN whether Hypixel's add: the three there are are one accessory line).
     */
    public static void addDiscount(ToDoubleFunction<Player> percent) {
        DISCOUNTS.add(percent);
    }

    /** Their discount on shops now, in percent. */
    static double discount(Player player) {
        double most = 0;
        for (ToDoubleFunction<Player> discount : DISCOUNTS) most = Math.max(most, discount.applyAsDouble(player));
        return Math.min(100, most);
    }

    /** A ware's costs to this player: its coins less their discount, the rest as they are. */
    public static List<Cost> costs(Ware ware, Player player) {
        double discount = player == null ? 0 : discount(player);
        if (discount <= 0) return ware.costs();
        List<Cost> costs = new ArrayList<>(ware.costs().size());
        for (Cost cost : ware.costs()) costs.add(cost instanceof CoinCost coins ? new CoinCost(discounted(coins.getAmount(), discount)) : cost);
        return costs;
    }

    /** Coins less a discount of this many percent, rounded up to a whole coin (UNKNOWN how Hypixel rounds). */
    static int discounted(int coins, double percent) {
        return (int) Math.ceil(coins * (1 - percent / 100) - 1e-9);
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

    /**
     * The Farm Merchant's, in the Hub's Farm, as recorded (2026-10-24): crops and the like, each with a
     * day's stock of 640, then the Rookie Hoe and Rookie Farming Axe (locked until Farming Skill 1,
     * which the items require) and the Beginner Agronomy Sack (which does nothing yet: there are no
     * sacks).
     */
    public static final Shop FARM_MERCHANT = new Shop("Farm Merchant", List.of(
            stocked("WHEAT", 3, 30),
            stocked("CARROT_ITEM", 3, 30),
            stocked("POTATO_ITEM", 3, 30),
            stocked("MELON", 10, 40),
            stocked("SUGAR_CANE", 3, 30),
            stocked("PUMPKIN", 1, 25),
            stocked("INK_SACK:3", 1, 5),
            stocked("RED_MUSHROOM", 1, 25),
            stocked("BROWN_MUSHROOM", 1, 25),
            stocked("SAND", 2, 8),
            stocked("CACTUS", 1, 15),
            stocked("ENCHANTED_BONE_MEAL", 3, 6),
            ware("ROOKIE_HOE", 0, coins(10)),
            ware("ROOKIE_FARMING_AXE", 0, coins(10)),
            ware("BEGINNER_AGRONOMY_SACK", 0, coins(250))));

    /** This many for this many coins, with a day's stock. */
    private static Ware stocked(String item, int amount, int coins) {
        return new Ware(item, amount, 0, false, List.of(coins(coins)), ShopStock.DAILY);
    }

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
