package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.gem.GemstoneGrinder;
import net.icxd.dungeons.item.gemstone.GemSlots;
import net.icxd.dungeons.item.gemstone.GemstoneTable;

/**
 * Gemstones, for items with gemstone slots: here it opens Geo's Gemstone Grinder with the item (the owner's wish:
 * "I would also like to be able to access the gemstone menu from The Hex, which isn't usually possible"; Hypixel's
 * Hex has a gem page of its own, "The Hex ➜ Gemstones", whose layout isn't known, U13), whose Go Back comes back
 * here. Carpentry 25. Its button is the Perfect Ruby Gemstone, as the wiki's table of categories pictures it (as the
 * item data has it: paper, today). UNKNOWN (U1): the button's words (ours, below). Its summary is the item's slots
 * as its "Gemstones:" line has them ("  &7Gemstones &8[&7❁&8] &9[&d⚔&9]"). It reads the gemstone table (hex/
 * gemstones.json), which gems on items need wherever they are.
 */
public final class Gemstones extends HexCategory {
    public Gemstones() {
        // UNKNOWN: our own words.
        super("Gemstones", 25, List.of("&7Apply &dGemstones &7to your item", "&7at the &aGemstone Grinder&7!"));
        HexData.add("gemstones", (folder, problems) -> GemstoneTable.read(HexData.json(folder, GemstoneTable.FILE, problems), problems),
                GemstoneTable::use);
    }

    @Override
    public Look look() {
        return lookOf("PERFECT_RUBY_GEM", new Look(Material.RED_DYE, null));
    }

    @Override
    public boolean applies(HexItem item) {
        return item.item().gemstoneSlots() != null;
    }

    @Override
    public List<String> summary(HexItem item) {
        return List.of("  &7Gemstones " + GemSlots.glyphs(GemSlots.of(item.item(), item.tag())));
    }

    @Override
    public void open(HexSession session) {
        session.open(new GemstoneGrinder(session, true));
    }
}
