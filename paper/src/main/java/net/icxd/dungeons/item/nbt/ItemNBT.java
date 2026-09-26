package net.icxd.dungeons.item.nbt;

import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
 * The compound is stored as typed JSON in one entry of the item's persistent data container, so
 * keys don't have to be valid {@link NamespacedKey}s.
 */
public final class ItemNBT {
    private static final NamespacedKey KEY = new NamespacedKey("dungeons", "data");

    private final ItemStack stack;
    private NBTTagCompound tag;

    private ItemNBT(ItemStack stack, NBTTagCompound tag) {
        this.stack = stack;
        this.tag = tag;
    }

    public static ItemNBT of(ItemStack stack) {
        if (stack == null) return new ItemNBT(null, null);
        ItemStack copy = stack.clone();
        ItemMeta meta = copy.getItemMeta();
        String json = meta == null ? null : meta.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return new ItemNBT(copy, json == null ? null : fromJson(json));
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
        if (tag == null) meta.getPersistentDataContainer().remove(KEY);
        else meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, toJson(tag));
        out.setItemMeta(meta);
        return out;
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
