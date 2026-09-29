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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The numbers the bonuses read out of their text, against the real items.json (the private data repository:
 * -Ditems.file, else the checkout next to this one; skipped without it): each is where the code looks for it, and
 * the private tables (bonus_tables.json next to items.json, or -Dbonus.tables) start where the text's step is.
 */
class PrivateBonusTextTest {
    private static final double EPSILON = 1e-9;
    private static Map<String, DataItem> items;
    private static Path folder;

    @BeforeAll
    static void load() throws IOException {
        String property = System.getProperty("items.file");
        Path file = property != null ? Path.of(property)
                : Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent()
                        .resolveSibling("skyblock-dungeon-data/items/items.json");
        if (!Files.exists(file)) return;
        folder = file.getParent();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            items = ItemData.load(reader).items();
        }
    }

    private static ItemBlock block(String id, String name) {
        DataItem item = items.get(id);
        assertNotNull(item, id);
        for (ItemBlock block : item.blocks()) if (name.equals(block.name())) return block;
        throw new AssertionError(id + " has no " + name);
    }

    private static double after(String id, String name, String before) {
        return BonusText.after(block(id, name), before, Double.NaN);
    }

    private static double lore(String id, String before) {
        return BonusText.after(items.get(id).lore(), before, Double.NaN);
    }

    @Test
    void setBonusNumbers() {
        assumeTrue(items != null, "no items.json");
        assertEquals(50, after("FERVOR_CHESTPLATE", "Intimidate", "has a"), EPSILON);
        assertEquals(3, after("FERVOR_CHESTPLATE", "Intimidate", "from"), EPSILON);
        assertEquals(3, after("GOLEM_ARMOR_CHESTPLATE", "Absorption", "Absorption"), EPSILON);
        assertEquals(20_000, BonusText.millis(block("GOLEM_ARMOR_CHESTPLATE", "Absorption"), Double.NaN), EPSILON);
        assertEquals(25, after("BOUNCY_CHESTPLATE", "Bouncing Arrow", "have a"), EPSILON);
        assertEquals(50, after("THERMODYNAMIC_CHESTPLATE", "Newton's Demise", "Grants"), EPSILON);
        assertEquals(50, after("WEREWOLF_CHESTPLATE", "Regenerative Howl", "gain"), EPSILON);
        assertEquals(5_000, BonusText.millis(block("WEREWOLF_CHESTPLATE", "Regenerative Howl"), Double.NaN), EPSILON);
        assertEquals(10, after("WEREWOLF_CHESTPLATE", "Regenerative Howl", "up to"), EPSILON);
        assertEquals(2, after("STARLIGHT_CHESTPLATE", "Starpower", "by"), EPSILON);
        assertEquals(1, after("STARLIGHT_CHESTPLATE", "Starpower", "range by"), EPSILON);
        assertEquals(1, after("NUTCRACKER_CHESTPLATE", "Battalion", "Grants"), EPSILON);
        assertEquals(50, after("NUTCRACKER_CHESTPLATE", "Battalion", "and"), EPSILON);
        assertEquals(30, BonusText.blocks(block("NUTCRACKER_CHESTPLATE", "Battalion"), Double.NaN), EPSILON);
        assertEquals(5, after("NUTCRACKER_CHESTPLATE", "Battalion", "up to"), EPSILON);
        assertEquals(50, after("ZOMBIE_COMMANDER_CHESTPLATE", "Training", "Every"), EPSILON);
        assertEquals(5, after("ZOMBIE_COMMANDER_CHESTPLATE", "Training", "wearer"), EPSILON);
        assertEquals(500, after("ZOMBIE_COMMANDER_CHESTPLATE", "Training", "Max"), EPSILON);
        assertEquals(10, after("ARMOR_OF_MAGMA_CHESTPLATE", "Absorb", "Every"), EPSILON);
        assertEquals(1, after("ARMOR_OF_MAGMA_CHESTPLATE", "Absorb", "wearer"), EPSILON);
        assertEquals(200, after("ARMOR_OF_MAGMA_CHESTPLATE", "Absorb", "Max"), EPSILON);
        for (String id : List.of("REVENANT_CHESTPLATE", "TARANTULA_BOOTS", "FINAL_DESTINATION_HELMET")) {
            String name = id.startsWith("REVENANT") ? "Zombie Bulwark" : id.startsWith("TARANTULA") ? "Spider Bulwark" : "Enderman Bulwark";
            assertEquals(20, after(id, name, "Next Upgrade:"), EPSILON, id);
            assertFalse(Double.isNaN(after(id, name, "/")), id);
        }
        assertEquals(1, after("TERROR_CHESTPLATE", "Hydra Strike", "Damage and"), EPSILON);
        assertEquals(20, after("TERROR_CHESTPLATE", "Hydra Strike", "deal"), EPSILON);
        assertEquals(3, after("INFERNAL_TERROR_CHESTPLATE", "Hydra Strike", "Damage and"), EPSILON);
        assertEquals(10, after("HOLLOW_CHESTPLATE", "Spirit", "Max"), EPSILON);
        assertEquals(40, after("INFERNAL_HOLLOW_CHESTPLATE", "Spirit", "Max"), EPSILON);
        assertEquals(4, after("AURORA_CHESTPLATE", "Arcane Energy", "after"), EPSILON);
        assertEquals(50, after("LAPIS_ARMOR_CHESTPLATE", "Magnetic", "Earn"), EPSILON);
    }

    @Test
    void ownTextNumbers() {
        assumeTrue(items != null, "no items.json");
        assertEquals(5, lore("CHICKEN_HEAD", "fall damage by"), EPSILON);
        assertEquals(10, lore("ZOMBIE_HAT", "Gives"), EPSILON);
        assertEquals(8, lore("ZOMBIE_HAT", "within"), EPSILON);
        assertEquals(20, lore("SKELETON_HAT", "have a"), EPSILON);
        assertEquals(50, lore("SKELETON_HAT", "dealing"), EPSILON);
        assertEquals(8, lore("SKELETON_HAT", "within"), EPSILON);
        assertEquals(1, lore("OBSIDIAN_CHESTPLATE", "gain"), EPSILON);
        assertEquals(20, lore("OBSIDIAN_CHESTPLATE", "every"), EPSILON);
        assertEquals(1, lore("SUPER_HEAVY_CHESTPLATE", "by"), EPSILON);
        assertEquals(6, lore("DOJO_BLACK_BELT", "taken by"), EPSILON);
        assertEquals(0.5, lore("DOJO_WHITE_BELT", "taken by"), EPSILON);
        assertEquals(25, lore("ANNIHILATION_CLOAK", "Heal"), EPSILON);
        assertEquals(1.15, lore("DEMONLORD_GAUNTLET", "Deal"), EPSILON);
        assertEquals(5, lore("MITHRIL_BELT", "Grants"), EPSILON);
        assertEquals(10, lore("TITANIUM_NECKLACE", "Grants"), EPSILON);
        assertEquals(5, lore("CLOVER_HELMET", "Magic Find:"), EPSILON);
        assertEquals(5, lore("CLOVER_HELMET", "All Other Stats: -"), EPSILON);
        assertEquals(20, lore("SPRING_BOOTS", "fall damage by"), EPSILON);
        assertEquals(100, lore("SPRING_BOOTS", "reduces fall damage by"), EPSILON);
        assertEquals(5, lore("SEA_LANTERN_HAT", "Breathe"), EPSILON);
        assertEquals(1, PieceText.BossHeads.floor(items.get("GOLD_BONZO_HEAD").lore()));
        assertEquals(7, PieceText.BossHeads.floor(items.get("DIAMOND_NECRON_HEAD").lore()));
    }

    /** The private kill steps, where they're there, start with the step the pieces' text shows. */
    @Test
    void tables() {
        assumeTrue(items != null, "no items.json");
        String property = System.getProperty("bonus.tables");
        Path file = property != null ? Path.of(property) : folder.resolve("bonus_tables.json");
        assumeTrue(Files.exists(file), "no bonus_tables.json");
        BonusTables tables = BonusTables.load(file, null);
        for (String name : List.of("Zombie Bulwark", "Spider Bulwark", "Enderman Bulwark")) {
            List<BonusTables.Step> steps = tables.killSteps(name);
            assertFalse(steps.isEmpty(), name);
            String id = name.startsWith("Zombie") ? "REVENANT_CHESTPLATE" : name.startsWith("Spider") ? "TARANTULA_BOOTS" : "FINAL_DESTINATION_HELMET";
            assertEquals(after(id, name, "/"), steps.get(0).at(), EPSILON, name);
            assertEquals(after(id, name, "Next Upgrade:"), steps.get(0).value(), EPSILON, name);
        }
    }
}
