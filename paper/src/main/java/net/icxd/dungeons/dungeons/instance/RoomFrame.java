package net.icxd.dungeons.dungeons.instance;

import org.bukkit.block.structure.StructureRotation;

import net.icxd.dungeons.dungeons.paste.PastePlan;

/**
 * Where a room's capture frame (its schematic's own coordinates: the roof marker at x 0, z 0) is in the
 * world, the way the room was pasted: turned {@code turns} times clockwise ({@link PastePlan#rotate}) and
 * moved to the room's min corner. Heights are the same in both (rooms are pasted at the height they were
 * captured at, which is Hypixel's). Data recorded in that frame (mob spawns, crypts, secrets) lands where
 * it was on Hypixel, whichever way the room is turned (research mobs.md 6, secrets_puzzles.md 1.2).
 *
 * @param minX  the world block of the room's min corner
 * @param sizeX the capture's size along its x (before turning)
 */
record RoomFrame(int minX, int minZ, int sizeX, int sizeZ, int turns) {
    RoomFrame {
        turns = Math.floorMod(turns, 4);
    }

    /** How the room was pasted. */
    static RoomFrame of(PastePlan.RoomPaste paste) {
        int[] size = paste.capture().size();
        int turns = Math.floorMod(paste.turns(), 4);
        int worldX = turns % 2 == 0 ? size[0] : size[2];
        int worldZ = turns % 2 == 0 ? size[2] : size[0];
        // The paste puts the schematic's middle column on its centre (see PastePlan.planRoom).
        return new RoomFrame(paste.center().x() - (worldX - 1) / 2, paste.center().z() - (worldZ - 1) / 2, size[0], size[2], turns);
    }

    /** A block of the capture in the world. */
    PastePlan.Block block(int x, int y, int z) {
        int[] p = PastePlan.rotate(x, z, sizeX, sizeZ, turns);
        return new PastePlan.Block(minX + p[0], y, minZ + p[1]);
    }

    /**
     * A point of the capture (an entity's feet, say) in the world: its block turned as blocks are, and
     * where it is in that block turned with it.
     */
    double[] point(double x, double y, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        double fx = x - bx;
        double fz = z - bz;
        for (int i = 0; i < turns; i++) {
            double t = fx;
            fx = 1 - fz;
            fz = t;
        }
        PastePlan.Block block = block(bx, 0, bz);
        return new double[]{block.x() + fx, y, block.z() + fz};
    }

    /** A yaw in the capture, turned with the room: each clockwise turn is 90 degrees more (south becomes west). */
    float yaw(float yaw) {
        return (yaw + 90 * turns) % 360;
    }

    /** For turning block states placed in the room ({@link org.bukkit.block.data.BlockData#rotate}). */
    StructureRotation rotation() {
        return switch (turns) {
            case 1 -> StructureRotation.CLOCKWISE_90;
            case 2 -> StructureRotation.CLOCKWISE_180;
            case 3 -> StructureRotation.COUNTERCLOCKWISE_90;
            default -> StructureRotation.NONE;
        };
    }
}
