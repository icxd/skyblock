package net.icxd.dungeons.mining;

import net.icxd.dungeons.mining.blocks.MithrilBlock;
import net.icxd.dungeons.stats.Stat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MithrilBlockTest {
    /** Mithril is a Dwarven Metal (the wiki's Dwarven Metal Fortune, "Affected Blocks"). */
    @Test
    void dwarvenMetal() {
        assertEquals(Stat.DWARVEN_METAL_FORTUNE, new MithrilBlock.GrayMithrilBlock().fortune());
    }

    /** Every 100 fortune is another drop for sure: 150 is 2 and a 50% chance of a third. */
    @Test
    void fortune() {
        assertEquals(1, BlockListener.withFortune(1, 0));
        assertEquals(2, BlockListener.withFortune(1, 100));
        int drops = BlockListener.withFortune(1, 150);
        assertEquals(2.5, drops, 0.5);
    }
}
