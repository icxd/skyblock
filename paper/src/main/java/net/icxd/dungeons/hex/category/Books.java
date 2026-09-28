package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.menu.Icon;

/**
 * Books: Hot and Fuming Potato Books, the Book of Stats, The Art of War and Peace and the like, "The Hex ➜ Books"
 * (the wiki's Weapon and Armor tabs). Carpentry 20. Stage 1: the button as the wiki has it; LATER (the books,
 * modifiers and item upgrades part): which items, the summary ("  &5Hot Potato Book &e0&7/&a10" ...), the page.
 */
public final class Books extends HexCategory {
    private static final List<String> DESCRIPTION = List.of("&7Knowledge is &6power&7! Apply", "&7special books to your item to", "&7upgrade it!");

    public Books() {
        super("Books", 20, DESCRIPTION);
    }

    @Override
    public Look look() {
        return new Look(Material.BOOK, null);
    }

    @Override
    public boolean applies(HexItem item) {
        return false;
    }

    @Override
    public List<String> summary(HexItem item) {
        return List.of();
    }

    @Override
    public void open(HexSession session) {
        session.open(new Placeholder(session, "The Hex ➜ Books", new Icon(Material.ANVIL, "&aApply Books", DESCRIPTION)));
    }
}
