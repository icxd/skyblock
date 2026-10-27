package net.icxd.dungeons.dungeons.chests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.paste.BossArena;

class ChestModelTest {
    private static BossArena.ChestSpot spot(ChestType type) {
        return BossArena.BONZO.chests().stream().filter(s -> s.type() == type).findFirst().orElseThrow();
    }

    /** Where a part stands, built at its chest's spot in Bonzo's ring. */
    private static double[] at(ChestType type, int part) {
        BossArena.ChestSpot spot = spot(type);
        ChestModel.Part p = ChestModel.parts(type).get(part);
        double[] xz = ChestModel.offset(p.across(), p.forward(), spot.yaw());
        return new double[]{spot.x() + xz[0], spot.y() + p.up(), spot.z() + xz[1]};
    }

    private static void assertAt(double x, double y, double z, double[] at) {
        assertEquals(x, at[0], 0.002);
        assertEquals(y, at[1], 0.002);
        assertEquals(z, at[2], 0.002);
    }

    /** Built where Bonzo's are, the stands are where they were recorded (2026_10_26_00_59_04_0, 11:46.7). */
    @Test
    void asRecorded() {
        assertAt(-46.562, 69.594, 20.438, at(ChestType.WOODEN, 0));
        assertAt(-46.5, 71.313, 20.5, at(ChestType.WOODEN, 3));
        assertAt(-46.344, 70.281, 20.469, at(ChestType.WOODEN, 4));
        assertAt(-38.656, 70.313, 20.438, at(ChestType.GOLD, 4));
        assertAt(-42.625, 70.281, 16.688, at(ChestType.DIAMOND, 4));
        assertAt(-42.375, 70.625, 16.5, at(ChestType.DIAMOND, 8));
        assertAt(-42.5, 70.281, 24.375, at(ChestType.EMERALD, 1));
        assertAt(-41.969, 70.344, 20.594, at(ChestType.OBSIDIAN, 1));
        assertAt(-42.125, 71.781, 21.344, at(ChestType.OBSIDIAN, 20));
    }

    @Test
    void parts() {
        assertEquals(5, ChestModel.parts(ChestType.WOODEN).size());
        assertEquals(5, ChestModel.parts(ChestType.GOLD).size());
        assertEquals(9, ChestModel.parts(ChestType.DIAMOND).size());
        assertEquals(9, ChestModel.parts(ChestType.EMERALD).size());
        assertEquals(21, ChestModel.parts(ChestType.OBSIDIAN).size());
        assertFalse(ChestModel.recorded(ChestType.BEDROCK));
        for (ChestType type : List.of(ChestType.WOODEN, ChestType.GOLD, ChestType.DIAMOND, ChestType.EMERALD, ChestType.OBSIDIAN)) {
            List<ChestModel.Part> parts = ChestModel.parts(type);
            // One latch (the small head tilted forward), in front; a lid.
            assertEquals(1, parts.stream().filter(p -> p.headPitch() == -16).count(), type.name());
            assertTrue(parts.stream().filter(p -> p.headPitch() == -16).allMatch(p -> p.forward() > 0 && p.small() && p.head() != null));
            assertTrue(parts.stream().anyMatch(ChestModel.Part::lid), type.name());
        }
        assertEquals(1, ChestModel.parts(ChestType.WOODEN).stream().filter(ChestModel.Part::lid).count());
        assertEquals("5a736eb1a7ce5a5f5d5b168931fa31396386c160568b41964ba86cdb9ed6be", ChestModel.parts(ChestType.WOODEN).get(3).head());
    }

    /** The Wood chest's three rewards came out at -44.5 20.5, -47.531 22.219 and -47.531 18.75 (2026_10_27_06_28_42_1). */
    @Test
    void rewardSpots() {
        BossArena.ChestSpot wood = spot(ChestType.WOODEN);
        double[][] recorded = {{-44.5, 20.5}, {-47.531, 22.219}, {-47.531, 18.75}};
        for (int i = 0; i < 3; i++) {
            double[] xz = ChestModel.rewardSpot(i, 3, wood.yaw());
            assertEquals(recorded[i][0], wood.x() + xz[0], 0.04);
            assertEquals(recorded[i][1], wood.z() + xz[1], 0.04);
        }
    }

    @Test
    void bonzosChests() {
        assertEquals(5, BossArena.BONZO.chests().size());
        assertEquals(0, ChestType.DIAMOND.minScore());
        assertEquals(230, ChestType.EMERALD.minScore());
        assertEquals(270, ChestType.OBSIDIAN.minScore());
        assertEquals("Wood", ChestType.WOODEN.title());
    }
}
