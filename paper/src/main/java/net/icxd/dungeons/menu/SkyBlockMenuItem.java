package net.icxd.dungeons.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.user.StoredInventory;

/**
 * "SkyBlock Menu (Click)", the nether star in the last hotbar slot that opens the SkyBlock Menu, as
 * recorded (the menu tour's inventory, slot 44, the ninth of the hotbar; NEU's item has the same name
 * and lore). It belongs to where they are, not to them: it's never saved with
 * their items (StoredInventory's not-saved tag), each server puts it back when they join, switch
 * profile or respawn, and it can't be moved, dropped or put anywhere (see SkyBlockMenuListener).
 * Dungeon servers don't give it: the Magical Map is in that slot there, as on Hypixel. Main thread.
 */
public final class SkyBlockMenuItem {
    /** Hotbar slot 9. */
    public static final int SLOT = 8;
    public static final String NAME = "&aSkyBlock Menu &7(Click)";
    public static final List<String> LORE = List.of("&7View all of your SkyBlock progress,", "&7including your Skills, Collections,",
            "&7Recipes, and more!", "", "&eClick to open!");
    /** What makes it this item (Hypixel's has the SkyBlock id SKYBLOCK_MENU, which is also an item here and would be rebuilt as one). */
    static final NamespacedKey KEY = new NamespacedKey("skyblock", "skyblock_menu");

    private SkyBlockMenuItem() {
    }

    /** Whether servers of this type give it. */
    static boolean gives(ServerType type) {
        return type != ServerType.DUNGEONS;
    }

    /** Whether this server gives it. */
    public static boolean givenHere() {
        return gives(Dungeons.getSkyBlockServer().getServerType());
    }

    public static ItemStack create() {
        ItemStack item = new Icon(Material.NETHER_STAR, NAME, LORE).stack();
        // So a copy (a creative middle click) can't stack onto it.
        item.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        item.editPersistentDataContainer(data -> data.set(KEY, PersistentDataType.BOOLEAN, true));
        StoredInventory.markNotSaved(item);
        return item;
    }

    public static boolean is(ItemStack item) {
        return item != null && !item.isEmpty() && item.getType() == Material.NETHER_STAR && item.getPersistentDataContainer().has(KEY);
    }

    /**
     * Puts it in hotbar slot 9 if it isn't there, and takes away any other (a copy somewhere else, or
     * on the cursor). Whatever was in the slot moves to a free one; with none free they go without it
     * for now (it's put back once there's room). Nothing on a dungeon server. See {@link Plan}.
     */
    public static void give(Player player) {
        if (!givenHere()) return;
        PlayerInventory inventory = player.getInventory();
        int size = inventory.getSize();
        boolean[] item = new boolean[size];
        boolean[] empty = new boolean[size];
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = inventory.getItem(slot);
            item[slot] = is(stack);
            empty[slot] = stack == null || stack.isEmpty();
        }
        if (is(player.getItemOnCursor())) player.setItemOnCursor(null);
        Plan plan = Plan.of(item, empty);
        for (int slot : plan.clear()) inventory.setItem(slot, null);
        if (!plan.put()) return;
        if (plan.moveTo() >= 0) inventory.setItem(plan.moveTo(), inventory.getItem(SLOT));
        inventory.setItem(SLOT, create());
    }

    /**
     * What {@link #give} does to an inventory, from which of its slots hold this item ({@code item})
     * and which are empty ({@code empty}), in Bukkit's order (the 36 storage slots, hotbar first, then
     * the equipment: armor, the off hand): the other slots it's taken from ({@code clear}), then
     * whether it's put in its slot ({@code put}), what was there moving to {@code moveTo} first (-1:
     * nothing moves). It isn't put when it's there already, or when what's there has nowhere to go.
     */
    record Plan(List<Integer> clear, int moveTo, boolean put) {
        /** What's in its slot only moves to a storage slot (the first empty one, as firstEmpty finds it), not onto their armor. */
        static final int STORAGE = 36;

        static Plan of(boolean[] item, boolean[] empty) {
            List<Integer> clear = new ArrayList<>();
            for (int slot = 0; slot < item.length; slot++) {
                if (slot != SLOT && item[slot]) clear.add(slot);
            }
            if (item[SLOT]) return new Plan(clear, -1, false);
            if (empty[SLOT]) return new Plan(clear, -1, true);
            for (int slot = 0; slot < STORAGE; slot++) {
                // A copy taken away leaves its slot free.
                if (slot != SLOT && (empty[slot] || item[slot])) return new Plan(clear, slot, true);
            }
            return new Plan(clear, -1, false);
        }
    }

    /**
     * Gives it to everyone who should have it and doesn't: after anything that took it (a /clear, a
     * creative inventory). Not while their items are frozen or they're dead.
     */
    public static void sweep() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!InventorySyncListener.frozen(player) && !player.isDead()) give(player);
        }
    }
}
