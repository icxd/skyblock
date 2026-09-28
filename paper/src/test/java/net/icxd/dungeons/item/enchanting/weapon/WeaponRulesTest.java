package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.combat.Damage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The weapon enchantments' sums, with the wiki's worked examples where it has them. */
class WeaponRulesTest {
    private static final Damage.Target DUMMY = new Damage.Target(1_000_000, 1_000_000, 0, 0, null, 0);

    /** Initial damage (5 + 395) x (1 + 400 / 100) = 2,000, and Warrior LX's +210 additive: the wiki's example 4. */
    private static Damage.Attacker attacker(Map<String, Integer> enchantments) {
        return new Damage.Attacker(395, 400, 0, 0, 60, 100, enchantments, false, 0, 1);
    }

    @Test
    void damageAfterAdditiveBuffs() {
        assertEquals(6200, WeaponRules.postAdditive(attacker(Map.of()), DUMMY, false), 1e-9);
        // Sharpness V's +30: 2,000 x 3.4.
        assertEquals(6800, WeaponRules.postAdditive(attacker(Map.of("sharpness", 5)), DUMMY, false), 1e-9);
        // A crit's multiplier is in it: +100% Crit Damage doubles it.
        Damage.Attacker crits = new Damage.Attacker(395, 400, 100, 100, 60, 100, Map.of(), false, 0, 1);
        assertEquals(12400, WeaponRules.postAdditive(crits, DUMMY, true), 1e-9);
    }

    /** The wiki's example 4: Fire Aspect II's 0.06 of 6,200 is 372; Venomous III's 41 stacks of 0.006 then x 3.1 again. */
    @Test
    void theWikisDamagesOverTime() {
        assertEquals(372, WeaponRules.fireTick(6200, 6, 1, 0), 1e-9);
        assertEquals(4728.12, WeaponRules.venomTick(6200 * 0.006 * 41, 210, 0), 1e-6);
        // With Sharpness V the hit's damage is 6,800, but the second multiplier leaves it out (Sharpness is melee's).
        Damage.Attacker sharp = attacker(Map.of("sharpness", 5));
        double second = Damage.additive(WeaponRules.notMelee(sharp), DUMMY);
        assertEquals(210, second, 1e-9);
        // The wiki's 126.48 x 41 (it prints 5,158.68, a slip for 5,185.68).
        assertEquals(126.48 * 41, WeaponRules.venomTick(6800 * 0.006 * 41, second, 0), 1e-6);
    }

    @Test
    void fireThroughDefenseAndDuplex() {
        // 100 Defense halves it; Duplex's 1.5x fire damage taken on top.
        assertEquals(186, WeaponRules.fireTick(6200, 6, 1, 100), 1e-9);
        assertEquals(279, WeaponRules.fireTick(6200, 6, 1.5, 100), 1e-9);
        assertEquals(0, WeaponRules.fireTick(-5, 6, 1, 0));
    }

    /** Stacks save each hit's share up to the most, and a hit after they've run out starts again. */
    @Test
    void venomStacks() {
        WeaponRules.Venom venom = new WeaponRules.Venom();
        for (int i = 0; i < 45; i++) venom.hit(1000, 0.002, 40, 5000, 1000 + i);
        assertEquals(40, venom.stacks());
        assertEquals(80, venom.saved(), 1e-9);
        // "Venomous damage cannot be increased further": a bigger hit past the most saves nothing.
        venom.hit(1_000_000, 0.02, 40, 5000, 2000);
        assertEquals(80, venom.saved(), 1e-9);
        // Once it has run out (5 s after the last hit), it starts again.
        venom.hit(1000, 0.002, 40, 5000, 7000);
        assertEquals(1, venom.stacks());
        assertEquals(2, venom.saved(), 1e-9);
    }

    @Test
    void fatalTempo() {
        // +10% a hit, capped at 200%, from nothing again after 3 s without a hit.
        double tempo = 0;
        long at = 1000;
        for (int i = 0; i < 25; i++) tempo = WeaponRules.tempo(tempo, at, at += 100, 10, 200, 3000);
        assertEquals(200, tempo);
        assertEquals(10, WeaponRules.tempo(200, at, at + 3001, 10, 200, 3000));
        assertEquals(200, WeaponRules.tempo(200, at, at + 3000, 10, 200, 3000));
        assertEquals(300, WeaponRules.withTempo(100, 200), 1e-9);
        assertEquals(100, WeaponRules.withTempo(100, 0), 1e-9);
    }

    @Test
    void combo() {
        long now = 10_000;
        // Combo V: +5% a kill, up to 10 kills within 10 s; kills older than that don't count, nor unused slots.
        long[] kills = {now - 1000, now - 2000, now - 11_000, 0, 0};
        assertEquals(10, WeaponRules.combo(kills, now, 5, 10, 10_000), 1e-9);
        // Combo I: up to 2 kills within 2 s.
        long[] many = {now, now - 100, now - 200, now - 300};
        assertEquals(2, WeaponRules.combo(many, now, 1, 2, 2000), 1e-9);
    }

    @Test
    void swarmAndSoul() {
        assertEquals(20, WeaponRules.swarm(2, 10, 10), 1e-9);
        assertEquals(100, WeaponRules.swarm(40, 10, 10), 1e-9);
        assertEquals(0, WeaponRules.swarm(-1, 10, 10), 1e-9);
        // 10x a 150,000 Damage mob is capped at 1M outside a dungeon, not in one.
        assertEquals(1_000_000, WeaponRules.soul(10, 150_000, 1_000_000, false), 1e-9);
        assertEquals(1_500_000, WeaponRules.soul(10, 150_000, 1_000_000, true), 1e-9);
        assertEquals(400, WeaponRules.soul(2, 200, 1_000_000, false), 1e-9);
    }

    @Test
    void tiersUpAsTheCountReaches() {
        Map<Integer, Double> thresholds = Map.of(1, 50_000.0, 2, 100_000.0, 3, 250_000.0);
        assertEquals(1, WeaponRules.tier(1, 49_999, at -> thresholds.getOrDefault(at, 0.0)));
        assertEquals(2, WeaponRules.tier(1, 50_000, at -> thresholds.getOrDefault(at, 0.0)));
        // Several at once, and never past the last.
        assertEquals(4, WeaponRules.tier(1, 5_000_000, at -> thresholds.getOrDefault(at, 0.0)));
        assertEquals(3, WeaponRules.tier(3, 100_000, at -> thresholds.getOrDefault(at, 0.0)));
    }

    @Test
    void everyNth() {
        assertFalse(WeaponRules.every(1, 3));
        assertFalse(WeaponRules.every(2, 3));
        assertTrue(WeaponRules.every(3, 3));
        assertTrue(WeaponRules.every(6, 3));
        assertFalse(WeaponRules.every(0, 3));
        assertFalse(WeaponRules.every(3, 0));
    }

    @Test
    void knockbackAndHeads() {
        // 3 blocks a level: vanilla Knockback's 0.5 a level on a melee hit, Punch's 0.6 on an arrow.
        assertEquals(0.5, WeaponRules.knockback(3, false), 1e-9);
        assertEquals(1.2, WeaponRules.knockback(6, true), 1e-9);
        assertEquals(0, WeaponRules.knockback(-3, false));
        // A zombie (1.95 tall, eyes at 1.74) standing at y 64: its head is from about 65.53.
        assertTrue(WeaponRules.headshot(65.8, 64, 1.95, 1.74));
        assertTrue(WeaponRules.headshot(65.54, 64, 1.95, 1.74));
        assertFalse(WeaponRules.headshot(65, 64, 1.95, 1.74));
    }
}
