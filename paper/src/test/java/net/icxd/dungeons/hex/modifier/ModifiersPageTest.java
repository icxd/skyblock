package net.icxd.dungeons.hex.modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.icxd.dungeons.hex.modifier.HexModifiers.Offer;
import net.icxd.dungeons.item.ItemRegistry;

/** The Modifiers page's entries, on made-up items: their names and lore, the Cost block's line for the item. */
class ModifiersPageTest {
    @TempDir
    Path folder;

    @BeforeEach
    void items() throws IOException {
        Path file = folder.resolve("items.json");
        Files.writeString(file, """
                {"format":1,"items":{"RUBY_POWER_SCROLL":{"lore":["&7Does a test thing."],"material":"PAPER","name":"Ruby Power Scroll",\
                "rarity":"EPIC"},"TEST_MODIFIER":{"lore":["&7Modifies a test."],"material":"PAPER","name":"Test Modifier","rarity":"RARE"}}}""");
        ItemRegistry.loadData(file);
    }

    /** Loading a file that isn't there leaves no items. */
    @AfterEach
    void noItems() {
        ItemRegistry.loadData(folder.resolve("none.json"));
    }

    private static Offer offer(String id, String colour, boolean done, boolean counted) {
        return new Offer(id, colour, done, counted, t -> t);
    }

    @Test
    void aPowerScrollsCostLineIsInItsGemsColour() {
        Offer ruby = offer("RUBY_POWER_SCROLL", "&c", false, false);
        assertEquals("&cRuby Power Scroll", ModifiersPage.name(ruby));
        // The block as HexCosts makes it: the item in its rarity's colour.
        List<String> cost = List.of("&7Cost", "§5Ruby Power Scroll &c✖", "", "&cYou don't have that in your", "&cinventories!");
        // The wiki's screen: "&cRuby Power Scroll &c✖".
        assertEquals(List.of("&7Does a test thing.", "", "&7Cost", "&cRuby Power Scroll &c✖", "", "&cYou don't have that in your",
                "&cinventories!"), ModifiersPage.lore(ruby, cost));
        // Sandbox's block has no line for the item.
        assertEquals(List.of("&7Does a test thing.", "", "&7Cost", "&aFree", "", ModifiersPage.APPLY),
                ModifiersPage.lore(ruby, List.of("&7Cost", "&aFree", "", ModifiersPage.APPLY)));
    }

    @Test
    void theRestAreInTheirRaritys() {
        Offer modifier = offer("TEST_MODIFIER", null, false, true);
        assertEquals("&9Test Modifier", ModifiersPage.name(modifier));
        List<String> cost = List.of("&7Cost", "§9Test Modifier &a✔", "", ModifiersPage.APPLY);
        assertEquals(List.of("&7Modifies a test.", "", "&7Cost", "§9Test Modifier &a✔", "", ModifiersPage.APPLY),
                ModifiersPage.lore(modifier, cost));
    }

    @Test
    void oneThatsDoneSaysSo() {
        assertEquals(List.of("&7Modifies a test.", "", ModifiersPage.MAXED), ModifiersPage.lore(offer("TEST_MODIFIER", null, true, true), List.of()));
        assertEquals(List.of("&7Does a test thing.", "", ModifiersPage.APPLIED),
                ModifiersPage.lore(offer("RUBY_POWER_SCROLL", "&c", true, false), List.of()));
        // Without the item data, its id, and no text.
        assertEquals("&fNO_SUCH_MODIFIER", ModifiersPage.name(offer("NO_SUCH_MODIFIER", null, false, false)));
        assertEquals(List.of("&7Cost"), ModifiersPage.lore(offer("NO_SUCH_MODIFIER", null, false, false), List.of("&7Cost")));
    }
}
