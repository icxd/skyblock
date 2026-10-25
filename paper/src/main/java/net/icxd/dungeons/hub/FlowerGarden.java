package net.icxd.dungeons.hub;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.collection.CollectionGains;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.item.VanillaItems;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillGains;

/**
 * The flower beds by Marco's Flower House, which anyone may pick (Marco's 5 Poppies come from them),
 * as recorded (2026-10-25): a flower picked drops as its item (a Poppy, a Red Tulip) to pick up, gives
 * 1 Foraging XP, and grows back as itself; the grass between them doesn't break. The recorded six grew
 * back on a 5-second beat, on its 1st, 2nd, 1st, 2nd, 3rd and 4th beat after they were picked (2.3 to
 * 19 s): so every 5 seconds each picked one grows back or doesn't, by a chance (1 in 2 fits; Hypixel's
 * is UNKNOWN). Nothing else in the Hub breaks (see WorldListener). Main thread.
 */
@OnlyOn({ServerType.LOBBY})
public final class FlowerGarden implements Listener {
    /** The two beds, as the map has them: x 80 to 88 by z -4 to 3 before the house, x 83 to 91 by z 9 to 22 beside it. */
    static final BoundingBox BEDS = new BoundingBox(79, 73, -5, 92, 76, 23);
    static final double FORAGING_XP = 1;
    static final long REGROW_TICKS = 100;
    static final double REGROW_CHANCE = 0.5;

    /** Picked ones, as they were, till they grow back. */
    private final Map<Block, BlockData> picked = new HashMap<>();

    /** The beat they grow back on, if this is the Hub's map. */
    public FlowerGarden(Plugin plugin) {
        if (HubMap.here()) Bukkit.getScheduler().runTaskTimer(plugin, this::regrowSome, REGROW_TICKS, REGROW_TICKS);
    }

    /**
     * The Hub refuses the use of every block (HubProtection), which Paper takes as a refused click: a left
     * click would break nothing at all. One on a bed flower goes through, to be picked as it breaks.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onClick(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && clicked != null && event.getPlayer().getGameMode() != GameMode.CREATIVE
                && isBedFlower(clicked)) {
            event.setUseInteractedBlock(Event.Result.DEFAULT);
        }
    }

    /**
     * After WorldListener has cancelled it: still cancelled, so nothing vanilla drops; the flower is picked
     * here. A creative player's break stays refused (staff build, they don't pick).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || !isBedFlower(block)) return;
        event.setCancelled(true);
        BlockData flower = block.getBlockData();
        ItemStack drop = VanillaItems.asSkyBlock(new ItemStack(block.getType()));
        block.setType(Material.AIR, false);
        // From the block's middle, as vanilla drops a block's items (from its corner, they'd start in the ground and be pushed out anywhere).
        CollectionGains.fromWorld(block.getWorld().dropItemNaturally(block.getLocation().toCenterLocation(), drop));
        SkillGains.give(player, Skill.FORAGING, FORAGING_XP);
        picked.put(block, flower);
    }

    private boolean isBedFlower(Block block) {
        return HubMap.here() && block.getWorld().equals(Dungeons.getSkyBlockServer().getMainWorld()) && Tag.SMALL_FLOWERS.isTagged(block.getType())
                && BEDS.contains(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
    }

    private void regrowSome() {
        for (Block block : picked.keySet().toArray(new Block[0])) {
            if (ThreadLocalRandom.current().nextDouble() < REGROW_CHANCE) regrow(block);
        }
    }

    private void regrow(Block block) {
        BlockData flower = picked.remove(block);
        if (flower != null && block.getType().isAir()) block.setBlockData(flower, false);
    }

    /** Every picked one back, as the plugin stops: the map isn't left without them. */
    public void regrowAll() {
        for (Block block : picked.keySet().toArray(new Block[0])) regrow(block);
    }
}
