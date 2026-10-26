package net.icxd.dungeons.pet;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;

/**
 * Hypixel's pets, as {@code tools/pets/build_pets.py} makes them ({@code pets/pets.json}, format 1, in the private
 * data checkout; its README has the format): each pet's rarities with their head, stats, ability text and numbers,
 * the XP each level takes, and the Pet Score's points and rewards. Without the file there are no pets (the log says
 * why). No server needed.
 */
public final class PetData {
    static final String FOLDER = "pets";
    static final String FILE = "pets.json";
    static final int FORMAT = 1;

    /** A number that changes with the level: {@code base + perLevel x level}, shown with {@code decimals} at most. */
    public record Formula(double base, double perLevel, int decimals) {
        /** A stat's: shown as stats are (two decimals at most). */
        public static final int STAT = -1;

        public double at(int level) {
            return base + perLevel * level;
        }
    }

    /** What the lore has after the stats, a blank line between each: an ability's name ("&6Hive", or null) and its text. */
    public record Block(String name, List<String> text) {
        public Block {
            text = List.copyOf(text);
        }
    }

    /**
     * A pet at one rarity: its name (as the pet's, unless it's another at this rarity), its head's skin, its stats and
     * numbers by level, its text ({@code {n}} in a paragraph is {@code numbers.get(n)}), whether Kat can upgrade it ("Can
     * be upgraded at Kat in The Hub!"), what that takes (null where it's unknown), and what George pays for it (0: he
     * doesn't buy it, or it's unknown).
     */
    public record Variant(Rarity rarity, String name, String texture, Map<Stat, Formula> stats, List<Formula> numbers,
                          List<Block> blocks, boolean kat, KatUpgrade katUpgrade, long george) {
        public Variant {
            stats = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(stats));
            numbers = List.copyOf(numbers);
            blocks = List.copyOf(blocks);
        }

