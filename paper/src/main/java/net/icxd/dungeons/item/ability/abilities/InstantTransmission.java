package net.icxd.dungeons.item.ability.abilities;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Aspect of the End/Void: "Teleport 8 blocks ahead of you". How Hypixel works out where to isn't
 * published; this follows what the recorded runs show and the Skyblocker mod's copy of it ("very similar
 * to Hypixel's method"): the way is traced from the eyes, a block at a time, and the feet go where the
 * eyes' line got to, in the middle of that block (all 33 teleports of a recorded run landed in a block's
 * middle, 29 of them at a whole height), a block lower if there's room below. So looking a little down
 * doesn't put them in the floor, and they go over a block in front of their feet. Where the line runs
 * into blocks it stops short, and Hypixel says "There are blocks in the way!"; the recording shows the
 * teleport still happening then, and its mana spent, and neither when there's no room to move at all.
 * Running into the floor looking down isn't that: they land on it.
 */
public class InstantTransmission implements AbilityHandler {
    private static final double DISTANCE = 8;
    /** Finer than a block, so the line can't slip past a corner. */
    private static final double STEP = 0.25;
    static final String BLOCKED = "§cThere are blocks in the way!";

    /** Whether a block can be teleported through, by its coordinates. */
    @FunctionalInterface
    interface Blocks {
        boolean passable(int x, int y, int z);
    }

    /** Where the feet land (a block), and whether blocks cut the way short. */
    record Landing(int x, int y, int z, boolean blocked) {
    }

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        if (landing(player) != null) return true;
        player.sendMessage(BLOCKED);
        return false;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Landing landing = landing(player);
        if (landing == null) {
            player.sendMessage(BLOCKED);
            return;
        }
        Location from = player.getLocation();
        Location to = new Location(from.getWorld(), landing.x() + 0.5, landing.y(), landing.z() + 0.5, from.getYaw(), from.getPitch());
        player.teleport(to);
        player.setFallDistance(0);
        player.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
        if (landing.blocked()) player.sendMessage(BLOCKED);
    }

    private static Landing landing(Player player) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        return landing(eye.toVector(), eye.getDirection(), DISTANCE, (x, y, z) -> passable(world.getBlockAt(x, y, z)));
    }

    /**
     * Air and what has nothing to bump into, and (as the Skyblocker mod has Hypixel's) carpets and flower
     * pots, whose little shapes don't stop a teleport.
     */
    private static boolean passable(Block block) {
        return block.isPassable() || Tag.WOOL_CARPETS.isTagged(block.getType()) || Tag.FLOWER_POTS.isTagged(block.getType());
    }

    /**
     * Where the line from the eyes ({@code eye}, going {@code direction}) up to {@code distance} blocks
     * takes the feet: the last block along it that they and the head above fit in, then a block lower if
     * that fits too. Null if it can't leave the block it starts in.
     */
    static Landing landing(Vector eye, Vector direction, double distance, Blocks blocks) {
        Vector step = direction.clone().normalize().multiply(STEP);
        Vector at = eye.clone();
        int startX = at.getBlockX(), startY = at.getBlockY(), startZ = at.getBlockZ();
        int[] last = null;
        boolean blocked = false;
        for (double travelled = 0; travelled <= distance + 1e-9; travelled += STEP, at.add(step)) {
            int x = at.getBlockX(), y = at.getBlockY(), z = at.getBlockZ();
            if (last != null && x == last[0] && y == last[1] && z == last[2]) continue;
            if (!blocks.passable(x, y, z) || !blocks.passable(x, y + 1, z)) {
                // Coming down onto a block with room on it is the floor: a landing, not blocks in the way.
                boolean floor = last != null && y < last[1] && !blocks.passable(x, y, z) && blocks.passable(x, y + 1, z);
                blocked = !floor;
                break;
            }
            last = new int[] {x, y, z};
        }
        if (last == null || last[0] == startX && last[1] == startY && last[2] == startZ) return null;
        int y = blocks.passable(last[0], last[1] - 1, last[2]) ? last[1] - 1 : last[1];
        return new Landing(last[0], y, last[2], blocked);
    }
}
