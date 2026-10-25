package net.icxd.dungeons.yoink;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemstoneQuality;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.rune.Rune;

/**
 * Hypixel's items as the API gives them, made into this plugin's: the item of the same SkyBlock id, with what was done
 * to it written the way this plugin keeps it (ItemBuilder's data): its reforge, stars, books, enchantments, gems, a
 * Necron's blade's scrolls, attributes, the Grand Searing rune, the other modifiers, and its uuid. Its skin and dye
 * aren't kept (an item here looks as its kind does). What has no item here is left out and counted by id, and so are
 * pets (there are none here) and potions (this plugin's Potion is a Water Bottle); the SkyBlock Menu is this plugin's
 * own. Plain data until {@link #stack}, which needs the server.
 *
 * <p>The API's items are Minecraft 1.8's NBT ({@code {id, Count, Damage, tag: {ExtraAttributes: {...}, display}}}, an
 * empty slot an empty compound), and what an {@code ExtraAttributes} key means is as NEU and Skyblocker read it.
 */
final class HypixelItems {
    /** Counts kept under Hypixel's own key (see Book and ItemModifiers). */
    private static final List<String> COUNTS = List.of("stats_book", "farming_for_dummies_count", "bookworm_books", "polarvoid",
            "wet_book_count", ItemModifiers.TUNERS, ItemModifiers.MANA_DISINTEGRATORS, ItemModifiers.JALAPENO, ItemModifiers.WOOD_SINGULARITY);
    /** Text kept under Hypixel's own key: "SAPPHIRE_POWER_SCROLL", "magic_find". */
    private static final List<String> TEXTS = List.of(ItemModifiers.POWER_SCROLL, ItemModifiers.ENRICHMENT);
    /** What stacking enchantments count, under Hypixel's keys (see StackingEnchants). */
    private static final List<String> COUNTERS = List.of("champion_combat_xp", "compact_blocks", "farmed_cultivating", "expertise_kills",
            "hecatomb_s_runs", "absorb_logs_chopped", "toxophilite_combat_xp");
    /** A Necron's blade's scrolls (Hypixel's {@code ability_scroll}) to the blade's flags (see NecronsBlade). */
    private static final Map<String, String> SCROLLS = Map.of("IMPLOSION_SCROLL", "implosion", "WITHER_SHIELD_SCROLL", "wither_shield",
            "SHADOW_WARP_SCROLL", "shadow_warp");
    private static final List<String> ATTRIBUTE_KEYS = List.of("attribute_1", "attribute_1_level", "attribute_2", "attribute_2_level");
    /** Hypixel's own item in slot 9 of the inventory; this plugin gives its own. */
    static final String SKYBLOCK_MENU = "SKYBLOCK_MENU";

    private HypixelItems() {
    }

    /** What couldn't come over, by what Hypixel calls it, and how many of each. */
    static final class Left {
        final Map<String, Integer> items = new TreeMap<>();
        final Map<String, Integer> enchantments = new TreeMap<>();

        void item(String id, int amount) {
            items.merge(id, Math.max(1, amount), Integer::sum);
        }

        void enchantment(String id) {
            enchantments.merge(id, 1, Integer::sum);
        }
    }

    /** One item, as this plugin has it: its kind, its data, how many. */
    record Item(SkyBlockItem kind, NBTTagCompound tag, int amount) {
    }

    /**
     * The items in one of the API's {@code data} blobs, a slot each: null where it's empty or left out. Whoever
     * {@code owner} is owns those that are owned (see SkyBlockItem#isOwnable).
     */
    static List<Item> read(String data, UUID owner, Left left) throws IOException {
        List<Object> slots = Nbt.list(Nbt.fromBase64(data), "i");
        List<Item> items = new ArrayList<>(slots.size());
        for (Object slot : slots) items.add(slot instanceof Map<?, ?> ? item(cast(slot), owner, left) : null);
        return items;
    }

