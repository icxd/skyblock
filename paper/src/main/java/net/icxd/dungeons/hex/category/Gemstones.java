package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.menu.Icon;

/**
 * Gemstones: here it opens Geo's Gemstone Grinder with the item (the owner's wish; Hypixel's Hex has a gem page of
 * its own, "The Hex ➜ Gemstones", whose layout isn't known, U13). Carpentry 25. Its button is the Perfect Ruby
 * Gemstone, as the wiki's table of categories pictures it (as the item data has it: paper, today). UNKNOWN (U1):
 * the button's words (ours, below). Stage 1: the button; LATER (the gemstones and grinder part): which items, the
 * summary ("  &7Gemstones &8[&7❁&8]"), the grinder.
 */
public final class Gemstones extends HexCategory {
    public Gemstones() {
        // UNKNOWN: our own words.
        super("Gemstones", 25, List.of("&7Apply &dGemstones &7to your item", "&7at the &aGemstone Grinder&7!"));
    }

    @Override
    public Look look() {
        return lookOf("PERFECT_RUBY_GEM", new Look(Material.RED_DYE, null));
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
        session.open(new Placeholder(session, "The Hex ➜ Gemstones", new Icon(Material.END_PORTAL_FRAME, "&aGemstones", description())));
    }
}
