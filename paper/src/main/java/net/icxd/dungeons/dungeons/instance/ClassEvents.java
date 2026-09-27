package net.icxd.dungeons.dungeons.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

/**
 * Using a class's abilities in a run (MCW Classes: "Right clicking or ctrl-dropping activates your
 * normal ability, while left clicking or dropping activates your ultimate ability"; Mort: "Right click
 * the Orb for spells, and Left click (or Drop) to use your Ultimate!"). The drop key never drops what
 * you hold while the run is on (items dropped from the inventory screen still go). Telling ctrl+drop
 * from drop, and seeing the key pressed with nothing in hand (no item, no drop event), needs the
 * packet, so it takes PacketEvents; without it every drop is the ultimate and an empty hand does nothing.
 */
final class ClassEvents implements Listener {
    /** Players whose last drop key press was ctrl+drop (the whole stack), set off the main thread. */
    private static final Set<UUID> STACK_DROPS = ConcurrentHashMap.newKeySet();

    /** A key press's packet is looked at this many ticks later, once the server has dealt with it. */
    private static final int KEY_CHECK_DELAY = 2;

    private final RunManager manager;
    private final Plugin plugin;
    /** The tick each player last dropped something from the inventory screen. */
    private final Map<UUID, Integer> screenDrops = new HashMap<>();
    /** The tick each player last used the drop key on something they held. */
    private final Map<UUID, Integer> keyUses = new HashMap<>();

    ClassEvents(RunManager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    /** Listens for the drop keys' packets, if PacketEvents is here. */
    void watchDropKeys() {
        if (Bukkit.getPluginManager().isPluginEnabled("packetevents")) DropKeys.register(this);
    }

    /** A drop key was pressed (seen in its packet): with nothing in hand there's no drop event, so it's used here. */
    private void keyPressed(UUID id, boolean stack) {
        Player player = Bukkit.getPlayer(id);
        if (player == null || !player.getInventory().getItemInMainHand().isEmpty()) return;
        Integer used = keyUses.get(id);
        if (used != null && Bukkit.getCurrentTick() - used <= KEY_CHECK_DELAY + 1) return;
        DungeonRun run = running(player);
        if (run == null) return;
        STACK_DROPS.remove(id);
        if (stack) run.classes().ability(player);
        else run.classes().ultimate(player);
    }

    private DungeonRun running(Player player) {
        DungeonRun run = manager.runOf(player);
        return run != null && run.phase() == DungeonRun.Phase.RUNNING && !run.ghosts().isGhost(player.getUniqueId()) ? run : null;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onScreenDrop(InventoryClickEvent event) {
        InventoryAction action = event.getAction();
        boolean drop = action == InventoryAction.DROP_ALL_CURSOR || action == InventoryAction.DROP_ONE_CURSOR
                || action == InventoryAction.DROP_ALL_SLOT || action == InventoryAction.DROP_ONE_SLOT;
        if (drop) screenDrops.put(event.getWhoClicked().getUniqueId(), Bukkit.getCurrentTick());
    }

    /** The drop key: the ultimate, or with ctrl the ability. */
    @EventHandler(priority = EventPriority.LOW)
    public void onDropKey(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        // Whatever happens to this drop, the key press is used up.
        boolean stack = STACK_DROPS.remove(player.getUniqueId());
        DungeonRun run = running(player);
        if (run == null || event.isCancelled()) return;
        Integer screen = screenDrops.get(player.getUniqueId());
        if (screen != null && screen == Bukkit.getCurrentTick()) return;
        event.setCancelled(true);
        keyUses.put(player.getUniqueId(), Bukkit.getCurrentTick());
        if (stack) run.classes().ability(player);
        else run.classes().ultimate(player);
    }

    /** The Dungeon Orb: right click for the ability, left click for the ultimate (it's a head: never placed). */
    @EventHandler(priority = EventPriority.LOW)
    public void onOrb(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !RunClasses.isOrb(event.getItem())) return;
        event.setCancelled(true);
        DungeonRun run = running(event.getPlayer());
        if (run == null) return;
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) run.classes().ability(event.getPlayer());
        else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) run.classes().ultimate(event.getPlayer());
    }

    /**
     * An ability's arrow (Explosive Shot, Rapid Fire) landed: it does its own damage, not a vanilla hit.
     * Not when the hit was called off (it went through a ghost).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onArrowLand(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;
        DungeonRun run = manager.runOf(player);
        if (run != null && run.classes().arrowLanded(event.getEntity(), event.getHitEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        screenDrops.remove(event.getPlayer().getUniqueId());
        keyUses.remove(event.getPlayer().getUniqueId());
        STACK_DROPS.remove(event.getPlayer().getUniqueId());
    }

    /** Only loaded with PacketEvents there. Off the main thread. */
    private static final class DropKeys implements PacketListener {
        private final ClassEvents events;

        private DropKeys(ClassEvents events) {
            this.events = events;
        }

        static void register(ClassEvents events) {
            PacketEvents.getAPI().getEventManager().registerListener(new DropKeys(events), PacketListenerPriority.MONITOR);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event) {
            if (event.getPacketType() != PacketType.Play.Client.PLAYER_DIGGING) return;
            DiggingAction action = new WrapperPlayClientPlayerDigging(event).getAction();
            UUID id = event.getUser() == null ? null : event.getUser().getUUID();
            if (id == null || (action != DiggingAction.DROP_ITEM_STACK && action != DiggingAction.DROP_ITEM)) return;
            boolean stack = action == DiggingAction.DROP_ITEM_STACK;
            if (stack) STACK_DROPS.add(id);
            else STACK_DROPS.remove(id);
            Bukkit.getScheduler().runTaskLater(events.plugin, () -> events.keyPressed(id, stack), KEY_CHECK_DELAY);
        }
    }
}
