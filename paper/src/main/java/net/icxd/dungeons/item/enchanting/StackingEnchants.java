package net.icxd.dungeons.item.enchanting;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.ItemCounters;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.utils.Text;
import net.icxd.dungeons.utils.Utils;

/**
 * Stacking enchantments, which tier up as what they count on their item grows (the wiki's Stacking Enchantments):
 * Compact's blocks mined, Champion's Combat XP and the rest. The count is kept on the item under Hypixel's own key
 * (a live Titanium Drill's data has "compact_blocks": 4481; Skyblocker's StackingEnchantProgressTooltip names the
 * others), with {@link ItemCounters}, and each level's text says what the next tier takes ("&8100 blocks to tier
 * up!"). Live lore shows the count after the name while there's a tier to go ("&9Compact VIII &8202,861", not at X)
 * and the next tier's line after the description. Main thread.
 */
public final class StackingEnchants {
    /** Where each one's count is kept on an item, by the enchantment's id (Skyblocker's StackingEnchantProgressTooltip). */
    private static final Map<String, String> COUNTERS = Map.of("champion", "champion_combat_xp", "compact", "compact_blocks",
            "cultivating", "farmed_cultivating", "expertise", "expertise_kills", "hecatomb", "hecatomb_s_runs",
            "absorb", "absorb_logs_chopped", "toxophilite", "toxophilite_combat_xp");
    /** "&8100 blocks to tier up!", "&81.5k blocks", "&82.5m Combat XP". */
    private static final Pattern NEXT = Pattern.compile("^(?:&.)*([\\d.]+)([km]?) ");

    private StackingEnchants() {
    }

    /** The key its count is kept under on an item; null for one that doesn't stack. */
    public static String counter(String id) {
        return id == null ? null : COUNTERS.get(id);
    }

    /** What a level's next tier takes, from its tier-up text: 100 for "&8100 blocks", 1,500 for "&81.5k", 2,500,000 for "&82.5m"; -1 for none. */
    public static double nextTier(String tierUp) {
        if (tierUp == null) return -1;
        Matcher m = NEXT.matcher(tierUp);
        if (!m.find()) return -1;
        double value = Double.parseDouble(m.group(1));
        return switch (m.group(2)) {
            case "k" -> value * 1_000;
            case "m" -> value * 1_000_000;
            default -> value;
        };
    }

    /** The level a count takes it to from {@code level}: past each next tier the count has reached, never lower. */
    public static int tier(EnchantmentType type, int level, double count) {
        int tier = level;
        while (tier < type.getMaxLevel()) {
            double next = nextTier(type.getTierUp(tier));
            if (next < 0 || count < next) break;
            tier++;
        }
        return tier;
    }

    /** " &8202,861" after its name in lore: its count, while it has one and a tier to go; "" otherwise. */
    public static String countSuffix(NBTTagCompound tag, Enchantment enchantment) {
        String key = enchantment.getType() == null ? null : counter(enchantment.getType().getNamespace());
        if (key == null || !tag.hasKey(key) || enchantment.getType().getTierUp(enchantment.getLevel()) == null) return "";
        return " &8" + Text.number(Math.floor(ItemCounters.get(tag, key)));
    }

    /**
     * Counts {@code amount} more for an enchantment on what they hold in their main hand, if it has it, and tiers it up
     * when the count reaches its next tier. The item is made again for them, so its lore follows. Returns its level
     * after; 0 if what they hold doesn't have it, or their inventory can't change now (it's being handed on).
     */
    public static int addHeld(Player player, String id, double amount) {
        String key = counter(id);
        if (key == null || InventorySyncListener.frozen(player)) return 0;
        PlayerInventory inventory = player.getInventory();
        ItemStack stack = inventory.getItemInMainHand();
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        NBTTagCompound stored = item == null ? null : stored(tag, id);
        if (stored == null) return 0;
        int level = stored.getInt("lvl");
        double count = ItemCounters.add(tag, key, amount);
        EnchantmentType type = EnchantmentType.getByNamespace(id);
        int tier = type == null ? level : tier(type, level, count);
        if (tier != level) stored.setInt("lvl", tier);
        inventory.setItemInMainHand(ItemBuilder.build(item, tag, stack.getAmount(), player));
        // UNKNOWN: Hypixel's message for it (the wiki's Compact only says there is one, "Fixed a typo on Compact
        // upgrade message"), so it's a plain one.
        if (tier != level) {
            player.sendMessage(Utils.color("&aYour &9" + type.getName() + " " + Utils.getRomanNumeral(level) + " &aenchantment tiered up to &9"
                    + type.getName() + " " + Utils.getRomanNumeral(tier) + "&a!"));
        }
        return tier;
    }

    /** The item's stored entry for this enchantment (under its id or Hypixel's); null if it hasn't got it. */
    private static NBTTagCompound stored(NBTTagCompound tag, String id) {
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(EnchantmentData.id(list.get(i).getString("name")))) return list.get(i);
        }
        return null;
    }
}
