package net.icxd.dungeons.recipe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.recipe.Crafting.Cell;
import net.icxd.dungeons.recipe.Crafting.Match;
import net.icxd.dungeons.recipe.RecipeData.Ingredient;
import net.icxd.dungeons.recipe.RecipeData.Recipe;
import net.icxd.dungeons.recipe.RecipeData.Requirement;
import net.icxd.dungeons.recipe.RecipeData.Shape;

/** Which recipe a grid makes, how often, and what crafting it leaves: made-up recipes in NEU's shapes. */
class CraftingTest {
    /** "BLAZE_ROD:32" per cell, "" for none, row by row. */
    private static Shape shape(int count, String... cells) {
        List<Ingredient> list = new ArrayList<>();
        for (String cell : cells) list.add(RecipeData.ingredient(cell));
        return new Shape(list, count);
    }

    private static Recipe recipe(String result, Shape... shapes) {
        return new Recipe(result, List.of(shapes), Requirement.NONE, null);
    }

    /** "ROTTEN_FLESH:64" per slot, "" for empty, row by row. */
    private static Cell[] grid(String... slots) {
        Cell[] grid = new Cell[9];
        for (int i = 0; i < slots.length; i++) {
            if (slots[i].isEmpty()) continue;
            int colon = slots[i].lastIndexOf(':');
            grid[i] = new Cell(slots[i].substring(0, colon), Integer.parseInt(slots[i].substring(colon + 1)));
        }
        return grid;
    }

    private static final String F = "ROTTEN_FLESH:32";
    /** Enchanted Rotten Flesh as the data has it: NEU's plus, and the enchanted recipes' second way (the first five). */
    private static final Recipe ENCHANTED_FLESH = recipe("ENCHANTED_ROTTEN_FLESH",
            shape(1, "", F, "", F, F, F, "", F, ""), shape(1, F, F, F, F, F, "", "", "", ""));
    /** Spider's Boots: two columns of Enchanted String, the top two rows. */
    private static final Recipe BOOTS = recipe("SPIDER_BOOTS",
            shape(1, "ENCHANTED_STRING:64", "", "ENCHANTED_STRING:64", "ENCHANTED_STRING:64", "", "ENCHANTED_STRING:64", "", "", ""));

    @Test
    void plusOfThirtyTwo() {
        Match match = Crafting.match(ENCHANTED_FLESH, grid("", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32",
                "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", ""));
        assertNotNull(match);
        assertEquals(1, match.times());
        assertEquals(1, match.count());
        assertArrayEquals(new int[] {0, 32, 0, 32, 32, 32, 0, 32, 0}, match.taken());
    }

    /** More than a recipe takes: as many crafts as the fewest allows, and the rest stays. */
    @Test
    void timesAndWhatsLeft() {
        Cell[] grid = grid("", "ROTTEN_FLESH:64", "", "ROTTEN_FLESH:64", "ROTTEN_FLESH:40", "ROTTEN_FLESH:64", "", "ROTTEN_FLESH:64", "");
        Match match = Crafting.match(ENCHANTED_FLESH, grid);
        assertEquals(1, match.times());
        Cell[] left = Crafting.consume(grid, match, 1);
        assertEquals(new Cell("ROTTEN_FLESH", 32), left[1]);
        assertEquals(new Cell("ROTTEN_FLESH", 8), left[4]);
        assertNull(left[0]);
        Cell[] twice = grid("", "ROTTEN_FLESH:64", "", "ROTTEN_FLESH:64", "ROTTEN_FLESH:64", "ROTTEN_FLESH:64", "", "ROTTEN_FLESH:64", "");
        Match both = Crafting.match(ENCHANTED_FLESH, twice);
        assertEquals(2, both.times());
        Cell[] none = Crafting.consume(twice, both, 2);
        assertEquals(Arrays.asList(new Cell[9]), Arrays.asList(none));
    }

    @Test
    void theEnchantedRecipesSecondWay() {
        Match match = Crafting.match(ENCHANTED_FLESH, grid("ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32",
                "ROTTEN_FLESH:32", "", "", "", ""));
        assertNotNull(match);
        assertArrayEquals(new int[] {32, 32, 32, 32, 32, 0, 0, 0, 0}, match.taken());
    }

