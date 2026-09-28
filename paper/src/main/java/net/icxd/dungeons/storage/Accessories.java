package net.icxd.dungeons.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.icxd.dungeons.item.enums.Rarity;

/**
 * Which accessories take effect, and the Accessory Power they give (the wiki's Accessories and Accessory
 * Power): accessories work in the inventory and in the Accessory Bag, but only one of each counts, and of
 * a line (Talisman, Ring, Artifact, Relic) only the best one there; only those in the bag give Accessory
 * Power, by their rarity (see {@link StorageTables#accessoryPower}). In a dungeon a
 * dungeon accessory gives twice as much, and the Hegemony Artifact always does ("Counts for twice the
 * Accessory Power in your Accessory Bag"). The Abicases' Accessory Power from Abiphone contacts isn't
 * here: there are no Abiphones. No server in it.
 */
public final class Accessories {
    public static final String HEGEMONY = "HEGEMONY_ARTIFACT";

    /** An accessory they have: its SkyBlock id, the rarity it shows (one up if recombobulated), and whether it's a dungeon item. */
    public record Held(String id, Rarity rarity, boolean dungeon) {
    }

    private Accessories() {
    }

    /**
     * The ones that count, as indexes into {@code held} in its order: of two of the same only the one that
     * gives more Accessory Power (else the first; UNKNOWN: the wiki says only that they don't stack), and of
     * a line only the best, the one the others upgrade into; two of a line that don't upgrade into each
     * other (the Abicases), the one that gives more.
     */
    public static List<Integer> counted(List<Held> held, StorageTables tables) {
        Map<String, Integer> best = new HashMap<>();
        for (int i = 0; i < held.size(); i++) {
            String line = tables.line(held.get(i).id());
            Integer other = best.get(line);
            if (other == null || better(held.get(i), held.get(other), tables)) best.put(line, i);
        }
        List<Integer> out = new ArrayList<>(best.values());
        out.sort(null);
        return out;
    }

    /** Whether {@code a} beats {@code b}, of the same line: a higher tier, else more Accessory Power. */
    private static boolean better(Held a, Held b, StorageTables tables) {
        boolean aUpgradesB = tables.upgrades(b.id()).contains(a.id());
        boolean bUpgradesA = tables.upgrades(a.id()).contains(b.id());
        if (aUpgradesB != bUpgradesA) return aUpgradesB;
        return tables.accessoryPower(a.rarity()) > tables.accessoryPower(b.rarity());
    }

    /** The Accessory Power the counted ones give (see {@link #counted}): only the bag's accessories give any. */
    public static int power(List<Held> held, List<Integer> counted, boolean inDungeon, StorageTables tables) {
        int power = 0;
        for (int i : counted) power += power(held.get(i), inDungeon, tables);
        return power;
    }

    /** One accessory's: its rarity's, twice for the Hegemony Artifact, twice for a dungeon accessory in a dungeon. */
    public static int power(Held held, boolean inDungeon, StorageTables tables) {
        int power = tables.accessoryPower(held.rarity());
        if (HEGEMONY.equals(held.id())) power *= 2;
        if (held.dungeon() && inDungeon) power *= 2;
        return power;
    }

    /** "Every 10 MP grants 1 Tuning Point" (the recorded Stats Tuning item). */
    public static int tuningPoints(int accessoryPower) {
        return accessoryPower / 10;
    }
}
