package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Recorded on Hypixel (R1, research secrets_puzzles.md 1.5, 1.8, 2; critic C9), with the plain symbols in place of Hypixel's glyphs. */
class BlessingTest {
    @Test
    void grantedLinesAsRecorded() {
        assertEquals(List.of("     &7Granted you &a&a+4&7 & &a+1.02x &a❈ Defense &7and &a+6 &c❁ Damage&7."), Blessing.STONE.granted(1, 1));
        assertEquals(List.of("     &7Granted you &a&a+20&7 & &a+1.1x &a❈ Defense &7and &a+30 &c❁ Damage&7."), Blessing.STONE.granted(5, 1));
        assertEquals(List.of("     &7Granted you &a&a+20&7 & &a+1.1x &b✎ Intelligence &7and &a+20 &f✦ Speed&7."), Blessing.WISDOM.granted(5, 1));
        assertEquals(List.of("     &7Granted you &a&a+8&7 & &a+1.04x &b✎ Intelligence &7and &a+8 &f✦ Speed&7."), Blessing.WISDOM.granted(2, 1));
        assertEquals(List.of("     &7Granted you &a&a+20&7 & &a+1.1x &c❁ Strength&7.",
                "     &7Also granted you &a&a+20&7 & &a+1.1x &9☠ Crit Damage&7."), Blessing.POWER.granted(5, 1));
        assertEquals(List.of("     &7Granted you &a&a+1.15x HP &7and &a&a+1.15x &c❣ Health Regen&7."), Blessing.LIFE.granted(5, 1));
        assertEquals(List.of("     &7Granted you &a&a+1.03x HP &7and &a&a+1.03x &c❣ Health Regen&7."), Blessing.LIFE.granted(1, 1));
    }

    @Test
    void foundLines() {
        assertEquals("&6&lDUNGEON BUFF! &fYou found a &dBlessing of Stone I&f!", Blessing.STONE.found(null, 1, null));
        assertEquals("&6&lDUNGEON BUFF! &fYou found a &dBlessing of Wisdom V&f! (&a14s&f)", Blessing.WISDOM.found(null, 5, "14s"));
        assertEquals("&6&lDUNGEON BUFF! &fYou found a &dBlessing of Power V&f! (&a01m 22s&f)", Blessing.POWER.found(null, 5, "01m 22s"));
        assertEquals("&6&lDUNGEON BUFF! &bAlice &ffound a &dBlessing of Life V&f!", Blessing.LIFE.found("&bAlice", 5, null));
        assertEquals("Blessing of Wisdom XII", Blessing.WISDOM.displayName(12));
    }

    /** R1's run started at 00:32.5; blessings picked up at 00:46.8, 01:55.1 and 03:45.0 said 14s, 01m 22s and 03m 12s. */
    @Test
    void runTimeInWholeSeconds() {
        assertEquals("14s", Blessing.elapsed(14_300));
        assertEquals("01m 22s", Blessing.elapsed(82_600));
        assertEquals("03m 12s", Blessing.elapsed(192_500));
    }

    /** Flat first, then times the share: Stone I took 2,343 Defense to 2,394; Life VI (V and I) took 5,238.67 health to 6,181. */
    @Test
    void statsAsRecorded() {
        Stats stats = new Stats().set(Stat.DEFENSE, 2_343).set(Stat.HEALTH, 5_238.67).set(Stat.DAMAGE, 100).set(Stat.HEALTH_REGEN, 100);
        Blessing.apply(stats, Map.of(Blessing.STONE, 1, Blessing.LIFE, 6), 1);
        // 2,393.94 (shown 2,394) and 6,181.6 (shown 6,181).
        assertEquals(2_394, stats.get(Stat.DEFENSE), 0.1);
        assertEquals(6_181, (int) stats.get(Stat.HEALTH));
        assertEquals(106, stats.get(Stat.DAMAGE), 1e-9);
        assertEquals(118, stats.get(Stat.HEALTH_REGEN), 1e-9);
    }

