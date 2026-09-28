package net.icxd.dungeons.item.upgrade;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which books go on which items, how many, and what they add, as live items have them (see Book), on made-up items. */
class BookTest {
    static SkyBlockItem item(String type) {
        String json = "\"TEST_" + type + "\":{\"material\":\"IRON_SWORD\",\"name\":\"Test\",\"rarity\":\"LEGENDARY\",\"type\":\"" + type + "\"}";
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NBTTagCompound data() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInt("hot_potato_books", 0);
        tag.setBoolean("art_of_war", false);
        return tag;
    }

    @Test
    void whichBooksGoOnWhat() {
        List<Book> weapon = List.of(Book.HOT_POTATO, Book.FUMING_POTATO, Book.STATS, Book.ART_OF_WAR);
        // The live types with potato books and The Art of War: never a wand.
        for (String type : List.of("SWORD", "LONGSWORD", "BOW", "AXE", "GAUNTLET")) assertEquals(weapon, Book.on(item(type)), type);
        assertEquals(List.of(Book.HOT_POTATO, Book.FUMING_POTATO, Book.STATS, Book.ART_OF_WAR, Book.WET), Book.on(item("FISHING_ROD")));
        assertEquals(List.of(), Book.on(item("WAND")));
        for (String type : List.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")) {
            assertEquals(List.of(Book.HOT_POTATO, Book.FUMING_POTATO, Book.ART_OF_PEACE), Book.on(item(type)), type);
        }
        assertEquals(List.of(Book.POLARVOID), Book.on(item("DRILL")));
        assertEquals(List.of(Book.FARMING_FOR_DUMMIES, Book.BOOKWORM), Book.on(item("VACUUM")));
        assertEquals(List.of(Book.STATS, Book.FARMING_FOR_DUMMIES), Book.on(item("FARMING_TOOL")));
        for (String type : List.of("PICKAXE", "ACCESSORY", "BELT", "CARNIVAL_MASK", "DEPLOYABLE")) assertEquals(List.of(), Book.on(item(type)), type);
    }

    /** One count for both potato books: 10 Hot, then 5 Fuming (NEU's Hex); the summary's counts split it so. */
    @Test
    void potatoBooks() {
        SkyBlockItem sword = item("SWORD");
        NBTTagCompound tag = data();
        assertTrue(Book.HOT_POTATO.applicable(sword, tag));
        assertFalse(Book.FUMING_POTATO.applicable(sword, tag));
        for (int i = 0; i < 9; i++) Book.HOT_POTATO.apply(tag);
        assertEquals(9, Book.HOT_POTATO.count(tag));
        assertFalse(Book.FUMING_POTATO.applicable(sword, tag));
        Book.HOT_POTATO.apply(tag);
        assertTrue(Book.HOT_POTATO.maxed(tag));
        assertFalse(Book.HOT_POTATO.applicable(sword, tag));
        assertTrue(Book.FUMING_POTATO.applicable(sword, tag));
        for (int i = 0; i < 5; i++) Book.FUMING_POTATO.apply(tag);
        assertEquals(15, tag.getInt("hot_potato_books"));
        assertEquals(10, Book.HOT_POTATO.count(tag));
        assertEquals(5, Book.FUMING_POTATO.count(tag));
        assertFalse(Book.FUMING_POTATO.applicable(sword, tag));
        // Each is +2 Damage and +2 Strength on a weapon, in one bracket.
        Stats stats = Book.HOT_POTATO.stats(sword, tag);
        assertEquals(30, stats.get(Stat.DAMAGE), 1e-9);
        assertEquals(30, stats.get(Stat.STRENGTH), 1e-9);
        assertEquals(new Stats(), Book.FUMING_POTATO.stats(sword, tag));
        assertEquals("&e(+30)", Book.HOT_POTATO.bracket(30));
        // Armor: +4 Health and +2 Defense.
        Stats armor = Book.HOT_POTATO.stats(item("CHESTPLATE"), tag);
        assertEquals(60, armor.get(Stat.HEALTH), 1e-9);
        assertEquals(30, armor.get(Stat.DEFENSE), 1e-9);
        // The live Hellfire Rod and Gemstone Gauntlet: "(+30)" on Damage and Strength with 15.
        assertEquals(30, Book.HOT_POTATO.stats(item("FISHING_ROD"), tag).get(Stat.DAMAGE), 1e-9);
        assertEquals(30, Book.HOT_POTATO.stats(item("GAUNTLET"), tag).get(Stat.STRENGTH), 1e-9);
    }

