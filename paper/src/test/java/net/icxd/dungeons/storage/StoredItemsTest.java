package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/** Counting and taking items by id from stored slots (see {@link StoredItems}), with "ID:amount" for blobs. */
class StoredItemsTest {
    private static final StoredItems.Blobs PLAIN = new StoredItems.Blobs() {
        @Override
        public StoredItems.Stack read(Object blob) {
            String[] parts = ((String) blob).split(":");
            // "?" is an item that isn't a SkyBlock item.
            return parts[0].equals("?") ? null : new StoredItems.Stack(parts[0], Integer.parseInt(parts[1]));
        }

        @Override
        public Object withAmount(Object blob, int amount) {
            return ((String) blob).split(":")[0] + ":" + amount;
        }
    };

    private static List<Object> slots(String... blobs) {
        return new ArrayList<>(Arrays.asList((Object[]) blobs));
    }

    @Test
    void count() {
        Map<String, Integer> counts = new HashMap<>();
        StoredItems.count(slots("HOT_POTATO_BOOK:3", null, "?:1", "WITHER_BLOOD:1", "HOT_POTATO_BOOK:2"), PLAIN, counts);
        StoredItems.count(slots(null, "HOT_POTATO_BOOK:1"), PLAIN, counts);
        assertEquals(Map.of("HOT_POTATO_BOOK", 6, "WITHER_BLOOD", 1), counts);
    }

    @Test
    void takesFirstToLastLeavingTheRest() {
        List<Object> slots = slots("HOT_POTATO_BOOK:3", "WITHER_BLOOD:1", null, "HOT_POTATO_BOOK:5");
        assertEquals(4, StoredItems.take(slots, "hot_potato_book", 4, PLAIN));
        assertEquals(Arrays.asList(null, "WITHER_BLOOD:1", null, "HOT_POTATO_BOOK:4"), slots);
        // Only what there is.
        assertEquals(4, StoredItems.take(slots, "HOT_POTATO_BOOK", 10, PLAIN));
        assertEquals(Arrays.asList(null, "WITHER_BLOOD:1", null, null), slots);
        assertEquals(0, StoredItems.take(slots, "HOT_POTATO_BOOK", 1, PLAIN));
    }

    @Test
    void anotherSlotsBlobIsLeftAsItIs() {
        Object blood = "WITHER_BLOOD:1";
        List<Object> slots = slots("HOT_POTATO_BOOK:2");
        slots.add(blood);
        StoredItems.take(slots, "HOT_POTATO_BOOK", 1, PLAIN);
        assertEquals(blood, slots.get(1));
        assertEquals("HOT_POTATO_BOOK:1", slots.get(0));
    }
}
