package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.economy.ExpOrbs;
import net.icxd.dungeons.utils.SkyBlockTime;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.worn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The combat sets' rules that are only numbers: stacks, shares, strengths and the lines their lore swaps. */
class CombatSetsTest {
    private static final double EPSILON = 1e-9;

    /** Regenerative Howl: each stack its own time, the newest 10 counting. */
    @Test
    void regenerativeHowl() {
        Deque<Long> stacks = new ArrayDeque<>();
        for (int i = 0; i < 12; i++) CombatSets.RegenerativeHowl.add(stacks, 5_000 + i * 1_000L, 10);
        assertEquals(10, CombatSets.RegenerativeHowl.count(stacks, 0));
        assertEquals(4, CombatSets.RegenerativeHowl.count(stacks, 12_000));
        assertEquals(0, CombatSets.RegenerativeHowl.count(stacks, 20_000));
    }

    /** Refraction: 12.5% of the pieces' stats at light 0, 100% at 7, 200% at 15, and the light line for a holder. */
    @Test
    void refraction() {
        assertEquals(0.125, CombatSets.Refraction.share(0), EPSILON);
        assertEquals(1, CombatSets.Refraction.share(7), EPSILON);
        assertEquals(2, CombatSets.Refraction.share(15), EPSILON);
        List<String> text = List.of("&7The stats change with the light.", "", "&7Current Light Level", "&c0&8 (0%)");
        assertEquals("&c9&8 (125%)", CombatSets.Refraction.lightLine(text, 9).get(3));
    }

    /** Fearsome: mobs at or below their Fear, none without any. */
    @Test
    void fearsome() {
        assertTrue(CombatSets.Fearsome.afraid(10, 10));
        assertFalse(CombatSets.Fearsome.afraid(11, 10));
        assertFalse(CombatSets.Fearsome.afraid(0, 0));
    }

    /** Berserk takes 20%, 40% and 60% off health costs with 2, 3 and 4 pieces. */
    @Test
    void berserk() {
        assertEquals(0.8, TieredSets.Berserk.cost(2), EPSILON);
        assertEquals(0.4, TieredSets.Berserk.cost(4), EPSILON);
    }

    /** Dominus's swipe: the tier's strength times 1 to 3 for 2 to 4 pieces, over the hit's Additive Multiplier. */
    @Test
    void dominusSwipe() {
        assertEquals(0.5, TieredSets.Dominus.strength(0, 2), EPSILON);
        assertEquals(1.875, TieredSets.Dominus.strength(1, 4), EPSILON);
        assertEquals(3, TieredSets.Dominus.strength(4, 4), EPSILON);
        // A 10,000 hit with +300% additive and Swipe Strength 0.5: 10,000 x 50 / 400.
        assertEquals(1_250, TieredSets.Dominus.swipeDamage(10_000, 0.5, 300), EPSILON);
    }

    /** Kuudra stacks can have a most of their own, and be spent a few at a time. */
    @Test
    void stacksSpent() {
        TieredSets.Stacks stacks = new TieredSets.Stacks();
        for (int i = 0; i < 50; i++) stacks.hit(i * 100L, 0, 4, 30);
        assertEquals(30, stacks.at(4_900));
        assertEquals(12, stacks.spend(4_900, 12));
        assertEquals(18, stacks.at(4_900));
        assertEquals(18, stacks.spend(4_900, 99));
        assertEquals(0, stacks.at(4_900));
    }

    /** Spirit: the wearer and up to 4 others grant stacks, each at most once every so often. */
    @Test
    void spirit() {
        UUID wearer = new UUID(0, 1);
        TieredSets.Spirit.Gained gained = new TieredSets.Spirit.Gained();
        assertTrue(gained.hit(wearer, wearer, 0, 3, 4, 10));
        assertFalse(gained.hit(wearer, wearer, 1_000, 3, 4, 10));
        for (int i = 2; i <= 5; i++) assertTrue(gained.hit(wearer, new UUID(0, i), 1_000, 3, 4, 10));
        assertFalse(gained.hit(wearer, new UUID(0, 6), 1_000, 3, 4, 10), "a fifth other");
        assertTrue(gained.hit(wearer, wearer, 3_000, 3, 4, 10));
        assertEquals(6, gained.stacks.at(3_000));
    }

