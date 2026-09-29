package net.icxd.dungeons.economy;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.enchanting.EnchantmentType;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.SkillRewards;
import net.icxd.dungeons.skill.Skills;

/**
 * What raises the experience orbs a player gets ({@link ExpOrbs#addBonus}; they add up, the wiki's Experience):
 * <ul>
 *   <li>The Experience enchantment on what they hold: "Grants a 12.5% chance for mobs and ores to drop double
 *   experience" at I (its text's percent), +100% when it rolls, on a kill's or a mined ore's.</li>
 *   <li>Conjurer, the Enchanting skill's perk: "Gain 5% more experience orbs from any source" a level
 *   ({@link SkillRewards#perkValue}; the wiki's Experience: "+5% XP per level, up to a maximum of +300% at level
 *   60"). Bottles of experience already count it (the Hex's).</li>
 * </ul>
 * Main thread.
 */
public final class ExpBonuses implements Listener {
    public ExpBonuses() {
        ExpOrbs.addBonus((player, source) -> experience(player, source));
        ExpOrbs.addBonus((player, source) -> conjurer(player));
    }

    /** The Experience enchantment's: +100 when its chance rolls on a mob's or an ore's experience, else 0. */
    static double experience(Player player, ExpOrbs.Source source) {
        if (source == ExpOrbs.Source.OTHER) return 0;
        int level = Combat.heldEnchantments(player).getOrDefault("experience", 0);
        if (level <= 0) return 0;
        EnchantmentType type = EnchantmentType.getByNamespace("experience");
        return type == null ? 0 : doubled(type.percent(level), ThreadLocalRandom.current().nextDouble());
    }

    /** +100 (double) when a {@code chance} percent roll comes up, with {@code roll} (0 to 1) as the random; else 0. */
    static double doubled(double chance, double roll) {
        return roll < chance / 100 ? 100 : 0;
    }

    /** Conjurer's, at their Enchanting level: 5% a level. */
    static double conjurer(Player player) {
        return SkillRewards.perkValue(Skill.ENCHANTING, Skills.level(player, Skill.ENCHANTING));
    }
}
