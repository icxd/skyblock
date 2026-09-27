package net.icxd.dungeons.skill;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The XP tables (research skills.md 3.2-3.4), levels, progress and caps, and a profile's skill data. */
class SkillsTest {
    private static final double COMBAT_24 = 2_322_425 + 272_514.2;

    @Test
    void standardTable() {
        Skill combat = Skill.COMBAT;
        assertEquals(60, combat.maxLevel());
        assertEquals(50, combat.xpFor(1));
        assertEquals(125, combat.xpFor(2));
        assertEquals(3_500, combat.xpFor(10));
        assertEquals(700_000, combat.xpFor(25));
        assertEquals(2_750_000, combat.xpFor(45));
        assertEquals(7_000_000, combat.xpFor(60));
        assertEquals(0, combat.xpFor(61));
        assertEquals(175, combat.cumulative(2));
        assertEquals(9_925, combat.cumulative(10));
        assertEquals(3_022_425, combat.cumulative(25));
        assertEquals(55_172_425, combat.cumulative(50));
        assertEquals(111_672_425, combat.cumulative(60));
        // Every skill but Runecrafting and Social is on it.
        for (Skill skill : Skill.values()) {
            if (skill == Skill.RUNECRAFTING || skill == Skill.SOCIAL) continue;
            assertEquals(combat.cumulative(skill.cap()), skill.cumulative(skill.cap()), skill.name());
        }
    }

    @Test
    void runecraftingAndSocial() {
        assertEquals(25, Skill.RUNECRAFTING.maxLevel());
        assertEquals(785, Skill.RUNECRAFTING.xpFor(11));
        assertEquals(2_725, Skill.RUNECRAFTING.cumulative(10));
        // NEU's, not the wiki's 7,325 (which doesn't add up).
        assertEquals(7_360, Skill.RUNECRAFTING.cumulative(14));
        assertEquals(94_300, Skill.RUNECRAFTING.cumulative(25));
        assertEquals(100, Skill.SOCIAL.xpFor(2));
        assertEquals(7_550, Skill.SOCIAL.cumulative(10));
        assertEquals(272_800, Skill.SOCIAL.cumulative(25));
        assertTrue(Skill.RUNECRAFTING.cosmetic());
        assertTrue(Skill.SOCIAL.cosmetic());
        assertFalse(Skill.HUNTING.cosmetic());
    }

    @Test
    void levels() {
        Skill combat = Skill.COMBAT;
        assertEquals(0, combat.level(0));
        assertEquals(0, combat.level(49.9));
        assertEquals(1, combat.level(50));
        assertEquals(2, combat.level(175));
        assertEquals(24, combat.level(3_022_424.9));
        assertEquals(25, combat.level(3_022_425));
        assertEquals(50, combat.level(55_172_425));
        assertEquals(60, combat.level(111_672_425));
        assertEquals(60, combat.level(1e12));
    }

    @Test
    void caps() {
        assertEquals(60, Skill.COMBAT.cap());
        assertEquals(60, Skill.MINING.cap());
        assertEquals(60, Skill.ENCHANTING.cap());
        for (Skill skill : List.of(Skill.FARMING, Skill.FISHING, Skill.FORAGING, Skill.ALCHEMY, Skill.CARPENTRY, Skill.TAMING, Skill.HUNTING)) {
            assertEquals(50, skill.cap(), skill.name());
            // XP past the cap still counts, but the level stops at it.
            assertEquals(50, skill.level(111_672_425), skill.name());
            assertTrue(skill.maxed(55_172_425), skill.name());
        }
        assertEquals(25, Skill.RUNECRAFTING.level(1e9));
        assertEquals(25, Skill.SOCIAL.cap());
    }

    @Test
    void progress() {
        // Recorded: Combat XXIV at 272,514.2 of 700k is 38.9%.
        assertEquals(0.3893, Skill.COMBAT.progress(COMBAT_24), 1e-4);
        assertEquals(272_514.2, Skill.COMBAT.xpIntoLevel(COMBAT_24), 1e-6);
        assertEquals(0, Skill.COMBAT.progress(0));
        assertEquals(0.5, Skill.COMBAT.progress(25));
        assertEquals(1, Skill.COMBAT.progress(111_672_425));
        assertEquals(1, Skill.FARMING.progress(60_000_000));
        // Past the cap, XP beyond it.
        assertEquals(4_827_575, Skill.FARMING.xpIntoLevel(60_000_000), 1e-6);
    }

    @Test
    void coinsAndSkyBlockXp() {
        Skill combat = Skill.COMBAT;
        assertEquals(100, combat.coins(1));
        assertEquals(7_500, combat.coins(10));
        assertEquals(225_000, combat.coins(25));
        assertEquals(550_000, combat.coins(37));
        assertEquals(1_000_000, combat.coins(45));
        assertEquals(1_000_000, combat.coins(60));
        assertEquals(0, combat.coins(0));
        assertEquals(5, combat.skyBlockXp(10));
        assertEquals(10, combat.skyBlockXp(11));
        assertEquals(10, combat.skyBlockXp(25));
        assertEquals(20, combat.skyBlockXp(26));
        assertEquals(20, combat.skyBlockXp(50));
        assertEquals(30, combat.skyBlockXp(51));
        // The recorded next levels of the other skills.
        assertEquals(150_000, Skill.FARMING.coins(22));
        assertEquals(350_000, Skill.MINING.coins(30));
        assertEquals(5_000, Skill.HUNTING.coins(9));
        assertEquals(5, Skill.HUNTING.skyBlockXp(9));
        assertEquals(250, Skill.SOCIAL.coins(2));
        assertEquals(0, Skill.SOCIAL.skyBlockXp(2));
        assertEquals(0, Skill.RUNECRAFTING.coins(11));
        assertEquals(0, Skill.RUNECRAFTING.skyBlockXp(11));
    }

