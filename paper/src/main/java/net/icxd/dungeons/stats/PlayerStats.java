package net.icxd.dungeons.stats;

import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** A player's stats. Use {@link net.icxd.dungeons.session.PlayerSession#stats()}, which keeps them for the tick. */
public final class PlayerStats {
    private PlayerStats() {
    }

    /**
     * The base, the armor they wear, what they hold (unless it's armor: that counts when worn) and
     * their Heart of the Mountain perks.
     */
    public static Stats of(Player player) {
        Stats stats = Stats.base();
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (!isArmor(hand)) stats.add(ItemStats.of(hand, player));
        for (ItemStack armor : inventory.getArmorContents()) stats.add(ItemStats.of(armor, player));
        User user = User.ifLoaded(player.getUniqueId());
        if (user != null) {
            for (Perk perk : Perk.values()) {
                Integer level = user.get("dwarvenMines.hotm.tree." + perk.name(), Integer.class);
                if (level != null && level > 0) stats.add(perk.getStats().apply(level));
            }
        }
        return stats;
    }

    private static boolean isArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = ItemNBT.of(stack).getTag();
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item != null && item.genericItemType() == GenericItemType.ARMOR;
    }
}
