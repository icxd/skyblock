package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;

/**
 * Ultimate Enchantments: the same "The Hex ➜ Enchant Item" page, listing the ultimate enchantments (the wiki's
 * Weapon tab). No requirement. Stage 1: the button as the wiki has it; LATER (the enchantments part): which
 * items, the summary ("  &7Ultimate Enchantments &e0&7/&a1"), the page.
 */
public final class UltimateEnchantments extends HexCategory {
    public UltimateEnchantments() {
        super("Ultimate Enchantments", 0, List.of("&7Allows you to apply &d&lUltimate", "&d&lEnchantments &7and gives you the",
                "&7option to consume &aBottles of", "&aEnchanting &7directly!"));
    }

    @Override
    public Look look() {
        return new Look(Material.WRITABLE_BOOK, null);
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
        session.open(new Placeholder(session, Enchantments.TITLE, Enchantments.header()));
    }
}
