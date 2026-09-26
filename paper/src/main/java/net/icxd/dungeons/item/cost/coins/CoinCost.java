package net.icxd.dungeons.item.cost.coins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class CoinCost extends Cost {
    private final int amount;

    @Override
    public boolean canPay(Player player, User user) {
        return user.getCoins() >= amount;
    }

    @Override
    public void pay(Player player, User user) {
        user.getDocument().append("coins", user.getCoins() - amount);
    }
}
