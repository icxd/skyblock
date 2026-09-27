package net.icxd.dungeons.mob.goals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Player-shaped mobs walk as fast as a vanilla mob with the same movement speed attribute. */
class AvatarControlTest {
    /** speed^2 / (1 - 0.6 x 0.91): what vanilla's travel comes to on flat ground, once up to speed. */
    @Test
    void blocksPerTick() {
        assertEquals(0.1489, AvatarControl.blocksPerTick(0.26), 1e-4);
        // The Lost Adventurer's 0.36: 5.7 blocks a second.
        assertEquals(5.71, 20 * AvatarControl.blocksPerTick(0.36), 0.01);
        // A Speedy Crypt Undead's 0.35 + 0.12.
        assertEquals(9.73, 20 * AvatarControl.blocksPerTick(0.47), 0.01);
        assertEquals(0, AvatarControl.blocksPerTick(0));
    }
}
