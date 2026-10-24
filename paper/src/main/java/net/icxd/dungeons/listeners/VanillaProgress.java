package net.icxd.dungeons.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;

/**
 * Vanilla's own progress, which SkyBlock doesn't have: no advancements and nothing learnt for the
 * recipe book. Both pop up as the items come in ("Advancement Made! Stone Age" with the first
 * cobblestone, "New Recipe(s) Unlocked!" with the first log), which Hypixel never shows. Crafting in
 * the inventory's grid doesn't need the recipes learnt.
 */
public final class VanillaProgress implements Listener {
    @EventHandler
    public void onCriterion(PlayerAdvancementCriterionGrantEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onDiscover(PlayerRecipeDiscoverEvent event) {
        event.setCancelled(true);
    }
}
