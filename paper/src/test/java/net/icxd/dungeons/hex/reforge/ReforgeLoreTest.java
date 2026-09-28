package net.icxd.dungeons.hex.reforge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.reforge.ReforgeStats;
import net.icxd.dungeons.stats.Stat;

/**
 * "The Hex ➜ Reforges"' words: a stone's entry laid out as the wiki's Weapon tab has the Wither Blood (on a made-up
 * stone and reforge), the Random Basic Reforge button, the main menu's line and the chat line.
 */
class ReforgeLoreTest {
    private static final Reforge KEEN = new Reforge("keen", "Keen",
            new ReforgeStats(Map.of(Rarity.LEGENDARY, Map.of(Stat.STRENGTH, 135.0, Stat.HEALTH, 4.0))), null,
            Map.of(Rarity.LEGENDARY, List.of("&7Grants &a+1 &c❁ Strength &7per", "&cTest &7level.")), Map.of(Stat.STRENGTH, 1.0), Map.of());

    /** The stone's own lore up to its "&9Keen &7(&6Legendary&7):", which the entry writes for the item's rarity. */
    private static final List<String> STONE = List.of("&7Applies the &9Keen &7reforge when", "&7combined with a test.", "",
            "&8&oA flavour line.", "", "&9Keen &7(&6Legendary&7):", "&7Strength: &c+135", "", "&9Keen Bonus", "&7Grants...");

    /** The wiki's Wither Blood: what it is, what it gives at the item's rarity for the viewer's level, the bonus, the Cost. */
    @Test
    void aStone() {
        List<String> cost = List.of("&7Cost", "&610,000 Coins &a✔", "&5Test Stone &c✖", "", "&cYou don't have that in your", "&cinventories!");
        assertEquals(List.of(
                "&7Applies the &9Keen &7reforge when",
                "&7combined with a test.",
                "",
                "&8&oA flavour line.",
                "",
                "&9Keen &7(&6Legendary&7):",
                "&7Health: &c+4",
                "&7Strength: &c+147",
                "",
                "&9Keen Bonus",
                "&7Grants &a+1 &c❁ Strength &7per",
                "&cTest &7level.",
                "",
                "&7Cost",
                "&610,000 Coins &a✔",
                "&5Test Stone &c✖",
                "",
                "&cYou don't have that in your",
                "&cinventories!"), ReforgeLore.stone(STONE, KEEN, Rarity.LEGENDARY, 12, cost));
    }

    /** At a rarity it gives nothing at (Divine), no stats block; the bonus stays. */
    @Test
    void aStoneAtARarityWithNoNumbers() {
        List<String> lore = ReforgeLore.stone(STONE, KEEN, Rarity.DIVINE, 0, List.of("&7Cost", "&aFree", "", ReforgeLore.CLICK));
        assertEquals(List.of("&8&oA flavour line.", "", "&9Keen Bonus"), lore.subList(3, 6));
    }

    @Test
    void rarityNames() {
        assertEquals("&6Legendary", ReforgeLore.rarityName(Rarity.LEGENDARY));
        assertEquals("&cVery Special", ReforgeLore.rarityName(Rarity.VERY_SPECIAL));
    }

    /** The wiki's button: 250 Coins on its Common sword, "Click to reforge!" last (SkyHanni's ReforgeHelper wants it). */
    @Test
    void theRandomButton() {
        assertEquals(List.of("&7Apply a random, basic Reforge to", "&7the item, like in the Blacksmith", "&7anvil!", "", "&7Cost",
                "&6250 Coins", "", "&eClick to reforge!"), ReforgeLore.random(250, false, true));
        assertEquals(List.of("&7Cost", "&610,000 Coins", "", "&cYou don't have enough Coins!"),
                ReforgeLore.random(10_000, false, false).subList(4, 8));
        assertEquals(List.of("&7Cost", "&aFree", "", "&eClick to reforge!"), ReforgeLore.random(10_000, true, false).subList(4, 8));
    }

    @Test
    void summary() {
        assertEquals(List.of("  &7Reforge &c✖"), ReforgeLore.summary(null));
        assertEquals(List.of("  &7Reforge &a✔", "    &9Keen"), ReforgeLore.summary(KEEN));
    }

    /** SkyHanni's test line: "You reforged your Gentle Dreadlord Sword into a Heroic Dreadlord Sword!". */
    @Test
    void chat() {
        assertEquals("&aYou reforged your &5Gentle Test Sword &ainto a &5Heroic Test Sword&a!",
                ReforgeLore.reforged("&5Gentle Test Sword", "&5Heroic Test Sword"));
        assertEquals("&aYou reforged your &5Test Sword &ainto an &5Epic Test Sword&a!", ReforgeLore.reforged("&5Test Sword", "&5Epic Test Sword"));
    }

    @Test
    void costBlocks() {
        List<String> cost = List.of("&7Cost", "&61,000 Coins &c✖", "", "&cYou don't have enough Coins!");
        assertEquals(List.of("&7Cost", "&61,000 Coins &c✖", "", ReforgeLore.ALREADY), ReforgeLore.withAction(cost, ReforgeLore.ALREADY));
        assertEquals("&cYou don't have enough Coins!", ReforgeLore.missing(cost));
        assertEquals("&cYou don't have that in your &cinventories!",
                ReforgeLore.missing(List.of("&7Cost", "&5Stone &c✖", "", "&cYou don't have that in your", "&cinventories!")));
    }
}