        public Variant(Rarity rarity, String name, String texture, Map<Stat, Formula> stats, List<Formula> numbers, List<Block> blocks,
                       boolean kat) {
            this(rarity, name, texture, stats, numbers, blocks, kat, null, 0);
        }
    }

    /**
     * What Kat takes to upgrade a pet at one rarity to the next (the wiki's Kat/Costs): how long, how many coins (less
     * 0.3% a pet level, see Kat), and which items.
     */
    public record KatUpgrade(long seconds, long coins, List<ItemCost> items) {
        public KatUpgrade {
            items = List.copyOf(items);
        }
    }

    /** So many of an item, by its id. */
    public record ItemCost(String id, int amount) {
    }

    /**
     * A pet: its id ("BEE"), name, type line ("Farming Pet"), the skill it gains most XP from (null for none, as the
     * Wisp's, fed instead), whether every skill is its own (the Bingo pet's "All Skills"), its rarities, and where it
     * levels as no other does: its highest level (0: everyone's), the XP of its levels past 100, its rarities' offsets
     * into the table (null: everyone's), what its XP is multiplied by (the Reindeer's 2), and the level its stats and
     * numbers start at (the dragons' 100; 0 from the first).
     */
    public record Pet(String id, String name, String kind, Skill skill, boolean allSkills, int maxLevel, long[] extraLevels,
                      Map<Rarity, Integer> rarityOffset, double xpMultiplier, int statsFrom, Map<Rarity, Variant> variants) {
        /** It at this rarity; else at the highest it has below (a pet held at a rarity the data doesn't have). */
        public Variant variant(Rarity rarity) {
            Variant found = variants.get(rarity);
            for (int i = rarity.ordinal() - 1; found == null && i >= 0; i--) found = variants.get(Rarity.values()[i]);
            return found != null ? found : variants.values().stream().findFirst().orElse(null);
        }

        /** Its name at this rarity. */
        public String name(Rarity rarity) {
            Variant variant = variant(rarity);
            return variant == null || variant.name() == null ? name : variant.name();
        }
    }

    /** A Pet Score threshold and the Magic Find it gives ("&650 Score&8: +&b3 Magic Find"). */
    public record Reward(int score, int magicFind) {
    }

    private static final PetData EMPTY = new PetData(Map.of(), new long[0], Map.of(), 100, Map.of(), 0, List.of(), List.of());

    private final Map<String, Pet> pets;
    private final long[] levels;
    private final Map<Rarity, Integer> rarityOffset;
    private final int maxLevel;
    private final Map<Rarity, Integer> scoreRarity;
    private final int scoreMaxLevel;
    private final List<Reward> scoreRewards;
    private final List<String> problems;

    PetData(Map<String, Pet> pets, long[] levels, Map<Rarity, Integer> rarityOffset, int maxLevel, Map<Rarity, Integer> scoreRarity,
            int scoreMaxLevel, List<Reward> scoreRewards, List<String> problems) {
        this.pets = pets;
        this.levels = levels;
        this.rarityOffset = rarityOffset;
        this.maxLevel = maxLevel;
        this.scoreRarity = scoreRarity;
        this.scoreMaxLevel = scoreMaxLevel;
        this.scoreRewards = List.copyOf(scoreRewards);
        this.problems = List.copyOf(problems);
    }

    public static PetData empty() {
        return EMPTY;
    }

    /** {@code pets/pets.json} in the plugin's data folder (servermgr links the data checkout's {@code pets/} there). */
    public static PetData load(Path dataFolder) {
        Path file = dataFolder.resolve(FOLDER).resolve(FILE);
        if (!Files.isRegularFile(file)) return withProblem("no " + file + ", so there are no pets");
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return withProblem("couldn't read " + file + ": " + e);
        }
    }

    private static PetData withProblem(String problem) {
        return new PetData(Map.of(), new long[0], Map.of(), 100, Map.of(), 0, List.of(), List.of(problem));
    }

    static PetData parse(JsonObject json) {
        int format = json.has("format") ? json.get("format").getAsInt() : -1;
        if (format != FORMAT) return withProblem("format " + format + ", not " + FORMAT);
        List<String> problems = new ArrayList<>();
        long[] levels = longs(array(json, "levels"));
        Map<Rarity, Integer> offsets = rarities(object(json, "rarity_offset"), problems);
        int maxLevel = json.has("max_level") ? json.get("max_level").getAsInt() : 100;
        JsonObject score = object(json, "score");
        Map<Rarity, Integer> scoreRarity = rarities(object(score, "rarity"), problems);
        int scoreMaxLevel = score != null && score.has("max_level") ? score.get("max_level").getAsInt() : 0;
        List<Reward> rewards = new ArrayList<>();
        for (JsonElement reward : array(score, "rewards")) {
            JsonArray pair = reward.getAsJsonArray();
            rewards.add(new Reward(pair.get(0).getAsInt(), pair.get(1).getAsInt()));
        }
        Map<String, Pet> pets = new LinkedHashMap<>();
        JsonObject all = object(json, "pets");
        for (Map.Entry<String, JsonElement> entry : all == null ? List.<Map.Entry<String, JsonElement>>of() : all.entrySet()) {
            try {
                pets.put(entry.getKey(), pet(entry.getKey(), entry.getValue().getAsJsonObject(), problems));
            } catch (RuntimeException e) {
                problems.add(entry.getKey() + ": left out (" + e + ")");
            }
        }
        return new PetData(pets, levels, offsets, maxLevel, scoreRarity, scoreMaxLevel, rewards, problems);
    }

    private static Pet pet(String id, JsonObject json, List<String> problems) {
        String skillName = string(json, "skill");
        Skill skill = null;
        boolean allSkills = "ALL".equals(skillName);
        if (skillName != null && !allSkills) {
            try {
                skill = Skill.valueOf(skillName);
            } catch (IllegalArgumentException e) {
                problems.add(id + ": skill " + skillName + " isn't one of the plugin's (it gains no XP)");
            }
        }
        Map<Rarity, Variant> variants = new EnumMap<>(Rarity.class);
        JsonObject rarities = object(json, "rarities");
        for (Map.Entry<String, JsonElement> entry : rarities == null ? List.<Map.Entry<String, JsonElement>>of() : rarities.entrySet()) {
            Rarity rarity = rarity(entry.getKey());
            if (rarity == null) {
                problems.add(id + ": rarity " + entry.getKey() + " isn't one of the plugin's (left out)");
                continue;
            }
            variants.put(rarity, variant(id, rarity, entry.getValue().getAsJsonObject(), problems));
        }
        JsonObject offsets = object(json, "rarity_offset");
        return new Pet(id, string(json, "name"), string(json, "kind"), skill, allSkills,
                json.has("max_level") ? json.get("max_level").getAsInt() : 0, longs(array(json, "extra_levels")),
                offsets == null ? null : rarities(offsets, problems), json.has("xp_multiplier") ? json.get("xp_multiplier").getAsDouble() : 1,
                json.has("stats_from") ? json.get("stats_from").getAsInt() : 0, variants);
    }

    private static Variant variant(String id, Rarity rarity, JsonObject json, List<String> problems) {
        Map<Stat, Formula> stats = new LinkedHashMap<>();
        JsonObject statJson = object(json, "stats");
        for (Map.Entry<String, JsonElement> entry : statJson == null ? List.<Map.Entry<String, JsonElement>>of() : statJson.entrySet()) {
            try {
                stats.put(Stat.valueOf(entry.getKey()), formula(entry.getValue().getAsJsonObject(), Formula.STAT));
            } catch (IllegalArgumentException e) {
                problems.add(id + " " + rarity + ": stat " + entry.getKey() + " isn't one of the plugin's (left out)");
            }
        }
        List<Formula> numbers = new ArrayList<>();
        for (JsonElement number : array(json, "numbers")) numbers.add(formula(number.getAsJsonObject(), 2));
        List<Block> blocks = new ArrayList<>();
        for (JsonElement element : array(json, "blocks")) {
            JsonObject block = element.getAsJsonObject();
            List<String> text = new ArrayList<>();
            for (JsonElement line : array(block, "text")) text.add(line.getAsString());
            blocks.add(new Block(string(block, "name"), text));
        }
        KatUpgrade upgrade = null;
        JsonObject kat = object(json, "kat_upgrade");
        if (kat != null) {
            List<ItemCost> items = new ArrayList<>();
            for (JsonElement item : array(kat, "items")) {
                JsonArray pair = item.getAsJsonArray();
                items.add(new ItemCost(pair.get(0).getAsString(), pair.get(1).getAsInt()));
            }
            upgrade = new KatUpgrade(kat.has("seconds") ? kat.get("seconds").getAsLong() : 0, kat.has("coins") ? kat.get("coins").getAsLong() : 0,
                    items);
        }
        return new Variant(rarity, string(json, "name"), string(json, "texture"), stats, numbers, blocks,
                json.has("kat") && json.get("kat").getAsBoolean(), upgrade, json.has("george") ? json.get("george").getAsLong() : 0);
    }

    private static Formula formula(JsonObject json, int decimals) {
        return new Formula(json.get("base").getAsDouble(), json.get("per_level").getAsDouble(),
                json.has("decimals") ? json.get("decimals").getAsInt() : decimals);
    }

    // What's in it

    public Pet pet(String id) {
        return id == null ? null : pets.get(id.toUpperCase(Locale.ROOT));
    }

    public Collection<Pet> pets() {
        return pets.values();
    }

    public int count() {
        return pets.size();
    }

    public List<String> problems() {
        return problems;
    }

    // Levels

    /** The highest level a pet reaches: its own (200 for the dragons), else everyone's (100). */
    public int maxLevel(Pet pet) {
        return pet != null && pet.maxLevel() > 0 ? pet.maxLevel() : maxLevel;
    }

    /** Where a rarity starts in the XP table (a Rare's level 2 takes the table's 12th): the pet's own, else everyone's. */
    int offset(Pet pet, Rarity rarity) {
        Map<Rarity, Integer> offsets = pet != null && pet.rarityOffset() != null ? pet.rarityOffset() : rarityOffset;
        return offsets.getOrDefault(rarity, 0);
    }

    /**
     * The XP level {@code level} takes from the one before (0 for level 1 or past the most): the table from the
     * rarity's offset, then the pet's own levels past it (NEU's {@code custom_pet_leveling}, appended).
     */
    public long xpFor(Pet pet, Rarity rarity, int level) {
        if (level < 2 || level > maxLevel(pet)) return 0;
        int index = offset(pet, rarity) + level - 2;
        if (index < levels.length) return levels[index];
        long[] extra = pet == null ? new long[0] : pet.extraLevels();
        return index - levels.length < extra.length ? extra[index - levels.length] : 0;
    }

    /** The XP level {@code level} takes from nothing (level 1's is 0). */
    public long totalXp(Pet pet, Rarity rarity, int level) {
        long total = 0;
        for (int l = 2; l <= Math.min(level, maxLevel(pet)); l++) total += xpFor(pet, rarity, l);
        return total;
    }

    /** The level this much XP reaches, 1 at least, its most at most (XP past it is kept, as Hypixel keeps it). */
    public int level(Pet pet, Rarity rarity, double xp) {
        int level = 1;
        double total = 0;
        for (int l = 2; l <= maxLevel(pet); l++) {
            total += xpFor(pet, rarity, l);
            if (xp < total) break;
            level = l;
        }
        return level;
    }

    /** XP into the level after the one this much reaches (0 at the most). */
    public double xpIntoLevel(Pet pet, Rarity rarity, double xp) {
        int level = level(pet, rarity, xp);
        return level >= maxLevel(pet) ? 0 : Math.max(0, xp - totalXp(pet, rarity, level));
    }

    // The Pet Score

    /** A unique pet type's points at its best rarity ("+1→6 Pet Score"; none for Special and Very Special). */
    public int scoreOf(Rarity rarity) {
        return scoreRarity.getOrDefault(rarity, 0);
    }

    /** What a pet at its most level adds ("You gain an additional score for each max level pet you have!"). */
    public int scoreMaxLevel() {
        return scoreMaxLevel;
    }

    /** The thresholds, lowest first. */
    public List<Reward> scoreRewards() {
        return scoreRewards;
    }

    // Reading

    static Rarity rarity(String name) {
        try {
            return name == null ? null : Rarity.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Map<Rarity, Integer> rarities(JsonObject json, List<String> problems) {
        Map<Rarity, Integer> out = new EnumMap<>(Rarity.class);
        for (Map.Entry<String, JsonElement> entry : json == null ? List.<Map.Entry<String, JsonElement>>of() : json.entrySet()) {
            Rarity rarity = rarity(entry.getKey());
            if (rarity == null) problems.add("rarity " + entry.getKey() + " isn't one of the plugin's");
            else out.put(rarity, entry.getValue().getAsInt());
        }
        return out;
    }

    private static long[] longs(JsonArray array) {
        long[] out = new long[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsLong();
        return out;
    }

    private static JsonObject object(JsonObject json, String key) {
        return json != null && json.has(key) && json.get(key).isJsonObject() ? json.getAsJsonObject(key) : null;
    }

    private static JsonArray array(JsonObject json, String key) {
        return json != null && json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : new JsonArray();
    }

    private static String string(JsonObject json, String key) {
        return json != null && json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : null;
    }
}
