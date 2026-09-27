package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** What the utility abilities work out: heals, cooldowns, shares and durations (numbers from the items' text). */
class UtilityNumbersTest {
    private static String plain(String... lines) {
        return AbilityText.plain(List.of(lines));
    }

    /** Healing others is times the healer's Mending / 100; in the Catacombs all of it is times 1 + the boost. */
    @Test
    void healAmounts() {
        assertEquals(30, Heals.amount(30, false, 250, 0), 1e-9);
        // An orb's "Heal others for 15❤" from a healer with 150 Mending.
        assertEquals(22.5, Heals.amount(15, true, 150, 0), 1e-9);
        // Catacombs 21 (+195%): a wand's 60 on themselves.
        assertEquals(177, Heals.amount(60, false, 100, 1.95), 1e-9);
        assertEquals(66.375, Heals.amount(15, true, 150, 1.95), 1e-9);
        assertEquals(0, Heals.amount(-5, false, 100, 0), 1e-9);
    }

    /** "Heal 60❤ per second for 5s": 5 heals in all, the first at once. */
    @Test
    void healOverTimePulses() {
        assertEquals(4, Heals.pulses(5, true));
        assertEquals(5, Heals.pulses(5, false));
        assertEquals(0, Heals.pulses(1, true));
        assertEquals(0, Heals.pulses(0, true));
    }

    /** The four wands' texts since 0.26.1. */
    @Test
    void wands() {
        assertArrayEquals(new double[] {60, 5}, WandHeal.perSecondFor(plain("&7Heal &c60❤ &7per second for 5s.", "&8Wand heals don't stack.")));
        assertArrayEquals(new double[] {80, 5}, WandHeal.perSecondFor(plain("&7Heal &c80❤ &7per second for 5s.")));
        assertArrayEquals(new double[] {100, 6}, WandHeal.perSecondFor(plain("&7Heal &c100❤ &7per second for 6s.")));
        assertArrayEquals(new double[] {120, 6}, WandHeal.perSecondFor(plain("&7Heal &c120❤ &7per second for 6s.")));
        assertNull(WandHeal.perSecondFor(plain("&7Something else.")));
    }

    @Test
    void instantHeals() {
        assertEquals(new InstantHeal.Amounts(320, 7, 64),
                InstantHeal.amounts(plain("&7Heal for &c320❤ &7and heal players within &a7", "&7blocks for &c64❤&7.")));
        assertEquals(new InstantHeal.Amounts(500, 8, 100),
                InstantHeal.amounts(plain("&7Heal for &c500❤ &7and heal players within &a8", "&7blocks for &c100❤&7.")));
        // The Gloomlock Grimoire's Extreme Measures heals only them.
        assertEquals(new InstantHeal.Amounts(1000, 0, 0), InstantHeal.amounts(plain("&7Heal for &c1,000❤&7.")));
    }

    /** The fragged Spirit Mask's heal, and Bonzo's Mask's cooldown: 360 s, 3.6 less a Catacombs level. */
    @Test
    void lastStands() {
        String fragged = plain("&7Instead of dying, gain &f+50✦ Speed", "&7and damage immunity for &a3 &7seconds.",
                "&7Also heals you for &a10% &7of your &c❤", "&cHealth &7over &a5 &7seconds.");
        assertEquals(0.1, LastStand.healShare(fragged), 1e-9);
        assertEquals(5, LastStand.healSeconds(fragged));
        assertEquals(0, LastStand.healShare(plain("&7Instead of dying, gain &f+50✦ Speed")), 1e-9);
        assertEquals(360_000, LastStand.bonzoCooldownMillis(360, 0));
        assertEquals(270_000, LastStand.bonzoCooldownMillis(360, 25));
        assertEquals(180_000, LastStand.bonzoCooldownMillis(360, 50));
        assertEquals(180_000, LastStand.bonzoCooldownMillis(360, 80));
    }

    @Test
    void taunts() {
        String enrage = plain("&7Taunt enemies in a &a10 &7block radius and reduce", "&7their damage against you by &c10% &7for &a10s&7.");
        assertEquals(0.9, Taunt.factor(enrage), 1e-9);
        assertEquals(0, Taunt.manaShare(enrage), 1e-9);
        String bells = plain("&7Angers all monsters in a &a10 &7block", "&7range, consuming &b50% &7of your max", "&7mana and causing them to run");
        assertEquals(1, Taunt.factor(bells), 1e-9);
        assertEquals(0.5, Taunt.manaShare(bells), 1e-9);
    }

    @Test
    void lifeBloodCostsATenthOfMaxHealth() {
        assertEquals(0.1, TimedBuff.healthShare(plain("&7Use &c10% &7of your max health to boost your", "&7nearby allies by &c+30❁ Strength")), 1e-9);
        assertEquals(0, TimedBuff.healthShare(plain("&7Grants &f+100✦ Speed &7for &a30s&7.")), 1e-9);
    }

    /** "Cooldown is halved on deactivation". */
    @Test
    void veilCooldown() {
        assertEquals(10_000, CreeperVeil.cooldownAfter(10_000, false));
        assertEquals(5_000, CreeperVeil.cooldownAfter(10_000, true));
    }

    /** Buffs on their hits: Soulward's halving starts only once its 5 seconds are over, and lasts 2. */
    @Test
    void dealtFactors() {
        UUID player = UUID.randomUUID();
        Map<String, Long> until = new HashMap<>(Map.of("a", 1_000L));
        assertTrue(Protection.active(until, 999));
        assertFalse(Protection.active(until, 1_000));
        assertTrue(until.isEmpty());
        assertFalse(Protection.active(null, 0));
        assertEquals(1, Protection.dealtFactor(player, 0), 1e-9);
        Protection.dealt(player, Soulward.NAME, 0.5, 5_000, 2_000);
        assertEquals(1, Protection.dealtFactor(player, 4_999), 1e-9);
        assertEquals(0.5, Protection.dealtFactor(player, 5_000), 1e-9);
        assertEquals(0.5, Protection.dealtFactor(player, 6_999), 1e-9);
        assertEquals(1, Protection.dealtFactor(player, 7_000), 1e-9);
        Protection.forget(player);
    }
}
