package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** How a class's stats, damage factors and reminders combine in a run, at the recorded levels. */
class RunClassesTest {
    private static final double EPSILON = 1e-9;
    private static final long NOW = 1_000_000;

    @Test
    void berserkMeleeBloodlustAndRagnarokMultiply() {
        RunClasses.State s = new RunClasses.State(DungeonClass.BERSERK, 20, false);
        // Melee Damage +55% at 20; arrows get none of it.
        assertEquals(1.55, RunClasses.damageFactor(s, false, NOW), EPSILON);
        assertEquals(1, RunClasses.damageFactor(s, true, NOW), EPSILON);
        // Bloodlust's next hit: 35% more.
        s.bloodlustUntil = NOW + 1;
        assertEquals(1.55 * 1.35, RunClasses.damageFactor(s, false, NOW), EPSILON);
        // Ragnarok's 1.5x on top.
        s.ultimateUntil = NOW + 1;
        assertEquals(1.55 * 1.35 * 1.5, RunClasses.damageFactor(s, false, NOW), EPSILON);
        assertEquals(1, RunClasses.damageFactor(s, true, NOW), EPSILON);
        // Both over.
        assertEquals(1.55, RunClasses.damageFactor(s, false, NOW + 1), EPSILON);
    }

    @Test
    void aClassPlayedAloneDoublesItsBase() {
        // 40% doubled, and the levels' 15%.
        assertEquals(1.95, RunClasses.damageFactor(new RunClasses.State(DungeonClass.BERSERK, 20, true), false, NOW), EPSILON);
    }

    @Test
    void archerArrowsAndMelee() {
        RunClasses.State s = new RunClasses.State(DungeonClass.ARCHER, 16, false);
        assertEquals(2.756, RunClasses.damageFactor(s, true, NOW), EPSILON);
        assertEquals(0.75, RunClasses.damageFactor(s, false, NOW), EPSILON);
        // Alone: the -25% isn't a bonus, so it stays.
        assertEquals(0.75, RunClasses.damageFactor(new RunClasses.State(DungeonClass.ARCHER, 16, true), false, NOW), EPSILON);
    }

    @Test
    void theOthersDontChangeDamage() {
        for (DungeonClass dungeonClass : List.of(DungeonClass.HEALER, DungeonClass.MAGE, DungeonClass.TANK)) {
            RunClasses.State s = new RunClasses.State(dungeonClass, 15, false);
            s.ultimateUntil = NOW + 1;
            assertEquals(1, RunClasses.damageFactor(s, false, NOW), EPSILON, dungeonClass.name());
            assertEquals(1, RunClasses.damageFactor(s, true, NOW), EPSILON, dungeonClass.name());
        }
    }

    @Test
    void tankProtectiveBarrierMultipliesAllTheirDefense() {
        RunClasses.State s = new RunClasses.State(DungeonClass.TANK, 14, false);
        Stats stats = new Stats().set(Stat.HEALTH, 1000).set(Stat.DEFENSE, 100).set(Stat.VITALITY, 100);
        RunClasses.classStats(s, stats, NOW);
        assertEquals(1100, stats.get(Stat.HEALTH), EPSILON);
        // (100 + 64) x 1.278.
        assertEquals(164 * 1.278, stats.get(Stat.DEFENSE), EPSILON);
        assertEquals(111.4, stats.get(Stat.VITALITY), EPSILON);
    }

    @Test
    void castleOfStoneCutsDamageTakenNotDefense() {
        RunClasses.State s = new RunClasses.State(DungeonClass.TANK, 14, false);
        Stats before = new Stats().set(Stat.DEFENSE, 300);
        RunClasses.classStats(s, before, NOW);
        assertEquals(1, RunClasses.damageTakenFactor(s, NOW), EPSILON);

        s.ultimateUntil = NOW + 20_000;
        Stats during = new Stats().set(Stat.DEFENSE, 300);
        RunClasses.classStats(s, during, NOW);
        // Its Defense (and so Seismic Wave's damage) is as without it; what hits take is 30%.
        assertEquals(before.get(Stat.DEFENSE), during.get(Stat.DEFENSE), EPSILON);
        assertEquals(ClassAbilities.seismicWaveDamage(before.get(Stat.DEFENSE)), ClassAbilities.seismicWaveDamage(during.get(Stat.DEFENSE)), EPSILON);
        assertEquals(0.3, RunClasses.damageTakenFactor(s, NOW), EPSILON);
        assertEquals(1, RunClasses.damageTakenFactor(s, NOW + 20_000), EPSILON);

        // Only a Tank's.
        RunClasses.State berserk = new RunClasses.State(DungeonClass.BERSERK, 20, false);
        berserk.ultimateUntil = NOW + 1;
        assertEquals(1, RunClasses.damageTakenFactor(berserk, NOW), EPSILON);
    }

