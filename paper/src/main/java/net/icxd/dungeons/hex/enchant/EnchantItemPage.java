package net.icxd.dungeons.hex.enchant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.category.Enchantments;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.EnchantmentData.Level;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * "The Hex ➜ Enchant Item", as the wiki's Weapon and Armor tabs and NEU's Hex have it: the enchantments the Hex has
 * for the item (the normal ones, or the ultimate ones: both categories open this page), 15 a page row by row, each an
 * enchanted book saying what its lowest level does and whether the item has it, that opens its levels ({@link
 * EnchantLevelPage}). The header in 28 (Enchant Item) and the Sort in 51. The wiki's screens have Bookshelf Power in 48, but SkyBlock has none since 0.26.1, so 48 is glass. Main thread.
 */
public final class EnchantItemPage extends HexPage {
    /** How the list is sorted: the Sort button goes through them in this order. */
    public enum Sort {
        DEFAULT("Default"),
        MISSING_FIRST("Missing Enchantments First"),
        A_TO_Z("A to Z"),
        Z_TO_A("Z to A");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        Sort next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** Which list, sorted how, on which page: where Go Back on a level page takes them. */
    public record Shown(boolean ultimate, Sort sort, int page) {
        EnchantItemPage open(HexSession session) {
            EnchantItemPage page = new EnchantItemPage(session, ultimate);
            page.sort = sort;
            if (this.page > 0) page.page(this.page);
            return page;
        }
    }

    private final boolean ultimate;
    private Sort sort = Sort.DEFAULT;

    public EnchantItemPage(HexSession session, boolean ultimate) {
        super(session, Enchantments.TITLE);
        this.ultimate = ultimate;
    }

    Shown shown() {
        return new Shown(ultimate, sort, page());
    }

    @Override
    protected Icon header() {
        return Enchantments.header();
    }

    @Override
    protected List<Entry> entries() {
        HexItem item = session.hexItem();
        EnchantmentData data = EnchantmentData.current();
        Map<String, Integer> on = EnchantRules.on(item.tag());
        List<Entry> entries = new ArrayList<>();
        for (EnchantmentData.Entry enchantment : sorted(EnchantRules.offered(data, item.item(), ultimate), sort, on)) {
            entries.add(Entry.of(icon(enchantment, on.get(enchantment.id())),
                    () -> session.open(new EnchantLevelPage(session, enchantment, shown()))));
        }
        return entries;
    }

    @Override
    protected Map<Integer, Entry> extras() {
        return Map.of(FAR_RIGHT, Entry.of(sortButton(sort), () -> {
                    sort = sort.next();
                    page(0);
                }));
    }

    // What it shows

    /** The list in this sort: Default is the Hex's order, the missing ones first keeps it among each. */
    static List<EnchantmentData.Entry> sorted(List<EnchantmentData.Entry> offered, Sort sort, Map<String, Integer> on) {
        List<EnchantmentData.Entry> list = new ArrayList<>(offered);
        Comparator<EnchantmentData.Entry> byName = Comparator.comparing(e -> e.name().toLowerCase());
        switch (sort) {
            case DEFAULT -> {
            }
            case MISSING_FIRST -> list.sort(Comparator.comparing(e -> on.containsKey(e.id())));
            case A_TO_Z -> list.sort(byName);
            case Z_TO_A -> list.sort(byName.reversed());
        }
        return list;
    }

    /**
     * An enchantment's book: "&aSharpness" (an ultimate "&d&lOne For All"), what its lowest level does, whether the item
     * has it ("  &cSharpness&c ✖", the wiki's; with it, UNKNOWN: "  &aSharpness V&a ✔", in green with its level), and
     * "&eClick to view!".
     */
    static Icon icon(EnchantmentData.Entry enchantment, Integer level) {
        List<String> lore = new ArrayList<>(description(enchantment, enchantment.min()));
        lore.add("");
        lore.add(level == null ? "  &c" + enchantment.name() + "&c ✖" : "  &a" + enchantment.name() + " " + Utils.getRomanNumeral(level) + "&a ✔");
        lore.add("");
        lore.add("&eClick to view!");
        return new Icon(Material.ENCHANTED_BOOK, (enchantment.ultimate() ? "&d&l" : "&a") + enchantment.name(), lore);
    }

    /**
     * What a level says, as its enchanted book has the lines (the wiki's Hex screens show them so), and what the next
     * tier takes for one that grows ("&850k Combat XP to tier up!", the wiki's Champion). A level with no book of its
     * own: its text wrapped as item lore is.
     */
    static List<String> description(EnchantmentData.Entry enchantment, int level) {
        Level at = enchantment.level(level);
        if (at == null) return List.of();
        List<String> lines = new ArrayList<>(at.lines() != null ? at.lines() : Text.wrap(at.text(), Text.LORE_WIDTH));
        if (at.tierUp() != null) lines.add(at.tierUp());
        return lines;
    }

    /** The Sort, in 51 (the wiki's): the one in use marked "&b▶ ". */
    static Icon sortButton(Sort sort) {
        List<String> lore = new ArrayList<>(List.of("&7Change how Enchantments are", "&7sorted.", ""));
        for (Sort s : Sort.values()) lore.add(s == sort ? "&b▶ " + s.label : "&7" + s.label);
        lore.add("");
        lore.add("&eClick to switch sort!");
        return new Icon(Material.HOPPER, "&aSort", lore);
    }
}
