package net.icxd.dungeons.skill;

import net.icxd.dungeons.dungeons.DungeonClass;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The numbers, bars, action bar, tab list lines and level-up message (research skills.md 1.2, 2.1-2.3). */
class SkillTextTest {
    @Test
    void menuNumbers() {
        assertEquals("272,514.2", SkillText.number(272_514.2));
        assertEquals("82,575", SkillText.number(82_575));
        assertEquals("1,529,873.6", SkillText.number(1_529_873.6));
        assertEquals("0", SkillText.number(0));
        assertEquals("785", SkillText.shortNumber(785));
        assertEquals("2k", SkillText.shortNumber(2_000));
        assertEquals("15k", SkillText.shortNumber(15_000));
        assertEquals("75k", SkillText.shortNumber(75_000));
        assertEquals("700k", SkillText.shortNumber(700_000));
        assertEquals("192.7k", SkillText.shortNumber(192_700));
        assertEquals("1.2M", SkillText.shortNumber(1_200_000));
        assertEquals("1.8M", SkillText.shortNumber(1_800_000));
        assertEquals("7M", SkillText.shortNumber(7_000_000));
        // One decimal, ".0" dropped; Hunting's 85 of 2k is a tie that the menu shows as 4.2%.
        assertEquals("38.9", SkillText.percent(272_514.2 / 700_000));
        assertEquals("85", SkillText.percent(1_529_873.6 / 1_800_000));
        assertEquals("54.7", SkillText.percent(8_199.8 / 15_000));
        assertEquals("4.2", SkillText.percent(85 / 2_000.0));
    }

    /** Filled = ceil(fraction x 25): the recorded 4.2%, 16.5%, 36% and 95.5% bars. */
    @Test
    void bars() {
        assertEquals(2, SkillText.filled(0.042));
        assertEquals(5, SkillText.filled(0.165));
        assertEquals(9, SkillText.filled(0.36));
        assertEquals(24, SkillText.filled(0.955));
        assertEquals(0, SkillText.filled(0));
        assertEquals(1, SkillText.filled(0.0001));
        assertEquals(25, SkillText.filled(1));
        assertEquals("&2&l&m          &f&l&m               &r &e272,514.2&6/&e700k",
                SkillText.bar(272_514.2 / 700_000, 272_514.2, 700_000));
        // Empty and full: no part for the side with nothing in it.
        assertEquals("&f&l&m                         &r", SkillText.strip(0));
        assertEquals("&2&l&m                         &r", SkillText.strip(1));
    }

    /** The action bar's "+60.2 Combat (35.46%)": the last gain to one decimal, the level to two. */
    @Test
    void actionBar() {
        double at = Skill.COMBAT.cumulative(24) + 0.3546 * 700_000;
        assertEquals("&3+60.2 Combat (35.46%)", SkillText.actionBar(new Skills.Gain(Skill.COMBAT, 60.1856, at - 60.1856, at)));
        at = Skill.COMBAT.cumulative(24) + 0.359 * 700_000;
        assertEquals("&3+112.9 Combat (35.9%)", SkillText.actionBar(new Skills.Gain(Skill.COMBAT, 112.86, at - 112.86, at)));
        at = Skill.COMBAT.cumulative(24) + 0.36 * 700_000;
        assertEquals("&3+91.8 Combat (36%)", SkillText.actionBar(new Skills.Gain(Skill.COMBAT, 91.78, at - 91.78, at)));
        // Whole numbers and thousands (both UNKNOWN from the recordings): like the menus' numbers.
        assertEquals("&3+40 Combat (80%)", SkillText.actionBar(new Skills.Gain(Skill.COMBAT, 40, 0, 40)));
        assertEquals("&3+4,000 Combat (80%)", SkillText.actionBar(new Skills.Gain(Skill.COMBAT, 4_000, 9_925, 13_925)));
        // Maxed: XP past the cap over 0.
        assertEquals("&3+207.2 Hunting (5,183,244/0)",
                SkillText.actionBar(new Skills.Gain(Skill.HUNTING, 207.2, 60_355_669 - 207.2, 60_355_669)));
    }

    @Test
    void tabList() {
        Document profile = new Document("skills", new Document()
                .append("farming", Skill.FARMING.cumulative(21) + 0.399 * 400_000)
                .append("mining", Skill.MINING.cumulative(29) + 0.722 * 1_200_000)
                .append("combat", Skill.COMBAT.cumulative(24) + 0.365 * 700_000)
                .append("foraging", Skill.FORAGING.cumulative(13) + 0.547 * 15_000));
        assertEquals(List.of("&e&lSkills:", " Farming 21: &a39.9%", " Mining 29: &a72.2%", " Combat 24: &a36.5%", " Foraging 13: &a54.7%"),
                SkillText.hubTab(profile));
        assertEquals("&e&lSkills: &aCombat 24: &335.5%", SkillText.dungeonTab(Skill.COMBAT, Skill.COMBAT.cumulative(24) + 0.3546 * 700_000));
        assertEquals("&e&lSkills: &aCombat 24: &336%", SkillText.dungeonTab(Skill.COMBAT, Skill.COMBAT.cumulative(24) + 0.36 * 700_000));
        assertEquals(" Farming: &a0%", " " + SkillText.tab(Skill.FARMING, 0, "&a"));
        assertEquals("Combat 60: &aMAX", SkillText.tab(Skill.COMBAT, 111_672_425, "&a"));
        // The Dungeon Hub's: Catacombs 21 at 22% (of 71,500), Berserk 20 at 72.6% (of 52,500).
        assertEquals(List.of("&6&lDungeons:", " &fCatacombs 21: &a22%", " &aBerserk 20: 72.6%"),
                SkillText.dungeonsTab(188_140 + 0.22 * 71_500, DungeonClass.BERSERK, 135_640 + 0.726 * 52_500));
    }

    /** Not recorded: the documented layout of other remakes (research skills.md 2.3). */
    @Test
    void levelUp() {
        String rule = "&3&l" + "▬".repeat(64);
        assertEquals(List.of(rule,
                "  &b&lSKILL LEVEL UP &3Combat &8XXIV➜&3XXV",
                "",
                "  &a&lREWARDS",
                "    &eWarrior XXV",
                "      &fDeal &896➜&a100%&f more damage to mobs.",
                "    &8+&a0.5% &9☣ Crit Chance",
                "    &5Horns of Torment &7Power Stone",
                "    &6Bubba Blister &7Power Stone",
                "    &6Eccentric Painting &7Power Stone",
                "    &5Magma Urchin &7Power Stone",
                "    &5Precious Pearl &7Power Stone",
                "    &8+&6225,000 &7Coins",
                "    &8+&b10 SkyBlock XP",
                rule), SkillText.levelUp(Skill.COMBAT, 25));
        // From level 0 there's no old level.
        assertEquals("  &b&lSKILL LEVEL UP &3Combat &3I", SkillText.levelUp(Skill.COMBAT, 1).get(1));
    }

    @Test
    void names() {
        assertEquals("Combat XXIV", SkillText.named(Skill.COMBAT, 24));
        assertEquals("Social", SkillText.named(Skill.SOCIAL, 0));
    }
}
