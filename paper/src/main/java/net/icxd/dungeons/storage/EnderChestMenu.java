package net.icxd.dungeons.storage;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * A page of the Ender Chest ({@code /enderchest <page>}, or a page in Storage), as recorded (the Loadouts
 * and Storage tour, 02:15.8): "Ender Chest (1/5)", the page bar on top (see {@link PageBar}) and 45 slots
 * for items below it. Back goes to Storage; the arrows go through their pages. Main thread.
 */
final class EnderChestMenu extends ItemPage {
    private static final int[] SLOTS = range(9, StorageMenu.PAGE_SIZE);

    private final int page;
    private final int pages;

    private EnderChestMenu(Player viewer, Document profile, int page, int pages) {
        super("Ender Chest (" + page + "/" + pages + ")", Size.SIX, viewer, profile, SLOTS);
        this.page = page;
        this.pages = pages;
    }

    /** Opens one of their pages (from 1); one they don't have says so (UNKNOWN: what Hypixel says). */
    static void open(Player viewer, int page) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        int pages = StorageMenu.pages();
        if (page < 1 || page > pages) {
            viewer.sendMessage(Text.line(page < 1 || page > StorageMenu.PAGES ? "&cThat's not an Ender Chest page!"
                    : "&cYou haven't unlocked this Ender Chest page!"));
            return;
        }
        new EnderChestMenu(viewer, user.profile(), page, pages).open(viewer);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        for (Map.Entry<Integer, Shown> e : PageBar.icons(page, pages, StorageDocument.icon(storage, page)).entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            int to = PageBar.target(slot, page, pages);
            if (slot == PageBar.CLOSE) set(GUIClickableItem.close(slot));
            else if (slot == PageBar.BACK) set(GUIClickableItem.button(slot, stack, viewer, () -> new StorageMenu(viewer).open(viewer)));
            else if (to > 0) set(GUIClickableItem.button(slot, stack, viewer, () -> open(viewer, to)));
            else set(slot, stack);
        }
    }

    @Override
    ItemStack[] load() {
        return StorageItems.decode(viewer, StorageDocument.page(storage, page, StorageMenu.PAGE_SIZE), StorageDocument.ENDER_CHEST + "." + page,
                storage);
    }

    @Override
    boolean store(List<Binary> items) {
        StorageDocument.setPage(storage, page, items);
        return true;
    }

    /** {@code count} slots from {@code first}. */
    static int[] range(int first, int count) {
        int[] slots = new int[count];
        for (int i = 0; i < count; i++) slots[i] = first + i;
        return slots;
    }
}
