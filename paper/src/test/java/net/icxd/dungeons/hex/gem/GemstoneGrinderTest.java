package net.icxd.dungeons.hex.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexCategories;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexCosts.Check;
import net.icxd.dungeons.hex.HexCosts.Coins;
import net.icxd.dungeons.hex.HexCosts.Items;
import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.PrivateHex;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.gemstone.Gem;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemSlots.Slot;
import net.icxd.dungeons.item.gemstone.GemstoneQuality;
import net.icxd.dungeons.item.gemstone.GemstoneTable;
import net.icxd.dungeons.item.gemstone.GemstoneTable.ArmorSet;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;

/**
 * The Gemstone Grinder, its guide and the Hex's Gemstones, against the wiki's Geo/UI, the screenshots of the real
 * menu (a Hyperion, a Ring of Power) and the recreations, on made-up items.
 */
class GemstoneGrinderTest {
    /** Like a Hyperion: a Sapphire slot and a Combat one, each 250,000 coins and gems. */
    private static final DataItem SWORD = items("""
            "TEST_HYPERION":{"gemstone_slots":[{"costs":[{"coins":250000},{"amount":4,"item":"FLAWLESS_SAPPHIRE_GEM"}],"type":"SAPPHIRE"},\
            {"costs":[{"amount":1,"item":"FLAWLESS_JASPER_GEM"},{"coins":250000}],"type":"COMBAT"}],"material":"IRON_SWORD",\
            "name":"Test Hyperion","rarity":"LEGENDARY","type":"SWORD"}""").getFirst();
    /** The item names: a gem's rarity is its quality's colour. */
    private static final Function<String, String> NAMES = id -> Gem.of(id) == null ? "&f" + id : GemstoneGrinder.gemName(Gem.of(id));

