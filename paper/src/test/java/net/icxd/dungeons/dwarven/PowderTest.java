package net.icxd.dungeons.dwarven;

import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Powder on a profile: it adds up, to at most 2 billion, and an older profile gets its fields. */
class PowderTest {
    @Test
    void addsUp() {
        Document profile = new Document();
        assertEquals(1, Powder.add(profile, PowderType.MITHRIL, 1));
        assertEquals(3, Powder.add(profile, PowderType.MITHRIL, 2));
        assertEquals(3, profile.get("dwarvenMines", Document.class).get("powder", Document.class).getInteger("MITHRIL"));
        assertEquals(5, Powder.add(profile, PowderType.GEMSTONE, 5));
        // A saved number of another kind still reads.
        profile.get("dwarvenMines", Document.class).get("powder", Document.class).put("GLACITE", 7L);
        assertEquals(8, Powder.add(profile, PowderType.GLACITE, 1));
        assertEquals(0, Powder.add((Document) null, PowderType.MITHRIL, 1));
    }

    /** The drills' "Grants +20% Mithril Powder." while held: only the powder it names. */
    @Test
    void heldItemsText() {
        List<String> mithrilDrill = List.of("&7Grants &2+20% Mithril Powder&7.", "", "&7Fuel Tank: &cNot Installed");
        assertEquals(20, Powder.bonus(mithrilDrill, PowderType.MITHRIL), 1e-9);
        assertEquals(0, Powder.bonus(mithrilDrill, PowderType.GEMSTONE), 1e-9);
        assertEquals(5, Powder.bonus(List.of("&7Grants &d+5% Gemstone Powder&7, and"), PowderType.GEMSTONE), 1e-9);
        assertEquals(0, Powder.bonus(List.of("&7Grants &6+800⸕ Mining Speed"), PowderType.MITHRIL), 1e-9);
    }

    /** 1 powder with +20% is 1, and 2 a fifth of the time; +100% is 2. */
    @Test
    void withBonus() {
        assertEquals(1, Powder.withBonus(1, 0, 0));
        assertEquals(2, Powder.withBonus(1, 20, 0.19));
        assertEquals(1, Powder.withBonus(1, 20, 0.21));
        assertEquals(2, Powder.withBonus(1, 100, 0.99));
        assertEquals(0, Powder.withBonus(0, 40, 0));
    }

    @Test
    void cap() {
        assertEquals(Powder.CAP, Powder.sum(Powder.CAP - 1, 5));
        assertEquals(Powder.CAP, Powder.sum(Powder.CAP, 1));
        assertEquals(5, Powder.sum(5, -3));
        assertEquals(6, Powder.sum(5, 1));
    }
}
