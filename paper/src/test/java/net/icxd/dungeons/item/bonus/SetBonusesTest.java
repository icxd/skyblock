package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.data.DataItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.icxd.dungeons.item.bonus.TestPieces.fullSet;
import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.tiered;
import static net.icxd.dungeons.item.bonus.TestPieces.wearing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The bonuses that do something, and when they count: made-up pieces with their blocks' names. */
class SetBonusesTest {
    /** Four pieces of a set with this block. */
    private static DataItem[] set(String block) {
        return new DataItem[] {item("TEST_BONUS_HELMET", block), item("TEST_BONUS_CHESTPLATE", block), item("TEST_BONUS_LEGGINGS", block),
                item("TEST_BONUS_BOOTS", block)};
    }

    private static List<String> counting(DataItem... worn) {
        return SetBonuses.active(wearing(worn), SetBonuses.all()).stream().map(a -> a.bonus().name()).toList();
    }

    /** Shadow Assassin's full set counts at 4 pieces, not 3. */
    @Test
    void fullSetAtItsPieces() {
        DataItem[] pieces = set(fullSet("Shadow Assassin", 4));
        assertEquals(List.of(), counting(pieces[0], pieces[1], pieces[2]));
        assertEquals(List.of("Shadow Assassin"), counting(pieces));
    }

    /** Enrage's header has no count: it takes the Reaper set's three pieces. */
    @Test
    void noCountTakesTheSet() {
        String enrage = "{\"kind\":\"FULL_SET\",\"name\":\"Enrage SNEAK\",\"header\":\"&6Full Set Bonus: Enrage &e&lSNEAK\",\"cooldown\":25}";
        DataItem[] pieces = set(enrage);
        assertEquals(List.of(), counting(pieces[1], pieces[2]));
        assertEquals(List.of("Enrage SNEAK"), counting(pieces[1], pieces[2], pieces[3]));
    }

    /** Arachne's Faithful counts from 2 of its 8; a tiered bonus's count is what's worn. */
    @Test
    void tieredFromItsLeast() {
        DataItem[] pieces = set(tiered("Arachne's Faithful", 8, "&7Test."));
        assertEquals(List.of(), counting(pieces[0]));
        List<Bonus.Active> two = SetBonuses.active(wearing(pieces[0], pieces[3]), SetBonuses.all());
        assertEquals(1, two.size());
        assertEquals(2, two.get(0).count());
    }

    /** Two bonuses of the same name and different kinds are apart: Tarantula's full set and Primordial's tiered Octodexterity. */
    @Test
    void sameNameOtherKind() {
        assertTrue(SetBonuses.bonus(SetKey.FULL_SET, "Octodexterity") != SetBonuses.bonus(SetKey.TIERED, "Octodexterity"));
        assertEquals(List.of("Octodexterity"), counting(set(tiered("Octodexterity", 4, "&7Test."))));
        assertNull(SetBonuses.bonus(SetKey.FULL_SET, "Test Only Text"));
    }

    /** Items' own text: the Wither pieces count one each, with the set bonus as well once there are four. */
    @Test
    void itemsOwnText() {
        String witherborn = fullSet("Witherborn", 4);
        DataItem helmet = item("WITHER_HELMET", witherborn);
        DataItem boots = item("TANK_WITHER_BOOTS", witherborn);
        List<Bonus.Active> active = SetBonuses.active(wearing(helmet, boots), SetBonuses.all());
        assertEquals(List.of("Wither"), active.stream().map(a -> a.bonus().name()).toList());
        assertEquals(2, active.get(0).count());
    }
}