    @Test
    void tooFewOrAWrongItemOrSomethingElse() {
        // 31 in one slot.
        assertNull(Crafting.match(ENCHANTED_FLESH, grid("", "ROTTEN_FLESH:31", "", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32",
                "", "ROTTEN_FLESH:32", "")));
        // Bone in the middle.
        assertNull(Crafting.match(ENCHANTED_FLESH, grid("", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "BONE:32", "ROTTEN_FLESH:32", "",
                "ROTTEN_FLESH:32", "")));
        // A sixth stack in a corner.
        assertNull(Crafting.match(ENCHANTED_FLESH, grid("BONE:1", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32",
                "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "")));
        // 160 in the wrong places.
        assertNull(Crafting.match(ENCHANTED_FLESH, grid("ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "",
                "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32")));
        // An item with no SkyBlock id (a vanilla one) in a corner is in the way too, whatever looks up the recipe.
        Cell[] vanilla = grid("", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "");
        vanilla[0] = new Cell(null, 1);
        assertNull(Crafting.match(ENCHANTED_FLESH, vanilla));
        assertNull(new Crafting.Index(List.of(ENCHANTED_FLESH)).find(vanilla, r -> true));
    }

    /** A shape smaller than the grid fits anywhere it can be moved to: the boots in the bottom two rows too. */
    @Test
    void movedAlong() {
        Match top = Crafting.match(BOOTS, grid("ENCHANTED_STRING:64", "", "ENCHANTED_STRING:64", "ENCHANTED_STRING:64", "",
                "ENCHANTED_STRING:64", "", "", ""));
        assertNotNull(top);
        Match bottom = Crafting.match(BOOTS, grid("", "", "", "ENCHANTED_STRING:64", "", "ENCHANTED_STRING:64", "ENCHANTED_STRING:64", "",
                "ENCHANTED_STRING:64"));
        assertNotNull(bottom);
        assertArrayEquals(new int[] {0, 0, 0, 64, 0, 64, 64, 0, 64}, bottom.taken());
        // Not mirrored or squeezed: the columns next to each other aren't it.
        assertNull(Crafting.match(BOOTS, grid("ENCHANTED_STRING:64", "ENCHANTED_STRING:64", "", "ENCHANTED_STRING:64", "ENCHANTED_STRING:64",
                "", "", "", "")));
    }

    /** One shape makes more (Silent Pearl: 8), another of the same recipe makes nine (a block recipe). */
    @Test
    void howManyAShapeMakes() {
        Recipe diamond = recipe("ENCHANTED_DIAMOND", shape(1, "", "DIAMOND:32", "", "DIAMOND:32", "DIAMOND:32", "DIAMOND:32", "",
                "DIAMOND:32", ""), shape(9, "", "DIAMOND_BLOCK:32", "", "DIAMOND_BLOCK:32", "DIAMOND_BLOCK:32", "DIAMOND_BLOCK:32", "",
                "DIAMOND_BLOCK:32", ""));
        Match blocks = Crafting.match(diamond, grid("", "DIAMOND_BLOCK:32", "", "DIAMOND_BLOCK:32", "DIAMOND_BLOCK:32", "DIAMOND_BLOCK:32", "",
                "DIAMOND_BLOCK:32", ""));
        assertEquals(9, blocks.count());
        assertEquals(1, Crafting.match(diamond, grid("", "DIAMOND:32", "", "DIAMOND:32", "DIAMOND:32", "DIAMOND:32", "", "DIAMOND:32", ""))
                .count());
    }

    /** A shapeless recipe: its items in any slots, each in one of its own. */
    @Test
    void shapeless() {
        List<Ingredient> cells = new ArrayList<>(Arrays.asList(new Ingredient[9]));
        cells.set(0, new Ingredient("ENCHANTED_COCOA", 32));
        cells.set(1, new Ingredient("ENCHANTED_COCOA", 32));
        cells.set(2, new Ingredient("WHEAT", 1));
        Recipe cookie = new Recipe("ENCHANTED_COOKIE", List.of(new Shape(cells, 1, true)), Requirement.NONE, null);
        Match anywhere = Crafting.match(cookie, grid("", "", "", "", "WHEAT:3", "", "ENCHANTED_COCOA:64", "", "ENCHANTED_COCOA:40"));
        assertNotNull(anywhere);
        assertEquals(1, anywhere.times());
        assertArrayEquals(new int[] {0, 0, 0, 0, 1, 0, 32, 0, 32}, anywhere.taken());
        // One slot for two of them isn't enough.
        assertNull(Crafting.match(cookie, grid("ENCHANTED_COCOA:64", "WHEAT:1", "", "", "", "", "", "", "")));
        // Nor is something else too.
        assertNull(Crafting.match(cookie, grid("ENCHANTED_COCOA:32", "ENCHANTED_COCOA:32", "WHEAT:1", "BONE:1", "", "", "", "", "")));
    }

    /** A shift-click's results in stacks: full ones and the rest; one to a stack for an unstackable item (a sack, a talisman). */
    @Test
    void shiftCraftedStacks() {
        assertEquals(List.of(64, 64, 2), CraftingTable.stacks(130, 64));
        assertEquals(List.of(16, 4), CraftingTable.stacks(20, 16));
        assertEquals(List.of(1, 1, 1), CraftingTable.stacks(3, 1));
        assertEquals(List.of(), CraftingTable.stacks(0, 64));
    }

    /** The index finds a recipe by the grid's items, and skips the ones that aren't allowed (not unlocked). */
    @Test
    void index() {
        Crafting.Index index = new Crafting.Index(List.of(ENCHANTED_FLESH, BOOTS));
        Cell[] flesh = grid("", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "ROTTEN_FLESH:32", "", "ROTTEN_FLESH:32", "");
        assertEquals("ENCHANTED_ROTTEN_FLESH", index.find(flesh, r -> true).recipe().result());
        assertNull(index.find(flesh, r -> !r.result().equals("ENCHANTED_ROTTEN_FLESH")));
        assertNull(index.find(grid("BONE:64"), r -> true));
        assertNull(index.find(new Cell[9], r -> true));
    }
}
