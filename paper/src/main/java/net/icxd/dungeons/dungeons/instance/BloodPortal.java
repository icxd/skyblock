package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import net.icxd.dungeons.dungeons.generation.DungeonLayout.PlacedRoom;
import net.icxd.dungeons.dungeons.paste.PastePlan;

/**
 * The Blood Room's way to the boss: every captured Blood Room has an obsidian frame in a wall (14
 * blocks round a 3 wide, 4 high opening), empty until the Watcher is done. 4.5 seconds after he
 * lets them pass, a lightning bolt strikes it and it fills with portal (recorded, 2026-10-27: the
 * twelve blocks in one go, and the bolt at the bottom of the opening); walking into it takes you to
 * the boss room ({@link DungeonRun#enterBossRoom}). The portal is only a doorway: vanilla's nether
 * travel is refused in run worlds.
 */
final class BloodPortal {
    /** Where to look for the frame: the room's blocks from just under its floor to well above its doorways. */
    private static final int FROM_Y = 60;
    private static final int TO_Y = 100;

    /** The opening's blocks, x y z, and which way the portal runs. */
    record Frame(List<int[]> opening, Axis axis) {
    }

    private final List<int[]> blocks;
    private final BoundingBox box;

    private BloodPortal(List<int[]> blocks) {
        this.blocks = blocks;
        int[] min = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] max = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int[] b : blocks) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], b[i]);
                max[i] = Math.max(max[i], b[i]);
            }
        }
        this.box = new BoundingBox(min[0], min[1], min[2], max[0] + 1, max[1] + 1, max[2] + 1);
    }

    /**
     * Lights the Blood Room's portal: the lightning, then the portal in its frame. Null if its build has no
     * frame (then there's no way on).
     */
    static BloodPortal open(World world, RunLayout layout, PlacedRoom blood) {
        List<int[]> obsidian = new ArrayList<>();
        for (int[] corner : layout.cellMins(blood)) {
            for (int x = corner[0]; x < corner[0] + PastePlan.CELL; x++) {
                for (int z = corner[1]; z < corner[1] + PastePlan.CELL; z++) {
                    for (int y = FROM_Y; y <= TO_Y; y++) {
                        if (world.getBlockAt(x, y, z).getType() == Material.OBSIDIAN) obsidian.add(new int[]{x, y, z});
                    }
                }
            }
        }
        Frame frame = find(obsidian);
        if (frame == null) return null;
        int[] low = frame.opening().get(0);
        for (int[] b : frame.opening()) if (b[1] < low[1]) low = b;
        world.strikeLightningEffect(new Location(world, centre(frame.opening(), 0), low[1], centre(frame.opening(), 2)));
        for (int[] b : frame.opening()) {
            Block block = world.getBlockAt(b[0], b[1], b[2]);
            Orientable portal = (Orientable) Material.NETHER_PORTAL.createBlockData();
            portal.setAxis(frame.axis());
            // No physics: a portal block checks its frame, and the frame's corners are open.
            block.setBlockData(portal, false);
        }
        return new BloodPortal(frame.opening());
    }

    private static double centre(List<int[]> blocks, int axis) {
        double sum = 0;
        for (int[] b : blocks) sum += b[axis] + 0.5;
        return sum / blocks.size();
    }

    /**
     * The opening inside a portal frame of obsidian (the blocks' x y z): a flat ring, along x or z, round a
     * rectangle; null if they aren't one.
     */
    static Frame find(List<int[]> obsidian) {
        if (obsidian.size() < 10) return null;
        int[] min = obsidian.get(0).clone();
        int[] max = obsidian.get(0).clone();
        for (int[] b : obsidian) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], b[i]);
                max[i] = Math.max(max[i], b[i]);
            }
        }
        Axis axis;
        if (min[2] == max[2] && max[0] - min[0] >= 2) axis = Axis.X;
        else if (min[0] == max[0] && max[2] - min[2] >= 2) axis = Axis.Z;
        else return null;
        if (max[1] - min[1] < 2) return null;
        List<int[]> opening = new ArrayList<>();
        int from = axis == Axis.X ? min[0] : min[2];
        int to = axis == Axis.X ? max[0] : max[2];
        for (int along = from + 1; along < to; along++) {
            for (int y = min[1] + 1; y < max[1]; y++) {
                opening.add(axis == Axis.X ? new int[]{along, y, min[2]} : new int[]{min[0], y, along});
            }
        }
        // The ring itself has to be there, corners aside.
        for (int along = from + 1; along < to; along++) {
            if (!has(obsidian, axis, along, min[1], min) || !has(obsidian, axis, along, max[1], min)) return null;
        }
        return new Frame(opening, axis);
    }

    private static boolean has(List<int[]> obsidian, Axis axis, int along, int y, int[] min) {
        for (int[] b : obsidian) {
            if (b[1] == y && (axis == Axis.X ? b[0] == along && b[2] == min[2] : b[2] == along && b[0] == min[0])) return true;
        }
        return false;
    }

    /** Whether a player is in the portal (any part of them). */
    boolean touches(Player player) {
        return player.getBoundingBox().overlaps(box);
    }

    List<int[]> blocks() {
        return blocks;
    }
}