    /** Static Charge: a charge every so long from when it went on, up to its capacity; the text's numbers by pieces. */
    @Test
    void staticCharge() {
        TieredSets.StaticCharge.Charges charges = new TieredSets.StaticCharge.Charges();
        charges.tick(10_000, 10, 2);
        assertEquals(1, charges.charges);
        charges.tick(15_000, 10, 2);
        assertEquals(1, charges.charges);
        charges.tick(20_000, 10, 2);
        charges.tick(30_000, 10, 2);
        assertEquals(2, charges.charges);
        List<String> text = List.of("&7A charge every &e30", "&7seconds, adding &a15%&7 a charge.", "&7Maximum Charge Capacity: &c2");
        assertEquals(List.of("&7A charge every &e10", "&7seconds, adding &a40%&7 a charge.", "&7Maximum Charge Capacity: &c5"),
                new TieredSets.StaticCharge().text(text, 5));
    }

    /** Rekindle: burning damage +200% to +600%, and more a second on fire up to its most; the text's numbers by pieces. */
    @Test
    void rekindle() {
        assertEquals(3, TieredSets.Rekindle.factor(2, 0), EPSILON);
        assertEquals(3.12, TieredSets.Rekindle.factor(2, 10), EPSILON);
        assertEquals(3.5, TieredSets.Rekindle.factor(2, 1_000), EPSILON);
        assertEquals(9, TieredSets.Rekindle.factor(4, 1_000), EPSILON);
        List<String> text = List.of("&7Burning damage by &c200%&7, and by &c1.2%", "&7a second up to &c50%&7.");
        assertEquals(List.of("&7Burning damage by &c600%&7, and by &c5%", "&7a second up to &c200%&7."), new TieredSets.Rekindle().text(text, 4));
    }

    /** Shimmer: +200% experience from mobs and ores with 2 or 3 pieces, 300% with 4, none from the rest. */
    @Test
    void shimmer() {
        GatheringSets.Shimmer shimmer = new GatheringSets.Shimmer();
        Bonus.Active four = new Bonus.Active(shimmer, null, 4, List.of());
        assertEquals(300, shimmer.experience(null, four, ExpOrbs.Source.ORE), EPSILON);
        assertEquals(0, shimmer.experience(null, four, ExpOrbs.Source.OTHER), EPSILON);
        assertEquals(List.of("&7Tests by &a300%&7."), shimmer.text(List.of("&7Tests by &a200%&7."), 4));
    }

    /** Deepness Within's Health a Fishing level by pieces, and its level line. */
    @Test
    void deepnessWithin() {
        GatheringSets.DeepnessWithin deepness = new GatheringSets.DeepnessWithin();
        assertEquals(List.of("&7Gain &c10❤ &7a test level."), deepness.text(List.of("&7Gain &c6❤ &7a test level."), 4));
        assertEquals("&7Fishing: &eLevel 12", GatheringSets.DeepnessWithin.level(List.of("&7Fishing: &eLevel &k00"), 12).get(0));
    }

    /** Bat Person's pieces count three times in the Spooky Festival (Autumn 29th to 31st), twice at night. */
    @Test
    void batPerson() {
        assertEquals(3, OtherSets.BatPerson.times(new SkyBlockTime(400, 7, 30, 12, 0)));
        assertEquals(2, OtherSets.BatPerson.times(new SkyBlockTime(400, 7, 28, 22, 0)));
        assertEquals(1, OtherSets.BatPerson.times(new SkyBlockTime(400, 7, 28, 12, 0)));
    }

    /** The lowest Kuudra tier's block is the one whose numbers count. */
    @Test
    void lowestBlock() {
        String basic = TestPieces.tiered("Spirit", 4, "&7Test. &8(Max 10 Stacks)");
        String infernal = TestPieces.tiered("Spirit", 4, "&7Test. &8(Max 40 Stacks)");
        List<Worn.Piece> pieces = List.of(worn(item("INFERNAL_HOLLOW_HELMET", infernal)), worn(item("HOLLOW_BOOTS", basic)));
        assertEquals(10, TieredSets.Spirit.most(pieces));
        assertEquals(40, TieredSets.Spirit.most(List.of(pieces.get(0))));
    }
}
