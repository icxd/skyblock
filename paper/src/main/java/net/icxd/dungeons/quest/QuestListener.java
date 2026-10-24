package net.icxd.dungeons.quest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.hub.Hub;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.recipe.CraftingTable;
import net.icxd.dungeons.region.Areas;

/**
 * What moves the quests on: Getting Started's steps as the recording did them (a log broken on the
 * island, a crafting table made in the inventory's grid, a Wooden Pickaxe at a crafting table, then
 * Jerry, see Jerry, and arriving in the Hub after the portal), and the boss bar following them about.
 */
public final class QuestListener implements Listener {
    /** The boss bar comes this long after arriving (recorded: 0.4 s after the island's teleport). */
    static final long ARRIVAL_DELAY_TICKS = 8;

    public QuestListener() {
        CraftingTable.onCraft(QuestListener::crafted);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (Tag.LOGS.isTagged(event.getBlock().getType()) && Islands.isOwnIsland(player, event.getBlock().getWorld())) {
            QuestProgress.complete(player, Quests.GETTING_STARTED, Quests.BREAK_A_LOG);
        }
    }

    /** The inventory's grid (or a vanilla crafting table, which islands don't show: they open SkyBlock's). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Material made = event.getRecipe().getResult().getType();
        if (made == Material.CRAFTING_TABLE) crafted(player, "WORKBENCH");
        else if (made == Material.WOODEN_PICKAXE) crafted(player, "WOOD_PICKAXE");
    }

    /** Something crafted, by its SkyBlock id. */
    private static void crafted(Player player, String id) {
        if ("WORKBENCH".equals(id)) QuestProgress.complete(player, Quests.GETTING_STARTED, Quests.CRAFT_A_WORKBENCH);
        else if ("WOOD_PICKAXE".equals(id)) QuestProgress.complete(player, Quests.GETTING_STARTED, Quests.CRAFT_A_WOODEN_PICKAXE);
    }

    /** After their data and items are in place (PlayerListener), and their island's welcome (Islands). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        arrived(player, player.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        arrived(event.getPlayer(), event.getPlayer().getWorld());
    }

    /**
     * Somewhere new: in the Hub, Getting Started's last step is done (the teleporter's, or however they
     * got there). A moment later the area they're in is found (the Village's title came 0.4 s after
     * arriving), and the boss bar follows the quest at home there.
     */
    private static void arrived(Player player, World world) {
        if (Hub.is(world)) QuestProgress.complete(player, Quests.GETTING_STARTED, Quests.USE_THE_TELEPORTER);
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), () -> {
            if (!player.isOnline() || !player.getWorld().equals(world)) return;
            if (Hub.is(world)) Areas.update(player, player.getLocation());
            if (Dungeons.getSkyBlockServer().getServerType() == ServerType.DUNGEON_HUB) Areas.found(player, "Dungeon Hub", "&cDungeon Hub");
            QuestBar.moved(player);
        }, ARRIVAL_DELAY_TICKS);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        QuestBar.forget(event.getPlayer());
    }
}