    private static List<DataItem> items(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return new ArrayList<>(result.items().values());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void slotsCentredInTheFourthRow() {
        assertEquals(List.of(31), GemstoneGrinder.places(1));
        // The screenshots: a Hyperion's two in 30 and 32, a Ring of Power's four in 29, 30, 32 and 33.
        assertEquals(List.of(30, 32), GemstoneGrinder.places(2));
        assertEquals(List.of(30, 31, 32), GemstoneGrinder.places(3));
        assertEquals(List.of(29, 30, 32, 33), GemstoneGrinder.places(4));
        assertEquals(List.of(29, 30, 31, 32, 33), GemstoneGrinder.places(5));
        assertEquals(List.of(28, 29, 30, 32, 33, 34), GemstoneGrinder.places(6));
        assertEquals(List.of(28, 29, 30, 31, 32, 33, 34), GemstoneGrinder.places(7));
        // The Relic of Power's twelve: the empty grinder's panes.
        List<Integer> twelve = new ArrayList<>();
        for (int slot : GemstoneGrinder.PLACEHOLDERS) twelve.add(slot);
        assertEquals(twelve, GemstoneGrinder.places(12));
        // UNKNOWN on Hypixel: 8 to 11, the fourth row full and the rest centred in the fifth.
        assertEquals(List.of(28, 29, 30, 31, 32, 33, 34, 40), GemstoneGrinder.places(8));
        assertEquals(List.of(28, 29, 30, 31, 32, 33, 34, 38, 39, 41, 42), GemstoneGrinder.places(11));
    }

    @Test
    void aLockedSlot() {
        Slot sapphire = GemSlots.of(SWORD, new NBTTagCompound()).getFirst();
        HexCosts cost = GemstoneGrinder.unlockCost(sapphire);
        List<Check> checks = cost.parts().stream().map(p -> new Check(p, true)).toList();
        Icon icon = GemstoneGrinder.locked(sapphire, GemstoneGrinder.costBlock("&7Cost", checks, false, "&eClick to unlock!", NAMES));
        // The screenshot's: the name red whatever the type, coins first, the gems with their icons, x4 in dark gray.
        assertEquals(new Icon(Material.GRAY_STAINED_GLASS_PANE, "&c✎ Sapphire Gemstone Slot", "&7This slot is locked! Purchasing this",
                "&7slot allows you to apply a", "&b✎ Sapphire Gemstone &7to it!", "", "&7Cost", "&6250,000 Coins",
                "&5✎ Flawless Sapphire Gemstone &8x4", "", "&eClick to unlock!"), icon);
    }

    @Test
    void aLockedSpecialSlotListsItsGemsAndCoinsGoFirst() {
        Slot combat = GemSlots.of(SWORD, new NBTTagCompound()).get(1);
        HexCosts cost = GemstoneGrinder.unlockCost(combat);
        // The data lists the gem first; the lore has coins first.
        assertEquals(List.of(new Coins(250000), new Items("FLAWLESS_JASPER_GEM", 1)), cost.parts());
        List<Check> checks = List.of(new Check(cost.parts().get(0), true), new Check(cost.parts().get(1), false));
        List<String> lore = GemstoneGrinder.locked(combat, GemstoneGrinder.costBlock("&7Cost", checks, false, "&eClick to unlock!", NAMES)).lore();
        assertEquals(List.of("&7This slot is locked! Purchasing this", "&7slot allows you to apply a", "&4⚔ Combat Gemstone &7to it!", "",
                "&cRuby Gemstone", "&5Amethyst Gemstone", "&bSapphire Gemstone", "&dJasper Gemstone", "&8Onyx Gemstone", "&fOpal Gemstone", "",
                "&7Cost", "&6250,000 Coins", "&5❁ Flawless Jasper Gemstone", "", "&cYou don't have that in your", "&cinventories!"), lore);
    }

    @Test
    void costBlocks() {
        List<Check> poor = List.of(new Check(new Coins(10000), false));
        assertEquals(List.of("&7Cost to Remove", "&610,000 Coins", "", "&cYou don't have enough Coins!"),
                GemstoneGrinder.costBlock("&7Cost to Remove", poor, false, "&eClick to remove!", NAMES));
        // Sandbox: free, as the Hex's.
        assertEquals(List.of("&7Cost to Remove", "&aFree", "", "&eClick to remove!"),
                GemstoneGrinder.costBlock("&7Cost to Remove", poor, true, "&eClick to remove!", NAMES));
        assertEquals(List.of("", "&7Cost to Remove", "&aFree", "", "&eClick to remove!"),
                GemstoneGrinder.removal(GemstoneGrinder.costBlock("&7Cost to Remove", List.of(), true, "&eClick to remove!", NAMES)));
    }

    @Test
    void anOpenSlot() {
        NBTTagCompound tag = new NBTTagCompound();
        GemSlots.unlock(tag, SWORD, 0);
        List<Slot> slots = GemSlots.of(SWORD, tag);
        assertEquals(new Icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "&b✎ Sapphire Gemstone Slot", "&7Click a &bSapphire Gemstone &7of any",
                "&7quality in your inventory to apply it", "&7to this item!"), GemstoneGrinder.empty(slots.getFirst(), false));
        GemSlots.unlock(tag, SWORD, 1);
        Icon combat = GemstoneGrinder.empty(GemSlots.of(SWORD, tag).get(1), true);
        assertEquals(Material.RED_STAINED_GLASS_PANE, combat.material());
        assertEquals(List.of("&7Click &aany Gemstone &7of any quality in", "&7your inventory to apply it to this item!", "",
                "&7Applicable Gemstones", "&cRuby Gemstone", "&5Amethyst Gemstone", "&bSapphire Gemstone", "&dJasper Gemstone",
                "&8Onyx Gemstone", "&fOpal Gemstone", "", "&eClick to choose a Gemstone!"), combat.lore());
    }

    @Test
    void panesByColour() {
        assertEquals(Material.PINK_STAINED_GLASS_PANE, GemstoneGrinder.pane(GemstoneType.JASPER));
        assertEquals(Material.ORANGE_STAINED_GLASS_PANE, GemstoneGrinder.pane(GemstoneType.CHISEL));
        assertEquals(Material.WHITE_STAINED_GLASS_PANE, GemstoneGrinder.pane(GemstoneType.UNIVERSAL));
    }

