package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import net.icxd.dungeons.dungeons.generation.DoorType;
import net.icxd.dungeons.dungeons.generation.DungeonLayout.Door;

/**
 * What a room drops where its last starred mob dies (research mobs.md 1.4, the nine recorded clears),
 * laid out in this order half a block apart diagonally:
 * <ul>
 *   <li>a level V blessing (the wiki: "the final ✯ starred mob in a room"), in every recorded room bigger
 *       than 1x1 and the champion room, never in a 1x1 (6 of 9, all fitting that); which blessing is
 *       UNKNOWN, so any of the Entrance's four as often (Time comes from the Quiz, Floor IV on);</li>
 *   <li>the key of the door this room is the key room of (the wiki: "along the critical path, this drop will
 *       always guarantee a key");</li>
 *   <li>a Superboom TNT or a Revive Stone, always one of the two: 6 and 3 of the 9, so two in three a
 *       Superboom (UNKNOWN beyond that).</li>
 * </ul>
 */
final class RoomLoot {
    enum Kind {
        BLESSING, WITHER_KEY, BLOOD_KEY, SUPERBOOM_TNT, REVIVE_STONE
    }

    /** The blessings a room can drop on the Entrance. */
    static final List<String> BLESSINGS = List.of("Life", "Power", "Stone", "Wisdom");
    static final int BLESSING_LEVEL = 5;
    /** Superboom TNT in 6 of the 9 recorded drops, a Revive Stone in the other 3. */
    static final double SUPERBOOM_SHARE = 6 / 9.0;
    /** How far apart the items lie, along x and z both. */
    static final double SPACING = 0.5;

    /**
     * One item: its kind, the blessing's name ("Wisdom") or the key's door (null otherwise).
     */
    record Drop(Kind kind, String blessing, Door door) {
        /** Its name over it, as recorded. */
        String name() {
            return switch (kind) {
                case BLESSING -> "&dBlessing of " + blessing;
                case WITHER_KEY -> "&6&8Wither Key";
                case BLOOD_KEY -> "&c&cBlood Key";
                case SUPERBOOM_TNT -> "&9Superboom TNT";
                case REVIVE_STONE -> "&6Revive Stone";
            };
        }
    }

    private RoomLoot() {
    }

    /** Whether a room drops a blessing: bigger than 1x1, or a champion room. */
    static boolean blessed(int squares, boolean champion) {
        return squares > 1 || champion;
    }

    /** What a cleared room drops, in the order it's laid out. */
    static List<Drop> roll(boolean blessed, List<Door> keys, RandomGenerator random) {
        List<Drop> out = new ArrayList<>();
        if (blessed) out.add(new Drop(Kind.BLESSING, BLESSINGS.get(random.nextInt(BLESSINGS.size())), null));
        for (Door door : keys) out.add(new Drop(door.type() == DoorType.BLOOD ? Kind.BLOOD_KEY : Kind.WITHER_KEY, null, door));
        out.add(new Drop(random.nextDouble() < SUPERBOOM_SHARE ? Kind.SUPERBOOM_TNT : Kind.REVIVE_STONE, null, null));
        return out;
    }

    /** Where the i-th item lies, from where the mob died: {dx, dz}. */
    static double[] offset(int index) {
        return new double[]{index * SPACING, index * SPACING};
    }
}
