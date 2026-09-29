package net.icxd.dungeons.dwarven;

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

    @Test
    void cap() {
        assertEquals(Powder.CAP, Powder.sum(Powder.CAP - 1, 5));
        assertEquals(Powder.CAP, Powder.sum(Powder.CAP, 1));
        assertEquals(5, Powder.sum(5, -3));
        assertEquals(6, Powder.sum(5, 1));
    }
}
