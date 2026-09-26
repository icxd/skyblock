package net.icxd.dungeons.item;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.item.requirement.KuudraTierRequirement;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.hotm.HeartOfTheMountainRequirement;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Every item in {@link ItemRegistry} as it is today: all it says about itself, the data a new one
 * starts with, and its name and lore fresh, with 7 stars, with Sharpness V, recombobulated and (on
 * dungeon items) with two enchantments. Compared with src/test/resources/golden/items.json, so a
 * change that shows on any item fails here; {@code -Dgolden.update=true} writes the file anew.
 */
class GoldenItemsTest {
    private static final Path GOLDEN = Path.of(System.getProperty("basedir", "."), "src/test/resources/golden/items.json");
    /** Data that's random on every new item. */
    private static final List<String> RANDOM = List.of("attribute_1", "attribute_1_level", "attribute_2", "attribute_2_level", "uuid");

    @Test
    void itemsAreUnchanged() throws IOException {
        assertEquals(71, ItemRegistry.getRegistry().size());
        String actual = golden();
        if (Boolean.getBoolean("golden.update")) {
            Files.createDirectories(GOLDEN.getParent());
            Files.writeString(GOLDEN, actual, StandardCharsets.UTF_8);
            return;
        }
        assertTrue(Files.exists(GOLDEN), GOLDEN + " is missing; run with -Dgolden.update=true to write it");
        String expected = Files.readString(GOLDEN, StandardCharsets.UTF_8);
        if (expected.equals(actual)) return;
        // The first line that differs, rather than the whole file.
        String[] want = expected.split("\n", -1), got = actual.split("\n", -1);
        for (int i = 0; i < Math.max(want.length, got.length); i++) {
            String a = i < want.length ? want[i] : "<end>", b = i < got.length ? got[i] : "<end>";
            if (!a.equals(b)) fail("items differ from " + GOLDEN + " at line " + (i + 1) + "\nexpected: " + a + "\nactual:   " + b);
        }
    }

    static String golden() {
        Map<String, SkyBlockItem> byId = new TreeMap<>(ItemRegistry.getRegistry());
        JsonObject all = new JsonObject();
        for (SkyBlockItem item : byId.values()) {
            JsonObject entry = new JsonObject();
            entry.add("item", describe(item));
            entry.add("new_data", compound(fresh(item)));
            JsonObject renders = new JsonObject();
            renders.add("fresh", render(item, fresh(item)));
            if (item.upgradeCosts() != null) {
                NBTTagCompound tag = fresh(item);
                tag.setInt("upgrade_count", 7);
                renders.add("stars_7", render(item, tag));
            }
            NBTTagCompound sharp = fresh(item);
            enchant(sharp, "sharpness", 5);
            renders.add("sharpness_5", render(item, sharp));
            // As /recombobulate has stored it: the upgraded rarity and the flag.
            NBTTagCompound recombobulated = fresh(item);
            recombobulated.setString("rarity", item.rarity().upgrade().name());
            recombobulated.setBoolean("recombobulated", true);
            renders.add("recombobulated", render(item, recombobulated));
            if (item.dungeonItem()) {
                NBTTagCompound tag = fresh(item);
                enchant(tag, "smite", 7);
                enchant(tag, "critical", 6);
                renders.add("two_enchantments", render(item, tag));
            }
            entry.add("renders", renders);
            all.add(item.id(), entry);
        }
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create().toJson(all) + "\n";
    }

    private static NBTTagCompound fresh(SkyBlockItem item) {
        NBTTagCompound tag = ItemBuilder.newData(item);
        for (String key : RANDOM) tag.remove(key);
        return tag;
    }

    private static void enchant(NBTTagCompound tag, String name, int level) {
        NBTTagList list = tag.getList("enchantments", 10);
        NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setString("name", name);
        enchantment.setInt("lvl", level);
        list.add(enchantment);
        tag.set("enchantments", list);
    }

    private static JsonObject render(SkyBlockItem item, NBTTagCompound tag) {
        JsonObject render = new JsonObject();
        render.addProperty("name", ItemBuilder.name(item, tag));
        render.add("lore", strings(ItemBuilder.lore(item, tag)));
        return render;
    }