    @Test
    void onceOnly() {
        SkyBlockItem sword = item("SWORD"), helmet = item("HELMET");
        NBTTagCompound tag = data();
        assertTrue(Book.ART_OF_WAR.applicable(sword, tag));
        assertFalse(Book.ART_OF_WAR.applicable(helmet, tag));
        Book.ART_OF_WAR.apply(tag);
        assertTrue(tag.getBoolean("art_of_war"));
        assertFalse(Book.ART_OF_WAR.applicable(sword, tag));
        assertEquals(5, Book.ART_OF_WAR.stats(sword, tag).get(Stat.STRENGTH), 1e-9);
        assertEquals("&6[+5]", Book.ART_OF_WAR.bracket(5));

        NBTTagCompound armor = data();
        Book.ART_OF_PEACE.apply(armor);
        assertTrue(armor.getBoolean("art_of_peace"));
        assertTrue(Book.ART_OF_PEACE.maxed(armor));
        assertEquals(40, Book.ART_OF_PEACE.stats(helmet, armor).get(Stat.HEALTH), 1e-9);
        assertEquals("&c[+40]", Book.ART_OF_PEACE.bracket(40));
    }

    /** The Book of Stats starts at 0 kills, and counts them on weapons; a farming tool's counts crops. */
    @Test
    void bookOfStats() {
        SkyBlockItem sword = item("SWORD"), hoe = item("FARMING_TOOL");
        NBTTagCompound tag = data();
        assertEquals(List.of(), Book.statsLines(sword, tag));
        assertFalse(Book.countsKills(sword, tag));
        Book.STATS.apply(tag);
        assertEquals(0, tag.getInt("stats_book"));
        assertTrue(Book.STATS.maxed(tag));
        assertEquals(List.of("&fKills: &60"), Book.statsLines(sword, tag));
        assertTrue(Book.countsKills(sword, tag));
        for (int i = 0; i < 2133; i++) Book.addKill(tag);
        assertEquals(List.of("&fKills: &62,133"), Book.statsLines(sword, tag));
        assertEquals(List.of("&fCrops Harvested: &62,133"), Book.statsLines(hoe, tag));
        assertFalse(Book.countsKills(hoe, tag));
        assertEquals(new Stats(), Book.STATS.stats(sword, tag));
    }

    /** Five of each at most, with the live items' brackets. */
    @Test
    void toolBooks() {
        NBTTagCompound tag = data();
        SkyBlockItem drill = item("DRILL"), vacuum = item("VACUUM"), rod = item("FISHING_ROD");
        assertEquals(new Stats(), Book.POLARVOID.stats(drill, tag));
        for (int i = 0; i < 5; i++) {
            assertTrue(Book.POLARVOID.applicable(drill, tag));
            Book.POLARVOID.apply(tag);
            Book.BOOKWORM.apply(tag);
            Book.FARMING_FOR_DUMMIES.apply(tag);
            Book.WET.apply(tag);
        }
        assertFalse(Book.POLARVOID.applicable(drill, tag));
        assertFalse(Book.BOOKWORM.applicable(vacuum, tag));
        assertFalse(Book.WET.applicable(rod, tag));
        assertEquals(5, Book.WET.count(tag));
        // The live Divan's Drill: "[+50]" Mining Speed and "[+5]" Mining Fortune.
        Stats polarvoid = Book.POLARVOID.stats(drill, tag);
        assertEquals(50, polarvoid.get(Stat.MINING_SPEED), 1e-9);
        assertEquals(5, polarvoid.get(Stat.MINING_FORTUNE), 1e-9);
        assertEquals("&9[+50]", Book.POLARVOID.bracket(50));
        // The live Infini-Vacuum: "(+100)" Damage in gold, "(+5)" Farming Fortune in green.
        assertEquals(100, Book.BOOKWORM.stats(vacuum, tag).get(Stat.DAMAGE), 1e-9);
        assertEquals("&6(+100)", Book.BOOKWORM.bracket(100));
        assertEquals(5, Book.FARMING_FOR_DUMMIES.stats(vacuum, tag).get(Stat.FARMING_FORTUNE), 1e-9);
        assertEquals("&a(+5)", Book.FARMING_FOR_DUMMIES.bracket(5));
        // The live Hellfire Rod: "(+5)" Fishing Speed in aqua.
        assertEquals(5, Book.WET.stats(rod, tag).get(Stat.FISHING_SPEED), 1e-9);
        assertEquals("&b(+5)", Book.WET.bracket(5));
    }

    /** Only the books with stats on the item, in the lore's order: potato books, then The Art of War. */
    @Test
    void bonusesInOrder() {
        SkyBlockItem sword = item("SWORD");
        NBTTagCompound tag = data();
        assertEquals(Map.of(), Book.bonuses(sword, tag));
        Book.ART_OF_WAR.apply(tag);
        Book.HOT_POTATO.apply(tag);
        Book.STATS.apply(tag);
        assertEquals(List.of(Book.HOT_POTATO, Book.ART_OF_WAR), List.copyOf(Book.bonuses(sword, tag).keySet()));
    }
}
