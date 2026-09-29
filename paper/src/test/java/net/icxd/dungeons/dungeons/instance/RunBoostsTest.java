package net.icxd.dungeons.dungeons.instance;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.DungeonClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** A member's own boost to a run's experience (Hecatomb's). */
class RunBoostsTest {
    /** Catacombs experience and every class's that many percent more, to a tenth; Bits as they were; no boost changes nothing. */
    @Test
    void boosted() {
        Map<DungeonClass, Double> classes = new LinkedHashMap<>();
        classes.put(DungeonClass.values()[0], 100.0);
        classes.put(DungeonClass.values()[1], 25.0);
        RunRewards.Reward reward = new RunRewards.Reward(1000, classes, 5);
        RunRewards.Reward boosted = RunBoosts.boosted(reward, new RunBoosts.Boost(1, 2));
        assertEquals(1010, boosted.catacombs(), 1e-9);
        assertEquals(102, boosted.classes().get(DungeonClass.values()[0]), 1e-9);
        assertEquals(25.5, boosted.classes().get(DungeonClass.values()[1]), 1e-9);
        assertEquals(5, boosted.bits());
        assertSame(reward, RunBoosts.boosted(reward, RunBoosts.Boost.NONE));
        assertSame(reward, RunBoosts.boosted(reward, null));
    }
}
