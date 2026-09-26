package net.icxd.dungeons.item.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * An item's custom data, replacing 1.8's {@code CraftItemStack.asNMSCopy(stack).getTag()}:
 * <pre>
 *   ItemNBT nbt = ItemNBT.of(stack);          // a copy, like asNMSCopy
 *   NBTTagCompound tag = nbt.getTag();        // null if the item has no data
 *   nbt.setTag(tag);
 *   ItemStack updated = nbt.toItemStack();    // like asBukkitCopy
 * </pre>
 * The compound is kept in the item's persistent data container as real, typed data: a container
 * under {@code skyblock:item}, with each key as {@code skyblock:<key>} (so keys are lowercase, digits
 * and {@code _ . - /}), compounds as containers and lists (of compounds) as lists of containers. An
 * empty list isn't stored; reading it back gives an empty list anyway.
 * <p>
 * Items from before kept it as typed JSON in {@code dungeons:data}; they're read the same, and
 * written the new way the next time they're saved.
 */
public final class ItemNBT {
    private static final String NAMESPACE = "skyblock";
    private static final NamespacedKey KEY = new NamespacedKey(NAMESPACE, "item");
    private static final NamespacedKey LEGACY_KEY = new NamespacedKey("dungeons", "data");

    private final ItemStack stack;
    private NBTTagCompound tag;

    private ItemNBT(ItemStack stack, NBTTagCompound tag) {
        this.stack = stack;
        this.tag = tag;
    }

    public static ItemNBT of(ItemStack stack) {
        if (stack == null) return new ItemNBT(null, null);
        return new ItemNBT(stack.clone(), read(stack));
    }

