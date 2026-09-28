package net.icxd.dungeons.combat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which hits the hit listeners hear of, and that a listener's own hit isn't heard again. */
class HitListenersTest {
    private static final List<HitKind> HEARD = new ArrayList<>();
    /** The listener below only counts while a test here runs (it stays registered for the whole run). */
    private static boolean on;

    static {
        Combat.addHitListener((player, landing, target, damage, killed) -> {
            if (!on) return;
            HEARD.add(landing.kind());
            // Its own hit, as an ability's (a careless Cleave's): not heard again, so it can't set itself off.
            Combat.landed(player, new Combat.Landing(null, HitKind.ABILITY, false, null, null), target, damage / 2, false);
        });
    }

    @Test
    void heardOnceEach() {
        on = true;
        try {
            Combat.landed(null, new Combat.Landing(null, HitKind.MELEE, true, null, null), null, 100, false);
            Combat.landed(null, new Combat.Landing(null, HitKind.DOT, false, null, null), null, 100, false);
            Combat.landed(null, new Combat.Landing(null, HitKind.OTHER, false, null, null), null, 100, false);
            Combat.landed(null, new Combat.Landing(null, HitKind.FEROCITY, true, null, null), null, 100, true);
            assertEquals(List.of(HitKind.MELEE, HitKind.FEROCITY), HEARD);
        } finally {
            on = false;
            HEARD.clear();
        }
    }
}
