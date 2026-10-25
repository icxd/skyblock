package net.icxd.dungeons.island;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.collection.CollectionGains;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.hub.Hub;
import net.icxd.dungeons.item.VanillaItems;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.recipe.CraftingTable;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;

/**
 * Playing on your island, as the recording shows it: you break and place what you like there (the
 * hubs refuse both, see WorldListener), and what a block drops falls as its SkyBlock item, to be
 * picked up (recorded: the log popped out and was picked up 0.6 s later, "COLLECTION UNLOCKED Oak
 * Log" as it went in) and counted for its collection; a log gives 6 Foraging XP as it breaks
 * (recorded: "+6 Foraging (6/50)" each). A placed crafting table opens
 * SkyBlock's ("Craft Item", recorded at 00:49.7). The portal on the rock (players bridge the gap to
 * it themselves, see {@link StarterIsland}) takes you to the Hub, and dying there (into the void)
 * brings you back to the island's spawn. The Cobblestone Minion the island starts with stays put
 * (minions aren't here yet).
 *
 * <p>UNKNOWN: Skill XP for anything but logs (none is given); what falls off the trees when leaves
 * decay (vanilla's, made into SkyBlock items: the recording got an Oak Sapling).
 */
@OnlyOn({ServerType.ISLAND, ServerType.NONE})
public final class IslandRules implements Listener {
    /** Foraging XP a log gives: recorded for Oak, taken as the same for every log (UNKNOWN for the others). */
    static final double LOG_XP = 6;
    /** After a portal sends a player, it doesn't again for this long (the move takes a moment). */
    private static final long PORTAL_PAUSE_MILLIS = 3000;

    private final Map<UUID, Long> sentAt = new HashMap<>();
    private final Set<UUID> diedOnIsland = new HashSet<>();

    // Breaking and building

    /**
     * Its drops as SkyBlock items, which whoever picks them up collects; its Foraging XP for a log. Placing
     * and breaking are let through by WorldListener.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!Islands.isOwnIsland(player, block.getWorld())) return;
        event.setExpToDrop(0);
        if (player.getGameMode() == GameMode.CREATIVE) return;
        event.setDropItems(false);
        for (ItemStack drop : block.getDrops(player.getInventory().getItemInMainHand(), player)) {
            // From the block's middle, as vanilla drops them (from its corner, they'd start in the blocks round it).
            CollectionGains.fromWorld(block.getWorld().dropItemNaturally(block.getLocation().toCenterLocation(), VanillaItems.asSkyBlock(drop)));
        }
        if (Tag.LOGS.isTagged(block.getType())) SkillGains.give(player, Skill.FORAGING, LOG_XP);
    }

    /** What falls on its own (a sapling from decaying leaves) is a SkyBlock item too. */
    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item item = event.getEntity();
        if (!Islands.isIsland(item.getWorld())) return;
        ItemStack stack = item.getItemStack();
        ItemStack made = VanillaItems.asSkyBlock(stack);
        if (made != stack) item.setItemStack(made);
    }

    /** A crafting table opens SkyBlock's, unless they're sneaking with something in hand to place against it (vanilla's way). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();
        if (block == null || block.getType() != Material.CRAFTING_TABLE || !Islands.isIsland(block.getWorld())) return;
        if (player.isSneaking() && !player.getInventory().getItemInMainHand().isEmpty()) return;
        event.setUseInteractedBlock(Event.Result.DENY);
        if (!InventorySyncListener.frozen(player)) new CraftingTable(player).open(player);
    }

    // The starter island's minion: a small armor stand with its head and tools

    @EventHandler(priority = EventPriority.LOWEST)
    public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (Islands.isIsland(event.getRightClicked().getWorld()) && event.getRightClicked().isSmall()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof ArmorStand stand && stand.isSmall() && Islands.isIsland(stand.getWorld())) event.setCancelled(true);
    }

    /**
     * The starter island's capture has three invisible armor stands where Jerry's name tags float: it
     * was saved before their names came (they only come close up). Nameless, invisible and empty,
     * nobody can see or use them, and they'd stand in the way of a click on Jerry, so they go as an
     * island's entities load. Survival play can't make a stand like that, and the plugin's labels have
     * names.
     */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (!Islands.isIsland(event.getWorld())) return;
        for (Entity entity : event.getEntities()) {
            if (entity instanceof ArmorStand stand && !stand.isVisible() && stand.customName() == null && empty(stand)) stand.remove();
        }
    }

    private static boolean empty(ArmorStand stand) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (stand.canUseEquipmentSlot(slot) && !stand.getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    // Getting about

    /** The portal to the Hub. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player player = event.getPlayer();
        World world = event.getTo().getWorld();
        if (!Islands.isIsland(world)) return;
        if (portal(event.getTo().getBlock()) || portal(event.getTo().getBlock().getRelative(0, 1, 0))) {
            long now = System.currentTimeMillis();
            Long last = sentAt.get(player.getUniqueId());
            if (last != null && now - last < PORTAL_PAUSE_MILLIS) return;
            sentAt.put(player.getUniqueId(), now);
            Hub.send(player);
        }
    }

    private static boolean portal(Block block) {
        return block.getType() == Material.NETHER_PORTAL;
    }

    /** Vanilla's portals go nowhere here: the island's own handles it (onMove). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortal(PlayerPortalEvent event) {
        if (Islands.isIsland(event.getFrom().getWorld())) event.setCancelled(true);
    }

    /** A death that isn't called off (Deaths calls them off): its respawn is at the island's spawn. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (Islands.isOwnIsland(player, player.getWorld())) diedOnIsland.add(player.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (diedOnIsland.remove(player.getUniqueId()) && Islands.isOwnIsland(player, player.getWorld())) {
            event.setRespawnLocation(Islands.spawn(player.getWorld()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sentAt.remove(event.getPlayer().getUniqueId());
        diedOnIsland.remove(event.getPlayer().getUniqueId());
    }
}