    @Test
    void removingAGemItsSlotNoLongerTakesIsFree() {
        NBTTagCompound tag = new NBTTagCompound();
        GemSlots.apply(tag, SWORD, 0, new Gem(GemstoneType.JADE, GemstoneQuality.PERFECT));
        assertEquals(HexCosts.NOTHING, GemstoneGrinder.removalCost(GemSlots.of(SWORD, tag).getFirst()));
    }

    @Test
    void names() {
        assertEquals("&9❁ Fine Jasper Gemstone", GemstoneGrinder.gemName(new Gem(GemstoneType.JASPER, GemstoneQuality.FINE)));
        assertEquals("&4⚔ Combat Gemstone Slot", GemstoneGrinder.slotName(GemstoneType.COMBAT));
    }

    // The guide

    @Test
    void guideFrame() {
        assertEquals("(1/7) Gemstone Guide", GemstoneGuide.title(0, 7));
        assertEquals(7, GemstoneGuide.pages(187));
        assertEquals(1, GemstoneGuide.pages(0));
        assertEquals(10, GemstoneGuide.place(0));
        assertEquals(16, GemstoneGuide.place(6));
        assertEquals(19, GemstoneGuide.place(7));
        assertEquals(43, GemstoneGuide.place(27));
        Map<Integer, Icon> first = GemstoneGuide.frame(0, 7);
        assertEquals(List.of(4, 48, 53), new ArrayList<>(first.keySet()));
        assertEquals(new Icon(Material.ARROW, "&aNext Page", "&ePage 2"), first.get(53));
        assertEquals(new Icon(Material.ARROW, "&aGo Back", "&7To Gemstone Grinder"), first.get(48));
        Map<Integer, Icon> last = GemstoneGuide.frame(6, 7);
        assertEquals(new Icon(Material.ARROW, "&aPrevious Page", "&ePage 6"), last.get(45));
        assertFalse(last.containsKey(53));
        // The wiki's torch, without "Click to view!".
        assertEquals("&7have Gemstones applied to them.", GemstoneGuide.HEADER_ICON.lore().getLast());
        assertEquals("&eClick to view!", GemstoneGrinder.GUIDE_BUTTON.lore().getLast());
    }

    @Test
    void guideEntries() {
        List<DataItem> items = items("""
                "TEST_DIVAN_HELMET":{"gemstone_slots":[{"type":"AMBER"},{"type":"JADE"},{"type":"AMBER"},{"type":"JADE"},{"type":"TOPAZ"}],\
                "material":"LEATHER_HELMET","name":"Helmet of Test","rarity":"LEGENDARY","type":"HELMET"},
                "TEST_DIVAN_BOOTS":{"gemstone_slots":[{"type":"AMBER"}],"material":"LEATHER_BOOTS","name":"Boots of Test",\
                "rarity":"LEGENDARY","type":"BOOTS"},
                "TEST_AOTV":{"gemstone_slots":[{"type":"SAPPHIRE"}],"material":"DIAMOND_SHOVEL","name":"Aspect of the Test","rarity":"EPIC"},
                "TEST_ROD":{"gemstone_slots":[{"type":"AQUAMARINE"}],"material":"FISHING_ROD","name":"Zeta Rod","rarity":"RARE"},
                "TEST_STICK":{"material":"STICK","name":"Plain Stick"}""");
        List<SkyBlockItem> all = new ArrayList<>(items);
        Map<String, ArmorSet> sets = Map.of("TEST_DIVAN", new ArmorSet("Test Armor", List.of("TEST_DIVAN_HELMET", "TEST_DIVAN_BOOTS")));
        List<GemstoneGuide.Entry> entries = GemstoneGuide.entries(all, sets);
        // By name: the set once, as its helmet named after it; nothing for an item without slots.
        assertEquals(List.of("Aspect of the Test", "Helmet of Test", "Zeta Rod"), entries.stream().map(e -> e.item().name()).toList());
        GemstoneGuide.Entry set = entries.get(1);
        assertEquals("§6Test Armor", set.name());
        assertEquals(List.of("&7Available Gemstone Slots", "  &6⸕ Amber &8x2", "  &a☘ Jade &8x2", "  &e✧ Topaz"), set.slots());
        // Aquamarine's icon as its gems' names have it (the wiki's copy has α).
        assertEquals(List.of("&7Available Gemstone Slots", "  &3☂ Aquamarine"), entries.get(2).slots());
        assertEquals(null, entries.getFirst().name());
    }

