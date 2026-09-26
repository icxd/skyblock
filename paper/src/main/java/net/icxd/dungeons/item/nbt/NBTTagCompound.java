package net.icxd.dungeons.item.nbt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Named values, like 1.8's {@code NBTTagCompound}: getters return 0, "" or an empty compound/list
 * for missing keys, and number getters convert between number types.
 */
public final class NBTTagCompound extends NBTBase {
    private final Map<String, NBTBase> map = new LinkedHashMap<>();

    @Override
    public byte getTypeId() {
        return COMPOUND;
    }

    public void set(String key, NBTBase value) {
        map.put(key, value);
    }

    public void setByte(String key, byte value) {
        map.put(key, new NBTValue(BYTE, value));
    }

    public void setShort(String key, short value) {
        map.put(key, new NBTValue(SHORT, value));
    }

    public void setInt(String key, int value) {
        map.put(key, new NBTValue(INT, value));
    }

    public void setLong(String key, long value) {
        map.put(key, new NBTValue(LONG, value));
    }

    public void setFloat(String key, float value) {
        map.put(key, new NBTValue(FLOAT, value));
    }

    public void setDouble(String key, double value) {
        map.put(key, new NBTValue(DOUBLE, value));
    }

    public void setString(String key, String value) {
        map.put(key, new NBTValue(STRING, value));
    }

    public void setByteArray(String key, byte[] value) {
        map.put(key, new NBTValue(BYTE_ARRAY, value));
    }

    public void setIntArray(String key, int[] value) {
        map.put(key, new NBTValue(INT_ARRAY, value));
    }

    public void setLongArray(String key, long[] value) {
        map.put(key, new NBTValue(LONG_ARRAY, value));
    }

    /** Stored as a byte, like 1.8. */
    public void setBoolean(String key, boolean value) {
        setByte(key, (byte) (value ? 1 : 0));
    }

    public NBTBase get(String key) {
        return map.get(key);
    }

    public boolean hasKey(String key) {
        return map.containsKey(key);
    }

    public boolean hasKeyOfType(String key, int type) {
        NBTBase value = map.get(key);
        if (value == null) return false;
        if (type == ANY_NUMBER) return value instanceof NBTValue v && v.isNumber();
        return value.getTypeId() == type;
    }

    public void remove(String key) {
        map.remove(key);
    }

    /** The keys (1.8's obfuscated name for {@code keySet}). */
    public Set<String> c() {
        return map.keySet();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    private Number number(String key) {
        return map.get(key) instanceof NBTValue v && v.isNumber() ? v.number() : 0;
    }

    public byte getByte(String key) {
        return number(key).byteValue();
    }

    public short getShort(String key) {
        return number(key).shortValue();
    }

    public int getInt(String key) {
        return number(key).intValue();
    }

    public long getLong(String key) {
        return number(key).longValue();
    }

    public float getFloat(String key) {
        return number(key).floatValue();
    }

    public double getDouble(String key) {
        return number(key).doubleValue();
    }

    public boolean getBoolean(String key) {
        return getByte(key) != 0;
    }

    public String getString(String key) {
        return map.get(key) instanceof NBTValue v && v.getTypeId() == STRING ? (String) v.value() : "";
    }

    public byte[] getByteArray(String key) {
        return map.get(key) instanceof NBTValue v && v.getTypeId() == BYTE_ARRAY ? (byte[]) v.value() : new byte[0];
    }

    public int[] getIntArray(String key) {
        return map.get(key) instanceof NBTValue v && v.getTypeId() == INT_ARRAY ? (int[]) v.value() : new int[0];
    }

    /** The compound under {@code key}, or a new empty one (not added) if there is none. */
    public NBTTagCompound getCompound(String key) {
        return map.get(key) instanceof NBTTagCompound c ? c : new NBTTagCompound();
    }

    /**
     * The list under {@code key} if its elements are of {@code type} (or it's empty), otherwise a
     * new empty list (not added).
     */
    public NBTTagList getList(String key, int type) {
        if (map.get(key) instanceof NBTTagList list && (list.isEmpty() || list.getElementType() == type)) return list;
        return new NBTTagList();
    }

    @Override
    public NBTTagCompound copy() {
        NBTTagCompound copy = new NBTTagCompound();
        map.forEach((k, v) -> copy.map.put(k, v.copy()));
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, NBTBase> e : map.entrySet()) {
            if (sb.length() > 1) sb.append(',');
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.append('}').toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NBTTagCompound other && other.map.equals(map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
