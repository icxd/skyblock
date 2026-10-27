package net.icxd.dungeons.dungeons.chests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Kind;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Reward;
import net.icxd.dungeons.dungeons.chests.ChestLoot.Table;
import net.icxd.dungeons.item.cost.essence.EssenceType;

class ChestLootTest {
    /** Picks these, in order, for the weighted rolls. */
    private static RandomGenerator picks(int... values) {
        Deque<Integer> queue = new ArrayDeque<>();
        for (int v : values) queue.add(v);
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int nextInt(int bound) {
                int v = queue.isEmpty() ? 0 : queue.poll();
                if (v >= bound) throw new AssertionError(v + " out of " + bound);
                return v;
            }
        };
    }

    private static Table table(ChestType type) {
        return ChestLoot.table(DungeonFloor.FLOOR_1, type);
    }

    @Test
    void floorOne() {
        assertEquals(0, table(ChestType.WOODEN).cost());
        assertEquals(25_000, table(ChestType.GOLD).cost());
        assertEquals(50_000, table(ChestType.DIAMOND).cost());
        assertEquals(100_000, table(ChestType.EMERALD).cost());
        assertEquals(250_000, table(ChestType.OBSIDIAN).cost());
        assertEquals(110, table(ChestType.WOODEN).quality());
        assertEquals(300, table(ChestType.OBSIDIAN).quality());
        assertEquals(10, table(ChestType.WOODEN).entries().size());
        assertEquals(19, table(ChestType.OBSIDIAN).entries().size());
        assertNull(table(ChestType.BEDROCK));
        assertNull(ChestLoot.table(DungeonFloor.FLOOR_2, ChestType.WOODEN));
    }

    /** The recorded chests' qualities (a Treasure Talisman, twice), and MCW's S+ with every bonus (168 to 380). */
    @Test
    void quality() {
        assertEquals(112, ChestLoot.quality(110, false, 1.01, 0));
        assertEquals(164, ChestLoot.quality(160, false, 1.01, 0));
        assertEquals(204, ChestLoot.quality(200, false, 1.01, 0));
        assertEquals(110, ChestLoot.quality(110, false, 1, 0));
        // S+, a Treasure Artifact, Boss Luck +10 and the Catacombs Box's +13.
        int[] base = {110, 160, 200, 240, 300};
        int[] max = {168, 225, 269, 314, 380};
        for (int i = 0; i < base.length; i++) assertEquals(max[i], ChestLoot.quality(base[i], true, 1.03, 23), "base " + base[i]);
        assertEquals(1.02, ChestLoot.treasure(Set.of("TREASURE_TALISMAN", "TREASURE_RING", "WOLF_TALISMAN")));
        assertEquals(1.0, ChestLoot.treasure(Set.of()));
    }

    /** Wood at 112: Feather Falling VI (80), three Wither Essence (30), and the 2 left over as Undead. */
    @Test
    void recordedWood() {
        // Of 133: Brooch 1, Bank 20, Jerry 1, then Feather Falling's 40.
        List<Reward> rewards = ChestLoot.roll(table(ChestType.WOODEN), 112, picks(22, 0, 0, 0));
        assertEquals(List.of(new Reward(Kind.BOOK, "feather_falling", 6, 0), new Reward(Kind.ESSENCE, "UNDEAD", 5, 0),
                new Reward(Kind.ESSENCE, "WITHER", 7, 0)), rewards);
        assertEquals(0, ChestLoot.cost(table(ChestType.WOODEN), rewards));
    }

    /** Gold at 164: Combo I (120), four Wither Essence, four Undead left over. */
    @Test
    void recordedGold() {
        // Of 197: Hot Potato Book 5, then Combo's 20.
        List<Reward> rewards = ChestLoot.roll(table(ChestType.GOLD), 164, picks(5, 0, 0, 0, 0));
        assertEquals(List.of(new Reward(Kind.BOOK, "combo", 1, 0), new Reward(Kind.ESSENCE, "UNDEAD", 9, 0),
                new Reward(Kind.ESSENCE, "WITHER", 9, 0)), rewards);
        assertEquals(25_000, ChestLoot.cost(table(ChestType.GOLD), rewards));
    }

    /** Diamond at 204: Combo I, Rejuvenate I, and 4 left over as Undead. */
    @Test
    void recordedDiamond() {
        // Of 175: Mask 1, Red Nose 2, Balloon Snake 2, Hot Potato Book 6, then Combo's 20. Then of 101 (no Mask or Red Nose
        // or Balloon Snake or Hot Potato Book, quality 84 left): Rejuvenate is first.
        List<Reward> rewards = ChestLoot.roll(table(ChestType.DIAMOND), 204, picks(11, 0));
        assertEquals(List.of(new Reward(Kind.BOOK, "combo", 1, 0), new Reward(Kind.BOOK, "rejuvenate", 1, 0),
                new Reward(Kind.ESSENCE, "UNDEAD", 12, 0), new Reward(Kind.ESSENCE, "WITHER", 6, 0)), rewards);
    }

    @Test
    void costs() {
        Table diamond = table(ChestType.DIAMOND);
        assertEquals(1_050_000, ChestLoot.cost(diamond, List.of(new Reward(Kind.ITEM, "BONZO_MASK", 1, 1_000_000))));
    }

    /** MCW's average chances (no bonuses) come out of the rolling: Gold's Rejuvenate 43.539%, Feather Falling 40.6968%. */
    @Test
    void averagesLikeTheWiki() {
        Table gold = table(ChestType.GOLD);
        SplittableRandom random = new SplittableRandom(1);
        int runs = 100_000;
        int rejuvenate = 0;
        int featherFalling = 0;
        for (int i = 0; i < runs; i++) {
            for (Reward r : ChestLoot.roll(gold, 160, random)) {
                if ("rejuvenate".equals(r.id())) rejuvenate++;
                if ("feather_falling".equals(r.id())) featherFalling++;
            }
        }
        assertEquals(43.539, 100.0 * rejuvenate / runs, 0.6);
        assertEquals(40.6968, 100.0 * featherFalling / runs, 0.6);
    }

    @Test
    void lines() {
        assertEquals("&dUndead Essence &8x12", ChestLoot.essenceLine(EssenceType.UNDEAD, 12));
        assertEquals("&dWither Essence &8x7", ChestLoot.essenceLine(EssenceType.WITHER, 7));
        assertEquals("&fEnchanted Book (Feather Falling VI)", ChestLoot.bookLine("Feather Falling", 6, false));
        assertEquals("&fEnchanted Book (&d&lCombo I&f)", ChestLoot.bookLine("Combo", 1, true));
        assertEquals("&650,000 Coins", ChestLoot.costLine(50_000));
        assertEquals("&aFREE", ChestLoot.costLine(0));
        assertEquals("&f&lWOOD CHEST REWARDS", ChestType.WOODEN.rewardsTitle());
        assertNotNull(new Reward(Kind.ESSENCE, "WITHER", 1, 0).essence());
    }
}
