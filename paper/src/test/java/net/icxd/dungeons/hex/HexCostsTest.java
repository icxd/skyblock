package net.icxd.dungeons.hex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexCosts.Check;
import net.icxd.dungeons.hex.HexCosts.Coins;
import net.icxd.dungeons.hex.HexCosts.Essence;
import net.icxd.dungeons.hex.HexCosts.Items;
import net.icxd.dungeons.hex.HexCosts.Levels;
import net.icxd.dungeons.item.cost.essence.EssenceType;

/** The Cost block, against the wiki's Hex screens, with the Bazaar gone and Sandbox free. */
class HexCostsTest {
    private static final Function<String, String> NAMES = id -> switch (id) {
        case "WITHER_BLOOD" -> "&5Wither Blood";
        case "HOT_POTATO_BOOK" -> "&5Hot Potato Book";
        default -> "&f" + id;
    };

    @Test
    void theWikisReforgeStone() {
        // Withered on a Legendary: "&610,000 Coins &a✔", "&5Wither Blood &c✖" (then the Bazaar, which isn't here).
        List<Check> checks = List.of(new Check(new Coins(10_000), true), new Check(new Items("WITHER_BLOOD", 1), false));
        assertEquals(List.of("&7Cost", "&610,000 Coins &a✔", "&5Wither Blood &c✖", "", "&cYou don't have that in your", "&cinventories!"),
                HexCosts.lore(checks, false, "&eClick to reforge!", NAMES));
    }

    @Test
    void allOwnedShowsTheAction() {
        List<Check> checks = List.of(new Check(new Items("HOT_POTATO_BOOK", 1), true));
        assertEquals(List.of("&7Cost", "&5Hot Potato Book &a✔", "", "&eClick to apply!"), HexCosts.lore(checks, false, "&eClick to apply!", NAMES));
    }

    @Test
    void whatsMissingFirst() {
        // Coins, with the item there.
        List<Check> checks = List.of(new Check(new Coins(250_000), false), new Check(new Items("WITHER_BLOOD", 4), true));
        assertEquals(List.of("&7Cost", "&6250,000 Coins &c✖", "&5Wither Blood &8x4 &a✔", "", "&cYou don't have enough Coins!"),
                HexCosts.lore(checks, false, "&eClick!", NAMES));
        // An item before coins: it can't be bought here.
        checks = List.of(new Check(new Coins(250_000), false), new Check(new Items("WITHER_BLOOD", 4), false));
        assertEquals(List.of("&cYou don't have that in your", "&cinventories!"), HexCosts.actionLines(checks, "&eClick!"));
        // Essence and Exp levels (UNKNOWN lines).
        checks = List.of(new Check(new Essence(EssenceType.WITHER, 30), false), new Check(new Levels(15), false));
        assertEquals(List.of("&7Cost", "&dWither Essence &8x30 &c✖", "&315 Exp Levels &c✖", "", "&cYou don't have enough Wither Essence!"),
                HexCosts.lore(checks, false, "&eClick!", NAMES));
        assertEquals(List.of("&cYou don't have enough Exp Levels!"),
                HexCosts.actionLines(List.of(new Check(new Levels(15), false)), "&eClick!"));
    }

    @Test
    void sandboxIsFree() {
        List<Check> checks = List.of(new Check(new Coins(10_000), true), new Check(new Items("WITHER_BLOOD", 1), true));
        assertEquals(List.of("&7Cost", "&aFree", "", "&eClick to reforge!"), HexCosts.lore(checks, true, "&eClick to reforge!", NAMES));
    }

    @Test
    void partsOfAKindCountTogether() {
        // Checked as they're taken: coins twice are both, an item's two parts (any case) are three of it.
        HexCosts cost = HexCosts.of(new Coins(10_000), new Items("wither_blood", 1), new Coins(5_000), new Items("WITHER_BLOOD", 2),
                new Essence(EssenceType.WITHER, 30), new Essence(EssenceType.WITHER, 60), new Essence(EssenceType.DRAGON, 5), new Levels(3),
                new Levels(4));
        HexCosts.Totals totals = cost.totals();
        assertEquals(15_000, totals.coins());
        assertEquals(7, totals.levels());
        assertEquals(Map.of(EssenceType.WITHER, 90, EssenceType.DRAGON, 5), totals.essence());
        assertEquals(Map.of("WITHER_BLOOD", 3), totals.items());
        assertEquals(new HexCosts.Totals(0, 0, Map.of(), Map.of()), HexCosts.NOTHING.totals());
    }

    @Test
    void coinsAsTheWikiWritesThem() {
        assertEquals("&61,000 Coins", HexCosts.line(new Coins(1_000), NAMES));
        assertEquals("&62,957,298.8 Coins", HexCosts.line(new Coins(2_957_298.8), NAMES));
    }
}
