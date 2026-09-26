package net.icxd.dungeons.gui.guis;

import net.icxd.dungeons.item.enums.Rarity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The item browser's search, sorts, filters and pages, on made-up items. */
class ItemBrowserTest {
    private static final List<String> TYPES = List.of("SWORD", "BOW", "ACCESSORY", "OTHER", "REFORGE_STONE");

    /** {@code count} items spread over every rarity and five types; some names have colour codes. */
    private static List<ItemBrowser.Entry> many(int count) {
        Rarity[] rarities = Rarity.values();
        return IntStream.range(0, count).mapToObj(i -> new ItemBrowser.Entry(String.format("ITEM_%04d", i),
                (i % 3 == 0 ? "&6" : "") + "Thing " + (count - i), rarities[i % rarities.length], TYPES.get(i % TYPES.size()))).toList();
    }

    private static ItemBrowser.Entry entry(String id, String name, Rarity rarity, String type) {
        return new ItemBrowser.Entry(id, name, rarity, type);
    }

    private static List<String> ids(List<ItemBrowser.Entry> entries) {
        return entries.stream().map(ItemBrowser.Entry::id).toList();
    }

    /** A few with ties: same rarity, same name, same type. */
    private static final List<ItemBrowser.Entry> SMALL = List.of(
            entry("B_SWORD", "Blade", Rarity.EPIC, "SWORD"),
            entry("A_SWORD", "Blade", Rarity.EPIC, "SWORD"),
            entry("CHARM", "&9Charm", Rarity.RARE, "ACCESSORY"),
            entry("RUBY", "❤ Fine Ruby Gemstone", Rarity.RARE, "GEMSTONE"),
            entry("ZAP", "Zapper", Rarity.LEGENDARY, "BOW"),
            entry("APPLE", "apple", Rarity.COMMON, "OTHER"));

    @Test
    void sortsByRarityHighToLowFirst() {
        ItemBrowser browser = new ItemBrowser();
        assertEquals(ItemBrowser.Sort.RARITY_DESCENDING, browser.getSort());
        // Ties by name (without its colours or the gemstone's symbol), then by id.
        assertEquals(List.of("ZAP", "A_SWORD", "B_SWORD", "CHARM", "RUBY", "APPLE"), ids(browser.matches(SMALL)));
    }

    @Test
    void everySort() {
        ItemBrowser browser = new ItemBrowser();
        browser.cycleSort(true);
        assertEquals(ItemBrowser.Sort.RARITY_ASCENDING, browser.getSort());
        assertEquals(List.of("APPLE", "CHARM", "RUBY", "A_SWORD", "B_SWORD", "ZAP"), ids(browser.matches(SMALL)));
        browser.cycleSort(true);
        assertEquals(ItemBrowser.Sort.NAME_ASCENDING, browser.getSort());
        // Any case; the gemstone goes under F, not after Z.
        assertEquals(List.of("APPLE", "A_SWORD", "B_SWORD", "CHARM", "RUBY", "ZAP"), ids(browser.matches(SMALL)));
        browser.cycleSort(true);
        assertEquals(ItemBrowser.Sort.NAME_DESCENDING, browser.getSort());
        assertEquals(List.of("ZAP", "RUBY", "CHARM", "A_SWORD", "B_SWORD", "APPLE"), ids(browser.matches(SMALL)));
        browser.cycleSort(true);
        assertEquals(ItemBrowser.Sort.TYPE, browser.getSort());
        assertEquals(List.of("CHARM", "ZAP", "RUBY", "APPLE", "A_SWORD", "B_SWORD"), ids(browser.matches(SMALL)));
        browser.cycleSort(true);
        assertEquals(ItemBrowser.Sort.RARITY_DESCENDING, browser.getSort());
        browser.cycleSort(false);
        assertEquals(ItemBrowser.Sort.TYPE, browser.getSort());
    }

