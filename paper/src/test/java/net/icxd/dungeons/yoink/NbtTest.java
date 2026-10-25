package net.icxd.dungeons.yoink;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

/** The API's item data as Minecraft 1.8 wrote it: a gzipped NBT file of {@code {i: [items]}}, in base64. */
class NbtTest {
    /** Writes a tiny NBT file the way Minecraft does: the root compound "" with an item list "i". */
    static String inventory(List<byte[]> items) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
            out.writeByte(10);
            out.writeUTF("");
            out.writeByte(9);
            out.writeUTF("i");
            out.writeByte(10);
            out.writeInt(items.size());
            for (byte[] item : items) out.write(item);
            out.writeByte(0);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    /** An item's compound (without its END): {id: 276s, Count: 1b, Damage: 0s, tag: {ExtraAttributes: {id, enchantments}}}. */
    static byte[] sword() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(2);
            out.writeUTF("id");
            out.writeShort(276);
            out.writeByte(1);
            out.writeUTF("Count");
            out.writeByte(1);
            out.writeByte(10);
            out.writeUTF("tag");
            out.writeByte(10);
            out.writeUTF("ExtraAttributes");
            out.writeByte(8);
            out.writeUTF("id");
            out.writeUTF("ASPECT_OF_THE_END");
            out.writeByte(10);
            out.writeUTF("enchantments");
            out.writeByte(3);
            out.writeUTF("sharpness");
            out.writeInt(6);
            out.writeByte(0);
            out.writeByte(11);
            out.writeUTF("ints");
            out.writeInt(2);
            out.writeInt(7);
            out.writeInt(-1);
            out.writeByte(0);
            out.writeByte(0);
            out.writeByte(0);
        }
        return bytes.toByteArray();
    }

    /** An empty slot is an empty compound. */
    static byte[] empty() {
        return new byte[]{0};
    }

    @Test
    void readsTheApisItems() throws IOException {
        Map<String, Object> root = Nbt.fromBase64(inventory(List.of(sword(), empty())));
        List<Object> items = Nbt.list(root, "i");
        assertEquals(2, items.size());
        @SuppressWarnings("unchecked")
        Map<String, Object> sword = (Map<String, Object>) items.get(0);
        assertEquals((short) 276, sword.get("id"));
        assertEquals((byte) 1, sword.get("Count"));
        Map<String, Object> extra = Nbt.compound(Nbt.compound(sword, "tag"), "ExtraAttributes");
        assertEquals("ASPECT_OF_THE_END", Nbt.string(extra, "id"));
        assertEquals(6, Nbt.integer(Nbt.compound(extra, "enchantments"), "sharpness", 0));
        assertArrayEquals(new int[]{7, -1}, (int[]) extra.get("ints"));
        assertTrue(((Map<?, ?>) items.get(1)).isEmpty());
    }

    @Test
    void brokenDataSaysSo() {
        assertThrows(IOException.class, () -> Nbt.read(new byte[]{8, 0, 0}));
        assertThrows(IOException.class, () -> Nbt.read(new byte[]{10, 0, 0, 3, 0, 1, 'x'}));
        assertEquals(0, Nbt.integer(Map.of("a", "text"), "a", 0));
        assertTrue(Nbt.compound(Map.of(), "missing").isEmpty());
    }
}
