package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static net.icxd.dungeons.item.enchanting.weapon.WeaponStatsTest.enchanted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An item's levels of the weapon enchantments, and the tiers that grow with its count. */
class WeaponEnchantTest {
    @AfterEach
    void reset() {
        WeaponTexts.reset();
    }

    @Test
    void levelsFromTheItemsData() {
        NBTTagCompound tag = enchanted("TEST", "cleave", "5", "ultimate_soul_eater", "4", "syphon", "3", "sharpness", "7");
        WeaponEnchant.Levels levels = WeaponEnchant.levels(tag);
        assertEquals(5, levels.of(WeaponEnchant.CLEAVE));
        // Hypixel's id for an ultimate, and Drain as items store it.
        assertEquals(4, levels.of(WeaponEnchant.SOUL_EATER));
        assertEquals(3, levels.of(WeaponEnchant.DRAIN));
        assertFalse(levels.has(WeaponEnchant.VENOMOUS));
        assertFalse(levels.none());
        assertTrue(WeaponEnchant.levels(enchanted("TEST", "sharpness", "7")).none());
        assertTrue(WeaponEnchant.levels(null).none());
        assertEquals(4, WeaponEnchant.SOUL_EATER.on(tag));
        assertEquals(0, WeaponEnchant.REND.on(null));
    }

    @Test
    void settingALevel() {
        NBTTagCompound tag = enchanted("TEST", "champion", "1");
        assertTrue(WeaponEnchant.CHAMPION.setOn(tag, 3));
        assertEquals(3, WeaponEnchant.CHAMPION.on(tag));
        assertFalse(WeaponEnchant.TOXOPHILITE.setOn(tag, 3));
    }

    /** Champion counts Combat XP on the item, under Hypixel's key, and goes up a tier at each "to tier up!" (several at once). */
    @Test
    void championTiersUp() {
        WeaponTexts.use();
        NBTTagCompound tag = enchanted("TEST", "champion", "1");
        assertTrue(WeaponEnchants.tierUp(tag, WeaponEnchant.CHAMPION, WeaponEnchants.CHAMPION_XP, 49_999));
        assertEquals(1, WeaponEnchant.CHAMPION.on(tag));
        WeaponEnchants.tierUp(tag, WeaponEnchant.CHAMPION, WeaponEnchants.CHAMPION_XP, 1);
        assertEquals(2, WeaponEnchant.CHAMPION.on(tag));
        WeaponEnchants.tierUp(tag, WeaponEnchant.CHAMPION, WeaponEnchants.CHAMPION_XP, 5_000_000);
        // The table's last tier (III) is as high as it goes.
        assertEquals(3, WeaponEnchant.CHAMPION.on(tag));
        assertEquals(5_050_000, ItemCounters.get(tag, WeaponEnchants.CHAMPION_XP), 1e-9);
        // Without it on the item, nothing counts.
        assertFalse(WeaponEnchants.tierUp(tag, WeaponEnchant.TOXOPHILITE, WeaponEnchants.TOXOPHILITE_XP, 1000));
        assertEquals(0, ItemCounters.get(tag, WeaponEnchants.TOXOPHILITE_XP));
    }
}
