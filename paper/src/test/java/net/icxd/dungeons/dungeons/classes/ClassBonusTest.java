package net.icxd.dungeons.dungeons.classes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.DungeonClass;

class ClassBonusTest {
    /** RUN2 00:21.9, the Ready Up menu's class items at the recorded levels. */
    @Test
    void readyUpLinesAtTheRecordedLevels() {
        assertEquals(List.of("&7Vitality: &a+13", "&7Mending: &a+17.5"), ClassBonus.readyUpLines(DungeonClass.HEALER, 15));
        assertEquals(List.of("&7Intelligence: &a+325", "&7Ability Damage: &a+8%"), ClassBonus.readyUpLines(DungeonClass.MAGE, 15));
        assertEquals(List.of("&7Melee Damage: &c+55%", "&7Walk Speed: &a+38"), ClassBonus.readyUpLines(DungeonClass.BERSERK, 20));
        assertEquals(List.of("&7Arrow Damage: &c+175.6%", "&7Melee Damage: &c-25%"), ClassBonus.readyUpLines(DungeonClass.ARCHER, 16));
        assertEquals(List.of("&7Health: &a+100", "&7Defense: &a+64", "&7Vitality: &a+11.4"), ClassBonus.readyUpLines(DungeonClass.TANK, 14));
    }

    /** RUN1 00:32.5 and RUN2 01:52.3: the solo Berserk 20's chat as the run starts. */
    @Test
    void soloBerserkAsRecorded() {
        assertEquals(List.of(
                "&6Your &aBerserk &6stats are doubled because you are the only player using this class!",
                "&a[Berserk] &fMelee Damage &c55%&f -> &a95%",
                "&a[Berserk] &fWalk Speed &c38&f -> &a68",
                "&a[Berserk] &fBloodlust Damage &c35%&f -> &a55%",
                "&a[Berserk] &fLust For Blood Damage Increase Cap &c530%&f -> &a780%",
                "&a[Berserk] &fLust For Blood Damage Increase Per Hit &c75%&f -> &a90%",
                "&a[Berserk] &fIndomitable Strength to Defense &c7%&f -> &a12%",
                "&a[Berserk] &fWeapon Master Swing Range Increase &c3.2&f -> &a3.7",
                "&a[Berserk] &fBloodlust Heal Percent &c3%&f -> &a6%",
                "&a[Berserk] &fBloodlust Duration &c5&f -> &a10"), ClassBonus.soloMessage(DungeonClass.BERSERK, 20));
    }

    @Test
    void soloDoublesTheBaseOnly() {
        assertEquals(55, ClassBonus.BERSERK_MELEE_DAMAGE.value(20, false), 1e-9);
        assertEquals(95, ClassBonus.BERSERK_MELEE_DAMAGE.value(20, true), 1e-9);
        // The wiki's "+25% -> +50%" for a lone tank's Protective Barrier.
        assertEquals(50, ClassBonus.TANK_PROTECTIVE_BARRIER.value(0, true), 1e-9);
        // A penalty isn't a bonus: it stays.
        assertEquals(-25, ClassBonus.ARCHER_MELEE_DAMAGE.value(30, true), 1e-9);
        assertFalse(ClassBonus.ARCHER_MELEE_DAMAGE.doubles());
        assertFalse(ClassBonus.soloMessage(DungeonClass.ARCHER, 16).stream().anyMatch(line -> line.contains("Melee")));
        assertTrue(ClassBonus.soloMessage(DungeonClass.ARCHER, 16).contains("&a[Archer] &fArrow Damage &c175.6%&f -> &a325.6%"));
    }

    @Test
    void levelsPastFiftyAddNothing() {
        assertEquals(ClassBonus.MAGE_INTELLIGENCE.value(50, false), ClassBonus.MAGE_INTELLIGENCE.value(70, false), 1e-9);
        // The wiki's level-50 totals where the formula is the wiki's.
        assertEquals(500, ClassBonus.MAGE_INTELLIGENCE.value(50, false), 1e-9);
        assertEquals(15, ClassBonus.MAGE_ABILITY_DAMAGE.value(50, false), 1e-9);
        assertEquals(230, ClassBonus.ARCHER_ARROW_DAMAGE.value(50, false), 1e-9);
        assertEquals(77.5, ClassBonus.BERSERK_MELEE_DAMAGE.value(50, false), 1e-9);
        assertEquals(0, ClassBonus.TANK_DEFENSE.value(-3, false) - 50, 1e-9);
    }

    @Test
    void numbers() {
        assertEquals("13", ClassBonus.format(10 + 0.2 * 15));
        assertEquals("3.2", ClassBonus.format(0.5 + 0.135 * 20));
        assertEquals("1,554", ClassBonus.format(1554));
        assertEquals("-25", ClassBonus.format(-25));
        assertEquals("1.6", ClassBonus.multiplier(1.65));
        assertEquals("1.3", ClassBonus.multiplier(1.278));
    }
}
