package net.icxd.dungeons.item.bonus;

import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The bonuses against the real items.json (in the private data repository: -Ditems.file, else the checkout
 * next to this one; skipped without it): each one is some item's, and the tiered numbers the code swaps are
 * the ones the items' text has.
 */
class BonusDataTest {
    private static Collection<DataItem> items;

    @BeforeAll
    static void load() throws IOException {
        String property = System.getProperty("items.file");
        Path file = property != null ? Path.of(property)
                : Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent()
                        .resolveSibling("skyblock-dungeon-data/items/items.json");
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            items = ItemData.load(reader).items().values();
        }
    }

    private static List<ItemBlock> blocks(String kind, String name) {
        List<ItemBlock> blocks = new ArrayList<>();
        for (DataItem item : items) {
            for (ItemBlock block : item.blocks()) if (kind.equals(block.kind()) && name.equals(block.name())) blocks.add(block);
        }
        return blocks;
    }

    /** No bonus is for a name no item has (a typo would make it do nothing). */
    @Test
    void everyBonusIsAnItems() {
        assumeTrue(items != null, "no items.json");
        for (Bonus bonus : SetBonuses.all()) {
            if (Bonus.ITEM.equals(bonus.kind())) {
                assertTrue(items.stream().anyMatch(item -> bonus.item(item.id())), "no item for " + bonus.name());
            } else {
                assertFalse(blocks(bonus.kind(), bonus.name()).isEmpty(), "no " + bonus.kind() + " block " + bonus.name());
            }
        }
    }

    /** Where the code swaps a tiered bonus's numbers, every item with it has them to swap. */
    @Test
    void tieredNumbersAreInTheText() {
        assumeTrue(items != null, "no items.json");
        for (String name : Set.of("Arachne's Faithful", "Berserk", "Long Tuba", "Dominus", "Hydra Strike", "Arcane Energy", "Fervor", "Spirit")) {
            Bonus bonus = SetBonuses.bonus(SetKey.TIERED, name);
            List<ItemBlock> blocks = blocks(SetKey.TIERED, name);
            assertFalse(blocks.isEmpty(), name);
            for (ItemBlock block : blocks) assertNotEquals(block.text(), bonus.text(block.text(), 4), name + ": " + block.text());
        }
    }
}
