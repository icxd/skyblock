package net.icxd.dungeons.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.collection.CollectionData.Collection;
import net.icxd.dungeons.collection.CollectionData.Reward;
import net.icxd.dungeons.collection.CollectionText;
import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.collection.MenuSlot;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.recipe.RecipeData.BookEntry;
import net.icxd.dungeons.recipe.RecipeData.Ingredient;
import net.icxd.dungeons.recipe.RecipeData.Kind;
import net.icxd.dungeons.recipe.RecipeData.Recipe;
import net.icxd.dungeons.recipe.RecipeData.Requirement;
import net.icxd.dungeons.recipe.RecipeData.Shape;
import net.icxd.dungeons.utils.Utils;

/**
 * What the Recipe Book's menus show, slot by slot, for a profile: the book ({@code /recipes}, recorded
 * 01:08.0), a category's pages ("(1/4) Combat Recipes", 01:11.7-01:13.3) and a recipe (the wiki's Crafting
 * UI: none was opened in the recording). A slot that isn't in a map is empty. No server needed.
 */
public final class RecipeMenus {
    public static final int TOP = 4;
    public static final int BACK = 48;
    public static final int CLOSE = 49;
    public static final int TRADES = 50;
    public static final int SEARCH = 51;
    public static final int PREVIOUS = 45;
    public static final int NEXT = 53;
    /** A page's recipes: the seven-wide inside of its border, row by row (28 a page, as recorded). */
    public static final int[] INSIDE = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39,
            40, 41, 42, 43};

    /** A Recipe Book category: its id in the data, name, icon and slot in the book (all as recorded). */
    public record Category(String id, String name, Material icon, int slot) {
    }

    public static final List<Category> CATEGORIES = List.of(new Category("FARMING", "Farming", Material.GOLDEN_HOE, 13),
            new Category("MINING", "Mining", Material.STONE_PICKAXE, 20), new Category("COMBAT", "Combat", Material.STONE_SWORD, 21),
            new Category("FISHING", "Fishing", Material.FISHING_ROD, 22), new Category("FORAGING", "Foraging", Material.JUNGLE_SAPLING, 23),
            new Category("ENCHANTING", "Enchanting", Material.ENCHANTING_TABLE, 24), new Category("ALCHEMY", "Alchemy", Material.BREWING_STAND, 29),
            new Category("CARPENTRY", "Carpentry", Material.CRAFTING_TABLE, 30), new Category("HUNTING", "Hunting", Material.LEAD, 31),
            new Category("SLAYER", "Slayer", Material.BOW, 32), new Category("SPECIAL", "Special", Material.NETHER_STAR, 33));

    /** The recipe view's grid, row by row (the wiki's Crafting UI: rows 2-4, columns 2-4), its crafting table and its result. */
    public static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int VIEW_TABLE = 23;
    public static final int VIEW_RESULT = 25;

    private RecipeMenus() {
    }

    public static Category category(String id) {
        for (Category category : CATEGORIES) if (category.id().equals(id)) return category;
        return null;
    }

    static MenuSlot back(String to) {
        return MenuSlot.of(new Icon(Material.ARROW, "&aGo Back", "&7To " + to));
    }

    static MenuSlot close() {
        return MenuSlot.of(new Icon(Material.BARRIER, "&cClose"));
    }

    private static Map<Integer, MenuSlot> glass(int... except) {
        Map<Integer, MenuSlot> slots = new LinkedHashMap<>();
        outer:
        for (int slot = 0; slot < 54; slot++) {
            for (int e : except) if (e == slot) continue outer;
            slots.put(slot, MenuSlot.FILLER);
        }
        return slots;
    }

    // The book

    /** The Recipe Book item (the SkyBlock Menu's has "Click to view!" too): how many of the book's recipes are unlocked. */
    public static Icon summary(Document profile, boolean click) {
        List<BookEntry> all = Recipes.data().book();
        return summary(Recipes.unlocked(profile, all), all.size(), click);
    }

    public static Icon summary(int unlocked, int all, boolean click) {
        List<String> lore = new ArrayList<>(List.of("&7Through your adventure, you will", "&7unlock recipes for all kinds of",
                "&7special items! You can view how to", "&7craft these items here.", "",
                "&7Recipes Unlocked: " + CollectionText.unlocked(fraction(unlocked, all)), CollectionText.bar(unlocked, all), "",
                "&8Also accessible via /recipes."));
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return new Icon(Material.BOOK, "&aRecipe Book", lore);
    }

    private static double fraction(long of, long all) {
        return all <= 0 ? 0 : (double) of / all;
    }

    /** A category's item: "View all of the Combat Recipes that you've unlocked!" and how many are. */
    static Icon categoryIcon(Document profile, Category category, boolean click) {
        List<BookEntry> entries = Recipes.category(category.id());
        int unlocked = Recipes.unlocked(profile, entries);
        List<String> lore = new ArrayList<>(CollectionText.wrap("&7View all of the &a" + category.name() + " Recipes &7that you've unlocked!"));
        lore.add("");
        lore.add("&7Recipes Unlocked: " + CollectionText.unlocked(fraction(unlocked, entries.size())));
        lore.add(CollectionText.bar(unlocked, entries.size()));
        if (click) lore.addAll(List.of("", "&eClick to view!"));
        return new Icon(category.icon(), "&a" + category.name() + " Recipes", lore);
    }

    /** The book: its categories, Trades and the search sign (neither of which does anything yet: LATER). */
    public static Map<Integer, MenuSlot> book(Document profile) {
        int[] except = new int[CATEGORIES.size() + 5];
        for (int i = 0; i < CATEGORIES.size(); i++) except[i] = CATEGORIES.get(i).slot();
        except[CATEGORIES.size()] = TOP;
        except[CATEGORIES.size() + 1] = BACK;
        except[CATEGORIES.size() + 2] = CLOSE;
        except[CATEGORIES.size() + 3] = TRADES;
        except[CATEGORIES.size() + 4] = SEARCH;
        Map<Integer, MenuSlot> slots = glass(except);
        slots.put(TOP, MenuSlot.of(summary(profile, false)));
        for (Category category : CATEGORIES) slots.put(category.slot(), MenuSlot.of(categoryIcon(profile, category, true)));
        slots.put(BACK, back("SkyBlock Menu"));
        slots.put(CLOSE, close());
        slots.put(TRADES, MenuSlot.of(trades(profile)));
        slots.put(SEARCH, MenuSlot.of(new Icon(Material.OAK_SIGN, "&aSearch Recipes", "&8/recipe <query>", "",
                "&7Search all recipes in SkyBlock. May", "&7include recipes which aren't in the", "&7recipe book.", "", "&eClick to search!")));
        return slots;
    }

    /**
     * Trades, as recorded ("&7Trades Unlocked: &e87%", no gold % sign), counting the trades collection
     * tiers unlock (the recorded 23 has 2 more, UNKNOWN which). No Trades menu yet (LATER).
     */
    static Icon trades(Document profile) {
        int all = 0, unlocked = 0;
        for (Collection collection : Collections.data().collections().values()) {
            int tier = Collections.tier(profile, collection.id());
            for (int n = 1; n <= collection.maxTier(); n++) {
                for (Reward reward : collection.tier(n).rewards()) {
                    if (reward.type() != Reward.Type.TRADE) continue;
                    all++;
                    if (n <= tier) unlocked++;
                }
            }
        }
        double fraction = fraction(unlocked, all);
        return new Icon(Material.EMERALD, "&aTrades", "&7View your available trades. These", "&7trades are always available and",
                "&7accessible through the SkyBlock", "&7Menu.", "", "&7Trades Unlocked: " + (fraction >= 1 ? "&a" : "&e")
                        + net.icxd.dungeons.skill.SkillText.percent(fraction) + "%", CollectionText.bar(unlocked, all), "", "&eClick to view!");
    }

    // A category's pages

    /**
     * A category's entries in the order its pages show them: the unlocked ones by name, then the locked ones
     * by name (as recorded). Pets sort as "Pet", among themselves Z to A (the recording's Zombie, Spider,
     * Skeleton; UNKNOWN why).
     */
    public static List<BookEntry> ordered(Document profile, String category) {
        List<BookEntry> entries = new ArrayList<>(Recipes.category(category));
        Comparator<BookEntry> byName = Comparator.comparing(RecipeMenus::sortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(e -> e.kind() == Kind.PET ? e.name() : "", Comparator.reverseOrder());
        entries.sort(Comparator.comparing((BookEntry e) -> !Recipes.unlocked(profile, e)).thenComparing(byName));
        return entries;
    }

    private static String sortKey(BookEntry entry) {
        return entry.kind() == Kind.PET ? "Pet" : entry.name();
    }

    public static int pages(int entries) {
        return Math.max(1, (entries + INSIDE.length - 1) / INSIDE.length);
    }

    /** "(1/4) Combat Recipes". */
    public static String pageTitle(Category category, int page, int pages) {
        return "(" + page + "/" + pages + ") " + category.name() + " Recipes";
    }

    /** Page {@code page} (from 1) of a category. */
    public static Map<Integer, MenuSlot> page(Document profile, Category category, int page) {
        List<BookEntry> entries = ordered(profile, category.id());
        int pages = pages(entries.size());
        Map<Integer, MenuSlot> slots = new LinkedHashMap<>();
        for (int slot = 0; slot < 54; slot++) {
            int row = slot / 9, column = slot % 9;
            if (row == 0 || row == 5 || column == 0 || column == 8) slots.put(slot, MenuSlot.FILLER);
        }
        slots.put(TOP, MenuSlot.of(categoryIcon(profile, category, false)));
        int from = (page - 1) * INSIDE.length;
        for (int i = 0; i < INSIDE.length && from + i < entries.size(); i++) slots.put(INSIDE[i], entry(profile, entries.get(from + i)));
        if (page > 1) slots.put(PREVIOUS, MenuSlot.of(new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + (page - 1))));
        if (page < pages) slots.put(NEXT, MenuSlot.of(new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 1))));
        slots.put(BACK, back("Recipe Book"));
        slots.put(CLOSE, close());
        return slots;
    }

    /** An entry on a page: its item and "Click to view recipe!" once unlocked; "???" and what unlocks it before. */
    static MenuSlot entry(Document profile, BookEntry entry) {
        if (Recipes.unlocked(profile, entry)) return MenuSlot.item(entry.item(), 1, List.of("", "&eClick to view recipe!"));
        return MenuSlot.of(new Icon(Material.GRAY_DYE, "&c???", "&7Requires " + requirement(entry.requires()) + "&7!"));
    }

    /**
     * "&aEnder Pearl Collection VII" (recorded); a boss's and the rest's are UNKNOWN: "&aBonzo Collection VI",
     * "&aZombie Slayer 7", and what the data says.
     */
    static String requirement(Requirement requires) {
        if (requires == null || requires.none()) return "&a???";
        if (requires.collection() != null) {
            Collection collection = Collections.data().collection(requires.collection());
            String name = collection == null ? requires.collection() : collection.name();
            return "&a" + name + " Collection " + Utils.getRomanNumeral(requires.tier());
        }
        if (requires.slayer() != null) {
            return "&a" + Character.toUpperCase(requires.slayer().charAt(0)) + requires.slayer().substring(1).toLowerCase() + " Slayer "
                    + requires.level();
        }
        return "&a" + requires.other().replaceFirst("^Requires:? ", "");
    }

    // A recipe

    /** "Enchanted Bone Recipe": the title the wiki's Crafting UI gives a recipe. */
    public static String viewTitle(String itemId) {
        SkyBlockItem item = ItemRegistry.get(itemId);
        return (item == null ? itemId : item.name()) + " Recipe";
    }

    /**
     * A recipe as the wiki's Crafting UI shows it: its first shape's ingredients on the grid, the crafting table
     * that makes it and what it makes; the rest glass. Null if there's no recipe for the item.
     */
    public static Map<Integer, MenuSlot> view(String itemId, String back) {
        Recipe recipe = Recipes.data().recipe(itemId);
        if (recipe == null || recipe.shapes().isEmpty()) return null;
        Shape shape = recipe.shapes().getFirst();
        int[] except = new int[GRID.length + 4];
        System.arraycopy(GRID, 0, except, 0, GRID.length);
        except[GRID.length] = VIEW_TABLE;
        except[GRID.length + 1] = VIEW_RESULT;
        except[GRID.length + 2] = BACK;
        except[GRID.length + 3] = CLOSE;
        Map<Integer, MenuSlot> slots = glass(except);
        for (int i = 0; i < 9; i++) {
            Ingredient ingredient = shape.cells().get(i);
            if (ingredient != null) slots.put(GRID[i], MenuSlot.item(ingredient.item(), ingredient.amount(), List.of()));
        }
        slots.put(VIEW_TABLE, MenuSlot.of(new Icon(Material.CRAFTING_TABLE, "&aCrafting Table", "&7Craft this recipe by using a",
                "&7crafting table.")));
        slots.put(VIEW_RESULT, MenuSlot.item(itemId, shape.count(), List.of()));
        slots.put(BACK, back(back));
        slots.put(CLOSE, close());
        return slots;
    }
}
