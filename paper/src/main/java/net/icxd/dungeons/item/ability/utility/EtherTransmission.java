package net.icxd.dungeons.item.ability.utility;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.bonus.SetBonuses;

/**
 * The Etherwarp Conduit's Ether Transmission: "Teleport to your targeted block up to 57 blocks away." The
 * block is the first one the line from their eyes meets (things with nothing to bump into let it through),
 * and it needs air in the two blocks over it for them to stand there: the Skyblocker mod's copy of it. They
 * land on it, in its middle, facing as they did. Without such a block nothing happens and nothing is spent
 * (UNKNOWN what Hypixel says then). Not built: the Tuned Transmission's extra blocks, and the ethermerged
 * Aspect of the Void's sneak click (an item behaviour's block, when there is one).
 */
final class EtherTransmission implements AbilityHandler {
    private static final double RANGE = 57;

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return target(player, range(block)) != null;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Block target = target(player, range(block));
        if (target == null) return;
        Location from = player.getLocation();
        Location to = target.getLocation().add(0.5, 1, 0.5);
        to.setYaw(from.getYaw());
        to.setPitch(from.getPitch());
        player.teleport(to);
        SetBonuses.teleported(player);
        player.setFallDistance(0);
        player.getWorld().playSound(to, Sound.ENTITY_ENDER_DRAGON_HURT, 1, 0.5f);
    }

    private static double range(ItemBlock block) {
        return AbilityText.after(AbilityText.plain(block.text()), "up to").orElse(RANGE);
    }

    /** The block they'd land on, within {@code range}; null if there's none with room over it. */
    static Block target(Player player, double range) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true);
        Block block = hit == null ? null : hit.getHitBlock();
        if (block == null) return null;
        return standable(block.getRelative(0, 1, 0).getType(), block.getRelative(0, 2, 0).getType()) ? block : null;
    }

    /** Room to stand on a block: air in the two over it. */
    static boolean standable(Material above, Material twoAbove) {
        return above.isAir() && twoAbove.isAir();
    }
}
