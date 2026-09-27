package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

/** The numbers weapon abilities take from their text and the wiki. */
class WeaponRulesTest {
    /** "Each shot costs +30 mana more than the previous, resetting after 4s of not firing." */
    @Test
    void jerryGunCost() {
        assertEquals(30, JerryGun.cost(0));
        assertEquals(60, JerryGun.cost(1));
        assertEquals(120, JerryGun.cost(3));
        assertEquals(3, JerryGun.shotsInARow(3, 10_000, 13_999));
        assertEquals(0, JerryGun.shotsInARow(3, 10_000, 14_000));
    }

    /** "Consecutive throws cost 2x more Mana and deal 2x more damage than the previous. (max 16x)" */
    @Test
    void halberdDoubles() {
        assertEquals(List.of(1, 2, 4, 8, 16, 16), List.of(ThrownBlade.multiplier(0), ThrownBlade.multiplier(1), ThrownBlade.multiplier(2),
                ThrownBlade.multiplier(3), ThrownBlade.multiplier(4), ThrownBlade.multiplier(9)));
    }

    /** "doubled on the first bounce and tripled on the final bounce"; the barrage's +2% a 10% of health missing. */
    @Test
    void roses() {
        assertEquals(1, Roses.bounceFactor(0));
        assertEquals(2, Roses.bounceFactor(1));
        assertEquals(3, Roses.bounceFactor(2));
        assertEquals(1, Roses.missingHealthFactor(1_000, 1_000), 1e-9);
        assertEquals(1.1, Roses.missingHealthFactor(500, 1_000), 1e-9);
        assertEquals(1.18, Roses.missingHealthFactor(50, 1_000), 1e-9);
        assertEquals(1.2, Roses.missingHealthFactor(0, 1_000), 1e-9);
        // 9% missing is no whole 10%.
        assertEquals(1, Roses.missingHealthFactor(910, 1_000), 1e-9);
    }

    /** The first 3 take its melee damage, then it's halved for each one after; the curve rises 2 blocks over 13. */
    @Test
    void flay() {
        assertEquals(List.of(1.0, 1.0, 1.0, 0.5, 0.25), List.of(Flay.share(0), Flay.share(1), Flay.share(2), Flay.share(3), Flay.share(4)));
        assertEquals(0, Flay.height(0), 1e-9);
        assertEquals(2, Flay.height(6.5), 1e-9);
        assertEquals(0, Flay.height(13), 1e-9);
    }

    /** All of it at the middle, none at the edge; the lob lands where it's aimed, as Missile moves it. */
    @Test
    void terrainToss() {
        assertEquals(1, TerrainToss.falloff(0, 5), 1e-9);
        assertEquals(0.5, TerrainToss.falloff(2.5, 5), 1e-9);
        assertEquals(0, TerrainToss.falloff(6, 5), 1e-9);
        Vector from = new Vector(0, 65.6, 0);
        Vector to = new Vector(20, 64, 7);
        int ticks = 17;
        Vector velocity = TerrainToss.lob(from, to, ticks, 0.08);
        Vector at = from.clone();
        for (int i = 0; i < ticks; i++) {
            velocity.setY(velocity.getY() - 0.08);
            at.add(velocity);
        }
        assertEquals(0, at.distance(to), 1e-9);
    }

    /** "Charges: 4 / 5s": four, and each back 5 seconds after it was used. */
    @Test
    void charges() {
        Charges charges = new Charges(4, 5_000);
        UUID player = UUID.randomUUID();
        for (int i = 0; i < 4; i++) assertTrue(charges.use(player, 1_000 + i * 100));
        assertFalse(charges.use(player, 1_400));
        assertEquals(0, charges.left(player, 5_999));
        assertEquals(1, charges.left(player, 6_000));
        assertEquals(4, charges.left(player, 6_300));
        assertEquals(4, charges.left(UUID.randomUUID(), 0));
    }

    /** Reaving Strike's share of melee damage, off its text. */
    @Test
    void reavingStrikeShare() {
        assertEquals(1.25, ReavingStrike.share(List.of("&7Slash in a huge arc, dealing &c125%", "&7melee damage &7to all enemies hit!")), 1e-9);
        assertEquals(1.35, ReavingStrike.share(List.of("&7dealing &c135% &7melee damage to all enemies hit!")), 1e-9);
        assertEquals(1, ReavingStrike.share(List.of("&7Something else.")), 1e-9);
    }
}