    @Test
    void levelsAddAndOthersStayPut() {
        Stats stats = new Stats().set(Stat.INTELLIGENCE, 1_000).set(Stat.SPEED, 300).set(Stat.STRENGTH, 50);
        // Wisdom V + V + II, as recorded: XII.
        Blessing.apply(stats, Map.of(Blessing.WISDOM, 12), 1);
        assertEquals((1_000 + 48) * 1.24, stats.get(Stat.INTELLIGENCE), 1e-9);
        assertEquals(348, stats.get(Stat.SPEED), 1e-9);
        assertEquals(50, stats.get(Stat.STRENGTH), 1e-9);
    }

    @Test
    void strongerFromFloorThree() {
        assertEquals(1, Blessing.strength(DungeonFloor.ENTRANCE));
        assertEquals(1, Blessing.strength(DungeonFloor.FLOOR_2));
        assertEquals(1.2, Blessing.strength(DungeonFloor.FLOOR_3));
        assertEquals(1.2, Blessing.strength(DungeonFloor.MASTER_FLOOR_7));
        Stats stats = new Stats().set(Stat.STRENGTH, 100);
        Blessing.apply(stats, Map.of(Blessing.POWER, 5), 1.2);
        assertEquals((100 + 24) * 1.12, stats.get(Stat.STRENGTH), 1e-9);
        assertEquals(List.of("     &7Granted you &a&a+24&7 & &a+1.12x &c❁ Strength&7.",
                "     &7Also granted you &a&a+24&7 & &a+1.12x &9☠ Crit Damage&7."), Blessing.POWER.granted(5, 1.2));
    }

    @Test
    void runsKeepLevelsAndListThemInTheFootersOrder() {
        RunBlessings run = new RunBlessings(DungeonFloor.ENTRANCE);
        assertEquals(List.of("", "&6&lDungeon Buffs", "&7No Buffs active. Find them by exploring the Dungeon!"), run.footer());
        // R1's order of finding them; the footer lists Power, Wisdom, Stone, Life whatever the order.
        run.add(Blessing.WISDOM, 5);
        run.add(Blessing.WISDOM, 5);
        run.add(Blessing.STONE, 1);
        run.add(Blessing.POWER, 5);
        run.add(Blessing.WISDOM, 2);
        run.add(Blessing.LIFE, 5);
        run.add(Blessing.STONE, 5);
        run.add(Blessing.LIFE, 1);
        assertEquals(List.of("", "&6&lDungeon Buffs", "&fBlessing of Power V", "&fBlessing of Wisdom XII", "&fBlessing of Stone VI",
                "&fBlessing of Life VI"), run.footer());
        assertEquals(12, run.level(Blessing.WISDOM));
        assertEquals(0, run.level(Blessing.TIME));

        Stats stats = new Stats().set(Stat.HEALTH, 1_000);
        run.apply(stats);
        assertEquals(1_180, stats.get(Stat.HEALTH), 1e-9);
    }

    /** Rooms name the kind ("Wisdom", research mobs.md 1.4's drops). */
    @Test
    void byName() {
        assertEquals(Blessing.WISDOM, Blessing.named("Wisdom"));
        assertEquals(Blessing.POWER, Blessing.named("Blessing of Power"));
        assertEquals(Blessing.LIFE, Blessing.named("life"));
        assertEquals(null, Blessing.named("Luck"));
        assertEquals(null, Blessing.named(null));
    }

    @Test
    void numbers() {
        assertEquals("20", Blessing.number(20));
        assertEquals("1.1", Blessing.number(1.1));
        assertEquals("1.02", Blessing.number(1.02));
        assertEquals("4.8", Blessing.number(4.8));
        Map<Blessing, Integer> none = new EnumMap<>(Blessing.class);
        Stats stats = new Stats().set(Stat.DEFENSE, 10);
        Blessing.apply(stats, none, 1);
        assertEquals(10, stats.get(Stat.DEFENSE));
    }
}
