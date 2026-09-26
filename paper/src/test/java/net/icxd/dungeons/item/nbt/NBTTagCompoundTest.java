package net.icxd.dungeons.item.nbt;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NBTTagCompoundTest {

    private static NBTTagCompound sample() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "HYPERION");
        tag.setInt("hot_potato_books", 10);
        tag.setDouble("damage", 260.5);
        tag.setLong("timestamp", 1_700_000_000_123L);
        tag.setFloat("f", 1.5f);
        tag.setShort("s", (short) 7);
        tag.setByte("b", (byte) -3);
        tag.setBoolean("recombobulated", true);
        tag.setIntArray("ints", new int[]{1, 2, 3});
        tag.setByteArray("bytes", new byte[]{4, 5});
        NBTTagList enchantments = new NBTTagList();
        NBTTagCompound sharpness = new NBTTagCompound();
        sharpness.setString("name", "sharpness");
        sharpness.setShort("lvl", (short) 5);
        enchantments.add(sharpness);
        tag.set("enchantments", enchantments);
        NBTTagCompound sub = new NBTTagCompound();
        sub.setString("UPPER_case key", "kept");
        tag.set("nested", sub);
        return tag;
    }

    @Test
    void jsonRoundTripKeepsEveryType() {
        NBTTagCompound tag = sample();
        NBTTagCompound back = ItemNBT.fromJson(ItemNBT.toJson(tag));
        assertEquals(tag, back);
        assertEquals("HYPERION", back.getString("id"));
        assertEquals(1_700_000_000_123L, back.getLong("timestamp"));
        assertEquals(NBTBase.SHORT, back.get("s").getTypeId());
        assertEquals(NBTBase.FLOAT, back.get("f").getTypeId());
        assertArrayEquals(new int[]{1, 2, 3}, back.getIntArray("ints"));
        assertEquals("kept", back.getCompound("nested").getString("UPPER_case key"));
        assertEquals(5, back.getList("enchantments", NBTBase.COMPOUND).get(0).getInt("lvl"));
    }

    @Test
    void missingKeysGiveDefaultsLike18() {
        NBTTagCompound tag = new NBTTagCompound();
        assertEquals("", tag.getString("nope"));
        assertEquals(0, tag.getInt("nope"));
        assertFalse(tag.getBoolean("nope"));
        assertTrue(tag.getCompound("nope").isEmpty());
        assertTrue(tag.getList("nope", NBTBase.COMPOUND).isEmpty());
        assertNull(tag.get("nope"));
        // Not added by the getters.
        assertFalse(tag.hasKey("nope"));
    }

    @Test
    void numbersConvertBetweenTypes() {
        NBTTagCompound tag = sample();
        assertEquals(10.0, tag.getDouble("hot_potato_books"));
        assertEquals(260, tag.getInt("damage"));
        assertTrue(tag.hasKeyOfType("s", NBTBase.ANY_NUMBER));
        assertFalse(tag.hasKeyOfType("id", NBTBase.ANY_NUMBER));
        assertEquals("", tag.getString("hot_potato_books"));
    }

    @Test
    void listsOfAnotherTypeLookEmpty() {
        NBTTagCompound tag = sample();
        assertTrue(tag.getList("enchantments", NBTBase.STRING).isEmpty());
        assertEquals(1, tag.getList("enchantments", NBTBase.COMPOUND).size());
    }

    @Test
    void copyIsDeep() {
        NBTTagCompound tag = sample();
        NBTTagCompound copy = tag.copy();
        copy.getList("enchantments", NBTBase.COMPOUND).get(0).setShort("lvl", (short) 6);
        assertEquals(5, tag.getList("enchantments", NBTBase.COMPOUND).get(0).getInt("lvl"));
    }
}
