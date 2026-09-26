package net.icxd.dungeons.item.nbt;

import java.util.Arrays;

/** A number, string or array. */
public final class NBTValue extends NBTBase {
    private final byte type;
    private final Object value;

    NBTValue(byte type, Object value) {
        this.type = type;
        this.value = value;
    }

    @Override
    public byte getTypeId() {
        return type;
    }

    public Object value() {
        return value;
    }

    boolean isNumber() {
        return type >= BYTE && type <= DOUBLE;
    }

    Number number() {
        return (Number) value;
    }

    @Override
    public NBTBase copy() {
        return switch (type) {
            case BYTE_ARRAY -> new NBTValue(type, ((byte[]) value).clone());
            case INT_ARRAY -> new NBTValue(type, ((int[]) value).clone());
            case LONG_ARRAY -> new NBTValue(type, ((long[]) value).clone());
            default -> this;
        };
    }

    /** Same format as 1.8's NBT toString: 5b, 3s, 7, 9L, 1.5f, 2.0d, "text". */
    @Override
    public String toString() {
        return switch (type) {
            case BYTE -> value + "b";
            case SHORT -> value + "s";
            case LONG -> value + "L";
            case FLOAT -> value + "f";
            case DOUBLE -> value + "d";
            case STRING -> "\"" + ((String) value).replace("\"", "\\\"") + "\"";
            case BYTE_ARRAY -> "[" + ((byte[]) value).length + " bytes]";
            case INT_ARRAY -> Arrays.toString((int[]) value);
            case LONG_ARRAY -> Arrays.toString((long[]) value);
            default -> String.valueOf(value);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NBTValue other) || other.type != type) return false;
        return switch (type) {
            case BYTE_ARRAY -> Arrays.equals((byte[]) value, (byte[]) other.value);
            case INT_ARRAY -> Arrays.equals((int[]) value, (int[]) other.value);
            case LONG_ARRAY -> Arrays.equals((long[]) value, (long[]) other.value);
            default -> value.equals(other.value);
        };
    }

    @Override
    public int hashCode() {
        return switch (type) {
            case BYTE_ARRAY -> Arrays.hashCode((byte[]) value);
            case INT_ARRAY -> Arrays.hashCode((int[]) value);
            case LONG_ARRAY -> Arrays.hashCode((long[]) value);
            default -> value.hashCode();
        };
    }
}
