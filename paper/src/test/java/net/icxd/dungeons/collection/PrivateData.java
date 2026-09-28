package net.icxd.dungeons.collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.recipe.RecipeData;
import net.icxd.dungeons.recipe.Recipes;

/**
 * The private data the recorded menus are checked with, where it's there: the collections
 * ({@code -Dcollections.dir}, else the data checkout's collections/ next to this repository) and the items
 * ({@code -Ditems.file}, else the data checkout's items/items.json). Tests using it are skipped without it.
 */
public final class PrivateData {
    private PrivateData() {
    }

    static Path repository() {
        return Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
    }

    public static Path collectionsFolder() {
        String property = System.getProperty("collections.dir");
        if (property != null) return Path.of(property);
        return repository().resolveSibling("skyblock-dungeon-data").resolve(CollectionData.FOLDER);
    }

    public static Path itemsFile() {
        String property = System.getProperty("items.file");
        if (property != null) return Path.of(property);
        return repository().resolveSibling("skyblock-dungeon-data/items/items.json");
    }

    /** Loads the items, the collections and the recipes, or skips the test if they aren't here. */
    public static void load() {
        Path folder = collectionsFolder();
        Path items = itemsFile();
        assumeTrue(Files.isRegularFile(folder.resolve(CollectionData.FILE)), "no " + folder.resolve(CollectionData.FILE));
        assumeTrue(Files.isRegularFile(items), "no " + items);
        assertNull(ItemRegistry.loadData(items).failure(), "the items didn't load");
        CollectionData collections = CollectionData.load(folder);
        assertEquals(List.of(), collections.problems());
        Collections.setData(collections);
        RecipeData recipes = RecipeData.load(folder);
        assertEquals(List.of(), recipes.problems());
        Recipes.use(recipes);
    }

    /** No collections or recipes again (the items stay: other tests load their own). */
    public static void unload() {
        Collections.setData(CollectionData.empty());
        Recipes.use(RecipeData.empty());
    }
}
