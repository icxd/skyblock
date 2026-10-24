package net.icxd.dungeons.island;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;

/**
 * "Jerry The Assistant", Jerry's menu after his first words, as recorded (01:55.2): Patch Notes,
 * Deliveries, Visits & Guesting, Island Properties, Remove Hub Portal and Move Jerry around Close,
 * glass elsewhere. None of those is here yet, so they show and do nothing. The recording also had
 * "Warp to: Jerry's Workshop" (it was the Season of Jerry), which isn't here either. Main thread.
 */
public final class JerryMenu extends GUI {
    public static final String TITLE = "Jerry The Assistant";
    static final int CLOSE = 31;

    private JerryMenu() {
        super(TITLE, Size.FOUR);
    }

    public static void show(Player player) {
        new JerryMenu().open(player);
    }

    /** Every item but the glass, by slot. */
    static Map<Integer, Icon> icons() {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(11, new Icon(Material.BOOK, "&aPatch Notes", "&7View the latest features and", "&7changes to the game.", "", "&eClick to open!"));
        icons.put(13, new Icon(Material.ENDER_CHEST, "&aDeliveries", "&7Any items that may be delivered to", "&7yourself or your island will appear",
                "&7here for collection!", "", "&eClick to open!"));
        icons.put(15, new Icon(Material.EMERALD, "&aVisits & Guesting", "&7Learn all about how to &a/visit &7players", "&7across the SkyBlock universe!",
                "", "&eClick to learn!"));
        icons.put(27, new Icon(Material.NETHER_STAR, "&aIsland Properties", "&7Modify island time, mob spawns,", "&7biomes, and more.", "",
                "&eClick to view!"));
        icons.put(32, new Icon(Material.NETHER_BRICK, "&aRemove Hub Portal", "&7Removes all of the portal blocks", "&7from the Hub Portal on your island!",
                "", "&cRequires Level V in any skill!"));
        icons.put(35, new Icon(Material.BEDROCK, "&aMove Jerry"));
        return icons;
    }

    @Override
    public void beforeOpen(Player player) {
        fill(filler());
        icons().forEach((slot, icon) -> set(slot, icon.stack()));
        set(GUIClickableItem.close(CLOSE));
    }
}
