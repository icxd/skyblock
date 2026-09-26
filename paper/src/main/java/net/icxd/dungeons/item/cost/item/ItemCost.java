package net.icxd.dungeons.item.cost.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** An amount of one SkyBlock item (matched by its id: any player head isn't a Heavy Pearl). */
@Getter
@AllArgsConstructor
public class ItemCost extends Cost {
    private final SkyBlockItem item;
    private final int amount;

    private boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ItemNBT data = ItemNBT.of(stack);
        return data.hasTag() && item.id().equalsIgnoreCase(data.getTag().getString("id"));
    }

    @Override
    public boolean canPay(Player player, User user) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (matches(stack)) count += stack.getAmount();
        }
        return count >= amount;
    }

    @Override
    public void pay(Player player, User user) {
        int left = amount;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (left <= 0) break;
            if (!matches(stack)) continue;
            int take = Math.min(left, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            left -= take;
        }
    }
}
