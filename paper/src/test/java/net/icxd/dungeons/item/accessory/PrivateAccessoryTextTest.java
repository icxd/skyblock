package net.icxd.dungeons.item.accessory;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.mob.MobType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The numbers the accessories' effects read out of their text, against the real items.json (the private data
 * repository: -Ditems.file, else the checkout next to this one; skipped without it): each where the code looks.
 */
class PrivateAccessoryTextTest {
    private static final double EPSILON = 1e-9;
    private static boolean loaded;

    @BeforeAll
    static void load() {
        String property = System.getProperty("items.file");
        Path file = property != null ? Path.of(property)
                : Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent()
                        .resolveSibling("skyblock-dungeon-data/items/items.json");
        if (!Files.exists(file)) return;
        ItemRegistry.loadData(file);
        AccessoryText.clear();
        loaded = true;
    }

    @AfterAll
    static void forget() {
        AccessoryText.clear();
    }

    private static double after(String id, String before) {
        return AccessoryText.after(id, before, Double.NaN);
    }

    @Test
    void numbers() {
        assumeTrue(loaded, "no items.json");
        Map<String, Double> expected = Map.ofEntries(Map.entry("DAY_CRYSTAL/by", 5.0), Map.entry("MOONLIGHT_CRYSTAL/by", 10.0),
                Map.entry("IQ_POINT/by", 1.0), Map.entry("TWO_IQ_POINT/by", 2.0), Map.entry("BLUETOOTH_RING/least", 6.0),
                Map.entry("BLUETOOTH_RING/Deal", 1.0), Map.entry("VILLAGE_TALISMAN/by", 10.0), Map.entry("MASTER_SKULL_TIER_7/Grants", 10.0),
                Map.entry("REAPER_ORB/stack of", 2.0), Map.entry("REAPER_ORB/last", 5.0), Map.entry("BLOOD_GOD_SIGIL/Gain", 2.0),
                Map.entry("BLOOD_GOD_CREST/Max", 7.0), Map.entry("TARANTULA_RING/Every", 10.0), Map.entry("TARANTULA_RING/deals", 15.0),
                Map.entry("WEDDING_RING_9/1 in", 100.0), Map.entry("WEDDING_RING_9/deal", 100.0), Map.entry("WEDDING_RING_0/1 in", 1e12),
                Map.entry("VAMPIRE_DENTIST_RELIC/(", 20.0), Map.entry("DEVOUR_RING/Heal", 10.0), Map.entry("DEVOUR_RING/Cooldown:", 0.5),
                Map.entry("BURSTSTOPPER_ARTIFACT/at least", 50.0), Map.entry("BURSTSTOPPER_ARTIFACT/damage by", 0.9),
                Map.entry("EXPERIENCE_ARTIFACT/gain by", 25.0), Map.entry("FEATHER_ARTIFACT/damage by", 10.0),
                Map.entry("FEATHER_ARTIFACT/reduced by", 15.0), Map.entry("VACCINE_ARTIFACT/poison by", 50.0),
                Map.entry("FIRE_TALISMAN/grants a", 20.0), Map.entry("EMERALD_ARTIFACT/Get", 20.0), Map.entry("SEAL_OF_THE_FAMILY/Get", 3.0),
                Map.entry("INTIMIDATION_TALISMAN/Level", 1.0), Map.entry("INTIMIDATION_RELIC/Level", 30.0),
                Map.entry("CATACOMBS_EXPERT_RING/by", 10.0), Map.entry("SCARF_GRIMOIRE/Gain", 6.0), Map.entry("BUCKET_OF_DYE/Dyes by", 1.0));
        for (Map.Entry<String, Double> e : expected.entrySet()) {
            String[] key = e.getKey().split("/", 2);
            assertEquals(e.getValue(), after(key[0], key[1]), EPSILON, e.getKey());
        }
        assertEquals(3, AccessoryEffects.haste(AccessoryText.plain("HASTE_ARTIFACT")));
        assertEquals(22, AccessoryText.stats("SOUL_CAMPFIRE_TALISMAN_21").get(net.icxd.dungeons.stats.Stat.DEFENSE), EPSILON);
        assertEquals(25, AccessoryText.stats("MINE_TALISMAN").get(net.icxd.dungeons.stats.Stat.SPEED), EPSILON);
    }

    /** Every "Reduces the damage taken from X mobs" talisman is read, its mob type one the plugin knows. */
    @Test
    void reductions() {
        assumeTrue(loaded, "no items.json");
        Map<String, MobType> types = Map.of("SKELETON_TALISMAN", MobType.SKELETAL, "WOLF_RING", MobType.ANIMAL, "ZOMBIE_ARTIFACT", MobType.UNDEAD,
                "SPIDER_ARTIFACT", MobType.ARTHROPOD, "WITHER_RELIC", MobType.WITHER, "ENDER_RELIC", MobType.ENDER, "BLAZE_TALISMAN",
                MobType.INFERNAL, "NETHER_ARTIFACT", MobType.INFERNAL);
        for (Map.Entry<String, MobType> e : types.entrySet()) {
            AccessoryEffects.Reduction r = AccessoryEffects.reduction(AccessoryText.plain(e.getKey()));
            assertNotNull(r, e.getKey());
            assertEquals(e.getValue(), r.type(), e.getKey());
        }
        assertEquals(0.75, AccessoryEffects.reduction(AccessoryText.plain("WITHER_RELIC")).factor(), EPSILON);
        assertTrue(AccessoryEffects.reduction(AccessoryText.plain("NETHER_ARTIFACT")).allOnCrimsonIsle());
        AccessoryEffects.Summary s = AccessoryEffects.summarize(Set.of("NIGHT_VISION_CHARM", "LAVA_TALISMAN", "SHADY_RING"));
        assertTrue(s.nightVision && s.lava);
        assertEquals(1, after("SHADY_RING", "Get"), EPSILON);
    }
}
