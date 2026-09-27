package net.icxd.dungeons.skill;

import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What each skill level gives, as the menus list it and as stats: its perk (Warrior, Farmhand, ...),
 * its stat bonus, what it unlocks, its coins and SkyBlock XP (research skills.md 1.2, 1.3, 4.1, 4.2).
 * Combat's lines are all 60 recorded items'; the other skills' follow the one recorded next level of
 * each (their per-level values are the wiki's, which every recorded sample matches), with the unlock
 * lines only where a recording shows them (other levels' unlocks are UNKNOWN, so they have none).
 */
public final class SkillRewards {
    /** Lines shown under a level's rewards (under the perk's name, its description is indented two more). */
    private static final String SUB = "  ";

    private static final Map<Integer, List<String>> COMBAT_BEFORE = Map.of(
            1, List.of("&8+&aAccess to &cSpider's Den"),
            12, List.of("&8+&aAccess to &dThe End"),
            22, List.of("&8+&aAccess to &cCrimson Isle"));
    private static final Map<Integer, List<String>> COMBAT_AFTER = Map.of(
            10, List.of("&aDisplaced Leech &7Power Stone"),
            15, List.of("&7Intermediate Accessory Bag Powers", "&aLuxurious Spool &7Power Stone", "&aRock Candy &7Power Stone",
                    "&9Ender Monocle &7Power Stone"),
            20, List.of("&8+&aAccess to &5Zealot Bruiser Hideout", "&9Acacia Birdhouse &7Power Stone", "&9Sunflower Butter &7Power Stone",
                    "&6Glacite Chunk &7Power Stone", "&9Furball &7Power Stone", "&9Obsidian Tablet &7Power Stone",
                    "&9End Stone Shulker &7Power Stone", "&9Beating Heart &7Power Stone", "&9Dark Orb &7Power Stone",
                    "&6Fang-tastic Chocolate Chip &7Power", "&7Stone"),
            25, List.of("&5Horns of Torment &7Power Stone", "&6Bubba Blister &7Power Stone", "&6Eccentric Painting &7Power Stone",
                    "&5Magma Urchin &7Power Stone", "&5Precious Pearl &7Power Stone"),
            30, List.of("&9Mandraa &7Power Stone", "&6Hazmat Enderman &7Power Stone", "&6Vitamin Death &7Power Stone"),
            35, List.of("&dScorched Books &7Power Stone"));

    /**
     * Fishing XVIII's two sea creatures, recorded before its perk and with their names hidden as "???"
     * (REC3 "Your Skills" slot 21). The plugin has no sea creatures, so the hidden names are kept as
     * recorded. The other levels' sea creatures are UNKNOWN, so they have none.
     */
    private static final Map<Integer, List<String>> FISHING_BEFORE = Map.of(
            18, List.of("&5???&3 Sea Creature", "&5???&3 Sea Creature"));

    private SkillRewards() {
    }

    /**
     * A level's reward lines, unindented (a perk's description two spaces in): e.g. Combat XXV's
     * "&eWarrior XXV", "  &fDeal &896➜&a100%&f more damage to mobs.", "&8+&a0.5% &9☣ Crit Chance",
     * five power stones, "&8+&6225,000 &7Coins", "&8+&b10 SkyBlock XP".
     */
    public static List<String> lines(Skill skill, int level) {
        List<String> lines = new ArrayList<>();
        if (skill == Skill.COMBAT) lines.addAll(COMBAT_BEFORE.getOrDefault(level, List.of()));
        if (skill == Skill.FISHING) lines.addAll(FISHING_BEFORE.getOrDefault(level, List.of()));
        String perk = perkName(skill);
        if (perk != null) {
            lines.add("&e" + perk + " " + Utils.getRomanNumeral(level));
            for (String line : perkDescription(skill, level, true)) lines.add(SUB + line);
        }
        if (skill == Skill.TAMING) {
            // Exp Share: 6➜6.2 at XXXI is the one recorded value; 0.2 a level (6 at 30) is assumed for the rest (UNKNOWN).
            lines.add("&8+&fGain &8" + Text.number(0.2 * (level - 1)) + "➜&a" + Text.number(0.2 * level) + "% &fExp Share rate.");
        }
        lines.addAll(statLines(skill, level));
        lines.addAll(unlocks(skill, level));
        int coins = skill.coins(level);
        if (coins > 0) lines.add("&8+&6" + Text.number(coins) + " &7Coins");
        int skyBlockXp = skill.skyBlockXp(level);
        if (skyBlockXp > 0) lines.add("&8+&b" + skyBlockXp + " SkyBlock XP");
        return lines;
    }

    /** "Warrior": the perk every level of the skill raises; null for none (Carpentry, Runecrafting, Social). */
    public static String perkName(Skill skill) {
        return switch (skill) {
            case COMBAT -> "Warrior";
            case FARMING -> "Farmhand";
            case FISHING -> "Treasure Hunter";
            case MINING -> "Spelunker";
            case FORAGING -> "Logger";
            case ENCHANTING -> "Conjurer";
            case ALCHEMY -> "Brewer";
            case TAMING -> "Zoologist";
            case HUNTING -> "Charming";
            case CARPENTRY, RUNECRAFTING, SOCIAL -> null;
        };
    }

    /** What the perk is worth at this level: 100 for Warrior XXV (percent), 88 for Farmhand XXII (Farming Fortune). */
    public static double perkValue(Skill skill, int level) {
        return switch (skill) {
            case COMBAT -> Damage.warrior(level);
            case FARMING, MINING, FORAGING -> 4 * level;
            case FISHING -> 0.1 * level;
            case ENCHANTING -> 5 * level;
            case ALCHEMY, TAMING -> level;
            case HUNTING -> 0.04 * level;
            case CARPENTRY, RUNECRAFTING, SOCIAL -> 0;
        };
    }

    /**
     * The perk's description at this level: with the change from the level before ("&896➜&a100%"), as
     * the level items have it (not at level I: "&a4%"), or only what it is now, as the skill's own item
     * has it ("&a96%"). The recordings only show Combat's without the change; the others' are made the
     * same way.
     */
    public static List<String> perkDescription(Skill skill, int level, boolean change) {
        String value = value(perkValue(skill, level - 1), perkValue(skill, level), change && level > 1, "");
        String percent = value(perkValue(skill, level - 1), perkValue(skill, level), change && level > 1, "%");
        String grants = "&fGrants &a+" + value.replaceFirst("^&a", "") + "&f ";
        return switch (skill) {
            case COMBAT -> List.of("&fDeal " + percent + "&f more damage to mobs.");
            case FARMING -> List.of(grants + "&6☘ Farming Fortune&f,", "&fwhich increases your chance for", "&fmultiple crops.");
            case FISHING -> List.of(grants + "&6⛃ Treasure Chance&f.");
            case MINING -> List.of(grants + "&6☘ Mining Fortune&f,", "&fwhich increases your chance for", "&fmultiple ore drops.");
            case FORAGING -> List.of(grants + "&6☘ Foraging Fortune&f,", "&fwhich increases your chance for", "&fmultiple logs.");
            case ENCHANTING -> List.of("&fGain " + percent + "&f more experience", "&forbs from any source.");
            case ALCHEMY -> List.of("&fPotions that you brew have a", percent + "&f longer duration.");
            case TAMING -> List.of("&fGain " + percent + "&f extra pet exp.");
            case HUNTING -> List.of(grants + "&b❣ Charm Chance&f.");
            case CARPENTRY, RUNECRAFTING, SOCIAL -> List.of();
        };
    }

    /** "&896➜&a100%" with the change, "&a100%" without. */
    private static String value(double before, double now, boolean change, String unit) {
        return (change ? "&8" + Text.number(before) + "➜" : "") + "&a" + Text.number(now) + unit;
    }

    /** The level's stat bonus lines ("&8+&a4 &c❤ Health"). */
    static List<String> statLines(Skill skill, int level) {
        return switch (skill) {
            case COMBAT -> List.of("&8+&a0.5% &9☣ Crit Chance");
            case FARMING, FISHING -> List.of("&8+&a" + healthStep(level) + " &c❤ Health");
            // Recorded with no colour before the symbol.
            case MINING -> List.of("&8+&a" + early(level) + " ❈ Defense");
            case FORAGING -> List.of("&8+&a" + early(level) + " &c❁ Strength");
            case ENCHANTING -> List.of("&8+&a0.5% &c๑ Ability Damage", "&8+&a" + early(level) + " &b✎ Intelligence");
            case ALCHEMY -> List.of("&8+&a" + early(level) + " &b✎ Intelligence");
            case CARPENTRY -> List.of("&8+&a1 &c❤ Health");
            case TAMING -> List.of("&8+&a1 &d♣ Pet Luck");
            case HUNTING -> List.of("&8+&a1 &d☘ Hunting Fortune");
            case RUNECRAFTING, SOCIAL -> List.of();
        };
    }

    /** What else the level unlocks, where a recording shows it. */
    private static List<String> unlocks(Skill skill, int level) {
        return switch (skill) {
            case COMBAT -> COMBAT_AFTER.getOrDefault(level, List.of());
            case MINING -> level == 30 ? List.of("&5Amber Material &7Reforge", "&5Wither Blood &7Reforge", "&5Precursor Gear &7Reforge",
                    "&9Mangrove Gem &7Reforge", "&5Golden Ball &7Reforge", "&6Frigid Husk &7Reforge", "&9Jaderald &7Reforge",
                    "&5Daedalus' Notes &7Reforge") : List.of();
            case ENCHANTING -> level == 36 ? List.of("&d&lSoul Eater Enchantment") : List.of();
            case CARPENTRY -> level == 18 ? List.of("&fMedium Shelves &7Furniture Recipe") : List.of();
            case SOCIAL -> level == 2 ? List.of("&fAbility to purchase &dSocial Display", "&ffrom Amelia") : List.of();
            // Recorded for XI; the level's colour for the others is assumed the same.
            case RUNECRAFTING -> List.of("&7Access to Level &5" + level + " &7Runes");
            default -> List.of();
        };
    }

    /** Farming and Fishing Health a level: 2 to XIV, 3 to XIX, 4 to XXV, then 5. */
    private static int healthStep(int level) {
        if (level < 15) return 2;
        if (level < 20) return 3;
        if (level < 26) return 4;
        return 5;
    }

    /** Mining Defense, Foraging Strength, Enchanting and Alchemy Intelligence a level: 1 to XIV, then 2. */
    private static int early(int level) {
        return level < 15 ? 1 : 2;
    }

    /** What reaching this level (and all before it) adds to a player's stats. */
    public static Stats stats(Skill skill, int level) {
        Stats stats = new Stats();
        if (level < 1) return stats;
        int health = 0, early = 0;
        for (int l = 1; l <= level; l++) {
            health += healthStep(l);
            early += early(l);
        }
        switch (skill) {
            case COMBAT -> stats.add(Stat.CRIT_CHANCE, 0.5 * level);
            case FARMING -> stats.add(Stat.HEALTH, health).add(Stat.FARMING_FORTUNE, perkValue(skill, level));
            case FISHING -> stats.add(Stat.HEALTH, health).add(Stat.TREASURE_CHANCE, perkValue(skill, level));
            case MINING -> stats.add(Stat.DEFENSE, early).add(Stat.MINING_FORTUNE, perkValue(skill, level));
            case FORAGING -> stats.add(Stat.STRENGTH, early).add(Stat.FORAGING_FORTUNE, perkValue(skill, level));
            case ENCHANTING -> stats.add(Stat.ABILITY_DAMAGE, 0.5 * level).add(Stat.INTELLIGENCE, early);
            case ALCHEMY -> stats.add(Stat.INTELLIGENCE, early);
            case CARPENTRY -> stats.add(Stat.HEALTH, level);
            case TAMING -> stats.add(Stat.PET_LUCK, level);
            case HUNTING -> stats.add(Stat.HUNTING_FORTUNE, level);
            case RUNECRAFTING, SOCIAL -> {
            }
        }
        return stats;
    }
}
