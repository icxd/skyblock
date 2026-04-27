package net.icxd.dungeons.item.cost.coins;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.user.User;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class CoinCost extends Cost {
    private final int amount;

    @Override
    public Predicate<Player> check() { return (player) -> User.getUser(player.getUniqueId()).getDocument().getInteger("coins") >= amount; }

}
