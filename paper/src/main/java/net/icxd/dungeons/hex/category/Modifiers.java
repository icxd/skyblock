package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.modifier.HexModifiers;
import net.icxd.dungeons.hex.modifier.ModifiersPage;

/**
 * Modifiers: the Recombobulator 3000, Master Stars, Wither Scrolls, Power Scrolls, Enrichments and the like,
 * "The Hex ➜ Modifiers" (the wiki's tabs). Carpentry 20. Its button is the Recombobulator 3000's head (from the
 * item data). It's for an item that takes any of them, and its summary is a line for each (see HexModifiers; the
 * page is ModifiersPage).
 */
public final class Modifiers extends HexCategory {
    private static final List<String> DESCRIPTION = List.of("&7Apply miscellaneous item", "&7modifiers like the", "&6Recombobulator 3000&7,",
            "&5Wither Scrolls&7, and &cMaster", "&cStars&7!");

    public Modifiers() {
        super("Modifiers", 20, DESCRIPTION);
    }

    @Override
    public Look look() {
        return lookOf("RECOMBOBULATOR_3000", new Look(Material.PLAYER_HEAD, null));
    }

    @Override
    public boolean applies(HexItem item) {
        return !HexModifiers.of(item).isEmpty();
    }

    @Override
    public List<String> summary(HexItem item) {
        return HexModifiers.summary(item);
    }

    @Override
    public void open(HexSession session) {
        // The header's lore is the button's, without its summary (the wiki's screens).
        session.open(new ModifiersPage(session, DESCRIPTION, id -> lookOf(id, new Look(Material.PAPER, null))));
    }
}
