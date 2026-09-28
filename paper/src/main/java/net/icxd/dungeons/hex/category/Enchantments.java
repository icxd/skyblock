package net.icxd.dungeons.hex.category;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.icxd.dungeons.hex.HexCategory;
import net.icxd.dungeons.hex.HexData;
import net.icxd.dungeons.hex.HexItem;
import net.icxd.dungeons.hex.HexSession;
import net.icxd.dungeons.hex.enchant.EnchantItemPage;
import net.icxd.dungeons.hex.enchant.EnchantRules;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.user.User;

/**
 * Enchantments: the Hex's Enchantment Table, "The Hex ➜ Enchant Item" (the wiki's Weapon tab), for every item some
 * enchantment goes on (weapons, armor, equipment, tools). No requirement. Its summary is "  &7Enchantments
 * &e0&7/&a26": how many are on the item, of how many can be at once (see EnchantRules). The page and its level
 * pages are in {@code hex/enchant}. It also has the enchantments' table read with the Hex's (EnchantmentData, which
 * items' lore needs too).
 */
public final class Enchantments extends HexCategory {
    public static final String TITLE = "The Hex ➜ Enchant Item";

    public Enchantments() {
        super("Enchantments", 0, List.of("&7This special &aEnchantment Table", "&7allows you to access way more",
                "&7Enchantments and gives you the", "&7option to consume &aBottles of", "&aEnchanting &7directly!"));
        HexData.add("enchantments", EnchantmentData::read, Enchantments::use);
    }

    /**
     * The table is in: items show their enchantments from now on, so those made before it came (someone who joined
     * in the first ticks) are made again.
     */
    private static void use(EnchantmentData data) {
        EnchantmentData.use(data);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (User.ifLoaded(player.getUniqueId()) != null && !InventorySyncListener.frozen(player)) ItemBuilder.refreshInventory(player);
        }
    }

    /** The page's header (the wiki's Enchant Item). */
    public static Icon header() {
        return new Icon(Material.ENCHANTING_TABLE, "&aEnchant Item", "&7Add and remove enchantments from", "&7the item in the slot above!");
    }

    @Override
    public Look look() {
        return new Look(Material.ENCHANTING_TABLE, null);
    }

    @Override
    public boolean applies(HexItem item) {
        return !EnchantRules.offered(EnchantmentData.current(), item.item(), false).isEmpty();
    }

    @Override
    public List<String> summary(HexItem item) {
        return List.of(EnchantRules.summary(EnchantmentData.current(), item.item(), EnchantRules.on(item.tag())));
    }

    @Override
    public void open(HexSession session) {
        session.open(new EnchantItemPage(session, false));
    }
}
