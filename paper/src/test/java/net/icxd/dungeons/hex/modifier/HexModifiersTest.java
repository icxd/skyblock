package net.icxd.dungeons.hex.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexCategories;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.modifier.HexModifiers.Offer;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/** What the Hex's Modifiers offer which items, on made-up items, and what applying each does to the data. */
class HexModifiersTest {
    private static final String STAR = "[{\"amount\":10,\"essence\":\"WITHER\"}]";
    private static final String BLINK = "{\"activation\":\"RIGHT_CLICK\",\"header\":\"&6Ability: Instant Transmission  &e&lRIGHT CLICK\","
            + "\"kind\":\"ABILITY\",\"mana\":45,\"name\":\"Instant Transmission\",\"text\":[\"&7Teleport &a8 blocks&7 ahead.\"]}";

    private static DataItem item(String id, String fields) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{\"" + id + "\":{\"material\":\"STICK\","
                    + "\"name\":\"Test " + id + "\"" + (fields.isEmpty() ? "" : "," + fields) + "}}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HexItem hex(SkyBlockItem item, NBTTagCompound tag) {
        tag.setString("id", item.id());
        return new HexItem(item, tag, null);
    }

    private static HexItem hex(SkyBlockItem item) {
        return hex(item, new NBTTagCompound());
    }

    private static List<String> ids(HexItem item) {
        return HexModifiers.offers(item).stream().map(Offer::itemId).toList();
    }

    /** The item after the first offer of this item id is applied. */
    private static HexItem applied(HexItem item, String id) {
        Offer offer = HexModifiers.offers(item).stream().filter(o -> o.itemId().equals(id)).findFirst().orElseThrow();
        assertFalse(offer.done(), id);
        return new HexItem(item.item(), offer.apply().apply(item.tag().copy()), null);
    }

    private static final List<String> SCROLLS = List.of("RUBY_POWER_SCROLL", "SAPPHIRE_POWER_SCROLL", "JASPER_POWER_SCROLL",
            "AMETHYST_POWER_SCROLL", "AMBER_POWER_SCROLL", "OPAL_POWER_SCROLL");

    @Test
    void aRecombobulatorForAnythingWithAType() {
        HexItem sword = hex(item("TEST_SWORD", "\"rarity\":\"EPIC\",\"type\":\"SWORD\""));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖"), HexModifiers.summary(sword));
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(sword));
        assertTrue(HexCategories.MODIFIERS.applies(sword));

        HexItem recombobulated = applied(sword, "RECOMBOBULATOR_3000");
        assertTrue(recombobulated.tag().getBoolean("recombobulated"));
        assertEquals(Rarity.LEGENDARY, recombobulated.rarity());
        assertEquals(List.of("  &6Recombobulator 3000 &a✔"), HexModifiers.summary(recombobulated));
        // Still listed, done.
        assertTrue(HexModifiers.offers(recombobulated).getFirst().done());

