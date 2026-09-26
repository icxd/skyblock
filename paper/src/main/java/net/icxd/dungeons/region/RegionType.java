package net.icxd.dungeons.region;

import lombok.Getter;
import net.icxd.dungeons.Dungeons;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

@Getter
public enum RegionType {
    PRIVATE_ISLAND_3("Your Island", ChatColor.GREEN),
    VILLAGE("Village", new BoundingBox(229, 3, -194, -276, 182, 201)),
    MOUNTAIN("Mountain"),
    FOREST("Forest"),
    FARM("Farm"),
    RUINS("Ruins"),
    COLOSSEUM("Colosseum"),
    GRAVEYARD("Graveyard", ChatColor.RED),
    COAL_MINE("Coal Mine"),
    COAL_MINE_CAVES("Coal Mine"),
    WILDERNESS("Wilderness", ChatColor.DARK_GREEN),
    SANDBOX_COVE("Sandbox Cove", ChatColor.GRAY),
    HIGH_LEVEL("High Level", ChatColor.RED),
    AUCTION_HOUSE("Auction House", ChatColor.GOLD),
    AUCTION_HOUSE_LEFT_SIDE("Auction House", ChatColor.GOLD),
    AUCTION_HOUSE_RIGHT_SIDE("Auction House", ChatColor.GOLD),
    AUCTION_HOUSE_FRONT_SIDE("Auction House", ChatColor.GOLD),
    BAZAAR_ALLEY("Bazaar Alley", ChatColor.YELLOW),
    ARCHERY_RANGE("Archery Range", ChatColor.BLUE),
    BANK("Bank", ChatColor.GOLD),
    BLACKSMITH("Blacksmith"),
    LIBRARY("Library"),
    THE_BARN("The Barn", ChatColor.AQUA),
    MUSHROOM_DESERT("Mushroom Desert"),
    GOLD_MINE("Gold Mine", ChatColor.GOLD),
    DEEP_CAVERN("Deep Caverns", ChatColor.AQUA),
    GUNPOWDER_MINES("Gunpowder Mines"),
    LAPIS_QUARRY("Lapis Quarry"),
    PIGMENS_DEN("Pigmen's Den"),
    SLIMEHILL("Slimehill"),
    BIRCH_PARK("Birch Park", ChatColor.GREEN),
    SPRUCE_WOODS("Spruce Woods", ChatColor.GREEN),
    DARK_THICKET("Dark Thicket", ChatColor.GREEN),
    SAVANNA_WOODLAND("Savanna Woodland", ChatColor.GREEN),
    JUNGLE_ISLAND("Jungle Island", ChatColor.GREEN),
    HOWLING_CAVE("Howling Cave"),
    DIAMOND_RESERVE("Diamond Reserve"),
    OBSIDIAN_SANCTUARY("Obsidian Sanctuary"),
    SPIDERS_DEN("Spider's Den", ChatColor.RED),
    COMMUNITY_CENTER("Community Center"),
    SPIDERS_DEN_HIVE("Spider's Den", ChatColor.RED),
    BLAZING_FORTRESS("Blazing Fortress", ChatColor.RED),
    DWARVEN_VILLAGE("Dwarven Village"),
    DWARVEN_MINES("Dwarven Mines", ChatColor.DARK_GREEN),
    GOBLIN_BURROWS("Goblin Burrows"),
    THE_MIST("The Mist", ChatColor.DARK_GRAY),
    GREAT_ICE_WALL("Great Ice Wall"),
    GATES_TO_THE_MINES("Gates to the Mines"),
    RAMPARTS_QUARRY("Rampart's Quarry"),
    FORGE_BASIN("Forge Basin"),
    THE_FORGE("The Forge"),
    CLIFFSIDE_VEINS("Cliffside Veins"),
    ROYAL_MINES("Royal Mines"),
    DIVANS_GATEWAY("Divan's Gateway"),
    FAR_RESERVE("Far Reserve"),
    THE_END("The End", ChatColor.LIGHT_PURPLE),
    THE_END_NEST("The End", ChatColor.LIGHT_PURPLE),
    DESERT_SETTLEMENT("Desert Settlement", ChatColor.YELLOW),
    OASIS("Oasis"),
    ARCHAEOLOGICAL_SITE("Archaeological Site", ChatColor.GREEN),
    MUSHROOM_GORGE("Mushroom Gorge"),
    OVERGROWN_MUSHROOM_CAVE("Overgrown Mushroom Cave", ChatColor.GREEN),
    GLOWING_MUSHROOM_CAVE("Glowing Mushroom Cave", ChatColor.DARK_AQUA),
    BURNING_BRIDGE("Burning Bridge", ChatColor.DARK_RED),
    VOID_SEPULTURE("Void Sepulture", ChatColor.LIGHT_PURPLE),
    DRAGONS_NEST("Dragon's Nest", ChatColor.DARK_PURPLE),
    NONE("None", ChatColor.GRAY);

    private final String name;
    /** In the server's main world; null for regions without bounds yet. */
    private final BoundingBox bounds;
    private final ChatColor color;

    RegionType() {
        this(null, ChatColor.GRAY);
    }

    RegionType(String name) {
        this(name, ChatColor.AQUA);
    }

    RegionType(String name, ChatColor color) {
        this(name, null, color);
    }

    RegionType(String name, BoundingBox bounds) {
        this(name, bounds, ChatColor.AQUA);
    }

    RegionType(String name, BoundingBox bounds, ChatColor color) {
        this.name = name;
        this.bounds = bounds;
        this.color = color;
    }

    public static RegionType getType(String string) {
        try {
            return valueOf(string);
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    /** The region at a position in the server's main world. */
    public static RegionType getRegionType(double x, double y, double z) {
        for (RegionType region : values()) {
            BoundingBox box = region.bounds;
            // Both corners count, as they did when regions were two corner locations.
            if (box != null && x >= box.getMinX() && x <= box.getMaxX() && y >= box.getMinY() && y <= box.getMaxY()
                    && z >= box.getMinZ() && z <= box.getMaxZ()) {
                return region;
            }
        }
        return NONE;
    }

    /** NONE outside the main world, and on servers without regions (dungeons). */
    public static RegionType getRegionType(Location location) {
        if (!Dungeons.getSkyBlockServer().hasRegions() || !location.getWorld().equals(Dungeons.getSkyBlockServer().getMainWorld())) {
            return NONE;
        }
        return getRegionType(location.getX(), location.getY(), location.getZ());
    }
}