    @Test
    void healerRenewMultipliesAllTheirMending() {
        RunClasses.State s = new RunClasses.State(DungeonClass.HEALER, 15, false);
        Stats stats = new Stats().set(Stat.MENDING, 100).set(Stat.VITALITY, 100);
        RunClasses.classStats(s, stats, NOW);
        assertEquals(117.5 * 1.65, stats.get(Stat.MENDING), EPSILON);
        assertEquals(113, stats.get(Stat.VITALITY), EPSILON);
    }

    @Test
    void berserkStatsAndRagnarok() {
        RunClasses.State s = new RunClasses.State(DungeonClass.BERSERK, 20, false);
        Stats stats = new Stats().set(Stat.SPEED, 100).set(Stat.SWING_RANGE, 3).set(Stat.STRENGTH, 200);
        RunClasses.classStats(s, stats, NOW);
        assertEquals(138, stats.get(Stat.SPEED), EPSILON);
        assertEquals(6.2, stats.get(Stat.SWING_RANGE), EPSILON);
        // Indomitable: 7% of their Strength.
        assertEquals(14, stats.get(Stat.DEFENSE), EPSILON);
        assertEquals(0, stats.get(Stat.ATTACK_SPEED), EPSILON);

        s.ultimateUntil = NOW + 1;
        Stats raging = new Stats().set(Stat.SPEED, 100);
        RunClasses.classStats(s, raging, NOW);
        assertEquals(538, raging.get(Stat.SPEED), EPSILON);
        assertEquals(100, raging.get(Stat.ATTACK_SPEED), EPSILON);
    }

    @Test
    void mageStats() {
        RunClasses.State s = new RunClasses.State(DungeonClass.MAGE, 15, false);
        Stats stats = new Stats();
        RunClasses.classStats(s, stats, NOW);
        assertEquals(325, stats.get(Stat.INTELLIGENCE), EPSILON);
        assertEquals(8, stats.get(Stat.ABILITY_DAMAGE), EPSILON);
    }

    @Test
    void ultimateRemindersAsRecorded() {
        RunClasses.State s = new RunClasses.State(DungeonClass.BERSERK, 20, false);
        // 23.3 s in (RUN1 23.4 s, RUN2 23.2 s), then every 30 s while it's unused.
        assertEquals(List.of(466, 1066, 1666), dueUpTo(s, 1, 1700, 0));

        // Used at tick 1700: its 60 second cooldown, then every 30 s again.
        RunClasses.usedUltimate(s, 1700, NOW, 60_000);
        assertFalse(RunClasses.reminderDue(s, 2899, NOW + 59_950));
        assertTrue(RunClasses.reminderDue(s, 2900, NOW + 60_000));
        assertEquals(List.of(3500, 4100), dueUpTo(s, 2901, 4200, NOW + 60_000));
    }

    @Test
    void noReminderWhileItCantBeUsed() {
        RunClasses.State s = new RunClasses.State(DungeonClass.TANK, 14, false);
        s.ultimateReadyAt = NOW + 1;
        // Due, but not ready: nothing, and the next one's 30 s later.
        assertFalse(RunClasses.reminderDue(s, 466, NOW));
        assertTrue(RunClasses.reminderDue(s, 1066, NOW + 1));
    }

    private static List<Integer> dueUpTo(RunClasses.State s, int from, int to, long now) {
        List<Integer> due = new ArrayList<>();
        for (int tick = from; tick <= to; tick++) if (RunClasses.reminderDue(s, tick, now)) due.add(tick);
        return due;
    }
}
