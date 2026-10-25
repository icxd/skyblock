package net.icxd.dungeons.museum;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import org.bson.Document;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import net.icxd.dungeons.gui.MenuSound;
import net.icxd.dungeons.gui.item.GUIClickableItem;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.museum.Museum.Donation;
import net.icxd.dungeons.museum.Museum.Sort;
import net.icxd.dungeons.museum.MuseumData.Category;
import net.icxd.dungeons.museum.MuseumData.Entry;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;
import net.kyori.adventure.text.Component;

/**
 * A category's pages, "(1/9) Museum ➜ Combat" ("Museum ➜ Foraging" when it has one), as recorded (2026-10-25, 01:15.9
 * to 02:37.1): its item, then 28 entries a page inside the glass, in the sort's order. A missing entry is gray dye with
 * its name in red and how to donate it; a donated one is the item itself (a set's helmet) with when it was donated and
 * what a click does: give it back ("Click to retrieve item!"); a retrieved one is lime dye with the item's name and lore
 * and that it can come back. One that counts by its higher tier's donation is lime dye too (UNKNOWN: never recorded).
 * Previous and Next Page (silent, as recorded), Extra Details (a toggle: the menu opens again, with a click), Go Back,
 * Close, Museum Search (does nothing here), Sort (left for the next, right for the one before; UNKNOWN: the recording
 * never sorted, so the page goes back to the first). Special Items' page is the same, with nothing in it (none can be
 * donated here). Main thread.
 */
final class MuseumCategoryMenu extends MuseumGui {
    /** Where entries go: the four middle rows, inside the glass. */
    static final int[] ENTRY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    static final int ITEM = 4;
    static final int PREVIOUS = 45;
    static final int DETAILS = 47;
    static final int BACK = 48;
    static final int CLOSE = 49;
    static final int SEARCH = 50;
    static final int SORT = 51;
    static final int NEXT = 53;
    static final String SPECIAL_ITEMS = "Special Items";

    /** null for Special Items. */
    private final String category;
    private final int page;

    MuseumCategoryMenu(Player viewer, String category, int page) {
        super(title(name(category), page, pages(Museum.data(), category)), Size.SIX, viewer);
        this.category = category;
        this.page = page;
    }

    private static String name(String category) {
        Category found = category == null ? null : Museum.data().category(category);
        return category == null ? SPECIAL_ITEMS : found == null ? category : found.name();
    }

    private static int pages(MuseumData data, String category) {
        Category found = category == null ? null : data.category(category);
        return pages(found == null ? 0 : found.entries().size());
    }

    static int pages(int entries) {
        return Math.max(1, (entries + ENTRY_SLOTS.length - 1) / ENTRY_SLOTS.length);
    }

    /** "(1/9) Museum ➜ Combat"; "Museum ➜ Foraging" with one page. */
    static String title(String name, int page, int pages) {
        return (pages > 1 ? "(" + page + "/" + pages + ") " : "") + "Museum ➜ " + name;
    }

    @Override
    void reopen(Player player) {
        new MuseumCategoryMenu(player, category, page).open(player, MenuSound.CLICK);
    }

    @Override
    public void beforeOpen(Player player) {
        getItems().clear();
        fill(filler());
        User user = User.ifLoaded(viewer.getUniqueId());
        Document profile = user == null ? new Document() : user.profile();
        MuseumData data = Museum.data();
        boolean details = Museum.details(profile);
        Sort sort = Museum.sort(profile);
        Category shown = category == null ? null : data.category(category);
        if (shown != null && MuseumMenu.LOOKS.containsKey(category)) {
            set(ITEM, MuseumMenu.category(shown, MuseumMenu.LOOKS.get(category), Museum.counted(profile, data, shown), false).stack());
        } else if (category == null) {
            set(ITEM, MuseumMenu.special(false).stack());
        }
        // Special Items' page has nothing inside the glass; a category's last page leaves its unused slots empty.
        for (int slot : ENTRY_SLOTS) set(slot, null);
        List<Entry> entries = shown == null ? List.of() : sorted(shown.entries(), sort, e -> Museum.counts(profile, data, e));
        int pages = pages(entries.size());
        int first = (page - 1) * ENTRY_SLOTS.length;
        for (int i = 0; i < ENTRY_SLOTS.length && first + i < entries.size(); i++) {
            Entry entry = entries.get(first + i);
            Donation donation = Museum.donation(profile, entry.id());
            ItemStack stack = entryItem(entry, donation, Museum.credited(profile, data, entry), details);
            if (donation != null && !donation.retrieved()) {
                set(GUIClickableItem.button(ENTRY_SLOTS[i], stack, viewer, () -> MuseumDonations.retrieve(viewer, entry, () -> reopen(viewer))));
            } else {
                set(ENTRY_SLOTS[i], stack);
            }
        }
        if (page > 1) set(GUIClickableItem.button(PREVIOUS, page("&aPrevious Page", page - 1), viewer, () -> turn(page - 1)));
        if (page < pages) set(GUIClickableItem.button(NEXT, page("&aNext Page", page + 1), viewer, () -> turn(page + 1)));
        set(GUIClickableItem.button(DETAILS, new Icon(details ? Material.LIME_TERRACOTTA : Material.RED_TERRACOTTA, "&aExtra Details",
                MuseumText.extraDetailsLore(details)).stack(), viewer, () -> toggleDetails()));
        set(GUIClickableItem.button(BACK, new Icon(Material.ARROW, "&aGo Back", "&7To Your Museum").stack(), viewer,
                () -> MuseumMenu.show(viewer, MenuSound.CLICK)));
        set(GUIClickableItem.close(CLOSE));
        set(SEARCH, MuseumMenu.search().stack());
        set(GUIClickableItem.button(SORT, new Icon(Material.HOPPER, "&aSort", MuseumText.sortLore(sort)).stack(), viewer,
                (ClickType click) -> resort(click.isRightClick())));
    }

