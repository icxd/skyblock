package net.icxd.dungeons.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.Collections;
import net.icxd.dungeons.collection.MenuSlot;
import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.recipe.RecipeData.BookEntry;
import net.icxd.dungeons.recipe.RecipeData.Kind;

/**
 * The Recipe Book against the recorded one (Recipe Book 01:08.0, the Combat pages 01:11.7-01:13.3), on the
 * private data: what's unlocked by collection tiers, the order of a page, the locked entries' text.
 */
class RecipeMenusTest {
    @BeforeAll
    static void load() {
        PrivateData.load();
    }

    @AfterAll
    static void unload() {
        PrivateData.unload();
    }

    /** Banana's Combat collections (the recording's Combat Collections, 00:42.9). */
    private static Document banana() {
        Document counts = new Document("BLAZE_ROD", 35L).append("BONE", 17_153L).append("ENDER_PEARL", 7_316L).append("SULPHUR", 18L)
                .append("MAGMA_CREAM", 106L).append("ROTTEN_FLESH", 12_831L).append("SLIME_BALL", 823_679L).append("SPIDER_EYE", 662L)
                .append("STRING", 1_075L);
        return new Document("mode", "NORMAL").append(Collections.COUNTS, counts);
    }

    private static List<String> names(List<BookEntry> entries) {
        List<String> names = new ArrayList<>();
        for (BookEntry entry : entries) names.add(entry.name());
        return names;
    }

