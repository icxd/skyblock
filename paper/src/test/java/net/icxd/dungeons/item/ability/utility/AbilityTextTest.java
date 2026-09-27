package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** Reading abilities' numbers off their text, as items.json has it (these lines are the kind it has). */
class AbilityTextTest {
    private static String plain(String... lines) {
        return AbilityText.plain(List.of(lines));
    }

    @Test
    void colourCodesGo() {
        assertEquals("Grants +100✦ Speed for 30s.", plain("&7Grants &f+100✦ Speed &7for &a30s&7."));
        assertEquals("a b", plain("&7a", "  &8b "));
    }

    /** A stat by its symbol and name; symbols stats share (♨, ☘, ❁) go by the name after them. */
    @Test
    void stats() {
        String howl = plain("&7You and 4 nearby players gain:", "&7 &c+40❁ Strength", "&7 &e+5⚔ Attack Speed", "&7 &9+10☠ Crit Damage",
                "&7 &f+30✦ Speed", "&7for &a30 &7seconds.", "&8Effect doesn't stack.");
        assertEquals(new Stats().set(Stat.STRENGTH, 40).set(Stat.ATTACK_SPEED, 5).set(Stat.CRIT_DAMAGE, 10).set(Stat.SPEED, 30),
                AbilityText.stats(howl));
        String flare = plain("&5• &7Gain &4+25♨ Vitality.", "&5• &7Gain &f+30❂ True Defense&7.", "&5• &7Gain &c+10⫽ Ferocity&7.");
        assertEquals(new Stats().set(Stat.VITALITY, 25).set(Stat.TRUE_DEFENSE, 30).set(Stat.FEROCITY, 10), AbilityText.stats(flare));
        String lantern = plain("&6• &7Grants &6+100⸕ Mining Speed&7.", "&6• &7Grants &6+25☘ Mining Fortune&7.",
                "&6• &7Grants &c+20♨ Heat Resistance&7.", "&6• &7Grants &e+2.5▚ Gemstone Spread&7.");
        // Gemstone Spread isn't a stat here.
        assertEquals(new Stats().set(Stat.MINING_SPEED, 100).set(Stat.MINING_FORTUNE, 25).set(Stat.HEAT_RESISTANCE, 20),
                AbilityText.stats(lantern));
        assertEquals(new Stats().set(Stat.HEALTH_REGEN, 125), AbilityText.stats(plain("&6• &7Gain &c+125❣ Health Regen&7.")));
        // Bonzo's Mask has a space after the number.
        assertEquals(new Stats().set(Stat.STRENGTH, 20), AbilityText.stats(plain("&7immunity and &c+20 ❁ Strength &7for 3s")));
        // Heals and mana regen aren't stats.
        assertEquals(new Stats(), AbilityText.stats(plain("&7Heal yourself for &c30❤&7 per second.", "&7Grants &b+50% &7base mana regen.")));
    }

    @Test
    void howLong() {
        assertEquals(30_000, AbilityText.millis(plain("&7Grants &f+100✦ Speed &7for &a30s&7.")).orElseThrow());
        assertEquals(60_000, AbilityText.millis(plain("&7Place an orb for &a1m &7buffing up to &b5")).orElseThrow());
        assertEquals(180_000, AbilityText.millis(plain("&7Shoot the flare up in the sky for &a3m")).orElseThrow());
        assertEquals(20_000, AbilityText.millis(plain("&7for &a20 &7seconds.")).orElseThrow());
        assertEquals(1_500, AbilityText.millis(plain("&7For the next &a1.5s&7, reduce")).orElseThrow());
        // The first one: Second Wind's heal "over 5 seconds" isn't how long it lasts, and "for 10%" isn't a time.
        assertEquals(3_000, AbilityText.millis(plain("&7and damage immunity for &a3 &7seconds.", "&7Also heals you for &a10% &7of your")).orElseThrow());
        assertTrue(AbilityText.millis(plain("&7Heal for &c320❤")).isEmpty());
    }

    @Test
    void howFarAndHowMany() {
        String orb = plain("&7Place an orb for &a1m &7buffing up to &b5", "&7players within &a18 &7blocks.");
        assertEquals(18, AbilityText.blocks(orb).orElseThrow());
        assertEquals(5, AbilityText.players(orb).orElseThrow());
        assertEquals(10, AbilityText.blocks(plain("&7Taunt enemies in a &a10 &7block radius")).orElseThrow());
        assertEquals(10, AbilityText.blocks(plain("&7Angers all monsters in a &a10 &7block", "&7range")).orElseThrow());
        assertEquals(4, AbilityText.players(plain("&7You and 4 nearby players gain:")).orElseThrow());
    }

    @Test
    void numbersAfter() {
        assertEquals(1_000, AbilityText.after(plain("&7Heal for &c1,000❤&7."), "Heal for").orElseThrow());
        assertEquals(64, AbilityText.after(plain("&7Heal for &c320❤ &7and heal players within &a7", "&7blocks for &c64❤&7."), "blocks for").orElseThrow());
        assertEquals(20, AbilityText.after(plain("&7Max range of 20 blocks."), "Max range of").orElseThrow());
        assertTrue(AbilityText.after(plain("&7Heal yourself for &c30❤"), "Heal").isEmpty());
    }
}