    /** Every getter, in the interface's order. */
    private static JsonObject describe(SkyBlockItem item) {
        JsonObject o = new JsonObject();
        o.addProperty("id", item.id());
        o.addProperty("name", item.name());
        o.addProperty("material", item.material().name());
        o.addProperty("rarity", item.rarity().name());
        o.addProperty("skin", item.skin());
        o.addProperty("color", item.color() == null ? null : String.format("#%06x", item.color().asRGB()));
        o.addProperty("glowing", item.glowing());
        o.addProperty("specific_item_type", item.specificItemType().name());
        o.addProperty("generic_item_type", item.genericItemType() == null ? null : item.genericItemType().name());
        o.add("categories", strings(item.categories()));
        o.addProperty("gear_score", item.gearScore());
        o.add("stats", stats(item.stats()));
        o.addProperty("shot_cooldown", item.shotCooldown());
        if (item.gemstoneSlots() == null) {
            o.add("gemstone_slots", JsonNull.INSTANCE);
        } else {
            JsonArray slots = new JsonArray();
            for (GemstoneSlot slot : item.gemstoneSlots().getSlots()) {
                JsonObject s = new JsonObject();
                s.addProperty("type", slot.getType().name());
                s.add("costs", costs(slot.getCosts()));
                slots.add(s);
            }
            o.add("gemstone_slots", slots);
        }
        o.add("lore", strings(item.lore()));
        o.add("ability", ability(item.ability()));
        o.addProperty("reforgeable", item.reforgeable());
        o.addProperty("soulbound", item.soulbound().name());
        if (item.requirements() == null) {
            o.add("requirements", JsonNull.INSTANCE);
        } else {
            JsonArray requirements = new JsonArray();
            for (Requirement requirement : item.requirements().getRequirements()) requirements.add(requirement(requirement));
            o.add("requirements", requirements);
        }
        if (item.upgradeCosts() == null) {
            o.add("upgrade_costs", JsonNull.INSTANCE);
        } else {
            JsonArray stars = new JsonArray();
            for (UpgradeCost star : item.upgradeCosts().getCosts()) stars.add(costs(star.getCosts()));
            o.add("upgrade_costs", stars);
        }
        o.addProperty("can_have_attributes", item.canHaveAttributes());
        o.addProperty("dungeon_item", item.dungeonItem());
        o.addProperty("unstackable", item.unstackable());
        o.addProperty("npc_sell_price", item.npcSellPrice());
        o.add("nbt_keys", item.nbt() == null ? JsonNull.INSTANCE : strings(item.nbt().keySet()));
        o.addProperty("ownable", item.isOwnable());
        return o;
    }

    private static JsonObject stats(Stats stats) {
        JsonObject o = new JsonObject();
        for (Stat stat : Stat.values()) if (stats.has(stat)) o.addProperty(stat.name(), stats.get(stat));
        return o;
    }

    private static JsonArray costs(List<Cost> costs) {
        JsonArray array = new JsonArray();
        for (Cost cost : costs) {
            JsonObject o = new JsonObject();
            switch (cost) {
                case CoinCost c -> o.addProperty("coins", c.getAmount());
                case ItemCost c -> {
                    o.addProperty("item", c.getItemId());
                    o.addProperty("amount", c.getAmount());
                }
                case EssenceCost c -> {
                    o.addProperty("essence", c.getEssenceType().name());
                    o.addProperty("amount", c.getAmount());
                }
                default -> throw new IllegalStateException("no golden form for " + cost.getClass());
            }
            array.add(o);
        }
        return array;
    }

    private static JsonElement ability(Ability ability) {
        if (ability == null) return JsonNull.INSTANCE;
        JsonObject o = new JsonObject();
        o.addProperty("class", ability.getClass().getSimpleName());
        o.addProperty("name", ability.getName());
        o.addProperty("type", ability.getType().name());
        o.addProperty("activation", ability.getActivation().name());
        o.addProperty("description", ability.getDescription());
        o.addProperty("cooldown", ability.getCooldown());
        o.addProperty("mana_cost", ability.getManaCost());
        o.addProperty("soulflow_cost", ability.getSoulflowCost());
        o.addProperty("show_mana_cost", ability.isShowManaCost());
        return o;
    }

    private static JsonObject requirement(Requirement requirement) {
        JsonObject o = new JsonObject();
        o.addProperty("class", requirement.getClass().getSimpleName());
        switch (requirement) {
            case SkillRequirement r -> {
                o.addProperty("skill", r.getSkill().name());
                o.addProperty("level", r.getLevel());
            }
            case SlayerRequirement r -> {
                o.addProperty("boss", r.getBossType().name());
                o.addProperty("level", r.getLevel());
            }
            case DungeonTierRequirement r -> {
                o.addProperty("dungeon", r.getDungeonType().name());
                o.addProperty("tier", r.getTier());
            }
            case HeartOfTheMountainRequirement r -> o.addProperty("level", r.getLevel());
            case KuudraTierRequirement r -> o.addProperty("tier", r.getTier().name());
            default -> throw new IllegalStateException("no golden form for " + requirement.getClass());
        }
        o.add("lore", strings(requirement.lore()));
        return o;
    }

    /** Each key with its value as NBT writes it ("0b", "\"EPIC\""), so the type shows too. */
    private static JsonObject compound(NBTTagCompound tag) {
        JsonObject o = new JsonObject();
        for (String key : tag.keySet()) o.addProperty(key, tag.get(key).toString());
        return o;
    }

    private static JsonArray strings(Collection<String> strings) {
        JsonArray array = new JsonArray();
        for (String s : strings) array.add(s);
        return array;
    }
}
