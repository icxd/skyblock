package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.ItemBlock;

/** The dungeon tools' rules: the Dungeonbreaker's. */
class DungeonToolsTest {
    private static final double EPS = 1e-9;

    /** Its text's numbers: 1 a block, 20 at a time, back after 10 s, 2 a second, 20 at most. */
    @Test
    void dungeonBreakerReadsItsText() {
        ItemBlock block = new ItemBlock("ABILITY", "Dungeon Breaker", null, "DIG", List.of(
                "&7While in &cThe Catacombs&7, consume &e1⸕", "&7charge to break a block. &320 &7blocks can",
                "&7be broken at a time, and re-appear after", "&a10s&7. &e2⸕ &7charges are regenerated each", "&7second.", "",
                "&8Charges: &e20&8/&e20⸕"), 0, 0, 0, 0, 0, 0, 0);
        assertEquals(new DungeonBreaker.Rules(1, 20, 10_000, 2, 20), DungeonBreaker.Rules.of(block));
    }

    @Test
    void chargesComeBackTwoASecond() {
        assertEquals(5, DungeonBreaker.regained(5, 0, 2, 20), EPS);
        assertEquals(8, DungeonBreaker.regained(5, 1_500, 2, 20), EPS);
        assertEquals(20, DungeonBreaker.regained(5, 60_000, 2, 20), EPS);
    }
}
