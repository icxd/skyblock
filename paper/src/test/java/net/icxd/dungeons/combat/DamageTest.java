package net.icxd.dungeons.combat;

import net.icxd.dungeons.mob.MobType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The damage formulas against the wiki's worked examples (Damage Calculation) and what the recordings and
 * the in-game stats menu show (research damage.md, section 5's test vectors).
 */
class DamageTest {
    /** Damage stat 95 and no Strength: an initial hit of exactly 100, as the wiki's examples start from. */
    private static Damage.Attacker hundred(Map<String, Integer> enchantments, int combatLevel, double multiplier) {
        return new Damage.Attacker(95, 0, 0, 0, combatLevel, 1000, enchantments, false, 0, multiplier);
    }

    private static Damage.Target target(double health, double maxHealth, double defense, MobType... types) {
        return new Damage.Target(health, maxHealth, defense, 0, Set.of(types), 0);
    }

    @Test
    void initialDamage() {
        assertEquals(100, Damage.initial(95, 0), 1e-9);
        // The stats menu: "Damage Multiplier: 6.8x" at 579.25 Strength, "9.3x" at 834.03% Crit Damage.
        assertEquals(6.7925, 1 + 579.25 / 100, 1e-9);
        assertEquals(6.7925 * 25, Damage.initial(20, 579.25), 1e-9);
        assertEquals(9.3403, Damage.critMultiplier(834.03), 1e-9);
    }

    /** Wiki: 100 additive from Prosecute VI on a full-health target, and 250 from a pet: x4.5, 100 -> 450. */
    @Test
    void additiveExample() {
        Damage.Attacker attacker = hundred(Map.of("prosecute", 6), 0, 1);
        Damage.Target full = target(5000, 5000, 0);
        assertEquals(100, Damage.additive(attacker, full), 1e-9);
        assertEquals(450, 100 * (1 + (Damage.additive(attacker, full) + 250) / 100), 1e-9);
        assertEquals(200, Damage.exact(attacker, full, false), 1e-9);
    }

    /** Wiki: x1.15 and x3.5 multiply to x4.025, 100 -> 402.5. */
    @Test
    void multiplicativeExample() {
        assertEquals(402.5, Damage.exact(hundred(Map.of(), 0, 1.15 * 3.5), target(1, 1, 0), false), 1e-9);
        assertEquals(402, Damage.hit(hundred(Map.of(), 0, 1.15 * 3.5), target(1, 1, 0), false));
    }

    /**
     * Wiki: the two combine, 100 x 4.5 x 4.025 = 1,811.25. Here the 350 additive is Prosecute VI (100 at
     * full health), Warrior at Combat 50 (200), Sharpness IV (20) and Smite V (30) on an undead.
     */
    @Test
    void combinedExample() {
        Damage.Attacker attacker = hundred(Map.of("prosecute", 6, "sharpness", 4, "smite", 5), 50, 4.025);
        Damage.Target undead = target(7000, 7000, 0, MobType.UNDEAD);
        assertEquals(350, Damage.additive(attacker, undead), 1e-9);
        assertEquals(1811.25, Damage.exact(attacker, undead, false), 1e-9);
        assertEquals(1811, Damage.hit(attacker, undead, false));
    }

    /** Wiki: the Lv40 Tank Zombie has 2,000 Defense, so a 1,000 hit does 1000 / 21 = 47.62 (47, rounded down). */
    @Test
    void tankZombieDefense() {
        Damage.Attacker thousand = new Damage.Attacker(995, 0, 0, 0, 0, 1000, Map.of(), false, 0, 1);
        assertEquals(47.62, Damage.exact(thousand, target(1200, 1200, 2000, MobType.UNDEAD), false), 0.005);
        assertEquals(47, Damage.hit(thousand, target(1200, 1200, 2000, MobType.UNDEAD), false));
        assertEquals(1 / 21.0, Damage.defenseMultiplier(2000), 1e-12);
    }

    /** Wiki: the Apex Dragon (cap 100k, Defense 2,500) hit for 600M: 60,090,000 after the cap, 2,311,153.85 after Defense. */
    @Test
    void damageCapBeforeDefense() {
        assertEquals(60_090_000, Damage.cap(600_000_000, 100_000), 1e-3);
        Damage.Attacker bow = new Damage.Attacker(600_000_000 - 5, 0, 0, 0, 0, 1000, Map.of(), true, 0, 1);
        Damage.Target apex = new Damage.Target(1e9, 1e9, 2500, 0, Set.of(), 0, 100_000);
        assertEquals(2_311_153.85, Damage.exact(bow, apex, false), 0.01);
        // Several caps: lowest first (Bladesoul's are 800,000 and 1,200,000).
        // 20M: 800,000 + 1,920,000 = 2,720,000 after the first, then 1,200,000 + 152,000.
        assertEquals(1_352_000, Damage.cap(20_000_000, 1_200_000, 800_000), 1e-6);
        assertEquals(920_000, Damage.cap(2_000_000, 1_200_000, 800_000), 1e-6);
        assertEquals(500, Damage.cap(500, 800_000), 1e-9);
    }

    /** The stats menu: 744.5 Defense is "Damage Reduction: 88.2%", 33 True Defense "24.8%". */
    @Test
    void playerDefense() {
        assertEquals(0.882, 1 - Damage.defenseMultiplier(744.5), 0.0005);
        assertEquals(0.248, 1 - Damage.defenseMultiplier(33), 0.0005);
        assertEquals(100 * 100 / 844.5, Damage.taken(100, 744.5), 1e-9);
        assertEquals(0, Damage.taken(-5, 0), 1e-9);
    }

    @Test
    void crits() {
        Damage.Attacker attacker = new Damage.Attacker(95, 0, 50, 100, 0, 1000, Map.of(), false, 0, 1);
        assertEquals(200, Damage.exact(attacker, target(1, 1, 0), true), 1e-9);
        assertTrue(Damage.crits(50, 0.49));
        assertFalse(Damage.crits(50, 0.5));
        assertTrue(Damage.crits(153, 0.999));
        assertFalse(Damage.crits(0, 0));
    }

    /** Combat skill: +4% a level to 50 (200%), +1% a level 51-60 (210%). */
    @Test
    void warrior() {
        assertEquals(0, Damage.warrior(0));
        assertEquals(100, Damage.warrior(25));
        assertEquals(200, Damage.warrior(50));
        assertEquals(205, Damage.warrior(55));
        assertEquals(210, Damage.warrior(60));
        assertEquals(210, Damage.warrior(99));
    }

    /** One For All adds 500% (x6 alone), where the plugin used to multiply by 5. */
    @Test
    void oneForAll() {
        assertEquals(600, Damage.exact(hundred(Map.of("one_for_all", 1), 0, 1), target(1, 1, 0), false), 1e-9);
    }

    @Test
    void enchantmentsByTarget() {
        Damage.Attacker attacker = hundred(Map.of(), 0, 1);
        Damage.Target zombie = target(7000, 7000, 0, MobType.UNDEAD);
        Damage.Target cube = target(7000, 7000, 0, MobType.CUBIC, MobType.INFERNAL);
        assertEquals(45, Damage.enchantment("sharpness", 6, attacker, zombie), 1e-9);
        assertEquals(65, Damage.enchantment("sharpness", 7, attacker, zombie), 1e-9);
        assertEquals(40, Damage.enchantment("smite", 6, attacker, zombie), 1e-9);
        assertEquals(0, Damage.enchantment("smite", 6, attacker, cube), 1e-9);
        assertEquals(30, Damage.enchantment("cubism", 5, attacker, cube), 1e-9);
        assertEquals(30, Damage.enchantment("smoldering", 5, attacker, cube), 1e-9);
        // Execute V: 1% for each percent missing; Prosecute V: 0.7% for each percent left.
        Damage.Target hurt = target(2500, 10000, 0);
        assertEquals(75, Damage.enchantment("execute", 5, attacker, hurt), 1e-9);
        assertEquals(17.5, Damage.enchantment("prosecute", 5, attacker, hurt), 1e-9);
        // Titan Killer VI: 16% per 100 Defense up to 60%.
        assertEquals(60, Damage.enchantment("titan_killer", 6, attacker, target(1, 1, 2000)), 1e-9);
        assertEquals(48, Damage.enchantment("titan_killer", 6, attacker, target(1, 1, 399)), 1e-9);
        assertEquals(0, Damage.enchantment("growth", 5, attacker, zombie), 1e-9);
    }

    /** Giant Killer VI: 0.9% per percent of health the target has above yours (1,000 here), up to 45%. */
    @Test
    void giantKiller() {
        Damage.Attacker attacker = hundred(Map.of(), 0, 1);
        assertEquals(4.5, Damage.enchantment("giant_killer", 6, attacker, target(1050, 1050, 0)), 1e-9);
        assertEquals(45, Damage.enchantment("giant_killer", 6, attacker, target(7000, 7000, 0)), 1e-9);
        assertEquals(0, Damage.enchantment("giant_killer", 6, attacker, target(900, 7000, 0)), 1e-9);
    }

    @Test
    void firstHits() {
        Damage.Attacker melee = hundred(Map.of(), 0, 1);
        Damage.Attacker arrow = new Damage.Attacker(95, 0, 0, 0, 0, 1000, Map.of(), true, 25, 1);
        assertEquals(100, Damage.enchantment("first_strike", 4, melee, new Damage.Target(1, 1, 0, 0, Set.of(), 0)), 1e-9);
        assertEquals(0, Damage.enchantment("first_strike", 4, melee, new Damage.Target(1, 1, 0, 0, Set.of(), 1)), 1e-9);
        assertEquals(50, Damage.enchantment("triple_strike", 5, melee, new Damage.Target(1, 1, 0, 0, Set.of(), 2)), 1e-9);
        assertEquals(0, Damage.enchantment("triple_strike", 5, melee, new Damage.Target(1, 1, 0, 0, Set.of(), 3)), 1e-9);
        // Melee enchantments don't count for arrows, bow ones only for arrows.
        assertEquals(0, Damage.enchantment("sharpness", 5, arrow, target(1, 1, 0)), 1e-9);
        assertEquals(65, Damage.enchantment("power", 7, arrow, target(1, 1, 0)), 1e-9);
        assertEquals(0, Damage.enchantment("power", 7, melee, target(1, 1, 0)), 1e-9);
        // Snipe IV: 4% per 10 blocks.
        assertEquals(10, Damage.enchantment("snipe", 4, arrow, target(1, 1, 0)), 1e-9);
    }

    /** Stats menu: 153.5 Health Regen at 2,206 max health is "Avg HP/s: +36.2". */
    @Test
    void healthRegen() {
        assertEquals(36.2, Damage.healthRegen(2206, 153.5), 0.05);
        assertEquals(2.5, Damage.healthRegen(100, 100), 1e-9);
    }

    /** Recorded per-second mana gains: 2% of the pool, rounded up. */
    @Test
    void manaRegen() {
        assertEquals(26, Damage.manaRegen(1264));
        assertEquals(27, Damage.manaRegen(1312));
        assertEquals(24, Damage.manaRegen(1156));
        assertEquals(25, Damage.manaRegen(1201));
        assertEquals(21, Damage.manaRegen(1046));
        assertEquals(23, Damage.manaRegen(1145));
        assertEquals(21, Damage.manaRegen(1030));
        assertEquals(19, Damage.manaRegen(940));
        assertEquals(2, Damage.manaRegen(100));
        // Each hit in a dungeon: 5 and 1% of the pool (14.4 at 940).
        assertEquals(14.4, Damage.manaOnHit(940), 1e-9);
    }

    /** round(10 / (1 + Attack Speed / 100)), Attack Speed capped at 100. */
    @Test
    void attackSpeed() {
        assertEquals(10, Damage.invulnerabilityTicks(0));
        assertEquals(10, Damage.invulnerabilityTicks(5));
        assertEquals(5, Damage.invulnerabilityTicks(82));
        assertEquals(5, Damage.invulnerabilityTicks(100));
        assertEquals(5, Damage.invulnerabilityTicks(400));
    }

    /**
     * Wiki, the Scarf example: a 7,000 ability hit on a target with 15% magic resistance, a 6,000 cap and no
     * Defense is 5,950 after the resistance and still under the cap. (The example's last steps, the Scarf's
     * own reduction and Extra Infliction, are special mechanics none of this plugin's mobs have: 5,950 x 0.28
     * x 1.037 = 1,727.64.)
     */
    @Test
    void magicDamage() {
        Damage.Target scarf = new Damage.Target(1e6, 1e6, 0, 0.15, Set.of(), 0, 6000);
        assertEquals(5950, Damage.exactMagic(7000, 0, 1, scarf), 1e-9);
        // Resistance comes before the cap: 10,000 -> 8,500 -> 6,250, not 10,000 -> 6,400 -> 5,440.
        assertEquals(6250, Damage.exactMagic(10_000, 0, 1, scarf), 1e-9);
        // Then Defense: the Lv40 Tank Zombie's 2,000, and a Crypt Lurker's 10% resistance.
        assertEquals(900 / 21.0, Damage.exactMagic(1000, 0, 1, new Damage.Target(1, 1, 2000, 0.1, Set.of(), 0)), 1e-9);
        // Buffs first: +100% additive and x1.5.
        assertEquals(3000, Damage.exactMagic(1000, 100, 1.5, target(1, 1, 0)), 1e-9);
    }

    /**
     * Ability damage: base x (1 + Intelligence / 100 x scaling) x (1 + Ability Damage / 100). The stats menu
     * shows 17.5 Ability Damage as "Damage Multiplier: 1.2x" (1.175) and 692 Intelligence as "Magic Damage:
     * +692%" (scaling 1).
     */
    @Test
    void abilityDamage() {
        assertEquals(1.175, Damage.initialAbility(1, 0, 0, 17.5), 1e-9);
        assertEquals(7.92, Damage.initialAbility(1, 1, 692, 0), 1e-9);
        // Giant's Slam: 100,000 base, 0.05 scaling; 1,000 Intelligence makes it 150,000.
        assertEquals(150_000, Damage.initialAbility(100_000, 0.05, 1000, 0), 1e-6);
    }

    /** "This ability is on cooldown for 17s." with 16.9 seconds left. */
    @Test
    void cooldownSeconds() {
        assertEquals(17, Damage.cooldownSeconds(16_900));
        assertEquals(10, Damage.cooldownSeconds(10_000));
        assertEquals(1, Damage.cooldownSeconds(1));
    }
}
