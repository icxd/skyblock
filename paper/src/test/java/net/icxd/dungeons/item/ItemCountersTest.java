package net.icxd.dungeons.item;

import net.icxd.dungeons.item.behaviour.ItemBehaviour;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Counts kept on an item's data, and item behaviours that know their holder. */
class ItemCountersTest {
    @Test
    void counts() {
        NBTTagCompound tag = new NBTTagCompound();
        assertEquals(0, ItemCounters.get(tag, "test_kills"), 1e-9);
        assertFalse(tag.hasKey("test_kills"));
        assertEquals(1, ItemCounters.add(tag, "test_kills", 1), 1e-9);
        assertEquals(3.5, ItemCounters.add(tag, "test_kills", 2.5), 1e-9);
        assertEquals(3.5, ItemCounters.get(tag, "test_kills"), 1e-9);
        // Never below none.
        assertEquals(0, ItemCounters.add(tag, "test_kills", -10), 1e-9);
        assertEquals(0, ItemCounters.get(null, "test_kills"), 1e-9);
        // An int count the item already has (the Book of Stats' kills) reads as it is.
        tag.setInt("stats_book", 12);
        assertEquals(13, ItemCounters.add(tag, "stats_book", 1), 1e-9);
    }

    /** A behaviour that doesn't need its holder is asked as before. */
    @Test
    void whileHeldDefaults() {
        ItemBehaviour halves = new ItemBehaviour() {
            @Override
            public void whileHeld(Stats stats) {
                stats.set(Stat.CRIT_CHANCE, stats.get(Stat.CRIT_CHANCE) / 2);
            }
        };
        Stats stats = new Stats().set(Stat.CRIT_CHANCE, 80);
        halves.whileHeld(null, new NBTTagCompound(), stats);
        assertEquals(40, stats.get(Stat.CRIT_CHANCE), 1e-9);
    }
}
