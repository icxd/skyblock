package net.icxd.dungeons.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A mob's debuffs and damages over time (made-up sources, Lethality-like and Fire Aspect-like numbers). */
class DebuffsTest {
    private static final UUID ONE = new UUID(0, 1);
    private static final UUID TWO = new UUID(0, 2);
    /** 9% of its Defense a stack, up to 4, for 4 s. */
    private static final Debuffs.Spec SHRED = new Debuffs.Spec("shred", Debuffs.Kind.DEFENSE, 0.09, 4, 4_000);

    /** Each time it's put on it stacks, up to its most, and its time starts again. */
    @Test
    void stacksUpToItsMost() {
        Debuffs debuffs = new Debuffs();
        for (int i = 0; i < 6; i++) debuffs.add(SHRED, ONE, 1_000 * i);
        assertEquals(4, debuffs.stacks("shred", 5_000));
        assertEquals(1000 * (1 - 0.36), debuffs.defense(1000, 5_000), 1e-9);
        // 4 s after the last one (at 5 s) it's gone, stacks and all.
        assertEquals(4, debuffs.stacks("shred", 8_999));
        assertEquals(0, debuffs.stacks("shred", 9_000));
        assertEquals(1000, debuffs.defense(1000, 9_000), 1e-9);
        assertTrue(debuffs.expire(9_000));
    }

    /** Put on again weaker, it keeps the stronger value (and whose it is); a stronger one takes over. */
    @Test
    void refreshingNeverLowersIt() {
        Debuffs debuffs = new Debuffs();
        debuffs.add(SHRED, ONE, 0);
        debuffs.add(new Debuffs.Spec("shred", Debuffs.Kind.DEFENSE, 0.012, 4, 4_000), TWO, 100);
        assertEquals(1 - 2 * 0.09, debuffs.defense(1, 200), 1e-9);
        assertEquals(ONE, debuffs.by("shred", 200));
        debuffs.add(new Debuffs.Spec("shred", Debuffs.Kind.DEFENSE, 0.2, 4, 4_000), TWO, 300);
        assertEquals(1 - 3 * 0.2, debuffs.defense(1, 400), 1e-9);
        assertEquals(TWO, debuffs.by("shred", 400));
        // Once it has run out, it starts again from nothing.
        debuffs.add(new Debuffs.Spec("shred", Debuffs.Kind.DEFENSE, 0.012, 4, 4_000), ONE, 10_000);
        assertEquals(1, debuffs.stacks("shred", 10_000));
        assertEquals(1 - 0.012, debuffs.defense(1, 10_000), 1e-9);
    }

    /** Defense shreds and slows add up, never past all of it; "takes more" factors multiply. */
    @Test
    void kindsCombine() {
        Debuffs debuffs = new Debuffs();
        debuffs.add(new Debuffs.Spec("a", Debuffs.Kind.DEFENSE, 0.7, 1, 1_000), ONE, 0);
        debuffs.add(new Debuffs.Spec("b", Debuffs.Kind.DEFENSE, 0.6, 1, 1_000), ONE, 0);
        assertEquals(0, debuffs.defense(500, 10), 1e-9);
        debuffs.add(new Debuffs.Spec("frozen", Debuffs.Kind.TAKEN, 0.1, 1, 1_000), null, 0);
        debuffs.add(new Debuffs.Spec("marked", Debuffs.Kind.TAKEN, 0.5, 1, 1_000), null, 0);
        assertEquals(1.1 * 1.5, debuffs.takenFactor(10), 1e-9);
        debuffs.add(new Debuffs.Spec("venom", Debuffs.Kind.SLOW, 0.02, 40, 5_000), ONE, 0);
        debuffs.add(new Debuffs.Spec("venom", Debuffs.Kind.SLOW, 0.02, 40, 5_000), ONE, 0);
        assertEquals(0.04, debuffs.of(Debuffs.Kind.SLOW, 10), 1e-9);
        assertEquals(1, new Debuffs().takenFactor(0), 1e-9);
        // The TAKEN ones run out with their time.
        assertEquals(1, debuffs.takenFactor(1_000), 1e-9);
    }

