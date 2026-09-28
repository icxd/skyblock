package net.icxd.dungeons.item;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.behaviour.ItemBehaviours;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Every item in the real items.json as the plugin shows it: all it says about itself, the data a new one
 * starts with, and its name and lore fresh, with 7 stars, with Sharpness V, recombobulated and (on dungeon
 * items) with two enchantments. Compared with golden.json, so a change that shows on any item fails here;
 * {@code -Dgolden.update=true} writes it anew. Both files are Hypixel's text, so they're in the private data
 * repository (see {@link #itemsFile}, and -Ditems.golden for another golden), as is the enchantments' text (the
 * Hex's table, -Dhex.dir); without them, this is skipped.
 */
class GoldenItemsTest {
    /** Data that's random on every new item. */
    private static final List<String> RANDOM = List.of("attribute_1", "attribute_1_level", "attribute_2", "attribute_2_level", "uuid");

    @AfterEach
    void noItems() {
        ItemRegistry.clearData();
        FakeEnchantments.reset();
    }

    /** The private data repository's items.json: -Ditems.file, else the checkout next to this repository's. */
    static Path itemsFile() {
        String property = System.getProperty("items.file");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data/items/items.json");
    }

    /** -Ditems.golden, else golden.json next to items.json. */
    private static Path goldenFile() {
        String property = System.getProperty("items.golden");
        return property != null ? Path.of(property) : itemsFile().resolveSibling("golden.json");
    }

    @Test
    void itemsAreUnchanged() throws IOException {
        Path items = itemsFile(), golden = goldenFile();
        boolean update = Boolean.getBoolean("golden.update");
        assumeTrue(Files.exists(items), "no " + items);
        assumeTrue(update || Files.exists(golden), "no " + golden + "; run with -Dgolden.update=true to write it");
        assertNull(ItemRegistry.loadData(items).failure(), "the items didn't load");
        // The enchantments' text is the Hex's table (-Dhex.dir); skipped without it.
        EnchantmentData.use(FakeEnchantments.real());
        String actual = golden(ItemRegistry.getRegistry());
        if (update) {
            Files.writeString(golden, actual, StandardCharsets.UTF_8);
            return;
        }
        String expected = Files.readString(golden, StandardCharsets.UTF_8);
        if (expected.equals(actual)) return;
        // The first line that differs and whose item it is, rather than the whole file.
        String[] want = expected.split("\n", -1), got = actual.split("\n", -1);
        for (int i = 0; i < Math.max(want.length, got.length); i++) {
            String a = i < want.length ? want[i] : "<end>", b = i < got.length ? got[i] : "<end>";
            if (a.equals(b)) continue;
            String item = "?";
            for (int j = Math.min(i, want.length - 1); j >= 0; j--) {
                if (want[j].startsWith("  \"") && want[j].endsWith("{")) {
                    item = want[j].trim();
                    break;
                }
            }
            fail("items differ from " + golden + " at line " + (i + 1) + ", in " + item + "\nexpected: " + a + "\nactual:   " + b);
        }
    }

    private static String golden(Map<String, SkyBlockItem> items) {
        Map<String, SkyBlockItem> byId = new TreeMap<>(items);
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
        o.addProperty("type_key", item.typeKey());
        o.addProperty("type_label", item.typeLabel());
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
        JsonArray blocks = new JsonArray();
        for (ItemBlock block : item.blocks()) blocks.add(block(block));
        o.add("blocks", blocks);
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
        NBTTagCompound nbt = ItemBehaviours.of(item).nbt(item);
        o.add("nbt_keys", nbt == null ? JsonNull.INSTANCE : strings(nbt.keySet()));
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

    private static JsonObject block(ItemBlock block) {
        JsonObject o = new JsonObject();
        o.addProperty("kind", block.kind());
        o.addProperty("name", block.name());
        o.addProperty("header", block.header());
        o.addProperty("activation", block.activation());
        o.add("text", strings(block.text()));
        o.addProperty("mana", block.mana());
        o.addProperty("mana_percent", block.manaPercent());
        o.addProperty("cooldown", block.cooldown());
        o.addProperty("soulflow", block.soulflow());
        o.addProperty("health_cost", block.healthCost());
        o.addProperty("vitality", block.vitality());
        o.addProperty("pieces", block.pieces());
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
