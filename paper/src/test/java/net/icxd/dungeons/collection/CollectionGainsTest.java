package net.icxd.dungeons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The counting rules without a server: what an item adds, how much of a pickup went in, which drops merge.
 * (That only the plugin's world drops are collectable is its marking them: Mobs and BlockListener.)
 */
class CollectionGainsTest {
    private CollectionData before;

    @BeforeEach
    void useData() {
        before = Collections.data();
        Collections.setData(CollectionsTest.data());
    }

    @AfterEach
    void restore() {
        Collections.setData(before);
    }

    /** A collection's own item counts one each, an enchanted one what it's made of; the rest nothing. */
    @Test
    void whatAnItemAdds() {
        assertEquals(new CollectionData.Counted("ROTTEN_FLESH", 3), CollectionGains.worth("ROTTEN_FLESH", 3));
        assertEquals(new CollectionData.Counted("ROTTEN_FLESH", 320), CollectionGains.worth("ENCHANTED_ROTTEN_FLESH", 2));
        assertNull(CollectionGains.worth("HYPERION", 1));
        assertNull(CollectionGains.worth(null, 1));
        assertNull(CollectionGains.worth("ROTTEN_FLESH", 0));
    }

    /** What went into the inventory: all of it, what PlayerListener took before cancelling, or what vanilla takes. */
    @Test
    void howMuchAPickupTook() {
        assertEquals(5, CollectionGains.pickedUp(5, true, true, 0, 0));
        // PlayerListener fit 3 of 5 and left 2 on the ground.
        assertEquals(3, CollectionGains.pickedUp(5, false, true, 2, 0));
        // Cancelled by something else (a frozen player, a Sandbox drop): nothing.
        assertEquals(0, CollectionGains.pickedUp(5, false, true, 5, 0));
        // Vanilla's pickup, 1 of 5 left over.
        assertEquals(4, CollectionGains.pickedUp(5, false, false, 5, 1));
    }

    @Test
    void collectableDropsDontMergeWithOthers() {
        assertTrue(CollectionGains.mayMerge(true, true));
        assertTrue(CollectionGains.mayMerge(false, false));
        assertFalse(CollectionGains.mayMerge(true, false));
        assertFalse(CollectionGains.mayMerge(false, true));
    }
}