    /** A damage over time ticks every so many ticks, so many times, then ends. */
    @Test
    void dotTicks() {
        Debuffs debuffs = new Debuffs();
        debuffs.dot("fire", ONE, 90, 20, 3, DamageIndicators.Look.FIRE);
        List<Double> dealt = new ArrayList<>();
        Debuffs.DotSink sink = (source, by, damage, look) -> {
            assertEquals("fire", source);
            assertEquals(ONE, by);
            assertEquals(DamageIndicators.Look.FIRE, look);
            dealt.add(damage);
        };
        for (int tick = 1; tick <= 19; tick++) debuffs.tick(sink);
        assertTrue(dealt.isEmpty());
        debuffs.tick(sink);
        assertEquals(List.of(90.0), dealt);
        for (int tick = 21; tick <= 100; tick++) debuffs.tick(sink);
        assertEquals(List.of(90.0, 90.0, 90.0), dealt);
        assertFalse(debuffs.hasDot("fire"));
        assertTrue(debuffs.expire(0));
    }

    /**
     * Put on again, it counts its times from now, keeps its beat and the greater damage: "swapping to a Fire
     * Aspect II Aspect of the Jerry will refresh the timer of the 9,000 damage DoT, without decreasing it".
     */
    @Test
    void dotRefreshes() {
        Debuffs debuffs = new Debuffs();
        debuffs.dot("fire", ONE, 9_000, 20, 3, DamageIndicators.Look.FIRE);
        List<Double> dealt = new ArrayList<>();
        List<UUID> whose = new ArrayList<>();
        Debuffs.DotSink sink = (source, by, damage, look) -> {
            dealt.add(damage);
            whose.add(by);
        };
        for (int tick = 1; tick <= 30; tick++) debuffs.tick(sink);
        debuffs.dot("fire", TWO, 20, 20, 3, DamageIndicators.Look.FIRE);
        // Next due at tick 40, as before; three more from the renewal, all of the 9,000.
        for (int tick = 31; tick <= 39; tick++) debuffs.tick(sink);
        assertEquals(1, dealt.size());
        for (int tick = 40; tick <= 200; tick++) debuffs.tick(sink);
        assertEquals(List.of(9_000.0, 9_000.0, 9_000.0, 9_000.0), dealt);
        assertEquals(List.of(ONE, ONE, ONE, ONE), whose);
    }

    /** The tick looks through them only once one can have run out; put on again, it's looked at later. */
    @Test
    void dueOnlyWhenOneCanRunOut() {
        Debuffs debuffs = new Debuffs();
        assertFalse(debuffs.due(0));
        debuffs.add(SHRED, ONE, 0);
        assertFalse(debuffs.due(3_999));
        assertTrue(debuffs.due(4_000));
        debuffs.add(SHRED, ONE, 3_000);
        // Its first time was up at 4 s; looked at then, it's still on (till 7 s), and not due again before.
        assertFalse(debuffs.expire(4_000));
        assertEquals(2, debuffs.stacks("shred", 4_000));
        assertFalse(debuffs.due(6_999));
        assertTrue(debuffs.due(7_000));
        assertTrue(debuffs.expire(7_000));
        assertTrue(debuffs.isEmpty());
        assertFalse(debuffs.due(7_000));
    }

    @Test
    void removeTakesItOff() {
        Debuffs debuffs = new Debuffs();
        debuffs.add(SHRED, ONE, 0);
        debuffs.dot("shred", ONE, 5, 1, 10, DamageIndicators.Look.POISON);
        debuffs.remove("shred");
        assertEquals(0, debuffs.stacks("shred", 1));
        assertFalse(debuffs.hasDot("shred"));
        assertNull(debuffs.by("shred", 1));
        // Nothing to deal: no time, no damage.
        debuffs.dot("none", ONE, 0, 20, 5, DamageIndicators.Look.FIRE);
        debuffs.dot("never", ONE, 5, 20, 0, DamageIndicators.Look.FIRE);
        assertFalse(debuffs.hasDot("none"));
        assertFalse(debuffs.hasDot("never"));
    }
}
