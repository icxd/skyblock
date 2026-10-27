package net.icxd.dungeons.pet.ability;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.pet.PetData.Block;
import net.icxd.dungeons.pet.PetData.Formula;
import net.icxd.dungeons.pet.PetData.Variant;

/** How an ability's text finds its numbers in a block's: written ones, the block's {n}s, and where they are. */
class PetAbilityTextTest {
    @Test
    void numbers() {
        Pattern pattern = PetAbilities.pattern("Deal +#% damage against targets with no other mobs within # blocks.");
        PetAbilities.Slot[] slots = PetAbilities.slots(pattern, List.of("&7Deal &a+{2}% &7damage against targets with no other", "&7mobs within &a15 &7blocks."));
        assertArrayEquals(new PetAbilities.Slot[] {new PetAbilities.Slot(2, 0), new PetAbilities.Slot(-1, 15)}, slots);
        assertNull(PetAbilities.slots(pattern, List.of("&7Deal &a+{2}% &7damage to everything.")), "not its text");
        // A * is anything (a symbol the data has two ways).
        PetAbilities.Slot[] any = PetAbilities.slots(PetAbilities.pattern("Grants +#* Mining Speed when mining Mithril."),
                List.of("&7Grants &a+{0}&6☘ Mining Speed &7when mining &2Mithril&7."));
        assertEquals(1, any.length);
        assertEquals(PetAbilities.slots(PetAbilities.pattern(""), List.of("Whatever.")).length, 0, "no numbers");
    }

    @Test
    void valuesAtALevel() {
        Variant variant = new Variant(Rarity.LEGENDARY, null, "t", Map.of(), List.of(new Formula(0, 0, 0), new Formula(0, 0, 0), new Formula(0, 1, 0)),
                List.of(), false);
        assertEquals(37, new PetAbilities.Slot(2, 0).at(variant, 37), 1e-9);
        assertEquals(15, new PetAbilities.Slot(-1, 15).at(variant, 37), 1e-9);
        assertEquals(0, new PetAbilities.Slot(9, 0).at(variant, 37), "past its numbers");
    }

    @Test
    void unnamedBlocksAfter() {
        Variant variant = new Variant(Rarity.COMMON, null, "t", Map.of(), List.of(),
                List.of(new Block("&6Kill Combo", List.of("&7Gain buffs.")), new Block(null, List.of("&85 Combo (lasts &a{0}s&8)")),
                        new Block("&6Next", List.of("&7Other."))), false);
        assertEquals(List.of("&7Gain buffs.", "&85 Combo (lasts &a{0}s&8)"), PetAbilities.text(variant, 0));
        assertEquals("Kill Combo", PetAbilities.name(variant.blocks().get(0)));
        assertEquals("", PetAbilities.name(variant.blocks().get(1)));
    }

    @Test
    void registered() {
        assertTrue(PetAbilities.all().size() > 100, "the abilities are there");
    }
}
