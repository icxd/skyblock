package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.icxd.dungeons.item.bonus.TestPieces.fullSet;
import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.worn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The other sets in numbers: the Tuxedos, the Blaze auras, Emerald's Tank, Smart Miner, Bat Person's night. */
class OtherSetsTest {
    private static final double EPSILON = 1e-9;

    /** The Tuxedos: the cheapest worn piece's max health and damage. */
    @Test
    void dashing() {
        DataItem cheap = item("CHEAP_TUXEDO_BOOTS", fullSet("Dashing", 3));
        DataItem elegant = item("ELEGANT_TUXEDO_CHESTPLATE", fullSet("Dashing", 3));
        assertEquals(List.of(250.0, 150.0), list(OtherSets.Dashing.of(List.of(worn(elegant)))));
        assertEquals(List.of(75.0, 50.0), list(OtherSets.Dashing.of(List.of(worn(elegant), worn(cheap)))));
    }

    @Test
    void otherSets() {
        assertEquals(500, OtherSets.BlazingAura.most(4_999), EPSILON);
        assertEquals(700, OtherSets.BlazingAura.most(10_000), EPSILON);
        assertEquals(5_000, OtherSets.BlazingAura.most(1_000_000), EPSILON);
        assertEquals(300, OtherSets.BlazingAura.damage(10_000, false, 500), EPSILON);
        assertEquals(500, OtherSets.BlazingAura.damage(1_000_000, false, 500), EPSILON);
        assertEquals(330, OtherSets.BlazingAura.damage(1_000, true, 500), EPSILON);
        assertEquals(350, OtherSets.Tank.each(5_000_000), EPSILON);
        assertEquals(3, OtherSets.Tank.each(9_999), EPSILON);
        Stats stats = new Stats().set(Stat.INTELLIGENCE, 100).set(Stat.MINING_SPEED, 40);
        OtherSets.SmartMiner.convert(stats);
        assertEquals(0, stats.get(Stat.INTELLIGENCE), EPSILON);
        assertEquals(46, stats.get(Stat.MINING_SPEED), EPSILON);
    }

    /** Bat Person's pieces (not its accessories) count their Combat Stats twice at night, and nothing else. */
    @Test
    void batPerson() {
        Bonus bat = new OtherSets.BatPerson();
        assertTrue(bat.item("BAT_PERSON_HELMET"));
        assertTrue(bat.item("BAT_PERSON_BOOTS"));
        assertFalse(bat.item("BAT_PERSON_TALISMAN"));
        Stats stats = new Stats().set(Stat.DEFENSE, 100).set(Stat.SPEED, 100);
        OtherSets.BatPerson.twice(new Stats().set(Stat.DEFENSE, 30).set(Stat.STRENGTH, 10).set(Stat.SPEED, 5), stats);
        assertEquals(new Stats().set(Stat.DEFENSE, 130).set(Stat.STRENGTH, 10).set(Stat.SPEED, 100), stats);
    }

    /** Armor of the Pack's pieces against Animal mobs: +50 Defense each, the chestplate's +75, 225 for the set. */
    @Test
    void packPieces() {
        List<Worn.Piece> set = List.of(worn(item("HELMET_OF_THE_PACK")), worn(item("CHESTPLATE_OF_THE_PACK")),
                worn(item("LEGGINGS_OF_THE_PACK")), worn(item("BOOTS_OF_THE_PACK")));
        assertEquals(225, OtherSets.PackPieces.defense(set), EPSILON);
        assertEquals(50, OtherSets.PackPieces.defense(set.subList(0, 1)), EPSILON);
        Bonus pack = new OtherSets.PackPieces();
        Stats stats = new Stats();
        pack.stats(null, new Bonus.Active(pack, null, 4, set), stats);
        assertEquals(new Stats().set(Stat.TRUE_DEFENSE, 20), stats);
    }

    /** The hats are their own items only; the Racing Helmet raises the Speed cap by 100. */
    @Test
    void hats() {
        assertTrue(new OtherSets.SpiderHat().item("SPIDER_HAT"));
        assertFalse(new OtherSets.SpiderHat().item("SPIDER_BOOTS"));
        assertTrue(new OtherSets.GhastHead().item("GHAST_HEAD"));
        assertEquals(100, new OtherSets.RacingHelmet().speedCap(null, null), EPSILON);
    }

    private static List<Double> list(double[] values) {
        return java.util.Arrays.stream(values).boxed().toList();
    }

    /** A set that's only stats adds them; the Tuxedo's damage is additive, on every hit. */
    @Test
    void hooks() {
        Bonus speedster = new OtherSets.Flat("Bonus Speed", new Stats().set(Stat.SPEED, 20));
        Stats stats = new Stats().set(Stat.SPEED, 100);
        speedster.stats(null, new Bonus.Active(speedster, null, 4, List.of()), stats);
        assertEquals(120, stats.get(Stat.SPEED), EPSILON);
        Bonus dashing = new OtherSets.Dashing();
        Bonus.Active fancy = new Bonus.Active(dashing, null, 3, List.of(worn(item("FANCY_TUXEDO_BOOTS"))));
        assertEquals(new Combat.HitBuff(100, 1), dashing.hit(null, fancy, true, new Damage.Target(1, 1, 0, 0, null, 0)));
        Stats health = new Stats().set(Stat.HEALTH, 900);
        dashing.derivedStats(null, fancy, health);
        assertEquals(150, health.get(Stat.HEALTH), EPSILON);
    }
}
