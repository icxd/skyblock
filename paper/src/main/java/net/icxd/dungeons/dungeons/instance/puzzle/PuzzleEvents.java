package net.icxd.dungeons.dungeons.instance.puzzle;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;

/** Clicks, shots and hits in puzzle rooms, handed to the run's puzzles. */
public final class PuzzleEvents implements Listener {
    private PuzzleEvents() {
    }

    public static void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new PuzzleEvents(), plugin);
    }

    /**
     * Buttons, levers and chests do only what the puzzle does with them. The off hand's click of the
     * same block is stopped too (else it would flip a lever the main hand's click left alone).
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(PlayerInteractEvent event) {
        boolean right = event.getAction() == Action.RIGHT_CLICK_BLOCK;
        if (!right && event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        RunPuzzles puzzles = RunPuzzles.in(event.getPlayer().getWorld());
        if (puzzles == null) return;
        if (event.getHand() != EquipmentSlot.HAND) {
            if (right && puzzles.isPuzzleBlock(event.getClickedBlock())) event.setCancelled(true);
            return;
        }
        if (puzzles.click(event.getPlayer(), event.getClickedBlock(), right)) event.setCancelled(true);
    }

    /**
     * The Three Weirdos (and the puzzles' other entities, which do nothing). In 26.2 a right click on
     * any entity is one packet and one event, a {@link org.bukkit.event.player.PlayerInteractAtEntityEvent},
     * which has no handler list of its own and so comes here: a click on a Weirdo's name stand (the
     * top of its head) as much as on the NPC. A handler for that event as well would hear each click
     * twice.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onClickEntity(PlayerInteractEntityEvent event) {
        RunPuzzles puzzles = RunPuzzles.in(event.getRightClicked().getWorld());
        if (puzzles == null || !puzzles.owns(event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) puzzles.click(event.getPlayer(), event.getRightClicked());
    }

    /** A left click on them works too; they can't be hurt, so it never gets as far as a damage event. */
    @EventHandler(priority = EventPriority.LOW)
    public void onHit(PrePlayerAttackEntityEvent event) {
        RunPuzzles puzzles = RunPuzzles.in(event.getAttacked().getWorld());
        if (puzzles == null || !puzzles.owns(event.getAttacked())) return;
        event.setCancelled(true);
        puzzles.click(event.getPlayer(), event.getAttacked());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHurt(EntityDamageEvent event) {
        RunPuzzles puzzles = RunPuzzles.in(event.getEntity().getWorld());
        if (puzzles != null && puzzles.owns(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(HangingBreakEvent event) {
        RunPuzzles puzzles = RunPuzzles.in(event.getEntity().getWorld());
        if (puzzles != null && puzzles.owns(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        RunPuzzles puzzles = RunPuzzles.in(event.getRightClicked().getWorld());
        if (puzzles != null && puzzles.owns(event.getRightClicked())) event.setCancelled(true);
    }

    /** Creeper Beams: an arrow in a sea lantern picks it. */
    @EventHandler
    public void onShot(ProjectileHitEvent event) {
        if (event.getHitBlock() == null || !(event.getEntity().getShooter() instanceof Player shooter)) return;
        RunPuzzles puzzles = RunPuzzles.in(event.getHitBlock().getWorld());
        if (puzzles != null) puzzles.shot(shooter, event.getHitBlock());
    }
}
