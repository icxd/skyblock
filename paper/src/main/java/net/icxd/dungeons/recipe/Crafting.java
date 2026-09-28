package net.icxd.dungeons.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Predicate;

import net.icxd.dungeons.recipe.RecipeData.Ingredient;
import net.icxd.dungeons.recipe.RecipeData.Recipe;
import net.icxd.dungeons.recipe.RecipeData.Shape;

/**
 * Crafting on the 3x3 grid, as data: which recipe the grid's items make, how many times, and what's left
 * once it's made. A shaped recipe fits where the grid has its items, at least as many of each as it takes,
 * and nothing else; one smaller than the grid fits anywhere it can be moved to, as vanilla's do (UNKNOWN
 * for Hypixel's: the wiki says only "the amounts and positions of ingredients must align"), not mirrored.
 * A shapeless one ("crafted with no fixed configurations", the wiki's Crafting) fits when each of its items
 * has a slot of its own. Items count by their SkyBlock id; an item with none (a vanilla one) is in the way of
 * every recipe. No server needed.
 */
public final class Crafting {
    private Crafting() {
    }

    /** A grid slot's items: a SkyBlock id (null for an item that has none) and how many; null for an empty slot. */
    public record Cell(String item, int amount) {
    }

    /**
     * A recipe the grid makes: the shape that fits, how many one craft takes from each of the grid's nine
     * slots, and how many times the grid's items make it.
     */
    public record Match(Recipe recipe, Shape shape, int[] taken, int times) {
        public Match {
            taken = taken.clone();
        }

        @Override
        public int[] taken() {
            return taken.clone();
        }

        /** How many items one craft makes. */
        public int count() {
            return shape.count();
        }
    }

    /** The first of the recipe's shapes that fits the grid; null if none does. */
    public static Match match(Recipe recipe, Cell[] grid) {
        for (Shape shape : recipe.shapes()) {
            Match match = shape.shapeless() ? shapeless(recipe, shape, grid) : shaped(recipe, shape, grid);
            if (match != null) return match;
        }
        return null;
    }

    /** Nothing there: an item with no SkyBlock id isn't nothing, it's something no recipe takes. */
    private static boolean empty(Cell cell) {
        return cell == null || cell.amount() <= 0;
    }

    private static Cell cell(Cell[] grid, int slot) {
        return slot < grid.length ? grid[slot] : null;
    }

    static Match shaped(Recipe recipe, Shape shape, Cell[] grid) {
        int top = 3, bottom = -1, left = 3, right = -1;
        for (int i = 0; i < 9; i++) {
            if (shape.cells().get(i) == null) continue;
            top = Math.min(top, i / 3);
            bottom = Math.max(bottom, i / 3);
            left = Math.min(left, i % 3);
            right = Math.max(right, i % 3);
        }
        if (bottom < 0) return null;
        for (int rows = -top; rows + bottom < 3; rows++) {
            for (int columns = -left; columns + right < 3; columns++) {
                Match match = shapedAt(recipe, shape, grid, rows, columns);
                if (match != null) return match;
            }
        }
        return null;
    }

    /** The shape moved {@code rows} down and {@code columns} right, if the grid makes it there. */
    private static Match shapedAt(Recipe recipe, Shape shape, Cell[] grid, int rows, int columns) {
        Ingredient[] wanted = new Ingredient[9];
        for (int i = 0; i < 9; i++) {
            Ingredient ingredient = shape.cells().get(i);
            if (ingredient != null) wanted[(i / 3 + rows) * 3 + i % 3 + columns] = ingredient;
        }
        int[] taken = new int[9];
        int times = Integer.MAX_VALUE;
        for (int slot = 0; slot < 9; slot++) {
            Cell cell = cell(grid, slot);
            Ingredient ingredient = wanted[slot];
            if (ingredient == null) {
                if (!empty(cell)) return null;
                continue;
            }
            if (empty(cell) || !ingredient.item().equals(cell.item()) || cell.amount() < ingredient.amount()) return null;
            taken[slot] = ingredient.amount();
            times = Math.min(times, cell.amount() / ingredient.amount());
        }
        return times == Integer.MAX_VALUE ? null : new Match(recipe, shape, taken, times);
    }

