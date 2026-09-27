package net.icxd.dungeons.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Combat XP per kill (research skills.md 5.1, 5.2). */
class SkillGainsTest {
    @Test
    void champion() {
        assertEquals(0, SkillGains.champion(0));
        assertEquals(3, SkillGains.champion(1));
        assertEquals(6.11, SkillGains.champion(5));
        assertEquals(10, SkillGains.champion(10));
    }

    /** Research's arithmetic for the recorded kills with Champion V: 40 x 1.418 x 1.0611 = 60.19, shown "+60.2". */
    @Test
    void perKill() {
        assertEquals(60.19, SkillGains.combatXp(40, 41.8, 5), 0.005);
        assertEquals(40, SkillGains.combatXp(40, 0, 0));
        assertEquals(44, SkillGains.combatXp(40, 0, 10), 1e-9);
        assertEquals(60, SkillGains.combatXp(40, 50, 0), 1e-9);
    }

    /**
     * Every recorded gain, with the recording's Combat Wisdom (41.8 to 41.9; 41.85 fits them all): the
     * Zombie Grunt's +60.2, the Souleater's +67.7, the Dreadlord's +91.8, the Watcher's undeads' +112.9,
     * an Undead Skeleton killed without Champion +51.1, and a Grunt with Kill Combo's +15 wisdom +66.6.
     */
    @Test
    void recordedGains() {
        assertEquals("60.2", SkillText.number(SkillGains.combatXp(40, 41.85, 5)));
        assertEquals("67.7", SkillText.number(SkillGains.combatXp(45, 41.85, 5)));
        assertEquals("91.8", SkillText.number(SkillGains.combatXp(61, 41.85, 5)));
        assertEquals("112.9", SkillText.number(SkillGains.combatXp(75, 41.85, 5)));
        assertEquals("54.2", SkillText.number(SkillGains.combatXp(36, 41.85, 5)));
        assertEquals("51.1", SkillText.number(SkillGains.combatXp(36, 41.85, 0)));
        assertEquals("66.6", SkillText.number(SkillGains.combatXp(40, 56.85, 5)));
    }
}
