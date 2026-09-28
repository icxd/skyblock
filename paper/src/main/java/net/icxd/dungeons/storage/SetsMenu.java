package net.icxd.dungeons.storage;

import java.util.List;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;

/**
 * Armor Sets and Equipment Sets, opened from a loadout's piece (the Loadouts and Storage tour, 00:56.1 and
 * 01:14.7): nine sets a page (three pages of armor, two of equipment), a column each with its four pieces
 * and, under them, its button: "Click to equip to loadout!", or for the loadout's own set, "Click to
 * unequip from loadout!"; Clear Selection sets the loadout's set to None. Each goes back to the loadout.
 *
 * <p>The pieces are stored here: a piece is put in its row by clicking the slot with it (or shift-clicking
 * it in their inventory), and taken out by clicking it (shift: into their inventory). An empty slot shows
 * its column's glass, "Place a helmet here to add it to this set." The worn set's column is what they wear,
 * and can't be changed here (the wiki's Wardrobe: "The player cannot modify their current set"). Columns
 * past their rank's are locked (the wiki's Wardrobe and Equipment Wardrobe menus). Every change is written
 * at once, straight between their cursor or inventory and the profile. Main thread.
 */
final class SetsMenu extends GUI {
    /** Which sets: armor, or equipment. */
    enum Kind {
        ARMOR("Armor Sets", StorageDocument.ARMOR_SETS, StorageDocument.WORN_ARMOR_SET, Loadouts.ARMOR_SETS, Equipment.ARMOR),
        EQUIPMENT("Equipment Sets", StorageDocument.EQUIPMENT_SETS, StorageDocument.WORN_EQUIPMENT_SET, Loadouts.EQUIPMENT_SETS, Equipment.SLOTS);

        final String title;
        final String key;
        final String wornKey;
        final int sets;
        final List<String> pieces;

        Kind(String title, String key, String wornKey, int sets, List<String> pieces) {
            this.title = title;
            this.key = key;
            this.wornKey = wornKey;
            this.sets = sets;
            this.pieces = pieces;
        }

        int pages() {
            return sets / 9;
        }

        int unlocked(Rank rank) {
            return this == ARMOR ? Loadouts.armorSets(rank) : Loadouts.equipmentSets(rank);
        }

        /** Whether an item goes in this row of a set. */
        boolean fits(int row, ItemStack item) {
            return this == ARMOR ? Equipment.fitsArmor(row, StorageItems.skyBlockItem(item), item.getType())
                    : Equipment.fitsEquipment(row, StorageItems.skyBlockItem(item));
        }
    }

    static final int PREVIOUS = 45;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    static final int CLEAR = 50;
    static final int NEXT = 53;
    /**
     * Each column's glass: 4 to 9 are recorded (lime to purple); 1 to 3 go before them as the wiki's older
     * Wardrobe has 1 and 2 (red, orange), 3 UNKNOWN (yellow).
     */
    static final List<Material> COLUMNS = List.of(Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE,
            Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE);

    private final Player viewer;
    private final int loadout;
    private final Kind kind;
    private final int page;

    SetsMenu(Player viewer, int loadout, Kind kind, int page) {
        super(title(kind, page), Size.SIX);
        this.viewer = viewer;
        this.loadout = loadout;
        this.kind = kind;
        this.page = page;
    }

    /** "(1/3) Armor Sets", "(2/2) Equipment Sets": the page from 0. */
    static String title(Kind kind, int page) {
        return "(" + (page + 1) + "/" + kind.pages() + ") " + kind.title;
    }

    @Override
    public void beforeOpen(Player player) {
        items();
    }

