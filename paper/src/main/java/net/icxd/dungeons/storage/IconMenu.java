package net.icxd.dungeons.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;

/**
 * Choose an Icon, for an Ender Chest page (right-click it in Storage), as recorded (the Loadouts and
 * Storage tour, 02:50.7): Reset, then the icons in the middle seven by four, each named as the item is
 * with "Click to select!"; choosing one (or Reset) goes back to Storage with the page showing it. Hypixel's
 * menu has six pages; only the first was recorded, so that's the only one here (UNKNOWN: the rest).
 * Main thread.
 */
final class IconMenu extends GUI {
    static final int RESET = 10;
    static final int GO_BACK = 48;
    static final int CLOSE = 49;
    /** The recorded first page's icons, in order after Reset. */
    static final List<Material> ICONS = List.of(Material.COAL, Material.DIAMOND, Material.IRON_INGOT, Material.GOLD_INGOT, Material.BOWL,
            Material.STRING, Material.FEATHER, Material.GUNPOWDER, Material.WHEAT_SEEDS, Material.WHEAT, Material.FLINT, Material.LEATHER,
            Material.BRICKS, Material.CLAY, Material.SUGAR_CANE, Material.EGG, Material.GLOWSTONE, Material.INK_SAC, Material.BLAZE_ROD,
            Material.GOLD_NUGGET, Material.NETHER_WART, Material.EMERALD, Material.NETHER_STAR, Material.DRAGON_EGG, Material.NETHER_BRICKS,
            Material.QUARTZ, Material.PRISMARINE_CRYSTALS);
    private static final List<String> SAYS = List.of("&7Ender Chest icons replace the glass", "&7panes in the navigation bar.");

    private final Player viewer;
    private final int page;

    IconMenu(Player viewer, int page) {
        super("(1/1) Choose an Icon", Size.SIX);
        this.viewer = viewer;
        this.page = page;
    }

    /** Reset and the icons, by slot: the middle seven of rows two to five. */
    static Map<Integer, Shown> icons() {
        Map<Integer, Shown> icons = new LinkedHashMap<>();
        icons.put(RESET, Shown.of(new Icon(Material.BARRIER, "&cReset", lore("&eClick to reset!"))));
        int i = 0;
        for (int slot = RESET + 1; slot < 44 && i < ICONS.size(); slot++) {
            if (slot % 9 == 0 || slot % 9 == 8) continue;
            icons.put(slot, Shown.of(new Icon(ICONS.get(i++), null, lore("&eClick to select!"))));
        }
        return icons;
    }

    private static List<String> lore(String click) {
        return List.of(SAYS.get(0), SAYS.get(1), "", click);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        for (Map.Entry<Integer, Shown> e : icons().entrySet()) {
            Material icon = e.getKey() == RESET ? null : e.getValue().icon().material();
            set(GUIClickableItem.button(e.getKey(), e.getValue().stack(), viewer, () -> choose(icon)));
        }
        set(GUIClickableItem.button(GO_BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Storage").stack(), viewer,
                () -> new StorageMenu(viewer).open(viewer)));
        set(GUIClickableItem.close(CLOSE));
    }

    /** The page's icon (null: the glass again), then back to Storage. */
    private void choose(Material icon) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        StorageDocument.setIcon(StoredInventory.storage(user.profile()), page, icon == null ? null : icon.name());
        new StorageMenu(viewer).open(viewer);
    }
}
