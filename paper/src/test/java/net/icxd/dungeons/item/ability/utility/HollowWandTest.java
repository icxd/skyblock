package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.ItemBlock;

/** The Hollow Wand's spells: which two clicks make which, and their costs from its text. */
class HollowWandTest {
    private static final ItemBlock BLOCK = new ItemBlock("ABILITY", HollowWand.NAME, null, "LEFT_RIGHT_CLICK", List.of(
            "&7Left Click to cast a stack of &8[&c✤&8] &7and Right Click to", "&7cast a stack of &8[&b✦&8]&7. Combine stacks to cast",
            "&7different spells!", "", "&8⊳ [&c✤✤&8] &dSpirit Spark &8(&610 ⚶&8)", "&8⊳ [&b✦✦&8] &dHollowed Rush &8(&615 ⚶&8)",
            "&8⊳ [&b✦&c✤&8] &dRaging Wind &8(&620 ⚶&8)", "&8⊳ [&c✤&b✦&8] &dIchor Pool &8(&630 ⚶&8)"), 0, 0, 0, 0, 0, 0, 0);

    /** "[✤✤] Spirit Spark", "[✦✦] Hollowed Rush", "[✦✤] Raging Wind", "[✤✦] Ichor Pool": ✤ a left click, ✦ a right one. */
    @Test
    void twoClicksMakeASpell() {
        assertEquals(HollowWand.Spell.SPIRIT_SPARK, HollowWand.Spell.of("LL"));
        assertEquals(HollowWand.Spell.HOLLOWED_RUSH, HollowWand.Spell.of("RR"));
        assertEquals(HollowWand.Spell.RAGING_WIND, HollowWand.Spell.of("RL"));
        assertEquals(HollowWand.Spell.ICHOR_POOL, HollowWand.Spell.of("LR"));
        assertNull(HollowWand.Spell.of("L"));
    }

    @Test
    void costsFromTheText() {
        assertEquals(10, HollowWand.cost(BLOCK, HollowWand.Spell.SPIRIT_SPARK));
        assertEquals(15, HollowWand.cost(BLOCK, HollowWand.Spell.HOLLOWED_RUSH));
        assertEquals(20, HollowWand.cost(BLOCK, HollowWand.Spell.RAGING_WIND));
        assertEquals(30, HollowWand.cost(BLOCK, HollowWand.Spell.ICHOR_POOL));
    }
}
