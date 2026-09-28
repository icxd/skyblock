package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.Binary;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.user.StoredInventory;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * A backpack's page ({@code /backpack <slot>}, or a backpack in Storage), as recorded (the Loadouts and
 * Storage tour, 02:22.7): "Greater Backpack (Slot #1)", the page bar on top (see {@link PageBar}) and a
 * row below it for each nine of its slots (the wiki's Backpack: Small 9 to Jumbo 45). Its arrows go
 * through the slots that have a backpack, as the recorded ones did (four backpacks, slot 4 the last).
 * Main thread.
 */
final class BackpackMenu extends ItemPage {
    private final int slot;
    /** The slots with a backpack, in order: this one's page among them. */
    private final List<Integer> filled;

    private BackpackMenu(Player viewer, Document profile, int slot, String name, int size, List<Integer> filled) {
        super(name + " (Slot #" + slot + ")", 9 + size, viewer, profile, EnderChestMenu.range(9, size));
        this.slot = slot;
        this.filled = filled;
    }

    /** Opens the backpack in a slot (from 1); an empty or locked slot says so (UNKNOWN: what Hypixel says). */
    static void open(Player viewer, int slot) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Document storage = StoredInventory.storage(user.profile());
        StorageDocument.Backpack backpack = slot < 1 || slot > StorageMenu.BACKPACK_SLOTS ? null : StorageDocument.backpack(storage, slot);
        SkyBlockItem item = backpack == null ? null : ItemRegistry.get(backpack.id());
        int size = StorageItems.backpackSize(item);
        if (backpack == null || size <= 0) {
            viewer.sendMessage(Text.line(slot < 1 || slot > StorageMenu.BACKPACK_SLOTS ? "&cThat's not a backpack slot!"
                    : "&cThere's no backpack in slot " + slot + "!"));
            return;
        }
        List<Integer> filled = new ArrayList<>();
        for (int s = 1; s <= StorageMenu.BACKPACK_SLOTS; s++) {
            if (StorageDocument.backpack(storage, s) != null) filled.add(s);
        }
        new BackpackMenu(viewer, user.profile(), slot, item.name(), size, filled).open(viewer);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        int page = filled.indexOf(slot) + 1;
        for (Map.Entry<Integer, Shown> e : PageBar.icons(page, filled.size(), null).entrySet()) {
            int at = e.getKey();
            ItemStack stack = e.getValue().stack();
            int to = PageBar.target(at, page, filled.size());
            if (at == PageBar.CLOSE) set(GUIClickableItem.close(at));
            else if (at == PageBar.BACK) set(GUIClickableItem.button(at, stack, viewer, () -> new StorageMenu(viewer).open(viewer)));
            else if (to > 0) set(GUIClickableItem.button(at, stack, viewer, () -> open(viewer, filled.get(to - 1))));
            else set(at, stack);
        }
    }

    @Override
    ItemStack[] load() {
        return StorageItems.decode(viewer, StorageDocument.backpackContents(storage, slot, itemSlots().length),
                StorageDocument.BACKPACKS + "." + slot, storage);
    }

    /**
     * Back into the backpack; false if it's no longer in its slot (it can't be taken out while it holds
     * anything, so this shouldn't happen), and then what was in it goes to them (see ItemPage#sync).
     */
    @Override
    boolean store(List<Binary> items) {
        return StorageDocument.setBackpackContents(storage, slot, items);
    }
}
