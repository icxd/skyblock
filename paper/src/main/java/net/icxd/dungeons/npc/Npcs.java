package net.icxd.dungeons.npc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

/**
 * The NPCs this server has spawned, and their clicks: a right click on one, or a left one (Hypixel
 * takes either). Hub protection leaves NPCs to handle their own clicks. Main thread.
 */
public final class Npcs implements Listener {
    private static final List<Npc> SPAWNED = new ArrayList<>();

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

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        Npc npc = of(event.getRightClicked());
        if (npc == null) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) npc.click(event.getPlayer());
    }

    /** A left click. They're invulnerable, so it never gets as far as a damage event. */
    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Npc npc = of(event.getAttacked());
        if (npc == null) return;
        event.setCancelled(true);
        npc.click(event.getPlayer());
    }

    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (of(event.getRightClicked()) != null) event.setCancelled(true);
    }
}
