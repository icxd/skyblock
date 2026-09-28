package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.icxd.dungeons.combat.Damage;

/**
 * Magic damage, an ability's, with no Bukkit in it: its base damage (a dungeon item's grown in a run by
 * the Catacombs stat bonus: "10000 x (100% + 400%) = 50000", the wiki's Damage Calculation), times what the
 * caster's Intelligence and Ability Damage make of it ({@link Damage#initialAbility}), then the additive
 * buffs that count for abilities, then the target's magic resistance, caps and Defense
 * ({@link Damage#exactMagic}). Never a crit, and no Ferocity. Public for effects elsewhere (EFFECTS.md).
 */
public final class Magic {
    /**
     * Enchantments that don't count for abilities: "Enchantments that do not affect abilities, such as
     * Sharpness" (the wiki's Bonzo's Staff and Flower of Truth), and melee's First Strike and Triple-Strike
     * with it; One For All too, which "does not buff" the Flower of Truth's (for magic damage UNKNOWN).
     */
    public static final Set<String> NOT_FOR_ABILITIES = Set.of("sharpness", "first_strike", "triple_strike", "one_for_all");

    /**
     * How an ability's damage is worked out: its base damage and Intelligence scaling (the wiki's Damage
     * Calculation/Damage Abilities), whether the additive buffs count ("Not affected by the Additive
     * Multiplier": Dragon Rage, Burning Souls) and whether each hit is rounded down (Giant's Slam's recorded
     * totals are whole, Implosion's aren't: "Your Implosion hit 1 enemy for 636,116.8 damage.").
     */
    public record Spell(double base, double scaling, boolean additive, boolean floored) {
        Spell(double base, double scaling) {
            this(base, scaling, true, false);
        }

        Spell withBase(double base) {
            return new Spell(base, scaling, additive, floored);
        }
    }

    /** What the caster brings: Intelligence, Ability Damage, and the additive buffs on this target (in percent). */
    public record Caster(double intelligence, double abilityDamage, double additive) {
    }

    private Magic() {
    }

    /**
     * What the spell does to the target, {@code dungeonFactor} being what its base is multiplied by (1
     * outside a run, or for an item that isn't a dungeon item; in a run 1 + 10% a star + the Catacombs
     * boost, as the item's own stats are: ItemBuilder#dungeonFactor). The multiplicative buffs are 1: which of
     * them count for magic damage is UNKNOWN (the classes' are for melee and arrows).
     */
    public static double damage(Spell spell, double dungeonFactor, Caster caster, Damage.Target target) {
        double initial = Damage.initialAbility(spell.base() * dungeonFactor, spell.scaling(), caster.intelligence(), caster.abilityDamage());
        double damage = Damage.exactMagic(initial, spell.additive() ? caster.additive() : 0, 1, target);
        return spell.floored() ? Math.floor(damage) : damage;
    }

    /** The weapon's enchantments that count for an ability's hit. */
    public static Map<String, Integer> forAbilities(Map<String, Integer> enchantments) {
        Map<String, Integer> kept = new HashMap<>(enchantments);
        kept.keySet().removeAll(NOT_FOR_ABILITIES);
        return kept;
    }

    /**
     * The additive buffs on an ability's hit, in percent: the Combat skill's Warrior bonus (whether it counts
     * for magic is UNKNOWN; the wiki lists it as a plain additive source) and the enchantments that count.
     */
    public static double additive(int combatLevel, double health, Map<String, Integer> enchantments, Damage.Target target) {
        Damage.Attacker attacker = new Damage.Attacker(0, 0, 0, 0, combatLevel, health, forAbilities(enchantments), false, 0, 1);
        return Damage.additive(attacker, target);
    }
}
