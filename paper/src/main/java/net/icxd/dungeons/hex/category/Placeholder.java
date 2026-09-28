package net.icxd.dungeons.hex.category;

import java.util.List;

import net.icxd.dungeons.hex.HexPage;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.menu.Icon;

/**
 * A category's page until its own is built (task 70's later parts): the frame with the page's title and header,
 * and no entries. Goes once no category opens it.
 */
final class Placeholder extends HexPage {
    private final Icon header;

    Placeholder(HexSession session, String title, Icon header) {
        super(session, title);
        this.header = header;
    }

    @Override
    protected Icon header() {
        return header;
    }

    @Override
    protected List<Entry> entries() {
        return List.of();
    }
}
