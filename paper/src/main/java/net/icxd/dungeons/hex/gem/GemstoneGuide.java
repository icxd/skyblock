package net.icxd.dungeons.hex.gem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.hex.HexScreen;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.gemstone.GemstoneSlot;
import net.icxd.dungeons.item.gemstone.GemstoneTable;
import net.icxd.dungeons.item.gemstone.GemstoneTable.ArmorSet;
import net.icxd.dungeons.item.gemstone.GemstoneType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.utils.Text;

/**
 * "(1/7) Gemstone Guide", from the grinder's torch: every item that can have gemstones, with its slots (the wiki's
 * Geo/UI, a pager): its header torch in 4, 28 items a page inside the border (rows 2 to 5, columns 2 to 8), Previous
 * and Next Page in 45 and 53, Go Back to the grinder in 48 and Close in 49. Each item is as it is made new, then
 * "&7Available Gemstone Slots" and its slots' types, the same ones together ("  &b✎ Sapphire &8x2"). An armour set
 * shows once, as its first piece with slots (the helmet) named after the set ("&6Divan Armor"; the sets are the
 * gemstone table's, see {@link GemstoneTable#armorSets}; UNKNOWN: Hypixel's set names, "Divan's Armor" in the wiki's
 * copy). The item stays with the session meanwhile. Main thread.
 */
final class GemstoneGuide extends HexScreen {
    static final int HEADER = 4;
    static final int PREVIOUS = 45;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int NEXT = 53;
    static final int PER_PAGE = 28;
    static final Icon HEADER_ICON = new Icon(Material.REDSTONE_TORCH, "&aGemstone Guide", GemstoneGrinder.guideLore(false));
    static final Icon BACK_TO_GRINDER = new Icon(Material.ARROW, "&aGo Back", "&7To Gemstone Grinder");

    /**
     * One of the guide's items.
     *
     * @param name  its set's name, "&6Divan Armor"; null for its own
     * @param slots the lines after its lore
     */
    record Entry(SkyBlockItem item, String name, List<String> slots) {
    }

    private final List<Entry> entries;
    private final int page;
    private final boolean fromHex;

    private GemstoneGuide(HexSession session, List<Entry> entries, int page, boolean fromHex) {
        super(session, title(page, pages(entries.size())));
        this.entries = entries;
        this.page = page;
        this.fromHex = fromHex;
    }

    /** A page of it (from 0), as the item data has the items now. */
    static GemstoneGuide of(HexSession session, int page, boolean fromHex) {
        GemstoneTable table = GemstoneTable.get();
        List<Entry> entries = entries(ItemRegistry.getRegistry().values(), table == null ? Map.of() : table.armorSets());
        return new GemstoneGuide(session, entries, Math.max(0, Math.min(page, pages(entries.size()) - 1)), fromHex);
    }

    /** "(1/7) Gemstone Guide". */
    static String title(int page, int pages) {
        return "(" + (page + 1) + "/" + pages + ") Gemstone Guide";
    }

