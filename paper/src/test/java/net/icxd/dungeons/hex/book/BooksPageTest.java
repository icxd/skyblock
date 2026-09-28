package net.icxd.dungeons.hex.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.hex.HexCategories;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Book;
import net.icxd.dungeons.menu.Icon;

/** The Books category and "The Hex ➜ Books", against the wiki's Weapon and Armor tabs and the official screenshot. */
class BooksPageTest {
    /** Where a Cost block goes, with what it's for. */
    private static final Function<HexCosts, List<String>> COST = cost -> List.of("<cost " + cost.parts() + ">");

    @TempDir
    Path folder;

    @AfterEach
    void noItems() {
        ItemRegistry.loadData(folder.resolve("none.json"));
    }

    private SkyBlockItem load(String type) throws IOException {
        Path file = folder.resolve("items.json");
        Files.writeString(file, "{\"format\":1,\"items\":{"
                + "\"TEST_ITEM\":{\"material\":\"IRON_SWORD\",\"name\":\"Test Item\",\"rarity\":\"LEGENDARY\",\"type\":\"" + type + "\"},"
                + "\"HOT_POTATO_BOOK\":{\"glowing\":true,\"lore\":[\"&7A potato book for tests.\"],\"material\":\"BOOK\","
                + "\"name\":\"Hot Potato Book\",\"rarity\":\"EPIC\"}}}");
        ItemRegistry.loadData(file);
        return ItemRegistry.get("TEST_ITEM");
    }

    private static NBTTagCompound tag(int potatoBooks) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "TEST_ITEM");
        tag.setInt("hot_potato_books", potatoBooks);
        tag.setBoolean("art_of_war", false);
        return tag;
    }

    /** The wiki's Weapon tab: potato books, the Book of Stats and The Art of War, each once, centred in 21-25. */
    @Test
    void aWeapon() throws IOException {
        SkyBlockItem sword = load("SWORD");
        HexItem item = new HexItem(sword, tag(0), null);
        assertTrue(HexCategories.BOOKS.applies(item));
        assertEquals(List.of("  &5Hot Potato Book &e0&7/&a10", "  &5Fuming Potato Book &e0&7/&a5", "  &5Book of Stats &c✖",
                "  &6The Art of War &c✖"), HexCategories.BOOKS.summary(item));
        assertEquals(List.of(21, 22, 24, 25), List.copyOf(HexPage.grid(Book.on(sword).size(), 0, HexPage.Placement.CENTRED).keySet()));
    }

    /** The wiki's Armor tab: potato books and The Art of Peace, in 21, 23 and 25. */
    @Test
    void armor() throws IOException {
        SkyBlockItem helmet = load("HELMET");
        HexItem item = new HexItem(helmet, tag(0), null);
        assertEquals(List.of("  &5Hot Potato Book &e0&7/&a10", "  &5Fuming Potato Book &e0&7/&a5", "  &6The Art of Peace &c✖"),
                HexCategories.BOOKS.summary(item));
        assertEquals(List.of(21, 23, 25), List.copyOf(HexPage.grid(Book.on(helmet).size(), 0, HexPage.Placement.CENTRED).keySet()));
    }

    /** The screenshot's "Hot Potato Book 10/10" is green all through once it's full; what's on shows ✔. */
    @Test
    void appliedAndFull() throws IOException {
        SkyBlockItem sword = load("SWORD");
        NBTTagCompound tag = tag(10);
        Book.ART_OF_WAR.apply(tag);
        Book.STATS.apply(tag);
        assertEquals(List.of("  &5Hot Potato Book &a10&7/&a10", "  &5Fuming Potato Book &e0&7/&a5", "  &5Book of Stats &a✔",
                "  &6The Art of War &a✔"), HexCategories.BOOKS.summary(new HexItem(sword, tag, null)));
        tag.setInt("hot_potato_books", 13);
        assertEquals("  &5Fuming Potato Book &e3&7/&a5", HexCategories.BOOKS.summary(new HexItem(sword, tag, null)).get(1));
    }

    /** Items no book goes on get no Books button. */
    @Test
    void notForEveryItem() throws IOException {
        assertFalse(HexCategories.BOOKS.applies(new HexItem(load("WAND"), tag(0), null)));
    }

    /** An entry is its book's item: name, own text, a blank line, then the Cost block, or why none can go on. */
    @Test
    void entries() throws IOException {
        SkyBlockItem sword = load("SWORD");
        Icon hot = BooksPage.icon(Book.HOT_POTATO, sword, tag(0), COST);
        assertEquals(Material.BOOK, hot.material());
        assertEquals("&5Hot Potato Book", hot.name());
        assertEquals(List.of("&7A potato book for tests.", "", "<cost [Items[id=HOT_POTATO_BOOK, amount=1]]>"), hot.lore());
        // Fuming waits for 10 Hot; a full one says so (UNKNOWN wording: ours).
        assertEquals(List.of(BooksPage.FUMING_TOO_EARLY), BooksPage.icon(Book.FUMING_POTATO, sword, tag(9), COST).lore());
        assertEquals(List.of("<cost [Items[id=FUMING_POTATO_BOOK, amount=1]]>"), BooksPage.icon(Book.FUMING_POTATO, sword, tag(10), COST).lore());
        assertEquals(List.of("&7A potato book for tests.", "", BooksPage.MAXED), BooksPage.icon(Book.HOT_POTATO, sword, tag(12), COST).lore());
        assertEquals(List.of(BooksPage.MAXED), BooksPage.icon(Book.FUMING_POTATO, sword, tag(15), COST).lore());
        // Without its item in the data: a plain book, named as the Hex's summary names it.
        Icon war = BooksPage.icon(Book.ART_OF_WAR, sword, tag(0), COST);
        assertEquals(new Icon(Material.BOOK, "&6The Art of War", List.of("<cost [Items[id=THE_ART_OF_WAR, amount=1]]>")), war);
    }

    @Test
    void header() {
        assertEquals(new Icon(Material.ANVIL, "&aApply Books", List.of("&7Knowledge is &6power&7! Apply", "&7special books to your item to",
                "&7upgrade it!")), BooksPage.HEADER);
        assertEquals("The Hex ➜ Books", BooksPage.TITLE);
    }

    /** Every book's summary line, for an item of each kind they go on. */
    @Test
    void everyBooksLine() throws IOException {
        List<String> lines = new ArrayList<>();
        for (String type : List.of("DRILL", "VACUUM", "FARMING_TOOL", "FISHING_ROD")) {
            lines.addAll(HexCategories.BOOKS.summary(new HexItem(load(type), tag(0), null)));
        }
        assertEquals(List.of("  &9Polarvoid Book &e0&7/&a5",
                "  &5Farming for Dummies &e0&7/&a5", "  &9Bookworm's Favorite Book &e0&7/&a5",
                "  &5Book of Stats &c✖", "  &5Farming for Dummies &e0&7/&a5",
                "  &5Hot Potato Book &e0&7/&a10", "  &5Fuming Potato Book &e0&7/&a5", "  &5Book of Stats &c✖", "  &6The Art of War &c✖",
                "  &5Wet Book &e0&7/&a5"), lines);
    }
}
