package net.icxd.dungeons.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

/**
 * The NPCs this server has spawned, and their clicks: a right click on one, or a left one (Hypixel
 * takes either). Hub protection leaves NPCs to handle their own clicks. A click on one is the NPC's
 * alone: the game sends a right click on most of them (a Mannequin, a witch) on as the held item's use
 * too, and a left one's swing as a left click in the air, so what's held does nothing with it (an Aspect
 * of the End doesn't teleport). Main thread.
 */
public final class Npcs implements Listener {
    private static final List<Npc> SPAWNED = new ArrayList<>();
    /** Who last clicked an NPC (or a Fairy Soul), and on which tick. */
    private static final Map<UUID, Integer> CLICKED = new HashMap<>();

    static void add(Npc npc) {
        SPAWNED.add(npc);
    }

    static void forget(Npc npc) {
        SPAWNED.remove(npc);
    }

    /** The NPC this entity is (or is a name tag of); null for none. */
    public static Npc of(Entity entity) {
        for (Npc npc : SPAWNED) {
            if (npc.is(entity)) return npc;
        }
        return null;
    }

    /** When the plugin stops: they'd be left standing, unclickable, till the chunk unloads. */
    public static void removeAll() {
        for (Npc npc : List.copyOf(SPAWNED)) npc.remove();
    }

    /** They clicked something that's an NPC's to answer (a Fairy Soul's too): this tick, their item does nothing. */
    public static void clicked(Player player) {
        CLICKED.put(player.getUniqueId(), Bukkit.getCurrentTick());
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Npc npc = of(event.getRightClicked());
        if (npc == null) return;
        event.setCancelled(true);
        clicked(event.getPlayer());
        if (event.getHand() == EquipmentSlot.HAND) npc.click(event.getPlayer());
    }

    /** A left click. They're invulnerable, so it never gets as far as a damage event. */
    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Npc npc = of(event.getAttacked());
        if (npc == null) return;
        event.setCancelled(true);
        clicked(event.getPlayer());
        npc.click(event.getPlayer());
    }

    /** The held item's use that came with a click on an NPC, the same tick: none (abilities go by it, see PlayerListener). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseWithClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        Integer tick = CLICKED.get(event.getPlayer().getUniqueId());
        if (tick == null || tick != Bukkit.getCurrentTick()) return;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (of(event.getRightClicked()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CLICKED.remove(event.getPlayer().getUniqueId());
    }
}
