package net.icxd.dungeons.yoink;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Reads the item data Hypixel's API gives ({@code inventory.inv_contents.data} and the like): base64 of a gzipped
 * binary NBT file, as Minecraft 1.8 wrote it. A compound comes back as a {@code Map<String, Object>} (in its order),
 * a list as a {@code List<Object>}, and the rest as Java's own: Byte, Short, Integer, Long, Float, Double, String,
 * byte[], int[] and long[].
 */
public final class Nbt {
    private static final int END = 0, BYTE = 1, SHORT = 2, INT = 3, LONG = 4, FLOAT = 5, DOUBLE = 6, BYTE_ARRAY = 7,
            STRING = 8, LIST = 9, COMPOUND = 10, INT_ARRAY = 11, LONG_ARRAY = 12;
    /** Deeper than Minecraft's own limit (512) is broken data, not an item. */
    private static final int MAX_DEPTH = 512;

    private Nbt() {
    }

    /** The root compound of the API's base64 text. */
    public static Map<String, Object> fromBase64(String data) throws IOException {
        return read(Base64.getDecoder().decode(data.trim()));
    }

    /** The root compound of a gzipped (or plain) NBT file. */
    public static Map<String, Object> read(byte[] bytes) throws IOException {
        InputStream in = new ByteArrayInputStream(bytes);
        if (bytes.length >= 2 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b) in = new GZIPInputStream(in);
        try (DataInputStream data = new DataInputStream(in)) {
            int type = data.readUnsignedByte();
            if (type != COMPOUND) throw new IOException("the root isn't a compound (tag " + type + ")");
            data.readUTF();
            return compound(data, 0);
        } catch (EOFException e) {
            throw new IOException("the data ends too soon", e);
        }
    }

    private static Map<String, Object> compound(DataInputStream in, int depth) throws IOException {
        if (depth > MAX_DEPTH) throw new IOException("nested too deep");
        Map<String, Object> out = new LinkedHashMap<>();
        while (true) {
            int type = in.readUnsignedByte();
            if (type == END) return out;
            out.put(in.readUTF(), payload(in, type, depth + 1));
        }
    }

    private static Object payload(DataInputStream in, int type, int depth) throws IOException {
        return switch (type) {
            case BYTE -> in.readByte();
            case SHORT -> in.readShort();
            case INT -> in.readInt();
            case LONG -> in.readLong();
            case FLOAT -> in.readFloat();
            case DOUBLE -> in.readDouble();
            case BYTE_ARRAY -> {
                byte[] bytes = new byte[length(in)];
                in.readFully(bytes);
                yield bytes;
            }
            case STRING -> in.readUTF();
            case LIST -> {
                int of = in.readUnsignedByte();
                int size = length(in);
                List<Object> list = new ArrayList<>(Math.min(size, 1024));
                for (int i = 0; i < size; i++) list.add(payload(in, of, depth + 1));
                yield list;
            }
            case COMPOUND -> compound(in, depth);
            case INT_ARRAY -> {
                int[] ints = new int[length(in)];
                for (int i = 0; i < ints.length; i++) ints[i] = in.readInt();
                yield ints;
            }
            case LONG_ARRAY -> {
                long[] longs = new long[length(in)];
                for (int i = 0; i < longs.length; i++) longs[i] = in.readLong();
                yield longs;
            }
            default -> throw new IOException("unknown tag " + type);
        };
    }

    private static int length(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0 || length > 1 << 24) throw new IOException("a length of " + length);
        return length;
    }

    // Reading what came back

    /** The compound at this key; empty if it isn't one. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> compound(Map<String, Object> in, String key) {
        return in != null && in.get(key) instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    /** The list at this key; empty if it isn't one. */
    @SuppressWarnings("unchecked")
    public static List<Object> list(Map<String, Object> in, String key) {
        return in != null && in.get(key) instanceof List<?> list ? (List<Object>) list : List.of();
    }

    /** The number at this key as an int ({@code fallback} if it isn't a number). */
    public static int integer(Map<String, Object> in, String key, int fallback) {
        return in != null && in.get(key) instanceof Number n ? n.intValue() : fallback;
    }

    /** The text at this key; null if it isn't text. */
    public static String string(Map<String, Object> in, String key) {
        return in != null && in.get(key) instanceof String s ? s : null;
    }
}