    /**
     * Each ingredient paired with a slot of the same item: of each item, the slots with the most with the
     * ingredients that take the most, so it fits whenever any pairing would.
     */
    static Match shapeless(Recipe recipe, Shape shape, Cell[] grid) {
        Map<String, List<Integer>> slots = new HashMap<>();
        Map<String, List<Ingredient>> wanted = new HashMap<>();
        for (int slot = 0; slot < 9; slot++) {
            Cell cell = cell(grid, slot);
            if (!empty(cell)) slots.computeIfAbsent(cell.item(), k -> new ArrayList<>()).add(slot);
        }
        for (Ingredient ingredient : shape.cells()) {
            if (ingredient != null) wanted.computeIfAbsent(ingredient.item(), k -> new ArrayList<>()).add(ingredient);
        }
        if (!slots.keySet().equals(wanted.keySet())) return null;
        int[] taken = new int[9];
        int times = Integer.MAX_VALUE;
        for (Map.Entry<String, List<Ingredient>> e : wanted.entrySet()) {
            List<Integer> have = slots.get(e.getKey());
            List<Ingredient> need = new ArrayList<>(e.getValue());
            if (have.size() != need.size()) return null;
            have.sort(Comparator.comparingInt((Integer s) -> grid[s].amount()).reversed());
            need.sort(Comparator.comparingInt(Ingredient::amount).reversed());
            for (int i = 0; i < have.size(); i++) {
                int slot = have.get(i);
                int amount = need.get(i).amount();
                if (grid[slot].amount() < amount) return null;
                taken[slot] = amount;
                times = Math.min(times, grid[slot].amount() / amount);
            }
        }
        return times == Integer.MAX_VALUE ? null : new Match(recipe, shape, taken, times);
    }

    /** The grid after {@code times} crafts of the match: each slot less what they take (null once empty). */
    public static Cell[] consume(Cell[] grid, Match match, int times) {
        int[] taken = match.taken();
        Cell[] left = new Cell[9];
        for (int slot = 0; slot < 9; slot++) {
            Cell cell = cell(grid, slot);
            if (cell == null) continue;
            int amount = cell.amount() - taken[slot] * times;
            left[slot] = amount > 0 ? new Cell(cell.item(), amount) : null;
        }
        return left;
    }

    /**
     * The recipes, looked up by the kinds of items in the grid: a recipe can only fit a grid with exactly
     * the items it takes.
     */
    public static final class Index {
        private final Map<String, List<Recipe>> byItems = new HashMap<>();

        public Index(Collection<Recipe> recipes) {
            for (Recipe recipe : recipes) {
                for (Shape shape : recipe.shapes()) {
                    String key = key(shape.cells().stream().map(i -> i == null ? null : i.item()).toList());
                    List<Recipe> list = byItems.computeIfAbsent(key, k -> new ArrayList<>());
                    if (!list.contains(recipe)) list.add(recipe);
                }
            }
        }

        /** The first recipe, of those {@code allowed}, that the grid makes; null if none. */
        public Match find(Cell[] grid, Predicate<Recipe> allowed) {
            List<String> items = new ArrayList<>();
            for (Cell cell : grid) items.add(empty(cell) ? null : cell.item());
            for (Recipe recipe : byItems.getOrDefault(key(items), List.of())) {
                if (!allowed.test(recipe)) continue;
                Match match = match(recipe, grid);
                if (match != null) return match;
            }
            return null;
        }

        private static String key(List<String> items) {
            TreeSet<String> kinds = new TreeSet<>();
            for (String item : items) if (item != null) kinds.add(item);
            return String.join(",", kinds);
        }
    }
}
