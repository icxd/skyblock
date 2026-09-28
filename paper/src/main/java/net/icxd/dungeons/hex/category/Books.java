package net.icxd.dungeons.hex.category;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.book.BooksPage;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.upgrade.Book;

/**
 * Books: Hot and Fuming Potato Books, the Book of Stats, The Art of War and Peace and the rest (see {@link Book}),
 * "The Hex ➜ Books" ({@link BooksPage}; the wiki's Weapon and Armor tabs). Carpentry 20. For any item a book goes
 * on; its summary is a line for each of those books, as the official screenshot and the wiki's tabs have them:
 * "  &5Hot Potato Book &e0&7/&a10" (the count green once it's full: the screenshot's 10/10), "  &6The Art of War
 * &c✖" (&a✔ once it's on).
 */
public final class Books extends HexCategory {
    public static final List<String> DESCRIPTION = List.of("&7Knowledge is &6power&7! Apply", "&7special books to your item to", "&7upgrade it!");

    public Books() {
        super("Books", 20, DESCRIPTION);
    }

    @Override
    public Look look() {
        return new Look(Material.BOOK, null);
    }

    @Override
    public boolean applies(HexItem item) {
        return !Book.on(item.item()).isEmpty();
    }

    @Override
    public List<String> summary(HexItem item) {
        List<String> lines = new ArrayList<>();
        for (Book book : Book.on(item.item())) lines.add(line(book, item.tag()));
        return lines;
    }

    /** A book's line: how many are on it of how many ("&e3&7/&a10"), or for a book that goes on once, ✔ or ✖. */
    static String line(Book book, NBTTagCompound tag) {
        int count = book.count(tag);
        if (book.max() == 1) return "  " + book.displayName() + (count > 0 ? " &a✔" : " &c✖");
        return "  " + book.displayName() + " " + (book.maxed(tag) ? "&a" : "&e") + count + "&7/&a" + book.max();
    }

    @Override
    public void open(HexSession session) {
        session.open(new BooksPage(session));
    }
}
