package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/** What's registered, against the item data; and the numbers the last abilities take from their text. */
class WeaponAbilitiesTest {
    private static List<String> registered() {
        List<String> names = new ArrayList<>();
        WeaponAbilities.register((name, handler) -> names.add(name));
        return names;
    }

    @Test
    void oneHandlerAName() {
        List<String> names = registered();
        assertEquals(names.size(), new HashSet<>(names).size(), "a name registered twice: " + names);
    }

    /**
     * Every name is an ability some item's data has (so a typo doesn't leave one unused), but Wither Impact,
     * which Necron's Blade makes of the three scrolls. Needs the private items.json (-Ditems.file).
     */
    @Test
    void everyNameIsAnItemsAbility() throws IOException {
        String property = System.getProperty("items.file");
        assumeTrue(property != null && Files.exists(Path.of(property)), "no items.json");
        Set<String> abilities = new HashSet<>();
        try (Reader reader = Files.newBufferedReader(Path.of(property))) {
            for (DataItem item : ItemData.load(reader).items().values()) {
                for (ItemBlock block : item.blocks()) if (block.isAbility()) abilities.add(block.name());
            }
        }
        Set<String> missing = new TreeSet<>(registered());
        missing.removeAll(abilities);
        missing.remove("Wither Impact");
        assertTrue(missing.isEmpty(), "not an ability of any item: " + missing);
    }

    @Test
    void numbersFromText() {
        assertEquals(3, Bows.mobsOnImpact(List.of("&7Hits &c3 &7mobs on impact.", "&7Can damage endermen.")));
        assertEquals(0, Bows.mobsOnImpact(List.of("&7Shoots &b3 &7arrows at once.")));
        assertEquals(100_000, Bolts.number(List.of("&7Coin Cost: &6100,000"), Pattern.compile("Coin Cost: (\\d+)")), 1e-9);
        assertEquals(-1, Bolts.number(List.of("&7nothing"), Pattern.compile("Coin Cost: (\\d+)")), 1e-9);
    }

    /** The Staff of the Rising Sun's share of their max mana, less 1% a Mana Disintegrator (it's a wand: 0.20.5). */
    @Test
    void rayOfHopesCost() {
        ItemBlock ray = new ItemBlock("ABILITY", "Ray of Hope", null, "RIGHT_CLICK", List.of("&7Costs &b10% &7of your total mana to use."),
                0, 0, 0, 0, 0, 0, 0);
        assertEquals(100, Bolts.RayOfHope.cost(ray, new NBTTagCompound(), 1_000));
        NBTTagCompound ten = new NBTTagCompound();
        ten.setInt(ItemModifiers.MANA_DISINTEGRATORS, 10);
        assertEquals(90, Bolts.RayOfHope.cost(ray, ten, 1_000));
    }

    /** 10% of their health, at most 500. */
    @Test
    void enderWarp() {
        assertEquals(100, Bows.EnderWarp.damage(1_000), 1e-9);
        assertEquals(500, Bows.EnderWarp.damage(1_000_000), 1e-9);
    }

    /** "+5 Strength for every 5% of total HP you have ... Capped at +100". */
    @Test
    void badHealth() {
        assertEquals(100, Buffs.BadHealth.strength(1_000, 1_000), 1e-9);
        assertEquals(50, Buffs.BadHealth.strength(500, 1_000), 1e-9);
        assertEquals(45, Buffs.BadHealth.strength(499, 1_000), 1e-9);
        assertEquals(0, Buffs.BadHealth.strength(40, 1_000), 1e-9);
    }

    /** Each recast 1.5 times the last's mana; the zap's damage all there to 10 blocks, then decaying. */
    @Test
    void sinrecallAndRunicZap() {
        assertEquals(1, Strikes.SinrecallTransmission.costFactor(0), 1e-9);
        assertEquals(1.5, Strikes.SinrecallTransmission.costFactor(1), 1e-9);
        assertEquals(2.25, Strikes.SinrecallTransmission.costFactor(2), 1e-9);
        assertEquals(1, Bolts.RunicZap.decay(10), 1e-9);
        assertEquals(Math.exp(-0.0225 * 10), Bolts.RunicZap.decay(20), 1e-9);
    }
}
