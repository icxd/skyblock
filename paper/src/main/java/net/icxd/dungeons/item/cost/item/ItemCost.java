package net.icxd.dungeons.item.cost.item;

import lombok.Getter;
import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** An amount of one SkyBlock item (matched by its id: any player head isn't a Heavy Pearl). */
@Getter
public class ItemCost extends Cost {
    /** By id, so definitions can name items that are registered after them. */
    private final String itemId;
    private final int amount;

    public ItemCost(String itemId, int amount) {
        this.itemId = itemId;
        this.amount = amount;
    }

    public ItemCost(SkyBlockItem item, int amount) {
        this(item.id(), amount);
    }

    /** Null if there's no such item. */
    public SkyBlockItem getItem() {
        return ItemRegistry.get(itemId);
    }

    private boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        NBTTagCompound tag = ItemNBT.read(stack);
        return tag != null && itemId.equalsIgnoreCase(tag.getString("id"));
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
