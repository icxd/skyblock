package net.icxd.dungeons.item.modifier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.collection.PrivateData;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.stats.Stats;

/**
 * The modifiers' numbers read from the real items' text (the private items.json: -Ditems.file), so every
 * Enrichment and Power Scroll does something. Skipped without it.
 */
class ModifierDataTest {
    @BeforeAll
    static void items() {
        Path items = PrivateData.itemsFile();
        assumeTrue(Files.isRegularFile(items), "no " + items);
        assertNull(ItemRegistry.loadData(items).failure(), "the items didn't load");
    }

    @Test
    void everyEnrichmentGivesItsStat() {
        for (Enrichment enrichment : Enrichment.values()) {
            assertNotNull(ItemRegistry.get(enrichment.itemId()), enrichment.itemId());
            assertTrue(enrichment.amount() > 0, enrichment.name());
        }
    }

    @Test
    void everyPowerScrollDoesSomething() {
        for (PowerScroll scroll : PowerScroll.values()) {
            SkyBlockItem item = ItemRegistry.get(scroll.itemId());
            assertNotNull(item, scroll.itemId());
            PowerScroll.Effect effect = PowerScroll.effect(item.lore());
            boolean buff = !effect.stats().equals(new Stats()) && effect.millis() > 0;
            // A heal or mana comes with a cooldown; a buff lasts a while.
            boolean now = (effect.heal() > 0 || effect.mana() > 0) && effect.cooldown() > 0;
            assertTrue(buff || now, scroll + ": " + effect);
        }
    }
}
