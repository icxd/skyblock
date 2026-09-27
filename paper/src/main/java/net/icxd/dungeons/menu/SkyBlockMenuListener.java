package net.icxd.dungeons.menu;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.listeners.InventorySyncListener;

/**
 * The SkyBlock Menu item ({@link SkyBlockMenuItem}): a left or right click with it in hand, or on it
 * in their inventory, opens the SkyBlock Menu, and so does dropping it (the fandom wiki's SkyBlock
 * Menu, "How to open"); a click on an entity (an NPC, a hit) is the entity's. Nothing moves it: not a
 * click, a number key, the off-hand key or a drag, and it can't go into an item frame, onto an armor
 * stand or to an allay. Death doesn't drop it; they get it back when they join (after their items are
 * put on them) and when they respawn.
 */
public final class SkyBlockMenuListener implements Listener {
    /** What a click does to the item. */
    enum Response { NONE, BLOCK, OPEN }

    /** The clicks on it that open the menu; the rest (number keys, middle, double, off-hand) only do nothing. */
    private static final Set<ClickType> OPENS = EnumSet.of(ClickType.LEFT, ClickType.RIGHT, ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT,
            ClickType.DROP, ClickType.CONTROL_DROP);

    /** When each player last had it opened (a tick): one click can come as two interactions (on a block, then in the air). */
    private final Map<UUID, Integer> openedAt = new HashMap<>();
    /** When each player last clicked or hit an entity (a tick). */
    private final Map<UUID, Integer> touchedEntityAt = new HashMap<>();

    /**
     * A click that would move it: on it ({@code onItem}), a number key swapping its slot
     * ({@code swapsItem}), or with it on the cursor.
     */
    static Response click(ClickType click, boolean onItem, boolean swapsItem, boolean onCursor) {
        if (!onItem && !swapsItem && !onCursor) return Response.NONE;
        return onItem && OPENS.contains(click) ? Response.OPEN : Response.BLOCK;
    }

    /** After the freeze (InventorySyncListener, LOWEST), before the menus' own clicks (GUIListener, NORMAL): a menu never gets it. */
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        boolean onItem = event.getClickedInventory() instanceof PlayerInventory && SkyBlockMenuItem.is(event.getCurrentItem());
        boolean swapsItem = event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0
                && SkyBlockMenuItem.is(player.getInventory().getItem(event.getHotbarButton()));
        Response response = click(event.getClick(), onItem, swapsItem, SkyBlockMenuItem.is(event.getCursor()));
        if (response == Response.NONE) return;
        event.setCancelled(true);
        if (response == Response.OPEN) open(player);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent event) {
        if (SkyBlockMenuItem.is(event.getOldCursor())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        if (!SkyBlockMenuItem.is(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        open(player);
        // A cancelled drop goes back to the hand, or anywhere there's room: into its slot, and no copy elsewhere.
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (player.isOnline() && !InventorySyncListener.frozen(player)) SkyBlockMenuItem.give(player);
        });
    }

    /**
     * Also when something else (the hub's protection) has cancelled the click on a block. Not for the
     * click in the air that comes with a click on an entity (an NPC's, a hit): that one is the entity's.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL || !SkyBlockMenuItem.is(event.getItem())) return;
        event.setCancelled(true);
        boolean air = event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR;
        Integer touched = touchedEntityAt.get(event.getPlayer().getUniqueId());
        if (air && touched != null && touched == Bukkit.getCurrentTick()) return;
        open(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        touchedEntityAt.put(event.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
        Entity entity = event.getRightClicked();
        if (!(entity instanceof ItemFrame) && !(entity instanceof Allay)) return;
        if (SkyBlockMenuItem.is(event.getPlayer().getInventory().getItem(event.getHand()))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAttack(PrePlayerAttackEntityEvent event) {
        touchedEntityAt.put(event.getPlayer().getUniqueId(), Bukkit.getCurrentTick());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (SkyBlockMenuItem.is(event.getPlayerItem())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (SkyBlockMenuItem.is(event.getMainHandItem()) || SkyBlockMenuItem.is(event.getOffHandItem())) event.setCancelled(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(SkyBlockMenuItem::is);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (player.isOnline() && !InventorySyncListener.frozen(player)) SkyBlockMenuItem.give(player);
        });
    }

    /** After PlayerListener (LOWEST) has put their stored items on them. */
    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        if (!InventorySyncListener.frozen(event.getPlayer())) SkyBlockMenuItem.give(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openedAt.remove(event.getPlayer().getUniqueId());
        touchedEntityAt.remove(event.getPlayer().getUniqueId());
    }

    /** Once a tick at most, and not while their items are frozen (on their way to another server). */
    private void open(Player player) {
        if (InventorySyncListener.frozen(player)) return;
        int tick = Bukkit.getCurrentTick();
        Integer last = openedAt.put(player.getUniqueId(), tick);
        if (last != null && last == tick) return;
        SkyBlockMenu.openLater(player);
    }
}
