package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.menu.Icon;

/**
 * Modifiers: the Recombobulator 3000, Master Stars, Wither Scrolls, Power Scrolls, Enrichments and the like,
 * "The Hex ➜ Modifiers" (the wiki's tabs). Carpentry 20. Its button is the Recombobulator 3000's head (from the
 * item data). Stage 1: the button as the wiki has it; LATER (the books, modifiers and item upgrades part): which
 * items, the summary ("  &6Recombobulator 3000 &c✖" ...), the page.
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
        return false;
    }

    @Override
    public List<String> summary(HexItem item) {
        return List.of();
    }

    @Override
    public void open(HexSession session) {
        session.open(new Placeholder(session, "The Hex ➜ Modifiers", new Icon(Material.ANVIL, "&aApply Modifiers", DESCRIPTION)));
    }
}
