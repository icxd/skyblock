package net.icxd.dungeons.storage;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.icxd.dungeons.user.StoredInventory;

/**
 * Storage, bags and Loadouts (see STORAGE.md), as the rest of the plugin starts and opens them: the
 * Storage menu with the Ender Chest and backpacks, Your Bags with the Accessory Bag and its Accessory
 * Powers, and Loadouts with the armor and equipment sets. What they keep is in the profile, next to the
 * inventory (see {@link StorageDocument}). Main thread.
 */
public final class Storage {
    private Storage() {
    }

    /**
     * Once, at startup: a storage menu's items are saved with the inventory, the Accessory Bag and the
     * equipment count for stats, and the tables are read off the main thread (until then there are no
     * Accessory Powers, see {@link StorageTables}).
     */
    public static void start(JavaPlugin plugin) {
        StoredInventory.captureWith((player, profile) -> {
            ItemPage page = ItemPage.current(player);
            if (page != null) page.sync(true);
        });
        AccessoryBag.register();
        Equipment.register();
        Logger log = plugin.getLogger();
        Path folder = plugin.getDataFolder().toPath().resolve(StorageTables.FOLDER);
        CompletableFuture.supplyAsync(() -> StorageTables.load(folder)).whenComplete((tables, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (error != null) {
                log.log(Level.SEVERE, "Storage: couldn't read " + folder, error);
                return;
            }
            tables.problems().forEach(p -> log.warning("Storage: " + p));
            StorageTables.set(tables);
            log.info("Storage: " + tables.powers().size() + " Accessory Powers, " + tables.upgradable().size() + " accessories with upgrades");
            // Worked out again with the tables.
            for (Player player : Bukkit.getOnlinePlayers()) AccessoryBag.changed(player);
        }));
    }

    public static void openStorage(Player player) {
        new StorageMenu(player).open(player);
    }

    /** A page of their Ender Chest, from 1. */
    public static void openEnderChest(Player player, int page) {
        EnderChestMenu.open(player, page);
    }

    /** The backpack in a slot, from 1. */
    public static void openBackpack(Player player, int slot) {
        BackpackMenu.open(player, slot);
    }

    public static void openBags(Player player) {
        new YourBagsMenu(player).open(player);
    }

    /** A bag's first page. */
    public static void openBag(Player player, Bag bag) {
        BagMenu.open(player, bag, 0);
    }

    public static void openLoadouts(Player player) {
        new LoadoutsMenu(player, 0).open(player);
    }

    /** They've left: what's kept of them here goes. */
    static void forget(UUID player) {
        AccessoryBag.forget(player);
        Equipment.forget(player);
        StorageMenu.forget(player);
    }
}
