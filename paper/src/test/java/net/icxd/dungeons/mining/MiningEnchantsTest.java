package net.icxd.dungeons.mining;

import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.dwarven.PowderType;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.mining.blocks.MithrilBlock;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a Mithril block gives, and the mining enchantments: Efficiency, Aqua Affinity, Flowstate and Compact. */
class MiningEnchantsTest {
    private static final MinableBlock MITHRIL = new MithrilBlock.GrayMithrilBlock();

    /** Gray Mithril: 45 Mining XP and 1 Mithril Powder a block, and Enchanted Mithril for Compact. */
    @Test
    void mithrilGives() {
        assertEquals(45, MITHRIL.miningXp(), 1e-9);
        assertEquals(1, MITHRIL.powder());
        assertEquals(PowderType.MITHRIL, MITHRIL.powderType());
        assertEquals("ENCHANTED_MITHRIL", MITHRIL.compactDrop());
    }

    /** "+30" at I to "+110" at V and "+210" at X (the wiki's Efficiency). */
    @Test
    void efficiency() {
        double[] speeds = {30, 50, 70, 90, 110, 130, 150, 170, 190, 210};
        for (int level = 1; level <= 10; level++) assertEquals(speeds[level - 1], MiningTools.efficiencySpeed(level), 1e-9);
        assertEquals(0, MiningTools.efficiencySpeed(0), 1e-9);
    }

    /** Everywhere but the Hub (and not with no server at all); only on pickaxes, drills and the gauntlet. */
    @Test
    void whereEfficiencyGrantsSpeed() {
        assertFalse(MiningTools.efficiencyGrantsSpeed(ServerType.LOBBY));
        assertFalse(MiningTools.efficiencyGrantsSpeed(null));
        assertTrue(MiningTools.efficiencyGrantsSpeed(ServerType.DWARVEN_MINES));
        assertTrue(MiningTools.efficiencyGrantsSpeed(ServerType.CRIMSON_ISLE));
        assertTrue(MiningTools.efficiencyGrantsSpeed(ServerType.DUNGEONS));
        assertTrue(MiningTools.isMiningTool(item(SpecificItemType.PICKAXE)));
        assertTrue(MiningTools.isMiningTool(item(SpecificItemType.DRILL)));
        assertTrue(MiningTools.isMiningTool(item(SpecificItemType.GAUNTLET)));
        assertFalse(MiningTools.isMiningTool(item(SpecificItemType.AXE)));
        assertFalse(MiningTools.isMiningTool(null));
        // Not on a server here (no server in tests): the book's text.
        assertNull(MiningTools.efficiencyText("efficiency", item(SpecificItemType.PICKAXE), 5));
        assertNull(MiningTools.efficiencyText("fortune", item(SpecificItemType.PICKAXE), 3));
    }

    /** Aqua Affinity's "+100%" under water; nothing out of it, or without it. */
    @Test
    void aquaAffinity() {
        assertEquals(2, MiningTools.miningRate(true, true), 1e-9);
        assertEquals(1, MiningTools.miningRate(true, false), 1e-9);
        assertEquals(1, MiningTools.miningRate(false, true), 1e-9);
    }

    /** Flowstate II: +2 Mining Speed a block, 10 s, 200 blocks at most (its text's numbers). */
    @Test
    void flowstate() {
        Flowstate.Numbers numbers = Flowstate.Numbers.of("&7Consecutive blocks broken grant &6+2⸕ Mining Speed&7. Stops after &a10s &7of not "
                + "mining and caps at &a200 &7blocks.");
        assertEquals(new Flowstate.Numbers(2, 10, 200), numbers);
        assertEquals(2, Flowstate.speed(numbers, 1), 1e-9);
        assertEquals(100, Flowstate.speed(numbers, 50), 1e-9);
        assertEquals(400, Flowstate.speed(numbers, 200), 1e-9);
        assertEquals(400, Flowstate.speed(numbers, 250), 1e-9);
        assertNull(Flowstate.Numbers.of(null));
        assertNull(Flowstate.Numbers.of("&7Increases how quickly your tool breaks blocks."));
    }

    /** Compact's chance, and what it says. */
    @Test
    void compact() {
        assertTrue(Compact.procs(0.25, 0.0024));
        assertFalse(Compact.procs(0.25, 0.0025));
        assertFalse(Compact.procs(0, 0));
        assertEquals("&6&lCOMPACT! &fYou found an §9Enchanted Mithril&f!", Compact.message("§9", "Enchanted Mithril"));
    }

    private static SkyBlockItem item(SpecificItemType type) {
        return new SkyBlockItem() {
            @Override
            public String id() {
                return "TEST";
            }

            @Override
            public String name() {
                return "Test";
            }

            @Override
            public Material material() {
                return Material.STONE;
            }

            @Override
            public SpecificItemType specificItemType() {
                return type;
            }
        };
    }
}
