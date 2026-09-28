package net.icxd.dungeons.mob;

import net.icxd.dungeons.combat.HitKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the death event says killed a mob. */
class KillingBlowTest {
    /** Overkill is what the blow did past what the mob had left, never below 0. */
    @Test
    void overkill() {
        assertEquals(2_500, KillingBlow.of(HitKind.MELEE, null, 10_000, 7_500).overkill(), 1e-9);
        assertEquals(0, KillingBlow.of(HitKind.ARROW, null, 100, 100).overkill(), 1e-9);
        assertEquals(0, new KillingBlow(HitKind.DOT, null, 5, -3).overkill(), 1e-9);
        assertEquals(10, KillingBlow.of(HitKind.ABILITY, null, 10, -4).overkill(), 1e-9);
    }

    /** The blow being dealt nests: each one put back the one it replaced once it's done. */
    @Test
    void dealingNests() {
        assertNull(KillingBlow.dealing());
        KillingBlow outer = KillingBlow.of(HitKind.MELEE, null, 1, 1);
        KillingBlow inner = KillingBlow.of(HitKind.OTHER, null, 2, 1);
        KillingBlow before = KillingBlow.dealing(outer);
        KillingBlow middle = KillingBlow.dealing(inner);
        assertSame(inner, KillingBlow.dealing());
        KillingBlow.dealing(middle);
        assertSame(outer, KillingBlow.dealing());
        KillingBlow.dealing(before);
        assertNull(KillingBlow.dealing());
    }

    /** Hits are what the hit listeners hear of; a damage over time's tick and another effect's damage aren't. */
    @Test
    void whatsAHit() {
        for (HitKind kind : new HitKind[] {HitKind.MELEE, HitKind.ARROW, HitKind.FEROCITY, HitKind.ABILITY}) assertTrue(kind.isHit(), kind.name());
        assertFalse(HitKind.DOT.isHit());
        assertFalse(HitKind.OTHER.isHit());
    }
}
