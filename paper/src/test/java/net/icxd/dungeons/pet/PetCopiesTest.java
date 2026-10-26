package net.icxd.dungeons.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/** What the items that copy a pet's stats say they copy (their lines as the items data has them). */
class PetCopiesTest {
    @Test
    void theirTexts() {
        PetCopies.Copier claw = PetCopies.copier("&7Copies &a10% &7of your active pet's &cCombat &7stats. &eRight-click to equip!");
        assertEquals(10, claw.combatShare(), 1e-9);
        assertFalse(claw.baseCombat());
        PetCopies.Copier blade = PetCopies.copier("&7Gains &c+4❁ Damage &7per Taming level &7and copies the base &cCombat &7stats of "
                + "&7your active pet. &7Grants &c+1% ❁ Damage &7and &b+0.2✯ &bMagic Find &7on &2✿ Mythological &7mobs");
        assertEquals(4, blade.perTaming(), 1e-9);
        assertTrue(blade.baseCombat());
        assertEquals(0, blade.combatShare(), 1e-9);
        assertEquals("Bee", PetCopies.copier("&aDouble &7the base pet stats of your &7active &6Bee &7pet. &8&oBuzz, buzz!").doubles());
        assertNull(PetCopies.copier("&7Grants &c+30❁ Strength&7.").doubles());
    }

    @Test
    void shares() {
        Stats pet = new Stats().set(Stat.STRENGTH, 50).set(Stat.FARMING_FORTUNE, 30);
        Stats to = new Stats();
        PetCopies.share(to, pet, PetCopies.COMBAT, 10);
        assertEquals(5, to.get(Stat.STRENGTH), 1e-9);
        assertFalse(to.has(Stat.FARMING_FORTUNE), "not a Combat stat");
        Stats all = new Stats();
        PetCopies.share(all, pet, null, 60);
        assertEquals(List.of(30.0, 18.0), List.of(all.get(Stat.STRENGTH), all.get(Stat.FARMING_FORTUNE)));
    }
}