    private static ItemStack page(String name, int to) {
        return new Icon(Material.ARROW, name, "&ePage " + to).stack();
    }

    private void turn(int to) {
        new MuseumCategoryMenu(viewer, category, to).open(viewer, null);
    }

    private void toggleDetails() {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Museum.details(user.profile(), !Museum.details(user.profile()));
        reopen(viewer);
    }

    private void resort(boolean back) {
        User user = User.ifLoaded(viewer.getUniqueId());
        if (user == null) return;
        Museum.sort(user.profile(), Museum.sort(user.profile()).step(back));
        new MuseumCategoryMenu(viewer, category, 1).open(viewer, MenuSound.CLICK);
    }

    /**
     * The entries in this sort's order: Ascending XP is the menus' own (the data's); the others keep it among equals
     * (UNKNOWN how Hypixel breaks ties).
     */
    static List<Entry> sorted(List<Entry> entries, Sort sort, Predicate<Entry> counts) {
        List<Entry> out = new ArrayList<>(entries);
        Comparator<Entry> byName = Comparator.comparing(e -> e.name().toLowerCase(Locale.ROOT));
        switch (sort) {
            case ASCENDING_XP -> {
            }
            case DESCENDING_XP -> out.sort(Comparator.comparingInt(Entry::xp).reversed());
            case MISSING_FIRST -> out.sort(Comparator.comparing(e -> counts.test(e)));
            case A_TO_Z -> out.sort(byName);
            case Z_TO_A -> out.sort(byName.reversed());
        }
        return out;
    }

    /** What an entry shows: missing, donated, retrieved, or counted by its higher tier. */
    static ItemStack entryItem(Entry entry, Donation donation, boolean credited, boolean details) {
        if (donation == null) {
            return credited ? new Icon(Material.LIME_DYE, "&a" + entry.name(), MuseumText.credited(entry)).stack()
                    : new Icon(Material.GRAY_DYE, "&c" + entry.name(), MuseumText.missing(entry)).stack();
        }
        ItemStack item = MuseumDonations.shown(MuseumDonations.stored(donation));
        if (item == null) {
            // Its bytes can't be read here: what it was is shown as a set would be, by name.
            item = new Icon(Material.LIME_DYE, "&a" + entry.name(), List.of()).stack();
        } else if (donation.retrieved()) {
            item = item.withType(Material.LIME_DYE);
        }
        item.setAmount(1);
        return withLore(item, MuseumText.donatedLines(entry, donation.at(), created(item), details, donation.retrieved()));
    }

    /** When it was made, if it says (Hypixel's {@code timestamp}); 0 if not. */
    private static long created(ItemStack item) {
        NBTTagCompound tag = ItemNBT.read(item);
        return tag == null || !tag.hasKey(ItemBuilder.CREATED) ? 0 : tag.getLong(ItemBuilder.CREATED);
    }

    private static ItemStack withLore(ItemStack stack, List<String> more) {
        ItemLore lore = stack.getData(DataComponentTypes.LORE);
        List<Component> lines = new ArrayList<>(lore == null ? List.of() : lore.lines());
        lines.addAll(Text.lines(more));
        stack.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
        return stack;
    }
}
