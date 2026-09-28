package net.icxd.dungeons.storage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import net.icxd.dungeons.menu.Icon;

/**
 * The top row of an Ender Chest page and of a backpack, as recorded (the Loadouts and Storage tour,
 * 02:15.8 to 02:40.7): Close, Back, three panes of glass, then « First Page and ← Previous Page past the
 * first page and Next Page → and Last Page » before the last, glass where they aren't. A page's icon
 * (Choose an Icon: "Ender Chest icons replace the glass panes in the navigation bar") shows in the glass's
 * place, without a tooltip as the glass has none (UNKNOWN: only the choosing was recorded).
 */
final class PageBar {
    static final int CLOSE = 0;
    static final int BACK = 1;
    static final int FIRST = 5;
    static final int PREVIOUS = 6;
    static final int NEXT = 7;
    static final int LAST = 8;
    /** The heads' skins, from the recording's packets. */
    static final String FIRST_HEAD = "8af22a97292de001079a5d98a0ae3a82c427172eabc370ed6d4a31c7e3a0024f";
    static final String PREVIOUS_HEAD = "f247603e42ad5c9387d6df2ee48cd2effccd38e485dc9549be31196a57584217";
    static final String NEXT_HEAD = "92d61121e2de01af7b42c75f0652da2c0e64ae18ed4ac1856e1c8677c18805ab";
    static final String LAST_HEAD = "1ceb50d0d79b9fb790a7392660bc296b7ad2f856c5cbe1c566d99cfec191e668";

    private PageBar() {
    }

    /** The page an arrow in this slot goes to (from 1); 0 for a slot that has no arrow on this page. */
    static int target(int slot, int page, int pages) {
        return switch (slot) {
            case FIRST -> page > 1 ? 1 : 0;
            case PREVIOUS -> page > 1 ? page - 1 : 0;
            case NEXT -> page < pages ? page + 1 : 0;
            case LAST -> page < pages ? pages : 0;
            default -> 0;
        };
    }

    /** The nine slots, for page {@code page} of {@code pages} (from 1); {@code icon} a material's name in place of the glass, or null. */
    static Map<Integer, Shown> icons(int page, int pages, String icon) {
        Material glass = icon == null ? null : Material.matchMaterial(icon);
        Shown blank = Shown.blank(glass == null ? Material.BLACK_STAINED_GLASS_PANE : glass);
        Map<Integer, Shown> row = new LinkedHashMap<>();
        row.put(CLOSE, Shown.of(new Icon(Material.BARRIER, "&cClose")));
        row.put(BACK, Shown.of(new Icon(Material.ARROW, "&eBack")));
        for (int slot = 2; slot < 9; slot++) row.put(slot, blank);
        if (page > 1) {
            row.put(FIRST, Shown.of(new Icon(Material.PLAYER_HEAD, "&e« First Page", List.of(), FIRST_HEAD)));
            row.put(PREVIOUS, Shown.of(new Icon(Material.PLAYER_HEAD, "&a← Previous Page", List.of(), PREVIOUS_HEAD)));
        }
        if (page < pages) {
            row.put(NEXT, Shown.of(new Icon(Material.PLAYER_HEAD, "&aNext Page →", List.of(), NEXT_HEAD)));
            row.put(LAST, Shown.of(new Icon(Material.PLAYER_HEAD, "&eLast Page »", List.of(), LAST_HEAD)));
        }
        return row;
    }
}
