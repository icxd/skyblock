package net.icxd.dungeons.hex.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexCategories;
import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.item.DungeonItems;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;

/** The Item Upgrades category and "The Hex ➜ Item Upgrades" (their lore is ours: UNKNOWN U12), on made-up items. */
class UpgradesPageTest {
    private static final Function<HexCosts, List<String>> COST = cost -> List.of("<cost " + cost.parts() + ">");
    /** Like the Hellfire Rod's first stars: items, then essence (the API's order). */
    private static final SkyBlockItem ROD = item("""
            "TEST_ROD":{"material":"FISHING_ROD","name":"Test Rod","rarity":"LEGENDARY","type":"FISHING_ROD","upgrade_costs":[\
            [{"amount":20,"item":"LUMP_OF_MAGMA"},{"amount":50,"essence":"CRIMSON"}],@,@,@,@,@,@,@,@,@]}"""
            .replace("@", "[{\"amount\":80,\"essence\":\"CRIMSON\"}]"));
    /** Like the Aspect of the Dragons: 5 stars, and it can be made a dungeon item for 150 Dragon Essence. */
    private static final SkyBlockItem SWORD = item("""
            "TEST_SWORD":{"dungeon_conversion_cost":[{"amount":150,"essence":"DRAGON"}],"material":"DIAMOND_SWORD",\
            "name":"Test Sword","rarity":"LEGENDARY","type":"SWORD","upgrade_costs":[@,@,@,@,@]}"""
            .replace("@", "[{\"amount\":50,\"essence\":\"DRAGON\"}]"));
    private static final SkyBlockItem DUNGEON_SWORD = item("""
            "TEST_DUNGEON_SWORD":{"dungeon_item":true,"material":"IRON_SWORD","name":"Test Dungeon Sword","rarity":"LEGENDARY",\
            "type":"SWORD","upgrade_costs":[@,@,@,@,@]}""".replace("@", "[{\"amount\":30,\"essence\":\"WITHER\"}]"));
    private static final SkyBlockItem PLAIN = item("""
            "TEST_PLAIN":{"material":"IRON_SWORD","name":"Test Plain","rarity":"RARE","type":"SWORD"}""");

    private static SkyBlockItem item(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NBTTagCompound tag(SkyBlockItem item, int stars) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", item.id());
        tag.setString("dungeon_star", "ZERO");
        tag.setBoolean("dungeon_item", item.dungeonItem());
        tag.setInt("upgrade_count", stars);
        return tag;
    }

    private static List<String> summary(SkyBlockItem item, NBTTagCompound tag) {
        return HexCategories.ITEM_UPGRADES.summary(new HexItem(item, tag, null));
    }

    /** The official screenshot's Fabled Livid Dagger: "Dungeon Item ✔", "Upgrade Level ✪✪✪✪✪". */
    @Test
    void summaries() {
        assertEquals(List.of("  &7Dungeon Item &a✔", "  &7Upgrade Level &6✪✪✪✪✪"), summary(DUNGEON_SWORD, tag(DUNGEON_SWORD, 5)));
        assertEquals(List.of("  &7Dungeon Item &c✖", "  &7Upgrade Level &c✖"), summary(SWORD, tag(SWORD, 0)));
        // Can't be made one: no Dungeon Item line.
        assertEquals(List.of("  &7Upgrade Level &d✪✪&6✪✪✪"), summary(ROD, tag(ROD, 7)));
        // Master stars show as they do on the item.
        assertEquals(List.of("  &7Dungeon Item &a✔", "  &7Upgrade Level &6✪✪✪✪✪&c➋"), summary(DUNGEON_SWORD, tag(DUNGEON_SWORD, 7)));
        assertTrue(HexCategories.ITEM_UPGRADES.applies(new HexItem(ROD, tag(ROD, 0), null)));
        assertFalse(HexCategories.ITEM_UPGRADES.applies(new HexItem(PLAIN, tag(PLAIN, 0), null)));
    }

    /** One entry a star, named by its stars; the next one has its Cost block, the ones before are on, the rest wait. */
    @Test
    void stars() {
        NBTTagCompound tag = tag(ROD, 5);
        Icon done = UpgradesPage.star(ROD, tag, 5, COST);
        assertEquals(Material.NETHER_STAR, done.material());
        assertEquals("&6✪✪✪✪✪", done.name());
        assertEquals(List.of("&7Each item level upgrade &6✪ &7grants a", "&a+2% &7stat bonus.", "", "&aThis upgrade has been applied!"), done.lore());
        Icon next = UpgradesPage.star(ROD, tag, 6, COST);
        assertEquals("&d✪&6✪✪✪✪", next.name());
        assertEquals("<cost [Essence[type=CRIMSON, amount=80]]>", next.lore().getLast());
        assertEquals("&cUpgrade to &d✪✪&6✪✪✪ &cfirst!", UpgradesPage.star(ROD, tag, 8, COST).lore().getLast());
        assertEquals("<cost [Items[id=LUMP_OF_MAGMA, amount=20], Essence[type=CRIMSON, amount=50]]>",
                UpgradesPage.star(ROD, tag(ROD, 0), 1, COST).lore().getLast());
        // A dungeon item's say what they do in a dungeon too.
        assertEquals(List.of("&7Each item level upgrade &6✪ &7grants a", "&a+2% &7stat bonus and a &a+10% &7bonus", "&7while in Dungeons.", "",
                "<cost [Essence[type=WITHER, amount=30]]>"), UpgradesPage.star(DUNGEON_SWORD, tag(DUNGEON_SWORD, 0), 1, COST).lore());
    }

    /** Convert to Dungeon Item: its cost until it's done, then "This item is already a Dungeon Item" (the fragment NEU reads). */
    @Test
    void convert() {
        NBTTagCompound tag = tag(SWORD, 3);
        Icon icon = UpgradesPage.convert(SWORD, tag, COST);
        assertEquals(Material.ANVIL, icon.material());
        assertEquals("&aConvert to Dungeon Item", icon.name());
        assertEquals("<cost [Essence[type=DRAGON, amount=150]]>", icon.lore().getLast());
        DungeonItems.convert(tag);
        assertEquals(List.of("&aThis item is already a Dungeon", "&aItem!"), UpgradesPage.convert(SWORD, tag, COST).lore().subList(5, 7));
        assertEquals(List.of("  &7Dungeon Item &a✔", "  &7Upgrade Level &6✪✪✪"), summary(SWORD, tag));
        assertTrue(String.join("", UpgradesPage.convert(DUNGEON_SWORD, tag(DUNGEON_SWORD, 0), COST).lore()).contains("This item is already a Dungeon"));
        // Neither one nor can be made one: no button.
        assertNull(UpgradesPage.convert(ROD, tag(ROD, 0), COST));
    }

    @Test
    void costs() {
        HexCosts cost = UpgradesPage.cost(SWORD.dungeonConversionCost());
        assertEquals(List.of(new HexCosts.Essence(EssenceType.DRAGON, 150)), cost.parts());
        assertEquals(List.of(new HexCosts.Items("LUMP_OF_MAGMA", 20), new HexCosts.Essence(EssenceType.CRIMSON, 50)),
                UpgradesPage.cost(ROD.upgradeCosts().getCosts().getFirst()).parts());
    }
}
