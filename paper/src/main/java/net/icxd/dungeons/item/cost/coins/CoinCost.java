package net.icxd.dungeons.item.cost.coins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.economy.Purse;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

/** Coins from the purse (an upgrade star's, a gemstone slot's, a shop's). */
@Getter
@AllArgsConstructor
public class CoinCost extends Cost {
    private final int amount;

    @Override
    public boolean canPay(Player player, User user) {
        return Purse.has(user, amount);
    }

    @Override
    public void pay(Player player, User user) {
        Purse.take(user, amount);
    }
}