    /** The item in a slot; null if it's empty, or left out (and counted). */
    static Item item(Map<String, Object> slot, UUID owner, Left left) {
        if (slot.isEmpty()) return null;
        Map<String, Object> extra = Nbt.compound(Nbt.compound(slot, "tag"), "ExtraAttributes");
        int amount = Math.max(1, Nbt.integer(slot, "Count", 1));
        String id = Nbt.string(extra, "id");
        if (id == null || id.isBlank()) {
            left.item("minecraft:" + slot.get("id"), amount);
            return null;
        }
        if (id.equals(SKYBLOCK_MENU)) return null;
        if (id.equals("PET")) {
            left.item(pet(extra) + " Pet", amount);
            return null;
        }
        SkyBlockItem kind = id.equals("POTION") ? null : ItemRegistry.get(id);
        if (kind == null) {
            left.item(id, amount);
            return null;
        }
        return new Item(kind, data(kind, extra, owner, left), amount);
    }

    /** As an item stack (on the server). */
    static ItemStack stack(Item item) {
        return item == null ? null : ItemBuilder.build(item.kind(), item.tag(), item.amount());
    }

    /** These, as item stacks; null where there's none. */
    static ItemStack[] stacks(List<Item> items, int size) {
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size && i < items.size(); i++) stacks[i] = stack(items.get(i));
        return stacks;
    }

    /** A new one of {@code kind}'s data, with what Hypixel's {@code ExtraAttributes} say was done to it. */
    static NBTTagCompound data(SkyBlockItem kind, Map<String, Object> extra, UUID owner, Left left) {
        NBTTagCompound tag = ItemBuilder.newData(kind);
        if (tag.hasKey("uuid") && Nbt.string(extra, "uuid") instanceof String uuid && !uuid.isBlank()) tag.setString("uuid", uuid);
        if (owner != null && kind.isOwnable()) tag.setString("owner", owner.toString());
        if (Nbt.string(extra, "modifier") instanceof String reforge) tag.setString("reforge", reforge);
        if (Nbt.integer(extra, "rarity_upgrades", 0) > 0) tag.setBoolean("recombobulated", true);
        if (Nbt.integer(extra, "dungeon_item", 0) > 0) tag.setBoolean("dungeon_item", true);
        int stars = Math.max(Nbt.integer(extra, "upgrade_level", 0), Nbt.integer(extra, "dungeon_item_level", 0));
        if (stars > 0) tag.setInt("upgrade_count", stars);
        int potatoes = Nbt.integer(extra, "hot_potato_count", 0);
        if (potatoes > 0) tag.setInt("hot_potato_books", Math.min(potatoes, 15));
        if (Nbt.integer(extra, "art_of_war_count", 0) > 0) tag.setBoolean("art_of_war", true);
        if (Nbt.integer(extra, "artOfPeaceApplied", 0) > 0) tag.setBoolean("art_of_peace", true);
        for (String key : COUNTS) if (extra.get(key) instanceof Number n) tag.setInt(key, n.intValue());
        for (String key : TEXTS) if (Nbt.string(extra, key) instanceof String text && !text.isBlank()) tag.setString(key, text);
        for (String key : COUNTERS) if (extra.get(key) instanceof Number n) tag.setDouble(key, n.doubleValue());
        for (Object scroll : Nbt.list(extra, "ability_scroll")) {
            String flag = scroll instanceof String s ? SCROLLS.get(s) : null;
            if (flag != null && tag.hasKey(flag)) tag.setBoolean(flag, true);
        }
        enchantments(tag, Nbt.compound(extra, "enchantments"), left);
        gems(tag, kind, Nbt.compound(extra, "gems"));
        attributes(tag, Nbt.compound(extra, "attributes"));
        rune(tag, kind, Nbt.compound(extra, "runes"));
        return tag;
    }

    /** {@code {sharpness: 6, ultimate_wise: 5}} as this plugin's list: {@code [{name: "sharpness", lvl: 6}, {name: "wise", ...}]}. */
    private static void enchantments(NBTTagCompound tag, Map<String, Object> enchantments, Left left) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Object> e : enchantments.entrySet()) {
            EnchantmentData.Entry known = EnchantmentData.current().get(e.getKey());
            if (known == null || !(e.getValue() instanceof Number level)) {
                left.enchantment(e.getKey());
                continue;
            }
            NBTTagCompound enchantment = new NBTTagCompound();
            enchantment.setString("name", known.id());
            enchantment.setInt("lvl", level.intValue());
            list.add(enchantment);
        }
        tag.set("enchantments", list);
    }

    /**
     * Hypixel's {@code gems}: each slot by its type and number among that type's ({@code JASPER_0}, {@code COMBAT_1}),
     * its gem's quality ("FINE", or {@code {uuid, quality}} on newer items), a special slot's gem as
     * {@code COMBAT_0_gem}, and the slots paid for in {@code unlocked_slots}. This item's slots are in the same order.
     */
    private static void gems(NBTTagCompound tag, SkyBlockItem kind, Map<String, Object> gems) {
        if (gems.isEmpty() || kind.gemstoneSlots() == null) return;
        List<GemstoneSlot> slots = kind.gemstoneSlots().getSlots();
        for (Object key : Nbt.list(gems, "unlocked_slots")) {
            int index = key instanceof String s ? slot(slots, s) : -1;
            if (index >= 0) GemSlots.unlock(tag, kind, index);
        }
        for (Map.Entry<String, Object> e : gems.entrySet()) {
            int index = slot(slots, e.getKey());
            if (index < 0) continue;
            String quality = e.getValue() instanceof String s ? s : e.getValue() instanceof Map<?, ?> ? Nbt.string(cast(e.getValue()), "quality") : null;
            GemstoneType type = slots.get(index).getType();
            GemstoneType gem = type.gem() ? type : GemstoneType.of(Nbt.string(gems, e.getKey() + "_gem"));
            GemstoneQuality of = GemstoneQuality.of(quality);
            if (of != null && type.accepts(gem)) GemSlots.apply(tag, kind, index, new Gem(gem, of));
        }
    }

    /** Where {@code JASPER_1} (the second Jasper slot) is among the item's slots; -1 if it hasn't one. */
    static int slot(List<GemstoneSlot> slots, String key) {
        int split = key.lastIndexOf('_');
        if (split <= 0) return -1;
        int number;
        try {
            number = Integer.parseInt(key.substring(split + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
        String type = key.substring(0, split);
        for (int i = 0, seen = 0; i < slots.size(); i++) {
            if (slots.get(i).getType() != null && slots.get(i).getType().name().equals(type) && seen++ == number) return i;
        }
        return -1;
    }

    /** Hypixel's {@code {mana_pool: 5, mending: 3}}, the first two this plugin has (an attribute it hasn't would break the item). */
    private static void attributes(NBTTagCompound tag, Map<String, Object> attributes) {
        for (String key : ATTRIBUTE_KEYS) tag.remove(key);
        int written = 0;
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            Attribute attribute = attribute(e.getKey());
            if (attribute == null || !(e.getValue() instanceof Number level) || written == 2) continue;
            written++;
            tag.setString("attribute_" + written, attribute.name());
            tag.setInt("attribute_" + written + "_level", level.intValue());
        }
    }

    private static Attribute attribute(String id) {
        for (Attribute attribute : Attribute.values()) if (attribute.name().equalsIgnoreCase(id)) return attribute;
        return null;
    }

    /** Hypixel's {@code {GRAND_SEARING: 3}}, if it's a rune this plugin has and the item takes it. */
    private static void rune(NBTTagCompound tag, SkyBlockItem kind, Map<String, Object> runes) {
        for (Map.Entry<String, Object> e : runes.entrySet()) {
            for (Rune rune : Rune.values()) {
                if (!rune.name().equalsIgnoreCase(e.getKey()) || !(e.getValue() instanceof Number level)) continue;
                if (rune.getSpecificItemTypes() != null && !rune.getSpecificItemTypes().contains(kind.specificItemType())) continue;
                tag.setString("rune", rune.name());
                tag.setInt("rune_level", level.intValue());
                return;
            }
        }
    }

    /** A pet's type, from its {@code petInfo} ("GOLDEN_DRAGON"). */
    private static String pet(Map<String, Object> extra) {
        String info = Nbt.string(extra, "petInfo");
        try {
            JsonElement type = info == null ? null : JsonParser.parseString(info).getAsJsonObject().get("type");
            return type != null && type.isJsonPrimitive() ? type.getAsString().toUpperCase(Locale.ROOT) : "PET";
        } catch (JsonParseException | IllegalStateException e) {
            return "PET";
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object map) {
        return (Map<String, Object>) map;
    }
}
