package net.icxd.dungeons.item.cost.item;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.Cost;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

@Getter
@Setter
@AllArgsConstructor
public class ItemCost extends Cost {
    private final SkyBlockItem item;
    private int amount;

    @Override
    public Predicate<Player> check() {
        return (player) -> {
            ItemStack itemStack = ItemBuilder.build(item);
            ItemStack[] contents = player.getInventory().getContents();
            int count = 0;
            for (ItemStack content : contents) {
                if (content == null) continue;
                if (content.getType() != itemStack.getType()) continue;
                count += content.getAmount();
            }
            return count >= amount;
        };
    }
}