    @Test
    void rarityFilterGoesThroughEveryRarity() {
        ItemBrowser browser = new ItemBrowser();
        List<ItemBrowser.Entry> items = many(500);
        assertNull(browser.getRarity());
        for (Rarity rarity : Rarity.values()) {
            browser.cycleRarity(true);
            assertEquals(rarity, browser.getRarity());
            List<ItemBrowser.Entry> matches = browser.matches(items);
            assertEquals(50, matches.size());
            assertTrue(matches.stream().allMatch(e -> e.rarity() == rarity));
        }
        browser.cycleRarity(true);
        assertNull(browser.getRarity());
        assertEquals(500, browser.matches(items).size());
        browser.cycleRarity(false);
        assertEquals(Rarity.UNOBTAINABLE, browser.getRarity());
        browser.cycleRarity(false);
        assertEquals(Rarity.VERY_SPECIAL, browser.getRarity());
    }

    @Test
    void typeFilterGoesThroughTheTypesThereAre() {
        List<ItemBrowser.Entry> items = many(500);
        List<String> types = ItemBrowser.types(items);
        assertEquals(List.of("ACCESSORY", "BOW", "OTHER", "REFORGE_STONE", "SWORD"), types);

        ItemBrowser browser = new ItemBrowser();
        for (String type : types) {
            browser.cycleType(types, true);
            assertEquals(type, browser.getType());
            List<ItemBrowser.Entry> matches = browser.matches(items);
            assertEquals(100, matches.size());
            assertTrue(matches.stream().allMatch(e -> e.type().equals(type)));
        }
        browser.cycleType(types, true);
        assertNull(browser.getType());
        browser.cycleType(types, false);
        assertEquals("SWORD", browser.getType());

        // With a rarity too: both have to match.
        browser.cycleRarity(true);
        List<ItemBrowser.Entry> both = browser.matches(items);
        assertEquals(50, both.size());
        assertTrue(both.stream().allMatch(e -> e.type().equals("SWORD") && e.rarity() == Rarity.COMMON));
    }

    @Test
    void searchesNamesWithoutColoursAndIds() {
        ItemBrowser browser = new ItemBrowser();
        browser.search("  cHaRm ");
        assertEquals("cHaRm", browser.getQuery());
        assertEquals(List.of("CHARM"), ids(browser.matches(SMALL)));
        // "&9" is the colour, not the name.
        browser.search("&9");
        assertEquals(List.of(), ids(browser.matches(SMALL)));
        browser.search("ruby gem");
        assertEquals(List.of("RUBY"), ids(browser.matches(SMALL)));
        browser.search("_sword");
        assertEquals(List.of("A_SWORD", "B_SWORD"), ids(browser.matches(SMALL)));
        browser.search("blade");
        browser.cycleRarity(true);
        assertEquals(List.of(), ids(browser.matches(SMALL)));
        browser.search("");
        assertEquals(List.of("APPLE"), ids(browser.matches(SMALL)));
    }

    @Test
    void nothingMatchingIsOneEmptyPage() {
        ItemBrowser browser = new ItemBrowser();
        browser.search("nothing is called this");
        ItemBrowser.Page page = browser.view(many(300));
        assertEquals(List.of(), page.entries());
        assertEquals(0, page.page());
        assertEquals(1, page.pages());
        assertEquals(0, page.total());
        browser.nextPage();
        assertEquals(0, browser.view(many(300)).page());
    }

    @Test
    void pages() {
        List<ItemBrowser.Entry> items = many(1000);
        ItemBrowser browser = new ItemBrowser();
        ItemBrowser.Page first = browser.view(items);
        assertEquals(23, first.pages());
        assertEquals(1000, first.total());
        assertEquals(45, first.entries().size());

        // Every item on exactly one page, in the sorted order.
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < first.pages(); i++) {
            ItemBrowser.Page page = browser.view(items);
            assertEquals(i, page.page());
            assertEquals(i == 22 ? 10 : 45, page.entries().size());
            seen.addAll(ids(page.entries()));
            browser.nextPage();
        }
        assertEquals(ids(browser.matches(items)), seen);
        assertEquals(1000, new HashSet<>(seen).size());