        // A reforge stone has a type too (the wiki: "any item with a named category").
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(hex(item("TEST_STONE", "\"type\":\"REFORGE_STONE\""))));
    }

    @Test
    void noRecombobulatorWithoutOne() {
        // No type.
        assertEquals(List.of(), HexModifiers.of(hex(item("TEST_THING", ""))));
        assertFalse(HexCategories.MODIFIERS.applies(hex(item("TEST_THING", ""))));
        // The item data says it can't be.
        assertEquals(List.of(), HexModifiers.of(hex(item("TEST_CLOAK", "\"type\":\"CLOAK\",\"can_recombobulate\":false"))));
        // Pet items can't (the wiki's Recombobulator 3000).
        assertEquals(List.of(), HexModifiers.of(hex(item("TEST_PET_ITEM", "\"type\":\"PET_ITEM\""))));
    }

    @Test
    void masterStarsOnAFiveStarDungeonItem() {
        DataItem dungeon = item("TEST_DUNGEON_SWORD", "\"dungeon_item\":true,\"rarity\":\"LEGENDARY\",\"type\":\"SWORD\",\"upgrade_costs\":[@,@,@,@,@]"
                .replace("@", STAR));
        NBTTagCompound three = new NBTTagCompound();
        three.setInt("upgrade_count", 3);
        // The line shows, but not a star to apply before the five.
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &cMaster Star &c✖"), HexModifiers.summary(hex(dungeon, three)));
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(hex(dungeon, three)));

        NBTTagCompound five = new NBTTagCompound();
        five.setInt("upgrade_count", 5);
        HexItem item = hex(dungeon, five);
        assertEquals(List.of("RECOMBOBULATOR_3000", "FIRST_MASTER_STAR"), ids(item));
        item = applied(item, "FIRST_MASTER_STAR");
        assertEquals(6, item.tag().getInt("upgrade_count"));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &cMaster Star &e1&7/&a5"), HexModifiers.summary(item));
        assertEquals(List.of("RECOMBOBULATOR_3000", "SECOND_MASTER_STAR"), ids(item));
        for (String next : List.of("SECOND_MASTER_STAR", "THIRD_MASTER_STAR", "FOURTH_MASTER_STAR", "FIFTH_MASTER_STAR")) item = applied(item, next);
        assertEquals(10, item.tag().getInt("upgrade_count"));
        assertEquals("  &cMaster Star &a5&7/&a5", HexModifiers.summary(item).get(1));
        Offer fifth = HexModifiers.offers(item).get(1);
        assertEquals("FIFTH_MASTER_STAR", fifth.itemId());
        assertTrue(fifth.done());

        // Not on an item that isn't a dungeon item, but on one made one (its data says so).
        DataItem plain = item("TEST_HELMET", "\"type\":\"HELMET\",\"upgrade_costs\":[@,@,@,@,@]".replace("@", STAR));
        NBTTagCompound starred = new NBTTagCompound();
        starred.setInt("upgrade_count", 5);
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(hex(plain, starred.copy())));
        starred.setBoolean("dungeon_item", true);
        assertEquals(List.of("RECOMBOBULATOR_3000", "FIRST_MASTER_STAR"), ids(hex(plain, starred)));
    }

    @Test
    void witherScrollsOnNecronsBlade() {
        // Its id is what makes it one (see ItemBehaviours).
        HexItem blade = hex(item("NECRON_BLADE", "\"dungeon_item\":true,\"rarity\":\"LEGENDARY\",\"type\":\"SWORD\""));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &5Wither Scrolls &e0&7/&a3"), HexModifiers.summary(blade));
        assertEquals(List.of("RECOMBOBULATOR_3000", "IMPLOSION_SCROLL", "SHADOW_WARP_SCROLL", "WITHER_SHIELD_SCROLL"), ids(blade));
        HexItem one = applied(blade, "SHADOW_WARP_SCROLL");
        assertTrue(one.tag().getBoolean("shadow_warp"));
        assertEquals("  &5Wither Scrolls &e1&7/&a3", HexModifiers.summary(one).get(1));
        assertTrue(HexModifiers.offers(one).get(2).done());
        assertFalse(HexModifiers.offers(one).get(1).done());
        // Not on another sword.
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(hex(item("TEST_SWORD", "\"type\":\"SWORD\""))));
    }

    @Test
    void tunersAndPowerScrollsOnATransmissionSword() {
        HexItem sword = hex(item("TEST_BLINK_SWORD", "\"abilities\":[" + BLINK + "],\"rarity\":\"EPIC\",\"type\":\"SWORD\""));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &5Transmission Tuner &e0&7/&a4", "  &7Power Scroll &c✖"),
                HexModifiers.summary(sword));
        List<String> expected = new ArrayList<>(List.of("RECOMBOBULATOR_3000", "TRANSMISSION_TUNER"));
        expected.addAll(SCROLLS);
        assertEquals(expected, ids(sword));

        HexItem tuned = sword;
        for (int i = 0; i < 4; i++) tuned = applied(tuned, "TRANSMISSION_TUNER");
        assertEquals(4, tuned.tag().getInt(ItemModifiers.TUNERS));
        assertEquals("  &5Transmission Tuner &a4&7/&a4", HexModifiers.summary(tuned).get(1));
        assertTrue(HexModifiers.offers(tuned).get(1).done());

        // One scroll at a time: another takes its place.
        HexItem ruby = applied(sword, "RUBY_POWER_SCROLL");
        assertEquals("RUBY_POWER_SCROLL", ruby.tag().getString(ItemModifiers.POWER_SCROLL));
        assertEquals("  &7Power Scroll &a✔", HexModifiers.summary(ruby).get(2));
        HexItem opal = applied(ruby, "OPAL_POWER_SCROLL");
        assertEquals("OPAL_POWER_SCROLL", opal.tag().getString(ItemModifiers.POWER_SCROLL));
        List<Offer> offers = HexModifiers.offers(opal);
        assertFalse(offers.get(2).done());
        assertTrue(offers.get(7).done());
        // Their names are in their gems' colours (the wiki's screen).
        assertEquals("&c", offers.get(2).colour());

        // The Aspect of the Leech takes one tuner.
        HexItem leech = hex(item("TEST_LEECH", "\"abilities\":[" + BLINK.replace("Instant Transmission", "Weird Transmission") + "],\"type\":\"WAND\""));
        assertEquals("  &5Transmission Tuner &e0&7/&a1", HexModifiers.summary(leech).get(1));
    }

    @Test
    void aPowerScrollWhereTheDataSays() {
        // No RIGHT CLICK ability, but the item data says it takes one.
        HexItem item = hex(item("TEST_SCROLLED", "\"can_have_power_scroll\":true"));
        assertEquals(List.of("  &7Power Scroll &c✖"), HexModifiers.summary(item));
        assertEquals(SCROLLS, ids(item));
    }

    @Test
    void noPowerScrollOnTheEgglocatorOrAWitherScroll() {
        String rightClick = "\"abilities\":[" + BLINK.replace("Instant Transmission", "Test Ability") + "]";
        assertEquals(SCROLLS, ids(hex(item("TEST_TRACKER", rightClick))));
        // 0.20.5: "Fixed Power Scrolls being applicable on Eggolocators".
        assertEquals(List.of(), HexModifiers.of(hex(item("EGGLOCATOR", rightClick))));
        // What's put on an item in an anvil (a Wither Scroll: its RIGHT CLICK is the one it gives).
        assertEquals(List.of(), HexModifiers.of(hex(item("TEST_WITHER_SCROLL", rightClick + ",\"categories\":[\"Combinable in Anvil\"]"))));
    }

    @Test
    void silexOnAPickaxeWithEfficiencyFive() {
        DataItem pickaxe = item("TEST_PICKAXE", "\"type\":\"PICKAXE\"");
        // Without Efficiency V, no Silex.
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(hex(pickaxe, ItemModifiers.withEfficiency(new NBTTagCompound(), 4))));
        HexItem five = hex(pickaxe, ItemModifiers.withEfficiency(new NBTTagCompound(), 5));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &5Silex &e0&7/&a5"), HexModifiers.summary(five));
        HexItem six = applied(five, "SIL_EX");
        assertEquals(6, ItemModifiers.efficiency(six.tag()));
        HexItem ten = hex(pickaxe, ItemModifiers.withEfficiency(new NBTTagCompound(), 10));
        assertEquals("  &5Silex &a5&7/&a5", HexModifiers.summary(ten).get(1));
        assertTrue(HexModifiers.offers(ten).get(1).done());
        // Drills and the Gemstone Gauntlet too, not the Stonk.
        assertEquals(2, ids(hex(item("TEST_DRILL", "\"type\":\"DRILL\""), ItemModifiers.withEfficiency(new NBTTagCompound(), 5))).size());
        assertEquals(2, ids(hex(item("TEST_GAUNTLET", "\"type\":\"GAUNTLET\""), ItemModifiers.withEfficiency(new NBTTagCompound(), 5))).size());
        assertEquals(List.of("RECOMBOBULATOR_3000"), ids(hex(item("STONK_PICKAXE", "\"type\":\"PICKAXE\""), ItemModifiers.withEfficiency(new NBTTagCompound(), 6))));
    }

    @Test
    void aWoodSingularityOnAnAxe() {
        HexItem axe = hex(item("TEST_AXE", "\"type\":\"AXE\""));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &6Wood Singularity &c✖"), HexModifiers.summary(axe));
        HexItem singular = applied(axe, "WOOD_SINGULARITY");
        assertEquals(1, singular.tag().getInt(ItemModifiers.WOOD_SINGULARITY));
        assertEquals("  &6Wood Singularity &a✔", HexModifiers.summary(singular).get(1));
    }

    @Test
    void disintegratorsAndJalapenoOnDeployables() {
        HexItem wand = hex(item("TEST_WAND", "\"type\":\"WAND\""));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &9Mana Disintegrator &e0&7/&a10"), HexModifiers.summary(wand));
        HexItem orb = hex(item("TEST_ORB", "\"abilities\":[" + BLINK.replace("Instant Transmission", "Deploy") + "],\"type\":\"DEPLOYABLE\""));
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &9Mana Disintegrator &e0&7/&a10", "  &5Jalapeno Book &c✖", "  &7Power Scroll &c✖"),
                HexModifiers.summary(orb));
        HexItem one = applied(orb, "MANA_DISINTEGRATOR");
        assertEquals(1, one.tag().getInt(ItemModifiers.MANA_DISINTEGRATORS));
        HexItem spicy = applied(one, "JALAPENO_BOOK");
        assertEquals(List.of("  &6Recombobulator 3000 &c✖", "  &9Mana Disintegrator &e1&7/&a10", "  &5Jalapeno Book &a✔", "  &7Power Scroll &c✖"),
                HexModifiers.summary(spicy));
    }

    @Test
    void enrichmentsOnLegendaryAccessories() {
        DataItem epic = item("TEST_TALISMAN", "\"rarity\":\"EPIC\",\"type\":\"ACCESSORY\"");
        assertEquals(List.of("  &6Recombobulator 3000 &c✖"), HexModifiers.summary(hex(epic)));
        // Recombobulated, it's Legendary: then they're offered, in the wiki's order, after the Recombobulator.
        HexItem legendary = applied(hex(epic), "RECOMBOBULATOR_3000");
        assertEquals(List.of("  &6Recombobulator 3000 &a✔", "  &7Enrichment &c✖"), HexModifiers.summary(legendary));
        assertEquals(List.of("RECOMBOBULATOR_3000", "TALISMAN_ENRICHMENT_WALK_SPEED", "TALISMAN_ENRICHMENT_INTELLIGENCE",
                "TALISMAN_ENRICHMENT_CRITICAL_DAMAGE", "TALISMAN_ENRICHMENT_CRITICAL_CHANCE", "TALISMAN_ENRICHMENT_STRENGTH",
                "TALISMAN_ENRICHMENT_DEFENSE", "TALISMAN_ENRICHMENT_HEALTH", "TALISMAN_ENRICHMENT_MAGIC_FIND", "TALISMAN_ENRICHMENT_FEROCITY",
                "TALISMAN_ENRICHMENT_SEA_CREATURE_CHANCE", "TALISMAN_ENRICHMENT_ATTACK_SPEED"), ids(legendary));
        HexItem enriched = applied(legendary, "TALISMAN_ENRICHMENT_MAGIC_FIND");
        assertEquals("magic_find", enriched.tag().getString(ItemModifiers.ENRICHMENT));
        assertEquals("  &7Enrichment &a✔", HexModifiers.summary(enriched).get(1));
        // One at a time.
        HexItem other = applied(enriched, "TALISMAN_ENRICHMENT_FEROCITY");
        assertEquals("ferocity", other.tag().getString(ItemModifiers.ENRICHMENT));
        assertTrue(HexModifiers.offers(other).get(9).done());
        assertFalse(HexModifiers.offers(other).get(8).done());
    }
}
