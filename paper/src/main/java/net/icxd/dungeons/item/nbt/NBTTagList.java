package net.icxd.dungeons.item.nbt;

import java.util.ArrayList;
import java.util.List;

/** An ordered list of values of one type, like 1.8's {@code NBTTagList}. */
public final class NBTTagList extends NBTBase {
    private final List<NBTBase> list = new ArrayList<>();

    @Override
    public byte getTypeId() {
        return LIST;
    }

    /** Type of the elements, 0 while empty. */
    public byte getElementType() {
        return list.isEmpty() ? 0 : list.get(0).getTypeId();
    }

    public void add(NBTBase value) {
        list.add(value);
    }

    public void set(int index, NBTBase value) {
        list.set(index, value);
    }

    public NBTBase remove(int index) {
        return list.remove(index);
    }

    /** The compound at {@code index}, or an empty one (not in the list) if it isn't a compound. */
    public NBTTagCompound get(int index) {
        if (index >= 0 && index < list.size() && list.get(index) instanceof NBTTagCompound compound) return compound;
        return new NBTTagCompound();
    }

    public NBTBase getRaw(int index) {
        return list.get(index);
    }

    public String getString(int index) {
        if (index >= 0 && index < list.size() && list.get(index) instanceof NBTValue v && v.getTypeId() == STRING) {
            return (String) v.value();
        }
        return "";
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public NBTTagList copy() {
        NBTTagList copy = new NBTTagList();
        for (NBTBase value : list) copy.list.add(value.copy());
        return copy;
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof NBTTagList other && other.list.equals(list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }
}