    /** The guide from the private item data and table: Divan's Armor once, as the report's example has it. */
    @Test
    void theGuideFromTheRealItems() throws IOException {
        String property = System.getProperty("items.file");
        Path file = property != null ? Path.of(property)
                : Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent()
                .resolveSibling("skyblock-dungeon-data/items/items.json");
        assumeTrue(Files.exists(file), "no " + file);
        List<String> problems = new ArrayList<>();
        GemstoneTable table = GemstoneTable.read(HexData.json(PrivateHex.folder(), GemstoneTable.FILE, problems), problems);
        List<SkyBlockItem> all;
        try (Reader reader = Files.newBufferedReader(file)) {
            all = new ArrayList<>(ItemData.load(reader).items().values());
        }
        List<GemstoneGuide.Entry> entries = GemstoneGuide.entries(all, table.armorSets());
        GemstoneGuide.Entry divan = entries.stream().filter(e -> e.item().id().equals("DIVAN_HELMET")).findFirst().orElseThrow();
        assertEquals("§6Divan Armor", divan.name());
        assertEquals(List.of("&7Available Gemstone Slots", "  &6⸕ Amber &8x2", "  &a☘ Jade &8x2", "  &e✧ Topaz"), divan.slots());
        assertTrue(entries.stream().noneMatch(e -> e.item().id().equals("DIVAN_CHESTPLATE")));
        GemstoneGuide.Entry hyperion = entries.stream().filter(e -> e.item().id().equals("HYPERION")).findFirst().orElseThrow();
        assertEquals(List.of("&7Available Gemstone Slots", "  &b✎ Sapphire", "  &4⚔ Combat"), hyperion.slots());
    }

    // The picker (Sandbox)

    @Test
    void pickerLayout() {
        Map<Integer, Gem> jasper = GemPicker.gems(GemstoneType.JASPER, 0);
        // One gem: the middle column, Rough at the top to Perfect.
        assertEquals(Map.of(4, new Gem(GemstoneType.JASPER, GemstoneQuality.ROUGH), 13, new Gem(GemstoneType.JASPER, GemstoneQuality.FLAWED),
                22, new Gem(GemstoneType.JASPER, GemstoneQuality.FINE), 31, new Gem(GemstoneType.JASPER, GemstoneQuality.FLAWLESS),
                40, new Gem(GemstoneType.JASPER, GemstoneQuality.PERFECT)), jasper);
        // Universal: twelve gems, seven a page.
        assertEquals(35, GemPicker.gems(GemstoneType.UNIVERSAL, 0).size());
        assertEquals(25, GemPicker.gems(GemstoneType.UNIVERSAL, 1).size());
        assertEquals(List.of(53), new ArrayList<>(GemPicker.arrows(GemstoneType.UNIVERSAL, 0).keySet()));
        assertEquals(List.of(45), new ArrayList<>(GemPicker.arrows(GemstoneType.UNIVERSAL, 1).keySet()));
        assertTrue(GemPicker.arrows(GemstoneType.COMBAT, 0).isEmpty());
    }

    // The Hex's category

    @Test
    void theHexsGemstones() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", SWORD.id());
        HexItem item = new HexItem(SWORD, tag, null);
        assertTrue(HexCategories.GEMSTONES.applies(item));
        assertEquals(List.of("  &7Gemstones &8[✎] &8[⚔]"), HexCategories.GEMSTONES.summary(item));
        GemSlots.apply(tag, SWORD, 1, new Gem(GemstoneType.JASPER, GemstoneQuality.FINE));
        assertEquals(List.of("  &7Gemstones &8[✎] &9[&d⚔&9]"), HexCategories.GEMSTONES.summary(new HexItem(SWORD, tag, null)));
        SkyBlockItem stick = items("""
                "TEST_STICK":{"material":"STICK","name":"Plain Stick"}""").getFirst();
        assertFalse(HexCategories.GEMSTONES.applies(new HexItem(stick, new NBTTagCompound(), null)));
    }
}
