package net.icxd.dungeons.region;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

@Getter
public enum RegionType {
    PRIVATE_ISLAND_3("Your Island", ChatColor.GREEN),
    VILLAGE("Village", new Location(Bukkit.getWorld("world"), 229, 3, -194), new Location(Bukkit.getWorld("world"), -276, 182, 201)),
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
    private final Location loc1, loc2;
    private final ChatColor color;

    RegionType() {
        this(null, ChatColor.GRAY);
    }

    RegionType(String name) {
        this(name, ChatColor.AQUA);
    }

    RegionType(String name, ChatColor color) {
        this(name, null, null, color);
    }

    RegionType(String name, Location loc1, Location loc2) {
        this(name, loc1, loc2, ChatColor.AQUA);
    }

    RegionType(String name, Location loc1, Location loc2, ChatColor color) {
        this.name = name;
        this.loc1 = loc1;
        this.loc2 = loc2;
        this.color = color;
    }

    public static RegionType getByID(int id) {
        return RegionType.values()[id];
    }

    public boolean isForagingRegion() {
        return this == BIRCH_PARK || this == SPRUCE_WOODS || this == DARK_THICKET || this == SAVANNA_WOODLAND || this == JUNGLE_ISLAND || this == FOREST;
    }

    public static RegionType getType(String string) {
        try {
            return valueOf(string);
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    /*private boolean isInsideRegion(RegionType type, double x, double y, double z) {
        if (type == null) return false;
        if (type.getLoc1() == null || type.getLoc2() == null) return false;
        return x >= type.getLoc1().getX() && x <= type.getLoc2().getX() && y >= type.getLoc1().getY() && y <= type.getLoc2().getY() && z >= type.getLoc1().getZ() && z <= type.getLoc2().getZ();
    }*/

    public static RegionType getRegionType(double x, double y, double z) {
        for (RegionType region : values()) {
            if (region == NONE) continue;
            if (region.getLoc1() == null || region.getLoc2() == null) continue;

            double minX = Math.min(region.getLoc1().getX(), region.getLoc2().getX());
            double maxX = Math.max(region.getLoc1().getX(), region.getLoc2().getX());
            double minY = Math.min(region.getLoc1().getY(), region.getLoc2().getY());
            double maxY = Math.max(region.getLoc1().getY(), region.getLoc2().getY());
            double minZ = Math.min(region.getLoc1().getZ(), region.getLoc2().getZ());
            double maxZ = Math.max(region.getLoc1().getZ(), region.getLoc2().getZ());

            // debugging: Bukkit.getLogger().info("Checking region " + region.name() + " with coords " + minX + ", " + maxX + ", " + minY + ", " + maxY + ", " + minZ + ", " + maxZ);

            if (x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ) {
                return region;
            }
        }
        return NONE;
    }

    public static RegionType getRegionType(Location location) {
        return getRegionType(location.getX(), location.getY(), location.getX());
    }
}