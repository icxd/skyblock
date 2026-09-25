package net.icxd.dungeons.item.nbt;

/**
 * A value in an item's custom data. Mirrors the 1.8 NBT classes this plugin was written against,
 * so the item code keeps working now that the data lives in the item's persistent data container
 * (see {@link ItemNBT}).
 */
public abstract class NBTBase {
    public static final byte BYTE = 1;
    public static final byte SHORT = 2;
    public static final byte INT = 3;
    public static final byte LONG = 4;
    public static final byte FLOAT = 5;
    public static final byte DOUBLE = 6;
    public static final byte BYTE_ARRAY = 7;
    public static final byte STRING = 8;
    public static final byte LIST = 9;
    public static final byte COMPOUND = 10;
    public static final byte INT_ARRAY = 11;
    public static final byte LONG_ARRAY = 12;
    /** Any number, for {@link NBTTagCompound#hasKeyOfType}. */
    public static final byte ANY_NUMBER = 99;

    public abstract byte getTypeId();

    public abstract NBTBase copy();
}