    static int pages(int entries) {
        return Math.max(1, (entries + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void draw() {
        fill(filler());
        int first = page * PER_PAGE;
        for (int i = first; i < Math.min(entries.size(), first + PER_PAGE); i++) set(place(i - first), stack(entries.get(i)));
        for (Map.Entry<Integer, Icon> e : frame(page, pages(entries.size())).entrySet()) {
            int slot = e.getKey();
            ItemStack stack = e.getValue().stack();
            switch (slot) {
                case PREVIOUS -> set(button(slot, stack, () -> session.open(new GemstoneGuide(session, entries, page - 1, fromHex))));
                case NEXT -> set(button(slot, stack, () -> session.open(new GemstoneGuide(session, entries, page + 1, fromHex))));
                case BACK -> set(button(slot, stack, () -> session.open(new GemstoneGrinder(session, fromHex))));
                default -> set(slot, stack);
            }
        }
        set(GUIClickableItem.close(CLOSE));
    }

    /** Where a page's {@code i}th item goes: rows 2 to 5, columns 2 to 8. */
    static int place(int i) {
        return 10 + i / 7 * 9 + i % 7;
    }

    /** The header, Go Back and the arrows where there's a page that way ("&aNext Page", "&ePage 2": the wiki's pager). */
    static Map<Integer, Icon> frame(int page, int pages) {
        Map<Integer, Icon> icons = new LinkedHashMap<>();
        icons.put(HEADER, HEADER_ICON);
        if (page > 0) icons.put(PREVIOUS, new Icon(Material.ARROW, "&aPrevious Page", "&ePage " + page));
        icons.put(BACK, BACK_TO_GRINDER);
        if (page + 1 < pages) icons.put(NEXT, new Icon(Material.ARROW, "&aNext Page", "&ePage " + (page + 2)));
        return icons;
    }

    /** The item as it's made new (without the random attributes a new one rolls), renamed for its set, with its slots after its lore. */
    private static ItemStack stack(Entry entry) {
        ItemStack stack = ItemBuilder.build(entry.item());
        NBTTagCompound tag = ItemNBT.read(stack);
        if (tag != null && tag.hasKey("attribute_1")) {
            for (String key : List.of("attribute_1", "attribute_1_level", "attribute_2", "attribute_2_level")) tag.remove(key);
            stack = ItemBuilder.build(entry.item(), tag);
        }
        if (entry.name() != null) stack.setData(DataComponentTypes.CUSTOM_NAME, Text.line(entry.name()));
        List<String> more = new ArrayList<>();
        more.add("");
        more.addAll(entry.slots());
        return GemstoneGrinder.withLore(stack, more);
    }

    // The entries

    /**
     * Every item with slots, a set once (see the class's doc), sorted by the item's own name without its colours,
     * not its set's: the wiki's copy has the Helmet of Divan ("Divan's Armor") under H, the Yog Helmet ("Armor of
     * Yog") under Y and the fragged ("⚚") pieces last (a patch sorted it in October 2023). UNKNOWN: Hypixel's
     * exact key (the copy has some Mk. I to III tools out of order).
     */
    static List<Entry> entries(Collection<SkyBlockItem> items, Map<String, ArmorSet> sets) {
        Map<String, SkyBlockItem> byId = new HashMap<>();
        for (SkyBlockItem item : items) byId.put(item.id(), item);
        Map<String, ArmorSet> setOf = new HashMap<>();
        for (ArmorSet set : sets.values()) for (String piece : set.pieces()) setOf.putIfAbsent(piece, set);
        List<Entry> entries = new ArrayList<>();
        List<ArmorSet> shown = new ArrayList<>();
        for (SkyBlockItem item : items) {
            if (item.gemstoneSlots() == null) continue;
            ArmorSet set = setOf.get(item.id());
            if (set == null) {
                entries.add(new Entry(item, null, slotLines(item)));
                continue;
            }
            if (shown.contains(set)) continue;
            shown.add(set);
            SkyBlockItem first = item;
            for (String piece : set.pieces()) {
                SkyBlockItem p = byId.get(piece);
                if (p != null && p.gemstoneSlots() != null) {
                    first = p;
                    break;
                }
            }
            entries.add(new Entry(first, first.rarity().getColor() + set.name(), slotLines(first)));
        }
        entries.sort(Comparator.comparing(GemstoneGuide::sortName));
        return List.copyOf(entries);
    }

    private static String sortName(Entry entry) {
        return entry.item().name().replaceAll("[&§][0-9a-fk-or]", "");
    }

    /**
     * "&7Available Gemstone Slots" and a line a slot type, in the order they first come, how many past one
     * ("  &6⸕ Amber &8x2"): the wiki's. Aquamarine has ☂, as its gems' names have it (the wiki's copy has α).
     */
    static List<String> slotLines(SkyBlockItem item) {
        Map<GemstoneType, Integer> counts = new LinkedHashMap<>();
        for (GemstoneSlot slot : item.gemstoneSlots().getSlots()) counts.merge(slot.getType(), 1, Integer::sum);
        List<String> lines = new ArrayList<>();
        lines.add("&7Available Gemstone Slots");
        for (Map.Entry<GemstoneType, Integer> e : counts.entrySet()) {
            GemstoneType type = e.getKey();
            lines.add("  &" + type.getColor() + type.getIcon() + " " + type.getName() + (e.getValue() > 1 ? " &8x" + e.getValue() : ""));
        }
        return lines;
    }
}
