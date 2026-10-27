package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** What the Pet Items' lines (as the items data has them) say they do. */
class HeldItemsTest {
    private static HeldItems.Effect parse(String... lines) {
        return HeldItems.parse(List.of(lines), true);
    }

    @Test
    void grantsEveryStatItNames() {
        // Spooky Cupcake: two stats, the second wrapped onto the next line.
        HeldItems.Effect cupcake = parse("&7Grants &c+30❁ Strength&7 and &f+20✦", "&fSpeed&7.");
        assertEquals(30, cupcake.grants().get(Stat.STRENGTH), 1e-9);
        assertEquals(20, cupcake.grants().get(Stat.SPEED), 1e-9);
        // Dwarf Turtle Shelmet: the stat, and no knockback.
        HeldItems.Effect shelmet = parse("&7Grants &f+10❂ True Defense &7and", "&7makes the pet's owner immune to", "&7knockback.");
        assertEquals(10, shelmet.grants().get(Stat.TRUE_DEFENSE), 1e-9);
        assertTrue(shelmet.noKnockback());
        // Rubber Snorkel: no "+", and the doubling is only sometimes.
        assertEquals(5, parse("&7Grants &35⚶ Respiration&7, doubled while", "&7on &2Foraging Islands&7.").grants().get(Stat.RESPIRATION), 1e-9);
        // Burnt Texts: what it takes away.
        Stats burnt = parse("&7Grants &9+5⚓ Double Hook Chance&7, but", "&7reduces your &b☂ Fishing Speed &7by &c50&7.").grants();
        assertEquals(5, burnt.get(Stat.DOUBLE_HOOK_CHANCE), 1e-9);
        assertEquals(-50, burnt.get(Stat.FISHING_SPEED), 1e-9);
    }

    @Test
    void notWhatIsOnlySometimes() {
        // Titanium Minecart, and Grandma's Knitting Needle, whose Magic Find is its combo's.
        assertFalse(parse("&7Grants &6+33.3☘ Mining Fortune &7while", "&7mining &fTitanium Ore&7.").grants().has(Stat.MINING_FORTUNE));
        HeldItems.Effect needle = parse("&7Can be applied to &fGrandma Wolf &7pets.", "&7Grants &b+5% ✯ Magic Find &7when",
                "&7getting a combo of &a50 &7or above.");
        assertFalse(needle.grants().has(Stat.MAGIC_FIND));
        assertEquals(50, needle.comboAt());
        assertEquals(5, needle.combo().get(Stat.MAGIC_FIND), 1e-9);
        assertEquals("Grandma Wolf", needle.onlyFor());
        // "+2,000 coins when digging" is no stat; the Four-Eyed Fish's two are.
        HeldItems.Effect fish = parse("&7This pet item may only be used on a", "&6Griffin &7pet.", "", "&7Gain &6+2,000 coins &7when digging up",
                "&7any &eGriffin Burrow&7.", "", "&7Grants &b+13✯ Magic Find&7 and &a+55❈", "&aDefense&7.");
        assertEquals("Griffin", fish.onlyFor());
        assertEquals(13, fish.grants().get(Stat.MAGIC_FIND), 1e-9);
        assertEquals(55, fish.grants().get(Stat.DEFENSE), 1e-9);
        assertFalse(fish.grants().has(Stat.TRUE_DEFENSE));
    }

    @Test
    void sharesOfThePetsStats() {
        HeldItems.Effect claws = parse("&7Increases the pet's &9☠ Crit Damage", "&7by &a50% &7and &9☣ Crit Chance &7by &a50%&7.");
        assertEquals(Map.of(Stat.CRIT_DAMAGE, 50.0, Stat.CRIT_CHANCE, 50.0), claws.shares());
        assertEquals(Map.of(Stat.STRENGTH, 100.0), parse("&7Increases this pet's &c❁ Strength &7by", "&a100%&7.").shares());
        HeldItems.Effect relic = parse("&7Increases all pet stats by &a33.3%&7.");
        assertEquals(33.3, relic.allShare(), 1e-9);
    }

    @Test
    void theRest() {
        HeldItems.Effect claw = parse("&7Every 2 pet levels, you gain &6+1⸕", "&6Mining Speed &7and &6+1☘ Mining Fortune&7.");
        assertEquals(2, claw.everyLevels());
        assertEquals(1, claw.perLevels().get(Stat.MINING_SPEED), 1e-9);
        assertEquals(1, claw.perLevels().get(Stat.MINING_FORTUNE), 1e-9);
        HeldItems.Effect gem = parse("&7Gain &c8⫽ Ferocity &7for &a5s &7on kill.");
        assertEquals(8, gem.onKill().get(Stat.FEROCITY), 1e-9);
        assertEquals(5_000, gem.onKillMillis());
        assertFalse(gem.grants().has(Stat.FEROCITY));
        assertEquals(50, parse("&7Grants &f+50 Max Speed Cap&7.").speedCap(), 1e-9);
        assertEquals(1, parse("&7Boosts the &ararity &7of your pet by 1", "&7tier!").tiers());
        assertEquals(15, parse("&7While equipped in an Exp Share slot", "&7this pet gains &a+15% &7Exp Share rate.").expShare(), 1e-9);
        HeldItems.Effect kat = HeldItems.parse(List.of("&7Use at &bKat &7to upgrade &ePig Pets &7from", "&fCommon &7to &aUncommon."), false);
        assertFalse(kat.appliable());
        assertNull(kat.onlyFor());
    }

    @Test
    void whatItGivesAPet() {
        // A Minos Relic's third more of each of the pet's stats; Gold Claws' half more of two of them; Quick Claw's by level.
        Stats pet = new Stats().set(Stat.STRENGTH, 30).set(Stat.CRIT_DAMAGE, 50);
        HeldItems.Effect relic = parse("&7Increases all pet stats by &a33.3%&7.");
        assertEquals(9.99, HeldItems.granted(relic, pet, 100).get(Stat.STRENGTH), 1e-9);
        HeldItems.Effect claws = parse("&7Increases the pet's &9☠ Crit Damage", "&7by &a50% &7and &9☣ Crit Chance &7by &a50%&7.");
        Stats granted = HeldItems.granted(claws, pet, 100);
        assertEquals(25, granted.get(Stat.CRIT_DAMAGE), 1e-9);
        assertFalse(granted.has(Stat.STRENGTH), "none of what the pet doesn't have, nor of the others");
        HeldItems.Effect claw = parse("&7Every 2 pet levels, you gain &6+1⸕", "&6Mining Speed &7and &6+1☘ Mining Fortune&7.");
        assertEquals(49, HeldItems.granted(claw, new Stats(), 99).get(Stat.MINING_FORTUNE), 1e-9);
    }
}
