package net.icxd.dungeons.hex.enchant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.enchant.EnchantItemPage.Sort;
import net.icxd.dungeons.hex.enchant.EnchantLevelPage.Offer;
import net.icxd.dungeons.hex.enchant.EnchantRules.Action;
import net.icxd.dungeons.hex.enchant.EnchantRules.Change;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.FakeEnchantments;
import net.icxd.dungeons.menu.Icon;

/** The Enchant Item pages, as data, against the wiki's screens (a made-up table). */
class EnchantPagesTest {
    private EnchantmentData data;

    @BeforeEach
    void table() {
        data = FakeEnchantments.use();
    }

    @AfterEach
    void noTable() {
        FakeEnchantments.reset();
    }

    @Test
    void listEntries() {
        // The wiki's: the lowest level's text (the book's lines), whether it's on the item, "Click to view!".
        assertEquals(new Icon(Material.ENCHANTED_BOOK, "&aSharpness", "&7Increases melee damage dealt by &a5%", "", "  &cSharpness&c ✖", "",
                "&eClick to view!"), EnchantItemPage.icon(data.get("sharpness"), null));
        // UNKNOWN (U6): on the item, its level in green.
        assertEquals("  &aSharpness V&a ✔", EnchantItemPage.icon(data.get("sharpness"), 5).lore().get(2));
        // The book's own lines, and what the next tier takes (the wiki's Champion).
        assertEquals(new Icon(Material.ENCHANTED_BOOK, "&aChampion", "&7Gain &a3% &7extra", "&7Combat XP.", "&850k Combat XP to tier up!", "",
                "  &cChampion&c ✖", "", "&eClick to view!"), EnchantItemPage.icon(data.get("champion"), null));
        assertEquals("&d&lOne For All", EnchantItemPage.icon(data.get("one_for_all"), null).name());
    }

    @Test
    void sorts() {
        List<EnchantmentData.Entry> offered = List.of(data.get("sharpness"), data.get("champion"), data.get("smite"), data.get("critical"));
        Map<String, Integer> on = Map.of("sharpness", 5, "smite", 7);
        assertEquals(List.of("sharpness", "champion", "smite", "critical"), ids(EnchantItemPage.sorted(offered, Sort.DEFAULT, on)));
        assertEquals(List.of("champion", "critical", "sharpness", "smite"), ids(EnchantItemPage.sorted(offered, Sort.MISSING_FIRST, on)));
        assertEquals(List.of("champion", "critical", "sharpness", "smite"), ids(EnchantItemPage.sorted(offered, Sort.A_TO_Z, on)));
        assertEquals(List.of("smite", "sharpness", "critical", "champion"), ids(EnchantItemPage.sorted(offered, Sort.Z_TO_A, on)));
        assertEquals(Sort.DEFAULT, Sort.Z_TO_A.next());
    }

    private static List<String> ids(List<EnchantmentData.Entry> entries) {
        return entries.stream().map(EnchantmentData.Entry::id).toList();
    }

    /** The wiki's Sort. */
    @Test
    void buttons() {
        assertEquals(new Icon(Material.HOPPER, "&aSort", "&7Change how Enchantments are", "&7sorted.", "", "&b▶ Default",
                "&7Missing Enchantments First", "&7A to Z", "&7Z to A", "", "&eClick to switch sort!"), EnchantItemPage.sortButton(Sort.DEFAULT));
        assertEquals("&b▶ A to Z", EnchantItemPage.sortButton(Sort.A_TO_Z).lore().get(5));
    }

    private static final List<String> BLOCK = List.of("&7Cost", "&3100 Exp Levels &a✔", "", "&eClick to enchant!");

    private static Offer offer(int level, Change change, boolean book, List<String> blocked) {
        return new Offer(level, change, HexCosts.of(new HexCosts.Levels(100)), 100, book, blocked);
    }

    @Test
    void levels() {
        EnchantmentData.Entry sharpness = data.get("sharpness");
        Change apply = new Change(Action.APPLY, Map.of("sharpness", 5), List.of());
        assertEquals(new Icon(Material.ENCHANTED_BOOK, "&aSharpness V", "&7Increases melee damage dealt by &a30%", "", "&7Cost",
                "&3100 Exp Levels &a✔", "", "&eClick to enchant!"), EnchantLevelPage.icon(data, sharpness, offer(5, apply, false, null), BLOCK));
        // Above the Enchantment Table's: its book too, which nobody has yet.
        Change six = new Change(Action.APPLY, Map.of("sharpness", 6), List.of());
        assertEquals(List.of("&7Increases melee damage dealt by &a45%", "", "&7Cost", "&9Enchanted Book (Sharpness VI) &c✖", "&3100 Exp Levels &a✔",
                        "", "&cYou don't have that in your", "&cinventories!"),
                EnchantLevelPage.icon(data, sharpness, offer(6, six, true, EnchantLevelPage.NO_BOOK), BLOCK).lore());
        // Short of the Enchanting level: the plugin's requirement line in place of the action.
        List<String> requirement = List.of(EnchantLevelPage.requirement(15));
        assertEquals("&4❣ &cRequires &aEnchanting Skill 15&c.", requirement.getFirst());
        Change drain = new Change(Action.APPLY, Map.of("syphon", 1), List.of("life_steal", "mana_steal"));
        assertEquals(List.of("&7Heals on crits.", "", "&cReplaces Life Steal, Mana Steal", "", "&7Cost", "&3100 Exp Levels &a✔", "",
                requirement.getFirst()), EnchantLevelPage.icon(data, data.get("syphon"), offer(1, drain, false, requirement), BLOCK).lore());
        // Lower than the item's.
        Change lower = new Change(Action.LOWER, Map.of("sharpness", 6), List.of());
        assertEquals(List.of("&7Increases melee damage dealt by &a5%", "", EnchantLevelPage.LOWER),
                EnchantLevelPage.icon(data, sharpness, offer(1, lower, false, List.of(EnchantLevelPage.LOWER)), List.of()).lore());
        // Nothing to pay (no Exp levels known): the action alone.
        Change remove = new Change(Action.REMOVE, Map.of(), List.of());
        assertEquals(List.of("&7Increases melee damage dealt by &a5%", "", EnchantLevelPage.REMOVE),
                EnchantLevelPage.icon(data, sharpness, offer(1, remove, false, null), List.of()).lore());
        assertEquals("&d&lOne For All I", EnchantLevelPage.icon(data, data.get("one_for_all"), offer(1, apply, false, null), BLOCK).name());
    }

    @Test
    void actions() {
        assertEquals("&eClick to enchant!", EnchantLevelPage.action(Action.APPLY));
        assertEquals("&eClick to upgrade!", EnchantLevelPage.action(Action.UPGRADE));
        assertEquals("&eClick to remove!", EnchantLevelPage.action(Action.REMOVE));
    }
}
