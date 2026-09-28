package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.profile.Profiles;

/** Your Bags and the bags against the recorded ones (the SkyBlock Menu tour, 06:01.3 to 06:18.6), and their sizes. */
class BagMenusTest {

    @Test
    void yourBags() {
        YourBagsMenu.View view = new YourBagsMenu.View(List.of(Bag.values()), 571, "Silky", List.of("&9+472.03 Crit Damage", "&f+12.42 Speed"),
                List.of("&e+5 Attack Speed"));
        Map<Integer, Icon> icons = YourBagsMenu.icons(view, PrivateTables.madeUp(Map.of()));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aSack of Sacks", List.of("&7A sack which contains other sacks.", "&7Sackception!", "",
                "&8Also accessible via /sacks", "", "&eClick to open!"), Bag.SACK_OF_SACKS.texture()), icons.get(19));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aFishing Bag", List.of("&7A useful bag which can hold all",
                "&7types of fish, bait, and fishing loot!", "", "&8Also accessible via /fishingbag", "", "&eClick to open!"),
                Bag.FISHING_BAG.texture()), icons.get(20));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aPotion Bag", List.of("&7A handy bag for holding your", "&7Potions in.", "",
                "&8Also accessible via /potionbag", "", "&eClick to open!"), Bag.POTION_BAG.texture()), icons.get(21));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aQuiver", List.of("&7A masterfully crafted Quiver which", "&7holds any kind of projectile you can",
                "&7think of!", "", "&8Also accessible via /quiver", "", "&eClick to open!"), YourBagsMenu.QUIVER_HEAD), icons.get(23));
        assertEquals(new Icon(Material.PLAYER_HEAD, "&aAccessory Bag", List.of("&7A special bag which can hold", "&7Talismans, Rings, Artifacts, and Orbs",
                "&7within it. All will still work while in this", "&7bag!", "", "&7Accessory Power: &6571", "", "&7Selected Power: &aSilky",
                "&9+472.03 Crit Damage", "&f+12.42 Speed", "", "&7Unique Power Bonus:", "&e+5 Attack Speed", "", "&8Also accessible via /accessorybag",
                "", "&eClick to open!"), Bag.ACCESSORY_BAG.texture()), icons.get(24));
        assertEquals(new Icon(Material.GRAY_DYE, "&cTime Pocket", "&7A bag which holds items that evolve", "&7over time. Time flows twice as fast in",
                "&7here!", "", "&cRequires &aTimite Rift Collection II&c."), icons.get(25));
        assertEquals(6, icons.size());
    }

    @Test
    void lockedBag() {
        StorageTables tables = new StorageTables(Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of("POTION_BAG", new StorageTables.BagSize("NETHER_STALK", 2, 9, Map.of(5, 9))), List.of());
        YourBagsMenu.View view = new YourBagsMenu.View(List.of(Bag.ACCESSORY_BAG), 0, null, List.of(), List.of());
        Map<Integer, Icon> icons = YourBagsMenu.icons(view, tables);
        // As the Time Pocket is shown.
        assertEquals(new Icon(Material.GRAY_DYE, "&cPotion Bag", "&7A handy bag for holding your", "&7Potions in.", "",
                "&cRequires &aNether Wart Collection II&c."), icons.get(21));
        // Without its table, it just says it's locked.
        assertEquals(new Icon(Material.GRAY_DYE, "&cFishing Bag", "&7A useful bag which can hold all", "&7types of fish, bait, and fishing loot!"),
                icons.get(20));
        assertEquals(List.of("&7A special bag which can hold", "&7Talismans, Rings, Artifacts, and Orbs", "&7within it. All will still work while in this",
                "&7bag!", "", "&7Accessory Power: &60", "", "&7Selected Power: &cNone", "", "&8Also accessible via /accessorybag", "",
                "&eClick to open!"), icons.get(24).lore());
    }

    @Test
    void bagSizes() {
        StorageTables.BagSize potions = new StorageTables.BagSize("NETHER_STALK", 2, 9, Map.of(5, 9, 8, 9));
        assertEquals(0, potions.at(1));
        assertEquals(9, potions.at(2));
        assertEquals(18, potions.at(5));
        assertEquals(27, potions.at(12));
        // Without collections (none here yet), every bag but the Accessory Bag is locked, and it has its 9.
        StorageTables none = PrivateTables.madeUp(Map.of());
        assertEquals(9, Bag.ACCESSORY_BAG.capacity(new Document(), none));
        assertEquals(0, Bag.POTION_BAG.capacity(new Document(), none));
        assertNull(Bag.POTION_BAG.requirement(none));
        assertNull(Bag.ACCESSORY_BAG.requirement(none));
        // A Sandbox profile's Accessory Bag has every slot there is; its other bags still go by collections.
        Document sandbox = new Document(Profiles.MODE, ProfileMode.SANDBOX.name());
        assertEquals(281, Bag.ACCESSORY_BAG.capacity(sandbox, none));
        assertEquals(7, Bag.pages(281));
        assertEquals(0, Bag.POTION_BAG.capacity(sandbox, none));
    }

    @Test
    void recordedBagSizes() {
        StorageTables tables = PrivateTables.load();
        assertEquals(9, Bag.ACCESSORY_BAG.capacity(new Document(), tables));
        assertEquals(0, Bag.SACK_OF_SACKS.capacity(new Document(), tables));
        assertEquals("&cRequires &aTropical Fish Collection IV&c.", Bag.SACK_OF_SACKS.requirement(tables));
        assertEquals("&cRequires &aNether Wart Collection II&c.", Bag.POTION_BAG.requirement(tables));
        assertEquals("&cRequires &aRaw Cod Collection III&c.", Bag.FISHING_BAG.requirement(tables));
    }

    @Test
    void bagPages() {
        assertEquals(1, Bag.pages(0));
        assertEquals(1, Bag.pages(45));
        assertEquals(2, Bag.pages(83));
        assertEquals(45, Bag.slotsOn(0, 83));
        assertEquals(38, Bag.slotsOn(1, 83));
        assertEquals(0, Bag.slotsOn(2, 83));
        // The recorded Accessory Bag's two pages, and the 9-slot Potion Bag (06:06.4).
        assertEquals("Accessory Bag (1/2)", BagMenu.title(Bag.ACCESSORY_BAG, 0, 83));
        assertEquals("Accessory Bag (2/2)", BagMenu.title(Bag.ACCESSORY_BAG, 1, 83));
        assertEquals("Potion Bag", BagMenu.title(Bag.POTION_BAG, 0, 9));
        assertEquals(54, BagMenu.size(0, 83));
        assertEquals(54, BagMenu.size(1, 83));
        assertEquals(18, BagMenu.size(0, 9));
    }

    @Test
    void bagBottomRow() {
        Map<Integer, Icon> second = BagMenu.bottomRow(Bag.ACCESSORY_BAG, 1, 2);
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&ePage 1"), second.get(BagMenu.PREVIOUS));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Your Bags"), second.get(BagMenu.GO_BACK));
        assertEquals(new Icon(Material.BARRIER, "&cClose"), second.get(BagMenu.CLOSE));
        assertEquals(new Icon(Material.REDSTONE_TORCH, "&aNeed more room?", "&7You can expand your Accessory Bag", "&7by doing any of the following:", "",
                "&7Increasing your &aRedstone Collection&7.", "", "&7Purchasing slots for &6Coins &7from", "&6Jacobus &7in the &cCombat Settlement &7in",
                "&7the &bHub&7.", "", "&7Unlocking slots from &dElizabeth &7in the", "&bCommunity Center &7in the &bHub&7."), second.get(BagMenu.EXTRA));
        assertFalse(second.containsKey(BagMenu.NEXT));
        Map<Integer, Icon> first = BagMenu.bottomRow(Bag.ACCESSORY_BAG, 0, 2);
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&ePage 2"), first.get(BagMenu.NEXT));
        assertFalse(first.containsKey(BagMenu.PREVIOUS));
        assertEquals(Material.LIME_DYE, BagMenu.bottomRow(Bag.FISHING_BAG, 0, 1).get(BagMenu.EXTRA).material());
        assertEquals(Material.CHEST, BagMenu.bottomRow(Bag.SACK_OF_SACKS, 0, 1).get(BagMenu.EXTRA).material());
        assertEquals(Map.of(BagMenu.GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Your Bags"), BagMenu.CLOSE, new Icon(Material.BARRIER, "&cClose")),
                BagMenu.bottomRow(Bag.POTION_BAG, 0, 1));
    }
}
