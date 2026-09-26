package net.icxd.dungeons.item.data;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.icxd.dungeons.crimsonisle.kuudra.KuudraTier;
import net.icxd.dungeons.item.ability.AbilityActivation;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.cost.UpgradeCost;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.cost.coins.CoinCost;
import net.icxd.dungeons.item.cost.essence.EssenceCost;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.cost.item.ItemCost;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.requirement.KuudraTierRequirement;
import net.icxd.dungeons.item.requirement.Requirement;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonTierRequirement;
import net.icxd.dungeons.item.requirement.dungeontier.DungeonType;
import net.icxd.dungeons.item.requirement.hotm.HeartOfTheMountainRequirement;
import net.icxd.dungeons.item.requirement.skill.SkillRequirement;
import net.icxd.dungeons.item.requirement.slayer.SlayerBossType;
import net.icxd.dungeons.item.requirement.slayer.SlayerRequirement;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads items.json (format 1): Hypixel's items as data, an object per item id with only name and
 * material required. Each item is read on its own, so a bad one is skipped and the rest still load.
 * <ul>
 *   <li>Errors, the item is skipped: no name ("name"); no material, or one that isn't a current,
 *       non-legacy Material ("material"); an unknown rarity ("rarity") or soulbound ("soulbound"); an
 *       id that came before ("duplicate"); a value of the wrong kind, such as a string where a number
 *       goes, a fraction for a whole number, a colour that isn't "#rrggbb" or a cost of nothing ("value").</li>
 *   <li>Warnings, the item loads without the name this plugin doesn't have (yet): a stat ("stat"),
 *       gemstone slot type ("gemstone slot"), essence ("essence"), requirement, or its skill, slayer
 *       boss, dungeon or Kuudra tier ("requirement"), or activation ("activation"). A type that isn't a
 *       SpecificItemType ("type") is kept as the rarity line's words and the item's type key.</li>
 * </ul>
 * The whole file fails (an IOException) only if it isn't JSON, or isn't format 1.
 */
public final class ItemData {
    public static final int FORMAT = 1;

    /** The items by id, in the file's order, and what was wrong with the others. */
    public record Result(Map<String, DataItem> items, List<Problem> errors, List<Problem> warnings) {
    }

    /** What's wrong with an item: what kind of thing ("material", "stat") and what the data said. */
    public record Problem(String id, String kind, String detail) {
        @Override
        public String toString() {
            return id + ": " + kind + " " + detail;
        }
    }

    // What the file says, as Gson reads it. Names stay strings until they're looked up, so an unknown
    // one is reported (Gson would read it as null); numbers are boxed, so a missing one is null.
    private record ItemJson(String name, String material, String rarity, String type, String typeLabel,
                            List<String> categories, String texture, String skin, String color, Boolean glowing,
                            Boolean unstackable, Boolean dungeonItem, Boolean canHaveAttributes, Boolean reforgeable,
                            String soulbound, Integer gearScore, Double npcSellPrice, Map<String, Double> stats,
                            Double shotCooldown, List<SlotJson> gemstoneSlots, List<List<CostJson>> upgradeCosts,
                            List<RequirementJson> requirements, List<String> lore, List<BlockJson> abilities) {
    }

    private record SlotJson(String type, List<CostJson> costs) {
    }

    private record CostJson(Integer coins, String item, String essence, Integer amount) {
    }

    /** The API's requirement objects, with the fields of the kinds this plugin has. */
    private record RequirementJson(String type, String skill, Integer level, String slayerBossType, String dungeonType,
                                   Integer tier, String kuudraTier) {
    }

    private record BlockJson(String kind, String name, String header, String activation, List<String> text, Double mana,
                             Double manaPercent, Double cooldown, Double soulflow, Double healthCost, Double vitality, Integer pieces) {
    }

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            // Gson would take "true" and "5" from strings, and 1.5 as the whole number 1; here they're errors.
            .registerTypeAdapter(Boolean.class, only(JsonToken.BOOLEAN, JsonReader::nextBoolean))
            .registerTypeAdapter(Double.class, only(JsonToken.NUMBER, JsonReader::nextDouble))
            .registerTypeAdapter(Integer.class, only(JsonToken.NUMBER, ItemData::wholeNumber))
            .create();
    private static final TypeAdapter<JsonElement> TREE = GSON.getAdapter(JsonElement.class);
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    private ItemData() {
    }

