package net.icxd.dungeons.item.gemstone;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static net.icxd.dungeons.item.gemstone.GemstoneType.AMBER;
import static net.icxd.dungeons.item.gemstone.GemstoneType.AMETHYST;
import static net.icxd.dungeons.item.gemstone.GemstoneType.JADE;
import static net.icxd.dungeons.item.gemstone.GemstoneType.JASPER;
import static net.icxd.dungeons.item.gemstone.GemstoneType.ONYX;
import static net.icxd.dungeons.item.gemstone.GemstoneType.OPAL;
import static net.icxd.dungeons.item.gemstone.GemstoneType.RUBY;
import static net.icxd.dungeons.item.gemstone.GemstoneType.SAPPHIRE;
import static net.icxd.dungeons.item.gemstone.GemstoneType.TOPAZ;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which gemstones each special slot takes, as the wiki's Gemstone Slot page lists them. */
class GemstoneTypeTest {
    @Test
    void specialSlotsTakeWhatHypixelsDo() {
        assertEquals(List.of(RUBY, AMETHYST, SAPPHIRE, JASPER, ONYX, OPAL), GemstoneType.COMBAT.getTypes());
        assertEquals(List.of(RUBY, AMETHYST, OPAL), GemstoneType.DEFENSIVE.getTypes());
        assertEquals(List.of(JADE, AMBER, TOPAZ), GemstoneType.MINING.getTypes());
        // Universal: any gemstone.
        assertEquals(Arrays.stream(GemstoneType.values()).filter(t -> t.getTypes() == null).toList(), GemstoneType.UNIVERSAL.getTypes());
    }
}
