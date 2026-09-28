package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.menu.Icon;

/**
 * Item Upgrades: Convert to Dungeon Item and Star Upgrades, "The Hex ➜ Item Upgrades" (the wiki's The Hex).
 * Carpentry 25. Its button is Dragon Essence's head, as the wiki's table of categories pictures it (the item
 * data's, else NEU's ESSENCE_DRAGON skin). UNKNOWN (U1, U12): the button's words (ours, below) and the page's
 * header. Stage 1: the button; LATER (the books, modifiers and item upgrades part): which items, the summary
 * ("  &7Dungeon Item &a✔", "  &7Upgrade Level &6✪✪✪✪✪"), the page.
 */
public final class ItemUpgrades extends HexCategory {
    /** NEU's ESSENCE_DRAGON head, for when the item data has no Dragon Essence (it doesn't, today). */
    static final String DRAGON_ESSENCE_HEAD = "33ff416aa8bec1665b92701fbe68a4effff3d06ed9147454fa77712dd6079b33";

    public ItemUpgrades() {
        // UNKNOWN: our own words.
        super("Item Upgrades", 25, List.of("&7Upgrade your item with &6Stars", "&7or convert it into a &cDungeon", "&citem&7!"));
    }

    @Override
    public Look look() {
        return lookOf("ESSENCE_DRAGON", new Look(Material.PLAYER_HEAD, DRAGON_ESSENCE_HEAD));
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
        // UNKNOWN: the header (NEU's Hex only says it's an anvil or an enchantment table).
        session.open(new Placeholder(session, "The Hex ➜ Item Upgrades", new Icon(Material.ANVIL, "&aItem Upgrades", description())));
    }
}
