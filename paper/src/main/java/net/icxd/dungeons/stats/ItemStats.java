package net.icxd.dungeons.stats;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.reforge.Reforge;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** What a SkyBlock item adds to its holder's stats. */
public final class ItemStats {
    private ItemStats() {
    }

    /**
     * The item's own stats, its reforge, hot potato books, the stats its enchantments grant (Growth,
     * Protection, ...) and its attributes, which count for whoever wears or holds it if they meet the attribute's
     * requirement. Not a SkyBlock item: nothing.
     */
    public static Stats of(ItemStack stack, Player wearer) {
        Stats stats = new Stats();
        if (stack == null || stack.isEmpty()) return stats;
        NBTTagCompound tag = ItemNBT.read(stack);
        if (tag == null) return stats;
        SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
        if (item == null) return stats;

        Stats base = item.stats();
        stats.add(base);
        // Each star on a dungeon item adds 2% of its base stats (see ItemBuilder: what the lore shows is what counts).
        if (item.dungeonItem()) {
            int stars = Math.min(ItemBuilder.starCount(tag), 5);
            for (Stat stat : Stat.values()) stats.add(stat, base.get(stat) * 0.02 * stars);
        }
        if (tag.getBoolean("art_of_war")) stats.add(Stat.STRENGTH, 5);
        String rarityName = tag.getString("rarity");
        Rarity rarity = rarityName.isEmpty() ? item.rarity() : Rarity.valueOf(rarityName);
        if (!tag.getString("reforge").isEmpty()) stats.add(Reforge.valueOf(tag.getString("reforge")).getStats().at(rarity));

        int books = tag.getInt("hot_potato_books");
        if (item.genericItemType() == GenericItemType.WEAPON) stats.add(Stat.DAMAGE, books * 2).add(Stat.STRENGTH, books * 2);
        if (item.genericItemType() == GenericItemType.ARMOR) stats.add(Stat.HEALTH, books * 4).add(Stat.DEFENSE, books * 2);

        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = 0; i < enchantments.size(); i++) {
            Enchantment enchant = Enchantment.getByIdentifiable(enchantments.get(i).getString("name") + "." + enchantments.get(i).getInt("lvl"));
            if (enchant.getType() != null) stats.add(enchant.getType().getStats(enchant.getLevel()));
        }

        if (wearer != null && tag.hasKey("attribute_1") && tag.hasKey("attribute_2")) {
            addAttribute(stats, Attribute.of(tag.getString("attribute_1")), tag.getInt("attribute_1_level"), wearer);
            addAttribute(stats, Attribute.of(tag.getString("attribute_2")), tag.getInt("attribute_2_level"), wearer);
        }
        return stats;
    }

    private static void addAttribute(Stats stats, Attribute attribute, int level, Player wearer) {
        if (attribute.requirement().test(wearer)) stats.add(attribute.getStatsFunction().apply(level));
    }
}
