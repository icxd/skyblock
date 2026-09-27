package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.DataItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.data.ItemData;

/** What's registered. */
class UtilityAbilitiesTest {
    private static Map<String, AbilityHandler> registered() {
        Map<String, AbilityHandler> handlers = new LinkedHashMap<>();
        UtilityAbilities.register((name, handler) -> assertTrue(handlers.put(name, handler) == null, "two handlers for " + name));
        return handlers;
    }

    /** Every name is one an item's ABILITY block has in the real items.json (so it's what a click finds). */
    @Test
    void everyNameIsAnItemsAbility() throws IOException {
        Path items = Path.of(System.getProperty("items.file", "../../skyblock-dungeon-data/items/items.json"));
        assumeTrue(Files.exists(items), "no " + items);
        Set<String> names = new HashSet<>();
        try (Reader reader = Files.newBufferedReader(items)) {
            for (DataItem item : ItemData.load(reader).items().values()) {
                for (ItemBlock block : item.blocks()) if (block.isAbility()) names.add(block.name());
            }
        }
        for (String name : registered().keySet()) assertTrue(names.contains(name), name + " is no item's ability");
    }

    @Test
    void theDungeonOnesAreThere() {
        Map<String, AbilityHandler> handlers = registered();
        for (String name : List.of("Howl", "Small Heal", "Huge Heal", "Speed Boost")) {
            assertTrue(handlers.containsKey(name), name);
        }
        // Done by the run itself (a Revive Stone's right click is the run's).
        assertFalse(handlers.containsKey("Revive"));
    }
}