    @Test
    void theBook() {
        Map<Integer, MenuSlot> slots = RecipeMenus.book(new Document("mode", "NORMAL"));
        assertEquals(Material.BOOK, slots.get(4).icon().material());
        assertEquals(List.of("&7Through your adventure, you will", "&7unlock recipes for all kinds of", "&7special items! You can view how to",
                "&7craft these items here."), slots.get(4).icon().lore().subList(0, 4));
        assertEquals("&8Also accessible via /recipes.", slots.get(4).icon().lore().getLast());
        String[] names = {"Farming", "Mining", "Combat", "Fishing", "Foraging", "Enchanting", "Alchemy", "Carpentry", "Hunting", "Slayer", "Special"};
        int[] at = {13, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
        for (int i = 0; i < at.length; i++) assertEquals("&a" + names[i] + " Recipes", slots.get(at[i]).icon().name());
        // "View all of the &aFarming Recipes &7that" / "&7you've unlocked!", the long ones before "that" (as recorded).
        assertEquals(List.of("&7View all of the &aFarming Recipes &7that", "&7you've unlocked!"), slots.get(13).icon().lore().subList(0, 2));
        assertEquals(List.of("&7View all of the &aEnchanting Recipes", "&7that you've unlocked!"), slots.get(24).icon().lore().subList(0, 2));
        assertEquals(List.of("&7View all of the &aCarpentry Recipes", "&7that you've unlocked!"), slots.get(30).icon().lore().subList(0, 2));
        assertEquals(new Icon(Material.OAK_SIGN, "&aSearch Recipes", "&8/recipe <query>", "", "&7Search all recipes in SkyBlock. May",
                "&7include recipes which aren't in the", "&7recipe book.", "", "&eClick to search!"), slots.get(51).icon());
        assertEquals(Material.EMERALD, slots.get(50).icon().material());
        assertEquals("&7To SkyBlock Menu", slots.get(48).icon().lore().getFirst());
    }

    /** What Banana's collections unlock of the recorded page 1 (minions' and pets' stay locked here: LATER). */
    @Test
    void unlockedByCollections() {
        Document banana = banana();
        List<BookEntry> combat = RecipeMenus.ordered(banana, "COMBAT");
        List<String> unlocked = new ArrayList<>();
        for (BookEntry entry : combat) if (Recipes.unlocked(banana, entry)) unlocked.add(entry.name());
        assertEquals(List.of("Beginner Combat Sack", "Enchanted Bone", "Enchanted Ender Pearl", "Enchanted Rotten Flesh", "Enchanted Slime Block",
                "Enchanted Slimeball", "Enchanted String", "Ender Bow", "Grappling Hook", "Hurricane Bow", "Launch Pad", "Medium Combat Sack",
                "Silent Pearl", "Skeleton Hat", "Slime Bow", "Slime Hat", "Small Combat Sack", "Spider Hat", "Spider Sword", "Web", "Zombie Hat",
                "Zombie Pickaxe", "Zombie Sword", "Zombie's Heart"), unlocked);
        // The unlocked come first, then the locked, each by name.
        assertEquals(unlocked, names(combat.subList(0, unlocked.size())));
        BookEntry firstLocked = combat.get(unlocked.size());
        assertEquals("Absolute Ender Pearl", firstLocked.name());
        assertFalse(Recipes.unlocked(banana, firstLocked));
    }

    @Test
    void aPage() {
        Document banana = banana();
        RecipeMenus.Category combat = RecipeMenus.category("COMBAT");
        int pages = RecipeMenus.pages(RecipeMenus.ordered(banana, "COMBAT").size());
        assertEquals("(1/" + pages + ") Combat Recipes", RecipeMenus.pageTitle(combat, 1, pages));
        Map<Integer, MenuSlot> first = RecipeMenus.page(banana, combat, 1);
        assertEquals(MenuSlot.item("BEGINNER_COMBAT_SACK", 1, List.of("", "&eClick to view recipe!")), first.get(10));
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&ePage 2"), first.get(53).icon());
        assertTrue(first.get(45).filler());
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Recipe Book"), first.get(48).icon());
        Map<Integer, MenuSlot> second = RecipeMenus.page(banana, combat, 2);
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&ePage 1"), second.get(45).icon());
        // Locked: "???" and what unlocks it (recorded: "Requires Ender Pearl Collection VII"). The first locked
        // entry (Absolute Ender Pearl) follows the 24 unlocked ones on page 1.
        MenuSlot locked = first.get(RecipeMenus.INSIDE[24]);
        assertEquals(new Icon(Material.GRAY_DYE, "&c???", "&7Requires &aEnder Pearl Collection VII&7!"), locked.icon());
        Map<Integer, MenuSlot> last = RecipeMenus.page(banana, combat, pages);
        assertTrue(last.get(53).filler());
        // Every entry is on a page, 28 to a page.
        int shown = 0;
        for (int page = 1; page <= pages; page++) {
            Map<Integer, MenuSlot> slots = RecipeMenus.page(banana, combat, page);
            for (int slot : RecipeMenus.INSIDE) if (slots.get(slot) != null && !slots.get(slot).filler()) shown++;
        }
        assertEquals(RecipeMenus.ordered(banana, "COMBAT").size(), shown);
    }

    /** Minions and pets wait for their systems: always locked, whatever the collection. */
    @Test
    void minionsAndPetsStayLocked() {
        Document banana = banana();
        int minions = 0, pets = 0;
        for (BookEntry entry : Recipes.category("COMBAT")) {
            if (entry.kind() == Kind.MINION) minions++;
            if (entry.kind() == Kind.PET) pets++;
            if (entry.kind() != Kind.RECIPE) assertFalse(Recipes.unlocked(banana, entry), entry.name());
        }
        assertTrue(minions > 0);
        assertTrue(pets > 0);
        assertFalse(Recipes.unlocked(banana, Recipes.data().recipe("BLAZE_GENERATOR_1")));
    }

    /** Everyone has the recipes nothing unlocks (vanilla ones like a Block of Iron); a collection's need its tier. */
    @Test
    void unlocking() {
        Document fresh = new Document("mode", "NORMAL");
        assertTrue(Recipes.unlocked(fresh, Recipes.data().recipe("IRON_BLOCK")));
        assertFalse(Recipes.unlocked(fresh, Recipes.data().recipe("ENCHANTED_ROTTEN_FLESH")));
        Document four = new Document("mode", "NORMAL").append(Collections.COUNTS, new Document("ROTTEN_FLESH", 1_000L));
        assertTrue(Recipes.unlocked(four, Recipes.data().recipe("ENCHANTED_ROTTEN_FLESH")));
        // Slayers aren't here: their recipes stay locked.
        boolean slayer = false;
        for (RecipeData.Recipe recipe : Recipes.data().recipes().values()) {
            if (recipe.requires().slayer() == null) continue;
            slayer = true;
            assertFalse(Recipes.unlocked(four, recipe), recipe.result());
        }
        assertTrue(slayer);
        // A Sandbox profile collects nothing, so it has every recipe but those waiting for minions and pets.
        Document sandbox = new Document("mode", "SANDBOX");
        assertTrue(Recipes.unlocked(sandbox, Recipes.data().recipe("ENCHANTED_ROTTEN_FLESH")));
        assertTrue(Recipes.unlocked(sandbox, Recipes.data().recipe("REVENANT_SWORD")));
        assertFalse(Recipes.unlocked(sandbox, Recipes.data().recipe("BLAZE_GENERATOR_1")));
        for (BookEntry entry : Recipes.category("COMBAT")) assertEquals(entry.kind() == Kind.RECIPE, Recipes.unlocked(sandbox, entry), entry.name());
    }

    /** A recipe's view: its first shape on the grid, the crafting table, the result (the wiki's Crafting UI). */
    @Test
    void aRecipe() {
        Map<Integer, MenuSlot> view = RecipeMenus.view("ENCHANTED_ROTTEN_FLESH", "Recipe Book");
        assertNotNull(view);
        assertEquals("Enchanted Rotten Flesh Recipe", RecipeMenus.viewTitle("ENCHANTED_ROTTEN_FLESH"));
        assertEquals(MenuSlot.item("ROTTEN_FLESH", 32, List.of()), view.get(11));
        assertEquals(MenuSlot.item("ROTTEN_FLESH", 32, List.of()), view.get(20));
        assertEquals(null, view.get(10));
        assertEquals(new Icon(Material.CRAFTING_TABLE, "&aCrafting Table", "&7Craft this recipe by using a", "&7crafting table."),
                view.get(23).icon());
        assertEquals(MenuSlot.item("ENCHANTED_ROTTEN_FLESH", 1, List.of()), view.get(25));
        assertEquals("&7To Recipe Book", view.get(48).icon().lore().getFirst());
        // Enchanted Rotten Flesh has the plus and the first five slots (the wiki's two frames).
        assertEquals(2, Recipes.data().recipe("ENCHANTED_ROTTEN_FLESH").shapes().size());
    }

    /** What the grid makes, through the data's own recipes and unlocks. */
    @Test
    void craftingWithTheData() {
        Crafting.Cell f = new Crafting.Cell("ROTTEN_FLESH", 64);
        Crafting.Cell[] grid = {null, f, null, f, f, f, null, f, null};
        Document fresh = new Document("mode", "NORMAL");
        assertEquals(null, Recipes.index().find(grid, r -> Recipes.unlocked(fresh, r)));
        Document four = new Document("mode", "NORMAL").append(Collections.COUNTS, new Document("ROTTEN_FLESH", 1_000L));
        Crafting.Match match = Recipes.index().find(grid, r -> Recipes.unlocked(four, r));
        assertEquals("ENCHANTED_ROTTEN_FLESH", match.recipe().result());
        assertEquals(2, match.times());
        // 3% of 160 Rotten Flesh's sell price (2 coins each): 9.6 Carpentry XP.
        assertEquals(9.6, CraftingTable.xp(match, 1), 1e-9);
        Crafting.Cell i = new Crafting.Cell("IRON_INGOT", 1);
        assertEquals("IRON_BLOCK", Recipes.index().find(new Crafting.Cell[] {i, i, i, i, i, i, i, i, i}, r -> Recipes.unlocked(fresh, r))
                .recipe().result());
    }
}
