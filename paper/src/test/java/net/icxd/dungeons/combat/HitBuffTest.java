package net.icxd.dungeons.combat;

import net.icxd.dungeons.mob.MobType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Buffs known only once a hit lands (armor bonuses'): the additive ones join the hit's additive sum. */
class HitBuffTest {
    private static final Damage.Target UNDEAD = new Damage.Target(1000, 1000, 0, 0, Set.of(MobType.UNDEAD), 0);

    /** Warrior at Combat 50 (200) and Smite V (30) on an undead, x3.3; +100 more additive is x4.3, not x3.3 x2. */
    @Test
    void additiveJoinsTheSum() {
        Damage.Attacker attacker = new Damage.Attacker(95, 0, 0, 0, 50, 1000, Map.of("smite", 5), false, 0, 1);
        assertEquals(330, Damage.exact(attacker, UNDEAD, false), 1e-9);
        assertEquals(430, Damage.exact(Damage.buffed(attacker, UNDEAD, 100, 1), UNDEAD, false), 1e-9);
    }

    /** A factor multiplies with the rest (Reaper Armor's 1% against other mobs), and on a crit too. */
    @Test
    void multiplierMultiplies() {
        Damage.Attacker attacker = new Damage.Attacker(95, 0, 0, 100, 0, 1000, Map.of(), false, 0, 2);
        assertEquals(400, Damage.exact(attacker, UNDEAD, true), 1e-9);
        assertEquals(4, Damage.exact(Damage.buffed(attacker, UNDEAD, 0, 0.01), UNDEAD, true), 1e-9);
        assertEquals(1000, Damage.exact(Damage.buffed(attacker, UNDEAD, 25, 2), UNDEAD, true), 1e-9);
    }

    /**
     * "Add Damage" (Soul Eater): added after the additive and multiplicative buffs, and only the crit multiplies
     * it: (100 x 2 + 50) x 2 on a crit with 100 Crit Damage, then Defense.
     */
    @Test
    void addedDamage() {
        Damage.Attacker attacker = new Damage.Attacker(95, 0, 0, 100, 25, 1000, Map.of(), false, 0, 2);
        Damage.Target none = new Damage.Target(1000, 1000, 0, 0, Set.of(), 0);
        assertEquals(100 * 2 * 2, Damage.exact(attacker, none, false), 1e-9);
        assertEquals(100 * 2 * 2 + 50, Damage.exact(attacker, none, false, 50), 1e-9);
        assertEquals((100 * 2 * 2 + 50) * 2, Damage.exact(attacker, none, true, 50), 1e-9);
        Damage.Target defended = new Damage.Target(1000, 1000, 100, 0, Set.of(), 0);
        assertEquals((100 * 2 * 2 + 50) * 2 / 2.0, Damage.exact(attacker, defended, true, 50), 1e-9);
        assertEquals(Damage.exact(attacker, none, true), Damage.exact(attacker, none, true, 0), 1e-9);
        assertEquals(0, new Combat.HitBuff(10, 2).added(), 1e-9);
    }

    @Test
    void nothingMore() {
        Damage.Attacker attacker = new Damage.Attacker(95, 0, 0, 0, 0, 1000, Map.of(), true, 0, 1);
        assertSame(attacker, Damage.buffed(attacker, UNDEAD, 0, 1));
    }
}
