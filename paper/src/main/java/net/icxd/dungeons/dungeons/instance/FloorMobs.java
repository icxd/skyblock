package net.icxd.dungeons.dungeons.instance;

import java.util.List;
import java.util.random.RandomGenerator;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;

/**
 * What a room's mob spot holds on a floor other than the one its data was recorded on (the Entrance's). Floor I's
 * three recorded runs (2026-10-26 and 27, 887 room mobs) have the Entrance's kinds in the Entrance's shares but for
 * the skeletons: where the Entrance has Skeleton Grunts and Scared Skeletons (27% of its room mobs, 28% of Floor I's),
 * Floor I has 72 Skeleton Grunts, 102 Skeleton Soldiers, 61 Scared Skeletons and 10 Snipers. Which spot gets which is
 * UNKNOWN (the one room recorded on both floors had other kinds at the same spots), so on Floor I a skeleton's spot
 * takes one of those as often as each was recorded.
 */
final class FloorMobs {
    private static final List<MobKind> FLOOR_1_SKELETONS = List.of(MobKinds.SKELETON_GRUNT, MobKinds.SKELETON_SOLDIER,
            MobKinds.SCARED_SKELETON, MobKinds.SNIPER);
    private static final int[] FLOOR_1_SEEN = {72, 102, 61, 10};

    private FloorMobs() {
    }

    /** The kind a spot recorded with this kind has on this floor. */
    static MobKind kind(MobKind recorded, DungeonFloor floor, RandomGenerator random) {
        boolean skeleton = recorded == MobKinds.SKELETON_GRUNT || recorded == MobKinds.SCARED_SKELETON;
        if (floor != DungeonFloor.FLOOR_1 || !skeleton) return recorded;
        int total = 0;
        for (int seen : FLOOR_1_SEEN) total += seen;
        int pick = random.nextInt(total);
        for (int i = 0; i < FLOOR_1_SEEN.length; i++) {
            pick -= FLOOR_1_SEEN[i];
            if (pick < 0) return FLOOR_1_SKELETONS.get(i);
        }
        return recorded;
    }

    /**
     * The level a spot's mob has on this floor: the recorded one if the kind has it here, else the floor's first
     * (null). The data's levels are the Entrance's: its Lv80 Angry Archaeologist is a Lv90 on Floor I, which has
     * no Lv80.
     */
    static Integer level(MobKind kind, DungeonFloor floor, Integer recorded) {
        return recorded != null && kind.variant(floor, recorded) == null ? null : recorded;
    }
}
