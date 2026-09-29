package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.regex.Pattern;

import org.bson.Document;
import org.junit.jupiter.api.Test;

/** The Archfiend Dice's rules. */
class ArchfiendDiceTest {
    private static final double EPS = 1e-9;

    /** 1 to 6: -120, -80, -40, +40, +80, +120 (the wiki's), from the text's most. */
    @Test
    void eachRollsHealth() {
        double[] expected = {-120, -80, -40, 40, 80, 120};
        for (int roll = 1; roll <= 6; roll++) assertEquals(expected[roll - 1], ArchfiendDice.health(roll, 120), EPS);
        assertEquals(-300, ArchfiendDice.health(1, 300), EPS);
        assertEquals(200, ArchfiendDice.health(5, 300), EPS);
    }

    /** A 6 one roll in 24, else 1 to 5 alike. */
    @Test
    void sixIsRare() {
        assertEquals(6, ArchfiendDice.roll(0.01, 3));
        assertEquals(4, ArchfiendDice.roll(0.5, 3));
        assertEquals(1, ArchfiendDice.roll(0.99, 0));
        assertEquals(5, ArchfiendDice.roll(0.99, 4));
    }

    @Test
    void coinsFromTheText() {
        Pattern costs = Pattern.compile("Costs ([\\d.]+)([kMB]?) coins to roll");
        assertEquals(666_700, ArchfiendDice.coins(costs, "Costs 666.7k coins to roll between 1-6."), EPS);
        assertEquals(6_700_000, ArchfiendDice.coins(costs, "Costs 6.7M coins to roll between 1-6."), EPS);
        assertEquals(-1, ArchfiendDice.coins(costs, "nothing"), EPS);
    }

    /** Kept on the profile until it runs out. */
    @Test
    void rolledUntilItRunsOut() {
        Document profile = new Document(ArchfiendDice.FIELD, new Document("health", -80.0).append("until", 1_000L));
        assertEquals(-80, ArchfiendDice.rolled(profile, 999), EPS);
        assertEquals(0, ArchfiendDice.rolled(profile, 1_000), EPS);
        assertEquals(0, ArchfiendDice.rolled(new Document(), 0), EPS);
    }
}
