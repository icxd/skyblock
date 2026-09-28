package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.types.Binary;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoClientSettings;

/**
 * Where storage keeps its items in the profile ({@link StorageDocument}), with made-up blobs for items: they
 * only move, and survive the trip through the database.
 */
class StorageDocumentTest {
    private static Binary blob(String what) {
        return new Binary(what.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** As Mongo gives it back. */
    private static Document roundTrip(Document doc) {
        BsonDocument bson = doc.toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry());
        return new DocumentCodec().decode(new BsonDocumentReader(bson), DecoderContext.builder().build());
    }

    @Test
    void slots() {
        Document storage = new Document();
        assertEquals(Arrays.asList(null, null, null), StorageDocument.slots(storage, "potionBag", 3));
        StorageDocument.setSlots(storage, "potionBag", Arrays.asList(blob("a"), null, blob("c"), blob("d")));
        assertEquals(Arrays.asList(blob("a"), null), StorageDocument.slots(storage, "potionBag", 2));
        // A smaller page writes its slots and keeps what's after them.
        StorageDocument.setSlots(storage, "potionBag", Arrays.asList(null, blob("b")));
        assertEquals(Arrays.asList(null, blob("b"), blob("c"), blob("d"), null), StorageDocument.slots(storage, "potionBag", 5));
    }

    @Test
    void enderChest() {
        Document storage = new Document();
        List<Object> page = new ArrayList<>(Arrays.asList(new Object[45]));
        page.set(44, blob("last"));
        StorageDocument.setPage(storage, 3, page);
        assertEquals(blob("last"), StorageDocument.page(storage, 3, 45).get(44));
        assertTrue(StorageDocument.empty(StorageDocument.page(storage, 1, 45)));
        assertTrue(StorageDocument.empty(StorageDocument.page(storage, 9, 45)));
        assertNull(StorageDocument.icon(storage, 3));
        StorageDocument.setIcon(storage, 3, "INK_SAC");
        assertEquals("INK_SAC", StorageDocument.icon(storage, 3));
        StorageDocument.setIcon(storage, 3, null);
        assertNull(StorageDocument.icon(storage, 3));
    }

    @Test
    void backpacks() {
        Document storage = new Document();
        assertNull(StorageDocument.backpack(storage, 2));
        assertFalse(StorageDocument.setBackpackContents(storage, 2, List.of()));
        assertTrue(StorageDocument.placeBackpack(storage, 2, "GREATER_BACKPACK", blob("backpack")));
        // Not two in a slot.
        assertFalse(StorageDocument.placeBackpack(storage, 2, "SMALL_BACKPACK", blob("another")));
        StorageDocument.Backpack backpack = StorageDocument.backpack(storage, 2);
        assertEquals("GREATER_BACKPACK", backpack.id());
        assertEquals(blob("backpack"), backpack.item());
        assertTrue(backpack.empty());

        assertTrue(StorageDocument.setBackpackContents(storage, 2, Arrays.asList(null, blob("apple"))));
        assertEquals(Arrays.asList(null, blob("apple"), null), StorageDocument.backpackContents(storage, 2, 3));
        // It doesn't come out with something in it.
        assertNull(StorageDocument.removeBackpack(storage, 2));
        assertEquals("GREATER_BACKPACK", StorageDocument.backpack(storage, 2).id());

        StorageDocument.setBackpackContents(storage, 2, Arrays.asList(null, null));
        StorageDocument.Backpack out = StorageDocument.removeBackpack(storage, 2);
        assertEquals(blob("backpack"), out.item());
        assertNull(StorageDocument.backpack(storage, 2));
        assertNull(StorageDocument.removeBackpack(storage, 2));
    }

    @Test
    void sets() {
        Document storage = new Document();
        assertEquals(-1, StorageDocument.worn(storage, StorageDocument.WORN_ARMOR_SET));
        assertEquals(Arrays.asList(null, null, null, null), StorageDocument.set(storage, StorageDocument.ARMOR_SETS, 5));
        StorageDocument.setSet(storage, StorageDocument.ARMOR_SETS, 5, Arrays.asList(blob("helm"), null, null, blob("boots")));
        assertEquals(Arrays.asList(blob("helm"), null, null, blob("boots")), StorageDocument.set(storage, StorageDocument.ARMOR_SETS, 5));
        assertTrue(StorageDocument.empty(StorageDocument.set(storage, StorageDocument.ARMOR_SETS, 4)));
        StorageDocument.setWorn(storage, StorageDocument.WORN_ARMOR_SET, 5);
        assertEquals(5, StorageDocument.worn(storage, StorageDocument.WORN_ARMOR_SET));
    }

    @Test
    void throughTheDatabase() {
        Document profile = new Document();
        Document storage = new Document();
        profile.put("storage", storage);
        StorageDocument.setPage(storage, 2, Arrays.asList(blob("sword"), null, blob("bow")));
        StorageDocument.placeBackpack(storage, 1, "JUMBO_BACKPACK", blob("jumbo"));
        StorageDocument.setBackpackContents(storage, 1, Arrays.asList(null, blob("stone")));
        StorageDocument.setIcon(storage, 2, "DIAMOND");
        StorageDocument.setSlots(storage, Bag.ACCESSORY_BAG.key(), Arrays.asList(blob("ring"), null));
        StorageDocument.setSet(storage, StorageDocument.EQUIPMENT_SETS, 1, Arrays.asList(null, blob("cloak"), null, null));
        StorageDocument.setSlots(storage, StorageDocument.EQUIPMENT, Arrays.asList(blob("necklace"), null, null, null));
        StorageDocument.setWorn(storage, StorageDocument.WORN_EQUIPMENT_SET, 0);
        Loadouts.put(profile, 0, new Loadouts.Loadout("Loadout 1", 0, 1, "Silky"));

        Document back = roundTrip(profile);
        Document stored = back.get("storage", Document.class);
        assertEquals(Arrays.asList(blob("sword"), null, blob("bow"), null), StorageDocument.page(stored, 2, 4));
        assertEquals("JUMBO_BACKPACK", StorageDocument.backpack(stored, 1).id());
        assertEquals(blob("jumbo"), StorageDocument.backpack(stored, 1).item());
        assertEquals(Arrays.asList(null, blob("stone")), StorageDocument.backpackContents(stored, 1, 2));
        assertEquals("DIAMOND", StorageDocument.icon(stored, 2));
        assertEquals(Arrays.asList(blob("ring"), null), StorageDocument.slots(stored, Bag.ACCESSORY_BAG.key(), 2));
        assertEquals(Arrays.asList(null, blob("cloak"), null, null), StorageDocument.set(stored, StorageDocument.EQUIPMENT_SETS, 1));
        assertEquals(blob("necklace"), StorageDocument.slots(stored, StorageDocument.EQUIPMENT, 4).getFirst());
        assertEquals(0, StorageDocument.worn(stored, StorageDocument.WORN_EQUIPMENT_SET));
        assertEquals(new Loadouts.Loadout("Loadout 1", 0, 1, "Silky"), Loadouts.get(back, 0));
    }
}
