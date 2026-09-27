package net.icxd.dungeons.dungeons.instance;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.DataMob;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.utils.Utils;

/** What players and mobs do that {@link RoomMobs} cares about: deaths, hits on waiting mobs, Superboom TNT. Main thread. */
final class RoomEvents implements Listener {
    static final String SUPERBOOM_TNT = "SUPERBOOM_TNT";

    /** A room's mob died (after its drops): the room counts it, and a skeleton leaves its skull. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(SkyBlockMobDeathEvent event) {
        RoomMobs.Tracked tracked = RoomMobs.tracked(event.mob());
        if (tracked != null) tracked.owner.died(tracked, event.location());
    }

    /** Hitting a mob that waits for its room to open (through a window, say) opens the room. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(EntityDamageByEntityEvent event) {
        Mobs.Live live = Mobs.of(event.getEntity());
        if (live == null || !(live.type() instanceof DataMob mob) || !mob.dormant() || Combat.playerBehind(event.getDamager()) == null) return;
        RoomMobs.Tracked tracked = RoomMobs.tracked(mob);
        if (tracked != null) tracked.owner.open(tracked.room.room);
    }

    /**
     * Superboom TNT, right-clicked on a block: it goes off at once where it would be placed, and one is
     * used up. Outside a dungeon run: "Cannot use this item here!" (the wiki's Superboom TNT).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) return;
        if (event.useItemInHand() == Event.Result.DENY) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) return;
        NBTTagCompound tag = ItemNBT.read(held);
        if (tag == null || !SUPERBOOM_TNT.equals(tag.getString("id"))) return;
        event.setCancelled(true);
        DungeonRun run = RunManager.of(player);
        if (run == null || !run.isStarted()) {
            player.sendMessage(Utils.color("&cCannot use this item here!"));
            return;
        }
        Block at = event.getClickedBlock().getRelative(event.getBlockFace());
        held.setAmount(held.getAmount() - 1);
        run.superboom(at);
    }

    /** Nothing comes off the rooms' stands (skulls, loot). */
    @EventHandler
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (RoomMobs.isStand(event.getRightClicked())) event.setCancelled(true);
    }
}
