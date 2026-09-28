package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The dragon sets in numbers: Superior, Old and Protective Blood. */
class DragonSetsTest {
    private static final double EPSILON = 1e-9;

    /** Superior Blood: +5% of the Combat stats and Magic Find, nothing else. */
    @Test
    void superiorBlood() {
        Stats stats = new Stats().set(Stat.STRENGTH, 200).set(Stat.HEALTH, 1000).set(Stat.MAGIC_FIND, 100).set(Stat.SPEED, 150);
        DragonSets.SuperiorBlood.raise(stats);
        assertEquals(210, stats.get(Stat.STRENGTH), EPSILON);
        assertEquals(1050, stats.get(Stat.HEALTH), EPSILON);
        assertEquals(105, stats.get(Stat.MAGIC_FIND), EPSILON);
        assertEquals(150, stats.get(Stat.SPEED), EPSILON);
    }

    @Test
    void oldAndProtectiveBlood() {
        // On top of the enchantment's own: Growth V +50 Health, Protection VII +14 Defense, True Protection I +3.
        assertEquals(50, DragonSets.OldBlood.extra("growth", 5), EPSILON);
        assertEquals(14, DragonSets.OldBlood.extra("protection", 7), EPSILON);
        assertEquals(3, DragonSets.OldBlood.extra("true_protection", 1), EPSILON);
        assertEquals(0, DragonSets.OldBlood.extra("sharpness", 5), EPSILON);
        // 600 Defense on the pieces at 70% health: +30%.
        assertEquals(180, DragonSets.ProtectiveBlood.extra(600, 0.7), EPSILON);
        assertEquals(0, DragonSets.ProtectiveBlood.extra(600, 1), EPSILON);
        assertEquals(594, DragonSets.ProtectiveBlood.extra(600, 0.005), EPSILON);
    }
}
