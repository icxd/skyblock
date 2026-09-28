package net.icxd.dungeons.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * A bag's menu, as recorded (the SkyBlock Menu tour: the Accessory Bag's two pages at 06:10.9 and
 * 06:13.2, the Potion Bag, the Fishing Bag and the Sack of Sacks): a row for each nine of its slots, the
 * rest of the last row in glass, and a bottom row with Go Back and Close in its middle, and next to them
 * the Accessory Bag's "Need more room?", the Fishing Bag's Use Baits From Bag and the Sack of Sacks' Insert
 * inventory (those two only show: there's no fishing or sacks here yet). The Accessory Bag has a page for
 * each 45 slots, with Previous and Next Page in the bottom corners and "(1/2)" in its title. Only what the
 * bag holds goes in (see {@link Bag#accepts}). Main thread.
 */
final class BagMenu extends ItemPage {
    static final int GO_BACK = 3;
    static final int CLOSE = 4;
    static final int EXTRA = 5;
    static final int PREVIOUS = 0;
    static final int NEXT = 8;

    private final Bag bag;
    private final int page;
    private final int pages;
    private final int capacity;

    private BagMenu(Player viewer, Document profile, Bag bag, int page, int capacity) {
        super(title(bag, page, capacity), size(page, capacity), viewer, profile, EnderChestMenu.range(0, Bag.slotsOn(page, capacity)));
        this.bag = bag;
        this.page = page;
        this.pages = Bag.pages(capacity);
        this.capacity = capacity;
    }

    /** Opens a page of a bag (from 0); a locked one says so (UNKNOWN: what Hypixel says). */
    static void open(Player viewer, Bag bag, int page) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        int capacity = bag.capacity(user.profile(), StorageTables.get());
        if (capacity <= 0) {
            String requirement = bag.requirement(StorageTables.get());
            viewer.sendMessage(Text.line(requirement == null ? "&cYou haven't unlocked this bag yet!" : requirement));
            return;
        }
        new BagMenu(viewer, user.profile(), bag, Math.clamp(page, 0, Bag.pages(capacity) - 1), capacity).open(viewer);
    }

    /** "Accessory Bag", or "Accessory Bag (1/2)" with more pages. */
    static String title(Bag bag, int page, int capacity) {
        int pages = Bag.pages(capacity);
        return bag.displayName() + (pages > 1 ? " (" + (page + 1) + "/" + pages + ")" : "");
    }

    /** Rows for the page's slots, and the bottom row. */
    static int size(int page, int capacity) {
        return ((Bag.slotsOn(page, capacity) + 8) / 9 + 1) * 9;
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        int bottom = getSize() - 9;
        for (int slot = Bag.slotsOn(page, capacity); slot < getSize(); slot++) set(slot, filler());
        for (Map.Entry<Integer, Icon> e : bottomRow(bag, page, pages).entrySet()) {
            int slot = bottom + e.getKey();
            ItemStack stack = e.getValue().stack();
            switch (e.getKey()) {
                case GO_BACK -> set(GUIClickableItem.button(slot, stack, viewer, () -> new YourBagsMenu(viewer).open(viewer)));
                case CLOSE -> set(GUIClickableItem.close(slot));
                case PREVIOUS -> set(GUIClickableItem.button(slot, stack, viewer, () -> open(viewer, bag, page - 1)));
                case NEXT -> set(GUIClickableItem.button(slot, stack, viewer, () -> open(viewer, bag, page + 1)));
                default -> set(slot, stack);
            }
        }
    }

    /** The bottom row's buttons, by column (the rest is glass). */
    static Map<Integer, Icon> bottomRow(Bag bag, int page, int pages) {
        Map<Integer, Icon> row = new LinkedHashMap<>();
        if (page > 0) row.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        row.put(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Your Bags"));
        row.put(CLOSE, new Icon(Material.BARRIER, "&cClose"));
        switch (bag) {
            case ACCESSORY_BAG -> row.put(EXTRA, new Icon(Material.REDSTONE_TORCH, "&aNeed more room?", "&7You can expand your Accessory Bag",
                    "&7by doing any of the following:", "", "&7Increasing your &aRedstone Collection&7.", "", "&7Purchasing slots for &6Coins &7from",
                    "&6Jacobus &7in the &cCombat Settlement &7in", "&7the &bHub&7.", "", "&7Unlocking slots from &dElizabeth &7in the",
                    "&bCommunity Center &7in the &bHub&7."));
            case FISHING_BAG -> row.put(EXTRA, new Icon(Material.LIME_DYE, "&aUse Baits From Bag", "&7Consume baits directly from your",
                    "&7bag when fishing.", "", "&eClick to disable!"));
            case SACK_OF_SACKS -> row.put(EXTRA, new Icon(Material.CHEST, "&aInsert inventory", "&7Inserts your inventory items into",
                    "&7your sacks.", "", "&eClick to put items in!"));
            default -> {
            }
        }
        if (page < pages - 1) row.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        return row;
    }

    @Override
    boolean accepts(int slot, ItemStack item) {
        return StorageItems.storable(item) && bag.accepts(StorageItems.skyBlockItem(item), item.getType());
    }

    @Override
    ItemStack[] load() {
        List<Object> all = StorageDocument.slots(storage, bag.key(), capacity);
        return StorageItems.decode(viewer, all.subList(page * Bag.PAGE, page * Bag.PAGE + itemSlots().length), bag.key(), page * Bag.PAGE,
                storage);
    }

    @Override
    boolean store(List<Binary> items) {
        List<Object> all = StorageDocument.slots(storage, bag.key(), capacity);
        for (int i = 0; i < items.size(); i++) all.set(page * Bag.PAGE + i, items.get(i));
        StorageDocument.setSlots(storage, bag.key(), all);
        return true;
    }

    @Override
    void stored() {
        if (bag == Bag.ACCESSORY_BAG) AccessoryBag.changed(viewer);
    }
}
