package net.icxd.dungeons.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;

/**
 * Storage's tables for tests: the private data's (storage/), where it's there: -Dstorage.dir, else the data
 * checkout next to this repository; tests that need it are skipped without it. And made-up ones, for the
 * rules alone.
 */
final class PrivateTables {
    private PrivateTables() {
    }

    static Path folder() {
        String property = System.getProperty("storage.dir");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data").resolve(StorageTables.FOLDER);
    }

    /** The private tables, read without a problem; the test is skipped if they aren't there. */
    static StorageTables load() {
        Path folder = folder();
        assumeTrue(Files.isDirectory(folder), "no " + folder);
        StorageTables tables = StorageTables.load(folder);
        assertEquals(List.of(), tables.problems());
        return tables;
    }

    /** Made-up tables: Accessory Power 1 a rarity up from Common, and these upgrades. */
    static StorageTables madeUp(Map<String, List<String>> upgrades) {
        Map<Rarity, Integer> power = Map.of(Rarity.COMMON, 1, Rarity.UNCOMMON, 2, Rarity.RARE, 3, Rarity.EPIC, 4, Rarity.LEGENDARY, 5,
                Rarity.MYTHIC, 6);
        return new StorageTables(Map.of(), power, Map.of(Stat.HEALTH, 2.0), upgrades, Map.of(), List.of());
    }

    /** Made-up tables with these powers. */
    static StorageTables withPowers(StorageTables.Power... powers) {
        Map<String, StorageTables.Power> byName = new java.util.LinkedHashMap<>();
        for (StorageTables.Power power : powers) byName.put(power.name(), power);
        return new StorageTables(byName, Map.of(), Map.of(Stat.HEALTH, 2.0), Map.of(), Map.of(), List.of());
    }
}
