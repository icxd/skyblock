package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.generation.room.RoomType;
import net.icxd.dungeons.mob.MobKinds;

/** What doesn't need a world: the room multiplier, which rooms have mobs, which mobs leave skulls. */
class RoomMobsTest {
    /**
     * Every recorded opening (research mobs.md 1.3): squares already opened, the Entrance's not counted, and
     * the multiplier seen on the name tags.
     */
    @Test
    void multipliersAsRecorded() {
        // R1 Pirate and R2 Altar, the first rooms: x1.05.
        assertEquals(1.05, RoomMobs.multiplier(0), 1e-9);
        // R1 Crypt after Pirate (3): 1.10. Tomioka after 7: 1.30. Long Hall after 8: 1.35. Dragon after 9: 1.40. Catwalk after 11: 1.50.
        assertEquals(1.10, RoomMobs.multiplier(3), 1e-9);
        assertEquals(1.30, RoomMobs.multiplier(7), 1e-9);
        assertEquals(1.35, RoomMobs.multiplier(8), 1e-9);
        assertEquals(1.40, RoomMobs.multiplier(9), 1e-9);
        assertEquals(1.50, RoomMobs.multiplier(11), 1e-9);
        // R2 Criss-Cross after 5: 1.20.
        assertEquals(1.20, RoomMobs.multiplier(5), 1e-9);
        // The recorded health: 7,000 x 1.05 = 7,350; 9,000 x 1.10 = 9,900; 7,000 x 1.30 = 9,100; 1,200 x 1.35 = 1,620.
        assertEquals(7_350, Math.round(7_000 * RoomMobs.multiplier(0)));
        assertEquals(9_900, Math.round(9_000 * RoomMobs.multiplier(3)));
        assertEquals(9_100, Math.round(7_000 * RoomMobs.multiplier(7)));
        assertEquals(1_620, Math.round(1_200 * RoomMobs.multiplier(8)));
    }

    @Test
    void roomsWithMobs() {
        assertTrue(RoomMobs.hasMobs(RoomType.REGULAR));
        assertTrue(RoomMobs.hasMobs(RoomType.RARE));
        assertTrue(RoomMobs.hasMobs(RoomType.MINIBOSS));
        for (RoomType none : new RoomType[]{RoomType.START, RoomType.BLOOD, RoomType.FAIRY, RoomType.PUZZLE, RoomType.TRAP}) {
            assertFalse(RoomMobs.hasMobs(none), none.name());
        }
    }

    /**
     * A skull rises 12 times over 1.7 s, 2.5 blocks, and is gone 0.2 s after (skull 499637: 39.2 to 40.9, gone
     * at 41.1), so it starts that long before the recorded times the Undead Skeletons appeared; a bolt comes
     * 0.3 to 1.4 s before, while it rises.
     */
    @Test
    void skullsRiseBeforeTheUndeadSkeleton() {
        assertEquals(36, RoomMobs.RISE_TICKS);
        assertEquals(1.7, (RoomMobs.RISE_STEPS - 1) * RoomMobs.RISE_EVERY / 20.0, 0.15);
        assertEquals(2.5, RoomMobs.RISE_STEPS * RoomMobs.RISE_STEP, 0.05);
        assertEquals(0.3, RoomMobs.LIGHTNING_MIN / 20.0, 1e-9);
        assertEquals(1.4, RoomMobs.LIGHTNING_MAX / 20.0, 1e-9);
        assertTrue(RoomMobs.LIGHTNING_MAX < RoomMobs.RISE_TICKS && RoomMobs.LIGHTNING_MIN >= RoomMobs.RISE_EVERY, "during the rise");
    }

    /** "A killed skeleton (Scared/Skeleton Grunt/Undead) leaves a skull." */
    @Test
    void skeletonsLeaveSkulls() {
        assertTrue(RoomMobs.skeletal(MobKinds.SKELETON_GRUNT));
        assertTrue(RoomMobs.skeletal(MobKinds.SCARED_SKELETON));
        assertTrue(RoomMobs.skeletal(MobKinds.UNDEAD_SKELETON));
        assertFalse(RoomMobs.skeletal(MobKinds.ZOMBIE_GRUNT));
        assertFalse(RoomMobs.skeletal(MobKinds.CRYPT_DREADLORD));
    }
}
