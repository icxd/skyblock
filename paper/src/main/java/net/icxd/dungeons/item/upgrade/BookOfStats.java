package net.icxd.dungeons.item.upgrade;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * The Book of Stats' count: a kill of a SkyBlock mob adds one to the weapon the killer holds, if it has the book
 * (see {@link Book#STATS}), and its "Kills:" line shows the new count. UNKNOWN: whether a bow's kill counts for
 * the bow it was shot from if they've switched since (here, what they hold when it dies). A farming tool's
 * book counts crops: LATER, there's no farming. Main thread.
 */
public final class BookOfStats implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(SkyBlockMobDeathEvent event) {
        Player killer = event.killer();
        if (killer == null || InventorySyncListener.frozen(killer)) return;
        PlayerInventory inventory = killer.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        ItemStack counted = counted(held, killer);
        if (counted != null) inventory.setItemInMainHand(counted);
    }

    /** The item with one more kill on its book; null if it doesn't count kills (not a SkyBlock item, no book). */
    static ItemStack counted(ItemStack stack, Player holder) {
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        if (item == null || !Book.countsKills(item, tag)) return null;
        Book.addKill(tag);
        return ItemBuilder.build(item, tag, stack.getAmount(), holder);
    }
}
