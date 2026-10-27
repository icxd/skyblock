package net.icxd.dungeons.dungeons.paste;

import java.util.List;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.chests.ChestType;
import net.icxd.dungeons.dungeons.paste.PastePlan.Block;
import net.icxd.dungeons.dungeons.paste.PastePlan.Box;

/**
 * A floor's boss room, where Hypixel has it: built apart from the floor's cells, and taken from a
 * world saved with the dungeon scanner ({@code /dscan world}) during a run of that floor, found by
 * the area its sidebar named ({@link ArenaLibrary}). Only its blocks in {@link #box} are used: the
 * capture's armor stands are the end of that run (its reward chests), which the run makes itself.
 *
 * @param id     names its template world ("bonzo")
 * @param area   the sidebar's area in a run of its floor, which finds its capture
 * @param box    everything it's built of (the recorded build, stone shell and all)
 * @param arrive where the Blood Room's portal sends players (recorded), and which way they face
 * @param center the middle of its floor, where its boss starts
 * @param chests where its reward chests stand once the boss is beaten
 */
public record BossArena(String id, String area, Box box, double[] arrive, float arriveYaw, double[] center, List<ChestSpot> chests) {
    /** Where a reward chest stands: its anchor (see {@link net.icxd.dungeons.dungeons.chests.ChestModel}) and the way it faces. */
    public record ChestSpot(ChestType type, double x, double y, double z, float yaw) {
    }

    /**
     * Bonzo's circus ring on Floor I, from the 2026-10-27 capture: a sand floor at y 70 with seven 3x3
     * pillars, closed in stone from x -72 to -14, y 55 to 147 and z -40 to 49 (outside the floor's 4x5
     * cells, which end at x -73 and z -41). The portal puts players at -42.5 71.5 34.5 in all three
     * recordings, facing the middle (-42.5 71 20.5), where Bonzo stands. His chests stand round the middle facing it,
     * Wood to the west, Gold to the east, Diamond to the north and Emerald to the south, and Obsidian on it facing north
     * (all three recordings, the 301 S+ one with all five).
     */
    public static final BossArena BONZO = new BossArena("bonzo", "The Catacombs (F1)",
            new Box(new Block(-72, 55, -40), new Block(-14, 147, 49)), new double[]{-42.5, 71.5, 34.5}, 180, new double[]{-42.5, 71, 20.5},
            List.of(new ChestSpot(ChestType.WOODEN, -46.5, 71, 20.5, -90), new ChestSpot(ChestType.GOLD, -38.5, 71, 20.5, 90),
                    new ChestSpot(ChestType.DIAMOND, -42.5, 71, 16.5, 0), new ChestSpot(ChestType.EMERALD, -42.5, 71, 24.5, 180),
                    new ChestSpot(ChestType.OBSIDIAN, -42, 71, 21, 180)));

    /** Every boss room there is. */
    public static final List<BossArena> ALL = List.of(BONZO);

    /** The boss room of a floor; null for one without (the Entrance ends with the Watcher) or not built yet. */
    public static BossArena of(DungeonFloor floor) {
        return floor == DungeonFloor.FLOOR_1 ? BONZO : null;
    }

    /** Whether a block is in it. */
    public boolean contains(double x, double y, double z) {
        return x >= box.min().x() && x < box.max().x() + 1 && y >= box.min().y() && y < box.max().y() + 1
                && z >= box.min().z() && z < box.max().z() + 1;
    }
}
