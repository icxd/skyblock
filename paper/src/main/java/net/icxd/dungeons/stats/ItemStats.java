package net.icxd.dungeons.stats;

import net.icxd.dungeons.attributes.Attribute;
import net.icxd.dungeons.dungeons.instance.RunManager;
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
     * Critical, ...) and its attributes, which count for whoever wears or holds it if they meet the attribute's
     * requirement. In a dungeon run a dungeon item's come to what its lore's dark gray brackets say (see
     * {@link #of(SkyBlockItem, NBTTagCompound, Double)}). Not a SkyBlock item: nothing.
     */
    public static Stats of(ItemStack stack, Player wearer) {
        if (stack == null || stack.isEmpty()) return new Stats();
        NBTTagCompound tag = ItemNBT.read(stack);
        if (tag == null) return new Stats();
        SkyBlockItem item = ItemRegistry.get(tag.getString("id"));
        if (item == null) return new Stats();

        Double catacombs = wearer != null && item.dungeonItem() && RunManager.inRun(wearer) ? ItemBuilder.catacombsBoost(wearer) : null;
        Stats stats = of(item, tag, catacombs);
        if (wearer != null && tag.hasKey("attribute_1") && tag.hasKey("attribute_2")) {
            addAttribute(stats, Attribute.of(tag.getString("attribute_1")), tag.getInt("attribute_1_level"), wearer);
            addAttribute(stats, Attribute.of(tag.getString("attribute_2")), tag.getInt("attribute_2_level"), wearer);
        }
        return stats;
    }

    /**
     * The stats its lore lists: its own, its reforge's, its hot potato books', Art of War's and its
     * enchantments'. On a dungeon item each star adds 2% of its own stats ({@link ItemBuilder#starBonus});
     * in a dungeon ({@code catacombs} is the wearer's Catacombs boost there, null elsewhere) the whole
     * line is multiplied instead by
     * {@link ItemBuilder#dungeonFactor} (+10% a star and the Catacombs boost), as Hypixel does: the
     * recorded Giant's Sword's 265 Strength was 922.2 in a dungeon.
     */
    public static Stats of(SkyBlockItem item, NBTTagCompound tag, Double catacombs) {
        Stats stats = new Stats();
        Stats base = item.stats();
        stats.add(base);
        if (tag.getBoolean("art_of_war")) stats.add(Stat.STRENGTH, 5);
        Rarity rarity = ItemBuilder.rarity(item, tag);
        if (!tag.getString("reforge").isEmpty()) stats.add(Reforge.valueOf(tag.getString("reforge")).getStats().at(rarity));

        int books = tag.getInt("hot_potato_books");
        if (item.genericItemType() == GenericItemType.WEAPON) stats.add(Stat.DAMAGE, books * 2).add(Stat.STRENGTH, books * 2);
        if (item.genericItemType() == GenericItemType.ARMOR) stats.add(Stat.HEALTH, books * 4).add(Stat.DEFENSE, books * 2);

        NBTTagList enchantments = tag.getList("enchantments", 10);
        for (int i = 0; i < enchantments.size(); i++) {
            Enchantment enchant = Enchantment.getByIdentifiable(enchantments.get(i).getString("name") + "." + enchantments.get(i).getInt("lvl"));
            if (enchant.getType() != null) stats.add(enchant.getType().getStats(enchant.getLevel()));
        }

        if (!item.dungeonItem()) return stats;
        int stars = Math.min(ItemBuilder.starCount(tag), 5);
        for (Stat stat : Stat.values()) {
            // In a dungeon only what the lore gives a bracket grows: stats above 0, not breaking power or
            // a weapon's own ability damage.
            boolean boosted = catacombs != null && stats.get(stat) > 0 && stat != Stat.BREAKING_POWER && stat != Stat.WEAPON_ABILITY_DAMAGE;
            if (boosted) stats.set(stat, stats.get(stat) * ItemBuilder.dungeonFactor(stat, stars, catacombs));
            else stats.add(stat, ItemBuilder.starBonus(stat, base.get(stat), stars));
        }
        return stats;
    }

    private static void addAttribute(Stats stats, Attribute attribute, int level, Player wearer) {
        if (attribute.requirement().test(wearer)) stats.add(attribute.getStatsFunction().apply(level));
    }
}