    /** Just the item's data (null if it has none): read through its read-only view, so without copying its meta. */
    public static NBTTagCompound read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        PersistentDataContainerView pdc = stack.getPersistentDataContainer();
        PersistentDataContainer data = pdc.get(KEY, PersistentDataType.TAG_CONTAINER);
        if (data != null) return read(data);
        String json = pdc.get(LEGACY_KEY, PersistentDataType.STRING);
        return json == null ? null : fromJson(json);
    }

    public boolean hasTag() {
        return tag != null;
    }

    public NBTTagCompound getTag() {
        return tag;
    }

    public void setTag(NBTTagCompound tag) {
        this.tag = tag;
    }

    /** A copy of the item with the current tag written to it. */
    public ItemStack toItemStack() {
        if (stack == null) return null;
        ItemStack out = stack.clone();
        ItemMeta meta = out.getItemMeta();
        if (meta == null) return out;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(LEGACY_KEY);
        if (tag == null) {
            pdc.remove(KEY);
        } else {
            PersistentDataContainer data = pdc.getAdapterContext().newPersistentDataContainer();
            write(tag, data);
            pdc.set(KEY, PersistentDataType.TAG_CONTAINER, data);
        }
        out.setItemMeta(meta);
        return out;
    }

    private static final PersistentDataType<List<PersistentDataContainer>, List<PersistentDataContainer>> CONTAINERS =
            PersistentDataType.LIST.dataContainers();

    private static void write(NBTTagCompound tag, PersistentDataContainer into) {
        for (String name : tag.keySet()) {
            NamespacedKey key = new NamespacedKey(NAMESPACE, name);
            NBTBase value = tag.get(name);
            if (value instanceof NBTTagCompound compound) {
                PersistentDataContainer child = into.getAdapterContext().newPersistentDataContainer();
                write(compound, child);
                into.set(key, PersistentDataType.TAG_CONTAINER, child);
            } else if (value instanceof NBTTagList list) {
                if (list.isEmpty()) continue;
                List<PersistentDataContainer> children = new ArrayList<>(list.size());
                for (int i = 0; i < list.size(); i++) {
                    if (!(list.getRaw(i) instanceof NBTTagCompound compound)) {
                        throw new IllegalArgumentException("item data lists hold compounds; " + name + " has " + list.getRaw(i));
                    }
                    PersistentDataContainer child = into.getAdapterContext().newPersistentDataContainer();
                    write(compound, child);
                    children.add(child);
                }
                into.set(key, CONTAINERS, children);
            } else {
                NBTValue v = (NBTValue) value;
                switch (v.getTypeId()) {
                    case NBTBase.STRING -> into.set(key, PersistentDataType.STRING, (String) v.value());
                    case NBTBase.BYTE -> into.set(key, PersistentDataType.BYTE, (Byte) v.value());
                    case NBTBase.SHORT -> into.set(key, PersistentDataType.SHORT, (Short) v.value());
                    case NBTBase.INT -> into.set(key, PersistentDataType.INTEGER, (Integer) v.value());
                    case NBTBase.LONG -> into.set(key, PersistentDataType.LONG, (Long) v.value());
                    case NBTBase.FLOAT -> into.set(key, PersistentDataType.FLOAT, (Float) v.value());
                    case NBTBase.DOUBLE -> into.set(key, PersistentDataType.DOUBLE, (Double) v.value());
                    case NBTBase.BYTE_ARRAY -> into.set(key, PersistentDataType.BYTE_ARRAY, (byte[]) v.value());
                    case NBTBase.INT_ARRAY -> into.set(key, PersistentDataType.INTEGER_ARRAY, (int[]) v.value());
                    case NBTBase.LONG_ARRAY -> into.set(key, PersistentDataType.LONG_ARRAY, (long[]) v.value());
                    default -> throw new IllegalArgumentException("unknown tag type " + v.getTypeId());
                }
            }
        }
    }

    private static NBTTagCompound read(PersistentDataContainer from) {
        NBTTagCompound tag = new NBTTagCompound();
        for (NamespacedKey key : from.getKeys()) {
            if (!key.getNamespace().equals(NAMESPACE)) continue;
            String name = key.getKey();
            if (from.has(key, PersistentDataType.TAG_CONTAINER)) {
                tag.set(name, read(from.get(key, PersistentDataType.TAG_CONTAINER)));
            } else if (from.has(key, CONTAINERS)) {
                NBTTagList list = new NBTTagList();
                for (PersistentDataContainer child : from.get(key, CONTAINERS)) list.add(read(child));
                tag.set(name, list);
            } else if (from.has(key, PersistentDataType.STRING)) {
                tag.setString(name, from.get(key, PersistentDataType.STRING));
            } else if (from.has(key, PersistentDataType.BYTE)) {
                tag.setByte(name, from.get(key, PersistentDataType.BYTE));
            } else if (from.has(key, PersistentDataType.SHORT)) {
                tag.setShort(name, from.get(key, PersistentDataType.SHORT));
            } else if (from.has(key, PersistentDataType.INTEGER)) {
                tag.setInt(name, from.get(key, PersistentDataType.INTEGER));
            } else if (from.has(key, PersistentDataType.LONG)) {
                tag.setLong(name, from.get(key, PersistentDataType.LONG));
            } else if (from.has(key, PersistentDataType.FLOAT)) {
                tag.setFloat(name, from.get(key, PersistentDataType.FLOAT));
            } else if (from.has(key, PersistentDataType.DOUBLE)) {
                tag.setDouble(name, from.get(key, PersistentDataType.DOUBLE));
            } else if (from.has(key, PersistentDataType.BYTE_ARRAY)) {
                tag.setByteArray(name, from.get(key, PersistentDataType.BYTE_ARRAY));
            } else if (from.has(key, PersistentDataType.INTEGER_ARRAY)) {
                tag.setIntArray(name, from.get(key, PersistentDataType.INTEGER_ARRAY));
            } else if (from.has(key, PersistentDataType.LONG_ARRAY)) {
                tag.setLongArray(name, from.get(key, PersistentDataType.LONG_ARRAY));
            }
        }
        return tag;
    }

    // Every value is [type id, payload].

    public static String toJson(NBTTagCompound tag) {
        return encode(tag).toString();
    }

    public static NBTTagCompound fromJson(String json) {
        NBTBase value = decode(JsonParser.parseString(json).getAsJsonArray());
        return value instanceof NBTTagCompound c ? c : new NBTTagCompound();
    }

    private static JsonArray encode(NBTBase value) {
        JsonArray out = new JsonArray();
        out.add(value.getTypeId());
        if (value instanceof NBTTagCompound c) {
            JsonObject o = new JsonObject();
            for (String key : c.keySet()) o.add(key, encode(c.get(key)));
            out.add(o);
        } else if (value instanceof NBTTagList l) {
            JsonArray a = new JsonArray();
            for (int i = 0; i < l.size(); i++) a.add(encode(l.getRaw(i)));
            out.add(a);
        } else {
            NBTValue v = (NBTValue) value;
            switch (v.getTypeId()) {
                case NBTBase.STRING -> out.add((String) v.value());
                case NBTBase.BYTE_ARRAY -> {
                    JsonArray a = new JsonArray();
                    for (byte b : (byte[]) v.value()) a.add(b);
                    out.add(a);
                }
                case NBTBase.INT_ARRAY -> {
                    JsonArray a = new JsonArray();
                    for (int i : (int[]) v.value()) a.add(i);
                    out.add(a);
                }
                case NBTBase.LONG_ARRAY -> {
                    JsonArray a = new JsonArray();
                    for (long l : (long[]) v.value()) a.add(l);
                    out.add(a);
                }
                default -> out.add(new JsonPrimitive(v.number()));
            }
        }
        return out;
    }

    private static NBTBase decode(JsonArray a) {
        byte type = a.get(0).getAsByte();
        JsonElement p = a.get(1);
        switch (type) {
            case NBTBase.COMPOUND -> {
                NBTTagCompound c = new NBTTagCompound();
                for (Map.Entry<String, JsonElement> e : p.getAsJsonObject().entrySet()) c.set(e.getKey(), decode(e.getValue().getAsJsonArray()));
                return c;
            }
            case NBTBase.LIST -> {
                NBTTagList l = new NBTTagList();
                for (JsonElement e : p.getAsJsonArray()) l.add(decode(e.getAsJsonArray()));
                return l;
            }
            case NBTBase.STRING -> {
                return new NBTValue(type, p.getAsString());
            }
            case NBTBase.BYTE_ARRAY -> {
                JsonArray arr = p.getAsJsonArray();
                byte[] out = new byte[arr.size()];
                for (int i = 0; i < out.length; i++) out[i] = arr.get(i).getAsByte();
                return new NBTValue(type, out);
            }
            case NBTBase.INT_ARRAY -> {
                JsonArray arr = p.getAsJsonArray();
                int[] out = new int[arr.size()];
                for (int i = 0; i < out.length; i++) out[i] = arr.get(i).getAsInt();
                return new NBTValue(type, out);
            }
            case NBTBase.LONG_ARRAY -> {
                JsonArray arr = p.getAsJsonArray();
                long[] out = new long[arr.size()];
                for (int i = 0; i < out.length; i++) out[i] = arr.get(i).getAsLong();
                return new NBTValue(type, out);
            }
            case NBTBase.BYTE -> {
                return new NBTValue(type, p.getAsByte());
            }
            case NBTBase.SHORT -> {
                return new NBTValue(type, p.getAsShort());
            }
            case NBTBase.INT -> {
                return new NBTValue(type, p.getAsInt());
            }
            case NBTBase.LONG -> {
                return new NBTValue(type, p.getAsLong());
            }
            case NBTBase.FLOAT -> {
                return new NBTValue(type, p.getAsFloat());
            }
            case NBTBase.DOUBLE -> {
                return new NBTValue(type, p.getAsDouble());
            }
            default -> throw new IllegalArgumentException("unknown tag type " + type);
        }
    }
}
