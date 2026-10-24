package net.icxd.dungeons.recipe;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.item.VanillaItems;

/**
 * Vanilla's recipes in the inventory's crafting grid make SkyBlock items, as Hypixel's grid does (the
 * recording's first Oak Planks and Crafting Table came out of it as the SkyBlock items): the result is
 * the SkyBlock item of what vanilla would make. SkyBlock's own recipes are the crafting table's
 * ({@link CraftingTable}).
 */
public final class VanillaCrafting implements Listener {
    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        ItemStack made = VanillaItems.asSkyBlock(result);
        if (made != result) event.getInventory().setResult(made);
    }
}
