package net.icxd.dungeons.recipe;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Every crafting recipe there is, and the Recipe Book's entries, as {@code collections/recipes.json} in the
 * private data folder has them (made by tools/collections/build_collections.py from NEU's recipes, Hypixel's
 * collections API and the wiki's Recipe Book pages). Hypixel's data, so it stays out of this repository. No
 * Bukkit in it.
 */
public final class RecipeData {
    public static final String FILE = "recipes.json";

    /** How many of an item a slot of a recipe takes. */
    public record Ingredient(String item, int amount) {
    }

    /**
     * One way to lay a recipe out: its nine cells, row by row (null for an empty one), and how many it makes;
     * a shapeless one's cells are only what it takes, laid out anyhow (see {@link Crafting}).
     */
    public record Shape(List<Ingredient> cells, int count, boolean shapeless) {
        public Shape {
            cells = java.util.Collections.unmodifiableList(new ArrayList<>(cells));
            if (cells.size() != 9) throw new IllegalArgumentException("a shape has 9 cells, not " + cells.size());
        }

        public Shape(List<Ingredient> cells, int count) {
            this(cells, count, false);
        }
    }

    /**
     * What unlocks a recipe: a collection's tier (a boss's too), a slayer's level (no slayers yet), or
     * something else the plugin can't tell ({@code other}, the text); all empty for a recipe everyone has.
     */
    public record Requirement(String collection, int tier, String slayer, int level, String other) {
        public static final Requirement NONE = new Requirement(null, 0, null, 0, null);

        public boolean none() {
            return collection == null && slayer == null && other == null;
        }
    }

    /** An item's crafting recipe: its shapes, what unlocks it, and the system it waits for ({@code later}: "minions"), if any. */
    public record Recipe(String result, List<Shape> shapes, Requirement requires, String later) {
        public Recipe {
            shapes = List.copyOf(shapes);
        }
    }

    /** What a Recipe Book entry is. */
    public enum Kind {
        /** A crafting recipe of an item the plugin has. */
        RECIPE,
        /** A minion's recipes: minions aren't in the plugin yet, so it's always locked. */
        MINION,
        /** A pet's recipe: pets aren't in the plugin yet, so it's always locked. */
        PET,
        /** A potion, a book or anything else there's no crafting recipe for here yet: always locked. */
        OTHER
    }

    /** A Recipe Book entry: its category, its name ("Blaze Minion"), its item (null for none) and what unlocks it. */
    public record BookEntry(String category, String name, String item, Kind kind, Requirement requires, String texture) {
    }

    private final Map<String, Recipe> recipes;
    private final List<BookEntry> book;
    private final List<String> problems;

    RecipeData(Map<String, Recipe> recipes, List<BookEntry> book, List<String> problems) {
        this.recipes = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
        this.book = List.copyOf(book);
        this.problems = List.copyOf(problems);
    }

    public static RecipeData empty() {
        return new RecipeData(Map.of(), List.of(), List.of());
    }

    /** Every recipe, by the item it makes. */
    public Map<String, Recipe> recipes() {
        return recipes;
    }

    public Recipe recipe(String item) {
        return item == null ? null : recipes.get(item);
    }

    public List<BookEntry> book() {
        return book;
    }

    public List<String> problems() {
        return problems;
    }

    /** Reads {@code recipes.json} in this folder (collections/ of the data folder); what's missing is in {@link #problems}. */
    public static RecipeData load(Path folder) {
        Path file = folder.resolve(FILE);
        if (!Files.isRegularFile(file)) return new RecipeData(Map.of(), List.of(), List.of("no " + file + ", so there are no recipes"));
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return new RecipeData(Map.of(), List.of(), List.of("couldn't read " + file + ": " + e));
        }
    }

    static RecipeData parse(JsonObject o) {
        List<String> problems = new ArrayList<>();
        Map<String, Recipe> recipes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("recipes").entrySet()) {
            try {
                JsonObject r = e.getValue().getAsJsonObject();
                List<Shape> shapes = new ArrayList<>();
                for (JsonElement s : r.getAsJsonArray("shapes")) {
                    JsonObject shape = s.getAsJsonObject();
                    List<Ingredient> cells = new ArrayList<>();
                    for (JsonElement cell : shape.getAsJsonArray("cells")) cells.add(ingredient(cell.getAsString()));
                    shapes.add(new Shape(cells, shape.has("count") ? shape.get("count").getAsInt() : 1,
                            shape.has("shapeless") && shape.get("shapeless").getAsBoolean()));
                }
                recipes.put(e.getKey(), new Recipe(e.getKey(), shapes, requirement(r), string(r, "later")));
            } catch (RuntimeException ex) {
                problems.add(e.getKey() + ": " + ex);
            }
        }
        List<BookEntry> book = new ArrayList<>();
        if (o.has("book")) {
            for (JsonElement e : o.getAsJsonArray("book")) {
                JsonObject b = e.getAsJsonObject();
                try {
                    book.add(new BookEntry(b.get("category").getAsString(), b.get("name").getAsString(), string(b, "item"),
                            b.has("kind") ? Kind.valueOf(b.get("kind").getAsString()) : Kind.RECIPE, requirement(b), string(b, "texture")));
                } catch (RuntimeException ex) {
                    problems.add("book entry " + b.get("name") + ": " + ex);
                }
            }
        }
        return new RecipeData(recipes, book, problems);
    }

    /** "BLAZE_ROD:32"; "" for an empty cell. */
    static Ingredient ingredient(String cell) {
        if (cell == null || cell.isEmpty()) return null;
        int colon = cell.lastIndexOf(':');
        return new Ingredient(cell.substring(0, colon), Integer.parseInt(cell.substring(colon + 1)));
    }

    private static Requirement requirement(JsonObject o) {
        if (!o.has("requires")) return Requirement.NONE;
        JsonObject r = o.getAsJsonObject("requires");
        return new Requirement(string(r, "collection"), r.has("tier") ? r.get("tier").getAsInt() : 0, string(r, "slayer"),
                r.has("level") ? r.get("level").getAsInt() : 0, string(r, "other"));
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
