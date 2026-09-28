package net.icxd.dungeons.hex.book;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCosts;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.category.Books;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Book;
import net.icxd.dungeons.menu.Icon;

/**
 * "The Hex ➜ Books", as the wiki's Weapon and Armor tabs have it: the header an anvil, "Apply Books", and the books
 * that go on the item centred in the grid (a weapon's Hot Potato Book, Fuming Potato Book, Book of Stats and The
 * Art of War in 21, 22, 24 and 25; armor's potato books and The Art of Peace in 21, 23 and 25). Each is its book's
 * item: its name, its own text, then what applying one costs (the book itself; free on a Sandbox profile, see
 * HexCosts). A click applies one. A book that's on as many times as it goes stays, saying so; the Fuming Potato Book
 * waits for 10 Hot Potato Books (NEU's Hex: it's applicable from 10 to 15). UNKNOWN (U4, U5): the lines for a book
 * that can be applied ("Click to apply!"), a full one ("Item Maxed Out!", after the name NEU's Hex skips) and the
 * Fuming Potato Book too early: our own words. The books' item text is the item data's (the wiki's copies are
 * older: "Stop quoting my brother", now "cousin"). Main thread.
 */
public final class BooksPage extends HexPage {
    public static final String TITLE = "The Hex ➜ Books";
    static final Icon HEADER = new Icon(Material.ANVIL, "&aApply Books", Books.DESCRIPTION);
    /** UNKNOWN (U4): our own words. */
    static final String APPLY = "&eClick to apply!";
    /** UNKNOWN (U5): our own words. */
    static final String MAXED = "&aItem Maxed Out!";
    /** UNKNOWN: our own words. */
    static final String FUMING_TOO_EARLY = "&cRequires &a10 &5Hot Potato Books&c!";

    public BooksPage(HexSession session) {
        super(session, TITLE);
    }

    @Override
    protected Icon header() {
        return HEADER;
    }

    @Override
    protected Placement placement() {
        return Placement.CENTRED;
    }

    @Override
    protected List<Entry> entries() {
        HexItem item = session.hexItem();
        List<Entry> entries = new ArrayList<>();
        for (Book book : Book.on(item.item())) {
            Icon icon = icon(book, item.item(), item.tag(), cost -> cost.lore(session, APPLY));
            entries.add(Entry.of(icon, book.applicable(item.item(), item.tag()) ? () -> apply(book) : null));
        }
        return entries;
    }

    /** One more of the book on the item, paid for with one of it (see HexSession#upgrade: "You applied a ..."). */
    private void apply(Book book) {
        SkyBlockItem kind = session.skyBlockItem();
        NBTTagCompound tag = session.tag();
        if (kind == null || tag == null || !book.applicable(kind, tag)) {
            redraw();
            return;
        }
        book.apply(tag);
        if (!session.upgrade(cost(book), tag, book.displayName())) redraw();
    }

    /** What one costs: one of the book, from wherever the Hex takes items. */
    static HexCosts cost(Book book) {
        return HexCosts.of(new HexCosts.Items(book.itemId(), 1));
    }

    /**
     * A book's entry for an item with this data: the book's look and name, its item's text, a blank line, and then
     * the Cost block ({@code costLore}: HexCosts#lore) if one can go on now, else why not.
     */
    static Icon icon(Book book, SkyBlockItem item, NBTTagCompound tag, Function<HexCosts, List<String>> costLore) {
        SkyBlockItem bookItem = ItemRegistry.get(book.itemId());
        List<String> lore = new ArrayList<>(bookItem == null ? List.of() : bookItem.lore());
        if (!lore.isEmpty()) lore.add("");
        if (book.maxed(tag)) lore.add(MAXED);
        else if (!book.applicable(item, tag)) lore.add(FUMING_TOO_EARLY);
        else lore.addAll(costLore.apply(cost(book)));
        return new Icon(bookItem == null ? Material.BOOK : bookItem.material(), book.displayName(), lore);
    }
}
