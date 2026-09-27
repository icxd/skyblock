package net.icxd.dungeons.dungeons.instance.puzzle;

import org.bukkit.block.BlockFace;

import net.icxd.dungeons.dungeons.paste.PastePlan;

/**
 * Where a puzzle room's capture ended up in the world. The puzzle data is written in the capture's
 * frame (roof marker at x=0,z=0; x east, z south; y is world y, as the room is pasted at its
 * captured height); the room was turned {@code turns} times clockwise and its minimum corner put at
 * {@code minX, minZ}, the same way {@link PastePlan} pastes it.
 *
 * @param sizeX the capture's size along x (before turning)
 * @param sizeZ the capture's size along z (before turning)
 */
public record PuzzleFrame(int minX, int minZ, int sizeX, int sizeZ, int turns) {
    private static final BlockFace[] CLOCKWISE = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    /** The world block of a block of the capture: {x, y, z}. */
    public int[] block(int x, int y, int z) {
        int[] turned = PastePlan.rotate(x, z, sizeX, sizeZ, turns);
        return new int[]{minX + turned[0], y, minZ + turned[1]};
    }

    public int[] block(int[] xyz) {
        return block(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * A point of the capture (not a block) in the world: a block's corner at (x, z) spans x..x+1, so
     * a quarter turn takes the point x to {@code sizeZ - x} rather than {@code sizeZ - 1 - x}.
     */
    public double[] point(double x, double y, double z) {
        double px = x;
        double pz = z;
        int sx = sizeX;
        int sz = sizeZ;
        for (int i = 0; i < Math.floorMod(turns, 4); i++) {
            double t = px;
            px = sz - pz;
            pz = t;
            int s = sx;
            sx = sz;
            sz = s;
        }
        return new double[]{minX + px, y, minZ + pz};
    }

    /** A direction of the capture in the world (north, east, south and west turn; up and down don't). */
    public BlockFace face(BlockFace face) {
        for (int i = 0; i < 4; i++) {
            if (CLOCKWISE[i] == face) return CLOCKWISE[Math.floorMod(i + turns, 4)];
        }
        return face;
    }

    /** A yaw of the capture in the world (clockwise is +90 a turn: south 0, west 90, north 180, east 270). */
    public float yaw(float yaw) {
        return yaw + 90f * Math.floorMod(turns, 4);
    }

    /** Whether a world column is inside the room's footprint. */
    public boolean contains(double x, double z) {
        boolean sideways = Math.floorMod(turns, 2) == 1;
        int w = sideways ? sizeZ : sizeX;
        int l = sideways ? sizeX : sizeZ;
        return x >= minX && x < minX + w && z >= minZ && z < minZ + l;
    }
}
