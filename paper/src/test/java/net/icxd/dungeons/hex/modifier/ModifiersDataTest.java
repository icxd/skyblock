package net.icxd.dungeons.hex.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Modifiers page with the real items (the private items.json: -Ditems.file): every modifier it offers is an
 * item there, and the items the wiki names get theirs. Skipped without it.
 */
class ModifiersDataTest {
    @BeforeAll
    static void items() {
        Path items = PrivateData.itemsFile();
        assumeTrue(Files.isRegularFile(items), "no " + items);
        assertNull(ItemRegistry.loadData(items).failure(), "the items didn't load");
    }

    private static HexItem hex(String id, int stars) {
        SkyBlockItem item = ItemRegistry.get(id);
        assertNotNull(item, id);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        tag.setInt("upgrade_count", stars);
        return new HexItem(item, tag, null);
    }

    private static List<String> ids(String id) {
        return HexModifiers.offers(hex(id, 0)).stream().map(HexModifiers.Offer::itemId).toList();
    }

    @Test
    void everyOfferIsAnItem() {
        Set<String> offered = new LinkedHashSet<>();
        for (SkyBlockItem item : ItemRegistry.getRegistry().values()) {
            for (int stars : new int[] {0, 5}) {
                for (HexModifiers.Offer offer : HexModifiers.offers(hex(item.id(), stars))) offered.add(offer.itemId());
            }
        }
        for (String id : offered) assertNotNull(ItemRegistry.get(id), id);
        // Each kind of modifier is offered somewhere (Silex needs Efficiency V, which no new item has).
        for (String id : List.of("RECOMBOBULATOR_3000", "FIRST_MASTER_STAR", "IMPLOSION_SCROLL", "TRANSMISSION_TUNER", "WOOD_SINGULARITY",
                "MANA_DISINTEGRATOR", "JALAPENO_BOOK", "RUBY_POWER_SCROLL", "TALISMAN_ENRICHMENT_MAGIC_FIND")) {
            assertTrue(offered.contains(id), id);
        }
    }

    @Test
    void theWikisItems() {
        // The Transmission Tuner's: the Aspects of the End and Void, the Etherwarp Conduit and the Sinseeker Scythe.
        for (String id : List.of("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID", "ETHERWARP_CONDUIT", "SINSEEKER_SCYTHE")) {
            assertTrue(ids(id).contains("TRANSMISSION_TUNER"), id);
        }
        // The Mana Disintegrator's, a few of them.
        for (String id : List.of("RADIANT_POWER_ORB", "WARNING_FLARE", "FIRE_VEIL_WAND", "GYROKINETIC_WAND")) {
            assertTrue(ids(id).contains("MANA_DISINTEGRATOR"), id);
        }
        assertTrue(ids("HYPERION").containsAll(List.of("IMPLOSION_SCROLL", "SHADOW_WARP_SCROLL", "WITHER_SHIELD_SCROLL")));
        assertFalse(ids("LIVID_DAGGER").contains("IMPLOSION_SCROLL"));
        // Power Scrolls on a RIGHT CLICK ability, but not the Egglocator's (0.20.5) or a Wither Scroll's own.
        assertTrue(ids("ASPECT_OF_THE_END").contains("RUBY_POWER_SCROLL"));
        for (String id : List.of("EGGLOCATOR", "IMPLOSION_SCROLL", "SHADOW_WARP_SCROLL", "WITHER_SHIELD_SCROLL")) {
            assertFalse(ids(id).contains("RUBY_POWER_SCROLL"), id);
        }
        // The official screenshot's five-star Livid Dagger: Recombobulator 3000, Master Star, Power Scroll.
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &cMaster Star &c✖", "  &7Power Scroll &c✖"),
                HexModifiers.summary(hex("LIVID_DAGGER", 5)));
    }

    @Test
    void theEntriesNames() {
        List<HexModifiers.Offer> offers = HexModifiers.offers(hex("LIVID_DAGGER", 5));
        // The Recombobulator in its rarity's colour (the wiki's "&6Recombobulator 3000"), a Power Scroll in its gem's.
        assertEquals("&6Recombobulator 3000", ModifiersPage.name(offers.get(0)));
        assertEquals("&cRuby Power Scroll", ModifiersPage.name(offers.get(2)));
        assertFalse(ModifiersPage.description(offers.get(0).itemId()).isEmpty());
    }
}
