package net.icxd.dungeons.item.gemstone;

import static net.icxd.dungeons.item.gemstone.GemstoneQuality.FINE;
import static net.icxd.dungeons.item.gemstone.GemstoneQuality.FLAWLESS;
import static net.icxd.dungeons.item.gemstone.GemstoneType.COMBAT;
import static net.icxd.dungeons.item.gemstone.GemstoneType.JADE;
import static net.icxd.dungeons.item.gemstone.GemstoneType.JASPER;
import static net.icxd.dungeons.item.gemstone.GemstoneType.PERIDOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.gemstone.GemSlots.Slot;
import net.icxd.dungeons.item.gemstone.GemstoneTable.Stone;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Gems on items, on made-up items and a made-up table (Hypixel's is private): the data, the lore's glyphs, the stats. */
class GemSlotsTest {
    /** Like a Shadow Assassin Chestplate: a Jasper slot to pay for, then a free Combat one. */
    private static final DataItem CHESTPLATE = item("""
            "TEST_CHESTPLATE":{"gemstone_slots":[{"costs":[{"coins":50000},{"amount":20,"item":"FINE_JASPER_GEM"}],"type":"JASPER"},\
            {"type":"COMBAT"}],"material":"LEATHER_CHESTPLATE","name":"Test Chestplate","rarity":"LEGENDARY","type":"CHESTPLATE"}""");
    private static final DataItem RING = item("""
            "POWER_RING":{"gemstone_slots":[{"type":"JASPER"}],"material":"PAPER","name":"Test Ring","rarity":"MYTHIC","type":"ACCESSORY"}""");
    private static final DataItem CHISEL = item("""
            "TEST_CHISEL":{"gemstone_slots":[{"type":"CHISEL"}],"material":"SHEARS","name":"Test Chisel","rarity":"EPIC"}""");

    private static DataItem item(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Jasper gives Strength: Fine 1 a rarity, 5 at Legendary, 7 at Mythic; Jade Mining Fortune with a Divine value. */
    private static GemstoneTable table() {
        Map<Rarity, Double> fine = Map.of(Rarity.COMMON, 1.0, Rarity.LEGENDARY, 5.0, Rarity.MYTHIC, 7.0);
        return new GemstoneTable(Map.of(
                JASPER, new Stone(Stat.STRENGTH, Map.of(FINE, fine, FLAWLESS, Map.of(Rarity.MYTHIC, 12.0))),
                JADE, new Stone(Stat.MINING_FORTUNE, Map.of(FINE, Map.of(Rarity.MYTHIC, 25.0, Rarity.DIVINE, 30.0))),
                PERIDOT, new Stone(Stat.FARMING_FORTUNE, Map.of(FINE, Map.of(Rarity.EPIC, 4.0)))),
                Map.of(FINE, 10000.0), Map.of(FINE, 50), Map.of(), Map.of());
    }

    @Test
    void gemIds() {
        Gem fineJasper = new Gem(JASPER, FINE);
        assertEquals(fineJasper, Gem.of("FINE_JASPER_GEM"));
        assertEquals("FINE_JASPER_GEM", fineJasper.id());
        assertEquals("❁ Fine Jasper Gemstone", fineJasper.name());
        assertEquals(new Gem(GemstoneType.AQUAMARINE, GemstoneQuality.PERFECT), Gem.of("PERFECT_AQUAMARINE_GEM"));
        // A slot type isn't a gem; nor are other items.
        assertNull(Gem.of("FINE_COMBAT_GEM"));
        assertNull(Gem.of("HYPERION"));
        assertNull(Gem.of("GEMSTONE_CHAMBER"));
        assertNull(Gem.of(null));
    }

    @Test
    void whatASlotTakes() {
        assertTrue(COMBAT.accepts(JASPER));
        assertFalse(COMBAT.accepts(JADE));
        assertTrue(JASPER.accepts(JASPER));
        assertFalse(JASPER.accepts(JADE));
        assertFalse(COMBAT.accepts(COMBAT));
        for (GemstoneType gem : GemstoneType.values()) assertEquals(gem.gem(), GemstoneType.UNIVERSAL.accepts(gem), gem.name());
        assertEquals(List.of(JASPER), JASPER.accepted());
        assertEquals(Arrays.asList(GemstoneType.ONYX, GemstoneType.AQUAMARINE, GemstoneType.CITRINE, PERIDOT), GemstoneType.CHISEL.accepted());
    }

    @Test
    void newItemsSlots() {
        // Before the item's data has its slots (never built): locked where there's a cost.
        List<Slot> slots = GemSlots.of(CHESTPLATE, new NBTTagCompound());
        assertEquals(2, slots.size());
        assertTrue(slots.get(0).locked());
        assertFalse(slots.get(1).locked());
        assertEquals(2, slots.get(0).costs().size());
        assertTrue(slots.get(1).empty());
        assertEquals("&7Gemstones: &8[❁] &8[&7⚔&8]", GemSlots.line(CHESTPLATE, new NBTTagCompound()));
        assertNull(GemSlots.line(item("""
                "TEST_PLAIN":{"material":"STICK","name":"Test Plain"}"""), new NBTTagCompound()));
    }

    @Test
    void unlockApplyRemove() {
        NBTTagCompound tag = new NBTTagCompound();
        Gem jasper = new Gem(JASPER, FINE);
        // Only the open Combat slot takes it.
        assertEquals(1, GemSlots.target(GemSlots.of(CHESTPLATE, tag), JASPER));
        assertEquals(-1, GemSlots.target(GemSlots.of(CHESTPLATE, tag), JADE));
        GemSlots.apply(tag, CHESTPLATE, 1, jasper);
        assertEquals("&7Gemstones: &8[❁] &9[&d⚔&9]", GemSlots.line(CHESTPLATE, tag));
        // The list is made up to the slot as ItemBuilder makes it: the first locked.
        NBTTagList list = tag.getList(GemSlots.KEY, 10);
        assertEquals(2, list.size());
        assertTrue(list.get(0).getBoolean("locked"));
        assertEquals("JASPER", list.get(1).getString("gem"));
        assertEquals("FINE", list.get(1).getString("quality"));
        assertEquals(-1, GemSlots.target(GemSlots.of(CHESTPLATE, tag), JASPER));

        GemSlots.unlock(tag, CHESTPLATE, 0);
        assertEquals("&7Gemstones: &8[&7❁&8] &9[&d⚔&9]", GemSlots.line(CHESTPLATE, tag));
        assertEquals(0, GemSlots.target(GemSlots.of(CHESTPLATE, tag), JASPER));
        GemSlots.apply(tag, CHESTPLATE, 0, new Gem(JASPER, FLAWLESS));
        assertEquals("&7Gemstones: &5[&d❁&5] &9[&d⚔&9]", GemSlots.line(CHESTPLATE, tag));

        GemSlots.remove(tag, CHESTPLATE, 1);
        Slot combat = GemSlots.of(CHESTPLATE, tag).get(1);
        assertTrue(combat.empty());
        assertEquals("&8[&7⚔&8]", GemSlots.glyph(combat));
    }

    @Test
    void aGemOpensItsSlot() {
        // Hypixel's older items have gems in slots never paid for: open, whatever the data's flag says.
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        NBTTagCompound slot = new NBTTagCompound();
        slot.setBoolean("locked", true);
        slot.setString("gem", "JASPER");
        slot.setString("quality", "FINE");
        list.add(slot);
        tag.set(GemSlots.KEY, list);
        Slot first = GemSlots.of(CHESTPLATE, tag).getFirst();
        assertFalse(first.locked());
        assertEquals(new Gem(JASPER, FINE), first.gem());
        assertTrue(first.fits());
    }

    @Test
    void stats() {
        GemstoneTable table = table();
        NBTTagCompound tag = new NBTTagCompound();
        GemSlots.unlock(tag, CHESTPLATE, 0);
        GemSlots.apply(tag, CHESTPLATE, 0, new Gem(JASPER, FINE));
        GemSlots.apply(tag, CHESTPLATE, 1, new Gem(JASPER, FINE));
        // Legendary: 5 each.
        assertEquals(new Stats().set(Stat.STRENGTH, 10), GemSlots.stats(table, CHESTPLATE, tag));
        // Recombobulated, Mythic's: the recorded Shadow Assassin pieces' 2 Fine Jasper, 14.
        tag.setBoolean("recombobulated", true);
        assertEquals(new Stats().set(Stat.STRENGTH, 14), GemSlots.stats(table, CHESTPLATE, tag));
        // Nothing until the table's read.
        assertEquals(new Stats(), GemSlots.stats(null, CHESTPLATE, tag));
    }

    @Test
    void halvedOnThePowerLine() {
        NBTTagCompound tag = new NBTTagCompound();
        GemSlots.apply(tag, RING, 0, new Gem(JASPER, FINE));
        assertTrue(GemSlots.halved(RING));
        assertEquals(new Stats().set(Stat.STRENGTH, 3.5), GemSlots.stats(table(), RING, tag));
    }

    @Test
    void aChiselsGemGivesNoStat() {
        NBTTagCompound tag = new NBTTagCompound();
        GemSlots.apply(tag, CHISEL, 0, new Gem(PERIDOT, FINE));
        assertEquals(new Stats(), GemSlots.stats(table(), CHISEL, tag));
        assertEquals("&7Gemstones: &9[&2❥&9]", GemSlots.line(CHISEL, tag));
    }

    @Test
    void aGemItsSlotNoLongerTakes() {
        // A Jade put in the Jasper slot (the item's slots changed since): shown, but gives nothing.
        NBTTagCompound tag = new NBTTagCompound();
        GemSlots.apply(tag, CHESTPLATE, 0, new Gem(JADE, FINE));
        Slot slot = GemSlots.of(CHESTPLATE, tag).getFirst();
        assertFalse(slot.fits());
        assertEquals("&9[&a❁&9]", GemSlots.glyph(slot));
        assertEquals(new Stats(), GemSlots.stats(table(), CHESTPLATE, tag));
    }

    @Test
    void valuesAboveMythic() {
        GemstoneTable table = table();
        Gem jade = new Gem(JADE, FINE), jasper = new Gem(JASPER, FINE);
        assertEquals(30, table.value(jade, Rarity.DIVINE));
        // UNKNOWN on Hypixel: Mythic's for the rest at Divine, and for all at Special and Very Special.
        assertEquals(7, table.value(jasper, Rarity.DIVINE));
        assertEquals(25, table.value(jade, Rarity.SPECIAL));
        assertEquals(7, table.value(jasper, Rarity.VERY_SPECIAL));
        // A rarity the table skips takes the one below.
        assertEquals(1, table.value(jasper, Rarity.RARE));
        assertEquals(0, table.value(new Gem(GemstoneType.RUBY, FINE), Rarity.RARE));
    }
}
