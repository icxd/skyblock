package net.icxd.dungeons.item.ability.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.behaviour.HeldStats;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;

/**
 * Training Weights: "The longer you hold this in your inventory, the stronger you'll become for 2 minutes after
 * removing it from your inventory! Max +50." (its text), and the wiki's Training Weights: "When the player removes it
 * from their inventory, or if it is dropped or moved to any storage without shift clicking, it will shatter. The
 * player will gain the Strength, which will last for 2 minutes. ... When the Training Weights shatter, it is
 * unrecoverable." Each minute in an online player's inventory counts on it ({@link HeldStats#TRAINING_WEIGHTS_MINUTES},
 * its "Time Held" and "Strength Gain" lines follow), and its Strength grows as the wiki's table has it ({@link
 * HeldStats#trainingWeightsStrength}). Dropped, or put into another inventory by a click or a drag (not a shift click, as the wiki says), it
 * shatters: it's gone, and they have its Strength for 2 minutes (a buff, which doesn't go with them to another
 * server: "If the player switches lobbies, the Strength bonus will be removed"). The chat line is UNKNOWN. The Quiz
 * puzzle maxing them out is LATER (there's no Quiz). Main thread.
 */
final class TrainingWeights {
    static final String ID = "TRAINING_WEIGHTS";
    private static final Pattern LASTS = Pattern.compile("become for (\\d+) minutes after removing it");

    private TrainingWeights() {
    }

    /** Its "for 2 minutes" (2 if it doesn't say). */
    private static long lastsMillis(SkyBlockItem item) {
        Matcher m = LASTS.matcher(AbilityText.plain(item.lore()));
        return (m.find() ? Long.parseLong(m.group(1)) : 2) * 60_000;
    }

    private static boolean is(ItemStack stack) {
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag != null && ID.equals(tag.getString("id"));
    }

    /** Once a minute: each one in an online player's inventory has been held a minute longer. */
    static void minute() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (InventorySyncListener.frozen(player)) continue;
            PlayerInventory inventory = player.getInventory();
            ItemStack[] contents = inventory.getStorageContents();
            for (int slot = 0; slot < contents.length; slot++) {
                if (!is(contents[slot])) continue;
                ItemStack counted = ItemCounters.add(contents[slot], player, HeldStats.TRAINING_WEIGHTS_MINUTES, 1);
                if (counted != null) inventory.setItem(slot, counted);
            }
        }
    }

    /** It shatters: gone, and its Strength is theirs for 2 minutes. */
    private static void shatter(Player player, ItemStack stack) {
        SkyBlockItem item = ItemRegistry.get(ID);
        if (item == null) return;
        int strength = HeldStats.trainingWeightsStrength(ItemNBT.read(stack));
        PlayerSession.of(player).buff(item.name(), new Stats().set(Stat.STRENGTH, strength), lastsMillis(item));
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.8f);
        player.sendMessage(Utils.color("&cYour Training Weights shattered! &7You gained &c+" + strength + "❁ Strength &7for 2 minutes."));
    }

    /** Dropped: it shatters where it would have fallen. */
    static void dropped(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (!is(stack)) return;
        event.getItemDrop().remove();
        shatter(event.getPlayer(), stack);
    }

    /**
     * A click that puts one into the other inventory open (a chest, a storage page): a placed cursor, or a hotbar
     * key's swap, not a shift click. The click is called off, and the one moved shatters.
     */
    static void clicked(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !intoStorage(event.getView().getTopInventory(), event.getClickedInventory())) return;
        InventoryAction action = event.getAction();
        ItemStack moved = switch (action) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> event.getCursor();
            case HOTBAR_SWAP -> event.getHotbarButton() < 0 ? null : player.getInventory().getItem(event.getHotbarButton());
            default -> null;
        };
        if (!is(moved)) return;
        event.setCancelled(true);
        if (action == InventoryAction.HOTBAR_SWAP) player.getInventory().setItem(event.getHotbarButton(), null);
        else player.setItemOnCursor(null);
        shatter(player, moved);
    }

    /** A drag of one over the other inventory's slots: called off, and it shatters. */
    static void dragged(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !is(event.getOldCursor())) return;
        Inventory top = event.getView().getTopInventory();
        boolean intoTop = false;
        for (int raw : event.getRawSlots()) if (raw < top.getSize()) intoTop = true;
        if (!intoTop || !intoStorage(top, top)) return;
        ItemStack moved = event.getOldCursor();
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> player.setItemOnCursor(null));
        shatter(player, moved);
    }

    /** Whether a click in {@code clicked} with {@code top} open puts something into storage (not their own crafting grid). */
    private static boolean intoStorage(Inventory top, Inventory clicked) {
        return clicked != null && clicked == top && top.getType() != InventoryType.CRAFTING && top.getType() != InventoryType.PLAYER;
    }
}
