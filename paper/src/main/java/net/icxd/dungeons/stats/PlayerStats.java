package net.icxd.dungeons.stats;

import net.icxd.dungeons.dwarven.Perk;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** A player's stats. Use {@link net.icxd.dungeons.session.PlayerSession#stats()}, which keeps them for the tick. */
public final class PlayerStats {
    private PlayerStats() {
    }

    /**
     * The base, the armor they wear, what they hold (unless its stats only count when worn or
     * equipped, see {@link SkyBlockItem#statsWhenHeld()}), their skill levels' bonuses (see
     * {@link Skills#stats}) and their Heart of the Mountain perks.
     */
    public static Stats of(Player player) {
        Stats stats = Stats.base();
        PlayerInventory inventory = player.getInventory();
        ItemStack hand = inventory.getItemInMainHand();
        if (countsInHand(hand)) stats.add(ItemStats.of(hand, player));
        for (ItemStack armor : inventory.getArmorContents()) stats.add(ItemStats.of(armor, player));
        User user = User.ifLoaded(player.getUniqueId());
        if (user != null) {
            stats.add(Skills.stats(user.profile()));
            for (Perk perk : Perk.values()) {
                Integer level = user.profileValue("dwarvenMines.hotm.tree." + perk.name(), Integer.class);
                if (level != null && level > 0) stats.add(perk.getStats().apply(level));
            }
        }
        return stats;
    }

    private static boolean countsInHand(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        NBTTagCompound tag = ItemNBT.read(stack);
        SkyBlockItem item = tag == null ? null : ItemRegistry.get(tag.getString("id"));
        return item == null || item.statsWhenHeld();
    }
}
