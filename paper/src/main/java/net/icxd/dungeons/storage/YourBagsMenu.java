package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.menu.SkyBlockMenu;
import net.icxd.dungeons.user.User;

/**
 * Your Bags ({@code /bags}, or the head in the SkyBlock Menu), as recorded (the SkyBlock Menu tour,
 * 06:01.3): the Sack of Sacks, Fishing Bag and Potion Bag, the Quiver, the Accessory Bag with its Accessory
 * Power and selected power, and the Time Pocket, locked. A bag its collection hasn't unlocked shows as the
 * Time Pocket does, in gray dye with what it needs ({@link Bag#requirement}; collections aren't here yet).
 * The Quiver shows as recorded but doesn't open (its arrows are for later: STORAGE.md). Main thread.
 */
public final class YourBagsMenu extends GUI {
    public static final String TITLE = "Your Bags";
    static final int SACK_OF_SACKS = 19;
    static final int FISHING_BAG = 20;
    static final int POTION_BAG = 21;
    static final int QUIVER = 23;
    static final int ACCESSORY_BAG = 24;
    static final int TIME_POCKET = 25;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    static final String QUIVER_HEAD = "44e1843df8f91a7033e4c810f10b003e07ade5100079ac44df74a4e46a489447";

    /**
     * What the menu shows of a profile: which bags are unlocked, the Accessory Bag's Accessory Power and the
     * selected power's lines (null for none).
     */
    record View(List<Bag> unlocked, int accessoryPower, String power, List<String> powerStats, List<String> powerBonus) {
    }

    private final Player viewer;

    public YourBagsMenu(Player viewer) {
        super(TITLE, Size.SIX);
        this.viewer = viewer;
    }

    static View view(Player player, Document profile) {
        StorageTables tables = StorageTables.get();
        List<Bag> unlocked = new ArrayList<>();
        for (Bag bag : Bag.values()) {
            if (bag.capacity(profile, tables) > 0) unlocked.add(bag);
        }
        int power = AccessoryBag.accessoryPower(player);
        StorageTables.Power selected = AccessoryBag.selectedPower(profile);
        return new View(unlocked, power, selected == null ? null : selected.name(),
                selected == null ? List.of() : Powers.statLines(selected, power, tables), selected == null ? List.of() : Powers.bonusLines(selected));
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        View view = view(viewer, user.profile());
        for (Map.Entry<Integer, Icon> e : icons(view, StorageTables.get()).entrySet()) {
            Bag bag = bag(e.getKey());
            if (bag != null && view.unlocked().contains(bag)) {
                set(GUIClickableItem.button(e.getKey(), e.getValue().stack(), viewer, () -> BagMenu.open(viewer, bag, 0)));
            } else {
                set(e.getKey(), e.getValue().stack());
            }
        }
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To SkyBlock Menu").stack(), viewer,
                () -> new SkyBlockMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    private static Bag bag(int slot) {
        return switch (slot) {
            case SACK_OF_SACKS -> Bag.SACK_OF_SACKS;
            case FISHING_BAG -> Bag.FISHING_BAG;
            case POTION_BAG -> Bag.POTION_BAG;
            case ACCESSORY_BAG -> Bag.ACCESSORY_BAG;
            default -> null;
        };
    }

    /** The bags, by slot. */
    static Map<Integer, Icon> icons(View view, StorageTables tables) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(SACK_OF_SACKS, bag(Bag.SACK_OF_SACKS, view, tables, "/sacks"));
        icons.put(FISHING_BAG, bag(Bag.FISHING_BAG, view, tables, "/fishingbag"));
        icons.put(POTION_BAG, bag(Bag.POTION_BAG, view, tables, "/potionbag"));
        icons.put(QUIVER, new Icon(Material.PLAYER_HEAD, "&aQuiver", List.of("&7A masterfully crafted Quiver which",
                "&7holds any kind of projectile you can", "&7think of!", "", "&8Also accessible via /quiver", "", "&eClick to open!"), QUIVER_HEAD));
        icons.put(ACCESSORY_BAG, accessoryBag(view));
        icons.put(TIME_POCKET, new Icon(Material.GRAY_DYE, "&cTime Pocket", "&7A bag which holds items that evolve",
                "&7over time. Time flows twice as fast in", "&7here!", "", "&cRequires &aTimite Rift Collection II&c."));
        return icons;
    }

    /** A bag as recorded, or, locked, the way the Time Pocket is: gray dye, its name in red, and what it needs. */
    private static Icon bag(Bag bag, View view, StorageTables tables, String command) {
        List<String> lore = new ArrayList<>(bag.description());
        if (view.unlocked().contains(bag)) {
            lore.addAll(List.of("", "&8Also accessible via " + command, "", "&eClick to open!"));
            return new Icon(Material.PLAYER_HEAD, "&a" + bag.displayName(), lore, bag.texture());
        }
        String requirement = bag.requirement(tables);
        if (requirement != null) lore.addAll(List.of("", requirement));
        return new Icon(Material.GRAY_DYE, "&c" + bag.displayName(), lore);
    }

    /** With its Accessory Power, and the selected power's stats and bonus (the wiki's older menu: "Selected Power: &cNone" without one). */
    static Icon accessoryBag(View view) {
        List<String> lore = new ArrayList<>(Bag.ACCESSORY_BAG.description());
        lore.addAll(List.of("", "&7Accessory Power: &6" + view.accessoryPower(), ""));
        if (view.power() == null) {
            lore.add("&7Selected Power: &cNone");
        } else {
            lore.add("&7Selected Power: &a" + view.power());
            lore.addAll(view.powerStats());
            if (!view.powerBonus().isEmpty()) {
                lore.addAll(List.of("", "&7Unique Power Bonus:"));
                lore.addAll(view.powerBonus());
            }
        }
        lore.addAll(List.of("", "&8Also accessible via /accessorybag", "", "&eClick to open!"));
        return new Icon(Material.PLAYER_HEAD, "&aAccessory Bag", lore, Bag.ACCESSORY_BAG.texture());
    }
}
