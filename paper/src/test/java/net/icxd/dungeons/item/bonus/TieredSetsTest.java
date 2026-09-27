package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.worn;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tiered sets: Hydra Strike's damage, the Kuudra stacks, and the numbers their lore swaps. */
class TieredSetsTest {
    private static final double EPSILON = 1e-9;

    /** Hydra Strike's Damage a stack: 2, 4 and 6% for 2, 3 and 4 pieces of the basic tier, 5, 10 and 15% of Infernal. */
    @Test
    void hydraStrike() {
        assertEquals(2, TieredSets.HydraStrike.perStack(0, 2), EPSILON);
        assertEquals(6, TieredSets.HydraStrike.perStack(0, 4), EPSILON);
        assertEquals(8.25, TieredSets.HydraStrike.perStack(1, 4), EPSILON);
        assertEquals(15, TieredSets.HydraStrike.perStack(4, 4), EPSILON);
        assertEquals(0, TieredSets.lowestTier(List.of(worn(item("INFERNAL_TERROR_HELMET")), worn(item("TERROR_BOOTS")))));
        assertEquals(3, TieredSets.lowestTier(List.of(worn(item("INFERNAL_TERROR_HELMET")), worn(item("FIERY_TERROR_BOOTS")))));
    }

    /** Kuudra stacks: one at most every so often, up to 10, one lost each time they go that long without one. */
    @Test
    void stacks() {
        TieredSets.Stacks stacks = new TieredSets.Stacks();
        stacks.hit(0, 1.5, 4);
        stacks.hit(1_000, 1.5, 4);
        assertEquals(1, stacks.at(1_000, 4));
        stacks.hit(1_500, 1.5, 4);
        assertEquals(2, stacks.at(1_500, 4));
        assertEquals(1, stacks.at(5_500, 4));
        assertEquals(0, stacks.at(9_500, 4));
        for (int i = 0; i < 20; i++) stacks.hit(20_000 + i * 2_000L, 1.5, 4);
        assertEquals(10, stacks.at(58_000, 4));
    }

    /** The numbers tiered bonuses swap in their lore: Dominus's, Hydra Strike's (to one decimal), Berserk's, Long Tuba's, Arachne's. */
    @Test
    void tieredText() {
        List<String> dominus = List.of("&7Every &a1.5s&7, melee attacks grant a test stack.", "&8Lose 1 stack after 4s of not gaining one.");
        assertEquals(List.of("&7Every &a1s&7, melee attacks grant a test stack.", "&8Lose 1 stack after 7s of not gaining one."),
                new TieredSets.Dominus().text(dominus, 3));
        List<String> hydra = List.of("&7Each stack grants &c+2.8% Damage&7 in a test.", "&8Lose 1 stack after 4s.");
        assertEquals(List.of("&7Each stack grants &c+8.2% Damage&7 in a test.", "&8Lose 1 stack after 10s."),
                new TieredSets.HydraStrike().text(hydra, 4));
        List<String> berserk = List.of("&7Cuts test costs by &c20%&7.");
        assertEquals(List.of("&7Cuts test costs by &c60%&7."), new TieredSets.Berserk().text(berserk, 4));
        List<String> tuba = List.of("&7Grants &3+2⚶ Respiration&7.");
        assertEquals(List.of("&7Grants &3+5⚶ Respiration&7."), new TieredSets.LongTuba().text(tuba, 3));
        assertEquals(tuba, new TieredSets.LongTuba().text(tuba, 0));
        List<String> arachne = List.of("&7Grants &c+5❤ Health &7and &a+5❈ Defense&7.");
        assertEquals(List.of("&7Grants &c+35❤ Health &7and &a+35❈ Defense&7."), new TieredSets.ArachnesFaithful().text(arachne, 5));
    }

    /** Stats by pieces worn: Arachne's +20 Health and Defense with 4, Long Tuba's +5 Respiration with 3; Berserk halves Health and Defense. */
    @Test
    void tieredStats() {
        Stats stats = new Stats();
        Bonus faithful = new TieredSets.ArachnesFaithful();
        faithful.stats(null, new Bonus.Active(faithful, null, 4, List.of()), stats);
        Bonus tuba = new TieredSets.LongTuba();
        tuba.stats(null, new Bonus.Active(tuba, null, 3, List.of()), stats);
        assertEquals(new Stats().set(Stat.HEALTH, 20).set(Stat.DEFENSE, 20).set(Stat.RESPIRATION, 5), stats);
        Bonus berserk = new TieredSets.Berserk();
        berserk.derivedStats(null, new Bonus.Active(berserk, null, 2, List.of()), stats);
        assertEquals(10, stats.get(Stat.HEALTH), EPSILON);
        assertEquals(10, stats.get(Stat.DEFENSE), EPSILON);
    }
}