    public static Result load(Reader file) throws IOException {
        Map<String, DataItem> items = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        List<Problem> errors = new ArrayList<>();
        List<Problem> warnings = new ArrayList<>();
        Integer format = null;
        JsonReader in = new JsonReader(file);
        in.setStrictness(Strictness.STRICT);
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "format" -> format = in.nextInt();
                case "items" -> {
                    in.beginObject();
                    while (in.hasNext()) {
                        String id = in.nextName();
                        // Read whole first, so whatever is wrong with the item, the next one starts where it should.
                        JsonElement json = TREE.read(in);
                        if (!ids.add(id.toUpperCase(Locale.ROOT))) {
                            errors.add(new Problem(id, "duplicate", "id given before"));
                            continue;
                        }
                        Warnings itemWarnings = new Warnings(id, new ArrayList<>());
                        try {
                            items.put(id, item(id, GSON.fromJson(json, ItemJson.class), itemWarnings));
                            warnings.addAll(itemWarnings.list());
                        } catch (Skip e) {
                            errors.add(new Problem(id, e.kind, e.getMessage()));
                        } catch (RuntimeException e) {
                            errors.add(new Problem(id, "value", e.getMessage() == null ? e.toString() : e.getMessage()));
                        }
                    }
                    in.endObject();
                }
                default -> in.skipValue();
            }
        }
        in.endObject();
        if (in.peek() != JsonToken.END_DOCUMENT) throw new IOException("more after the items");
        if (format == null || format != FORMAT) throw new IOException("format " + format + ", but this plugin reads format " + FORMAT);
        return new Result(items, errors, warnings);
    }

    private static DataItem item(String id, ItemJson json, Warnings warnings) throws Skip {
        if (json.name() == null || json.name().isBlank()) throw new Skip("name", "missing");
        if (json.material() == null) throw new Skip("material", "missing");
        Material material = Material.getMaterial(json.material());
        if (material == null || material.isLegacy()) throw new Skip("material", json.material());
        // Only a running server knows which materials are items (isItem looks in the item registry).
        if (Bukkit.getServer() != null && !material.isItem()) throw new Skip("material", json.material() + " isn't an item");
        Rarity rarity = json.rarity() == null ? Rarity.COMMON : named(Rarity.class, json.rarity());
        if (rarity == null) throw new Skip("rarity", json.rarity());
        Soulbound soulbound = json.soulbound() == null ? Soulbound.NONE : named(Soulbound.class, json.soulbound());
        if (soulbound == null) throw new Skip("soulbound", json.soulbound());

        SpecificItemType type = SpecificItemType.NONE;
        String typeLabel = json.typeLabel();
        if (json.type() != null) {
            SpecificItemType known = named(SpecificItemType.class, json.type());
            if (known != null) {
                type = known;
            } else {
                warnings.add("type", json.type());
                // Its rarity line still says what it is.
                if (typeLabel == null) typeLabel = json.type().replace('_', ' ');
            }
        }
        String typeKey = json.type() == null || json.type().equals("NONE") ? "OTHER" : json.type();

        Color color = null;
        if (json.color() != null) {
            if (!COLOR.matcher(json.color()).matches()) throw new Skip("value", "color " + json.color());
            color = Color.fromRGB(Integer.parseInt(json.color().substring(1), 16));
        }
        String skin = json.skin() != null ? json.skin() : json.texture() == null ? null : Utils.texture(json.texture());

        Stats stats = new Stats();
        if (json.stats() != null) {
            for (Map.Entry<String, Double> stat : json.stats().entrySet()) {
                Stat known = named(Stat.class, stat.getKey());
                if (known == null) warnings.add("stat", stat.getKey());
                else stats.set(known, need(stat.getValue(), "value for " + stat.getKey()));
            }
        }

        GemstoneSlots gemstoneSlots = null;
        if (json.gemstoneSlots() != null) {
            List<GemstoneSlot> slots = new ArrayList<>();
            for (SlotJson slot : json.gemstoneSlots()) {
                GemstoneType slotType = named(GemstoneType.class, need(slot.type(), "gemstone slot type"));
                if (slotType == null) warnings.add("gemstone slot", slot.type());
                else slots.add(new GemstoneSlot(slotType, costs(slot.costs(), warnings)));
            }
            // With none this plugin has, no "Gemstones:" line.
            if (!slots.isEmpty()) gemstoneSlots = new GemstoneSlots(slots.toArray(GemstoneSlot[]::new));
        }

        UpgradeCosts upgradeCosts = null;
        if (json.upgradeCosts() != null && !json.upgradeCosts().isEmpty()) {
            List<UpgradeCost> stars = new ArrayList<>();
            for (List<CostJson> star : json.upgradeCosts()) stars.add(new UpgradeCost(costs(star, warnings)));
            upgradeCosts = new UpgradeCosts(stars.toArray(UpgradeCost[]::new));
        }

        Requirements requirements = null;
        if (json.requirements() != null) {
            List<Requirement> known = new ArrayList<>();
            for (RequirementJson requirement : json.requirements()) {
                Requirement converted = requirement(requirement, warnings);
                if (converted != null) known.add(converted);
            }
            if (!known.isEmpty()) requirements = new Requirements(known.toArray(Requirement[]::new));
        }

        List<ItemBlock> blocks = new ArrayList<>();
        if (json.abilities() != null) {
            for (BlockJson block : json.abilities()) {
                String activation = block.activation();
                if (activation != null && named(AbilityActivation.class, activation) == null) {
                    warnings.add("activation", activation);
                    activation = null;
                }
                blocks.add(new ItemBlock(block.kind(), block.name(), block.header(), activation, block.text(), number(block.mana()),
                        number(block.manaPercent()), number(block.cooldown()), number(block.soulflow()), number(block.healthCost()), number(block.vitality()),
                        block.pieces() == null ? 0 : block.pieces()));
            }
        }

        return new DataItem(id, json.name(), material, rarity, type, typeKey, typeLabel, list(json.categories()), skin, color,
                flag(json.glowing()), flag(json.unstackable()), flag(json.dungeonItem()), flag(json.canHaveAttributes()),
                json.reforgeable(), soulbound, json.gearScore() == null ? 0 : json.gearScore(), number(json.npcSellPrice()), stats,
                number(json.shotCooldown()), gemstoneSlots, upgradeCosts, requirements, list(json.lore()), blocks);
    }

    /** The costs this plugin has: coins, an item or an essence. */
    private static Cost[] costs(List<CostJson> costs, Warnings warnings) throws Skip {
        if (costs == null) return new Cost[0];
        List<Cost> known = new ArrayList<>();
        for (CostJson cost : costs) {
            if (cost.coins() != null) {
                known.add(new CoinCost(cost.coins()));
            } else if (cost.item() != null) {
                known.add(new ItemCost(cost.item(), need(cost.amount(), "amount of " + cost.item())));
            } else if (cost.essence() != null) {
                EssenceType essence = named(EssenceType.class, cost.essence());
                if (essence == null) warnings.add("essence", cost.essence());
                else known.add(new EssenceCost(essence, need(cost.amount(), "amount of " + cost.essence() + " essence")));
            } else {
                throw new Skip("value", "a cost of nothing");
            }
        }
        return known.toArray(Cost[]::new);
    }

    /** The plugin's requirement; null (and a warning) if it has none like it. */
    private static Requirement requirement(RequirementJson json, Warnings warnings) throws Skip {
        String type = need(json.type(), "requirement type");
        switch (type) {
            case "SKILL" -> {
                Skill skill = named(Skill.class, need(json.skill(), "skill"));
                if (skill != null) return new SkillRequirement(skill, need(json.level(), "skill level"));
                warnings.add("requirement", type + " " + json.skill());
            }
            case "SLAYER" -> {
                // The API writes the boss in lower case ("enderman").
                String boss = need(json.slayerBossType(), "slayer boss");
                SlayerBossType bossType = named(SlayerBossType.class, boss.toUpperCase(Locale.ROOT));
                if (bossType != null) return new SlayerRequirement(bossType, need(json.level(), "slayer level"));
                warnings.add("requirement", type + " " + boss);
            }
            case "DUNGEON_TIER" -> {
                DungeonType dungeon = named(DungeonType.class, need(json.dungeonType(), "dungeon type"));
                if (dungeon != null) return new DungeonTierRequirement(dungeon, need(json.tier(), "dungeon tier"));
                warnings.add("requirement", type + " " + json.dungeonType());
            }
            case "HEART_OF_THE_MOUNTAIN" -> {
                return new HeartOfTheMountainRequirement(need(json.tier(), "Heart of the Mountain tier"));
            }
            case "KUUDRA_COMPLETION" -> {
                // The API calls the basic tier NONE; the game says "Kuudra Basic Tier Completion" (see CrimsonArmor).
                String name = need(json.kuudraTier(), "Kuudra tier");
                KuudraTier tier = name.equals("NONE") ? KuudraTier.BASIC : named(KuudraTier.class, name);
                if (tier != null) return new KuudraTierRequirement(tier);
                warnings.add("requirement", type + " " + name);
            }
            default -> warnings.add("requirement", type);
        }
        return null;
    }

    /** The constant with that name; null if there's none. */
    private static <E extends Enum<E>> E named(Class<E> type, String name) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static <T> T need(T value, String what) throws Skip {
        if (value == null) throw new Skip("value", "no " + what);
        return value;
    }

    private static double number(Double value) {
        return value == null ? 0 : value;
    }

    private static boolean flag(Boolean value) {
        return value != null && value;
    }

    private static List<String> list(List<String> value) {
        return value == null ? List.of() : value;
    }

    private static Integer wholeNumber(JsonReader in) throws IOException {
        double value = in.nextDouble();
        if (value != Math.rint(value) || Math.abs(value) > Integer.MAX_VALUE) {
            throw new JsonSyntaxException("expected a whole number at " + in.getPath() + ", not " + value);
        }
        return (int) value;
    }

    private interface Read<T> {
        T read(JsonReader in) throws IOException;
    }

    /** Reads a value only from a token of that kind (or null). */
    private static <T> TypeAdapter<T> only(JsonToken token, Read<T> read) {
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonToken next = in.peek();
                if (next == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                if (next != token) throw new JsonSyntaxException("expected " + token + " at " + in.getPath() + ", not " + next);
                return read.read(in);
            }
        };
    }

    /** Where an item's warnings go. */
    private record Warnings(String id, List<Problem> list) {
        void add(String kind, String detail) {
            list.add(new Problem(id, kind, detail));
        }
    }

    /** Why an item can't be loaded. */
    private static final class Skip extends Exception {
        final String kind;

        Skip(String kind, String detail) {
            super(detail, null, false, false);
            this.kind = kind;
        }
    }
}
