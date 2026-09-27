package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.mob.MobType;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static net.icxd.dungeons.item.bonus.TestPieces.item;
import static net.icxd.dungeons.item.bonus.TestPieces.worn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The slayer sets in numbers: Radioactive and Absolute Unit. */
class SlayerSetsTest {
    private static final double EPSILON = 1e-9;

    /** Radioactive: +1 Crit Damage per 10 Strength (the Primordial's +1.5), up to 1,000 Strength. */
    @Test
    void radioactive() {
        assertEquals(45, SlayerSets.Radioactive.critDamage(459, 1), EPSILON);
        assertEquals(100, SlayerSets.Radioactive.critDamage(2500, 1), EPSILON);
        assertEquals(150, SlayerSets.Radioactive.critDamage(1000, 1.5), EPSILON);
    }

    /** Absolute Unit: Defense capped at 300, then +1 Crit Damage per 2 of it, then +1% Health per 25 Crit Damage (at most 40%). */
    @Test
    void absoluteUnit() {
        Stats stats = new Stats().set(Stat.DEFENSE, 500).set(Stat.CRIT_DAMAGE, 250).set(Stat.HEALTH, 1000);
        SlayerSets.AbsoluteUnit.apply(stats);
        assertEquals(300, stats.get(Stat.DEFENSE), EPSILON);
        assertEquals(400, stats.get(Stat.CRIT_DAMAGE), EPSILON);
        assertEquals(1160, stats.get(Stat.HEALTH), EPSILON);
        Stats capped = new Stats().set(Stat.DEFENSE, 100).set(Stat.CRIT_DAMAGE, 2000).set(Stat.HEALTH, 1000);
        SlayerSets.AbsoluteUnit.apply(capped);
        assertEquals(1400, capped.get(Stat.HEALTH), EPSILON);
    }

    /** Reaper Armor: +100 additive against the Undead, 1% against the rest; Revenant Armor's pieces don't deal it. */
    @Test
    void trollingTheReaper() {
        Bonus bonus = new SlayerSets.TrollingTheReaper();
        List<Worn.Piece> reaper = List.of(worn(item("REAPER_CHESTPLATE")), worn(item("REAPER_LEGGINGS")), worn(item("REAPER_BOOTS")));
        Bonus.Active worn = new Bonus.Active(bonus, null, 3, reaper);
        Damage.Target undead = new Damage.Target(100, 100, 0, 0, Set.of(MobType.UNDEAD), 0);
        Damage.Target other = new Damage.Target(100, 100, 0, 0, Set.of(MobType.HUMANOID), 0);
        assertEquals(new Combat.HitBuff(100, 1), bonus.hit(null, worn, false, undead));
        assertEquals(new Combat.HitBuff(0, 0.01), bonus.hit(null, worn, true, other));
        List<Worn.Piece> mixed = List.of(worn(item("REVENANT_CHESTPLATE")), worn(item("REAPER_LEGGINGS")), worn(item("REAPER_BOOTS")));
        assertNull(bonus.hit(null, new Bonus.Active(bonus, null, 3, mixed), false, other));
    }
}
