package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.icxd.dungeons.item.bonus.TestPieces.fullSet;
import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.tiered;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Set bonus headers and tiered numbers for how many of the set is worn. */
class SetBonusLoreTest {
    private static ItemBlock block(String json) {
        return item("TEST_LORE_CHESTPLATE", json).blocks().get(0);
    }

    /** The data's own header with none worn; the count grey until the set is complete, then all gold (as recorded). */
    @Test
    void fullSetHeader() {
        ItemBlock set = block(fullSet("Test Set", 4));
        assertEquals("&6Full Set Bonus: Test Set &7(0/4)", SetBonusLore.header(set, 0, false));
        assertEquals("&6Full Set Bonus: Test Set &7(2/4)", SetBonusLore.header(set, 2, false));
        assertEquals("&6Full Set Bonus: Test Set (4/4)", SetBonusLore.header(set, 4, true));
    }

    /** A name in its own colour keeps it; the count follows it. */
    @Test
    void colouredName() {
        ItemBlock set = block("{\"kind\":\"FULL_SET\",\"name\":\"Test Pack\",\"header\":\"&6Full Set Bonus: &cTest Pack &7(0/4)\",\"pieces\":4}");
        assertEquals("&6Full Set Bonus: &cTest Pack &7(3/4)", SetBonusLore.header(set, 3, false));
        assertEquals("&6Full Set Bonus: &cTest Pack (4/4)", SetBonusLore.header(set, 4, true));
    }

    /** A tiered bonus is dark gray until it counts, then gold, and its count can go past its pieces ("(3/2)", the wiki's Armor). */
    @Test
    void tieredHeader() {
        ItemBlock tiers = block(tiered("Test Tiers", 2, "&7Test."));
        assertEquals("&8Tiered Bonus: Test Tiers (0/2)", SetBonusLore.header(tiers, 0, false));
        assertEquals("&8Tiered Bonus: Test Tiers (1/2)", SetBonusLore.header(tiers, 1, false));
        assertEquals("&6Tiered Bonus: Test Tiers (3/2)", SetBonusLore.header(tiers, 3, true));
    }

    /** A header with no count (a SNEAK bonus's) stays as it is. */
    @Test
    void noCount() {
        ItemBlock sneak = block("{\"kind\":\"FULL_SET\",\"name\":\"Test Sneak\",\"header\":\"&6Full Set Bonus: Test Sneak &e&lSNEAK\"}");
        assertEquals("&6Full Set Bonus: Test Sneak &e&lSNEAK", SetBonusLore.header(sneak, 4, true));
    }

    /** Nobody holding it: the block as the data has it. */
    @Test
    void noHolder() {
        ItemBlock set = block(fullSet("Test Set", 4));
        assertSame(set, SetBonusLore.shown(set, null));
    }

    /** A made-up tiered bonus whose "+5" grows: 5, 10 and 20 from 2 pieces. */
    private record Growing(String name) implements Bonus {
        static final Tiers HEALTH = new Tiers(2, 5, 10, 20);

        @Override
        public String kind() {
            return SetKey.TIERED;
        }

        @Override
        public int needs(SetKey set) {
            return 2;
        }

        @Override
        public List<String> text(List<String> text, int count) {
            return HEALTH.text(text, "&c+", count, 2);
        }
    }

    /** A tiered bonus's numbers for how many are worn; below the least it takes, the first tier's, as the data has them. */
    @Test
    void tieredNumbers() {
        DataItem helmet = item("TEST_GROWING_HELMET", tiered("Test Growing", 4, "&7Grants &c+5❤ Health&7."));
        ItemBlock growing = helmet.blocks().get(0);
        Bonus bonus = new Growing("Test Growing");
        ItemBlock three = SetBonusLore.shown(growing, 3, true, bonus);
        assertEquals("&6Tiered Bonus: Test Growing (3/4)", three.header());
        assertEquals(List.of("&7Grants &c+10❤ Health&7."), three.text());
        assertEquals(List.of("&7Grants &c+20❤ Health&7."), SetBonusLore.shown(growing, 4, true, bonus).text());
        assertEquals(growing.text(), SetBonusLore.shown(growing, 1, false, bonus).text());
    }
}
