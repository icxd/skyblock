package net.icxd.dungeons.yoink;

import java.util.List;

import org.bukkit.Material;

/**
 * What /yoinkprofile can bring over from a real profile, each from its place in the API's member data, and each
 * replacing the same part of the profile you play on. A part whose data the API doesn't have (the player turned that
 * API setting off) can't be picked.
 */
public enum YoinkPart {
    INVENTORY("Inventory", Material.CHEST, "Your inventory and armor.", "inventory.inv_contents"),
    EQUIPMENT("Equipment", Material.LEAD, "Your necklace, cloak, belt and gloves.", "inventory.equipment_contents"),
    WARDROBE("Wardrobe", Material.LEATHER_CHESTPLATE, "Your wardrobe's armor sets.", "inventory.wardrobe_contents"),
    ENDER_CHEST("Ender Chest", Material.ENDER_CHEST, "Your ender chest's pages.", "inventory.ender_chest_contents"),
    BACKPACKS("Backpacks", Material.BROWN_DYE, "Your backpacks and what's in them.", "inventory.backpack_contents"),
    ACCESSORIES("Accessory Bag", Material.ITEM_FRAME, "Your accessories, power and tuning.", "inventory.bag_contents.talisman_bag"),
    BAGS("Other Bags", Material.CAULDRON, "Your Potion Bag, Fishing Bag and Sack of Sacks.", "inventory.bag_contents"),
    COINS("Coins", Material.GOLD_INGOT, "Your purse and bank.", "currencies"),
    SKILLS("Skills", Material.DIAMOND_SWORD, "Every skill's XP.", "player_data.experience"),
    COLLECTIONS("Collections", Material.PAINTING, "Every collection's count.", "collection"),
    DUNGEONS("Dungeons", Material.WITHER_SKELETON_SKULL, "Catacombs and class XP, floors and essence.", "dungeons"),
    MINING("Heart of the Mountain", Material.PRISMARINE_SHARD, "HotM XP, tokens, perks and powder.", "mining_core"),
    MINIONS("Crafted Minions", Material.COBBLESTONE, "Which minion tiers you've crafted.", "player_data.crafted_generators");

    private final String title;
    private final Material icon;
    private final String description;
    private final String path;

    YoinkPart(String title, Material icon, String description, String path) {
        this.title = title;
        this.icon = icon;
        this.description = description;
        this.path = path;
    }

    public String title() {
        return title;
    }

    public Material icon() {
        return icon;
    }

    public String description() {
        return description;
    }

    /** Whether the API has this part of the profile (its setting may be off). */
    public boolean available(HypixelProfile profile) {
        return profile.at(path) != null;
    }

    /** The parts the API has, in this order. */
    public static List<YoinkPart> available(HypixelProfile profile, List<YoinkPart> parts) {
        return parts.stream().filter(part -> part.available(profile)).toList();
    }
}
