package net.icxd.dungeons.mining;

import net.icxd.dungeons.mining.blocks.MithrilBlock;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MithrilBlockTest {
    private static final MinableBlock MITHRIL = new MithrilBlock.GrayMithrilBlock();

    /** Mithril is a Dwarven Metal (the wiki's Dwarven Metal Fortune, "Affected Blocks"). */
    @Test
    void dwarvenMetal() {
        assertEquals(Stat.DWARVEN_METAL_FORTUNE, MITHRIL.fortune());
    }

    /** Too little Breaking Power: Hypixel's line, with the block's power and name. */
    @Test
    void tooWeak() {
        assertEquals("&cYou need a tool with a &aBreaking Power &cof &64&c to mine Mithril&c! Speak to &dFragilis &cby the entrance to the "
                + "Crystal Hollows to learn more!", BlockListener.tooWeak(MITHRIL));
    }

    /** Mining Fortune, and on a Dwarven Metal Dwarven Metal Fortune added to it; other blocks only the first. */
    @Test
    void fortuneOnTheBlock() {
        Stats stats = new Stats().set(Stat.MINING_FORTUNE, 120).set(Stat.DWARVEN_METAL_FORTUNE, 30);
        assertEquals(150, BlockListener.fortune(stats, MITHRIL), 1e-12);
        assertEquals(120, BlockListener.fortune(stats, new Plain(1500, -1)), 1e-12);
        assertEquals(30, BlockListener.fortune(new Stats().set(Stat.DWARVEN_METAL_FORTUNE, 30), MITHRIL), 1e-12);
    }

    /** Every 100 fortune is another drop for sure: 150 is 2 and a 50% chance of a third. */
    @Test
    void fortune() {
        assertEquals(1, BlockListener.withFortune(1, 0, 0));
        assertEquals(2, BlockListener.withFortune(1, 100, 0));
        assertEquals(3, BlockListener.withFortune(1, 200, 0));
        assertEquals(3, BlockListener.withFortune(1, 150, 0.49));
        assertEquals(2, BlockListener.withFortune(1, 150, 0.5));
        assertEquals(5, BlockListener.withFortune(2, 150, 0.99));
    }

    /**
     * "round(Block Strength x 30 / Mining Speed)" ticks, "at least 4" (the wiki's Mining Speed): Mithril's
     * 500 at 1000 Speed is 15, at 2000 7.5 so 8, at 4000 3.75 so 4, and faster than that still 4.
     */
    @Test
    void breakTicks() {
        assertEquals(150, BlockListener.breakTicks(500, 100));
        assertEquals(15, BlockListener.breakTicks(500, 1000));
        assertEquals(8, BlockListener.breakTicks(500, 2000));
        assertEquals(4, BlockListener.breakTicks(500, 4000));
        assertEquals(4, BlockListener.breakTicks(500, 10000));
        assertEquals(4, BlockListener.breakTicks(500, 30000));
    }

    /** Instant only at more than ">60x" its strength for a Dwarven Metal: 30,000 on Mithril isn't, 30,001 is. */
    @Test
    void instantBreak() {
        assertFalse(BlockListener.breaksInstantly(MITHRIL, 30000));
        assertTrue(BlockListener.breaksInstantly(MITHRIL, 30001));
        assertFalse(BlockListener.breaksInstantly(new Plain(500, -1), 1_000_000));
    }

    /** A swing every tick breaks it on the tick the formula says, its crack stages going up on the way. */
    @Test
    void stagesAddUpToTheBreak() {
        for (int ticks = BlockListener.SOFTCAP_TICKS; ticks <= 200; ticks++) {
            List<Integer> shown = new ArrayList<>();
            assertEquals(ticks, mine(ticks, shown), "break at " + ticks);
            for (int i = 0; i < shown.size(); i++) {
                assertTrue(shown.get(i) >= 0 && shown.get(i) < BlockListener.STAGES);
                if (i > 0) assertTrue(shown.get(i) > shown.get(i - 1));
            }
        }
        // 15 ticks: stages 0 to 9 at 0, 2, 3, 5, 6, 8, 9, 11, 12, 14, broken at 15.
        List<Integer> shown = new ArrayList<>();
        mine(15, shown);
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9), shown);
        // 4 ticks: several stages a swing.
        shown.clear();
        mine(4, shown);
        assertEquals(List.of(1, 3, 6, 8), shown);
    }

    /** The tick it breaks on, swinging every tick from tick 0 and counting the swings the listener would. */
    private static int mine(int ticks, List<Integer> shown) {
        int next = 0;
        int dueAt = 0;
        for (int tick = 0; tick < 10_000; tick++) {
            if (tick < dueAt) continue;
            int stage = BlockListener.reached(ticks, next);
            dueAt = tick + BlockListener.wait(ticks, stage);
            if (stage >= BlockListener.STAGES) return tick;
            shown.add(stage);
            next = stage + 1;
        }
        return -1;
    }

    private record Plain(int blockStrength, int instaBreakStrength) implements MinableBlock {
        @Override public org.bukkit.Material material() { return org.bukkit.Material.STONE; }
        @Override public int minBreakingPower() { return 1; }
    }
}