    private void items() {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document profile = user.profile();
        Document storage = StoredInventory.storage(profile);
        Loadouts.Loadout selected = Loadouts.get(profile, loadout);
        Integer chosen = kind == Kind.ARMOR ? selected.armor() : selected.equipment();
        int worn = StorageDocument.worn(storage, kind.wornKey);
        int unlocked = kind.unlocked(user.getRank());
        for (int column = 0; column < 9; column++) {
            int set = page * 9 + column;
            if (set >= unlocked) {
                Icon lock = locked(kind, set, user.getRank());
                for (int row = 0; row < 4; row++) set(row * 9 + column, new Icon(Material.BLACK_STAINED_GLASS_PANE, lock.name(), lock.lore()).stack());
                set(36 + column, lock.stack());
                continue;
            }
            List<ItemStack> pieces = set == worn ? wearing() : LoadoutActions.peek(StorageDocument.set(storage, kind.key, set));
            for (int row = 0; row < 4; row++) {
                ItemStack piece = pieces.get(row);
                ItemStack shown = piece == null || piece.isEmpty() ? placeholder(kind, column, set, row).stack() : piece.clone();
                int r = row;
                set(GUIClickableItem.button(row * 9 + column, shown, viewer, click -> piece(set, r, click)));
            }
            boolean mine = chosen != null && chosen == set;
            set(GUIClickableItem.button(36 + column, button(set, mine).stack(), viewer, () -> choose(mine ? null : set)));
        }
        if (page > 0) set(GUIClickableItem.button(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page).stack(), viewer,
                () -> new SetsMenu(viewer, loadout, kind, page - 1).open(viewer)));
        if (page < kind.pages() - 1) set(GUIClickableItem.button(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)).stack(),
                viewer, () -> new SetsMenu(viewer, loadout, kind, page + 1).open(viewer)));
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To " + selected.name()).stack(), viewer,
                () -> new LoadoutMenu(viewer, loadout).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
        set(GUIClickableItem.button(CLEAR, clearSelection().stack(), viewer, () -> choose(null)));
    }

    /** What they wear of this kind, in the set's order. */
    private List<ItemStack> wearing() {
        return kind == Kind.ARMOR ? LoadoutActions.armor(viewer.getInventory()) : List.of(Equipment.worn(viewer));
    }

    /** An empty piece slot: "&aSlot 4 Helmet", in its column's glass. */
    static Icon placeholder(Kind kind, int column, int set, int row) {
        return new Icon(COLUMNS.get(column), "&aSlot " + (set + 1) + " " + kind.pieces.get(row), placeLore(kind, row));
    }

    /** The recorded words, broken where Hypixel breaks them. */
    static List<String> placeLore(Kind kind, int row) {
        if (kind == Kind.ARMOR) {
            return switch (row) {
                case 0 -> List.of("&7Place a helmet here to add it to this", "&7set.");
                case 1 -> List.of("&7Place a chestplate here to add it to", "&7this set.");
                case 2 -> List.of("&7Place a pair of leggings here to add", "&7it to this set.");
                default -> List.of("&7Place a pair of boots here to add it", "&7to this set.");
            };
        }
        return switch (row) {
            case 0 -> List.of("&7Place a necklace here to add it to", "&7this set.");
            case 1 -> List.of("&7Place a cloak here to add it to this", "&7set.");
            case 2 -> List.of("&7Place a belt here to add it to this set.");
            default -> List.of("&7Place a pair of gloves or a bracelet", "&7here to add it to this set.");
        };
    }

    /** A set's button, "Slot 2: Ready" or, the loadout's, "Slot 1: Selected". */
    static Icon button(int set, boolean selected) {
        if (selected) {
            return new Icon(Material.LIME_DYE, "Slot " + (set + 1) + ":&a Selected", "&7This slot contains this loadout's",
                    "&7currently selected set.", "", "&eClick to unequip from loadout!");
        }
        return new Icon(Material.GRAY_DYE, "Slot " + (set + 1) + ":&a Ready", "&7This slot is ready to be selected.", "",
                "&eClick to equip to loadout!");
    }

    static Icon clearSelection() {
        return new Icon(Material.LAVA_BUCKET, "&cClear Selection", "&7Clears your current selection for", "&7this component of your loadout.", "",
                "&eClick to clear!");
    }

    /**
     * A set past their rank's: "Requires" the rank that has it, as the wiki's Wardrobe menus say, or past
     * every rank's, where the Account Upgrades' come from.
     */
    static Icon locked(Kind kind, int set, Rank rank) {
        String name = "&7Slot " + (set + 1) + ": &cLocked";
        for (Rank higher : List.of(Rank.VIP_PLUS, Rank.MVP_PLUS)) {
            if (kind.unlocked(higher) > set) {
                return new Icon(Material.RED_DYE, name, "&7This wardrobe slot is locked and", "&7cannot be used", "",
                        "&cRequires " + Loadouts.rankName(higher));
            }
        }
        return new Icon(Material.RED_DYE, name, Loadouts.unlockLines(rank, kind::unlocked));
    }

    /** The loadout's set of this kind (null: None), then back to the loadout. */
    private void choose(Integer set) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Loadouts.Loadout l = Loadouts.get(user.profile(), loadout);
        Loadouts.put(user.profile(), loadout, kind == Kind.ARMOR ? l.withArmor(set) : l.withEquipment(set));
        new LoadoutMenu(viewer, loadout).open(viewer);
    }

    // Moving pieces in and out

    /**
     * A click on a set's piece slot: with nothing on the cursor, the piece comes onto it; with a piece of
     * this row on it, that goes in (and what was there comes out); a shift-click puts the piece in their
     * inventory. Not in the worn set, a locked one, or on a piece that couldn't be read (it stays).
     */
    private void piece(int set, int row, ClickType click) {
        User user = User.ifLoaded(viewer.getUniqueId());
        // Not if the menu has closed since the click (it's the tick after).
        if (user == null || InventorySyncListener.frozen(viewer) || GUI.GUI_MAP.get(viewer.getUniqueId()) != this) return;
        Document storage = StoredInventory.storage(user.profile());
        if (set >= kind.unlocked(user.getRank()) || set == StorageDocument.worn(storage, kind.wornKey)) return;
        List<Object> blobs = StorageDocument.set(storage, kind.key, set);
        Object blob = blobs.get(row);
        ItemStack there = blob == null ? null : StorageItems.peek(blob);
        if (blob != null && there == null) return;
        ItemStack cursor = viewer.getItemOnCursor();
        if (click.isShiftClick()) {
            if (there == null || !viewer.getInventory().addItem(there).isEmpty()) {
                // A partial add can't happen with one item; with no room nothing moved.
                return;
            }
            blobs.set(row, null);
        } else if (cursor.isEmpty()) {
            if (there == null) return;
            blobs.set(row, null);
            viewer.setItemOnCursor(there);
        } else {
            if (cursor.getAmount() != 1 || !StorageItems.storable(cursor) || !kind.fits(row, cursor)) return;
            blobs.set(row, StorageItems.encode(new ItemStack[]{cursor}).getFirst());
            viewer.setItemOnCursor(there);
        }
        StorageDocument.setSet(storage, kind.key, set, blobs);
        items();
        refresh(viewer.getOpenInventory().getTopInventory());
    }

    /** Shift-clicking a piece in their inventory puts it in the first set on this page with room for it. */
    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getAction() != InventoryAction.MOVE_TO_OTHER_INVENTORY) return false;
        event.setCancelled(true);
        ItemStack moving = event.getCurrentItem();
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null || moving == null || moving.isEmpty() || moving.getAmount() != 1 || !StorageItems.storable(moving)
                || !(event.getClickedInventory() instanceof PlayerInventory from)) return true;
        Document storage = StoredInventory.storage(user.profile());
        int worn = StorageDocument.worn(storage, kind.wornKey);
        int unlocked = kind.unlocked(user.getRank());
        for (int column = 0; column < 9; column++) {
            int set = page * 9 + column;
            if (set >= unlocked || set == worn) continue;
            List<Object> blobs = StorageDocument.set(storage, kind.key, set);
            for (int row = 0; row < 4; row++) {
                if (blobs.get(row) != null || !kind.fits(row, moving)) continue;
                blobs.set(row, StorageItems.encode(new ItemStack[]{moving}).getFirst());
                StorageDocument.setSet(storage, kind.key, set, blobs);
                from.setItem(event.getSlot(), null);
                items();
                refresh(event.getView().getTopInventory());
                return true;
            }
        }
        return true;
    }
}
