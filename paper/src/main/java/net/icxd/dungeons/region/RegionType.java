package net.icxd.dungeons.region;

import lombok.Getter;
import net.icxd.dungeons.Dungeons;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

@Getter
public enum RegionType {
    PRIVATE_ISLAND_3("Your Island", "§a"),
    VILLAGE("Village", new BoundingBox(229, 3, -194, -276, 182, 201)),
    MOUNTAIN("Mountain"),
    FOREST("Forest"),
    FARM("Farm"),
    RUINS("Ruins"),
    COLOSSEUM("Colosseum"),
    GRAVEYARD("Graveyard", "§c"),
    COAL_MINE("Coal Mine"),
    COAL_MINE_CAVES("Coal Mine"),
    WILDERNESS("Wilderness", "§2"),
    SANDBOX_COVE("Sandbox Cove", "§7"),
    HIGH_LEVEL("High Level", "§c"),
    AUCTION_HOUSE("Auction House", "§6"),
    AUCTION_HOUSE_LEFT_SIDE("Auction House", "§6"),
    AUCTION_HOUSE_RIGHT_SIDE("Auction House", "§6"),
    AUCTION_HOUSE_FRONT_SIDE("Auction House", "§6"),
    BAZAAR_ALLEY("Bazaar Alley", "§e"),
    ARCHERY_RANGE("Archery Range", "§9"),
    BANK("Bank", "§6"),
    BLACKSMITH("Blacksmith"),
    LIBRARY("Library"),
    THE_BARN("The Barn", "§b"),
    MUSHROOM_DESERT("Mushroom Desert"),
    GOLD_MINE("Gold Mine", "§6"),
    DEEP_CAVERN("Deep Caverns", "§b"),
    GUNPOWDER_MINES("Gunpowder Mines"),
    LAPIS_QUARRY("Lapis Quarry"),
    PIGMENS_DEN("Pigmen's Den"),
    SLIMEHILL("Slimehill"),
    BIRCH_PARK("Birch Park", "§a"),
    SPRUCE_WOODS("Spruce Woods", "§a"),
    DARK_THICKET("Dark Thicket", "§a"),
    SAVANNA_WOODLAND("Savanna Woodland", "§a"),
    JUNGLE_ISLAND("Jungle Island", "§a"),
    HOWLING_CAVE("Howling Cave"),
    DIAMOND_RESERVE("Diamond Reserve"),
    OBSIDIAN_SANCTUARY("Obsidian Sanctuary"),
    SPIDERS_DEN("Spider's Den", "§c"),
    COMMUNITY_CENTER("Community Center"),
    SPIDERS_DEN_HIVE("Spider's Den", "§c"),
    BLAZING_FORTRESS("Blazing Fortress", "§c"),
    DWARVEN_VILLAGE("Dwarven Village"),
    DWARVEN_MINES("Dwarven Mines", "§2"),
    GOBLIN_BURROWS("Goblin Burrows"),
    THE_MIST("The Mist", "§8"),
    GREAT_ICE_WALL("Great Ice Wall"),
    GATES_TO_THE_MINES("Gates to the Mines"),
    RAMPARTS_QUARRY("Rampart's Quarry"),
    FORGE_BASIN("Forge Basin"),
    THE_FORGE("The Forge"),
    CLIFFSIDE_VEINS("Cliffside Veins"),
    ROYAL_MINES("Royal Mines"),
    DIVANS_GATEWAY("Divan's Gateway"),
    FAR_RESERVE("Far Reserve"),
    THE_END("The End", "§d"),
    THE_END_NEST("The End", "§d"),
    DESERT_SETTLEMENT("Desert Settlement", "§e"),
    OASIS("Oasis"),
    ARCHAEOLOGICAL_SITE("Archaeological Site", "§a"),
    MUSHROOM_GORGE("Mushroom Gorge"),
    OVERGROWN_MUSHROOM_CAVE("Overgrown Mushroom Cave", "§a"),
    GLOWING_MUSHROOM_CAVE("Glowing Mushroom Cave", "§3"),
    BURNING_BRIDGE("Burning Bridge", "§4"),
    VOID_SEPULTURE("Void Sepulture", "§d"),
    DRAGONS_NEST("Dragon's Nest", "§5"),
    NONE("None", "§7");

    private final String name;
    /** In the server's main world; null for regions without bounds yet. */
    private final BoundingBox bounds;
    private final String color;

    RegionType() {
        this(null, "§7");
    }

    RegionType(String name) {
        this(name, "§b");
    }

    RegionType(String name, String color) {
        this(name, null, color);
    }

    RegionType(String name, BoundingBox bounds) {
        this(name, bounds, "§b");
    }

    RegionType(String name, BoundingBox bounds, String color) {
        this.name = name;
        this.bounds = bounds;
        this.color = color;
    }

    /** Its name in its colour, as the sidebar and action bar show it. */
    public String displayName() {
        return color + name;
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