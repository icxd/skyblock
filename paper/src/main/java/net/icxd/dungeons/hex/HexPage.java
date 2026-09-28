package net.icxd.dungeons.hex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.menu.Icon;

/**
 * A page of the Hex under its main menu ("The Hex ➜ Books", "The Hex ➜ Enchant Item", ...), as the wiki's
 * screens and NEU's Hex have them: 6 rows of glass, the item in slot 19 (to look at: it's taken out on the main
 * menu), a header in 28 saying what the page does, Go Back in 45 ("To The Hex") and Close in 49, and its entries
 * in the 5x3 grid of 12-16, 21-25 and 30-34, 15 a page, with Previous Page in 17 and Next Page in 35 when there
 * are more. Some pages have buttons in 48, 50 and 51 ({@link #extras}).
 *
 * <p>A page gives its entries ({@link #entries}), worked out from the item as it is now each time it's drawn
 * (after an upgrade too); an entry's click runs on the next tick while the page is still open. Enchantment
 * lists fill the grid row by row; Books, Modifiers, Reforges and the bottles are centred ({@link #placement}).
 * Main thread.
 */
public abstract class HexPage extends HexScreen {
    public static final int ITEM = 19;
    public static final int HEADER = 28;
    public static final int PREVIOUS = 17;
    public static final int NEXT = 35;
    public static final int BACK = 45;
    public static final int CLOSE = 49;
    /** The bottom row's buttons some pages have. */
    public static final int LEFT = 48;
    public static final int RIGHT = 50;
    public static final int FAR_RIGHT = 51;
    public static final int[] GRID = {12, 13, 14, 15, 16, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34};
    public static final int PER_PAGE = GRID.length;

    /** How a page's entries sit in the grid. */
    public enum Placement {
        /** Row by row from slot 12 (the enchantment lists). */
        ROW_MAJOR,
        /** Centred, as the wiki's screens show a few entries (see {@link #centred}). */
        CENTRED
    }

    /** One of the grid's entries (or a bottom-row button): what it shows, and what a click on it does (null for nothing). */
    public record Entry(Icon icon, Consumer<ClickType> action) {
        public static Entry of(Icon icon, Runnable action) {
            return new Entry(icon, action == null ? null : click -> action.run());
        }

        /** Shown, not clicked. */
        public static Entry shown(Icon icon) {
            return new Entry(icon, null);
        }
    }

    private int page;

    protected HexPage(HexSession session, String title) {
        super(session, title);
    }

    /** The header in slot 28. */
    protected abstract Icon header();

    /** Every entry, in order, for the item as it is now; the grid shows a page of them. */
    protected abstract List<Entry> entries();

    protected Placement placement() {
        return Placement.ROW_MAJOR;
    }

    /** Buttons in 48, 50 and 51 (by slot); none unless a page has them. */
    protected Map<Integer, Entry> extras() {
        return Map.of();
    }

    /** Go Back's line: where it goes. */
    protected String backTo() {
        return "&7To The Hex";
    }

    /** Where Go Back goes: The Hex, with the item. */
    protected void back() {
        session.open(new HexMenu(session));
    }

    /** The page of entries shown, from 0. */
    public int page() {
        return page;
    }

    /** Shows another page of entries (kept within the pages there are). */
    public void page(int page) {
        this.page = page;
        redraw();
    }

    @Override
    protected void draw() {
        List<Entry> entries = entries();
        int pages = pages(entries.size());
        page = Math.max(0, Math.min(page, pages - 1));
        fill(filler());
        for (Map.Entry<Integer, Icon> e : frame(entries.size(), page, header(), backTo()).entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            switch (slot) {
                case PREVIOUS -> set(button(slot, stack, () -> page(page - 1)));
                case NEXT -> set(button(slot, stack, () -> page(page + 1)));
                case BACK -> set(button(slot, stack, this::back));
                default -> set(slot, stack);
            }
        }
        set(GUIClickableItem.close(CLOSE));
        ItemStack item = session.item();
        set(ITEM, item == null ? filler() : item.clone());
        for (Map.Entry<Integer, Integer> e : grid(entries.size(), page, placement()).entrySet()) put(e.getKey(), entries.get(e.getValue()));
        for (Map.Entry<Integer, Entry> e : extras().entrySet()) put(e.getKey(), e.getValue());
    }

    private void put(int slot, Entry entry) {
        ItemStack stack = entry.icon().stack();
        if (entry.action() == null) set(slot, stack);
        else set(button(slot, stack, entry.action()));
    }

    /**
     * The page's frame, as data (the rest is glass, but for the item, the entries, the extras and Close): the
     * header, Go Back, and the page arrows where there's a page that way ("&8Page 2", as NEU reads it). UNKNOWN:
     * the arrows' look (the wiki's "Arrow Up" and "Arrow Down" pictures); arrows, as the plugin's other menus page.
     */
    static Map<Integer, Icon> frame(int entries, int page, Icon header, String backTo) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HEADER, header);
        icons.put(BACK, new Icon(Material.ARROW, "&aGo Back", backTo));
        int pages = pages(entries);
        if (page > 0) icons.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&8Page " + page));
        if (page + 1 < pages) icons.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&8Page " + (page + 2)));
        return icons;
    }

    /** How many pages this many entries take (1 for none). */
    public static int pages(int entries) {
        return Math.max(1, (entries + PER_PAGE - 1) / PER_PAGE);
    }

    /** Where a page's entries go: slot to entry index (in {@code 0..entries-1}). */
    public static Map<Integer, Integer> grid(int entries, int page, Placement placement) {
        int first = page * PER_PAGE;
        int count = Math.max(0, Math.min(PER_PAGE, entries - first));
        List<Integer> slots = placement == Placement.CENTRED ? centred(count) : rowMajor(count);
        Map<Integer, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) out.put(slots.get(i), first + i);
        return out;
    }

    private static List<Integer> rowMajor(int count) {
        List<Integer> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) slots.add(GRID[i]);
        return slots;
    }

    /**
     * The centred places for {@code count} entries, as the wiki's screens show them: 1 in 23; 3 in 21, 23 and 25;
     * 4 in 21, 22, 24 and 25; 5 in 21-25; 6 in 22-24 and 31-33; 12 row by row from 12. UNKNOWN (no screen shows
     * them): 2, taken as 22 and 24 (4's gap in the middle); 7 and more, row by row as 12 is.
     */
    public static List<Integer> centred(int count) {
        return switch (count) {
            case 0 -> List.of();
            case 1 -> List.of(23);
            case 2 -> List.of(22, 24);
            case 3 -> List.of(21, 23, 25);
            case 4 -> List.of(21, 22, 24, 25);
            case 5 -> List.of(21, 22, 23, 24, 25);
            case 6 -> List.of(22, 23, 24, 31, 32, 33);
            default -> rowMajor(count);
        };
    }
}
