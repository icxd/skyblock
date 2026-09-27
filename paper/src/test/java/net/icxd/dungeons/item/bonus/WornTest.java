package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.data.DataItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.icxd.dungeons.item.bonus.TestPieces.fullSet;
import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.piece;
import static net.icxd.dungeons.item.bonus.TestPieces.tiered;
import static net.icxd.dungeons.item.bonus.TestPieces.wearing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which sets a player wears pieces of, and which bonuses that makes count. */
class WornTest {
    private static final DataItem HELMET = item("TEST_SET_HELMET", fullSet("Test Set", 4));
    private static final DataItem CHESTPLATE = item("TEST_SET_CHESTPLATE", fullSet("Test Set", 4), piece("PIECE", "Test Piece"));
    private static final DataItem LEGGINGS = item("TEST_SET_LEGGINGS", fullSet("Test Set", 4));
    private static final DataItem BOOTS = item("TEST_SET_BOOTS", fullSet("Test Set", 4));
    /** In two sets at once, as Nutcracker Armor is. */
    private static final DataItem TWO_SETS = item("TEST_TWO_HELMET", fullSet("Test Set", 4), fullSet("Other Set", 4));
    /** The same name, but counting to 8 with equipment: another set. */
    private static final DataItem OF_EIGHT = item("TEST_EIGHT_CHESTPLATE", fullSet("Test Set", 8));
    private static final DataItem TIERED_HELMET = item("TEST_TIER_HELMET", tiered("Test Tiers", 4, "&7Test."));
    private static final DataItem TIERED_BOOTS = item("TEST_TIER_BOOTS", tiered("Test Tiers", 4, "&7Test."));

    private static final SetKey SET = new SetKey("FULL_SET", "Test Set", 4);

    @Test
    void piecesOfASetCount() {
        Worn worn = wearing(HELMET, CHESTPLATE, LEGGINGS);
        assertEquals(3, worn.count(SET));
        assertEquals(0, worn.count(new SetKey("FULL_SET", "Other Set", 4)));
        assertEquals(List.of("TEST_SET_HELMET", "TEST_SET_CHESTPLATE", "TEST_SET_LEGGINGS"), worn.in(SET).stream().map(Worn.Piece::id).toList());
    }

    /** Two sets' pieces worn together: two sets of 2, neither complete. */
    @Test
    void mixedSets() {
        Worn worn = wearing(HELMET, CHESTPLATE, TIERED_HELMET, TIERED_BOOTS);
        assertEquals(2, worn.count(SET));
        assertEquals(2, worn.count(new SetKey("TIERED", "Test Tiers", 4)));
    }

    /** A piece in two sets counts in each; a set of the same name that counts to 8 is another set. */
    @Test
    void piecesInTwoSets() {
        Worn worn = wearing(TWO_SETS, CHESTPLATE, OF_EIGHT);
        assertEquals(2, worn.count(SET));
        assertEquals(1, worn.count(new SetKey("FULL_SET", "Other Set", 4)));
        assertEquals(1, worn.count(new SetKey("FULL_SET", "Test Set", 8)));
    }

    /** A piece with two blocks of the same set still counts once. */
    @Test
    void aPieceCountsOnce() {
        DataItem twice = item("TEST_TWICE_BOOTS", fullSet("Test Set", 4), fullSet("Test Set", 4));
        assertEquals(1, wearing(twice).count(SET));
    }

    /** A made-up bonus for these tests. */
    private record TestBonus(String kind, String name, int needs) implements Bonus {
        @Override
        public int needs(SetKey set) {
            return needs > 0 ? needs : Bonus.super.needs(set);
        }

        @Override
        public boolean item(String id) {
            return id.startsWith("TEST_SET_");
        }
    }

    /** A full set's bonus counts once all its pieces are worn, not before. */
    @Test
    void aFullSetCountsAtItsPieces() {
        TestBonus set = new TestBonus("FULL_SET", "Test Set", 0);
        assertTrue(SetBonuses.active(wearing(HELMET, CHESTPLATE, LEGGINGS), List.of(set)).isEmpty());
        List<Bonus.Active> active = SetBonuses.active(wearing(HELMET, CHESTPLATE, LEGGINGS, BOOTS), List.of(set));
        assertEquals(1, active.size());
        assertEquals(4, active.get(0).count());
        assertEquals(SET, active.get(0).set());
    }

    /** A tiered bonus counts from its least pieces, with however many there are. */
    @Test
    void aTieredBonusCountsFromItsLeast() {
        TestBonus tiers = new TestBonus("TIERED", "Test Tiers", 2);
        assertTrue(SetBonuses.active(wearing(TIERED_HELMET), List.of(tiers)).isEmpty());
        assertEquals(2, SetBonuses.active(wearing(TIERED_HELMET, TIERED_BOOTS), List.of(tiers)).get(0).count());
    }

    /** A piece's own bonus counts for each piece that has it; an item's own for each piece it's on. */
    @Test
    void piecesAndItems() {
        TestBonus own = new TestBonus("PIECE", "Test Piece", 0);
        TestBonus items = new TestBonus(Bonus.ITEM, "Test Items", 0);
        List<Bonus.Active> active = SetBonuses.active(wearing(HELMET, CHESTPLATE, TIERED_BOOTS), List.of(own, items));
        assertEquals(2, active.size());
        assertEquals(1, active.get(0).count());
        assertEquals(2, active.get(1).count());
        assertTrue(SetBonuses.active(Worn.NOTHING, List.of(own, items)).isEmpty());
    }

    /** With no bonus to say, a full set takes all its pieces and a tiered one counts from 1. */
    @Test
    void whatTheTextOnlyNeeds() {
        assertEquals(4, SetBonuses.needs(null, SET));
        assertEquals(1, SetBonuses.needs(null, new SetKey("TIERED", "Test Tiers", 4)));
        assertEquals(1, SetBonuses.needs(null, new SetKey("FULL_SET", "No Count", 0)));
    }
}
