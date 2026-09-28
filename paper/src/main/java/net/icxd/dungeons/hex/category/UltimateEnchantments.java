package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Material;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.enchant.EnchantItemPage;
import net.icxd.dungeons.hex.enchant.EnchantRules;
import net.icxd.dungeons.item.enchanting.EnchantmentData;

/**
 * Ultimate Enchantments: the same "The Hex ➜ Enchant Item" page, listing the ultimate enchantments (the wiki's
 * Weapon tab), for every item an ultimate goes on (weapons, wands, armor, equipment, tools). No requirement. Its
 * summary is "  &7Ultimate Enchantments &e0&7/&a1": only one goes on an item.
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
        return !EnchantRules.offered(EnchantmentData.current(), item.item(), true).isEmpty();
    }

    @Override
    public List<String> summary(HexItem item) {
        return List.of(EnchantRules.ultimateSummary(EnchantmentData.current(), EnchantRules.on(item.tag())));
    }

    @Override
    public void open(HexSession session) {
        session.open(new EnchantItemPage(session, true));
    }
}
