package net.icxd.dungeons.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemData;
import net.icxd.dungeons.item.modifier.ItemModifiers;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.ItemStats;
import net.icxd.dungeons.stats.Stat;

/**
 * What the Hex's modifiers do to an item's lore and stats, on made-up items (Hypixel's text stays out of this
 * repository): the brackets, the Enrichment's line, the abilities' ranges, marks and costs, and master stars in
 * Master Mode.
 */
class ModifierLoreTest {
    private static final String STAR = "[{\"amount\":10,\"essence\":\"WITHER\"}]";

    @AfterEach
    void noItems() {
        ItemRegistry.clearData();
    }

    private static DataItem item(String json) {
        try {
            ItemData.Result result = ItemData.load(new StringReader("{\"format\":1,\"items\":{" + json + "}}"));
            assertEquals(List.of(), result.errors());
            return result.items().values().iterator().next();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NBTTagCompound data(SkyBlockItem item) {
        NBTTagCompound tag = ItemBuilder.newData(item);
        tag.remove("attribute_1");
        tag.remove("attribute_2");
        return tag;
    }

    @Test
    void aWoodSingularitysBracket() {
        DataItem axe = item("""
                "TEST_AXE":{"material":"IRON_AXE","name":"Test Axe","rarity":"RARE","stats":{"FORAGING_FORTUNE":10},"type":"AXE"}""");
        NBTTagCompound tag = data(axe);
        tag.setInt(ItemModifiers.WOOD_SINGULARITY, 1);
        assertEquals("&7Foraging Fortune: &6+35 &6(+25)", ItemBuilder.lore(axe, tag).getFirst());
        assertEquals(35, ItemStats.of(axe, tag, null).get(Stat.FORAGING_FORTUNE), 1e-9);
    }

    @Test
    void anEnrichmentFirstThenItsStatInTheTotal(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("items.json");
        Files.writeString(file, """
                {"format":1,"items":{"TALISMAN_ENRICHMENT_MAGIC_FIND":{"lore":["&7Enriches a test with the","&7power of &b+0.7✯ Magic Find"],\
                "material":"PAPER","name":"Test Enrichment","rarity":"SPECIAL"},\
                "TEST_RELIC":{"material":"PAPER","name":"Test Relic","rarity":"LEGENDARY","stats":{"MAGIC_FIND":2},"type":"ACCESSORY"}}}""");
        ItemRegistry.loadData(file);
        SkyBlockItem relic = ItemRegistry.get("TEST_RELIC");
        NBTTagCompound tag = data(relic);
        tag.setString(ItemModifiers.ENRICHMENT, "magic_find");
        assertEquals(List.of("&7&8Enriched with Magic Find", "", "&7Magic Find: &b+2.7"), ItemBuilder.lore(relic, tag).subList(0, 3));
        assertEquals(2.7, ItemStats.of(relic, tag, null).get(Stat.MAGIC_FIND), 1e-9);
    }

    @Test
    void abilitiesShowTheirModifiers() {
        DataItem sword = item("""
                "TEST_BLINK_SWORD":{"abilities":[{"activation":"RIGHT_CLICK","header":"&6Ability: Test Transmission  &e&lRIGHT CLICK",\
                "kind":"ABILITY","mana":45,"name":"Test Transmission","text":["&7Moves you &a8 blocks&7 ahead."]}],\
                "material":"DIAMOND_SHOVEL","name":"Test Blink Sword","rarity":"EPIC","type":"SWORD"}""");
        NBTTagCompound tag = data(sword);
        tag.setInt(ItemModifiers.TUNERS, 4);
        tag.setString(ItemModifiers.POWER_SCROLL, "JASPER_POWER_SCROLL");
        tag.setInt(ItemModifiers.MANA_DISINTEGRATORS, 3);
        List<String> lore = ItemBuilder.lore(sword, tag);
        int header = lore.indexOf("&d&l⦾ &6Ability: Test Transmission  &e&lRIGHT CLICK");
        assertTrue(header >= 0, lore.toString());
        assertEquals(List.of("&7Moves you &a12 blocks&7 ahead.", "&8Mana Cost: &b45✎&8 (&93&9ᛃ&8)"), lore.subList(header + 1, header + 3));
    }

    @Test
    void aDeployablesBuffAndCost() {
        DataItem orb = item("""
                "TEST_ORB":{"abilities":[{"activation":"RIGHT_CLICK","header":"&6Ability: Deploy  &e&lRIGHT CLICK","kind":"ABILITY",\
                "mana_percent":50,"name":"Deploy","text":["&7Place a test orb."]}],"lore":["&9Orb Buff: Test","&9• &7Grants &c+1❁ Strength&7.",\
                "","&8Only one deployable buff applies."],"material":"PLAYER_HEAD","name":"Test Orb","rarity":"RARE","type":"DEPLOYABLE"}""");
        NBTTagCompound tag = data(orb);
        tag.setInt(ItemModifiers.JALAPENO, 1);
        tag.setInt(ItemModifiers.MANA_DISINTEGRATORS, 10);
        List<String> lore = ItemBuilder.lore(orb, tag);
        assertEquals(List.of("&9Orb Buff: Test", "&9• &7Grants &c+1❁ Strength&7.", "&9• &7Grants &9+5☠ Crit Damage&7. &a⒥",
                "&9• &7Grants &9+1☣ Crit Chance&7. &a⒥", "", "&8Only one deployable buff applies.", "", "&6Ability: Deploy  &e&lRIGHT CLICK",
                "&7Place a test orb.", "&8Mana Cost: &b40% of max"), lore.subList(0, 10));
    }

    @Test
    void masterStarsInMasterMode() {
        DataItem sword = item("""
                "TEST_DUNGEON_SWORD":{"dungeon_item":true,"material":"IRON_SWORD","name":"Test Dungeon Sword","rarity":"LEGENDARY",\
                "stats":{"DAMAGE":200,"CRIT_CHANCE":10},"type":"SWORD","upgrade_costs":[@,@,@,@,@]}""".replace("@", STAR));
        NBTTagCompound tag = data(sword);
        tag.setInt("upgrade_count", 7);
        // Five stars' 50%, the Catacombs boost's 10% on Damage, and two master stars' 5% each.
        assertEquals(200 * (1 + 0.5 + 0.1 + 0.1), ItemStats.of(sword, tag, 0.1, ItemModifiers.masterStars(tag)).get(Stat.DAMAGE), 1e-9);
        assertEquals(10 * (1 + 0.5 + 0.1), ItemStats.of(sword, tag, 0.1, 2).get(Stat.CRIT_CHANCE), 1e-9);
        // Anywhere else they're only the five stars.
        assertEquals(200 * (1 + 0.5 + 0.1), ItemStats.of(sword, tag, 0.1).get(Stat.DAMAGE), 1e-9);
        assertEquals(200 * 1.1, ItemStats.of(sword, tag, null, 2).get(Stat.DAMAGE), 1e-9);
    }
}