        // Past the last page stays on it; before the first stays on it.
        assertEquals(22, browser.view(items).page());
        for (int i = 0; i < 30; i++) browser.previousPage();
        assertEquals(0, browser.getPage());
        browser.previousPage();
        assertEquals(0, browser.view(items).page());
    }

    @Test
    void changesGoBackToTheFirstPage() {
        List<ItemBrowser.Entry> items = many(1000);
        List<Consumer<ItemBrowser>> changes = List.of(b -> b.cycleSort(true), b -> b.cycleRarity(false),
                b -> b.cycleType(ItemBrowser.types(items), true), b -> b.search("thing"));
        for (Consumer<ItemBrowser> change : changes) {
            ItemBrowser browser = new ItemBrowser();
            browser.nextPage();
            browser.nextPage();
            assertEquals(2, browser.view(items).page());
            change.accept(browser);
            assertEquals(0, browser.getPage());
            assertEquals(0, browser.view(items).page());
        }
    }

    @Test
    void aPageThatNoLongerExistsBecomesTheLast() {
        List<ItemBrowser.Entry> items = new ArrayList<>(many(1000));
        ItemBrowser browser = new ItemBrowser();
        for (int i = 0; i < 20; i++) browser.nextPage();
        assertEquals(20, browser.view(items).page());
        // Fewer items (another list of them): 100 is three pages.
        ItemBrowser.Page page = browser.view(items.subList(0, 100));
        assertEquals(2, page.page());
        assertEquals(3, page.pages());
        assertEquals(10, page.entries().size());
        assertEquals(2, browser.getPage());
    }

    @Test
    void typeOptionsAroundTheSelectedOne() {
        assertEquals(0, ItemBrowser.window(5, 3, 12));
        assertEquals(0, ItemBrowser.window(40, 0, 12));
        assertEquals(0, ItemBrowser.window(40, 6, 12));
        assertEquals(1, ItemBrowser.window(40, 7, 12));
        assertEquals(14, ItemBrowser.window(40, 20, 12));
        assertEquals(28, ItemBrowser.window(40, 39, 12));
    }

    @Test
    void completesIds() {
        List<String> ids = IntStream.range(0, 500).mapToObj(i -> String.format("ITEM_%03d", i)).toList();
        List<String> all = ItemBrowser.complete(Stream.concat(Stream.of("list"), ids.stream()), "", 200);
        assertEquals(200, all.size());
        assertEquals("ITEM_000", all.getFirst());
        assertEquals(List.of("ITEM_120", "ITEM_121", "ITEM_122", "ITEM_123", "ITEM_124", "ITEM_125", "ITEM_126", "ITEM_127",
                "ITEM_128", "ITEM_129"), ItemBrowser.complete(ids.stream(), "item_12", 200));
        assertEquals(List.of("list"), ItemBrowser.complete(Stream.concat(Stream.of("list"), ids.stream()), "LI", 200));
        assertEquals(List.of(), ItemBrowser.complete(ids.stream(), "HYPERION", 200));
        Set<String> limited = new HashSet<>(ItemBrowser.complete(ids.stream(), "ITEM_", 3));
        assertEquals(Set.of("ITEM_000", "ITEM_001", "ITEM_002"), limited);
    }

    @Test
    void labels() {
        assertEquals("Reforge Stone", ItemBrowser.label("REFORGE_STONE"));
        assertEquals("Very Special", ItemBrowser.label("VERY_SPECIAL"));
        assertEquals("Sword", ItemBrowser.label("SWORD"));
        assertEquals("Fine Ruby", ItemBrowser.plain("&9Fine §lRuby"));
    }
}
