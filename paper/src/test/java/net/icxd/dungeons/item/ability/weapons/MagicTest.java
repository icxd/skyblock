package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.mob.MobType;

/** Abilities' magic damage against the wiki's numbers. */
class MagicTest {
    private static final Damage.Target DUMMY = new Damage.Target(1_000_000, 1_000_000, 0, 0, Set.of(), 0);
    private static final Magic.Caster NOTHING = new Magic.Caster(0, 0, 0);

    /** With no Intelligence or Ability Damage it's the base damage, as the item's text says ("dealing 100,000 damage"). */
    @Test
    void baseDamage() {
        assertEquals(100_000, Magic.damage(GiantsSlam.SLAM, 1, NOTHING, DUMMY), 1e-6);
        assertEquals(10_000, Magic.damage(WitherBlade.IMPLOSION, 1, NOTHING, DUMMY), 1e-6);
    }

    /** "10000 x (100% + 400%) = 50000": the base in the Catacombs with a +400% stat boost (the wiki's Damage Calculation). */
    @Test
    void catacombsBoostGrowsTheBase() {
        assertEquals(50_000, Magic.damage(WitherBlade.IMPLOSION, 5, NOTHING, DUMMY), 1e-6);
    }

    /**
     * Base x (1 + Intelligence / 100 x scaling) x (1 + Ability Damage / 100): the recorded stats menu's 692
     * Intelligence and 17.5 Ability Damage on a Giant's Slam, 100,000 x 1.346 x 1.175, rounded down as its
     * hits are; the same on Implosion (scaling 0.3) isn't.
     */
    @Test
    void intelligenceAndAbilityDamage() {
        Magic.Caster caster = new Magic.Caster(692, 17.5, 0);
        assertEquals(158_155, Magic.damage(GiantsSlam.SLAM, 1, caster, DUMMY), 1e-6);
        assertEquals(10_000 * (1 + 6.92 * 0.3) * 1.175, Magic.damage(WitherBlade.IMPLOSION, 1, caster, DUMMY), 1e-6);
    }

    /**
     * The additive buffs, then the target's magic resistance and Defense: 10,000 x 1.5 (+50%) x 0.9 (10%
     * resisted, a Catacombs melee mob's) x 100 / 200 (100 Defense). Dragon Rage isn't "affected by the Additive Multiplier".
     */
    @Test
    void additiveResistanceDefense() {
        Damage.Target zombie = new Damage.Target(1_000_000, 1_000_000, 100, 0.1, Set.of(MobType.UNDEAD), 0);
        Magic.Caster caster = new Magic.Caster(0, 0, 50);
        assertEquals(6_750, Magic.damage(WitherBlade.IMPLOSION, 1, caster, zombie), 1e-6);
        Magic.Spell noAdditive = new Magic.Spell(10_000, 0.1, false, false);
        assertEquals(4_500, Magic.damage(noAdditive, 1, caster, zombie), 1e-6);
    }

    /** Sharpness, First Strike, Triple-Strike and One For All don't count for abilities; Smite and Giant Killer do. */
    @Test
    void enchantmentsForAbilities() {
        Map<String, Integer> kept = Magic.forAbilities(Map.of("sharpness", 7, "first_strike", 5, "one_for_all", 1, "smite", 7, "giant_killer", 7));
        assertEquals(Map.of("smite", 7, "giant_killer", 7), kept);
        Damage.Target zombie = new Damage.Target(1_000, 1_000, 0, 0, Set.of(MobType.UNDEAD), 0);
        // Smite VII's 50% and Combat 50's Warrior 200%.
        assertEquals(250, Magic.additive(50, 100, Map.of("sharpness", 7, "smite", 7), zombie), 1e-6);
    }
}
