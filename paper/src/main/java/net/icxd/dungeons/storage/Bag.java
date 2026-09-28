package net.icxd.dungeons.storage;

import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;
import net.icxd.dungeons.utils.Utils;

/**
 * The bags in Your Bags that keep items (the SkyBlock Menu tour's Your Bags, 06:01.3): what each holds
 * and how big it is. A bag's size comes with its collection (the Hypixel collections API's "+9 Potion
 * Bag Slots", see {@link StorageTables#bag}); the Accessory Bag has 9 slots from the start. Collections
 * aren't in this plugin yet, so {@link #setCollections} is where they're wired in; until then every bag
 * but the Accessory Bag is locked, and it has its 9. The Quiver and the Time Pocket only show (see
 * {@link YourBagsMenu}).
 */
public enum Bag {
    ACCESSORY_BAG("accessoryBag", "Accessory Bag", "1a11a7f11bcd5784903c5201d08261c4df8379109d6e611c1cd3ededf031afed", null,
            List.of("&7A special bag which can hold", "&7Talismans, Rings, Artifacts, and Orbs", "&7within it. All will still work while in this",
                    "&7bag!")),
    POTION_BAG("potionBag", "Potion Bag", "2fc626b3a12d099c44da04b5f15c95f2e360e0789bccecf842ef09c72b93d842", "Nether Wart",
            List.of("&7A handy bag for holding your", "&7Potions in.")),
    FISHING_BAG("fishingBag", "Fishing Bag", "eab810b1b4ff598d1d99336e71da4e356d3ad321e169dbd1fa9d506aae61717d", "Raw Cod",
            List.of("&7A useful bag which can hold all", "&7types of fish, bait, and fishing loot!")),
    SACK_OF_SACKS("sackOfSacks", "Sack of Sacks", "80a077e248d142772ea800864f8c578b9d36885b29daf836b64a706882b6ec10", "Tropical Fish",
            List.of("&7A sack which contains other sacks.", "&7Sackception!"));

    /** The Accessory Bag's slots without the table (the wiki's Accessory Bag: "9 Accessory Bag slots are unlocked for everyone"). */
    static final int ACCESSORY_BAG_BASE = 9;
    /**
     * Every Accessory Bag slot there is, which a Sandbox profile has (the wiki's Accessory Bag: the 9, 48
     * from the Redstone Collection, 4 from the Redstone Miner, 12 Account Upgrades, 198 from Jacobus and 10
     * from the Accessory Size attribute).
     */
    static final int ACCESSORY_BAG_MOST = 281;
    /** Most slots a page shows; the rest go on more pages (the recorded Accessory Bag had two). */
    public static final int PAGE = 45;
    private static final Set<Material> POTIONS = Set.of(Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION);
    private static final Set<Material> FISH = Set.of(Material.COD, Material.SALMON, Material.PUFFERFISH, Material.TROPICAL_FISH,
            Material.COOKED_COD, Material.COOKED_SALMON);

    /** The tier a profile has of a collection, by Hypixel's id ("NETHER_STALK"). */
    public interface CollectionTiers {
        int tier(Document profile, String collection);
    }

    private static CollectionTiers collections = (profile, collection) -> 0;

    /** Where collection tiers come from: none until the collections are wired in (see STORAGE.md). */
    public static void setCollections(CollectionTiers source) {
        collections = source;
    }

    private final String key;
    private final String displayName;
    private final String texture;
    /** The collection's name as the locked bag says it ("Nether Wart"); null for the Accessory Bag, which isn't locked. */
    private final String collectionName;
    private final List<String> description;

    Bag(String key, String displayName, String texture, String collectionName, List<String> description) {
        this.key = key;
        this.displayName = displayName;
        this.texture = texture;
        this.collectionName = collectionName;
        this.description = description;
    }

    /** Its section of the profile's storage. */
    public String key() {
        return key;
    }

    public String displayName() {
        return displayName;
    }

    String texture() {
        return texture;
    }

    List<String> description() {
        return description;
    }

    /** How many slots it has for this profile; 0 while it's locked. A Sandbox profile's Accessory Bag has them all. */
    public int capacity(Document profile, StorageTables tables) {
        if (this == ACCESSORY_BAG && Profiles.mode(profile) == ProfileMode.SANDBOX) return ACCESSORY_BAG_MOST;
        StorageTables.BagSize size = tables.bag(name());
        if (size == null) return this == ACCESSORY_BAG ? ACCESSORY_BAG_BASE : 0;
        return size.at(size.collection() == null ? 0 : collections.tier(profile, size.collection()));
    }

    /** "&cRequires &aNether Wart Collection II&c.", the way the recorded Time Pocket says it; null if it's never locked. */
    String requirement(StorageTables tables) {
        StorageTables.BagSize size = tables.bag(name());
        if (collectionName == null || size == null || size.unlock() <= 0) return null;
        return "&cRequires &a" + collectionName + " Collection " + Utils.getRomanNumeral(size.unlock()) + "&c.";
    }

    /**
     * Whether an item goes in: accessories in the Accessory Bag; potions (and God Potions, the wiki's
     * Potion Bag since 0.11.2) in the Potion Bag; bait and fish in the Fishing Bag; sacks in the Sack of
     * Sacks. What else Hypixel lets in ("fishing loot") is UNKNOWN.
     */
    public boolean accepts(SkyBlockItem item, Material material) {
        return switch (this) {
            case ACCESSORY_BAG -> item != null && item.specificItemType() == SpecificItemType.ACCESSORY;
            case POTION_BAG -> POTIONS.contains(material) || item != null && item.id().startsWith("GOD_POTION");
            case FISHING_BAG -> item != null && "BAIT".equals(item.typeKey()) || FISH.contains(material)
                    && (item == null || item.specificItemType() != SpecificItemType.PET_ITEM);
            case SACK_OF_SACKS -> item != null && "SACK".equals(item.typeKey());
        };
    }

    /** How many pages its menu has: one per {@link #PAGE} slots. */
    public static int pages(int capacity) {
        return Math.max(1, (capacity + PAGE - 1) / PAGE);
    }

    /** How many of its slots are on a page (from 0). */
    public static int slotsOn(int page, int capacity) {
        return Math.max(0, Math.min(PAGE, capacity - page * PAGE));
    }
}