    @Test
    void profileXp() {
        Document profile = new Document();
        assertEquals(0, Skills.xp(profile, Skill.COMBAT));
        assertEquals(0, Skills.level(profile, Skill.FARMING));
        Skills.Gain gain = Skills.add(profile, Skill.COMBAT, 60.2);
        assertEquals(0, gain.before());
        assertEquals(60.2, gain.after());
        assertEquals(1, gain.newLevel());
        assertTrue(gain.leveledUp());
        assertEquals(60.2, profile.get("skills", Document.class).get("combat"));
        // Nothing for nothing.
        assertFalse(Skills.add(profile, Skill.COMBAT, 0).leveledUp());
        assertEquals(60.2, Skills.xp(profile, Skill.COMBAT));
    }

    /** Profiles from before kept whole numbers: they read the same, and the next gain writes a double. */
    @Test
    void wholeNumbersMigrate() {
        Document profile = new Document("skills", new Document("combat", 23_000));
        assertEquals(12, Skills.level(profile, Skill.COMBAT));
        Skills.Gain gain = Skills.add(profile, Skill.COMBAT, 10_000.5);
        assertEquals(13, gain.newLevel());
        assertInstanceOf(Double.class, profile.get("skills", Document.class).get("combat"));
        assertEquals(33_000.5, Skills.xp(profile, Skill.COMBAT));
        Skills.add(profile, Skill.FARMING, 50);
        assertEquals(1, Skills.level(profile, Skill.FARMING));
    }

    /** Research skills.md 1.1: the recorded levels average 21.6, Hunting in, Runecrafting and Social out. */
    @Test
    void average() {
        Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
        levels.put(Skill.COMBAT, 24);
        levels.put(Skill.FARMING, 21);
        levels.put(Skill.FISHING, 17);
        levels.put(Skill.MINING, 29);
        levels.put(Skill.FORAGING, 13);
        levels.put(Skill.ENCHANTING, 35);
        levels.put(Skill.ALCHEMY, 22);
        levels.put(Skill.CARPENTRY, 17);
        levels.put(Skill.RUNECRAFTING, 10);
        levels.put(Skill.TAMING, 30);
        levels.put(Skill.SOCIAL, 1);
        levels.put(Skill.HUNTING, 8);
        assertEquals(21.6, Skills.average(levels), 1e-9);
        assertEquals(0, Skills.average(new Document()));
    }

    /** Research skills.md 1.5: Farming 21, Fishing 17 and Carpentry 17 give +51, +37 and +17 Health. */
    @Test
    void stats() {
        assertEquals(51, SkillRewards.stats(Skill.FARMING, 21).get(Stat.HEALTH));
        assertEquals(37, SkillRewards.stats(Skill.FISHING, 17).get(Stat.HEALTH));
        assertEquals(17, SkillRewards.stats(Skill.CARPENTRY, 17).get(Stat.HEALTH));
        assertEquals(12, SkillRewards.stats(Skill.COMBAT, 24).get(Stat.CRIT_CHANCE));
        assertEquals(30, SkillRewards.stats(Skill.COMBAT, 60).get(Stat.CRIT_CHANCE));
        assertEquals(84, SkillRewards.stats(Skill.FARMING, 21).get(Stat.FARMING_FORTUNE));
        // Mining Defense: 36 at 25, 86 at 50, 106 at 60 (the wiki's totals).
        assertEquals(36, SkillRewards.stats(Skill.MINING, 25).get(Stat.DEFENSE));
        assertEquals(86, SkillRewards.stats(Skill.MINING, 50).get(Stat.DEFENSE));
        assertEquals(106, SkillRewards.stats(Skill.MINING, 60).get(Stat.DEFENSE));
        assertEquals(1.7, SkillRewards.stats(Skill.FISHING, 17).get(Stat.TREASURE_CHANCE), 1e-9);
        assertEquals(17.5, SkillRewards.stats(Skill.ENCHANTING, 35).get(Stat.ABILITY_DAMAGE));
        assertEquals(8, SkillRewards.stats(Skill.HUNTING, 8).get(Stat.HUNTING_FORTUNE));
        assertEquals(new Stats(), SkillRewards.stats(Skill.SOCIAL, 25));
        assertEquals(new Stats(), SkillRewards.stats(Skill.COMBAT, 0));

        Document profile = new Document("skills", new Document("farming", Skill.FARMING.cumulative(21))
                .append("fishing", Skill.FISHING.cumulative(17)).append("carpentry", Skill.CARPENTRY.cumulative(17)));
        assertEquals(105, Skills.stats(profile).get(Stat.HEALTH));
    }

    @Test
    void skyBlockXp() {
        Document profile = new Document("skills", new Document("combat", COMBAT_24));
        // 10 levels of 5 and 14 of 10.
        assertEquals(190, Skills.skyBlockXp(profile));
        assertEquals(0, Skills.skyBlockXp(new Document()));
    }

    @Test
    void byName() {
        assertEquals(Skill.HUNTING, Skill.parse("hunting"));
        assertNull(Skill.parse("DUNGEONEERING"));
        assertEquals("combat", Skill.COMBAT.key());
        assertEquals("Runecrafting", Skill.RUNECRAFTING.getName());
    }
}
