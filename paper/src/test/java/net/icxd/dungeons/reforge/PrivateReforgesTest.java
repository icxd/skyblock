package net.icxd.dungeons.reforge;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.PrivateHex;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The real reforge table (the private data's hex/reforges.json, -Dhex.dir; skipped without it) against what Hypixel's
 * items show: the recordings, the live Auction House's items and the item data's stone lore.
 */
class PrivateReforgesTest {
    @TempDir
    Path folder;

    /** Loading a file that isn't there leaves no items. */
    @AfterEach
    void noItems() {
        ItemRegistry.loadData(folder.resolve("none.json"));
    }

    private static ReforgeTable table() {
        List<String> problems = new ArrayList<>();
        ReforgeTable table = ReforgeTable.read(HexData.json(PrivateHex.folder(), ReforgeTable.FILE, problems), problems);
        assertEquals(List.of(), problems);
        assertNotNull(table);
        return table;
    }

    /** Heroic on the recorded epic Aspect of the Void: (+32) Strength, (+80) Intelligence, (+3%) Attack Speed. */
    @Test
    void heroicOnTheRecordedAspectOfTheVoid() {
        Reforge heroic = table().reforge("HEROIC");
        assertEquals(32, heroic.stat(Stat.STRENGTH, Rarity.EPIC, 0));
        assertEquals(80, heroic.stat(Stat.INTELLIGENCE, Rarity.EPIC, 0));
        assertEquals(3, heroic.stat(Stat.ATTACK_SPEED, Rarity.EPIC, 0));
    }

    /**
     * The four the plugin had keep their numbers (Hasty's and Heroic's are NEU's and live items'), and Withered and
     * Ancient add their stat a Catacombs level (a live Mythic Withered Dark Claymore: 170 and its owner's 36).
     */
    @Test
    void thePluginsFourReforges() {
        ReforgeTable table = table();
        assertEquals(170 + 36, table.reforge("WITHERED").stat(Stat.STRENGTH, Rarity.MYTHIC, 36));
        assertEquals(135, table.reforge("WITHERED").stat(Stat.STRENGTH, Rarity.LEGENDARY, 0));
        assertEquals(15, table.reforge("ANCIENT").stat(Stat.CRIT_CHANCE, Rarity.MYTHIC, 0));
        // NEU's Common Crit Damage is the wiki's (and the old table's) Crit Chance.
        assertEquals(3, table.reforge("ANCIENT").stat(Stat.CRIT_CHANCE, Rarity.COMMON, 0));
        assertEquals(0, table.reforge("ANCIENT").stat(Stat.CRIT_DAMAGE, Rarity.COMMON, 0));
        assertEquals(38, table.reforge("ANCIENT").stat(Stat.CRIT_DAMAGE, Rarity.SPECIAL, 38));
        assertEquals(60, table.reforge("HASTY").stat(Stat.CRIT_CHANCE, Rarity.MYTHIC, 0));
        assertEquals(List.of("&9Withered Bonus"), table.reforge("withered").bonusSection(Rarity.MYTHIC).subList(0, 1));
    }

    /** Where live items and NEU disagree, the table has what the items show (REFORGES.md). */
    @Test
    void liveItemsOverNeu() {
        ReforgeTable table = table();
        assertEquals(5, table.reforge("groovy").stat(Stat.FORAGING_FORTUNE, Rarity.EPIC, 0));
        assertEquals(4, table.reforge("spicy").stat(Stat.ATTACK_SPEED, Rarity.RARE, 0));
        assertEquals(5, table.reforge("double_bit").stat(Stat.SPEED, Rarity.EPIC, 0));
        // Suspicious's +15 Damage is its bonus: no item shows it with the stats.
        assertEquals(0, table.reforge("suspicious").stat(Stat.DAMAGE, Rarity.LEGENDARY, 0));
    }

    @Test
    void poolsAndPrices() {
        ReforgeTable table = table();
        assertEquals(List.of("SWORD/ROD", "BOW", "ARMOR", "EQUIPMENT", "PICKAXE", "AXE", "FARMING_TOOL"),
                table.pools().stream().map(ReforgeTable.Pool::type).toList());
        assertEquals(50, table.pools().stream().mapToInt(p -> p.reforges().size()).sum());
        // The wiki's Reforging/Prices, which the Hex's button shows (250 Coins on the wiki's Common sword).
        assertEquals(250, table.randomPrice(Rarity.COMMON));
        assertEquals(50_000, table.randomPrice(Rarity.VERY_SPECIAL));
    }

    /**
     * Every stone is an item the plugin has, but the Boo Stone (not in the item data), and what its own lore shows at
     * Legendary (the item data's) is what the table gives an item of that rarity, stat for stat.
     */
    @Test
    void stonesAsTheirOwnLoreHasThem() {
        Path items = PrivateData.itemsFile();
        assumeTrue(Files.exists(items), "no " + items);
        ReforgeTable table = table();
        assertNull(ItemRegistry.loadData(items).failure());
        List<String> missing = new ArrayList<>();
        List<String> differ = new ArrayList<>();
        for (ReforgeStone stone : table.stones()) {
            SkyBlockItem item = ItemRegistry.get(stone.item());
            if (item == null) {
                missing.add(stone.item());
                continue;
            }
            // (Jerry's shows Legendary numbers, but goes on Aspects of the Jerry, which are never Legendary.)
            if (stone.cost(Rarity.LEGENDARY) == null) continue;
            List<String> lore = item.lore();
            int header = -1;
            for (int i = 0; i < lore.size(); i++) if (lore.get(i).matches("&9.+ &7\\(&6Legendary&7\\):")) header = i;
            if (header < 0) continue;
            for (int i = header + 1; i < lore.size() && !lore.get(i).isEmpty(); i++) {
                String[] parts = lore.get(i).replaceAll("&.", "").split(": ");
                Stat stat = null;
                for (Stat s : Stat.values()) if (s.getDisplayName().equals(parts[0]) && stat == null) stat = s;
                // Overbloom isn't one of the plugin's stats.
                if (stat == null) continue;
                double shown = Double.parseDouble(parts[1].replace("%", "").replace("+", ""));
                double mine = stone.reforge().stat(stat, Rarity.LEGENDARY, 0);
                if (shown != mine) differ.add(stone.item() + " " + stat + " " + shown + " " + mine);
            }
        }
        assertEquals(List.of("BOO_STONE"), missing);
        assertEquals(List.of(), differ);
    }
}
