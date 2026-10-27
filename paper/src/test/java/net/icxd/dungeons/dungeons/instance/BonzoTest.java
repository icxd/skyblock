package net.icxd.dungeons.dungeons.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.mob.MobKind;
import net.icxd.dungeons.mob.MobKinds;

class BonzoTest {
    /** His name tag's health as recorded over three fights (the health is what the same packets' metadata had). */
    @Test
    void healthLikeHypixel() {
        assertEquals("250k", Bonzo.shortHealth(250_000));
        assertEquals("236.4k", Bonzo.shortHealth(236_449.84375));
        assertEquals("222.1k", Bonzo.shortHealth(222_121.203125));
        assertEquals("217k", Bonzo.shortHealth(217_031.484375));
        assertEquals("198.6k", Bonzo.shortHealth(198_572.703125));
        assertEquals("178k", Bonzo.shortHealth(178_018.359375));
        assertEquals("172.6k", Bonzo.shortHealth(172_555.09375));
        assertEquals("121k", Bonzo.shortHealth(120_966.8203125));
        assertEquals("97.6k", Bonzo.shortHealth(97_605));
        assertEquals("249.3k", Bonzo.shortHealth(249_280.71875));
        assertEquals("2.8k", Bonzo.shortHealth(2_814.38671875));
        assertEquals("0", Bonzo.shortHealth(0));
        assertEquals("0", Bonzo.shortHealth(-5));
    }

    /** Recorded: "&e(Humanoid)&5(Arcane) &cBonzo&r &a250k&c❤", and Undead for Humanoid in his second phase. */
    @Test
    void nameTags() {
        assertEquals("&e✰&5♃ &cBonzo &a250k&c❤", Bonzo.nameTag(false, 250_000));
        assertEquals("&2༕&5♃ &cBonzo &a236.4k&c❤", Bonzo.nameTag(true, 236_449.8));
        assertEquals("&e✰&5♃ &cBonzo &e0&c❤", Bonzo.nameTag(false, 0));
        assertEquals("&2༕ &7Undead &a50,000&c❤", BonzoUndead.tagText(50_000));
        assertEquals("&2༕ &7Undead &a36,502&c❤", BonzoUndead.tagText(36_501.2));
        assertEquals("&2༕ &7Undead &e0&c❤", BonzoUndead.tagText(-20));
    }

    /** As recorded: the skull's with thousands grouped, the balloon's as Java prints 0.6 x 2,664. */
    @Test
    void hitMessages() {
        assertEquals("&7Bonzo's Wither Skull hit you for &c5,328&7 damage.", Bonzo.skullMessage());
        assertEquals("&7Bonzo's Balloon hit you for &c1598.3999999999999&7 damage.", Bonzo.balloonMessage());
    }

    @Test
    void kinds() {
        MobKind.Variant bonzo = MobKinds.BONZO.variant(DungeonFloor.FLOOR_1, null);
        assertEquals(250_000, bonzo.health());
        assertEquals(Bonzo.BASE_DAMAGE, bonzo.damage());
        assertEquals(1_657, bonzo.combatXp());
        MobKind.Variant undead = MobKinds.BONZO_UNDEAD.variant(DungeonFloor.FLOOR_1, null);
        assertEquals(50_000, undead.health());
        assertEquals(1_440, undead.damage());
        assertEquals(148, undead.combatXp());
    }

    /** The recorded gaps: "Oh I'm dead!" a second after, "Sike" 3.1 s later, back 1.1 s after that. */
    @Test
    void timings() {
        assertEquals(20, Bonzo.DEAD_LINE_AFTER);
        assertEquals(62, Bonzo.SIKE_AFTER - Bonzo.DEAD_LINE_AFTER);
        assertEquals(22, Bonzo.BACK_AFTER - Bonzo.SIKE_AFTER);
        // 56 balloons in the barrage, every 3 ticks, as recorded.
        assertEquals(56, (Bonzo.BARRAGE_TO - Bonzo.BARRAGE_FROM) / Bonzo.BARRAGE_EVERY);
        // The pillar 2 s after the summary, the tunnel's door 2 s after that.
        assertEquals(40, Bonzo.PILLAR_AFTER - Bonzo.SUMMARY_AFTER);
        assertEquals(40, Bonzo.TUNNEL_AFTER - Bonzo.PILLAR_AFTER);
        assertEquals(3, Bonzo.INTRO.size());
        assertEquals(13, Bonzo.HURT_LINES.size());
    }

    /** The seven pillars, as captured, and a block's room round each. */
    @Test
    void pillars() {
        assertEquals(7, Bonzo.PILLARS.length);
        assertTrue(Bonzo.inPillar(new Location(null, -42.5, 71, 5.5)));
        assertTrue(Bonzo.inPillar(new Location(null, -27.2, 71, 21.9)));
        assertFalse(Bonzo.inPillar(new Location(null, -42.5, 71, 20.5)));
        assertFalse(Bonzo.inPillar(new Location(null, -42.5, 71, 34.5)));
    }
}
