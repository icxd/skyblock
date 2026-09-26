package net.icxd.dungeons.item.cost.essence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.icxd.dungeons.item.cost.Cost;
import net.icxd.dungeons.user.User;
import org.bson.Document;
import org.bukkit.entity.Player;

import java.util.function.Predicate;

@Getter
@AllArgsConstructor
public class EssenceCost extends Cost {
    private final EssenceType essenceType;
    private final int amount;

    @Override
    public Predicate<Player> check() {
        return (player) -> User.getUser(player.getUniqueId()).getDocument().get("dungeons", Document.class).get("essence", Document.class).getInteger(essenceType.name().toLowerCase()) >= amount;
    }
}
